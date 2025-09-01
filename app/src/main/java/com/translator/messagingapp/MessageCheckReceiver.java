package com.translator.messagingapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

/**
 * Receiver for periodic message checks triggered by AlarmManager.
 * Handles message synchronization during deep sleep periods.
 */
public class MessageCheckReceiver extends BroadcastReceiver {
    private static final String TAG = "MessageCheckReceiver";
    private static final String ACTION_CHECK_MESSAGES = "com.translator.messagingapp.CHECK_MESSAGES";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_CHECK_MESSAGES.equals(intent.getAction())) {
            Log.d(TAG, "Periodic message check triggered");
            
            // Acquire wake lock to ensure we can complete the check
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = null;
            
            try {
                if (powerManager != null) {
                    wakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "LinguaSMS:MessageCheck"
                    );
                    wakeLock.acquire(30000); // Hold for max 30 seconds
                }
                
                // Perform message check
                performMessageCheck(context);
                
                // Schedule next check
                MessageAlarmManager alarmManager = new MessageAlarmManager(context);
                alarmManager.scheduleNextCheck();
                
            } catch (Exception e) {
                Log.e(TAG, "Error during message check", e);
            } finally {
                // Release wake lock
                if (wakeLock != null && wakeLock.isHeld()) {
                    wakeLock.release();
                }
            }
        }
    }
    
    /**
     * Performs the actual message check and synchronization
     */
    private void performMessageCheck(Context context) {
        try {
            TranslatorApp app = (TranslatorApp) context.getApplicationContext();
            MessageService messageService = app.getMessageService();
            
            if (messageService != null) {
                // Schedule deep sleep compatible background message sync
                MessageWorkManager workManager = new MessageWorkManager(context);
                workManager.scheduleDeepSleepCompatibleSync();
                
                Log.d(TAG, "Scheduled deep sleep compatible message sync");
            } else {
                Log.w(TAG, "MessageService not available for deep sleep check");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error performing message check", e);
        }
    }
}