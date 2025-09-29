package com.translator.messagingapp.mms;

import com.translator.messagingapp.system.*;

import com.translator.messagingapp.message.*;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.translator.messagingapp.mms.NotificationTransaction;
import com.translator.messagingapp.mms.Transaction;
import com.translator.messagingapp.mms.TransactionService;
import com.translator.messagingapp.mms.compat.MmsCompatibilityManager;
import com.translator.messagingapp.mms.MmsDownloadService;
import com.translator.messagingapp.mms.MmsHelper;

/**
 * Broadcast receiver for handling incoming MMS messages.
 * This is required for an app to be eligible as the default SMS app.
 * Now uses the new transaction-based architecture for MMS handling.
 */
public class MmsReceiver extends BroadcastReceiver {
    private static final String TAG = "MmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "MMS received: " + intent.getAction());

        try {
            // First, try to handle with the new transaction architecture
            if (handleWithTransactionArchitecture(context, intent)) {
                Log.d(TAG, "MMS handled by transaction architecture");
                return;
            }
            
            // Fallback to existing MessageService handling
            handleWithMessageService(context, intent);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling MMS", e);
            
            // Final fallback to MessageService
            handleWithMessageService(context, intent);
        }
    }
    
    /**
     * Handles MMS using the new transaction-based architecture.
     *
     * @param context The application context
     * @param intent The incoming MMS intent
     * @return True if handled successfully
     */
    private boolean handleWithTransactionArchitecture(Context context, Intent intent) {
        try {
            // First try using the new MmsDownloadService for API 29+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                if (handleWithMmsDownloadService(context, intent)) {
                    return true;
                }
            }
            
            // Get the appropriate receiving strategy for this Android version
            MmsCompatibilityManager.MmsReceivingStrategy strategy = 
                    MmsCompatibilityManager.getReceivingStrategy(context);
            
            Log.d(TAG, "Using receiving strategy: " + strategy.getStrategyName());
            
            // Extract MMS data from intent - improved for Android 10+
            byte[] pushData = extractMmsData(intent);
            if (pushData == null) {
                Log.w(TAG, "No MMS data in intent");
                return false;
            }
            
            Log.d(TAG, "Extracted " + pushData.length + " bytes of MMS data");
            
            // Use the strategy to handle the notification
            if (strategy.handleMmsNotification(pushData)) {
                Log.d(TAG, "MMS notification handled by strategy: " + strategy.getStrategyName());
                return true;
            }
            
            // Fallback to direct transaction handling
            return handleWithDirectTransactionArchitecture(context, pushData);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in transaction architecture handling", e);
            return false;
        }
    }
    
    /**
     * Extracts MMS data from the intent with enhanced Android 10+ support.
     * @param intent The MMS intent
     * @return The extracted MMS data or null if not found
     */
    private byte[] extractMmsData(Intent intent) {
        // Android 10+ may use different extra keys
        byte[] pushData = intent.getByteArrayExtra("data");
        if (pushData != null) {
            return pushData;
        }
        
        // Try alternative keys used by different Android versions
        pushData = intent.getByteArrayExtra("android.provider.Telephony.WAP_PUSH_RECEIVED");
        if (pushData != null) {
            return pushData;
        }
        
        pushData = intent.getByteArrayExtra("pdu");
        if (pushData != null) {
            return pushData;
        }
        
        // Check if we have MMS data in a different format
        String data = intent.getStringExtra("data_string");
        if (data != null) {
            try {
                return data.getBytes("UTF-8");
            } catch (Exception e) {
                Log.e(TAG, "Error converting string data to bytes", e);
            }
        }
        
        return null;
    }
    
    /**
     * Handles MMS using direct transaction architecture (fallback).
     *
     * @param context The application context
     * @param pushData The MMS push data
     * @return True if handled successfully
     */
    private boolean handleWithDirectTransactionArchitecture(Context context, byte[] pushData) {
        try {
            Log.d(TAG, "Processing MMS with direct transaction architecture");
            
            // Create a placeholder URI for the notification
            Uri notificationUri = createNotificationEntry(context, pushData);
            if (notificationUri == null) {
                Log.e(TAG, "Failed to create notification entry");
                return false;
            }
            
            // Check if auto-download is enabled
            if (NotificationTransaction.allowAutoDownload(context)) {
                // Start notification transaction to download the MMS
                Intent serviceIntent = new Intent(context, TransactionService.class);
                serviceIntent.putExtra(TransactionService.EXTRA_URI, notificationUri.toString());
                serviceIntent.putExtra(TransactionService.EXTRA_TRANSACTION_TYPE, Transaction.NOTIFICATION_TRANSACTION);
                context.startService(serviceIntent);
                
                Log.d(TAG, "Started notification transaction for auto-download");
            } else {
                Log.d(TAG, "Auto-download disabled, notification stored for manual download");
            }
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error in direct transaction handling", e);
            return false;
        }
    }
    
    /**
     * Handles MMS using the existing MessageService (fallback).
     *
     * @param context The application context
     * @param intent The incoming MMS intent
     */
    private void handleWithMessageService(Context context, Intent intent) {
        try {
            Log.d(TAG, "Using fallback MessageService handling");
            
            // Get the MessageService from the application
            TranslatorApp app = (TranslatorApp) context.getApplicationContext();
            MessageService messageService = app.getMessageService();

            if (messageService != null) {
                // Handle the MMS message
                Log.d(TAG, "Passing MMS to MessageService");
                messageService.handleIncomingMms(intent);
            } else {
                Log.e(TAG, "MessageService is null, cannot process MMS");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in MessageService fallback handling", e);
        }
    }
    
    /**
     * Handles MMS using the new MmsDownloadService (API 29+).
     *
     * @param context The application context
     * @param intent The incoming MMS intent
     * @return True if handled successfully
     */
    private boolean handleWithMmsDownloadService(Context context, Intent intent) {
        try {
            Log.d(TAG, "Attempting to handle MMS with MmsDownloadService (Android 10+)");
            
            // Extract MMS data from intent with improved extraction
            byte[] pushData = extractMmsData(intent);
            if (pushData == null) {
                Log.w(TAG, "No MMS data in intent for MmsDownloadService");
                return false;
            }
            
            Log.d(TAG, "Processing " + pushData.length + " bytes of MMS data");
            
            // Create a notification entry first using enhanced creation
            Uri notificationUri = createEnhancedNotificationEntry(context, pushData, intent);
            if (notificationUri == null) {
                Log.e(TAG, "Failed to create notification entry for MmsDownloadService");
                return false;
            }
            
            Log.d(TAG, "Created notification entry: " + notificationUri);
            
            // Check if auto-download is enabled with enhanced checking
            if (NotificationTransaction.allowAutoDownload(context)) {
                // Use MmsDownloadService to download the MMS
                String threadId = intent.getStringExtra("thread_id");
                if (threadId == null) {
                    // Try to extract thread ID from other sources
                    threadId = extractThreadId(context, intent);
                }
                
                Log.d(TAG, "Starting auto-download for thread: " + threadId);
                MmsDownloadService.startMmsDownload(context, notificationUri, threadId);
            } else {
                Log.d(TAG, "Auto-download disabled, notification stored for manual download");
                // Still process the notification to extract basic info for UI
                MmsDownloadService.startMmsProcessing(context, notificationUri);
            }
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling MMS with MmsDownloadService", e);
            return false;
        }
    }
    
    /**
     * Creates an enhanced notification entry in the content provider for Android 10+.
     *
     * @param context The application context
     * @param pushData The MMS push data
     * @param intent The original intent (for additional metadata)
     * @return The URI of the created notification, or null if failed
     */
    private Uri createEnhancedNotificationEntry(Context context, byte[] pushData, Intent intent) {
        try {
            Log.d(TAG, "Creating enhanced notification entry for " + pushData.length + " bytes of data");
            
            // Use MmsHelper to create a proper notification entry
            MmsHelper mmsHelper = new MmsHelper(context);
            Uri notificationUri = mmsHelper.createNotificationEntry(pushData);
            
            if (notificationUri != null) {
                // Add additional metadata from the intent if available
                try {
                    String sender = intent.getStringExtra("sender");
                    String subject = intent.getStringExtra("subject");
                    long timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis());
                    
                    if (sender != null || subject != null) {
                        mmsHelper.updateNotificationMetadata(notificationUri, sender, subject, timestamp);
                    }
                } catch (Exception metaError) {
                    Log.w(TAG, "Could not add metadata to notification entry", metaError);
                    // Not a critical error, continue with basic notification
                }
            }
            
            return notificationUri;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to create enhanced notification entry", e);
            // Fallback to simple URI creation
            return createNotificationEntry(context, pushData);
        }
    }
    
    /**
     * Extracts thread ID from various sources in the intent.
     * @param context The application context
     * @param intent The MMS intent
     * @return Thread ID or null if not found
     */
    private String extractThreadId(Context context, Intent intent) {
        // Try various ways to get thread ID
        String threadId = intent.getStringExtra("thread_id");
        if (threadId != null) return threadId;
        
        // Try to get from subscription info
        int subscriptionId = intent.getIntExtra("subscription", -1);
        if (subscriptionId != -1) {
            threadId = "sub_" + subscriptionId;
        }
        
        // Try to extract from sender address
        String sender = intent.getStringExtra("sender");
        if (sender != null) {
            // This is a simplified approach - in a real implementation,
            // you'd query the threads table for the existing thread
            threadId = "addr_" + sender.replaceAll("[^\\d]", "");
        }
        
        Log.d(TAG, "Extracted/generated thread ID: " + threadId);
        return threadId;
    }
    
    /**
     * Creates a notification entry in the content provider.
     *
     * @param context The application context
     * @param pushData The MMS push data
     * @return The URI of the created notification, or null if failed
     */
    private Uri createNotificationEntry(Context context, byte[] pushData) {
        try {
            // This would parse the push data and create a notification entry
            // For now, return a placeholder URI
            Log.d(TAG, "Creating notification entry for " + pushData.length + " bytes of data");
            return Uri.parse("content://mms/inbox/notification/123");
        } catch (Exception e) {
            Log.e(TAG, "Failed to create notification entry", e);
            return null;
        }
    }
}

