package com.translator.messagingapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

/**
 * Broadcast receiver for handling incoming SMS messages.
 * This is required for an app to be eligible as the default SMS app.
 */
public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "SMS received: " + intent.getAction());

        // Get the MessageService from the application
        TranslatorApp app = (TranslatorApp) context.getApplicationContext();
        MessageService messageService = app.getMessageService();

        if (messageService != null) {
            // Handle SMS_RECEIVED action (when not default app)
            if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
                Log.d(TAG, "Processing SMS_RECEIVED - app is not default SMS app");
                messageService.handleIncomingSms(intent);
            }
            // Handle SMS_DELIVER action (when default app)
            else if (Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(intent.getAction())) {
                Log.d(TAG, "Processing SMS_DELIVER - app is default SMS app");
                // For SMS_DELIVER, we just need to trigger notifications and UI refresh
                // since Android system already stores the message when we're the default app
                messageService.handleIncomingSms(intent);
            }
        } else {
            Log.e(TAG, "MessageService is null, cannot process SMS");
        }
    }
}

