package com.translator.messagingapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * BroadcastReceiver that handles scheduled message alarms.
 */
public class ScheduledMessageReceiver extends BroadcastReceiver {
    private static final String TAG = "ScheduledMessageReceiver";
    private static final String ACTION_SEND_SCHEDULED_MESSAGE = "com.translator.messagingapp.SEND_SCHEDULED_MESSAGE";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (ACTION_SEND_SCHEDULED_MESSAGE.equals(action)) {
            long messageId = intent.getLongExtra("message_id", -1);
            Log.d(TAG, "Received alarm for scheduled message: " + messageId);
            
            // Process the scheduled message
            ScheduledMessageManager manager = new ScheduledMessageManager(context);
            manager.processReadyMessages();
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Log.d(TAG, "Device boot completed, rescheduling pending messages");
            
            // Reschedule all pending messages after device reboot
            ScheduledMessageManager manager = new ScheduledMessageManager(context);
            manager.rescheduleAllPendingMessages();
        } else if (Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            Log.d(TAG, "Timezone changed, rescheduling pending messages");
            
            // Reschedule all pending messages after timezone change
            ScheduledMessageManager manager = new ScheduledMessageManager(context);
            manager.rescheduleAllPendingMessages();
        } else if (Intent.ACTION_TIME_SET.equals(action)) {
            Log.d(TAG, "Time set, rescheduling pending messages");
            
            // Reschedule all pending messages after time change
            ScheduledMessageManager manager = new ScheduledMessageManager(context);
            manager.rescheduleAllPendingMessages();
        }
    }
}