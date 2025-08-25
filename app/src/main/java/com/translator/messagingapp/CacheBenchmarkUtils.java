package com.translator.messagingapp;

import android.util.Log;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Utility class for benchmarking cache performance.
 * Provides methods to compare different cache implementations and measure their efficiency.
 */
public class CacheBenchmarkUtils {
    
    private static final String TAG = "CacheBenchmarkUtils";
    
    /**
     * Data class to hold cache comparison results.
     */
    public static class CacheComparison {
        private final String cacheName;
        private final long hitCount;
        private final long missCount;
        private final double hitRatio;
        private final long averageResponseTime;
        
        public CacheComparison(String cacheName, long hitCount, long missCount, long averageResponseTime) {
            this.cacheName = cacheName;
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.hitRatio = (hitCount + missCount) > 0 ? (double) hitCount / (hitCount + missCount) : 0.0;
            this.averageResponseTime = averageResponseTime;
        }
        
        public String getCacheName() { return cacheName; }
        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public double getHitRatio() { return hitRatio; }
        public long getAverageResponseTime() { return averageResponseTime; }
        
        @Override
        public String toString() {
            return String.format("Cache: %s, Hits: %d, Misses: %d, Hit Ratio: %.2f%%, Avg Response: %dms", 
                    cacheName, hitCount, missCount, hitRatio * 100, averageResponseTime);
        }
    }
    
    /**
     * Interface for cache implementations to be benchmarked.
     */
    public interface CacheImplementation {
        String getName();
        Object get(String key);
        void put(String key, Object value);
        void clear();
        long getHitCount();
        long getMissCount();
    }
    
    /**
     * Wrapper for OptimizedMessageCache to make it compatible with benchmarking.
     */
    public static class OptimizedMessageCacheWrapper implements CacheImplementation {
        private final OptimizedMessageCache cache;
        private long hitCount = 0;
        private long missCount = 0;
        
        public OptimizedMessageCacheWrapper(OptimizedMessageCache cache) {
            this.cache = cache;
        }
        
        @Override
        public String getName() {
            return "OptimizedMessageCache";
        }
        
        @Override
        public Object get(String key) {
            List<Message> result = cache.getCachedMessages(key);
            if (result != null) {
                hitCount++;
                return result;
            } else {
                missCount++;
                return null;
            }
        }
        
        @Override
        public void put(String key, Object value) {
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Message> messages = (List<Message>) value;
                cache.cacheMessages(key, messages);
            }
        }
        
        @Override
        public void clear() {
            cache.clearCache();
        }
        
        @Override
        public long getHitCount() {
            return hitCount;
        }
        
        @Override
        public long getMissCount() {
            return missCount;
        }
    }
    
    /**
     * Wrapper for MessageCache to make it compatible with benchmarking.
     */
    public static class MessageCacheWrapper implements CacheImplementation {
        private long hitCount = 0;
        private long missCount = 0;
        
        @Override
        public String getName() {
            return "MessageCache";
        }
        
        @Override
        public Object get(String key) {
            List<Message> result = MessageCache.getCachedMessages(key);
            if (result != null) {
                hitCount++;
                return result;
            } else {
                missCount++;
                return null;
            }
        }
        
        @Override
        public void put(String key, Object value) {
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Message> messages = (List<Message>) value;
                MessageCache.cacheMessages(key, messages);
            }
        }
        
        @Override
        public void clear() {
            MessageCache.clearCache();
        }
        
        @Override
        public long getHitCount() {
            return hitCount;
        }
        
        @Override
        public long getMissCount() {
            return missCount;
        }
    }
    
    /**
     * Benchmark data class for test operations.
     */
    public static class BenchmarkData {
        private final List<String> testKeys;
        private final List<Object> testValues;
        
        public BenchmarkData(List<String> testKeys, List<Object> testValues) {
            this.testKeys = testKeys;
            this.testValues = testValues;
        }
        
        public List<String> getTestKeys() { return testKeys; }
        public List<Object> getTestValues() { return testValues; }
    }
    
    /**
     * Compares cache implementations using provided test data.
     * This method fixes the syntax error from line 229.
     *
     * @param implementations List of cache implementations to compare
     * @param testData Benchmark data to use for testing
     * @return List of comparison results for each implementation
     */
    public static List<CacheComparison> compareCacheImplementations(List<CacheImplementation> implementations, BenchmarkData testData) {
        List<CacheComparison> results = new ArrayList<>();
        
        if (implementations == null || implementations.isEmpty()) {
            Log.w(TAG, "No cache implementations provided for comparison");
            return results;
        }
        
        if (testData == null || testData.getTestKeys().isEmpty()) {
            Log.w(TAG, "No test data provided for benchmark");
            return results;
        }
        
        Log.d(TAG, "Starting cache benchmark with " + implementations.size() + " implementations");
        
        for (CacheImplementation impl : implementations) {
            if (impl == null) continue;
            
            Log.d(TAG, "Benchmarking: " + impl.getName());
            
            // Clear cache before testing
            impl.clear();
            
            // Measure performance
            long startTime = System.currentTimeMillis();
            
            // First pass: populate cache
            List<String> keys = testData.getTestKeys();
            List<Object> values = testData.getTestValues();
            
            for (int i = 0; i < keys.size() && i < values.size(); i++) {
                impl.put(keys.get(i), values.get(i));
            }
            
            // Second pass: test cache hits
            for (String key : keys) {
                impl.get(key);
            }
            
            // Third pass: test cache misses with different keys
            for (int i = 0; i < keys.size(); i++) {
                impl.get("miss_" + keys.get(i));
            }
            
            long endTime = System.currentTimeMillis();
            long averageResponseTime = (endTime - startTime) / Math.max(1, keys.size() * 2);
            
            CacheComparison comparison = new CacheComparison(
                impl.getName(),
                impl.getHitCount(),
                impl.getMissCount(),
                averageResponseTime
            );
            
            results.add(comparison);
            Log.d(TAG, "Benchmark result: " + comparison.toString());
        }
        
        return results;
    }
    
    /**
     * Creates sample test data for benchmarking.
     *
     * @param size Number of test entries to create
     * @return BenchmarkData object with sample keys and values
     */
    public static BenchmarkData createSampleTestData(int size) {
        List<String> keys = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        
        for (int i = 0; i < size; i++) {
            keys.add("thread_" + i);
            
            // Create sample messages for testing
            List<Message> messages = new ArrayList<>();
            for (int j = 0; j < 5; j++) {
                Message msg = new Message();
                msg.setId("msg_" + i + "_" + j);
                msg.setBody("Sample message " + j + " for thread " + i);
                msg.setThreadId("thread_" + i);
                msg.setDate(System.currentTimeMillis() - (j * 1000));
                messages.add(msg);
            }
            values.add(messages);
        }
        
        return new BenchmarkData(keys, values);
    }
    
    /**
     * Prints benchmark results in a formatted way.
     *
     * @param results List of cache comparison results
     */
    public static void printBenchmarkResults(List<CacheComparison> results) {
        if (results == null || results.isEmpty()) {
            Log.i(TAG, "No benchmark results to display");
            return;
        }
        
        Log.i(TAG, "=== Cache Benchmark Results ===");
        for (CacheComparison result : results) {
            Log.i(TAG, result.toString());
        }
        
        // Find best performing cache
        CacheComparison best = results.get(0);
        for (CacheComparison result : results) {
            if (result.getHitRatio() > best.getHitRatio() || 
                (result.getHitRatio() == best.getHitRatio() && result.getAverageResponseTime() < best.getAverageResponseTime())) {
                best = result;
            }
        }
        
        Log.i(TAG, "Best performing cache: " + best.getCacheName());
        Log.i(TAG, "=== End Benchmark Results ===");
    }
    
    /**
     * Runs a complete benchmark comparison between available cache implementations.
     *
     * @param testDataSize Number of test entries to create
     * @return Map of cache names to their comparison results
     */
    public static Map<String, CacheComparison> runCompleteBenchmark(int testDataSize) {
        Log.d(TAG, "Starting complete cache benchmark with " + testDataSize + " test entries");
        
        // Create test data
        BenchmarkData testData = createSampleTestData(testDataSize);
        
        // Setup cache implementations
        List<CacheImplementation> implementations = new ArrayList<>();
        
        // Add MessageCache wrapper
        implementations.add(new MessageCacheWrapper());
        
        // Add OptimizedMessageCache wrapper if available
        try {
            OptimizedMessageCache optimizedCache = new OptimizedMessageCache();
            implementations.add(new OptimizedMessageCacheWrapper(optimizedCache));
        } catch (Exception e) {
            Log.w(TAG, "OptimizedMessageCache not available for benchmarking", e);
        }
        
        // Run benchmark
        List<CacheComparison> results = compareCacheImplementations(implementations, testData);
        
        // Print results
        printBenchmarkResults(results);
        
        // Convert to map for easy access
        Map<String, CacheComparison> resultMap = new HashMap<>();
        for (CacheComparison result : results) {
            resultMap.put(result.getCacheName(), result);
        }
        
        Log.d(TAG, "Complete benchmark finished");
        return resultMap;
    }
}