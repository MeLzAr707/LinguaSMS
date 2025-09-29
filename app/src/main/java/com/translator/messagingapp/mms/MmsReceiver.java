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
            
            // Extract MMS data from intent
            byte[] pushData = intent.getByteArrayExtra("data");
            if (pushData == null) {
                Log.w(TAG, "No MMS data in intent");
                return false;
            }
            
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
            Log.d(TAG, "Attempting to handle MMS with MmsDownloadService");
            
            // Extract MMS data from intent
            byte[] pushData = intent.getByteArrayExtra("data");
            if (pushData == null) {
                Log.w(TAG, "No MMS data in intent for MmsDownloadService");
                return false;
            }
            
            // Create a notification entry first using MmsHelper
            MmsHelper mmsHelper = new MmsHelper(context);
            Uri notificationUri = createNotificationEntry(context, pushData);
            if (notificationUri == null) {
                Log.e(TAG, "Failed to create notification entry for MmsDownloadService");
                return false;
            }
            
            // Check if auto-download is enabled
            if (NotificationTransaction.allowAutoDownload(context)) {
                // Use MmsDownloadService to download the MMS
                String threadId = intent.getStringExtra("thread_id");
                MmsDownloadService.startMmsDownload(context, notificationUri, threadId);
                Log.d(TAG, "Started MmsDownloadService for auto-download");
            } else {
                Log.d(TAG, "Auto-download disabled, notification stored for manual download");
                // Still process the notification to extract basic info
                MmsDownloadService.startMmsProcessing(context, notificationUri);
            }
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling MMS with MmsDownloadService", e);
            return false;
        }
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

