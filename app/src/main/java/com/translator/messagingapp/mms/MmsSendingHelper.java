package com.translator.messagingapp.mms;

import com.translator.messagingapp.message.*;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import com.translator.messagingapp.mms.MmsMessageSender;
import com.translator.messagingapp.mms.compat.MmsCompatibilityManager;

import java.util.List;

/**
 * Helper class for sending MMS messages using modern Android APIs.
 * Since minimum SDK is 24 (Android N), all modern MMS APIs are available.
 */
public class MmsSendingHelper {
    private static final String TAG = "MmsSendingHelper";

    /**
     * Sends an MMS message using the best available method for the current Android version.
     * Since minimum SDK is 24, this always uses modern APIs.
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
            Log.d(TAG, "Sending MMS using compatibility manager for Android " + Build.VERSION.SDK_INT);
            
            // First validate MMS configuration
            if (!com.translator.messagingapp.mms.http.HttpUtils.validateMmsConfiguration(context)) {
                Log.e(TAG, "MMS configuration validation failed - cannot send MMS");
                return false;
            }
            
            // Use the compatibility manager to get the best strategy
            MmsCompatibilityManager.MmsSendingStrategy strategy = 
                    MmsCompatibilityManager.getSendingStrategy(context);
            
            if (strategy.isAvailable()) {
                Log.d(TAG, "Using sending strategy: " + strategy.getStrategyName());
                boolean success = strategy.sendMms(contentUri, address, subject);
                
                if (success) {
                    Log.d(TAG, "MMS sent successfully using " + strategy.getStrategyName());
                    return true;
                } else {
                    Log.w(TAG, "Primary strategy failed, trying direct SmsManager");
                    return sendMmsUsingSmsManager(context, contentUri, address);
                }
            }
            
            // Fallback to direct SmsManager (always available on API 24+)
            return sendMmsUsingSmsManager(context, contentUri, address);
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending MMS message", e);
            return false;
        }
    }

    /**
     * Sends MMS using SmsManager (always available on API 24+)
     */
    private static boolean sendMmsUsingSmsManager(Context context, Uri contentUri, String address) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            
            // Create PendingIntent for send result
            android.content.Intent sentIntent = new android.content.Intent("com.translator.messagingapp.MMS_SENT");
            sentIntent.putExtra("message_uri", contentUri.toString());
            sentIntent.putExtra("recipient", address);
            
            android.app.PendingIntent sentPendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                (int) System.currentTimeMillis(), // Unique request code
                sentIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
            );
            
            // Send using SmsManager (available since API 21, always available on our minSdkVersion 24)
            smsManager.sendMultimediaMessage(
                context,
                contentUri,
                null, // locationUrl
                null, // configOverrides
                sentPendingIntent
            );
            
            Log.d(TAG, "MMS sent using SmsManager: " + contentUri);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sending MMS using SmsManager", e);
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

    /**
     * Gets information about the MMS capabilities for the current device.
     * Since minimum SDK is 24, all modern features are supported.
     *
     * @param context The application context
     * @return MMS capability information
     */
    public static MmsCapabilityInfo getMmsCapabilities(Context context) {
        MmsCompatibilityManager.MmsFeatureFlags flags = MmsCompatibilityManager.getFeatureFlags();
        MmsCompatibilityManager.MmsSendingStrategy strategy = MmsCompatibilityManager.getSendingStrategy(context);
        
        return new MmsCapabilityInfo(
            strategy.getStrategyName(),
            strategy.isAvailable(),
            flags.supportsGroupMms,
            flags.supportsDeliveryReports,
            flags.supportsReadReports,
            flags.supportsRichContent,
            flags.supportsLargeMessages,
            flags.requiresSpecialPermissions,
            true // SmsManager API is always available on API 24+
        );
    }

    /**
     * Container for MMS capability information.
     */
    public static class MmsCapabilityInfo {
        public final String strategyName;
        public final boolean strategyAvailable;
        public final boolean supportsGroupMms;
        public final boolean supportsDeliveryReports;
        public final boolean supportsReadReports;
        public final boolean supportsRichContent;
        public final boolean supportsLargeMessages;
        public final boolean requiresSpecialPermissions;
        public final boolean smsManagerApiAvailable;

        public MmsCapabilityInfo(String strategyName, boolean strategyAvailable,
                               boolean supportsGroupMms, boolean supportsDeliveryReports,
                               boolean supportsReadReports, boolean supportsRichContent,
                               boolean supportsLargeMessages, boolean requiresSpecialPermissions,
                               boolean smsManagerApiAvailable) {
            this.strategyName = strategyName;
            this.strategyAvailable = strategyAvailable;
            this.supportsGroupMms = supportsGroupMms;
            this.supportsDeliveryReports = supportsDeliveryReports;
            this.supportsReadReports = supportsReadReports;
            this.supportsRichContent = supportsRichContent;
            this.supportsLargeMessages = supportsLargeMessages;
            this.requiresSpecialPermissions = requiresSpecialPermissions;
            this.smsManagerApiAvailable = smsManagerApiAvailable;
        }

        @Override
        public String toString() {
            return "MmsCapabilityInfo{" +
                    "strategyName='" + strategyName + '\'' +
                    ", strategyAvailable=" + strategyAvailable +
                    ", supportsGroupMms=" + supportsGroupMms +
                    ", supportsDeliveryReports=" + supportsDeliveryReports +
                    ", supportsReadReports=" + supportsReadReports +
                    ", supportsRichContent=" + supportsRichContent +
                    ", supportsLargeMessages=" + supportsLargeMessages +
                    ", requiresSpecialPermissions=" + requiresSpecialPermissions +
                    ", smsManagerApiAvailable=" + smsManagerApiAvailable +
                    '}';
        }
    }
}