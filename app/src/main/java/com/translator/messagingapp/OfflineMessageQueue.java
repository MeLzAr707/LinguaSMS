package com.translator.messagingapp;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages offline message queue for handling messages when network connectivity is limited.
 * Provides methods to queue, retry, and manage failed messages.
 */
public class OfflineMessageQueue {
    private static final String TAG = "OfflineMessageQueue";
    private final ConcurrentLinkedQueue<QueuedMessage> messageQueue;
    private final List<QueuedMessage> failedMessages;
    private long nextMessageId = 1;
    
    public OfflineMessageQueue() {
        this.messageQueue = new ConcurrentLinkedQueue<>();
        this.failedMessages = new ArrayList<>();
    }
    
    /**
     * Gets all failed messages in the queue.
     * @return List of failed messages
     */
    public List<QueuedMessage> getFailedMessages() {
        synchronized (failedMessages) {
            return new ArrayList<>(failedMessages);
        }
    }
    
    /**
     * Retries a failed message by its ID.
     * @param messageId The ID of the message to retry
     * @return true if the message was found and queued for retry
     */
    public boolean retryFailedMessage(long messageId) {
        synchronized (failedMessages) {
            for (QueuedMessage message : failedMessages) {
                if (message.getId() == messageId) {
                    message.setFailed(false);
                    message.setRetryCount(message.getRetryCount() + 1);
                    messageQueue.offer(message);
                    failedMessages.remove(message);
                    Log.d(TAG, "Retrying message ID: " + messageId);
                    return true;
                }
            }
        }
        Log.w(TAG, "Failed message not found for retry: " + messageId);
        return false;
    }
    
    /**
     * Clears all failed messages from the queue.
     * @return The number of messages that were cleared
     */
    public int clearFailedMessages() {
        synchronized (failedMessages) {
            int count = failedMessages.size();
            failedMessages.clear();
            Log.d(TAG, "Cleared " + count + " failed messages");
            return count;
        }
    }
    
    /**
     * Gets the next message in the queue.
     * @return The next QueuedMessage or null if queue is empty
     */
    public QueuedMessage getNextMessage() {
        QueuedMessage message = messageQueue.poll();
        if (message != null) {
            Log.d(TAG, "Retrieved message ID: " + message.getId());
        }
        return message;
    }
    
    /**
     * Adds a message to the queue.
     * @param address The recipient address
     * @param body The message body
     * @return The queued message
     */
    public QueuedMessage addMessage(String address, String body) {
        QueuedMessage message = new QueuedMessage(nextMessageId++, address, body, System.currentTimeMillis());
        messageQueue.offer(message);
        Log.d(TAG, "Added message to queue: " + message.getId());
        return message;
    }
    
    /**
     * Marks a message as failed and moves it to the failed messages list.
     * @param message The message that failed
     */
    public void markMessageFailed(QueuedMessage message) {
        message.setFailed(true);
        synchronized (failedMessages) {
            failedMessages.add(message);
        }
        Log.w(TAG, "Marked message as failed: " + message.getId());
    }
    
    /**
     * Gets the current size of the message queue.
     * @return The number of pending messages
     */
    public int getQueueSize() {
        return messageQueue.size();
    }
    
    /**
     * Gets the number of failed messages.
     * @return The number of failed messages
     */
    public int getFailedMessageCount() {
        synchronized (failedMessages) {
            return failedMessages.size();
        }
    }
}