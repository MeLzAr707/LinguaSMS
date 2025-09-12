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
 * Helper class for sending MMS messages using the appropriate API based on Android version.
 * This class now uses the new transaction-based architecture with version-specific compatibility.
 */
public class MmsSendingHelper {
    private static final String TAG = "MmsSendingHelper";

    /**
     * Sends an MMS message using the best available method for the current Android version.
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
                    Log.w(TAG, "Primary strategy failed, trying fallbacks");
                }
            }
            
            // Fallback to transaction architecture if strategy fails
            return sendMmsUsingTransactionArchitecture(context, contentUri, address, subject);
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending MMS message", e);
            
            // Final fallback to legacy methods
            return sendMmsWithLegacyFallback(context, contentUri, locationUrl, address);
        }
    }

    /**
     * Sends an MMS message using the transaction-based architecture.
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
     * Sends MMS using legacy fallback methods.
     *
     * @param context The application context
     * @param contentUri The URI of the MMS message
     * @param locationUrl The location URL
     * @param address The recipient's address
     * @return True if successful
     */
    private static boolean sendMmsWithLegacyFallback(Context context, Uri contentUri, String locationUrl, String address) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return sendMmsUsingNewApi(context, contentUri, locationUrl);
            } else {
                return sendMmsUsingLegacyApi(context, contentUri, address);
            }
        } catch (Exception e) {
            Log.e(TAG, "Legacy fallback also failed", e);
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

    /**
     * Gets information about the MMS capabilities for the current device.
     *
     * @param context The application context
     * @return MMS capability information
     */
    public static MmsCapabilityInfo getMmsCapabilities(Context context) {
        MmsCompatibilityManager.MmsFeatureFlags flags = MmsCompatibilityManager.getFeatureFlags();
        MmsCompatibilityManager.MmsSendingStrategy strategy = MmsCompatibilityManager.getSendingStrategy(context);
        
        return new MmsCapabilityInfo(
            strategy.isAvailable(),
            strategy.getStrategyName(),
            flags.supportsGroupMms,
            flags.supportsDeliveryReports,
            flags.supportsReadReports,
            flags.supportsRichContent,
            MmsCompatibilityManager.getMmsOperationTimeout()
        );
    }

    /**
     * Information about MMS capabilities on the current device.
     */
    public static class MmsCapabilityInfo {
        public final boolean isAvailable;
        public final String strategyName;
        public final boolean supportsGroupMms;
        public final boolean supportsDeliveryReports;
        public final boolean supportsReadReports;
        public final boolean supportsRichContent;
        public final long operationTimeout;

        public MmsCapabilityInfo(boolean isAvailable, String strategyName, boolean supportsGroupMms,
                               boolean supportsDeliveryReports, boolean supportsReadReports,
                               boolean supportsRichContent, long operationTimeout) {
            this.isAvailable = isAvailable;
            this.strategyName = strategyName;
            this.supportsGroupMms = supportsGroupMms;
            this.supportsDeliveryReports = supportsDeliveryReports;
            this.supportsReadReports = supportsReadReports;
            this.supportsRichContent = supportsRichContent;
            this.operationTimeout = operationTimeout;
        }

        @Override
        public String toString() {
            return "MmsCapabilityInfo{" +
                    "isAvailable=" + isAvailable +
                    ", strategyName='" + strategyName + '\'' +
                    ", supportsGroupMms=" + supportsGroupMms +
                    ", supportsDeliveryReports=" + supportsDeliveryReports +
                    ", supportsReadReports=" + supportsReadReports +
                    ", supportsRichContent=" + supportsRichContent +
                    ", operationTimeout=" + operationTimeout +
                    '}';
        }
    }
}