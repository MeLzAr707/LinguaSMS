package com.translator.messagingapp.util;

import com.translator.messagingapp.message.*;

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
import android.os.PowerManager;
import android.provider.Settings;
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
    private static final String PREF_USER_DECLINED_SMS = "user_declined_sms";
    private static final String PREF_BATTERY_OPTIMIZATION_REQUESTED = "battery_optimization_requested";
    private static final String PREF_USER_DECLINED_BATTERY_OPT = "user_declined_battery_opt";
    private static final int MAX_DEFAULT_SMS_REQUESTS = 3;
    private static final int MAX_BATTERY_OPT_REQUESTS = 3;

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
     * based on how many times we've already asked and if user has declined
     */
    public static boolean shouldRequestDefaultSmsApp(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("sms_app_prefs", Context.MODE_PRIVATE);
        
        // Don't ask if user has permanently declined
        if (prefs.getBoolean(PREF_USER_DECLINED_SMS, false)) {
            Log.d(TAG, "User has permanently declined default SMS app, not asking again");
            return false;
        }
        
        // Don't ask if we've already asked too many times
        int requestCount = prefs.getInt(PREF_DEFAULT_SMS_REQUESTED, 0);
        boolean shouldRequest = requestCount < MAX_DEFAULT_SMS_REQUESTS;
        Log.d(TAG, "Should request default SMS app: " + shouldRequest + " (count: " + requestCount + "/" + MAX_DEFAULT_SMS_REQUESTS + ")");
        return shouldRequest;
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
     * Check if the user has permanently declined to set this app as default SMS app
     */
    public static boolean hasUserDeclinedDefaultSms(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("sms_app_prefs", Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_USER_DECLINED_SMS, false);
    }

    /**
     * Mark that the user has permanently declined to set this app as default SMS app
     */
    public static void setUserDeclinedDefaultSms(Context context, boolean declined) {
        SharedPreferences prefs = context.getSharedPreferences("sms_app_prefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_USER_DECLINED_SMS, declined).apply();
        Log.d(TAG, "User declined default SMS setting: " + declined);
    }

    /**
     * Reset the SMS app request preferences (for testing or user settings)
     */
    public static void resetDefaultSmsRequestPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("sms_app_prefs", Context.MODE_PRIVATE);
        prefs.edit()
                .remove(PREF_DEFAULT_SMS_REQUESTED)
                .remove(PREF_USER_DECLINED_SMS)
                .apply();
        Log.d(TAG, "Reset default SMS request preferences");
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
            Toast.makeText(activity, activity.getString(R.string.error_requesting_sms_permissions, e.getMessage()), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(activity, activity.getString(R.string.error_making_call, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    // ========== BATTERY OPTIMIZATION METHODS ==========

    /**
     * Check if the app is whitelisted from battery optimizations
     */
    public static boolean isIgnoringBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
            }
        }
        return true; // Assume true for older versions
    }

    /**
     * Check if we should request battery optimization whitelist
     */
    public static boolean shouldRequestBatteryOptimizationWhitelist(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("sms_app_prefs", Context.MODE_PRIVATE);
        
        // Don't ask if user has permanently declined
        if (prefs.getBoolean(PREF_USER_DECLINED_BATTERY_OPT, false)) {
            Log.d(TAG, "User has permanently declined battery optimization whitelist, not asking again");
            return false;
        }
        
        // Don't ask if already whitelisted
        if (isIgnoringBatteryOptimizations(context)) {
            Log.d(TAG, "App is already ignoring battery optimizations");
            return false;
        }
        
        // Don't ask if we've already asked too many times
        int requestCount = prefs.getInt(PREF_BATTERY_OPTIMIZATION_REQUESTED, 0);
        boolean shouldRequest = requestCount < MAX_BATTERY_OPT_REQUESTS;
        
        Log.d(TAG, "Should request battery optimization whitelist: " + shouldRequest + 
                  " (count: " + requestCount + "/" + MAX_BATTERY_OPT_REQUESTS + ")");
        return shouldRequest;
    }

    /**
     * Request battery optimization whitelist
     */
    public static void requestBatteryOptimizationWhitelist(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                SharedPreferences prefs = activity.getSharedPreferences("sms_app_prefs", Context.MODE_PRIVATE);
                int requestCount = prefs.getInt(PREF_BATTERY_OPTIMIZATION_REQUESTED, 0);
                prefs.edit().putInt(PREF_BATTERY_OPTIMIZATION_REQUESTED, requestCount + 1).apply();

                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(intent);
                
                Log.d(TAG, "Requested battery optimization whitelist");
            } catch (Exception e) {
                Log.e(TAG, "Error requesting battery optimization whitelist", e);
                // Fallback to general battery optimization settings
                try {
                    Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    activity.startActivity(intent);
                } catch (Exception fallbackError) {
                    Log.e(TAG, "Error opening battery optimization settings", fallbackError);
                }
            }
        }
    }

    /**
     * Mark that the user has permanently declined battery optimization whitelist
     */
    public static void setUserDeclinedBatteryOptimization(Context context, boolean declined) {
        SharedPreferences prefs = context.getSharedPreferences("sms_app_prefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_USER_DECLINED_BATTERY_OPT, declined).apply();
        Log.d(TAG, "User declined battery optimization whitelist: " + declined);
    }

    /**
     * Check if user has permanently declined battery optimization whitelist
     */
    public static boolean hasUserDeclinedBatteryOptimization(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("sms_app_prefs", Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_USER_DECLINED_BATTERY_OPT, false);
    }

    /**
     * Show dialog to request battery optimization whitelist
     */
    public static void showBatteryOptimizationDialog(Activity activity) {
        new AlertDialog.Builder(activity)
            .setTitle("Battery Optimization")
            .setMessage("To ensure reliable message reception during sleep mode, please disable battery optimization for LinguaSMS. This helps prevent messages from being lost when your phone is in deep sleep.")
            .setPositiveButton("Open Settings", (dialog, which) -> {
                requestBatteryOptimizationWhitelist(activity);
            })
            .setNegativeButton("Not Now", (dialog, which) -> {
                // Just dismiss
            })
            .setNeutralButton("Don't Ask Again", (dialog, which) -> {
                setUserDeclinedBatteryOptimization(activity, true);
            })
            .show();
    }
}



