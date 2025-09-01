package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Worker class for processing scheduled messages.
 * This worker runs in the background to check for and send scheduled messages.
 */
public class ScheduledMessageWorker extends Worker {
    private static final String TAG = "ScheduledMessageWorker";

    public ScheduledMessageWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "ScheduledMessageWorker started");
        
        try {
            // Create ScheduledMessageManager instance
            // Note: Error log shows constructor called with context, but our implementation uses no-arg constructor
            ScheduledMessageManager manager = new ScheduledMessageManager();
            
            // Process messages that are ready to be sent
            manager.processReadyMessages();
            
            Log.d(TAG, "ScheduledMessageWorker completed successfully");
            return Result.success();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in ScheduledMessageWorker", e);
            
            // Return retry to attempt again later
            return Result.retry();
        }
    }
    
    /**
     * Gets the unique work name for this worker.
     * 
     * @return The work name
     */
    public static String getWorkName() {
        return "scheduled_message_processing";
    }
}