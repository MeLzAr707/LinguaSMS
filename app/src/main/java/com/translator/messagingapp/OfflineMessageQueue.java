package com.translator.messagingapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Persistent message queue system for handling message sending when connectivity is intermittent.
 * Ensures messages are queued and automatically sent once a stable connection is restored.
 */
public class OfflineMessageQueue {
    private static final String TAG = "OfflineMessageQueue";
    private static final String PREFS_NAME = "offline_message_queue";
    private static final String KEY_QUEUED_MESSAGES = "queued_messages";
    
    // Message states
    public static final int STATE_PENDING = 0;
    public static final int STATE_SENDING = 1;
    public static final int STATE_SENT = 2;
    public static final int STATE_FAILED = 3;
    public static final int STATE_RETRY = 4;
    
    // Message priorities
    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_NORMAL = 1;
    public static final int PRIORITY_HIGH = 2;
    public static final int PRIORITY_URGENT = 3;
    
    // Retry configuration
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final long BASE_RETRY_DELAY_MS = 30000; // 30 seconds
    private static final long MAX_RETRY_DELAY_MS = 300000; // 5 minutes
    
    private final Context context;
    private final SharedPreferences preferences;
    private final WorkManager workManager;
    private final ConnectivityManager connectivityManager;
    private final NetworkCallback networkCallback;
    private final List<QueuedMessage> messageQueue;
    
    private boolean isNetworkAvailable = false;
    private boolean isMonitoringNetwork = false;
    
    /**
     * Represents a queued message with retry logic and state management.
     */
    public static class QueuedMessage {
        public long id;
        public String recipient;
        public String messageBody;
        public String threadId;
        public int messageType; // SMS = 0, MMS = 1
        public String[] attachmentUris;
        public int state;
        public int priority;
        public long queuedTime;
        public long lastAttemptTime;
        public int retryCount;
        public String lastError;
        
        public QueuedMessage() {
            this.id = System.currentTimeMillis();
            this.queuedTime = System.currentTimeMillis();
            this.state = STATE_PENDING;
            this.priority = PRIORITY_NORMAL;
            this.retryCount = 0;
        }
        
        public JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("recipient", recipient);
            json.put("messageBody", messageBody);
            json.put("threadId", threadId);
            json.put("messageType", messageType);
            if (attachmentUris != null) {
                JSONArray uriArray = new JSONArray();
                for (String uri : attachmentUris) {
                    uriArray.put(uri);
                }
                json.put("attachmentUris", uriArray);
            }
            json.put("state", state);
            json.put("priority", priority);
            json.put("queuedTime", queuedTime);
            json.put("lastAttemptTime", lastAttemptTime);
            json.put("retryCount", retryCount);
            json.put("lastError", lastError);
            return json;
        }
        
        public static QueuedMessage fromJSON(JSONObject json) throws JSONException {
            QueuedMessage message = new QueuedMessage();
            message.id = json.getLong("id");
            message.recipient = json.optString("recipient");
            message.messageBody = json.optString("messageBody");
            message.threadId = json.optString("threadId");
            message.messageType = json.optInt("messageType", 0);
            
            if (json.has("attachmentUris")) {
                JSONArray uriArray = json.getJSONArray("attachmentUris");
                message.attachmentUris = new String[uriArray.length()];
                for (int i = 0; i < uriArray.length(); i++) {
                    message.attachmentUris[i] = uriArray.getString(i);
                }
            }
            
            message.state = json.optInt("state", STATE_PENDING);
            message.priority = json.optInt("priority", PRIORITY_NORMAL);
            message.queuedTime = json.optLong("queuedTime", System.currentTimeMillis());
            message.lastAttemptTime = json.optLong("lastAttemptTime", 0);
            message.retryCount = json.optInt("retryCount", 0);
            message.lastError = json.optString("lastError", null);
            return message;
        }
    }
    
    /**
     * Network callback for monitoring connectivity changes.
     */
    private class NetworkCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(Network network) {
            Log.d(TAG, "Network became available");
            isNetworkAvailable = true;
            processQueuedMessages();
        }
        
        @Override
        public void onLost(Network network) {
            Log.d(TAG, "Network connection lost");
            isNetworkAvailable = false;
        }
        
        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            boolean hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            boolean hasValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            
            boolean wasAvailable = isNetworkAvailable;
            isNetworkAvailable = hasInternet && hasValidated;
            
            if (!wasAvailable && isNetworkAvailable) {
                Log.d(TAG, "Network quality improved, processing queue");
                processQueuedMessages();
            }
        }
    }
    
    public OfflineMessageQueue(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.workManager = WorkManager.getInstance(this.context);
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.networkCallback = new NetworkCallback();
        this.messageQueue = new ArrayList<>();
        
        // Load queued messages from persistent storage
        loadQueueFromStorage();
        
        // Check initial network state
        updateNetworkState();
    }
    
    /**
     * Queues a message for sending when network is available.
     */
    public long queueMessage(String recipient, String messageBody, String threadId, 
                           int messageType, String[] attachmentUris, int priority) {
        QueuedMessage message = new QueuedMessage();
        message.recipient = recipient;
        message.messageBody = messageBody;
        message.threadId = threadId;
        message.messageType = messageType;
        message.attachmentUris = attachmentUris;
        message.priority = priority;
        
        synchronized (messageQueue) {
            messageQueue.add(message);
            // Sort by priority (higher priority first)
            messageQueue.sort((m1, m2) -> Integer.compare(m2.priority, m1.priority));
        }
        
        saveQueueToStorage();
        
        Log.d(TAG, "Message queued with ID: " + message.id + ", priority: " + priority);
        
        // Try to process immediately if network is available
        if (isNetworkAvailable) {
            processQueuedMessages();
        } else {
            startNetworkMonitoring();
        }
        
        return message.id;
    }
    
    /**
     * Processes all queued messages that are ready to be sent.
     */
    public void processQueuedMessages() {
        if (!isNetworkAvailable) {
            Log.d(TAG, "Network not available, skipping queue processing");
            return;
        }
        
        List<QueuedMessage> messagesToProcess = new ArrayList<>();
        
        synchronized (messageQueue) {
            for (QueuedMessage message : messageQueue) {
                if (message.state == STATE_PENDING || 
                    (message.state == STATE_RETRY && isReadyForRetry(message))) {
                    messagesToProcess.add(message);
                }
            }
        }
        
        Log.d(TAG, "Processing " + messagesToProcess.size() + " queued messages");
        
        for (QueuedMessage message : messagesToProcess) {
            scheduleMessageSending(message);
        }
    }
    
    /**
     * Checks if a message is ready for retry based on exponential backoff.
     */
    private boolean isReadyForRetry(QueuedMessage message) {
        if (message.retryCount >= MAX_RETRY_ATTEMPTS) {
            return false;
        }
        
        long retryDelay = Math.min(
            BASE_RETRY_DELAY_MS * (1L << message.retryCount),
            MAX_RETRY_DELAY_MS
        );
        
        return System.currentTimeMillis() - message.lastAttemptTime >= retryDelay;
    }
    
    /**
     * Schedules a message to be sent using WorkManager.
     */
    private void scheduleMessageSending(QueuedMessage message) {
        message.state = STATE_SENDING;
        message.lastAttemptTime = System.currentTimeMillis();
        saveQueueToStorage();
        
        Data inputData = new Data.Builder()
            .putString(MessageProcessingWorker.KEY_WORK_TYPE, 
                message.messageType == 0 ? MessageProcessingWorker.WORK_TYPE_SEND_SMS : 
                                          MessageProcessingWorker.WORK_TYPE_SEND_MMS)
            .putString(MessageProcessingWorker.KEY_RECIPIENT, message.recipient)
            .putString(MessageProcessingWorker.KEY_MESSAGE_BODY, message.messageBody)
            .putString(MessageProcessingWorker.KEY_THREAD_ID, message.threadId)
            .putLong("queue_message_id", message.id)
            .build();
        
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build();
        
        OneTimeWorkRequest sendWork = new OneTimeWorkRequest.Builder(MessageProcessingWorker.class)
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag("queued_message_" + message.id)
            .build();
        
        workManager.enqueueUniqueWork(
            "send_queued_" + message.id,
            ExistingWorkPolicy.REPLACE,
            sendWork
        );
        
        Log.d(TAG, "Scheduled sending for queued message: " + message.id);
    }
    
    /**
     * Marks a message as successfully sent and removes it from the queue.
     */
    public void markMessageSent(long messageId) {
        synchronized (messageQueue) {
            messageQueue.removeIf(message -> {
                if (message.id == messageId) {
                    message.state = STATE_SENT;
                    Log.d(TAG, "Message sent successfully: " + messageId);
                    return true;
                }
                return false;
            });
        }
        saveQueueToStorage();
    }
    
    /**
     * Marks a message as failed and schedules retry if attempts remain.
     */
    public void markMessageFailed(long messageId, String error) {
        synchronized (messageQueue) {
            for (QueuedMessage message : messageQueue) {
                if (message.id == messageId) {
                    message.retryCount++;
                    message.lastError = error;
                    
                    if (message.retryCount >= MAX_RETRY_ATTEMPTS) {
                        message.state = STATE_FAILED;
                        Log.e(TAG, "Message permanently failed after " + MAX_RETRY_ATTEMPTS + 
                             " attempts: " + messageId);
                    } else {
                        message.state = STATE_RETRY;
                        Log.w(TAG, "Message failed, will retry (" + message.retryCount + 
                             "/" + MAX_RETRY_ATTEMPTS + "): " + messageId);
                    }
                    break;
                }
            }
        }
        saveQueueToStorage();
        
        // Schedule retry processing for the future
        scheduleRetryProcessing();
    }
    
    /**
     * Schedules periodic retry processing for failed messages.
     */
    private void scheduleRetryProcessing() {
        OneTimeWorkRequest retryWork = new OneTimeWorkRequest.Builder(MessageProcessingWorker.class)
            .setInputData(new Data.Builder()
                .putString(MessageProcessingWorker.KEY_WORK_TYPE, "process_message_queue")
                .build())
            .setInitialDelay(BASE_RETRY_DELAY_MS, TimeUnit.MILLISECONDS)
            .addTag("retry_processing")
            .build();
        
        workManager.enqueueUniqueWork(
            "retry_message_queue",
            ExistingWorkPolicy.REPLACE,
            retryWork
        );
    }
    
    /**
     * Gets the current queue status.
     */
    public QueueStatus getQueueStatus() {
        QueueStatus status = new QueueStatus();
        
        synchronized (messageQueue) {
            for (QueuedMessage message : messageQueue) {
                switch (message.state) {
                    case STATE_PENDING:
                        status.pendingCount++;
                        break;
                    case STATE_SENDING:
                        status.sendingCount++;
                        break;
                    case STATE_RETRY:
                        status.retryCount++;
                        break;
                    case STATE_FAILED:
                        status.failedCount++;
                        break;
                }
            }
            status.totalCount = messageQueue.size();
        }
        
        return status;
    }

    /**
     * Gets a list of failed messages for manual intervention.
     */
    public java.util.List<QueuedMessage> getFailedMessages() {
        java.util.List<QueuedMessage> failedMessages = new java.util.ArrayList<>();
        
        synchronized (messageQueue) {
            for (QueuedMessage message : messageQueue) {
                if (message.state == STATE_FAILED) {
                    failedMessages.add(message);
                }
            }
        }
        
        return failedMessages;
    }

    /**
     * Retries a specific failed message.
     */
    public boolean retryFailedMessage(long messageId) {
        synchronized (messageQueue) {
            for (QueuedMessage message : messageQueue) {
                if (message.id == messageId && message.state == STATE_FAILED) {
                    message.state = STATE_PENDING;
                    message.retryCount = 0;
                    message.lastError = null;
                    saveQueueToStorage();
                    
                    if (isNetworkAvailable) {
                        processQueuedMessages();
                    }
                    
                    Log.d(TAG, "Manually retrying failed message: " + messageId);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Clears all failed messages from the queue.
     */
    public int clearFailedMessages() {
        int clearedCount = 0;
        
        synchronized (messageQueue) {
            java.util.Iterator<QueuedMessage> iterator = messageQueue.iterator();
            while (iterator.hasNext()) {
                QueuedMessage message = iterator.next();
                if (message.state == STATE_FAILED) {
                    iterator.remove();
                    clearedCount++;
                }
            }
        }
        
        if (clearedCount > 0) {
            saveQueueToStorage();
            Log.d(TAG, "Cleared " + clearedCount + " failed messages");
        }
        
        return clearedCount;
    }

    /**
     * Gets the next message to be processed (highest priority pending/retry).
     */
    public QueuedMessage getNextMessage() {
        synchronized (messageQueue) {
            for (QueuedMessage message : messageQueue) {
                if (message.state == STATE_PENDING || 
                    (message.state == STATE_RETRY && isReadyForRetry(message))) {
                    return message;
                }
            }
        }
        return null;
    }
    
    /**
     * Gets a list of failed messages for manual intervention.
     */
    public java.util.List<QueuedMessage> getFailedMessages() {
        java.util.List<QueuedMessage> failedMessages = new java.util.ArrayList<>();
        
        synchronized (messageQueue) {
            for (QueuedMessage message : messageQueue) {
                if (message.state == STATE_FAILED) {
                    failedMessages.add(message);
                }
            }
        }
        
        return failedMessages;
    }

    /**
     * Retries a specific failed message.
     */
    public boolean retryFailedMessage(long messageId) {
        synchronized (messageQueue) {
            for (QueuedMessage message : messageQueue) {
                if (message.id == messageId && message.state == STATE_FAILED) {
                    message.state = STATE_PENDING;
                    message.retryCount = 0;
                    message.lastError = null;
                    saveQueueToStorage();
                    
                    if (isNetworkAvailable) {
                        processQueuedMessages();
                    }
                    
                    Log.d(TAG, "Manually retrying failed message: " + messageId);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Clears all failed messages from the queue.
     */
    public int clearFailedMessages() {
        int clearedCount = 0;
        
        synchronized (messageQueue) {
            java.util.Iterator<QueuedMessage> iterator = messageQueue.iterator();
            while (iterator.hasNext()) {
                QueuedMessage message = iterator.next();
                if (message.state == STATE_FAILED) {
                    iterator.remove();
                    clearedCount++;
                }
            }
        }
        
        if (clearedCount > 0) {
            saveQueueToStorage();
            Log.d(TAG, "Cleared " + clearedCount + " failed messages");
        }
        
        return clearedCount;
    }

    /**
     * Gets the next message to be processed (highest priority pending/retry).
     */
    public QueuedMessage getNextMessage() {
        synchronized (messageQueue) {
            for (QueuedMessage message : messageQueue) {
                if (message.state == STATE_PENDING || 
                    (message.state == STATE_RETRY && isReadyForRetry(message))) {
                    return message;
                }
            }
        }
        return null;
    }

    /**
     * Queue status information.
     */
    public static class QueueStatus {
        public int totalCount = 0;
        public int pendingCount = 0;
        public int sendingCount = 0;
        public int retryCount = 0;
        public int failedCount = 0;
        
        public boolean hasMessages() {
            return totalCount > 0;
        }
        
        public boolean hasFailedMessages() {
            return failedCount > 0;
        }
    }
    
    /**
     * Starts monitoring network connectivity changes.
     */
    public void startNetworkMonitoring() {
        if (!isMonitoringNetwork && connectivityManager != null) {
            NetworkRequest.Builder builder = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
            isMonitoringNetwork = true;
            Log.d(TAG, "Started network monitoring");
        }
    }
    
    /**
     * Stops monitoring network connectivity changes.
     */
    public void stopNetworkMonitoring() {
        if (isMonitoringNetwork && connectivityManager != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            isMonitoringNetwork = false;
            Log.d(TAG, "Stopped network monitoring");
        }
    }
    
    /**
     * Updates the current network state.
     */
    private void updateNetworkState() {
        if (connectivityManager != null) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                isNetworkAvailable = capabilities != null &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            } else {
                isNetworkAvailable = false;
            }
        }
        
        Log.d(TAG, "Network available: " + isNetworkAvailable);
    }
    
    /**
     * Loads the message queue from persistent storage.
     */
    private void loadQueueFromStorage() {
        try {
            String queueJson = preferences.getString(KEY_QUEUED_MESSAGES, "[]");
            JSONArray jsonArray = new JSONArray(queueJson);
            
            synchronized (messageQueue) {
                messageQueue.clear();
                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        QueuedMessage message = QueuedMessage.fromJSON(jsonArray.getJSONObject(i));
                        // Reset sending state to pending on app restart
                        if (message.state == STATE_SENDING) {
                            message.state = STATE_PENDING;
                        }
                        messageQueue.add(message);
                    } catch (JSONException e) {
                        Log.w(TAG, "Failed to parse queued message", e);
                    }
                }
                
                // Sort by priority
                messageQueue.sort((m1, m2) -> Integer.compare(m2.priority, m1.priority));
            }
            
            Log.d(TAG, "Loaded " + messageQueue.size() + " messages from storage");
        } catch (JSONException e) {
            Log.e(TAG, "Failed to load message queue from storage", e);
        }
    }
    
    /**
     * Saves the message queue to persistent storage.
     */
    private void saveQueueToStorage() {
        try {
            JSONArray jsonArray = new JSONArray();
            
            synchronized (messageQueue) {
                for (QueuedMessage message : messageQueue) {
                    try {
                        jsonArray.put(message.toJSON());
                    } catch (JSONException e) {
                        Log.w(TAG, "Failed to serialize queued message", e);
                    }
                }
            }
            
            preferences.edit()
                .putString(KEY_QUEUED_MESSAGES, jsonArray.toString())
                .apply();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to save message queue to storage", e);
        }
    }
    
    /**
     * Cleans up resources and stops monitoring.
     */
    public void cleanup() {
        stopNetworkMonitoring();
        Log.d(TAG, "OfflineMessageQueue cleanup complete");
    }
}