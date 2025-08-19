package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;
import android.util.LruCache;
import java.util.ArrayList;
import java.util.List;

/**
 * Optimized cache for storing messages using LRU eviction policy.
 * Improves performance by avoiding repeated database queries for frequently accessed conversations.
 */
public class OptimizedMessageCache {
    private static final String TAG = "OptimizedMessageCache";
    private static final int MAX_MEMORY_SIZE = 1024 * 1024 * 4; // 4MB cache size
    
    private LruCache<String, List<Message>> messageCache;
    private LruCache<String, Conversation> conversationCache;
    private final Context context; // Add context field for constructor compatibility
    
    public OptimizedMessageCache() {
        this.context = null;
        initializeCaches();
    }
    
    /**
     * Constructor that accepts a Context parameter for compatibility.
     *
     * @param context The application context
     */
    public OptimizedMessageCache(Context context) {
        this.context = context;
        initializeCaches();
    }
    
    /**
     * Initializes the LRU caches.
     */

    private void initializeCaches() {
        // Initialize LRU cache for messages with memory-based eviction
        messageCache = new LruCache<String, List<Message>>(MAX_MEMORY_SIZE) {
            @Override
            protected int sizeOf(String key, List<Message> messages) {
                // Estimate memory usage per message (approximate)
                return messages.size() * 500; // ~500 bytes per message
            }
            
            @Override
            protected void entryRemoved(boolean evicted, String key, List<Message> oldValue, List<Message> newValue) {
                if (evicted) {
                    Log.d(TAG, "Evicted messages for thread: " + key);
                }
            }
        };
        
        // Initialize LRU cache for conversations
        conversationCache = new LruCache<String, Conversation>(200) {
            @Override
            protected void entryRemoved(boolean evicted, String key, Conversation oldValue, Conversation newValue) {
                if (evicted) {
                    Log.d(TAG, "Evicted conversation: " + key);
                }
            }
        };
    }
    
    /**
     * Gets cached messages for a thread.
     *
     * @param threadId The thread ID
     * @return Copy of cached messages, or null if not in cache
     */
    public List<Message> getCachedMessages(String threadId) {
        if (threadId == null || threadId.isEmpty()) {
            return null;
        }
        
        List<Message> cached = messageCache.get(threadId);
        if (cached != null) {
            Log.d(TAG, "Cache hit for messages in thread: " + threadId);
            // Return a copy to prevent external modification
            return new ArrayList<>(cached);
        }
        return null;
    }
    
    /**
     * Caches messages for a thread.
     *
     * @param threadId The thread ID
     * @param messages The list of messages to cache
     */
    public void cacheMessages(String threadId, List<Message> messages) {
        if (threadId == null || threadId.isEmpty() || messages == null) {
            return;
        }
        
        // Store a copy to prevent external modification
        List<Message> messageCopy = new ArrayList<>(messages);
        messageCache.put(threadId, messageCopy);
        Log.d(TAG, "Cached " + messages.size() + " messages for thread: " + threadId);
    }
    
    /**
     * Gets cached conversation details.
     *
     * @param threadId The thread ID
     * @return Cached conversation, or null if not in cache
     */
    public Conversation getCachedConversation(String threadId) {
        if (threadId == null || threadId.isEmpty()) {
            return null;
        }
        
        Conversation cached = conversationCache.get(threadId);
        if (cached != null) {
            Log.d(TAG, "Cache hit for conversation: " + threadId);
        }
        return cached;
    }
    
    /**
     * Caches conversation details.
     *
     * @param threadId The thread ID
     * @param conversation The conversation to cache
     */
    public void cacheConversation(String threadId, Conversation conversation) {
        if (threadId == null || threadId.isEmpty() || conversation == null) {
            return;
        }
        
        conversationCache.put(threadId, conversation);
        Log.d(TAG, "Cached conversation: " + threadId);
    }
    
    /**
     * Clears all caches.
     */
    public void clearCache() {
        messageCache.evictAll();
        conversationCache.evictAll();
        Log.d(TAG, "All caches cleared");
    }
    
    /**
     * Clears cache for a specific thread.
     *
     * @param threadId The thread ID to clear from cache
     */
    public void clearCacheForThread(String threadId) {
        if (threadId == null || threadId.isEmpty()) {
            return;
        }
        
        messageCache.remove(threadId);
        conversationCache.remove(threadId);
        Log.d(TAG, "Cleared cache for thread: " + threadId);
    }
    
    /**
     * Gets cache statistics.
     *
     * @return String with cache hit/miss statistics
     */
    public String getCacheStats() {
        return String.format("Message cache: %d/%d, Conversation cache: %d/%d",
                messageCache.hitCount(), messageCache.missCount() + messageCache.hitCount(),
                conversationCache.hitCount(), conversationCache.missCount() + conversationCache.hitCount());
    }
    
    /**
     * Performs maintenance operations on the cache.
     * This method can be called periodically to optimize cache performance.
     */
    public void performMaintenance() {
        Log.d(TAG, "Performing cache maintenance");
        
        // Log current cache statistics
        Log.d(TAG, "Current cache stats: " + getCacheStats());
        
        // Optionally trim memory if needed
        messageCache.trimToSize(MAX_MEMORY_SIZE / 2);
        
        Log.d(TAG, "Cache maintenance completed");
    }
}