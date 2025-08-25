package com.translator.messagingapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages scheduled messages - creation, storage, scheduling, and delivery.
 */
public class ScheduledMessageManager {
    private static final String TAG = "ScheduledMessageManager";
    private static final String PREFS_NAME = "scheduled_messages";
    private static final String PREFS_KEY_MESSAGES = "messages";
    private static final String ACTION_SEND_SCHEDULED_MESSAGE = "com.translator.messagingapp.SEND_SCHEDULED_MESSAGE";
    
    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final AlarmManager alarmManager;
    private final MessageWorkManager messageWorkManager;
    private long nextMessageId = 1;

    public ScheduledMessageManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.messageWorkManager = new MessageWorkManager(context);
        loadNextMessageId();
    }

    /**
     * Check if the app has permission to schedule exact alarms.
     * On Android 12+ (API 31+), apps need special permission for exact alarms.
     */
    public boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return alarmManager.canScheduleExactAlarms();
        }
        return true; // Always available on older versions
    }

    /**
     * Get user-friendly message about scheduling reliability.
     */
    public String getSchedulingReliabilityMessage() {
        if (canScheduleExactAlarms()) {
            return "Messages will be delivered at the exact scheduled time.";
        } else {
            return "Scheduled messages will be delivered approximately at the scheduled time. " +
                   "For exact timing, enable 'Alarms & reminders' permission in Settings.";
        }
    }

    /**
     * Schedule a message to be sent at the specified time.
     */
    public long scheduleMessage(String recipient, String messageBody, long scheduledTime, String threadId) {
        if (scheduledTime <= System.currentTimeMillis()) {
            Log.w(TAG, "Cannot schedule message for past time");
            return -1;
        }

        ScheduledMessage message = new ScheduledMessage(recipient, messageBody, scheduledTime, threadId);
        message.setId(getNextMessageId());
        
        // Store the message
        saveScheduledMessage(message);
        
        // Schedule the alarm
        scheduleAlarm(message);
        
        Log.d(TAG, "Scheduled message " + message.getId() + " for delivery at " + scheduledTime);
        return message.getId();
    }

    /**
     * Cancel a scheduled message.
     */
    public boolean cancelScheduledMessage(long messageId) {
        List<ScheduledMessage> messages = getScheduledMessages();
        ScheduledMessage messageToCancel = null;
        
        for (ScheduledMessage message : messages) {
            if (message.getId() == messageId) {
                messageToCancel = message;
                break;
            }
        }
        
        if (messageToCancel == null) {
            Log.w(TAG, "Message with ID " + messageId + " not found");
            return false;
        }
        
        // Cancel the alarm
        cancelAlarm(messageToCancel);
        
        // Remove from storage
        messages.remove(messageToCancel);
        saveAllScheduledMessages(messages);
        
        Log.d(TAG, "Cancelled scheduled message " + messageId);
        return true;
    }

    /**
     * Get all scheduled messages.
     */
    public List<ScheduledMessage> getScheduledMessages() {
        String json = prefs.getString(PREFS_KEY_MESSAGES, "[]");
        Type listType = new TypeToken<List<ScheduledMessage>>(){}.getType();
        List<ScheduledMessage> messages = gson.fromJson(json, listType);
        return messages != null ? messages : new ArrayList<>();
    }

    /**
     * Get all pending (undelivered) scheduled messages.
     */
    public List<ScheduledMessage> getPendingScheduledMessages() {
        List<ScheduledMessage> allMessages = getScheduledMessages();
        List<ScheduledMessage> pendingMessages = new ArrayList<>();
        
        for (ScheduledMessage message : allMessages) {
            if (!message.isDelivered()) {
                pendingMessages.add(message);
            }
        }
        
        return pendingMessages;
    }

    /**
     * Process and send any messages that are ready to be sent.
     */
    public void processReadyMessages() {
        List<ScheduledMessage> messages = getScheduledMessages();
        boolean hasChanges = false;
        
        Iterator<ScheduledMessage> iterator = messages.iterator();
        while (iterator.hasNext()) {
            ScheduledMessage message = iterator.next();
            
            if (message.isReadyToSend()) {
                Log.d(TAG, "Processing ready message: " + message.getId());
                
                // Send the message via WorkManager for reliability
                messageWorkManager.scheduleSendSms(
                    message.getRecipient(),
                    message.getMessageBody(),
                    message.getThreadId()
                );
                
                // Mark as delivered
                message.setDelivered(true);
                hasChanges = true;
                
                Log.d(TAG, "Sent scheduled message " + message.getId() + " to " + message.getRecipient());
            }
        }
        
        if (hasChanges) {
            saveAllScheduledMessages(messages);
        }
    }

    /**
     * Reschedule all pending messages (useful after device reboot).
     */
    public void rescheduleAllPendingMessages() {
        List<ScheduledMessage> pendingMessages = getPendingScheduledMessages();
        
        for (ScheduledMessage message : pendingMessages) {
            if (message.isScheduledForFuture()) {
                scheduleAlarm(message);
                Log.d(TAG, "Rescheduled message " + message.getId());
            } else if (message.isReadyToSend()) {
                // If message time has passed, send it immediately
                processReadyMessages();
            }
        }
        
        Log.d(TAG, "Rescheduled " + pendingMessages.size() + " pending messages");
    }

    /**
     * Update an existing scheduled message.
     */
    public boolean updateScheduledMessage(long messageId, String newRecipient, String newMessageBody, long newScheduledTime) {
        List<ScheduledMessage> messages = getScheduledMessages();
        ScheduledMessage messageToUpdate = null;
        
        for (ScheduledMessage message : messages) {
            if (message.getId() == messageId && !message.isDelivered()) {
                messageToUpdate = message;
                break;
            }
        }
        
        if (messageToUpdate == null) {
            return false;
        }
        
        // Cancel the old alarm
        cancelAlarm(messageToUpdate);
        
        // Update the message
        messageToUpdate.setRecipient(newRecipient);
        messageToUpdate.setMessageBody(newMessageBody);
        messageToUpdate.setScheduledTime(newScheduledTime);
        
        // Schedule the new alarm
        scheduleAlarm(messageToUpdate);
        
        // Save changes
        saveAllScheduledMessages(messages);
        
        Log.d(TAG, "Updated scheduled message " + messageId);
        return true;
    }

    private void scheduleAlarm(ScheduledMessage message) {
        Intent intent = new Intent(context, ScheduledMessageReceiver.class);
        intent.setAction(ACTION_SEND_SCHEDULED_MESSAGE);
        intent.putExtra("message_id", message.getId());
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            (int) message.getId(), // Use message ID as request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        message.getScheduledTime(),
                        pendingIntent
                    );
                    Log.d(TAG, "Scheduled exact alarm for message " + message.getId());
                } else {
                    // Use inexact alarm as fallback
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        message.getScheduledTime(),
                        pendingIntent
                    );
                    Log.d(TAG, "Scheduled inexact alarm for message " + message.getId() + " (exact alarms not available)");
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    message.getScheduledTime(),
                    pendingIntent
                );
                Log.d(TAG, "Scheduled exact alarm for message " + message.getId());
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Failed to schedule alarm, falling back to WorkManager", e);
            
            // Fallback to WorkManager if exact alarms are restricted
            long delay = message.getScheduledTime() - System.currentTimeMillis();
            if (delay > 0) {
                Data inputData = new Data.Builder()
                    .putLong("message_id", message.getId())
                    .build();
                
                OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ScheduledMessageWorker.class)
                    .setInputData(inputData)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .addTag("scheduled_message_" + message.getId())
                    .build();
                
                WorkManager.getInstance(context).enqueue(workRequest);
            }
        }
    }

    private void cancelAlarm(ScheduledMessage message) {
        Intent intent = new Intent(context, ScheduledMessageReceiver.class);
        intent.setAction(ACTION_SEND_SCHEDULED_MESSAGE);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            (int) message.getId(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.cancel(pendingIntent);
        
        // Also cancel any WorkManager fallback
        WorkManager.getInstance(context).cancelAllWorkByTag("scheduled_message_" + message.getId());
        
        Log.d(TAG, "Cancelled alarm for message " + message.getId());
    }

    private void saveScheduledMessage(ScheduledMessage message) {
        List<ScheduledMessage> messages = getScheduledMessages();
        messages.add(message);
        saveAllScheduledMessages(messages);
    }

    private void saveAllScheduledMessages(List<ScheduledMessage> messages) {
        String json = gson.toJson(messages);
        prefs.edit().putString(PREFS_KEY_MESSAGES, json).apply();
    }

    private long getNextMessageId() {
        return nextMessageId++;
    }

    private void loadNextMessageId() {
        List<ScheduledMessage> messages = getScheduledMessages();
        long maxId = 0;
        for (ScheduledMessage message : messages) {
            if (message.getId() > maxId) {
                maxId = message.getId();
            }
        }
        nextMessageId = maxId + 1;
    }
}