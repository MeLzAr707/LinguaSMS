package com.translator.messagingapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages offline capabilities for the messaging application.
 * Handles offline translation, message queuing, and network state management.
 */
public class OfflineCapabilitiesManager {
    private static final String TAG = "OfflineCapabilitiesManager";
    
    private final Context context;
    private final OfflineTranslationService offlineTranslationService;
    private final OfflineModelManager offlineModelManager;
    private final OfflineMessageQueue offlineMessageQueue;
    private final ExecutorService executorService;
    
    private boolean isNetworkAvailable;
    private List<OfflineCapabilityListener> listeners;
    
    /**
     * Interface for offline capability status updates.
     */
    public interface OfflineCapabilityListener {
        /**
         * Called when network connectivity changes.
         *
         * @param isAvailable true if network is available, false otherwise
         */
        void onNetworkStatusChanged(boolean isAvailable);
        
        /**
         * Called when offline translation availability changes.
         *
         * @param isAvailable true if offline translation is available, false otherwise
         */
        void onOfflineTranslationStatusChanged(boolean isAvailable);
        
        /**
         * Called when the message queue status changes.
         *
         * @param status The new queue status
         */
        void onQueueStatusChanged(OfflineMessageQueue.QueueStatus status);
    }
    
    /**
     * Class to hold offline capability status information.
     */
    public static class OfflineCapabilityStatus {
        public boolean networkAvailable;
        public boolean offlineTranslationAvailable;
        public boolean hasDownloadedModels;
        public int queuedMessageCount;
        public OfflineMessageQueue.QueueStatus queueStatus = null;
        
        public OfflineCapabilityStatus() {
            // Default constructor
        }
    }
    
    /**
     * Creates a new OfflineCapabilitiesManager.
     *
     * @param context The application context
     * @param userPreferences The user preferences instance
     */
    public OfflineCapabilitiesManager(Context context, UserPreferences userPreferences) {
        this.context = context.getApplicationContext();
        this.offlineTranslationService = new OfflineTranslationService(context, userPreferences);
        this.offlineModelManager = new OfflineModelManager(context);
        this.offlineMessageQueue = new OfflineMessageQueue(context);
        this.executorService = Executors.newCachedThreadPool();
        this.listeners = new ArrayList<>();
        
        // Initialize network status
        updateNetworkStatus();
        
        Log.d(TAG, "OfflineCapabilitiesManager initialized");
    }
    
    /**
     * Registers a listener for offline capability updates.
     *
     * @param listener The listener to register
     */
    public void addListener(OfflineCapabilityListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Unregisters a listener for offline capability updates.
     *
     * @param listener The listener to unregister
     */
    public void removeListener(OfflineCapabilityListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Gets the current offline capability status.
     *
     * @return The current status
     */
    public OfflineCapabilityStatus getStatus() {
        OfflineCapabilityStatus status = new OfflineCapabilityStatus();
        status.networkAvailable = isNetworkAvailable;
        status.offlineTranslationAvailable = offlineTranslationService.hasAnyDownloadedModels();
        status.hasDownloadedModels = !offlineTranslationService.getDownloadedModels().isEmpty();
        status.queuedMessageCount = offlineMessageQueue.getQueueSize();
        status.queueStatus = offlineMessageQueue.getStatus();
        
        return status;
    }
    
    /**
     * Checks if the device is currently offline.
     *
     * @return true if the device is offline, false otherwise
     */
    public boolean isOffline() {
        return !isNetworkAvailable;
    }
    
    /**
     * Checks if offline translation is available for a language pair.
     *
     * @param sourceLanguage The source language code
     * @param targetLanguage The target language code
     * @return true if offline translation is available, false otherwise
     */
    public boolean isOfflineTranslationAvailable(String sourceLanguage, String targetLanguage) {
        return offlineTranslationService.isOfflineTranslationAvailable(sourceLanguage, targetLanguage);
    }
    
    /**
     * Queues a message for offline processing.
     *
     * @param recipient The message recipient
     * @param body The message body
     * @param requiresTranslation Whether the message requires translation
     */
    public void queueMessage(String recipient, String body, boolean requiresTranslation) {
        String messageId = generateMessageId();
        OfflineMessageQueue.QueuedMessage message = new OfflineMessageQueue.QueuedMessage(
                messageId, recipient, body, requiresTranslation);
        
        offlineMessageQueue.queueMessage(message);
        
        // Notify listeners of queue status change
        notifyQueueStatusChanged(offlineMessageQueue.getStatus());
    }
    
    /**
     * Processes queued messages when network becomes available.
     */
    public void processQueuedMessages() {
        if (!isNetworkAvailable) {
            Log.d(TAG, "Cannot process queued messages - network unavailable");
            return;
        }
        
        executorService.execute(() -> {
            while (!offlineMessageQueue.isEmpty()) {
                OfflineMessageQueue.QueuedMessage message = offlineMessageQueue.processNextMessage();
                if (message != null) {
                    try {
                        // Process the message (implement actual message sending logic here)
                        processMessage(message);
                        Log.d(TAG, "Processed queued message: " + message.getId());
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing queued message: " + message.getId(), e);
                    }
                }
            }
        });
    }
    
    /**
     * Updates the network status and notifies listeners if changed.
     */
    public void updateNetworkStatus() {
        boolean wasAvailable = isNetworkAvailable;
        isNetworkAvailable = checkNetworkAvailability();
        
        if (wasAvailable != isNetworkAvailable) {
            Log.d(TAG, "Network status changed: " + (isNetworkAvailable ? "available" : "unavailable"));
            notifyNetworkStatusChanged(isNetworkAvailable);
            
            // If network became available, process queued messages
            if (isNetworkAvailable) {
                processQueuedMessages();
            }
        }
    }
    
    /**
     * Gets the offline translation service instance.
     *
     * @return The offline translation service
     */
    public OfflineTranslationService getOfflineTranslationService() {
        return offlineTranslationService;
    }
    
    /**
     * Gets the offline model manager instance.
     *
     * @return The offline model manager
     */
    public OfflineModelManager getOfflineModelManager() {
        return offlineModelManager;
    }
    
    /**
     * Gets the offline message queue instance.
     *
     * @return The offline message queue
     */
    public OfflineMessageQueue getOfflineMessageQueue() {
        return offlineMessageQueue;
    }
    
    /**
     * Checks network availability using ConnectivityManager.
     *
     * @return true if network is available, false otherwise
     */
    private boolean checkNetworkAvailability() {
        try {
            ConnectivityManager connectivityManager = 
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            
            if (connectivityManager != null) {
                NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking network availability", e);
        }
        
        return false;
    }
    
    /**
     * Processes a single queued message.
     *
     * @param message The message to process
     */
    private void processMessage(OfflineMessageQueue.QueuedMessage message) {
        // Implement actual message processing logic here
        // This could involve sending the SMS, handling translation, etc.
        Log.d(TAG, "Processing message to " + message.getRecipient() + ": " + message.getBody());
        
        // Simulate processing delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Generates a unique message ID.
     *
     * @return A unique message ID
     */
    private String generateMessageId() {
        return "msg_" + System.currentTimeMillis() + "_" + System.nanoTime();
    }
    
    /**
     * Notifies listeners of network status changes.
     *
     * @param isAvailable The new network availability status
     */
    private void notifyNetworkStatusChanged(boolean isAvailable) {
        for (OfflineCapabilityListener listener : listeners) {
            try {
                listener.onNetworkStatusChanged(isAvailable);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying listener of network status change", e);
            }
        }
    }
    
    /**
     * Notifies listeners of offline translation status changes.
     *
     * @param isAvailable The new offline translation availability status
     */
    private void notifyOfflineTranslationStatusChanged(boolean isAvailable) {
        for (OfflineCapabilityListener listener : listeners) {
            try {
                listener.onOfflineTranslationStatusChanged(isAvailable);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying listener of offline translation status change", e);
            }
        }
    }
    
    /**
     * Notifies listeners of queue status changes.
     *
     * @param status The new queue status
     */
    private void notifyQueueStatusChanged(OfflineMessageQueue.QueueStatus status) {
        for (OfflineCapabilityListener listener : listeners) {
            try {
                listener.onQueueStatusChanged(status);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying listener of queue status change", e);
            }
        }
    }
    
    /**
     * Cleans up resources and shuts down background tasks.
     */
    public void cleanup() {
        listeners.clear();
        offlineTranslationService.cleanup();
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        Log.d(TAG, "OfflineCapabilitiesManager cleanup complete");
    }
}