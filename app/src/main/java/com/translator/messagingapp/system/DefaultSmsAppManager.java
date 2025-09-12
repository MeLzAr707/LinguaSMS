package com.translator.messagingapp.system;

import com.translator.messagingapp.system.*;

import com.translator.messagingapp.message.*;
import com.translator.messagingapp.util.PhoneUtils;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.util.Log;
import android.widget.Toast;

/**
 * Simplified manager for handling default SMS app functionality.
 */
public class DefaultSmsAppManager {
    private static final String TAG = "DefaultSmsAppManager";
    private final Context context;

    /**
     * Creates a new DefaultSmsAppManager.
     *
     * @param context The application context
     */
    public DefaultSmsAppManager(Context context) {
        this.context = context;
    }

    /**
     * Check if the app is the default SMS app.
     *
     * @return true if the app is the default SMS app, false otherwise
     */
    public boolean isDefaultSmsApp() {
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
     * Request to be the default SMS app using the most appropriate API for the device's Android version.
     *
     * @param activity The activity that will receive the result
     * @param requestCode The request code to use for the activity result
     */
    public void requestDefaultSmsApp(Activity activity, int requestCode) {
        try {
            Log.d(TAG, "Requesting default SMS app role");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 (Q) and above, use the RoleManager API
                RoleManager roleManager = (RoleManager) activity.getSystemService(Context.ROLE_SERVICE);
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    Intent roleRequestIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS);
                    activity.startActivityForResult(roleRequestIntent, requestCode);
                    Log.d(TAG, "Started role request using RoleManager");
                    return;
                }
            }

            // For older versions or if RoleManager failed
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.getPackageName());
            activity.startActivityForResult(intent, requestCode);
            Log.d(TAG, "Started legacy default SMS app request");

        } catch (Exception e) {
            Log.e(TAG, "Error requesting default SMS app status", e);
            Toast.makeText(activity, "Error requesting SMS permissions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Check if the app should be the default SMS app and request if needed.
     *
     * @param activity The activity context
     * @param requestCode The request code to use for the activity result
     */
    public void checkAndRequestDefaultSmsApp(Activity activity, int requestCode) {
        if (!isDefaultSmsApp()) {
            // Check if we should request based on user preferences and request count
            if (PhoneUtils.shouldRequestDefaultSmsApp(context)) {
                Toast.makeText(activity,
                        "This app needs to be set as the default SMS app to show conversations",
                        Toast.LENGTH_LONG).show();
                
                // Increment the request count when we actually make the request
                PhoneUtils.incrementDefaultSmsRequestCount(context);
                requestDefaultSmsApp(activity, requestCode);
            } else {
                Log.d(TAG, "Not requesting default SMS app - user preferences or limit reached");
            }
        }
    }
}

