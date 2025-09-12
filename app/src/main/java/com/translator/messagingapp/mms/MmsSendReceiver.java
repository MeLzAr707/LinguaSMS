
package com.translator.messagingapp.mms;

import com.translator.messagingapp.mms.*;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Broadcast receiver for handling MMS send results.
 * This receiver is triggered when MMS messages are sent to provide feedback to the UI.
 */
public class MmsSendReceiver extends BroadcastReceiver {
    private static final String TAG = "MmsSendReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received MMS send result: " + action);

        if ("com.translator.messagingapp.MMS_SENT".equals(action)) {
            handleMmsSentResult(context, intent);
        } else if ("android.provider.Telephony.MMS_SENT".equals(action)) {
            handleSystemMmsSentResult(context, intent);
        } else if ("android.intent.action.MMS_SEND_REQUEST".equals(action)) {
            // Handle legacy MMS send requests
            handleLegacyMmsSendRequest(context, intent);
        }
    }

    /**
     * Handles the result of an MMS send operation (new API).
     */
    private void handleMmsSentResult(Context context, Intent intent) {
        try {
            String messageUriString = intent.getStringExtra("message_uri");
            int resultCode = getResultCode();
            
            Log.d(TAG, "MMS send result: " + resultCode + " for URI: " + messageUriString);
            
            boolean success = (resultCode == android.app.Activity.RESULT_OK);
            
            // Broadcast the result to update UI
            Intent resultIntent = new Intent("com.translator.messagingapp.MMS_SEND_RESULT");
            resultIntent.putExtra("success", success);
            resultIntent.putExtra("message_uri", messageUriString);
            LocalBroadcastManager.getInstance(context).sendBroadcast(resultIntent);
            
            if (success) {
                Log.d(TAG, "MMS sent successfully");
            } else {
                Log.w(TAG, "MMS send failed with result code: " + resultCode);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling MMS sent result", e);
        }
    }

    /**
     * Handles system MMS sent results.
     */
    private void handleSystemMmsSentResult(Context context, Intent intent) {
        try {
            String messageUriString = intent.getStringExtra("message_uri");
            Log.d(TAG, "System MMS sent notification for URI: " + messageUriString);
            
            // Broadcast success to update UI
            Intent resultIntent = new Intent("com.translator.messagingapp.MMS_SEND_RESULT");
            resultIntent.putExtra("success", true);
            resultIntent.putExtra("message_uri", messageUriString);
            LocalBroadcastManager.getInstance(context).sendBroadcast(resultIntent);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling system MMS sent result", e);
        }
    }

    /**
     * Handles legacy MMS send requests.
     */
    private void handleLegacyMmsSendRequest(Context context, Intent intent) {
        Log.d(TAG, "Received legacy MMS send request");

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

            Log.d(TAG, "Processing legacy MMS send request for URI: " + uriString +
                    " to recipient: " + recipient);

            // Forward to the system MMS service
            Intent systemIntent = new Intent();
            systemIntent.setAction("android.provider.Telephony.MMS_SENT");
            systemIntent.putExtra("message_uri", uriString);
            context.sendBroadcast(systemIntent);

            Log.d(TAG, "Legacy MMS send request forwarded to system");
        } catch (Exception e) {
            Log.e(TAG, "Error processing legacy MMS send request", e);
        }
    }
}

