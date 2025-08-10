package com.translator.messagingapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * A headless service for sending SMS messages.
 * This service is required for an app to be set as the default SMS app in Android.
 * It doesn't actually do anything itself, as the real SMS sending functionality is
 * implemented elsewhere in the app.
 */
public class HeadlessSmsSendService extends Service {
    private static final String TAG = "HeadlessSmsSendService";

    /**
     * Called when the service is created.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
    }

    /**
     * Called when the service is started.
     *
     * @param intent The intent that started the service
     * @param flags Additional data about this start request
     * @param startId A unique integer representing this specific request to start
     * @return The return value indicates what semantics the system should use for the service's current started state
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started with intent: " + (intent != null ? intent.getAction() : "null"));
        return Service.START_NOT_STICKY;
    }

    /**
     * Called when a client binds to the service.
     * This method must be implemented, but can return null if clients can't bind to the service.
     *
     * @param intent The intent that was used to bind to this service
     * @return An IBinder through which clients can call on to the service
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service bound with intent: " + (intent != null ? intent.getAction() : "null"));
        return null;
    }

    /**
     * Called when the service is being destroyed.
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        super.onDestroy();
    }
}
