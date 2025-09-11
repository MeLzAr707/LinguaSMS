package com.translator.messagingapp;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.List;

/**
 * Helper class for sending MMS messages using the appropriate API based on Android version.
 */
public class MmsSendingHelper {
    private static final String TAG = "MmsSendingHelper";

    /**
     * Sends an MMS message using the appropriate method based on Android version.
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Use the newer API for Android 5.0 (Lollipop) and above
                return sendMmsUsingNewApi(context, contentUri, locationUrl);
            } else {
                // Use the legacy approach for older Android versions
                return sendMmsUsingLegacyApi(context, contentUri, address);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending MMS message", e);
            return false;
        }
    }

    /**
     * Sends an MMS message using the newer Android API (Lollipop and above).
     *
     * @param context The application context
     * @param contentUri The URI of the MMS message in the content provider
     * @param locationUrl The location URL for the MMS message
     * @return True if the message was sent successfully
     */
    private static boolean sendMmsUsingNewApi(Context context, Uri contentUri, String locationUrl) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                SmsManager smsManager = SmsManager.getDefault();
                
                // Use the sendMultimediaMessage method available in API 21+
                smsManager.sendMultimediaMessage(
                    context,
                    contentUri,
                    locationUrl,
                    null,  // configOverrides
                    null   // sentIntent
                );
                
                Log.d(TAG, "MMS sent using new API: " + contentUri);
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error sending MMS using new API", e);
            return false;
        }
    }

    /**
     * Sends an MMS message using the legacy approach for older Android versions.
     *
     * @param context The application context
     * @param contentUri The URI of the MMS message in the content provider
     * @param address The recipient's phone number
     * @return True if the message was sent successfully
     */
    private static boolean sendMmsUsingLegacyApi(Context context, Uri contentUri, String address) {
        try {
            // Use the broadcast intent approach for older versions
            android.content.Intent intent = new android.content.Intent();
            intent.setAction("android.intent.action.MMS_SEND_REQUEST");
            intent.putExtra("mms_uri", contentUri.toString());
            intent.putExtra("recipient", address);
            context.sendBroadcast(intent);
            
            Log.d(TAG, "MMS sent using legacy API: " + contentUri);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sending MMS using legacy API", e);
            return false;
        }
    }
}