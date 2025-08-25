package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages a queue of messages for offline processing and handling.
 * Provides functionality to queue messages when network is unavailable
 * and process them when connectivity is restored.
 */
public class OfflineMessageQueue {
    private static final String TAG = "OfflineMessageQueue";
    
    private final Context context;
    private final ConcurrentLinkedQueue<QueuedMessage> messageQueue;
    private QueueStatus currentStatus;
    
    /**
     * Enum representing the status of the offline message queue.
     */
    public enum QueueStatus {
        IDLE,           // Queue is empty and waiting
        PROCESSING,     // Queue is actively processing messages
        PAUSED,         // Queue processing is paused
        ERROR           // Queue encountered an error
    }
    
    /**
     * Interface for queue status change notifications.
     */
    public interface QueueStatusListener {
        void onQueueStatusChanged(QueueStatus status);
    }
    
    /**
     * Class representing a queued message.
     */
    public static class QueuedMessage {
        private final String id;
        private final String recipient;
        private final String body;
        private final long timestamp;
        private final boolean requiresTranslation;
        
        public QueuedMessage(String id, String recipient, String body, boolean requiresTranslation) {
            this.id = id;
            this.recipient = recipient;
            this.body = body;
            this.timestamp = System.currentTimeMillis();
            this.requiresTranslation = requiresTranslation;
        }
        
        // Getters
        public String getId() { return id; }
        public String getRecipient() { return recipient; }
        public String getBody() { return body; }
        public long getTimestamp() { return timestamp; }
        public boolean requiresTranslation() { return requiresTranslation; }
    }
    
    /**
     * Creates a new OfflineMessageQueue.
     *
     * @param context The application context
     */
    public OfflineMessageQueue(Context context) {
        this.context = context.getApplicationContext();
        this.messageQueue = new ConcurrentLinkedQueue<>();
        this.currentStatus = QueueStatus.IDLE;
    }
    
    /**
     * Adds a message to the offline queue.
     *
     * @param message The message to queue
     */
    public void queueMessage(QueuedMessage message) {
        if (message != null) {
            messageQueue.offer(message);
            Log.d(TAG, "Message queued: " + message.getId());
            
            if (currentStatus == QueueStatus.IDLE) {
                setStatus(QueueStatus.PROCESSING);
            }
        }
    }
    
    /**
     * Gets the current queue status.
     *
     * @return The current queue status
     */
    public QueueStatus getStatus() {
        return currentStatus;
    }
    
    /**
     * Sets the queue status and notifies listeners.
     *
     * @param status The new queue status
     */
    private void setStatus(QueueStatus status) {
        if (this.currentStatus != status) {
            this.currentStatus = status;
            Log.d(TAG, "Queue status changed to: " + status);
        }
    }
    
    /**
     * Gets the number of messages in the queue.
     *
     * @return The queue size
     */
    public int getQueueSize() {
        return messageQueue.size();
    }
    
    /**
     * Checks if the queue is empty.
     *
     * @return true if the queue is empty, false otherwise
     */
    public boolean isEmpty() {
        return messageQueue.isEmpty();
    }
    
    /**
     * Processes the next message in the queue.
     *
     * @return The next message, or null if queue is empty
     */
    public QueuedMessage processNextMessage() {
        QueuedMessage message = messageQueue.poll();
        if (message != null) {
            Log.d(TAG, "Processing message: " + message.getId());
        }
        
        if (messageQueue.isEmpty()) {
            setStatus(QueueStatus.IDLE);
        }
        
        return message;
    }
    
    /**
     * Clears all messages from the queue.
     */
    public void clearQueue() {
        messageQueue.clear();
        setStatus(QueueStatus.IDLE);
        Log.d(TAG, "Queue cleared");
    }
    
    /**
     * Pauses queue processing.
     */
    public void pauseProcessing() {
        setStatus(QueueStatus.PAUSED);
        Log.d(TAG, "Queue processing paused");
    }
    
    /**
     * Resumes queue processing.
     */
    public void resumeProcessing() {
        if (!messageQueue.isEmpty()) {
            setStatus(QueueStatus.PROCESSING);
        } else {
            setStatus(QueueStatus.IDLE);
        }
        Log.d(TAG, "Queue processing resumed");
    }
    
    /**
     * Gets a list of all queued messages.
     *
     * @return List of queued messages
     */
    public List<QueuedMessage> getAllMessages() {
        return new ArrayList<>(messageQueue);
    }
}