package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * WorkManager worker that handles scheduled message delivery as a fallback
 * when AlarmManager exact alarms are restricted.
 */
public class ScheduledMessageWorker extends Worker {
    private static final String TAG = "ScheduledMessageWorker";

    public ScheduledMessageWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        long messageId = getInputData().getLong("message_id", -1);
        
        if (messageId == -1) {
            Log.e(TAG, "No message ID provided to worker");
            return Result.failure();
        }
        
        Log.d(TAG, "Processing scheduled message " + messageId + " via WorkManager");
        
        try {
            ScheduledMessageManager manager = new ScheduledMessageManager(getApplicationContext());
            manager.processReadyMessages();
            
            Log.d(TAG, "Successfully processed scheduled message " + messageId);
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Failed to process scheduled message " + messageId, e);
            return Result.retry();
        }
    }
}