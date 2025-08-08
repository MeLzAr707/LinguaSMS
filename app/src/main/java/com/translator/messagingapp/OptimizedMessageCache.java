package com.translator.messagingapp;

import android.util.Log;
import android.util.LruCache;

import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced cache for storing messages with LRU (Least Recently Used) implementation
 * to avoid repeated database queries and improve performance.
 */
public class OptimizedMessageCache {
    private static final String TAG = "OptimizedMessageCache";
    private static final int CACHE_SIZE = 20; // Number of threads to cache
    
    private final LruCache<String, List<Message>> messageCache;
    
    public OptimizedMessageCache() {
        messageCache = new LruCache<>(CACHE_SIZE);
    }
    
    /**
     * Gets cached messages for a thread.
     *
     * @param threadId The thread ID
     * @return The list of messages, or null if not in cache
     */
    public List<Message> getMessages(String threadId) {
        if (threadId == null || threadId.isEmpty()) {
            return null;
        }
        
        List<Message> cachedMessages = messageCache.get(threadId);
        if (cachedMessages != null) {
            Log.d(TAG, "Cache hit for thread: " + threadId + " (" + cachedMessages.size() + " messages)");
            // Return a copy to prevent modification of cached data
            return new ArrayList<>(cachedMessages);
        }
        
        Log.d(TAG, "Cache miss for thread: " + threadId);
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
        
        // Store a copy to prevent modification of cached data
        messageCache.put(threadId, new ArrayList<>(messages));
        Log.d(TAG, "Cached " + messages.size() + " messages for thread: " + threadId);
    }
    
    /**
     * Clears the cache for a specific thread.
     *
     * @param threadId The thread ID to clear from cache
     */
    public void clearCache(String threadId) {
        if (threadId == null || threadId.isEmpty()) {
            return;
        }
        
        messageCache.remove(threadId);
        Log.d(TAG, "Cleared cache for thread: " + threadId);
    }
    
    /**
     * Clears all caches.
     */
    public void clearAllCaches() {
        messageCache.evictAll();
        Log.d(TAG, "All caches cleared");
    }
    
    /**
     * Gets the current cache size.
     *
     * @return The number of threads in cache
     */
    public int getCacheSize() {
        return messageCache.size();
    }
    
    /**
     * Gets the maximum cache size.
     *
     * @return The maximum number of threads that can be cached
     */
    public int getMaxCacheSize() {
        return messageCache.maxSize();
    }
}