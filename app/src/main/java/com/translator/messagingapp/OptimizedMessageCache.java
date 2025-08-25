package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;
import android.util.LruCache;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Optimized cache for storing messages using intelligent LRU eviction policy.
 * Improves performance by avoiding repeated database queries for frequently accessed conversations.
 * Features intelligent caching, frequency tracking, and performance metrics.
 */
public class OptimizedMessageCache {
    private static final String TAG = "OptimizedMessageCache";
    private static final int MAX_MEMORY_SIZE = 1024 * 1024 * 4; // 4MB cache size
    private static final int HIGH_FREQUENCY_THRESHOLD = 5; // Minimum access count for high frequency
    private static final long RECENT_ACCESS_WINDOW = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
    
    private LruCache<String, CachedMessageData> messageCache;
    private LruCache<String, Conversation> conversationCache;
    private final Context context;
    
    // Intelligence tracking
    private final Map<String, AccessStats> accessStats = new ConcurrentHashMap<>();
    private final AtomicLong totalHits = new AtomicLong(0);
    private final AtomicLong totalMisses = new AtomicLong(0);
    
    /**
     * Inner class to hold cached message data with metadata
     */
    private static class CachedMessageData {
        final List<Message> messages;
        final long cacheTime;
        final int estimatedSize;
        
        CachedMessageData(List<Message> messages) {
            this.messages = new ArrayList<>(messages);
            this.cacheTime = System.currentTimeMillis();
            this.estimatedSize = messages.size() * 500; // ~500 bytes per message
        }
    }
    
    /**
     * Inner class to track access statistics for intelligent caching
     */
    private static class AccessStats {
        long accessCount;
        long lastAccessed;
        long firstAccessed;
        
        AccessStats() {
            this.accessCount = 1;
            this.lastAccessed = System.currentTimeMillis();
            this.firstAccessed = this.lastAccessed;
        }
        
        void recordAccess() {
            this.accessCount++;
            this.lastAccessed = System.currentTimeMillis();
        }
        
        boolean isHighFrequency() {
            return accessCount >= HIGH_FREQUENCY_THRESHOLD;
        }
        
        boolean isRecentlyAccessed() {
            return (System.currentTimeMillis() - lastAccessed) < RECENT_ACCESS_WINDOW;
        }
    }
    
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
     * Initializes the intelligent LRU caches.
     */
    private void initializeCaches() {
        // Initialize intelligent LRU cache for messages with memory-based eviction
        messageCache = new LruCache<String, CachedMessageData>(MAX_MEMORY_SIZE) {
            @Override
            protected int sizeOf(String key, CachedMessageData data) {
                return data.estimatedSize;
            }
            
            @Override
            protected void entryRemoved(boolean evicted, String key, CachedMessageData oldValue, CachedMessageData newValue) {
                if (evicted) {
                    Log.d(TAG, "Evicted messages for thread: " + key + " (size: " + oldValue.estimatedSize + " bytes)");
                    
                    // Keep access stats for potential re-caching decisions
                    AccessStats stats = accessStats.get(key);
                    if (stats != null && !stats.isHighFrequency()) {
                        // Remove stats for low-frequency items to prevent memory leak
                        accessStats.remove(key);
                    }
                }
            }
            
            @Override
            protected CachedMessageData create(String key) {
                // This method is called when cache miss occurs
                totalMisses.incrementAndGet();
                return null; // Let the caller handle cache misses
            }
        };
        
        // Initialize LRU cache for conversations with intelligent eviction
        conversationCache = new LruCache<String, Conversation>(200) {
            @Override
            protected void entryRemoved(boolean evicted, String key, Conversation oldValue, Conversation newValue) {
                if (evicted) {
                    Log.d(TAG, "Evicted conversation: " + key);
                    // Keep access stats for high-frequency items, remove for low-frequency
                    AccessStats stats = accessStats.get(key);
                    if (stats != null && !stats.isHighFrequency()) {
                        accessStats.remove(key);
                    }
                }
            }
        };
    }
    
    /**
     * Gets cached messages for a thread with intelligent access tracking.
     *
     * @param threadId The thread ID
     * @return Copy of cached messages, or null if not in cache
     */
    public List<Message> getCachedMessages(String threadId) {
        if (threadId == null || threadId.isEmpty()) {
            return null;
        }
        
        CachedMessageData cachedData = messageCache.get(threadId);
        if (cachedData != null) {
            // Record successful cache hit
            totalHits.incrementAndGet();
            recordAccess(threadId);
            
            Log.d(TAG, "Cache hit for messages in thread: " + threadId + 
                  " (access count: " + getAccessCount(threadId) + ")");
            
            // Return a copy to prevent external modification
            return new ArrayList<>(cachedData.messages);
        }
        
        // Cache miss
        totalMisses.incrementAndGet();
        Log.d(TAG, "Cache miss for messages in thread: " + threadId);
        return null;
    }
    
    /**
     * Caches messages for a thread with intelligent prioritization.
     *
     * @param threadId The thread ID
     * @param messages The list of messages to cache
     */
    public void cacheMessages(String threadId, List<Message> messages) {
        if (threadId == null || threadId.isEmpty() || messages == null) {
            return;
        }
        
        // Create cached data with metadata
        CachedMessageData cachedData = new CachedMessageData(messages);
        
        // Record access for intelligent eviction
        recordAccess(threadId);
        
        // Cache the data
        messageCache.put(threadId, cachedData);
        
        Log.d(TAG, "Cached " + messages.size() + " messages for thread: " + threadId + 
              " (estimated size: " + cachedData.estimatedSize + " bytes)");
    }
    
    /**
     * Records access to a thread for intelligent caching decisions.
     */
    private void recordAccess(String threadId) {
        AccessStats stats = accessStats.get(threadId);
        if (stats == null) {
            stats = new AccessStats();
            accessStats.put(threadId, stats);
        } else {
            stats.recordAccess();
        }
    }
    
    /**
     * Gets access count for a thread.
     */
    private long getAccessCount(String threadId) {
        AccessStats stats = accessStats.get(threadId);
        return stats != null ? stats.accessCount : 0;
    }
    
    /**
     * Gets cached conversation details with intelligent access tracking.
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
            recordAccess(threadId);
            Log.d(TAG, "Cache hit for conversation: " + threadId + 
                  " (access count: " + getAccessCount(threadId) + ")");
        }
        return cached;
    }
    
    /**
     * Caches conversation details with intelligent prioritization.
     *
     * @param threadId The thread ID
     * @param conversation The conversation to cache
     */
    public void cacheConversation(String threadId, Conversation conversation) {
        if (threadId == null || threadId.isEmpty() || conversation == null) {
            return;
        }
        
        recordAccess(threadId);
        conversationCache.put(threadId, conversation);
        Log.d(TAG, "Cached conversation: " + threadId + 
              " (access count: " + getAccessCount(threadId) + ")");
    }
    
    /**
     * Clears all caches and access statistics.
     */
    public void clearCache() {
        messageCache.evictAll();
        conversationCache.evictAll();
        accessStats.clear();
        totalHits.set(0);
        totalMisses.set(0);
        Log.d(TAG, "All caches and statistics cleared");
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
        accessStats.remove(threadId);
        Log.d(TAG, "Cleared cache for thread: " + threadId);
    }
    
    /**
     * Gets enhanced cache statistics with intelligent metrics.
     *
     * @return String with detailed cache performance statistics
     */
    public String getCacheStats() {
        long hits = totalHits.get();
        long misses = totalMisses.get();
        long total = hits + misses;
        double hitRate = total > 0 ? (double) hits / total * 100 : 0;
        
        int highFrequencyItems = 0;
        int recentlyAccessedItems = 0;
        
        for (AccessStats stats : accessStats.values()) {
            if (stats.isHighFrequency()) {
                highFrequencyItems++;
            }
            if (stats.isRecentlyAccessed()) {
                recentlyAccessedItems++;
            }
        }
        
        return String.format(
            "Cache Stats - Hit Rate: %.1f%% (%d/%d), Message Cache: %d entries, " +
            "Conversation Cache: %d entries, High-Freq Items: %d, Recently Accessed: %d",
            hitRate, hits, total, messageCache.size(), conversationCache.size(),
            highFrequencyItems, recentlyAccessedItems
        );
    }
    
    /**
     * Performs intelligent maintenance operations on the cache.
     * This method can be called periodically to optimize cache performance.
     */
    public void performMaintenance() {
        Log.d(TAG, "Performing intelligent cache maintenance");
        
        // Log current cache statistics
        Log.d(TAG, "Current cache stats: " + getCacheStats());
        
        // Clean up old access statistics to prevent memory leaks
        long cutoffTime = System.currentTimeMillis() - (RECENT_ACCESS_WINDOW * 2);
        accessStats.entrySet().removeIf(entry -> {
            AccessStats stats = entry.getValue();
            return !stats.isHighFrequency() && stats.lastAccessed < cutoffTime;
        });
        
        // Trim cache if memory pressure detected
        if (messageCache.size() > 0) {
            int currentSize = messageCache.size();
            messageCache.trimToSize(MAX_MEMORY_SIZE / 2);
            
            if (messageCache.size() < currentSize) {
                Log.d(TAG, "Trimmed message cache from " + currentSize + " to " + messageCache.size() + " entries");
            }
        }
        
        Log.d(TAG, "Cache maintenance completed. Cleaned " + 
              (accessStats.size()) + " access statistics entries.");
    }
    
    /**
     * Gets the cache hit rate as a percentage.
     */
    public double getCacheHitRate() {
        long hits = totalHits.get();
        long total = hits + totalMisses.get();
        return total > 0 ? (double) hits / total * 100 : 0;
    }
    
    /**
     * Gets memory usage estimate for the cache.
     */
    public int getEstimatedMemoryUsage() {
        int totalSize = 0;
        // Note: We can't directly access LruCache's size calculation, 
        // so we estimate based on stored data
        for (String key : accessStats.keySet()) {
            if (messageCache.get(key) != null) {
                CachedMessageData data = messageCache.get(key);
                if (data != null) {
                    totalSize += data.estimatedSize;
                }
            }
        }
        return totalSize;
    }
}