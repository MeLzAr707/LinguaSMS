package com.translator.messagingapp;

import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Performance benchmarking utility for measuring cache performance improvements.
 * Provides metrics and comparison tools to validate the enhanced caching system.
 */
public class CacheBenchmarkUtils {
    private static final String TAG = "CacheBenchmark";
    
    /**
     * Benchmark results container
     */
    public static class BenchmarkResult {
        public final String testName;
        public final long startTime;
        public final long endTime;
        public final long duration;
        public final Map<String, Object> metrics;
        
        public BenchmarkResult(String testName, long startTime, long endTime, Map<String, Object> metrics) {
            this.testName = testName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.duration = endTime - startTime;
            this.metrics = metrics != null ? new HashMap<>(metrics) : new HashMap<>();
        }
        
        public long getDurationMs() {
            return duration;
        }
        
        public String getFormattedDuration() {
            if (duration < 1000) {
                return duration + "ms";
            } else {
                return String.format("%.2fs", duration / 1000.0);
            }
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Benchmark: ").append(testName).append("\n");
            sb.append("Duration: ").append(getFormattedDuration()).append("\n");
            
            for (Map.Entry<String, Object> entry : metrics.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            
            return sb.toString();
        }
    }
    
    /**
     * Performance comparison between old and new cache implementations
     */
    public static class CacheComparison {
        public final BenchmarkResult oldCacheResult;
        public final BenchmarkResult newCacheResult;
        public final double performanceImprovement;
        
        public CacheComparison(BenchmarkResult oldResult, BenchmarkResult newResult) {
            this.oldCacheResult = oldResult;
            this.newCacheResult = newResult;
            
            if (oldResult.duration > 0) {
                this.performanceImprovement = ((double) (oldResult.duration - newResult.duration)) / oldResult.duration * 100.0;
            } else {
                this.performanceImprovement = 0.0;
            }
        }
        
        public boolean isImprovement() {
            return performanceImprovement > 0;
        }
        
        public String getImprovementDescription() {
            if (performanceImprovement > 0) {
                return String.format("%.1f%% faster", performanceImprovement);
            } else if (performanceImprovement < 0) {
                return String.format("%.1f%% slower", Math.abs(performanceImprovement));
            } else {
                return "No significant change";
            }
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Cache Performance Comparison\n");
            sb.append("============================\n");
            sb.append("Old Cache: ").append(oldCacheResult.getFormattedDuration()).append("\n");
            sb.append("New Cache: ").append(newCacheResult.getFormattedDuration()).append("\n");
            sb.append("Improvement: ").append(getImprovementDescription()).append("\n");
            return sb.toString();
        }
    }
    
    /**
     * Benchmarks the performance of message caching operations.
     */
    public static BenchmarkResult benchmarkCacheOperations(OptimizedMessageCache cache, List<Message> testMessages, int iterations) {
        Map<String, Object> metrics = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        int cacheHits = 0;
        int cacheMisses = 0;
        
        try {
            // Warm up the cache
            for (int i = 0; i < Math.min(iterations / 10, 10); i++) {
                String threadId = "warmup_" + i;
                cache.cacheMessages(threadId, testMessages);
                cache.getCachedMessages(threadId);
            }
            
            // Start actual benchmark
            long benchmarkStart = System.currentTimeMillis();
            
            for (int i = 0; i < iterations; i++) {
                String threadId = "thread_" + (i % 20); // Reuse some thread IDs to test cache hits
                
                // Cache messages
                cache.cacheMessages(threadId, testMessages);
                
                // Retrieve messages (should hit cache)
                List<Message> retrieved = cache.getCachedMessages(threadId);
                if (retrieved != null) {
                    cacheHits++;
                } else {
                    cacheMisses++;
                }
                
                // Simulate some cache misses
                if (i % 10 == 0) {
                    cache.getCachedMessages("nonexistent_" + i);
                    cacheMisses++;
                }
            }
            
            long endTime = System.currentTimeMillis();
            
            // Collect metrics
            metrics.put("iterations", iterations);
            metrics.put("cache_hits", cacheHits);
            metrics.put("cache_misses", cacheMisses);
            metrics.put("hit_rate", cacheHits > 0 ? (double) cacheHits / (cacheHits + cacheMisses) * 100 : 0);
            metrics.put("messages_per_operation", testMessages.size());
            metrics.put("operations_per_second", iterations / ((endTime - benchmarkStart) / 1000.0));
            
            return new BenchmarkResult("Cache Operations", benchmarkStart, endTime, metrics);
            
        } catch (Exception e) {
            Log.e(TAG, "Error during cache benchmark", e);
            metrics.put("error", e.getMessage());
            return new BenchmarkResult("Cache Operations (Error)", startTime, System.currentTimeMillis(), metrics);
        }
    }
    
    /**
     * Benchmarks compression performance.
     */
    public static BenchmarkResult benchmarkCompression(List<Message> testMessages, int iterations) {
        Map<String, Object> metrics = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        int totalCompressed = 0;
        int totalDecompressed = 0;
        long totalCompressionTime = 0;
        long totalDecompressionTime = 0;
        int totalOriginalSize = 0;
        int totalCompressedSize = 0;
        
        try {
            for (int i = 0; i < iterations; i++) {
                // Compression benchmark
                long compStart = System.currentTimeMillis();
                CacheCompressionUtils.CompressedMessageData compressed = 
                    CacheCompressionUtils.compressMessages(testMessages);
                long compEnd = System.currentTimeMillis();
                
                if (compressed != null) {
                    totalCompressed++;
                    totalCompressionTime += (compEnd - compStart);
                    totalOriginalSize += compressed.originalSize;
                    totalCompressedSize += compressed.compressedSize;
                    
                    // Decompression benchmark
                    long decompStart = System.currentTimeMillis();
                    List<Message> decompressed = CacheCompressionUtils.decompressMessages(compressed);
                    long decompEnd = System.currentTimeMillis();
                    
                    if (decompressed != null && decompressed.size() == testMessages.size()) {
                        totalDecompressed++;
                        totalDecompressionTime += (decompEnd - decompStart);
                    }
                }
            }
            
            long endTime = System.currentTimeMillis();
            
            // Calculate metrics
            metrics.put("iterations", iterations);
            metrics.put("successful_compressions", totalCompressed);
            metrics.put("successful_decompressions", totalDecompressed);
            metrics.put("avg_compression_time_ms", totalCompressed > 0 ? totalCompressionTime / totalCompressed : 0);
            metrics.put("avg_decompression_time_ms", totalDecompressed > 0 ? totalDecompressionTime / totalDecompressed : 0);
            metrics.put("avg_compression_ratio", totalOriginalSize > 0 ? (double) totalCompressedSize / totalOriginalSize * 100 : 0);
            metrics.put("avg_space_saved_bytes", totalCompressed > 0 ? (totalOriginalSize - totalCompressedSize) / totalCompressed : 0);
            
            return new BenchmarkResult("Compression Performance", startTime, endTime, metrics);
            
        } catch (Exception e) {
            Log.e(TAG, "Error during compression benchmark", e);
            metrics.put("error", e.getMessage());
            return new BenchmarkResult("Compression Performance (Error)", startTime, System.currentTimeMillis(), metrics);
        }
    }
    
    /**
     * Compares performance between old MessageCache and new OptimizedMessageCache.
     */
    public static CacheComparison compareCache implementations(List<Message> testMessages, int iterations) {
        Log.d(TAG, "Starting cache comparison benchmark");
        
        // Benchmark old cache (using static methods)
        BenchmarkResult oldResult = benchmarkOldCache(testMessages, iterations);
        
        // Benchmark new cache
        OptimizedMessageCache newCache = new OptimizedMessageCache();
        BenchmarkResult newResult = benchmarkCacheOperations(newCache, testMessages, iterations);
        
        CacheComparison comparison = new CacheComparison(oldResult, newResult);
        Log.d(TAG, "Cache comparison completed: " + comparison.getImprovementDescription());
        
        return comparison;
    }
    
    /**
     * Benchmarks the old MessageCache implementation for comparison.
     */
    private static BenchmarkResult benchmarkOldCache(List<Message> testMessages, int iterations) {
        Map<String, Object> metrics = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        int cacheHits = 0;
        int cacheMisses = 0;
        
        try {
            // Clear old cache first
            MessageCache.clearCache();
            
            for (int i = 0; i < iterations; i++) {
                String threadId = "thread_" + (i % 20);
                
                // Cache messages using old cache
                MessageCache.cacheMessages(threadId, testMessages);
                
                // Retrieve messages
                List<Message> retrieved = MessageCache.getCachedMessages(threadId);
                if (retrieved != null) {
                    cacheHits++;
                } else {
                    cacheMisses++;
                }
                
                // Simulate cache misses
                if (i % 10 == 0) {
                    MessageCache.getCachedMessages("nonexistent_" + i);
                    cacheMisses++;
                }
            }
            
            long endTime = System.currentTimeMillis();
            
            metrics.put("iterations", iterations);
            metrics.put("cache_hits", cacheHits);
            metrics.put("cache_misses", cacheMisses);
            metrics.put("hit_rate", cacheHits > 0 ? (double) cacheHits / (cacheHits + cacheMisses) * 100 : 0);
            metrics.put("operations_per_second", iterations / ((endTime - startTime) / 1000.0));
            
            return new BenchmarkResult("Old Cache Operations", startTime, endTime, metrics);
            
        } catch (Exception e) {
            Log.e(TAG, "Error during old cache benchmark", e);
            metrics.put("error", e.getMessage());
            return new BenchmarkResult("Old Cache Operations (Error)", startTime, System.currentTimeMillis(), metrics);
        }
    }
    
    /**
     * Creates test messages for benchmarking.
     */
    public static List<Message> createBenchmarkMessages(int count) {
        List<Message> messages = new ArrayList<>();
        long baseTime = System.currentTimeMillis();
        
        for (int i = 0; i < count; i++) {
            Message message = new Message();
            message.setId(i + 1L);
            message.setBody("Benchmark message " + (i + 1) + " with substantial content to simulate real-world message sizes and complexity");
            message.setAddress("+1234567890");
            message.setDate(baseTime + i * 1000);
            message.setType(i % 2 == 0 ? Message.TYPE_INBOX : Message.TYPE_SENT);
            message.setThreadId(123L + (i % 10)); // Distribute across multiple threads
            message.setContactName("Benchmark Contact " + (i % 5));
            message.setRead(i % 3 == 0);
            messages.add(message);
        }
        
        return messages;
    }
    
    /**
     * Runs a comprehensive benchmark suite and logs results.
     */
    public static void runBenchmarkSuite() {
        Log.d(TAG, "Starting comprehensive cache benchmark suite");
        
        List<Message> testMessages = createBenchmarkMessages(100);
        
        // Test cache operations
        OptimizedMessageCache cache = new OptimizedMessageCache();
        BenchmarkResult cacheResult = benchmarkCacheOperations(cache, testMessages, 1000);
        Log.d(TAG, "Cache Operations Benchmark:\n" + cacheResult.toString());
        
        // Test compression
        BenchmarkResult compressionResult = benchmarkCompression(testMessages, 100);
        Log.d(TAG, "Compression Benchmark:\n" + compressionResult.toString());
        
        // Compare cache implementations
        CacheComparison comparison = compareCache implementations(testMessages, 500);
        Log.d(TAG, "Cache Comparison:\n" + comparison.toString());
        
        Log.d(TAG, "Benchmark suite completed");
    }
}