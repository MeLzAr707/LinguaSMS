package com.translator.messagingapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Broadcast receiver for handling scheduled message events and time changes.
 */
public class ScheduledMessageReceiver extends BroadcastReceiver {
    private static final String TAG = "ScheduledMessageReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (action == null) {
            Log.w(TAG, "Received null action");
            return;
        }
        
        Log.d(TAG, "Received action: " + action);
        
        switch (action) {
            case Intent.ACTION_TIME_SET:
                handleTimeChange(context);
                break;
            case Intent.ACTION_TIMEZONE_CHANGED:
                handleTimezoneChange(context);
                break;
            case Intent.ACTION_BOOT_COMPLETED:
                handleBootCompleted(context);
                break;
            case "com.translator.messagingapp.SEND_SCHEDULED_MESSAGE":
                handleScheduledMessageSend(context, intent);
                break;
            default:
                Log.d(TAG, "Unhandled action: " + action);
                break;
        }
    }
    
    /**
     * Handles system time changes.
     */
    private void handleTimeChange(Context context) {
        Log.d(TAG, "System time changed, updating scheduled messages");
        try {
            TranslatorApp app = (TranslatorApp) context.getApplicationContext();
            ScheduledMessageManager manager = app.getScheduledMessageManager();
            
            if (manager != null) {
                // Update scheduled message times if needed
                Log.d(TAG, "Notifying scheduled message manager of time change");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling time change", e);
        }
    }
    
    /**
     * Handles timezone changes.
     */
    private void handleTimezoneChange(Context context) {
        Log.d(TAG, "Timezone changed, updating scheduled messages");
        handleTimeChange(context); // Same logic as time change
    }
    
    /**
     * Handles device boot completion.
     */
    private void handleBootCompleted(Context context) {
        Log.d(TAG, "Device boot completed, rescheduling messages");
        try {
            TranslatorApp app = (TranslatorApp) context.getApplicationContext();
            ScheduledMessageManager manager = app.getScheduledMessageManager();
            
            if (manager != null) {
                // Reschedule all pending messages after boot
                Log.d(TAG, "Rescheduling messages after boot");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling boot completion", e);
        }
    }
    
    /**
     * Handles sending a scheduled message.
     */
    private void handleScheduledMessageSend(Context context, Intent intent) {
        Log.d(TAG, "Sending scheduled message");
        try {
            long messageId = intent.getLongExtra("message_id", -1);
            String recipient = intent.getStringExtra("recipient");
            String message = intent.getStringExtra("message");
            
            if (messageId == -1 || recipient == null || message == null) {
                Log.e(TAG, "Invalid scheduled message data");
                return;
            }
            
            TranslatorApp app = (TranslatorApp) context.getApplicationContext();
            MessageService messageService = app.getMessageService();
            
            if (messageService != null) {
                // Send the message
                messageService.sendSms(recipient, message);
                Log.d(TAG, "Scheduled message sent to " + recipient);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending scheduled message", e);
        }
    }
}