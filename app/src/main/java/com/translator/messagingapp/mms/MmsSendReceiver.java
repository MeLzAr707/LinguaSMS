
package com.translator.messagingapp.mms;

import com.translator.messagingapp.mms.*;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.ContentValues;

/**
 * Broadcast receiver for handling MMS send results.
 * This receiver is triggered when MMS messages are sent to provide feedback to the UI.
 * Enhanced for Android 10+ compatibility with proper result code handling.
 */
public class MmsSendReceiver extends BroadcastReceiver {
    private static final String TAG = "MmsSendReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received MMS send result: " + action);

        try {
            if ("com.translator.messagingapp.MMS_SENT".equals(action)) {
                handleMmsSentResult(context, intent);
            } else if ("android.provider.Telephony.MMS_SENT".equals(action)) {
                handleSystemMmsSentResult(context, intent);
            } else {
                Log.w(TAG, "Unknown action received: " + action);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling MMS send result", e);
        }
    }

    /**
     * Handles the result of an MMS send operation (new API).
     * Enhanced for Android 10+ with proper result code interpretation.
     */
    private void handleMmsSentResult(Context context, Intent intent) {
        try {
            String messageUriString = intent.getStringExtra("message_uri");
            String recipient = intent.getStringExtra("recipient");
            int subscriptionId = intent.getIntExtra("subscription_id", -1);
            int resultCode = getResultCode();
            
            Log.d(TAG, "MMS send result: " + resultCode + " for URI: " + messageUriString + 
                      " recipient: " + recipient + " subscription: " + subscriptionId);
            
            // Interpret result code according to Android documentation
            boolean success = (resultCode == android.app.Activity.RESULT_OK);
            String errorMessage = null;
            
            if (!success) {
                errorMessage = getMmsErrorMessage(resultCode);
                Log.w(TAG, "MMS send failed with result code: " + resultCode + " - " + errorMessage);
            } else {
                Log.d(TAG, "MMS sent successfully");
            }
            
            // Update the message status in the database
            if (messageUriString != null) {
                updateMessageStatusAfterSend(context, messageUriString, success);
            }
            
            // Broadcast the result to update UI
            broadcastSendResult(context, success, messageUriString, recipient, errorMessage);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling MMS sent result", e);
            // Broadcast failure result even if we can't process the intent properly
            broadcastSendResult(context, false, null, null, "Internal error processing send result");
        }
    }

    /**
     * Handles system MMS sent results (legacy/fallback).
     */
    private void handleSystemMmsSentResult(Context context, Intent intent) {
        try {
            String messageUriString = intent.getStringExtra("message_uri");
            int resultCode = getResultCode();
            
            Log.d(TAG, "System MMS sent notification for URI: " + messageUriString + " result: " + resultCode);
            
            boolean success = (resultCode == android.app.Activity.RESULT_OK);
            String errorMessage = success ? null : "System MMS send failed with code: " + resultCode;
            
            // Update message status if URI is available
            if (messageUriString != null) {
                updateMessageStatusAfterSend(context, messageUriString, success);
            }
            
            // Broadcast result to update UI
            broadcastSendResult(context, success, messageUriString, null, errorMessage);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling system MMS sent result", e);
        }
    }
    
    /**
     * Updates the message status in the database after send attempt.
     */
    private void updateMessageStatusAfterSend(Context context, String messageUriString, boolean success) {
        try {
            Uri messageUri = Uri.parse(messageUriString);
            ContentValues values = new ContentValues();
            
            if (success) {
                values.put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_SENT);
                Log.d(TAG, "Updated message to SENT status: " + messageUriString);
            } else {
                values.put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_FAILED);
                Log.d(TAG, "Updated message to FAILED status: " + messageUriString);
            }
            
            // Add timestamp
            values.put(Telephony.Mms.DATE, System.currentTimeMillis() / 1000);
            
            int rowsUpdated = context.getContentResolver().update(messageUri, values, null, null);
            if (rowsUpdated == 0) {
                Log.w(TAG, "No rows updated when setting message status");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating message status after send", e);
        }
    }
    
    /**
     * Broadcasts the send result to update UI.
     */
    private void broadcastSendResult(Context context, boolean success, String messageUri, 
                                   String recipient, String errorMessage) {
        try {
            Intent resultIntent = new Intent("com.translator.messagingapp.MMS_SEND_RESULT");
            resultIntent.putExtra("success", success);
            if (messageUri != null) {
                resultIntent.putExtra("message_uri", messageUri);
            }
            if (recipient != null) {
                resultIntent.putExtra("recipient", recipient);
            }
            if (errorMessage != null) {
                resultIntent.putExtra("error_message", errorMessage);
            }
            
            // Use LocalBroadcastManager for internal communication
            LocalBroadcastManager.getInstance(context).sendBroadcast(resultIntent);
            
            Log.d(TAG, "Broadcasted MMS send result: " + (success ? "SUCCESS" : "FAILED"));
            
        } catch (Exception e) {
            Log.e(TAG, "Error broadcasting send result", e);
        }
    }
    
    /**
     * Gets a human-readable error message for MMS send result codes.
     * Based on Android SmsManager result codes.
     */
    private String getMmsErrorMessage(int resultCode) {
        switch (resultCode) {
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                return "Generic failure";
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                return "Radio off";
            case SmsManager.RESULT_ERROR_NULL_PDU:
                return "Null PDU";
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                return "No service";
            case SmsManager.RESULT_ERROR_LIMIT_EXCEEDED:
                return "Limit exceeded";
            case SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE:
                return "FDN check failure";
            case SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED:
                return "Short code not allowed";
            case SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED:
                return "Short code never allowed";
            case SmsManager.RESULT_RIL_RADIO_NOT_AVAILABLE:
                return "Radio not available";
            case SmsManager.RESULT_RIL_SMS_SEND_FAIL_RETRY:
                return "Send failed, retry";
            case SmsManager.RESULT_RIL_NETWORK_REJECT:
                return "Network rejected";
            case SmsManager.RESULT_RIL_INVALID_STATE:
                return "Invalid state";
            case SmsManager.RESULT_RIL_INVALID_ARGUMENTS:
                return "Invalid arguments";
            case SmsManager.RESULT_RIL_NO_MEMORY:
                return "No memory";
            case SmsManager.RESULT_RIL_REQUEST_RATE_LIMITED:
                return "Request rate limited";
            case SmsManager.RESULT_RIL_INVALID_SMS_FORMAT:
                return "Invalid SMS format";
            case SmsManager.RESULT_RIL_SYSTEM_ERR:
                return "System error";
            case SmsManager.RESULT_RIL_ENCODING_ERR:
                return "Encoding error";
            case SmsManager.RESULT_RIL_INVALID_SMSC_ADDRESS:
                return "Invalid SMSC address";
            case SmsManager.RESULT_RIL_MODEM_ERR:
                return "Modem error";
            case SmsManager.RESULT_RIL_NETWORK_ERR:
                return "Network error";
            case SmsManager.RESULT_RIL_INTERNAL_ERR:
                return "Internal error";
            case SmsManager.RESULT_RIL_REQUEST_NOT_SUPPORTED:
                return "Request not supported";
            case SmsManager.RESULT_RIL_INVALID_MODEM_STATE:
                return "Invalid modem state";
            case SmsManager.RESULT_RIL_NETWORK_NOT_READY:
                return "Network not ready";
            case SmsManager.RESULT_RIL_OPERATION_NOT_ALLOWED:
                return "Operation not allowed";
            case SmsManager.RESULT_RIL_NO_RESOURCES:
                return "No resources";
            case SmsManager.RESULT_RIL_CANCELLED:
                return "Cancelled";
            case SmsManager.RESULT_RIL_SIM_ABSENT:
                return "SIM absent";
            default:
                return "Unknown error (code: " + resultCode + ")";
        }
    }
}
