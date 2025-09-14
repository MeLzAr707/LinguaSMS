package com.translator.messagingapp.mms.compat;

import com.translator.messagingapp.message.*;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import com.translator.messagingapp.mms.MmsMessageSender;
import com.translator.messagingapp.mms.Transaction;
import com.translator.messagingapp.mms.TransactionService;

/**
 * Provides Android version-specific compatibility for MMS operations.
 * Handles different APIs and capabilities across Android versions.
 */
public class MmsCompatibilityManager {
    private static final String TAG = "MmsCompatibilityManager";

    // Android version constants (API 24+ only)
    public static final int NOUGAT = Build.VERSION_CODES.N;  // API 24 - minimum supported
    public static final int OREO = Build.VERSION_CODES.O;

    /**
     * Gets the best MMS sending strategy for the current Android version.
     * Since minimum SDK is 24 (Android N), only modern API strategies are supported.
     *
     * @param context The application context
     * @return The appropriate sending strategy
     */
    public static MmsSendingStrategy getSendingStrategy(Context context) {
        int sdkVersion = Build.VERSION.SDK_INT;
        
        // Since minSdkVersion is 24, we only support Android N+ strategies
        if (sdkVersion >= OREO) {
            // Android 8.0+ - Use transaction architecture with Oreo optimizations
            return new OreoAndAboveSendingStrategy(context);
        } else {
            // Android 7.0-7.1 - Use transaction architecture with Nougat support
            return new NougatSendingStrategy(context);
        }
    }

    /**
     * Gets the best MMS receiving strategy for the current Android version.
     * Since minimum SDK is 24 (Android N), only modern API strategies are supported.
     *
     * @param context The application context
     * @return The appropriate receiving strategy
     */
    public static MmsReceivingStrategy getReceivingStrategy(Context context) {
        int sdkVersion = Build.VERSION.SDK_INT;
        
        // Since minSdkVersion is 24, we only support Android N+ strategies
        if (sdkVersion >= OREO) {
            return new OreoAndAboveReceivingStrategy(context);
        } else {
            return new NougatReceivingStrategy(context);
        }
    }

    /**
     * Checks if the transaction-based architecture is supported.
     *
     * @return True if transaction architecture is supported
     */
    public static boolean isTransactionArchitectureSupported() {
        // Transaction architecture is supported on all versions but works best on newer ones
        return true;
    }

    /**
     * Checks if the official SmsManager MMS API is available.
     * Since minimum SDK is 24, this is always true.
     *
     * @return Always true for API 24+
     */
    public static boolean isSmsManagerMmsApiAvailable() {
        return true; // Always available since minSdkVersion is 24 (> Lollipop=21)
    }

    /**
     * Checks if reflection-based access is needed for MMS operations.
     * Since minimum SDK is 24, this is always false.
     *
     * @return Always false for API 24+
     */
    public static boolean needsReflectionAccess() {
        return false; // Never needed since minSdkVersion is 24 (> Lollipop=21)
    }

    /**
     * Gets the recommended timeout for MMS operations based on Android version.
     *
     * @return Timeout in milliseconds
     */
    public static long getMmsOperationTimeout() {
        if (Build.VERSION.SDK_INT >= OREO) {
            return 90000; // 90 seconds for newer versions
        } else {
            return 120000; // 2 minutes for Nougat
        }
    }

    /**
     * Gets version-specific feature flags.
     *
     * @return Feature flags for the current Android version
     */
    public static MmsFeatureFlags getFeatureFlags() {
        return new MmsFeatureFlags();
    }

    /**
     * Interface for MMS sending strategies.
     */
    public interface MmsSendingStrategy {
        boolean sendMms(Uri messageUri, String address, String subject);
        boolean isAvailable();
        String getStrategyName();
    }

    /**
     * Interface for MMS receiving strategies.
     */
    public interface MmsReceivingStrategy {
        boolean handleMmsNotification(byte[] pduData);
        boolean isAutoDownloadSupported();
        String getStrategyName();
    }

    /**
     * Feature flags for different Android versions.
     * Since minimum SDK is 24, all modern features are supported.
     */
    public static class MmsFeatureFlags {
        public final boolean supportsGroupMms;
        public final boolean supportsDeliveryReports;
        public final boolean supportsReadReports;
        public final boolean supportsRichContent;
        public final boolean supportsLargeMessages;
        public final boolean requiresSpecialPermissions;

        public MmsFeatureFlags() {
            // All features are supported on API 24+
            supportsGroupMms = true;
            supportsDeliveryReports = true;
            supportsReadReports = true;
            supportsRichContent = true;
            supportsLargeMessages = true; // Always true since minSdkVersion 24 > KitKat
            requiresSpecialPermissions = true; // Always true since minSdkVersion 24 > Marshmallow
        }
    }

    /**
     * Android 8.0+ sending strategy.
     */
    private static class OreoAndAboveSendingStrategy implements MmsSendingStrategy {
        private final Context mContext;

        public OreoAndAboveSendingStrategy(Context context) {
            mContext = context;
        }

        @Override
        public boolean sendMms(Uri messageUri, String address, String subject) {
            try {
                // Use transaction architecture optimized for Oreo+
                MmsMessageSender sender = MmsMessageSender.create(mContext, messageUri);
                boolean success = sender.sendMessage(System.currentTimeMillis());
                
                if (!success) {
                    // Fallback: Use SmsManager API (always available on API 24+)
                    Log.d(TAG, "Transaction send failed, trying SmsManager API");
                    return sendWithSmsManager(messageUri);
                }
                
                return success;
            } catch (Exception e) {
                Log.e(TAG, "Error in Oreo+ sending strategy", e);
                return false;
            }
        }

        private boolean sendWithSmsManager(Uri messageUri) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                
                // Create PendingIntent for send result  
                android.content.Intent sentIntent = new android.content.Intent("com.translator.messagingapp.MMS_SENT");
                sentIntent.putExtra("message_uri", messageUri.toString());
                
                android.app.PendingIntent sentPendingIntent = android.app.PendingIntent.getBroadcast(
                    mContext,
                    (int) System.currentTimeMillis(), // Unique request code
                    sentIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
                );
                
                smsManager.sendMultimediaMessage(mContext, messageUri, null, null, sentPendingIntent);
                Log.d(TAG, "MMS sent via SmsManager with PendingIntent");
                return true;
            } catch (Exception e) {
                Log.e(TAG, "SmsManager send failed", e);
                return false;
            }
        }

        @Override
        public boolean isAvailable() {
            return MmsMessageSender.isMmsSendingAvailable(mContext);
        }

        @Override
        public String getStrategyName() {
            return "OreoAndAbove";
        }
    }

    /**
     * Android 7.0-7.1 sending strategy.
     */
    private static class NougatSendingStrategy implements MmsSendingStrategy {
        private final Context mContext;

        public NougatSendingStrategy(Context context) {
            mContext = context;
        }

        @Override
        public boolean sendMms(Uri messageUri, String address, String subject) {
            try {
                // Use transaction architecture with SmsManager API fallback
                MmsMessageSender sender = MmsMessageSender.create(mContext, messageUri);
                boolean success = sender.sendMessage(System.currentTimeMillis());
                
                if (!success) {
                    // Fallback: Use SmsManager API (always available on API 24+)
                    Log.d(TAG, "Transaction send failed, trying SmsManager API");
                    return sendWithSmsManager(messageUri);
                }
                
                return success;
            } catch (Exception e) {
                Log.e(TAG, "Error in Nougat sending strategy", e);
                return false;
            }
        }

        private boolean sendWithSmsManager(Uri messageUri) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                
                // Create PendingIntent for send result  
                android.content.Intent sentIntent = new android.content.Intent("com.translator.messagingapp.MMS_SENT");
                sentIntent.putExtra("message_uri", messageUri.toString());
                
                android.app.PendingIntent sentPendingIntent = android.app.PendingIntent.getBroadcast(
                    mContext,
                    (int) System.currentTimeMillis(), // Unique request code
                    sentIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
                );
                
                smsManager.sendMultimediaMessage(mContext, messageUri, null, null, sentPendingIntent);
                Log.d(TAG, "MMS sent via SmsManager with PendingIntent");
                return true;
            } catch (Exception e) {
                Log.e(TAG, "SmsManager send failed", e);
                return false;
            }
        }

        @Override
        public boolean isAvailable() {
            return MmsMessageSender.isMmsSendingAvailable(mContext);
        }

        @Override
        public String getStrategyName() {
            return "Nougat";
        }
    }

    /**
     * Android 8.0+ receiving strategy.
     */
    private static class OreoAndAboveReceivingStrategy implements MmsReceivingStrategy {
        private final Context mContext;

        public OreoAndAboveReceivingStrategy(Context context) {
            mContext = context;
        }

        @Override
        public boolean handleMmsNotification(byte[] pduData) {
            try {
                // Use transaction architecture for notification handling
                return startNotificationTransaction(pduData);
            } catch (Exception e) {
                Log.e(TAG, "Error in Oreo+ receiving strategy", e);
                return false;
            }
        }

        private boolean startNotificationTransaction(byte[] pduData) {
            // Create notification entry and start transaction
            // This would integrate with the TransactionService
            return true; // Placeholder
        }

        @Override
        public boolean isAutoDownloadSupported() {
            return true;
        }

        @Override
        public String getStrategyName() {
            return "OreoAndAbove";
        }
    }

    /**
     * Android 7.0-7.1 receiving strategy.
     */
    private static class NougatReceivingStrategy implements MmsReceivingStrategy {
        private final Context mContext;

        public NougatReceivingStrategy(Context context) {
            mContext = context;
        }

        @Override
        public boolean handleMmsNotification(byte[] pduData) {
            try {
                // Use transaction architecture for notification handling
                return startNotificationTransaction(pduData);
            } catch (Exception e) {
                Log.e(TAG, "Error in Nougat receiving strategy", e);
                return false;
            }
        }

        private boolean startNotificationTransaction(byte[] pduData) {
            // Create notification entry and start transaction
            // This would integrate with the TransactionService
            return true; // Placeholder
        }

        @Override
        public boolean isAutoDownloadSupported() {
            return true;
        }

        @Override
        public String getStrategyName() {
            return "Nougat";
        }
    }
}