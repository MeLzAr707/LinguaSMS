package com.translator.messagingapp;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

/**
 * Utility class for phone-related operations like checking if the app is the default SMS app,
 * making phone calls, etc.
 */
public class PhoneUtils {
    private static final String TAG = "PhoneUtils";
    private static final String PREF_DEFAULT_SMS_REQUESTED = "default_sms_requested";
    private static final int MAX_DEFAULT_SMS_REQUESTS = 3;

    /**
     * Check if the app is the default SMS app
     */
    public static boolean isDefaultSmsApp(Context context) {
        try {
            String defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context);
            String currentPackage = context.getPackageName();
            boolean isDefault = currentPackage.equals(defaultSmsPackage);

            Log.d(TAG, "Default SMS app check: " + isDefault +
                    " (current: " + currentPackage +
                    ", default: " + (defaultSmsPackage != null ? defaultSmsPackage : "null") + ")");

            return isDefault;
        } catch (Exception e) {
            Log.e(TAG, "Error checking default SMS app status", e);
            return false;
        }
    }

    /**
     * Check if the app should request to be the default SMS app
     * based on how many times we've already asked
     */
    public static boolean shouldRequestDefaultSmsApp(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("sms_app_prefs", Context.MODE_PRIVATE);
        int requestCount = prefs.getInt(PREF_DEFAULT_SMS_REQUESTED, 0);
        return requestCount < MAX_DEFAULT_SMS_REQUESTS;
    }

    /**
     * Increment the counter for how many times we've requested to be the default SMS app
     */
    public static void incrementDefaultSmsRequestCount(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("sms_app_prefs", Context.MODE_PRIVATE);
        int currentCount = prefs.getInt(PREF_DEFAULT_SMS_REQUESTED, 0);
        prefs.edit().putInt(PREF_DEFAULT_SMS_REQUESTED, currentCount + 1).apply();
    }

    /**
     * Get the current count of how many times we've requested to be the default SMS app
     */
    public static int getDefaultSmsRequestCount(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("sms_app_prefs", Context.MODE_PRIVATE);
        return prefs.getInt(PREF_DEFAULT_SMS_REQUESTED, 0);
    }

    /**
     * Check if the app should be the default SMS app and show a dialog if needed
     *
     * @param activity The activity context
     * @param requestCode The request code to use for the activity result
     * @param callback Optional callback to run after the user makes a choice
     * @return true if a dialog was shown, false otherwise
     */
    public static boolean checkAndRequestDefaultSmsApp(Activity activity, int requestCode, Runnable callback) {
        // Only proceed if we haven't asked too many times
        if (!shouldRequestDefaultSmsApp(activity)) {
            Log.d(TAG, "Already asked to be default SMS app " + getDefaultSmsRequestCount(activity) +
                    " times, not asking again");
            return false;
        }

        if (!isDefaultSmsApp(activity)) {
            // Show a dialog asking the user to set the app as default
            new AlertDialog.Builder(activity)
                    .setTitle("Set as Default SMS App")
                    .setMessage("This app needs to be set as the default SMS app to access and send messages. Would you like to set it as the default SMS app now?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        incrementDefaultSmsRequestCount(activity);
                        requestDefaultSmsApp(activity, requestCode);
                        if (callback != null) callback.run();
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        incrementDefaultSmsRequestCount(activity);
                        Toast.makeText(activity,
                                "App functionality will be limited without default SMS permissions",
                                Toast.LENGTH_LONG).show();
                        if (callback != null) callback.run();
                    })
                    .setCancelable(false)
                    .show();
            return true;
        }
        return false;
    }

    /**
     * Show a dialog explaining why the app needs to be the default SMS app
     *
     * @param activity The activity context
     * @param requestCode The request code to use for the activity result
     * @return true if a dialog was shown, false otherwise
     */
    public static boolean showDefaultSmsExplanationDialog(Activity activity, int requestCode) {
        // Only show if we haven't asked too many times
        if (!shouldRequestDefaultSmsApp(activity)) {
            return false;
        }

        new AlertDialog.Builder(activity)
                .setTitle("Default SMS App Required")
                .setMessage("This app needs to be set as the default SMS app to function properly. Would you like to try again?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    incrementDefaultSmsRequestCount(activity);
                    requestDefaultSmsApp(activity, requestCode);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    incrementDefaultSmsRequestCount(activity);
                    Toast.makeText(activity, "Some features will be limited", Toast.LENGTH_LONG).show();
                })
                .setCancelable(false)
                .show();
        return true;
    }

    /**
     * Request to be the default SMS app using the most appropriate API for the device's Android version
     *
     * @param activity The activity that will receive the result
     * @param requestCode The request code to use for the activity result
     * @return true if the request was initiated successfully
     */
    public static boolean requestDefaultSmsApp(Activity activity, int requestCode) {
        try {
            Log.d(TAG, "Requesting default SMS app role");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 (Q) and above, use the RoleManager API
                return requestSmsRoleQ(activity, requestCode);
            } else {
                // For Android 9 (Pie) and below, use the legacy approach
                return requestSmsRoleLegacy(activity, requestCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error requesting default SMS app status", e);
            Toast.makeText(activity, "Error requesting SMS permissions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Request SMS role using the modern RoleManager API (Android 10+)
     */
    private static boolean requestSmsRoleQ(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = activity.getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                    Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS);
                    activity.startActivityForResult(intent, requestCode);
                    Log.d(TAG, "Started role request using RoleManager");
                    return true;
                } else {
                    Log.d(TAG, "Role is already held by this app");
                    return true;
                }
            } else {
                Log.e(TAG, "RoleManager is null or SMS role is not available");
                return requestSmsRoleLegacy(activity, requestCode);
            }
        }
        return false;
    }

    /**
     * Request SMS role using the legacy approach (pre-Android 10)
     */
    private static boolean requestSmsRoleLegacy(Activity activity, int requestCode) {
        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.getPackageName());
        activity.startActivityForResult(intent, requestCode);
        Log.d(TAG, "Started legacy default SMS app request");
        return true;
    }

    /**
     * Try a more direct approach to set as default SMS app
     * This is a last resort for stubborn devices
     */
    public static void tryDirectDefaultSmsAppSetting(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+, try to use the RoleManager directly
                RoleManager roleManager = activity.getSystemService(RoleManager.class);
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS);
                    activity.startActivity(intent); // Not startActivityForResult - we're just forcing it
                    Log.d(TAG, "Attempted direct role request via RoleManager");
                }
            } else {
                // For older versions, use the legacy approach
                Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.getPackageName());
                activity.startActivity(intent); // Not startActivityForResult - we're just forcing it
                Log.d(TAG, "Attempted direct default SMS app setting via legacy method");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in tryDirectDefaultSmsAppSetting", e);
        }
    }

    /**
     * Force the system to recognize this app as the default SMS app
     * This is a workaround for devices where the system doesn't immediately update
     */
    public static void forceDefaultSmsAppUpdate(Context context) {
        try {
            // Try multiple approaches to force the system to recognize our app

            // Approach 1: Write to SMS provider
            try {
                ContentValues values = new ContentValues();
                values.put("force_sms_default", 1);
                context.getContentResolver().insert(Uri.parse("content://sms/force_default"), values);
                Log.d(TAG, "Attempted force update via SMS provider");
            } catch (Exception e) {
                Log.e(TAG, "Error with SMS provider approach", e);
            }

            // Approach 2: Send a broadcast to notify system of default SMS app change
            try {
                Intent intent = new Intent("android.provider.Telephony.SMS_DEFAULT_CHANGED");
                intent.putExtra("package_name", context.getPackageName());
                context.sendBroadcast(intent);
                Log.d(TAG, "Sent SMS_DEFAULT_CHANGED broadcast");
            } catch (Exception e) {
                Log.e(TAG, "Error sending broadcast", e);
            }

            // Approach 3: Try to write a dummy SMS to force permissions update
            if (isDefaultSmsApp(context)) {
                try {
                    ContentValues dummyValues = new ContentValues();
                    dummyValues.put("address", "0000000000");
                    dummyValues.put("body", "Default SMS app test");
                    dummyValues.put("type", 3); // Draft type
                    Uri uri = context.getContentResolver().insert(Uri.parse("content://sms/draft"), dummyValues);
                    if (uri != null) {
                        context.getContentResolver().delete(uri, null, null);
                        Log.d(TAG, "Created and deleted dummy SMS draft");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error with dummy SMS approach", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error forcing default SMS app update", e);
        }
    }

    /**
     * Handle the result of a default SMS app request
     *
     * @param activity The activity that received the result
     * @param requestCode The request code that was used
     * @param resultCode The result code
     * @param successCallback Callback to run if the app is successfully set as default
     * @param failureCallback Callback to run if the app is not set as default
     */
    public static void handleDefaultSmsAppResult(Activity activity, int requestCode, int resultCode,
                                                 int expectedRequestCode, Runnable successCallback,
                                                 Runnable failureCallback) {
        if (requestCode != expectedRequestCode) {
            return;
        }

        Log.d(TAG, "SMS request result: " + resultCode);

        // Give the system time to process the change
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isDefaultSmsApp(activity)) {
                Log.d(TAG, "Successfully set as default SMS app");
                if (successCallback != null) {
                    successCallback.run();
                }
            } else {
                Log.d(TAG, "Not set as default SMS app yet");

                // If the result was OK but we're still not the default, try to force it
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "Result was OK but app is not default yet, trying to force update");
                    forceDefaultSmsAppUpdate(activity);

                    // Check again after a short delay
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (isDefaultSmsApp(activity)) {
                            Log.d(TAG, "Force update successful");
                            if (successCallback != null) {
                                successCallback.run();
                            }
                        } else {
                            Log.d(TAG, "Force update failed, trying direct setting");
                            tryDirectDefaultSmsAppSetting(activity);

                            if (failureCallback != null) {
                                failureCallback.run();
                            }
                        }
                    }, 1000);
                } else if (shouldRequestDefaultSmsApp(activity)) {
                    // User declined but we haven't asked too many times
                    showDefaultSmsExplanationDialog(activity, expectedRequestCode);
                } else if (failureCallback != null) {
                    // We've asked too many times
                    failureCallback.run();
                }
            }
        }, 1500);
    }

    /**
     * Check if the device has telephony features
     */
    public static boolean hasTelephonyFeature(Context context) {
        PackageManager packageManager = context.getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    /**
     * Make a phone call to the given number
     */
    public static void callPhoneNumber(Activity activity, String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            activity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error making phone call", e);
            Toast.makeText(activity, "Error making call: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}



