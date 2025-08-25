package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;
import java.util.List;

/**
 * Enhanced message service that integrates the improved caching system
 * with intelligent caching, compression, and background loading capabilities.
 * 
 * This class demonstrates how to integrate the new caching features with
 * the existing MessageService for optimal performance.
 */
public class EnhancedMessageService {
    private static final String TAG = "EnhancedMessageService";
    
    private final Context context;
    private final MessageService messageService;
    private final OptimizedMessageCache optimizedCache;
    private final BackgroundMessageLoader backgroundLoader;
    
    // Performance tracking
    private long cacheHits = 0;
    private long cacheMisses = 0;
    private long totalRequestTime = 0;
    private int totalRequests = 0;
    
    public EnhancedMessageService(Context context, MessageService messageService) {
        this.context = context;
        this.messageService = messageService;
        this.optimizedCache = new OptimizedMessageCache(context);
        this.backgroundLoader = new BackgroundMessageLoader(context, messageService, optimizedCache);
        
        // Enable background loading by default
        backgroundLoader.setEnabled(true);
        
        Log.d(TAG, "Enhanced message service initialized with intelligent caching");
    }
    
    /**
     * Gets messages for a thread with intelligent caching and background loading.
     * This method provides the best performance by utilizing all caching improvements.
     */
    public List<Message> getMessagesWithEnhancedCaching(String threadId) {
        if (threadId == null || threadId.isEmpty()) {
            return null;
        }
        
        long startTime = System.currentTimeMillis();
        totalRequests++;
        
        try {
            // First, try to get messages from the intelligent cache
            List<Message> cachedMessages = optimizedCache.getCachedMessages(threadId);
            
            if (cachedMessages != null) {
                cacheHits++;
                long responseTime = System.currentTimeMillis() - startTime;
                totalRequestTime += responseTime;
                
                Log.d(TAG, "Cache hit for thread " + threadId + " (" + responseTime + "ms)");
                
                // Schedule background prefetch for older messages
                backgroundLoader.scheduleMessagePrefetch(threadId, cachedMessages.size());
                
                return cachedMessages;
            }
            
            // Cache miss - load from database
            cacheMisses++;
            Log.d(TAG, "Cache miss for thread " + threadId + ", loading from database");
            
            List<Message> messages = messageService.getMessagesByThreadId(threadId);
            
            if (messages != null && !messages.isEmpty()) {
                // Cache the messages with intelligent prioritization
                optimizedCache.cacheMessages(threadId, messages);
                
                // Schedule background prefetch for additional pages
                backgroundLoader.scheduleMessagePrefetch(threadId, messages.size());
                
                Log.d(TAG, "Loaded and cached " + messages.size() + " messages for thread " + threadId);
            }
            
            long responseTime = System.currentTimeMillis() - startTime;
            totalRequestTime += responseTime;
            
            return messages;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting messages for thread " + threadId, e);
            return null;
        }
    }
    
    /**
     * Preloads recent conversations in the background for faster startup.
     */
    public void preloadRecentConversations() {
        Log.d(TAG, "Starting preload of recent conversations");
        backgroundLoader.prefetchRecentConversations();
    }
    
    /**
     * Gets enhanced cache performance statistics.
     */
    public String getPerformanceStats() {
        double hitRate = totalRequests > 0 ? (double) cacheHits / totalRequests * 100 : 0;
        double avgResponseTime = totalRequests > 0 ? (double) totalRequestTime / totalRequests : 0;
        
        return String.format(
            "Enhanced Cache Performance:\n" +
            "Hit Rate: %.1f%% (%d/%d)\n" +
            "Avg Response Time: %.1fms\n" +
            "Background Prefetch Active: %d operations\n" +
            "Cache Details: %s",
            hitRate, cacheHits, totalRequests,
            avgResponseTime,
            backgroundLoader.getActivePrefetchCount(),
            optimizedCache.getCacheStats()
        );
    }
    
    /**
     * Performs comprehensive cache maintenance and optimization.
     */
    public void performMaintenance() {
        Log.d(TAG, "Performing enhanced cache maintenance");
        
        // Perform cache maintenance
        optimizedCache.performMaintenance();
        
        // Log performance statistics
        Log.d(TAG, getPerformanceStats());
        
        // Optional: Run benchmark to monitor performance
        if (BuildConfig.DEBUG) {
            runPerformanceBenchmark();
        }
    }
    
    /**
     * Runs a quick performance benchmark in debug builds.
     */
    private void runPerformanceBenchmark() {
        try {
            List<Message> benchmarkMessages = CacheBenchmarkUtils.createBenchmarkMessages(50);
            CacheBenchmarkUtils.BenchmarkResult result = 
                CacheBenchmarkUtils.benchmarkCacheOperations(optimizedCache, benchmarkMessages, 100);
            
            Log.d(TAG, "Quick benchmark completed: " + result.getFormattedDuration());
            
        } catch (Exception e) {
            Log.e(TAG, "Error during performance benchmark", e);
        }
    }
    
    /**
     * Enables or disables background prefetching.
     */
    public void setBackgroundPrefetchEnabled(boolean enabled) {
        backgroundLoader.setEnabled(enabled);
        Log.d(TAG, "Background prefetching " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Gets the current cache hit rate as a percentage.
     */
    public double getCacheHitRate() {
        return totalRequests > 0 ? (double) cacheHits / totalRequests * 100 : 0;
    }
    
    /**
     * Gets the optimized cache instance for direct access if needed.
     */
    public OptimizedMessageCache getOptimizedCache() {
        return optimizedCache;
    }
    
    /**
     * Gets the background loader instance for direct control if needed.
     */
    public BackgroundMessageLoader getBackgroundLoader() {
        return backgroundLoader;
    }
    
    /**
     * Clears all caches and resets statistics.
     */
    public void clearCache() {
        optimizedCache.clearCache();
        cacheHits = 0;
        cacheMisses = 0;
        totalRequestTime = 0;
        totalRequests = 0;
        
        Log.d(TAG, "All caches cleared and statistics reset");
    }
    
    /**
     * Shuts down the enhanced message service and cleans up resources.
     */
    public void shutdown() {
        backgroundLoader.shutdown();
        optimizedCache.clearCache();
        
        Log.d(TAG, "Enhanced message service shut down");
    }
    
    /**
     * Demonstrates how to use compression for large message sets.
     */
    public void demonstrateCompression(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        
        Log.d(TAG, "Demonstrating compression with " + messages.size() + " messages");
        
        // Compress the messages
        CacheCompressionUtils.CompressedMessageData compressed = 
            CacheCompressionUtils.compressMessages(messages);
        
        // Log compression statistics
        String compressionStats = CacheCompressionUtils.getCompressionStats(compressed);
        Log.d(TAG, "Compression result: " + compressionStats);
        
        // Decompress and verify integrity
        List<Message> decompressed = CacheCompressionUtils.decompressMessages(compressed);
        
        if (decompressed != null && decompressed.size() == messages.size()) {
            Log.d(TAG, "Compression/decompression successful - data integrity verified");
        } else {
            Log.w(TAG, "Compression/decompression failed - data integrity check failed");
        }
    }
}