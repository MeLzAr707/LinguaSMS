package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;
import java.util.List;

/**
 * Enhanced message service that provides additional message processing capabilities.
 * This service extends the basic message functionality with caching and optimization features.
 */
public class EnhancedMessageService {
    private static final String TAG = "EnhancedMessageService";
    
    private Context context;
    private OptimizedMessageCache optimizedCache;
    
    public EnhancedMessageService(Context context) {
        this.context = context;
        this.optimizedCache = new OptimizedMessageCache(context);
        Log.d(TAG, "EnhancedMessageService created");
    }
    
    /**
     * Gets messages for a thread from cache or database.
     * 
     * @param threadId The thread ID
     * @return List of messages for the thread
     */
    public List<Message> getMessagesForThread(String threadId) {
        Log.d(TAG, "Getting messages for thread: " + threadId);
        
        // Try to get from cache first
        List<Message> cachedMessages = optimizedCache.getMessagesForThread(Long.parseLong(threadId));
        
        if (cachedMessages != null && !cachedMessages.isEmpty()) {
            Log.d(TAG, "Found " + cachedMessages.size() + " cached messages");
            return cachedMessages;
        }
        
        // If not in cache, would normally load from database
        // For now, return empty list to prevent compilation error
        Log.d(TAG, "No cached messages found, would load from database");
        return null;
    }
    
    /**
     * Adds a message to the enhanced service.
     * 
     * @param message The message to add
     */
    public void addMessage(Message message) {
        if (message != null) {
            Log.d(TAG, "Adding message to enhanced service");
            optimizedCache.addMessage(String.valueOf(message.getThreadId()), message);
        }
    }
    
    /**
     * Clears the message cache.
     */
    public void clearCache() {
        Log.d(TAG, "Clearing enhanced message cache");
        optimizedCache.clear();
    }
    
    /**
     * Gets cache statistics.
     * 
     * @return Cache statistics string
     */
    public String getCacheStats() {
        return optimizedCache.getStats();
    }
    
    /**
     * Clears all data (for testing or reset purposes).
     */
    public void clearAllData() {
        Log.d(TAG, "Clearing all enhanced service data");
        optimizedCache.clear();
    }
}