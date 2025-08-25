package com.translator.messagingapp;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Helper class to manage WorkManager tasks for message processing.
 * Provides convenient methods to schedule background operations.
 */
public class MessageWorkManager {
    private static final String TAG = "MessageWorkManager";
    
    // Work tags for identification and cancellation
    public static final String TAG_MESSAGE_PROCESSING = "message_processing";
    public static final String TAG_SMS_SENDING = "sms_sending";
    public static final String TAG_MMS_SENDING = "mms_sending";
    public static final String TAG_TRANSLATION = "translation";
    public static final String TAG_SYNC = "sync";
    public static final String TAG_CLEANUP = "cleanup";
    
    // Unique work names
    private static final String WORK_PERIODIC_SYNC = "periodic_message_sync";
    private static final String WORK_PERIODIC_CLEANUP = "periodic_message_cleanup";
    
    private final Context context;
    private final WorkManager workManager;

    public MessageWorkManager(Context context) {
        this.context = context.getApplicationContext();
        this.workManager = WorkManager.getInstance(this.context);
    }

    /**
     * Schedules an SMS to be sent in the background with offline queue support.
     * Uses constraints to ensure network availability and battery optimization.
     */
    public void scheduleSendSms(String recipient, String messageBody, String threadId) {
        scheduleSendSms(recipient, messageBody, threadId, OfflineMessageQueue.PRIORITY_NORMAL);
    }

    /**
     * Schedules an SMS to be sent in the background with priority and offline queue support.
     */
    public void scheduleSendSms(String recipient, String messageBody, String threadId, int priority) {
        // Try to get the offline message queue for queuing
        try {
            Context appContext = context.getApplicationContext();
            if (appContext instanceof TranslatorApp) {
                TranslatorApp app = (TranslatorApp) appContext;
                OfflineMessageQueue messageQueue = app.getOfflineMessageQueue();
                
                if (messageQueue != null) {
                    // Queue the message for offline handling
                    long queueId = messageQueue.queueMessage(recipient, messageBody, threadId, 
                        0, // SMS type
                        null, // No attachments for SMS
                        priority);
                    Log.d(TAG, "SMS queued for offline handling with ID: " + queueId);
                    return;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not use offline queue, falling back to direct scheduling", e);
        }

        // Fallback to direct scheduling if offline queue is not available
        scheduleDirectSms(recipient, messageBody, threadId);
    }

    /**
     * Directly schedules SMS sending without offline queue.
     */
    private void scheduleDirectSms(String recipient, String messageBody, String threadId) {
        Data inputData = new Data.Builder()
            .putString(MessageProcessingWorker.KEY_WORK_TYPE, MessageProcessingWorker.WORK_TYPE_SEND_SMS)
            .putString(MessageProcessingWorker.KEY_RECIPIENT, recipient)
            .putString(MessageProcessingWorker.KEY_MESSAGE_BODY, messageBody)
            .putString(MessageProcessingWorker.KEY_THREAD_ID, threadId)
            .build();

        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build();

        OneTimeWorkRequest sendSmsWork = new OneTimeWorkRequest.Builder(MessageProcessingWorker.class)
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(TAG_MESSAGE_PROCESSING)
            .addTag(TAG_SMS_SENDING)
            .build();

        workManager.enqueueUniqueWork(
            "send_sms_" + System.currentTimeMillis(),
            ExistingWorkPolicy.APPEND,
            sendSmsWork
        );

        Log.d(TAG, "Scheduled SMS sending work for recipient: " + recipient);
    }

    /**
     * Schedules an MMS to be sent in the background.
     */
    public void scheduleSendMms(String recipient, String messageBody, List<Uri> attachments) {
        Data.Builder dataBuilder = new Data.Builder()
            .putString(MessageProcessingWorker.KEY_WORK_TYPE, MessageProcessingWorker.WORK_TYPE_SEND_MMS)
            .putString(MessageProcessingWorker.KEY_RECIPIENT, recipient)
            .putString(MessageProcessingWorker.KEY_MESSAGE_BODY, messageBody);

    /**
     * Directly schedules SMS sending without offline queue.
     */
    private void scheduleDirectSms(String recipient, String messageBody, String threadId) {
        Data inputData = new Data.Builder()
            .putString(MessageProcessingWorker.KEY_WORK_TYPE, MessageProcessingWorker.WORK_TYPE_SEND_SMS)
            .putString(MessageProcessingWorker.KEY_RECIPIENT, recipient)
            .putString(MessageProcessingWorker.KEY_MESSAGE_BODY, messageBody)
            .putString(MessageProcessingWorker.KEY_THREAD_ID, threadId)
            .build();

        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build();

        OneTimeWorkRequest sendSmsWork = new OneTimeWorkRequest.Builder(MessageProcessingWorker.class)
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(TAG_MESSAGE_PROCESSING)
            .addTag(TAG_SMS_SENDING)
            .build();

        workManager.enqueueUniqueWork(
            "send_sms_" + System.currentTimeMillis(),
            ExistingWorkPolicy.APPEND,
            sendSmsWork
        );

        Log.d(TAG, "Scheduled direct SMS sending work for recipient: " + recipient);
    }

    /**
     * Schedules an MMS to be sent in the background with offline queue support.
     */
    public void scheduleSendMms(String recipient, String messageBody, List<Uri> attachments) {
        scheduleSendMms(recipient, messageBody, attachments, OfflineMessageQueue.PRIORITY_NORMAL);
    }

    /**
     * Schedules an MMS to be sent in the background with priority and offline queue support.
     */
    public void scheduleSendMms(String recipient, String messageBody, List<Uri> attachments, int priority) {
        // Try to use offline message queue first
        try {
            Context appContext = context.getApplicationContext();
            if (appContext instanceof TranslatorApp) {
                TranslatorApp app = (TranslatorApp) appContext;
                OfflineMessageQueue messageQueue = app.getOfflineMessageQueue();
                
                if (messageQueue != null) {
                    // Convert attachments to string array
                    String[] attachmentUris = null;
                    if (attachments != null && !attachments.isEmpty()) {
                        attachmentUris = new String[attachments.size()];
                        for (int i = 0; i < attachments.size(); i++) {
                            attachmentUris[i] = attachments.get(i).toString();
                        }
                    }
                    
                    // Queue the message for offline handling
                    long queueId = messageQueue.queueMessage(recipient, messageBody, null, 
                        1, // MMS type
                        attachmentUris,
                        priority);
                    Log.d(TAG, "MMS queued for offline handling with ID: " + queueId);
                    return;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not use offline queue for MMS, falling back to direct scheduling", e);
        }

        // Fallback to direct scheduling if offline queue is not available
        scheduleDirectMms(recipient, messageBody, attachments);
    }

    /**
     * Directly schedules MMS sending without offline queue.
     */
    private void scheduleDirectMms(String recipient, String messageBody, List<Uri> attachments) {
        Data.Builder dataBuilder = new Data.Builder()
            .putString(MessageProcessingWorker.KEY_WORK_TYPE, MessageProcessingWorker.WORK_TYPE_SEND_MMS)
            .putString(MessageProcessingWorker.KEY_RECIPIENT, recipient)
            .putString(MessageProcessingWorker.KEY_MESSAGE_BODY, messageBody);

        // Convert URI list to string array for Data storage
        if (attachments != null && !attachments.isEmpty()) {
            String[] uriStrings = new String[attachments.size()];
            for (int i = 0; i < attachments.size(); i++) {
                uriStrings[i] = attachments.get(i).toString();
            }
            dataBuilder.putStringArray(MessageProcessingWorker.KEY_ATTACHMENT_URIS, uriStrings);
        }

        Data inputData = dataBuilder.build();

        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true) // MMS may require storage for attachments
            .build();

        OneTimeWorkRequest sendMmsWork = new OneTimeWorkRequest.Builder(MessageProcessingWorker.class)
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(TAG_MESSAGE_PROCESSING)
            .addTag(TAG_MMS_SENDING)
            .build();

        workManager.enqueueUniqueWork(
            "send_mms_" + System.currentTimeMillis(),
            ExistingWorkPolicy.APPEND,
            sendMmsWork
        );

        Log.d(TAG, "Scheduled MMS sending work for recipient: " + recipient);
    }

    /**
     * Schedules message translation in the background.
     */
    public void scheduleTranslateMessage(String messageId, String messageBody, 
                                       String sourceLanguage, String targetLanguage) {
        Data inputData = new Data.Builder()
            .putString(MessageProcessingWorker.KEY_WORK_TYPE, MessageProcessingWorker.WORK_TYPE_TRANSLATE_MESSAGE)
            .putString(MessageProcessingWorker.KEY_MESSAGE_ID, messageId)
            .putString(MessageProcessingWorker.KEY_MESSAGE_BODY, messageBody)
            .putString(MessageProcessingWorker.KEY_SOURCE_LANGUAGE, sourceLanguage)
            .putString(MessageProcessingWorker.KEY_TARGET_LANGUAGE, targetLanguage)
            .build();

        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build();

        OneTimeWorkRequest translateWork = new OneTimeWorkRequest.Builder(MessageProcessingWorker.class)
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(TAG_MESSAGE_PROCESSING)
            .addTag(TAG_TRANSLATION)
            .build();

        workManager.enqueueUniqueWork(
            "translate_message_" + messageId,
            ExistingWorkPolicy.REPLACE, // Replace if the same message is being translated again
            translateWork
        );

        Log.d(TAG, "Scheduled translation work for message: " + messageId);
    }

    /**
     * Schedules immediate message synchronization.
     */
    public void scheduleSyncMessages() {
        Data inputData = new Data.Builder()
            .putString(MessageProcessingWorker.KEY_WORK_TYPE, MessageProcessingWorker.WORK_TYPE_SYNC_MESSAGES)
            .build();

        Constraints constraints = new Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build();

        OneTimeWorkRequest syncWork = new OneTimeWorkRequest.Builder(MessageProcessingWorker.class)
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(TAG_MESSAGE_PROCESSING)
            .addTag(TAG_SYNC)
            .build();

        workManager.enqueueUniqueWork(
            "sync_messages_immediate",
            ExistingWorkPolicy.REPLACE,
            syncWork
        );

        Log.d(TAG, "Scheduled immediate message sync work");
    }

    /**
     * Schedules periodic message synchronization.
     * Runs every 15 minutes when device is charging and battery is not low.
     */
    public void schedulePeriodicSync() {
        Data inputData = new Data.Builder()
            .putString(MessageProcessingWorker.KEY_WORK_TYPE, MessageProcessingWorker.WORK_TYPE_SYNC_MESSAGES)
            .build();

        Constraints constraints = new Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(false) // Allow sync even when not charging
            .build();

        PeriodicWorkRequest periodicSyncWork = new PeriodicWorkRequest.Builder(
                MessageProcessingWorker.class, 15, TimeUnit.MINUTES)
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(TAG_MESSAGE_PROCESSING)
            .addTag(TAG_SYNC)
            .build();

        workManager.enqueueUniquePeriodicWork(
            WORK_PERIODIC_SYNC,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncWork
        );

        Log.d(TAG, "Scheduled periodic message sync work");
    }

    /**
     * Schedules periodic cleanup of old messages and cache maintenance.
     * Runs daily when device is charging and battery is not low.
     */
    public void schedulePeriodicCleanup() {
        Data inputData = new Data.Builder()
            .putString(MessageProcessingWorker.KEY_WORK_TYPE, MessageProcessingWorker.WORK_TYPE_CLEANUP_OLD_MESSAGES)
            .build();

        Constraints constraints = new Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(true) // Cleanup only when charging
            .build();

        PeriodicWorkRequest periodicCleanupWork = new PeriodicWorkRequest.Builder(
                MessageProcessingWorker.class, 1, TimeUnit.DAYS)
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(TAG_MESSAGE_PROCESSING)
            .addTag(TAG_CLEANUP)
            .build();

        workManager.enqueueUniquePeriodicWork(
            WORK_PERIODIC_CLEANUP,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicCleanupWork
        );

        Log.d(TAG, "Scheduled periodic cleanup work");
    }

    /**
     * Schedules immediate cleanup work.
     */
    public void scheduleCleanup() {
        Data inputData = new Data.Builder()
            .putString(MessageProcessingWorker.KEY_WORK_TYPE, MessageProcessingWorker.WORK_TYPE_CLEANUP_OLD_MESSAGES)
            .build();

        Constraints constraints = new Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build();

        OneTimeWorkRequest cleanupWork = new OneTimeWorkRequest.Builder(MessageProcessingWorker.class)
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(TAG_MESSAGE_PROCESSING)
            .addTag(TAG_CLEANUP)
            .build();

        workManager.enqueueUniqueWork(
            "cleanup_messages_immediate",
            ExistingWorkPolicy.REPLACE,
            cleanupWork
        );

        Log.d(TAG, "Scheduled immediate cleanup work");
    }

    /**
     * Cancels all pending message processing work.
     */
    public void cancelAllWork() {
        workManager.cancelAllWorkByTag(TAG_MESSAGE_PROCESSING);
        Log.d(TAG, "Cancelled all message processing work");
    }

    /**
     * Cancels work by specific tag.
     */
    public void cancelWorkByTag(String tag) {
        workManager.cancelAllWorkByTag(tag);
        Log.d(TAG, "Cancelled work with tag: " + tag);
    }

    /**
     * Cancels periodic work operations.
     */
    public void cancelPeriodicWork() {
        workManager.cancelUniqueWork(WORK_PERIODIC_SYNC);
        workManager.cancelUniqueWork(WORK_PERIODIC_CLEANUP);
        Log.d(TAG, "Cancelled periodic work");
    }

    /**
     * Initializes all periodic work operations.
     * Should be called during app startup.
     */
    public void initializePeriodicWork() {
        schedulePeriodicSync();
        schedulePeriodicCleanup();
        Log.d(TAG, "Initialized all periodic work");
    }
}