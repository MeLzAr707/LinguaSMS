
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
        } else {
            Log.w(TAG, "Unknown action received: " + action);
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
}

