package com.translator.messagingapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Telephony;
import android.util.Log;

/**
 * Manager for handling default SMS app functionality.
 * FIXED VERSION: Updated to use static methods and fix method signatures
 */
public class DefaultSmsAppManager {
    private static final String TAG = "DefaultSmsAppManager";
    private static final String PREFS_NAME = "default_sms_prefs";
    private static final String KEY_REQUEST_COUNT = "request_count";
    private static final int MAX_REQUESTS = 3;

    // Added state tracking to prevent multiple prompts
    private static boolean isPromptShowing = false;

    // Context reference
    private Context context;

    /**
     * Constructor with context.
     *
     * @param context The application context
     */
    public DefaultSmsAppManager(Context context) {
        this.context = context;
    }

    /**
     * Checks if this app is the default SMS app.
     *
     * @return True if this app is the default SMS app, false otherwise
     */
    public boolean isDefaultSmsApp() {
        if (context == null) {
            return false;
        }

        return isDefaultSmsApp(context);
    }

    /**
     * Checks if this app is the default SMS app.
     *
     * @param context The context
     * @return True if this app is the default SMS app, false otherwise
     */
    public static boolean isDefaultSmsApp(Context context) {
        if (context == null) {
            return false;
        }

        try {
            String defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context);
            String appPackage = context.getPackageName();

            Log.d(TAG, "Default SMS package: " + defaultSmsPackage);
            Log.d(TAG, "App package: " + appPackage);

            return appPackage.equals(defaultSmsPackage);
        } catch (Exception e) {
            Log.e(TAG, "Error checking default SMS app", e);
            return false;
        }
    }

    /**
     * Checks if we should request to be the default SMS app.
     * This is based on how many times we've already asked.
     *
     * @return True if we should request, false otherwise
     */
    public boolean shouldRequestDefaultSmsApp() {
        if (context == null) {
            return false;
        }

        return shouldRequestDefaultSmsApp(context);
    }

    /**
     * Checks if we should request to be the default SMS app.
     * This is based on how many times we've already asked.
     *
     * @param context The context
     * @return True if we should request, false otherwise
     */
    public static boolean shouldRequestDefaultSmsApp(Context context) {
        if (isDefaultSmsApp(context)) {
            return false;
        }

        int requestCount = getDefaultSmsRequestCount(context);
        return requestCount < MAX_REQUESTS;
    }

    /**
     * Increments the count of how many times we've requested to be the default SMS app.
     */
    public void incrementDefaultSmsRequestCount() {
        if (context == null) {
            return;
        }

        incrementDefaultSmsRequestCount(context);
    }

    /**
     * Increments the count of how many times we've requested to be the default SMS app.
     *
     * @param context The context
     */
    public static void incrementDefaultSmsRequestCount(Context context) {
        if (context == null) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int count = prefs.getInt(KEY_REQUEST_COUNT, 0);
        prefs.edit().putInt(KEY_REQUEST_COUNT, count + 1).apply();

        Log.d(TAG, "Incremented default SMS request count to " + (count + 1));
    }

    /**
     * Gets the count of how many times we've requested to be the default SMS app.
     *
     * @return The request count
     */
    public int getDefaultSmsRequestCount() {
        if (context == null) {
            return 0;
        }

        return getDefaultSmsRequestCount(context);
    }

    /**
     * Gets the count of how many times we've requested to be the default SMS app.
     *
     * @param context The context
     * @return The request count
     */
    public static int getDefaultSmsRequestCount(Context context) {
        if (context == null) {
            return 0;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_REQUEST_COUNT, 0);
    }

    /**
     * Checks if we should request to be the default SMS app, and if so, shows a dialog.
     * If the user agrees, we'll request to be the default SMS app.
     *
     * @param activity The activity
     * @param requestCode The request code for the result
     * @return True if we showed the dialog, false otherwise
     */
    public boolean checkAndRequestDefaultSmsApp(Activity activity, int requestCode) {
        return checkAndRequestDefaultSmsApp(activity, requestCode, null);
    }

    /**
     * Checks if we should request to be the default SMS app, and if so, shows a dialog.
     * If the user agrees, we'll request to be the default SMS app.
     *
     * @param activity The activity
     * @param requestCode The request code for the result
     * @param callback Callback to run if the user agrees
     * @return True if we showed the dialog, false otherwise
     */


    /**
     * Checks if we should request to be the default SMS app, and if so, shows a dialog.
     * If the user agrees, we'll request to be the default SMS app.
     *
     * @param activity The activity
     * @param requestCode The request code for the result
     * @param callback Callback to run if the user agrees
     * @return True if we showed the dialog, false otherwise
     */
    public static boolean checkAndRequestDefaultSmsApp(Activity activity, int requestCode, Runnable callback) {
        if (activity == null) {
            return false;
        }

        // Check if we're already the default SMS app
        if (isDefaultSmsApp(activity)) {
            Log.d(TAG, "Already default SMS app");
            if (callback != null) {
                callback.run();
            }
            return false;
        }

        // Check if we should request
        if (!shouldRequestDefaultSmsApp(activity)) {
            Log.d(TAG, "Not requesting default SMS app (reached max requests)");
            return false;
        }

        // Check if a prompt is already showing
        if (isPromptShowing) {
            Log.d(TAG, "Default SMS app prompt already showing, skipping");
            return false;
        }

        // Show explanation dialog
        return showDefaultSmsExplanationDialog(activity, requestCode);
    }

    /**
     * Shows a dialog explaining why we need to be the default SMS app.
     *
     * @param activity The activity
     * @param requestCode The request code for the result
     * @return True if we showed the dialog, false otherwise
     */
    public static boolean showDefaultSmsExplanationDialog(Activity activity, int requestCode) {
        if (activity == null) {
            return false;
        }

        // Check if a prompt is already showing
        if (isPromptShowing) {
            Log.d(TAG, "Default SMS app prompt already showing, skipping");
            return false;
        }

        try {
            isPromptShowing = true;

            AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                    .setTitle(R.string.default_sms_title)
                    .setMessage(R.string.default_sms_message)
                    .setPositiveButton(R.string.set_default, (dialog, which) -> {
                        requestDefaultSmsApp(activity, requestCode);
                    })
                    .setNegativeButton(R.string.not_now, (dialog, which) -> {
                        isPromptShowing = false;
                        incrementDefaultSmsRequestCount(activity);
                    })
                    .setOnCancelListener(dialog -> {
                        isPromptShowing = false;
                        incrementDefaultSmsRequestCount(activity);
                    });

            builder.show();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error showing default SMS explanation dialog", e);
            isPromptShowing = false;
            return false;
        }
    }

    /**
     * Requests to be the default SMS app.
     *
     * @param activity The activity
     * @param requestCode The request code for the result
     * @return True if we made the request, false otherwise
     */
    public static boolean requestDefaultSmsApp(Activity activity, int requestCode) {
        if (activity == null) {
            return false;
        }

        try {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.getPackageName());
            activity.startActivityForResult(intent, requestCode);

            Log.d(TAG, "Requested default SMS app");
            incrementDefaultSmsRequestCount(activity);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error requesting default SMS app", e);
            isPromptShowing = false;
            return false;
        }
    }

    /**
     * Tries to directly open the default SMS app settings.
     *
     * @param activity The activity
     */
    public static void tryDirectDefaultSmsAppSetting(Activity activity) {
        if (activity == null) {
            return;
        }

        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
            activity.startActivity(intent);

            Log.d(TAG, "Opened default apps settings");
        } catch (Exception e) {
            Log.e(TAG, "Error opening default apps settings", e);

            // Try alternative method
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                activity.startActivity(intent);

                Log.d(TAG, "Opened settings");
            } catch (Exception e2) {
                Log.e(TAG, "Error opening settings", e2);
            }
        }
    }

    /**
     * Forces an update of the default SMS app status.
     */
    public void forceDefaultSmsAppUpdate() {
        if (context == null) {
            return;
        }

        forceDefaultSmsAppUpdate(context);
    }

    /**
     * Forces an update of the default SMS app status.
     * This is useful after the user has changed the default SMS app.
     *
     * @param context The context
     */
    public static void forceDefaultSmsAppUpdate(Context context) {
        if (context == null) {
            return;
        }

        try {
            // This is a no-op, but it forces the system to update the default SMS app status
            String defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context);
            Log.d(TAG, "Forced default SMS app update: " + defaultSmsPackage);
        } catch (Exception e) {
            Log.e(TAG, "Error forcing default SMS app update", e);
        }
    }

    /**
     * Handles the result of the default SMS app request.
     *
     * @param activity The activity
     * @param requestCode The request code
     * @param resultCode The result code
     */
    public static void handleDefaultSmsAppResult(Activity activity, int requestCode, int resultCode) {
        // Reset prompt state
        isPromptShowing = false;

        if (activity == null) {
            return;
        }

        Log.d(TAG, "Default SMS app result: " + resultCode);

        // Force update
        forceDefaultSmsAppUpdate(activity);
    }

    /**
     * Checks if the device has telephony features.
     *
     * @return True if the device has telephony features, false otherwise
     */
    public boolean hasTelephonyFeature() {
        if (context == null) {
            return false;
        }

        return hasTelephonyFeature(context);
    }

    /**
     * Checks if the device has telephony features.
     *
     * @param context The context
     * @return True if the device has telephony features, false otherwise
     */
    public static boolean hasTelephonyFeature(Context context) {
        if (context == null) {
            return false;
        }

        return context.getPackageManager().hasSystemFeature("android.hardware.telephony");
    }

    /**
     * Calls a phone number.
     *
     * @param activity The activity
     * @param phoneNumber The phone number to call
     */
    public static void callPhoneNumber(Activity activity, String phoneNumber) {
        if (activity == null || phoneNumber == null) {
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(android.net.Uri.parse("tel:" + phoneNumber));
            activity.startActivity(intent);

            Log.d(TAG, "Called phone number: " + phoneNumber);
        } catch (Exception e) {
            Log.e(TAG, "Error calling phone number: " + phoneNumber, e);
        }
    }
}



