
package com.translator.messagingapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * Broadcast receiver for handling MMS send requests.
 * This receiver is triggered when the app attempts to send an MMS message.
 */
public class MmsSendReceiver extends BroadcastReceiver {
    private static final String TAG = "MmsSendReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received MMS send request");

        if (intent == null || !intent.hasExtra("mms_uri")) {
            Log.e(TAG, "Invalid MMS send request - missing URI");
            return;
        }

        try {
            // Get the URI of the MMS message to send
            String uriString = intent.getStringExtra("mms_uri");
            Uri messageUri = Uri.parse(uriString);

            // Get the recipient address
            String recipient = intent.getStringExtra("recipient");

            Log.d(TAG, "Processing MMS send request for URI: " + uriString +
                    " to recipient: " + recipient);

            // Forward to the system MMS service
            Intent systemIntent = new Intent();
            systemIntent.setAction("android.provider.Telephony.MMS_SENT");
            systemIntent.putExtra("message_uri", uriString);
            context.sendBroadcast(systemIntent);

            Log.d(TAG, "MMS send request forwarded to system");
        } catch (Exception e) {
            Log.e(TAG, "Error processing MMS send request", e);
        }
    }
}

