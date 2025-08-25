package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Enhanced message service with additional functionality and performance optimizations.
 */
public class EnhancedMessageService extends MessageService {
    private static final String TAG = "EnhancedMessageService";
    
    private final OptimizedMessageCache optimizedCache;
    private final ExecutorService backgroundExecutor;
    
    /**
     * Creates a new EnhancedMessageService.
     *
     * @param context The application context
     * @param translationManager The translation manager
     */
    public EnhancedMessageService(Context context, TranslationManager translationManager) {
        super(context, translationManager);
        this.optimizedCache = new OptimizedMessageCache(context);
        this.backgroundExecutor = Executors.newFixedThreadPool(2);
    }
    
    /**
     * Gets messages by thread ID (enhanced version with caching).
     *
     * @param threadId The thread ID
     * @return List of messages for the thread
     */
    @Override
    public List<Message> getMessagesByThreadId(String threadId) {
        try {
            // First try to get from cache
            List<Message> cachedMessages = optimizedCache.getMessagesForThread(Long.parseLong(threadId));
            if (cachedMessages != null && !cachedMessages.isEmpty()) {
                Log.d(TAG, "Retrieved " + cachedMessages.size() + " messages from cache for thread " + threadId);
                return cachedMessages;
            }
            
            // Fallback to parent implementation
            List<Message> messages = super.getMessagesByThreadId(threadId);
            
            // Cache the results for future use
            if (messages != null && !messages.isEmpty()) {
                backgroundExecutor.execute(() -> {
                    for (Message message : messages) {
                        optimizedCache.addMessage(message);
                    }
                });
            }
            
            return messages;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting enhanced messages for thread " + threadId, e);
            return super.getMessagesByThreadId(threadId);
        }
    }
    
    /**
     * Gets messages by thread ID with pagination (enhanced version).
     *
     * @param threadId The thread ID
     * @param offset The offset
     * @param limit The limit
     * @return List of messages
     */
    @Override
    public List<Message> getMessagesByThreadIdPaginated(String threadId, int offset, int limit) {
        try {
            return super.getMessagesByThreadIdPaginated(threadId, offset, limit);
        } catch (Exception e) {
            Log.e(TAG, "Error getting paginated messages for thread " + threadId, e);
            return super.getMessagesByThreadIdPaginated(threadId, offset, limit);
        }
    }
    
    /**
     * Performs cache benchmark operations for testing performance.
     *
     * @param messageCount The number of messages to create for benchmarking
     * @return Benchmark results
     */
    public CacheBenchmarkUtils.BenchmarkResult performCacheBenchmark(int messageCount) {
        try {
            // Create benchmark messages
            List<Message> benchmarkMessages = CacheBenchmarkUtils.createBenchmarkMessages(messageCount);
            
            // Run benchmark
            CacheBenchmarkUtils.BenchmarkResult result = CacheBenchmarkUtils.benchmarkCacheOperations(
                optimizedCache, benchmarkMessages, 1);
                
            Log.d(TAG, "Cache benchmark completed: " + result.toString());
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Error performing cache benchmark", e);
            return new CacheBenchmarkUtils.BenchmarkResult(0, 0, 0, 0);
        }
    }
    
    /**
     * Clears the optimized cache.
     */
    public void clearCache() {
        try {
            optimizedCache.clear();
            Log.d(TAG, "Enhanced message service cache cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing cache", e);
        }
    }
    
    /**
     * Gets cache statistics.
     *
     * @return String describing cache statistics
     */
    public String getCacheStats() {
        try {
            return optimizedCache.getStats();
        } catch (Exception e) {
            Log.e(TAG, "Error getting cache stats", e);
            return "Cache stats unavailable";
        }
    }
    
    /**
     * Cleanup method to release resources.
     */
    public void cleanup() {
        try {
            if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
                backgroundExecutor.shutdown();
            }
            if (optimizedCache != null) {
                optimizedCache.clear();
            }
            Log.d(TAG, "Enhanced message service cleanup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }
}