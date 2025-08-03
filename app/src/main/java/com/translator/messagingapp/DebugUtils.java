package com.translator.messagingapp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DebugUtils {
    private static final String TAG = "DebugUtils";

    public static boolean isDebugBuild() {
        return BuildConfig.ENABLE_DEBUG_TOOLS;
    }

    public static void openDebugActivity(Context context, String address) {
        if (!isDebugBuild()) {
            return;
        }

        try {
            Intent intent = new Intent(context, DebugActivity.class);
            if (address != null) {
                intent.putExtra("address", address);
            }
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening debug activity", e);
        }
    }

    public static void logDebug(String tag, String message) {
        if (isDebugBuild()) {
            Log.d(tag, message);
        }
    }
}
