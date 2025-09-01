package com.translator.messagingapp;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for benchmarking cache performance.
 * This class tests various cache operations and measures their performance.
 */
public class CacheBenchmarkUtils {
    private static final String TAG = "CacheBenchmarkUtils";
    
    /**
     * Runs benchmark tests on the OptimizedMessageCache.
     * 
     * @param cache The cache to benchmark
     * @return Performance metrics as a string
     */
    public static String runBenchmarks(OptimizedMessageCache cache) {
        Log.d(TAG, "Running cache benchmarks");
        
        long startTime = System.currentTimeMillis();
        
        // Create test messages
        List<Message> testMessages = createTestMessages(100);
        
        // Test adding messages
        for (Message message : testMessages) {
            cache.addMessage("thread_1", message);
        }
        
        // Test retrieving messages
        for (Message message : testMessages) {
            cache.getMessage("thread_1", message.getId());
        }
        
        // Test updating messages
        for (Message message : testMessages) {
            cache.updateMessage("thread_1", message.getId(), message);
        }
        
        // Test removing messages
        for (Message message : testMessages) {
            cache.removeMessage("thread_1", message.getId());
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        String result = "Cache benchmark completed in " + duration + "ms for " + testMessages.size() + " messages";
        Log.d(TAG, result);
        
        return result;
    }
    
    /**
     * Creates test messages for benchmarking.
     * 
     * @param count Number of test messages to create
     * @return List of test messages
     */
    private static List<Message> createTestMessages(int count) {
        List<Message> messages = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Message message = new Message();
            message.setId(i);
            message.setOriginalText("Test message " + i);
            message.setAddress("123456789" + i);
            message.setThreadId("thread_1");
            messages.add(message);
        }
        
        return messages;
    }
}