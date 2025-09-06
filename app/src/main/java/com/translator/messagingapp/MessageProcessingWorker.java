package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * WorkManager worker for background message processing tasks.
 * Handles message sending, translation, and other background operations.
 */
public class MessageProcessingWorker extends Worker {
    private static final String TAG = "MessageProcessingWorker";

    // Work types
    public static final String WORK_TYPE_SEND_SMS = "send_sms";
    public static final String WORK_TYPE_SEND_MMS = "send_mms";
    public static final String WORK_TYPE_TRANSLATE_MESSAGE = "translate_message";
    public static final String WORK_TYPE_SYNC_MESSAGES = "sync_messages";
    public static final String WORK_TYPE_CLEANUP_OLD_MESSAGES = "cleanup_old_messages";

    // Input data keys
    public static final String KEY_WORK_TYPE = "work_type";
    public static final String KEY_RECIPIENT = "recipient";
    public static final String KEY_MESSAGE_BODY = "message_body";
    public static final String KEY_MESSAGE_ID = "message_id";
    public static final String KEY_THREAD_ID = "thread_id";
    public static final String KEY_ATTACHMENT_URIS = "attachment_uris";
    public static final String KEY_SOURCE_LANGUAGE = "source_language";
    public static final String KEY_TARGET_LANGUAGE = "target_language";

    public MessageProcessingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Data inputData = getInputData();
            String workType = inputData.getString(KEY_WORK_TYPE);
            
            if (workType == null) {
                Log.e(TAG, "Work type not specified");
                return Result.failure();
            }

            Log.d(TAG, "Starting work: " + workType);

            switch (workType) {
                case WORK_TYPE_SEND_SMS:
                    return handleSendSms(inputData);
                
                case WORK_TYPE_SEND_MMS:
                    return handleSendMms(inputData);
                
                case WORK_TYPE_TRANSLATE_MESSAGE:
                    return handleTranslateMessage(inputData);
                
                case WORK_TYPE_SYNC_MESSAGES:
                    return handleSyncMessages(inputData);
                
                case WORK_TYPE_CLEANUP_OLD_MESSAGES:
                    return handleCleanupOldMessages(inputData);
                
                default:
                    Log.e(TAG, "Unknown work type: " + workType);
                    return Result.failure();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing work", e);
            return Result.retry();
        }
    }

    /**
     * Handles SMS sending in the background.
     */
    private Result handleSendSms(Data inputData) {
        try {
            String recipient = inputData.getString(KEY_RECIPIENT);
            String messageBody = inputData.getString(KEY_MESSAGE_BODY);
            String threadId = inputData.getString(KEY_THREAD_ID);

            if (recipient == null || messageBody == null) {
                Log.e(TAG, "SMS sending failed: missing recipient or message body");
                return Result.failure();
            }

            TranslatorApp app = (TranslatorApp) getApplicationContext();
            MessageService messageService = app.getMessageService();

            if (messageService == null) {
                Log.e(TAG, "MessageService not available");
                return Result.retry();
            }

            boolean success = messageService.sendSmsMessage(recipient, messageBody, threadId, null);
            
            if (success) {
                Log.d(TAG, "SMS sent successfully to: " + recipient);
                return Result.success();
            } else {
                Log.e(TAG, "Failed to send SMS to: " + recipient);
                return Result.retry();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS", e);
            return Result.retry();
        }
    }

    /**
     * Handles MMS sending in the background.
     */
    private Result handleSendMms(Data inputData) {
        try {
            String recipient = inputData.getString(KEY_RECIPIENT);
            String messageBody = inputData.getString(KEY_MESSAGE_BODY);
            String[] attachmentUris = inputData.getStringArray(KEY_ATTACHMENT_URIS);

            if (recipient == null) {
                Log.e(TAG, "MMS sending failed: missing recipient");
                return Result.failure();
            }

            TranslatorApp app = (TranslatorApp) getApplicationContext();
            MessageService messageService = app.getMessageService();

            if (messageService == null) {
                Log.e(TAG, "MessageService not available");
                return Result.retry();
            }

            // Convert string URIs to Uri objects (simplified implementation)
            java.util.List<android.net.Uri> attachments = new java.util.ArrayList<>();
            if (attachmentUris != null) {
                for (String uriString : attachmentUris) {
                    try {
                        attachments.add(android.net.Uri.parse(uriString));
                    } catch (Exception e) {
                        Log.w(TAG, "Invalid attachment URI: " + uriString, e);
                    }
                }
            }

            boolean success = messageService.sendMmsMessage(recipient, "MMS", messageBody, attachments);
            
            if (success) {
                Log.d(TAG, "MMS sent successfully to: " + recipient);
                return Result.success();
            } else {
                Log.e(TAG, "Failed to send MMS to: " + recipient);
                return Result.retry();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending MMS", e);
            return Result.retry();
        }
    }

    /**
     * Handles message translation in the background.
     */
    private Result handleTranslateMessage(Data inputData) {
        try {
            String messageId = inputData.getString(KEY_MESSAGE_ID);
            String messageBody = inputData.getString(KEY_MESSAGE_BODY);
            String sourceLanguage = inputData.getString(KEY_SOURCE_LANGUAGE);
            String targetLanguage = inputData.getString(KEY_TARGET_LANGUAGE);

            if (messageId == null || messageBody == null || targetLanguage == null) {
                Log.e(TAG, "Translation failed: missing required parameters");
                return Result.failure();
            }

            TranslatorApp app = (TranslatorApp) getApplicationContext();
            TranslationManager translationManager = app.getTranslationManager();

            if (translationManager == null) {
                Log.e(TAG, "TranslationManager not available");
                return Result.retry();
            }

            // Perform translation using asynchronous method with synchronization
            try {
                final Object lock = new Object();
                final String[] translatedTextHolder = new String[1];
                final String[] errorMessageHolder = new String[1];
                final boolean[] isCompleted = new boolean[1];

                translationManager.translateText(messageBody, sourceLanguage, targetLanguage, 
                    new TranslationManager.TranslationCallback() {
                        @Override
                        public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                            synchronized (lock) {
                                translatedTextHolder[0] = translatedText;
                                errorMessageHolder[0] = errorMessage;
                                isCompleted[0] = true;
                                lock.notify();
                            }
                        }
                    });

                // Wait for translation to complete
                synchronized (lock) {
                    while (!isCompleted[0]) {
                        try {
                            lock.wait(30000); // Wait up to 30 seconds
                            break;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            Log.e(TAG, "Translation was interrupted", e);
                            return Result.retry();
                        }
                    }
                }

                if (translatedTextHolder[0] != null && !translatedTextHolder[0].isEmpty()) {
                    // Store translation result (implementation would depend on your storage mechanism)
                    Log.d(TAG, "Translation completed for message: " + messageId);
                    
                    // Create success data with translation result
                    Data outputData = new Data.Builder()
                        .putString("translated_text", translatedTextHolder[0])
                        .putString("message_id", messageId)
                        .build();
                    
                    return Result.success(outputData);
                } else {
                    String error = errorMessageHolder[0] != null ? errorMessageHolder[0] : "empty result";
                    Log.e(TAG, "Translation failed: " + error);
                    
                    // Check if this is a missing model error that should not be retried
                    if (error.contains("Language models not downloaded") || 
                        error.contains("models not downloaded") ||
                        error.contains("model not available")) {
                        
                        Log.w(TAG, "Translation failed due to missing language models - background workers cannot prompt for downloads");
                        
                        // Create failure data with specific error info for potential UI notification
                        Data outputData = new Data.Builder()
                            .putString("error_type", "missing_models")
                            .putString("error_message", error)
                            .putString("message_id", messageId)
                            .putString("source_language", sourceLanguage)
                            .putString("target_language", targetLanguage)
                            .build();
                        
                        return Result.failure(outputData);
                    }
                    
                    return Result.retry();
                }
            } catch (Exception translationError) {
                Log.e(TAG, "Translation error", translationError);
                return Result.retry();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in translation work", e);
            return Result.retry();
        }
    }

    /**
     * Handles message synchronization in the background.
     */
    private Result handleSyncMessages(Data inputData) {
        try {
            Log.d(TAG, "Starting message synchronization");

            TranslatorApp app = (TranslatorApp) getApplicationContext();
            MessageService messageService = app.getMessageService();

            if (messageService == null) {
                Log.e(TAG, "MessageService not available");
                return Result.retry();
            }

            // Clear message cache to force reload from database
            MessageCache.clearCache();
            
            // Load and cache conversations
            java.util.List<Conversation> conversations = messageService.loadConversations();
            Log.d(TAG, "Synchronized " + conversations.size() + " conversations");

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error synchronizing messages", e);
            return Result.retry();
        }
    }

    /**
     * Handles cleanup of old messages and cache maintenance.
     */
    private Result handleCleanupOldMessages(Data inputData) {
        try {
            Log.d(TAG, "Starting cleanup of old messages");

            TranslatorApp app = (TranslatorApp) getApplicationContext();
            
            // Clean up translation cache
            TranslationCache translationCache = app.getTranslationCache();
            if (translationCache != null) {
                translationCache.performMaintenance();
            }

            // Clear old message cache entries
            MessageCache.clearCache();
            
            // Clean up optimized message cache if available
            try {
                OptimizedMessageCache optimizedCache = new OptimizedMessageCache(getApplicationContext());
                optimizedCache.performMaintenance();
            } catch (Exception e) {
                Log.w(TAG, "OptimizedMessageCache not available or error during maintenance", e);
            }

            Log.d(TAG, "Cleanup completed successfully");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
            return Result.retry();
        }
    }
}