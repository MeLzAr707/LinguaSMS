package com.translator.messagingapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

/**
 * Foreground service for monitoring messages during deep sleep mode.
 * This service ensures message reception even when the device is in Doze mode.
 */
public class MessageMonitoringService extends Service {
    private static final String TAG = "MessageMonitoringService";
    private static final String CHANNEL_ID = "MESSAGE_MONITORING_CHANNEL";
    private static final int NOTIFICATION_ID = 1001;
    
    private PowerManager.WakeLock wakeLock;
    private MessageAlarmManager alarmManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        
        createNotificationChannel();
        
        // Initialize alarm manager for periodic checks
        alarmManager = new MessageAlarmManager(this);
        
        // Acquire partial wake lock to keep CPU awake for message processing
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, 
                "LinguaSMS:MessageMonitoring"
            );
            wakeLock.setReferenceCounted(false);
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        
        // Start foreground service with persistent notification
        startForeground(NOTIFICATION_ID, createNotification());
        
        // Schedule periodic message checks
        if (alarmManager != null) {
            alarmManager.schedulePeriodicMessageCheck();
        }
        
        // Acquire wake lock for a short duration to process any pending messages
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(60000); // Hold for 1 minute max
        }
        
        // Check for any missed messages
        checkForMissedMessages();
        
        // Return START_STICKY to restart if killed by system
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        
        // Cancel alarms
        if (alarmManager != null) {
            alarmManager.cancelPeriodicMessageCheck();
        }
        
        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
    
    /**
     * Creates notification channel for the foreground service with minimal visibility
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Message Monitoring",
                NotificationManager.IMPORTANCE_MIN
            );
            channel.setDescription("Background message monitoring service");
            channel.setShowBadge(false);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setSound(null, null);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Creates the minimal notification for the foreground service
     */
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Background Service")
            .setContentText("Running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build();
    }
    
    /**
     * Check for any missed messages that might have been lost during deep sleep
     */
    private void checkForMissedMessages() {
        try {
            TranslatorApp app = (TranslatorApp) getApplicationContext();
            MessageService messageService = app.getMessageService();
            
            if (messageService != null) {
                // Schedule a background task to sync messages
                MessageWorkManager workManager = new MessageWorkManager(this);
                workManager.scheduleSyncMessages();
                Log.d(TAG, "Scheduled message sync to check for missed messages");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking for missed messages", e);
        }
    }
    
    /**
     * Starts the message monitoring service
     */
    public static void startService(Context context) {
        Intent intent = new Intent(context, MessageMonitoringService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
    
    /**
     * Stops the message monitoring service
     */
    public static void stopService(Context context) {
        Intent intent = new Intent(context, MessageMonitoringService.class);
        context.stopService(intent);
    }
}