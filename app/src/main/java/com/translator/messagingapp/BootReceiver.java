package com.translator.messagingapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receiver for handling device boot completed events.
 * This allows the app to initialize services after device restart.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Boot completed received");

        // Initialize any services or schedule any tasks that need to run after device boot
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "Device boot completed, initializing messaging services");

            // Check if we're the default SMS app
            if (PhoneUtils.isDefaultSmsApp(context)) {
                Log.d(TAG, "App is default SMS app, starting message monitoring service");
                
                // Start the message monitoring service for deep sleep handling
                MessageMonitoringService.startService(context);
                
                // Initialize WorkManager periodic tasks
                try {
                    MessageWorkManager workManager = new MessageWorkManager(context);
                    workManager.initializePeriodicWork();
                    Log.d(TAG, "Initialized periodic work tasks");
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing periodic work", e);
                }
            } else {
                Log.d(TAG, "App is not default SMS app, skipping service initialization");
            }
        }
    }
}


