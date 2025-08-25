package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Background loader for message operations to avoid blocking the UI thread.
 */
public class BackgroundMessageLoader {
    private static final String TAG = "BackgroundMessageLoader";
    
    private final Context context;
    private final MessageService messageService;
    private final ExecutorService executorService;
    
    /**
     * Interface for message loading callbacks.
     */
    public interface MessageLoadCallback {
        void onMessagesLoaded(List<Message> messages);
        void onError(Exception error);
    }
    
    /**
     * Creates a new BackgroundMessageLoader.
     *
     * @param context The application context
     * @param messageService The message service to use
     */
    public BackgroundMessageLoader(Context context, MessageService messageService) {
        this.context = context;
        this.messageService = messageService;
        this.executorService = Executors.newFixedThreadPool(2);
    }
    
    /**
     * Loads messages by thread ID with pagination in the background.
     *
     * @param threadId The thread ID
     * @param offset The offset
     * @param limit The limit
     * @param callback The callback to invoke when loading is complete
     */
    public void getMessagesByThreadIdPaginated(String threadId, int offset, int limit, MessageLoadCallback callback) {
        if (messageService == null) {
            callback.onError(new IllegalStateException("MessageService is null"));
            return;
        }
        
        executorService.execute(() -> {
            try {
                List<Message> messages = messageService.getMessagesByThreadIdPaginated(threadId, offset, limit);
                callback.onMessagesLoaded(messages);
            } catch (Exception e) {
                Log.e(TAG, "Error loading paginated messages for thread " + threadId, e);
                callback.onError(e);
            }
        });
    }
    
    /**
     * Loads all messages by thread ID in the background.
     *
     * @param threadId The thread ID
     * @param callback The callback to invoke when loading is complete
     */
    public void getMessagesByThreadId(String threadId, MessageLoadCallback callback) {
        if (messageService == null) {
            callback.onError(new IllegalStateException("MessageService is null"));
            return;
        }
        
        executorService.execute(() -> {
            try {
                List<Message> messages = messageService.getMessagesByThreadId(threadId);
                callback.onMessagesLoaded(messages);
            } catch (Exception e) {
                Log.e(TAG, "Error loading messages for thread " + threadId, e);
                callback.onError(e);
            }
        });
    }
    
    /**
     * Loads messages by address in the background.
     *
     * @param address The address to filter by
     * @param callback The callback to invoke when loading is complete
     */
    public void getMessagesByAddress(String address, MessageLoadCallback callback) {
        if (messageService == null) {
            callback.onError(new IllegalStateException("MessageService is null"));
            return;
        }
        
        executorService.execute(() -> {
            try {
                List<Message> messages = messageService.getMessagesByAddress(address);
                callback.onMessagesLoaded(messages);
            } catch (Exception e) {
                Log.e(TAG, "Error loading messages for address " + address, e);
                callback.onError(e);
            }
        });
    }
    
    /**
     * Cleanup method to release resources.
     */
    public void cleanup() {
        try {
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
            Log.d(TAG, "BackgroundMessageLoader cleanup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }
}