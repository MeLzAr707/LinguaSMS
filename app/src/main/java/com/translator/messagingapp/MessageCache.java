package com.translator.messagingapp;

import android.util.Log;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cache for storing messages to avoid repeated database queries.
 */
public class MessageCache {
    private static final String TAG = "MessageCache";
    private static final Object CACHE_LOCK = new Object();
    private static final Map<String, List<Message>> messageCache = new HashMap<>();
    private static final int MAX_CACHE_SIZE = 20; // Maximum number of threads to cache

    /**
     * Gets cached messages for a thread.
     *
     * @param threadId The thread ID
     * @return The list of messages, or null if not in cache
     */
    public static List<Message> getCachedMessages(String threadId) {
        if (threadId == null || threadId.isEmpty()) {
            return null;
        }

        synchronized (CACHE_LOCK) {
            return messageCache.get(threadId);
        }
    }

    /**
     * Caches messages for a thread.
     *
     * @param threadId The thread ID
     * @param messages The list of messages to cache
     */
    public static void cacheMessages(String threadId, List<Message> messages) {
        if (threadId == null || threadId.isEmpty() || messages == null) {
            return;
        }

        synchronized (CACHE_LOCK) {
            // Clean up cache if it gets too large
            if (messageCache.size() >= MAX_CACHE_SIZE) {
                // Remove oldest entry (first key)
                if (!messageCache.isEmpty()) {
                    String oldestKey = messageCache.keySet().iterator().next();
                    messageCache.remove(oldestKey);
                    Log.d(TAG, "Cache full, removed thread: " + oldestKey);
                }
            }

            // Add to cache
            messageCache.put(threadId, messages);
            Log.d(TAG, "Cached " + messages.size() + " messages for thread: " + threadId);
        }
    }

    /**
     * Clears the entire cache.
     */
    public static void clearCache() {
        synchronized (CACHE_LOCK) {
            messageCache.clear();
            Log.d(TAG, "Cache cleared");
        }
    }

    /**
     * Clears the cache for a specific thread.
     *
     * @param threadId The thread ID to clear from cache
     */
    public static void clearCacheForThread(String threadId) {
        if (threadId == null || threadId.isEmpty()) {
            return;
        }

        synchronized (CACHE_LOCK) {
            messageCache.remove(threadId);
            Log.d(TAG, "Cleared cache for thread: " + threadId);
        }
    }
}



