package com.translator.messagingapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receiver for handling scheduled message events and system time changes.
 * This receiver handles time-related events that may affect scheduled messages.
 */
public class ScheduledMessageReceiver extends BroadcastReceiver {
    private static final String TAG = "ScheduledMessageReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);

        if (action != null) {
            switch (action) {
                case Intent.ACTION_TIME_CHANGED: // Fixed: was ACTION_TIME_SET
                    handleTimeChanged(context);
                    break;
                case Intent.ACTION_TIMEZONE_CHANGED:
                    handleTimezoneChanged(context);
                    break;
                case Intent.ACTION_DATE_CHANGED:
                    handleDateChanged(context);
                    break;
                default:
                    Log.d(TAG, "Unhandled action: " + action);
                    break;
            }
        }
    }

    /**
     * Handles system time changes.
     * This may affect scheduled message delivery times.
     */
    private void handleTimeChanged(Context context) {
        Log.d(TAG, "System time changed");
        
        try {
            ScheduledMessageManager manager = new ScheduledMessageManager();
            manager.rescheduleAllPendingMessages();
            Log.d(TAG, "Rescheduled pending messages after time change");
        } catch (Exception e) {
            Log.e(TAG, "Error rescheduling messages after time change", e);
        }
    }

    /**
     * Handles timezone changes.
     * This may affect scheduled message delivery times.
     */
    private void handleTimezoneChanged(Context context) {
        Log.d(TAG, "Timezone changed");
        
        try {
            ScheduledMessageManager manager = new ScheduledMessageManager();
            manager.rescheduleAllPendingMessages();
            Log.d(TAG, "Rescheduled pending messages after timezone change");
        } catch (Exception e) {
            Log.e(TAG, "Error rescheduling messages after timezone change", e);
        }
    }

    /**
     * Handles date changes.
     * This may affect scheduled message delivery times.
     */
    private void handleDateChanged(Context context) {
        Log.d(TAG, "Date changed");
        
        try {
            ScheduledMessageManager manager = new ScheduledMessageManager();
            manager.rescheduleAllPendingMessages();
            Log.d(TAG, "Rescheduled pending messages after date change");
        } catch (Exception e) {
            Log.e(TAG, "Error rescheduling messages after date change", e);
        }
    }
}