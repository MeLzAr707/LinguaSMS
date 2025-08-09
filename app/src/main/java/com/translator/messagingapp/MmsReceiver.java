package com.translator.messagingapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Broadcast receiver for handling incoming MMS messages.
 * This is required for an app to be eligible as the default SMS app.
 */
public class MmsReceiver extends BroadcastReceiver {
    private static final String TAG = "MmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "MMS received: " + intent.getAction());

        // Get the MessageService from the application
        TranslatorApp app = (TranslatorApp) context.getApplicationContext();
        MessageService messageService = app.getMessageService();

        if (messageService != null) {
            // Handle the MMS message
            Log.d(TAG, "Passing MMS to MessageService");

            // Handle the incoming MMS
            messageService.handleIncomingMms(intent);
        } else {
            Log.e(TAG, "MessageService is null, cannot process MMS");
        }
    }
}

