package com.translator.messagingapp.mms;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.translator.messagingapp.R;
import com.translator.messagingapp.mms.MmsHelper;
import com.translator.messagingapp.notification.NotificationHelper;
import com.translator.messagingapp.ui.MainActivity;
import com.translator.messagingapp.util.PhoneUtils;

/**
 * Foreground service for downloading and processing MMS content.
 * Handles MMS download operations in the background while ensuring
 * the service remains running even when the app is not visible.
 * 
 * Compatible with API 29+ requirements for background services.
 */
public class MmsDownloadService extends Service {
    private static final String TAG = "MmsDownloadService";
    
    // Service constants
    public static final String ACTION_DOWNLOAD_MMS = "com.translator.messagingapp.DOWNLOAD_MMS";
    public static final String ACTION_PROCESS_MMS = "com.translator.messagingapp.PROCESS_MMS";
    public static final String EXTRA_MMS_URI = "mms_uri";
    public static final String EXTRA_THREAD_ID = "thread_id";
    
    // Notification constants
    private static final String CHANNEL_ID = "mms_download_channel";
    private static final int NOTIFICATION_ID = 2001;
    
    private NotificationManager notificationManager;
    private MmsHelper mmsHelper;
    private NotificationHelper notificationHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MmsDownloadService created");
        
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mmsHelper = new MmsHelper(this);
        notificationHelper = new NotificationHelper(this);
        
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createServiceNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.w(TAG, "Received null intent, stopping service");
            stopSelf();
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        Log.d(TAG, "onStartCommand: " + action);

        // Check if app is default SMS app before processing
        if (!PhoneUtils.isDefaultSmsApp(this)) {
            Log.w(TAG, "App is not default SMS app, stopping MMS download service");
            stopSelf();
            return START_NOT_STICKY;
        }

        switch (action) {
            case ACTION_DOWNLOAD_MMS:
                handleMmsDownload(intent);
                break;
            case ACTION_PROCESS_MMS:
                handleMmsProcessing(intent);
                break;
            default:
                Log.w(TAG, "Unknown action: " + action);
                stopSelf();
                return START_NOT_STICKY;
        }

        return START_REDELIVER_INTENT; // Restart service with same intent if killed
    }

    @Override
    public IBinder onBind(Intent intent) {
        // This service doesn't support binding
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "MmsDownloadService destroyed");
        super.onDestroy();
    }

    /**
     * Handles MMS download requests.
     */
    private void handleMmsDownload(Intent intent) {
        try {
            String mmsUriString = intent.getStringExtra(EXTRA_MMS_URI);
            String threadId = intent.getStringExtra(EXTRA_THREAD_ID);
            
            if (mmsUriString == null) {
                Log.e(TAG, "No MMS URI provided for download");
                stopSelf();
                return;
            }
            
            Uri mmsUri = Uri.parse(mmsUriString);
            Log.d(TAG, "Starting MMS download for URI: " + mmsUri);
            
            updateServiceNotification("Downloading MMS...");
            
            // Use MmsHelper to download and process the MMS
            boolean success = mmsHelper.downloadMms(mmsUri, threadId);
            
            if (success) {
                Log.d(TAG, "MMS download completed successfully");
                updateServiceNotification("MMS downloaded successfully");
                
                // Show user notification
                notificationHelper.showMmsReceivedNotification(
                    "MMS Downloaded", 
                    "New multimedia message received"
                );
                
            } else {
                Log.e(TAG, "MMS download failed");
                updateServiceNotification("MMS download failed");
                
                // Show error notification
                notificationHelper.showMmsReceivedNotification(
                    "MMS Download Failed", 
                    "Failed to download multimedia message"
                );
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error during MMS download", e);
        } finally {
            // Stop service after completion
            stopSelf();
        }
    }

    /**
     * Handles MMS processing requests.
     */
    private void handleMmsProcessing(Intent intent) {
        try {
            String mmsUriString = intent.getStringExtra(EXTRA_MMS_URI);
            
            if (mmsUriString == null) {
                Log.e(TAG, "No MMS URI provided for processing");
                stopSelf();
                return;
            }
            
            Uri mmsUri = Uri.parse(mmsUriString);
            Log.d(TAG, "Starting MMS processing for URI: " + mmsUri);
            
            updateServiceNotification("Processing MMS...");
            
            // Use MmsHelper to process the MMS content
            boolean success = mmsHelper.processMms(mmsUri);
            
            if (success) {
                Log.d(TAG, "MMS processing completed successfully");
                updateServiceNotification("MMS processed successfully");
            } else {
                Log.e(TAG, "MMS processing failed");
                updateServiceNotification("MMS processing failed");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error during MMS processing", e);
        } finally {
            // Stop service after completion
            stopSelf();
        }
    }

    /**
     * Creates the notification channel for the service (Android O+).
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "MMS Download Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Handles MMS downloading in the background");
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Creates the persistent notification for the foreground service.
     */
    private Notification createServiceNotification() {
        return createServiceNotification("MMS service is running");
    }

    /**
     * Creates a service notification with custom text.
     */
    private Notification createServiceNotification(String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            notificationIntent,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                PendingIntent.FLAG_IMMUTABLE : 0
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("MMS Download Service")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    /**
     * Updates the service notification text.
     */
    private void updateServiceNotification(String text) {
        Notification notification = createServiceNotification(text);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    /**
     * Static helper method to start MMS download.
     */
    public static void startMmsDownload(Context context, Uri mmsUri, String threadId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.w(TAG, "MmsDownloadService requires API 29+, using fallback");
            // For older versions, could fallback to existing transaction service
            return;
        }
        
        Intent intent = new Intent(context, MmsDownloadService.class);
        intent.setAction(ACTION_DOWNLOAD_MMS);
        intent.putExtra(EXTRA_MMS_URI, mmsUri.toString());
        if (threadId != null) {
            intent.putExtra(EXTRA_THREAD_ID, threadId);
        }
        
        try {
            context.startForegroundService(intent);
            Log.d(TAG, "Started MMS download service for URI: " + mmsUri);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start MMS download service", e);
        }
    }

    /**
     * Static helper method to start MMS processing.
     */
    public static void startMmsProcessing(Context context, Uri mmsUri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.w(TAG, "MmsDownloadService requires API 29+, using fallback");
            return;
        }
        
        Intent intent = new Intent(context, MmsDownloadService.class);
        intent.setAction(ACTION_PROCESS_MMS);
        intent.putExtra(EXTRA_MMS_URI, mmsUri.toString());
        
        try {
            context.startForegroundService(intent);
            Log.d(TAG, "Started MMS processing service for URI: " + mmsUri);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start MMS processing service", e);
        }
    }
}