package com.translator.messagingapp;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility class for benchmarking cache operations.
 */
public class CacheBenchmarkUtils {
    private static final String TAG = "CacheBenchmarkUtils";
    
    /**
     * Creates benchmark messages for testing cache performance.
     *
     * @param count The number of messages to create
     * @return List of benchmark messages
     */
    public static List<Message> createBenchmarkMessages(int count) {
        List<Message> messages = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < count; i++) {
            Message message = new Message();
            message.setId(i + 1L);
            message.setThreadId(random.nextInt(10) + 1L); // Random thread ID 1-10
            message.setAddress("+1555000" + String.format("%04d", random.nextInt(10000)));
            message.setBody("Benchmark message #" + (i + 1) + " - " + generateRandomText(random));
            message.setDate(System.currentTimeMillis() - random.nextInt(1000000));
            message.setType(random.nextBoolean() ? 1 : 2); // Inbox or sent
            message.setRead(random.nextBoolean());
            message.setMessageType(Message.MESSAGE_TYPE_SMS);
            
            messages.add(message);
        }
        
        Log.d(TAG, "Created " + count + " benchmark messages");
        return messages;
    }
    
    /**
     * Generates random text for benchmark messages.
     */
    private static String generateRandomText(Random random) {
        String[] words = {"hello", "world", "test", "message", "cache", "benchmark", 
                         "performance", "android", "app", "translation", "sms", "communication"};
        
        StringBuilder text = new StringBuilder();
        int wordCount = random.nextInt(10) + 5; // 5-15 words
        
        for (int i = 0; i < wordCount; i++) {
            if (i > 0) text.append(" ");
            text.append(words[random.nextInt(words.length)]);
        }
        
        return text.toString();
    }
    
    /**
     * Benchmarks cache operations and returns results.
     *
     * @param cache The cache to benchmark
     * @param messages The messages to use for benchmarking
     * @param iterations The number of iterations to run
     * @return Benchmark results
     */
    public static BenchmarkResult benchmarkCacheOperations(OptimizedMessageCache cache, 
                                                          List<Message> messages, 
                                                          int iterations) {
        if (cache == null || messages == null || messages.isEmpty()) {
            return new BenchmarkResult(0, 0, 0, 0);
        }
        
        Log.d(TAG, "Starting cache benchmark with " + messages.size() + " messages, " + iterations + " iterations");
        
        long startTime, endTime;
        long totalInsertTime = 0;
        long totalRetrieveTime = 0;
        long totalUpdateTime = 0;
        long totalDeleteTime = 0;
        
        // Benchmark insert operations
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (Message message : messages) {
                cache.addMessage(message);
            }
        }
        endTime = System.nanoTime();
        totalInsertTime = (endTime - startTime) / 1000000; // Convert to milliseconds
        
        // Benchmark retrieve operations
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (Message message : messages) {
                cache.getMessage(message.getId());
            }
        }
        endTime = System.nanoTime();
        totalRetrieveTime = (endTime - startTime) / 1000000;
        
        // Benchmark update operations
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (Message message : messages) {
                message.setBody(message.getBody() + " updated");
                cache.updateMessage(message);
            }
        }
        endTime = System.nanoTime();
        totalUpdateTime = (endTime - startTime) / 1000000;
        
        // Benchmark delete operations
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (Message message : messages) {
                cache.removeMessage(message.getId());
            }
        }
        endTime = System.nanoTime();
        totalDeleteTime = (endTime - startTime) / 1000000;
        
        BenchmarkResult result = new BenchmarkResult(totalInsertTime, totalRetrieveTime, 
                                                   totalUpdateTime, totalDeleteTime);
        
        Log.d(TAG, "Benchmark completed: " + result.toString());
        return result;
    }
    
    /**
     * Represents the results of a cache benchmark operation.
     */
    public static class BenchmarkResult {
        private final long insertTimeMs;
        private final long retrieveTimeMs;
        private final long updateTimeMs;
        private final long deleteTimeMs;
        
        public BenchmarkResult(long insertTimeMs, long retrieveTimeMs, 
                             long updateTimeMs, long deleteTimeMs) {
            this.insertTimeMs = insertTimeMs;
            this.retrieveTimeMs = retrieveTimeMs;
            this.updateTimeMs = updateTimeMs;
            this.deleteTimeMs = deleteTimeMs;
        }
        
        public long getInsertTimeMs() { return insertTimeMs; }
        public long getRetrieveTimeMs() { return retrieveTimeMs; }
        public long getUpdateTimeMs() { return updateTimeMs; }
        public long getDeleteTimeMs() { return deleteTimeMs; }
        
        public long getTotalTimeMs() {
            return insertTimeMs + retrieveTimeMs + updateTimeMs + deleteTimeMs;
        }
        
        @Override
        public String toString() {
            return "BenchmarkResult{" +
                    "insertTime=" + insertTimeMs + "ms, " +
                    "retrieveTime=" + retrieveTimeMs + "ms, " +
                    "updateTime=" + updateTimeMs + "ms, " +
                    "deleteTime=" + deleteTimeMs + "ms, " +
                    "totalTime=" + getTotalTimeMs() + "ms}";
        }
    }
}