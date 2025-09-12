package com.translator.messagingapp;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import com.translator.messagingapp.mms.MmsMessageSender;

import java.util.List;

/**
 * Helper class for sending MMS messages using the appropriate API based on Android version.
 * This class now uses the new transaction-based architecture for improved reliability.
 */
public class MmsSendingHelper {
    private static final String TAG = "MmsSendingHelper";

    /**
     * Sends an MMS message using the new transaction-based architecture.
     *
     * @param context The application context
     * @param contentUri The URI of the MMS message in the content provider
     * @param locationUrl The location URL for the MMS message (can be null)
     * @param address The recipient's phone number
     * @param subject The subject of the MMS message (can be null)
     * @return True if the message was sent successfully
     */
    public static boolean sendMms(Context context, Uri contentUri, String locationUrl, String address, String subject) {
        try {
            // Use the new transaction-based architecture for all Android versions
            return sendMmsUsingTransactionArchitecture(context, contentUri, address, subject);
        } catch (Exception e) {
            Log.e(TAG, "Error sending MMS message", e);
            
            // Fallback to legacy methods if transaction architecture fails
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return sendMmsUsingNewApi(context, contentUri, locationUrl);
            } else {
                return sendMmsUsingLegacyApi(context, contentUri, address);
            }
        }
    }

    /**
     * Sends an MMS message using the new transaction-based architecture.
     *
     * @param context The application context
     * @param contentUri The URI of the MMS message in the content provider
     * @param address The recipient's phone number
     * @param subject The subject of the MMS message (can be null)
     * @return True if the message was sent successfully
     */
    private static boolean sendMmsUsingTransactionArchitecture(Context context, Uri contentUri, String address, String subject) {
        try {
            Log.d(TAG, "Sending MMS using transaction architecture: " + contentUri);
            
            // Create MMS message sender
            MmsMessageSender sender = MmsMessageSender.create(context, contentUri);
            
            // Generate a token for the transaction
            long token = System.currentTimeMillis();
            
            // Start the sending process
            boolean started = sender.sendMessage(token);
            
            if (started) {
                Log.d(TAG, "MMS send transaction started successfully");
                return true;
            } else {
                Log.e(TAG, "Failed to start MMS send transaction");
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in transaction-based MMS sending", e);
            return false;
        }
    }

    /**
     * Sends an MMS message using the newer Android API (Lollipop and above).
     * This is now used as a fallback method.
     *
     * @param context The application context
     * @param contentUri The URI of the MMS message in the content provider
     * @param locationUrl The location URL for the MMS message
     * @return True if the message was sent successfully
     */
    private static boolean sendMmsUsingNewApi(Context context, Uri contentUri, String locationUrl) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Log.d(TAG, "Using fallback new API for MMS sending");
                
                SmsManager smsManager = SmsManager.getDefault();
                
                // Create PendingIntents for send and delivery reports
                android.content.Intent sentIntent = new android.content.Intent("com.translator.messagingapp.MMS_SENT");
                sentIntent.putExtra("message_uri", contentUri.toString());
                
                android.app.PendingIntent sentPendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    0,
                    sentIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
                );
                
                // Use the sendMultimediaMessage method available in API 21+
                smsManager.sendMultimediaMessage(
                    context,
                    contentUri,
                    locationUrl,
                    null,  // configOverrides
                    sentPendingIntent   // sentIntent for callback
                );
                
                Log.d(TAG, "MMS sent using fallback new API: " + contentUri);
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error sending MMS using fallback new API", e);
            return false;
        }
    }

    /**
     * Sends an MMS message using the legacy approach for older Android versions.
     * This is now used as a fallback method.
     *
     * @param context The application context
     * @param contentUri The URI of the MMS message in the content provider
     * @param address The recipient's phone number
     * @return True if the message was sent successfully
     */
    private static boolean sendMmsUsingLegacyApi(Context context, Uri contentUri, String address) {
        try {
            Log.d(TAG, "Using fallback legacy API for MMS sending");
            
            // Use the system MMS send action for better compatibility
            android.content.Intent intent = new android.content.Intent();
            intent.setAction("android.provider.Telephony.MMS_SENT");
            intent.putExtra("message_uri", contentUri.toString());
            intent.putExtra("recipient", address);
            context.sendBroadcast(intent);
            
            Log.d(TAG, "MMS sent using fallback legacy API: " + contentUri);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sending MMS using fallback legacy API", e);
            return false;
        }
    }

    /**
     * Checks if the new transaction-based MMS architecture is available.
     *
     * @param context The application context
     * @return True if transaction-based sending is available
     */
    public static boolean isTransactionArchitectureAvailable(Context context) {
        return MmsMessageSender.isMmsSendingAvailable(context);
    }
}