package com.translator.messagingapp.message;

import com.translator.messagingapp.message.*;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Manages alarms for periodic message checking during deep sleep mode.
 * Uses setExactAndAllowWhileIdle to ensure alarms fire even in Doze mode.
 */
public class MessageAlarmManager {
    private static final String TAG = "MessageAlarmManager";
    private static final String ACTION_CHECK_MESSAGES = "com.translator.messagingapp.CHECK_MESSAGES";
    private static final int ALARM_REQUEST_CODE = 1002;
    private static final long CHECK_INTERVAL_MS = 15 * 60 * 1000; // 15 minutes
    
    private final Context context;
    private final AlarmManager alarmManager;
    
    public MessageAlarmManager(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
    
    /**
     * Schedules periodic message checks that will fire even during Doze mode
     */
    public void schedulePeriodicMessageCheck() {
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager not available");
            return;
        }
        
        Intent intent = new Intent(context, MessageCheckReceiver.class);
        intent.setAction(ACTION_CHECK_MESSAGES);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, 
            ALARM_REQUEST_CODE, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        
        long triggerTime = System.currentTimeMillis() + CHECK_INTERVAL_MS;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Use setExactAndAllowWhileIdle for API 23+ to work during Doze mode
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                );
                Log.d(TAG, "Scheduled exact alarm with Doze mode compatibility");
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // Use setExact for API 19-22
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                );
                Log.d(TAG, "Scheduled exact alarm");
            } else {
                // Fallback for older versions
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                );
                Log.d(TAG, "Scheduled regular alarm");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule alarm", e);
        }
    }
    
    /**
     * Cancels the periodic message check alarm
     */
    public void cancelPeriodicMessageCheck() {
        if (alarmManager == null) {
            return;
        }
        
        Intent intent = new Intent(context, MessageCheckReceiver.class);
        intent.setAction(ACTION_CHECK_MESSAGES);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE
        );
        
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Cancelled periodic message check alarm");
        }
    }
    
    /**
     * Schedules the next alarm after a check is completed
     */
    public void scheduleNextCheck() {
        schedulePeriodicMessageCheck();
    }
}