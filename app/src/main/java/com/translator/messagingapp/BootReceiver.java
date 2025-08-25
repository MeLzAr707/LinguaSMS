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
                // Start any necessary services
                // For example, you might want to start a service to check for new messages
                // or initialize your message database

                // Example: Start a background service
                // Intent serviceIntent = new Intent(context, MessageSyncService.class);
                // context.startService(serviceIntent);
            }

            // Reschedule pending messages after device reboot
            try {
                ScheduledMessageManager scheduledMessageManager = new ScheduledMessageManager(context);
                scheduledMessageManager.rescheduleAllPendingMessages();
                Log.d(TAG, "Rescheduled pending messages after boot");
            } catch (Exception e) {
                Log.e(TAG, "Error rescheduling pending messages after boot", e);
            }
        }
    }
}


