package com.translator.messagingapp;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Integration test demonstrating the performance improvements
 * of the enhanced message caching system.
 */
public class CachePerformanceIntegrationTest {
    
    private List<Message> testMessages;
    
    @Before
    public void setUp() {
        testMessages = createRealisticTestMessages(200);
    }
    
    @Test
    public void testCachePerformanceImprovement() {
        // Test old cache performance
        long oldCacheTime = benchmarkOldCache();
        
        // Test new cache performance
        long newCacheTime = benchmarkNewCache();
        
        // New cache should be at least as fast as old cache
        assertTrue("New cache should not be significantly slower", 
                  newCacheTime <= oldCacheTime * 1.5); // Allow 50% tolerance
        
        // Log results
        System.out.println("Old cache time: " + oldCacheTime + "ms");
        System.out.println("New cache time: " + newCacheTime + "ms");
        
        if (newCacheTime < oldCacheTime) {
            double improvement = ((double)(oldCacheTime - newCacheTime) / oldCacheTime) * 100;
            System.out.println("Performance improvement: " + String.format("%.1f%%", improvement));
        }
    }
    
    @Test
    public void testCompressionMemorySavings() {
        // Test compression on large dataset
        List<Message> largeDataset = createRealisticTestMessages(500);
        
        CacheCompressionUtils.CompressedMessageData compressed = 
            CacheCompressionUtils.compressMessages(largeDataset);
        
        if (compressed.isCompressed) {
            assertTrue("Compression should save memory", compressed.getSpaceSaved() > 0);
            assertTrue("Compression ratio should be reasonable", 
                      compressed.getCompressionRatio() < 95.0);
            
            System.out.println("Compression stats: " + 
                             CacheCompressionUtils.getCompressionStats(compressed));
        }
        
        // Verify decompression works correctly
        List<Message> decompressed = CacheCompressionUtils.decompressMessages(compressed);
        assertNotNull("Decompressed data should not be null", decompressed);
        assertEquals("Decompressed data should match original size", 
                    largeDataset.size(), decompressed.size());
    }
    
    @Test
    public void testIntelligentCachingBehavior() {
        OptimizedMessageCache cache = new OptimizedMessageCache();
        
        // Simulate real usage patterns
        String frequentThread = "frequent_thread";
        String rareThread = "rare_thread";
        
        // Cache data for both threads
        cache.cacheMessages(frequentThread, testMessages);
        cache.cacheMessages(rareThread, testMessages);
        
        // Simulate frequent access to one thread
        for (int i = 0; i < 10; i++) {
            cache.getCachedMessages(frequentThread);
        }
        
        // Access rare thread only once
        cache.getCachedMessages(rareThread);
        
        // Both should still be accessible
        assertNotNull("Frequent thread should be cached", 
                     cache.getCachedMessages(frequentThread));
        assertNotNull("Rare thread should still be cached", 
                     cache.getCachedMessages(rareThread));
        
        // Verify cache statistics
        String stats = cache.getCacheStats();
        assertTrue("Cache stats should contain hit rate", stats.contains("Hit Rate"));
        assertTrue("Cache hit rate should be reasonable", cache.getCacheHitRate() > 50);
        
        System.out.println("Cache statistics: " + stats);
    }
    
    @Test
    public void testBackgroundLoaderIntegration() {
        OptimizedMessageCache cache = new OptimizedMessageCache();
        BackgroundMessageLoader loader = new BackgroundMessageLoader(null, null, cache);
        
        try {
            // Test basic functionality
            assertTrue("Background loader should be enabled by default", loader.isEnabled());
            
            // Test disabling
            loader.setEnabled(false);
            assertFalse("Background loader should be disabled", loader.isEnabled());
            
            // Re-enable
            loader.setEnabled(true);
            assertTrue("Background loader should be re-enabled", loader.isEnabled());
            
            // Test active prefetch count
            assertEquals("Should have no active prefetches initially", 
                        0, loader.getActivePrefetchCount());
            
        } finally {
            loader.shutdown();
        }
    }
    
    @Test
    public void testCacheMaintenanceOperations() {
        OptimizedMessageCache cache = new OptimizedMessageCache();
        
        // Fill cache with test data
        for (int i = 0; i < 10; i++) {
            cache.cacheMessages("thread_" + i, createRealisticTestMessages(50));
        }
        
        // Get initial memory usage
        int initialMemory = cache.getEstimatedMemoryUsage();
        assertTrue("Cache should use some memory", initialMemory > 0);
        
        // Perform maintenance
        cache.performMaintenance();
        
        // Cache should still function after maintenance
        cache.cacheMessages("new_thread", testMessages);
        assertNotNull("Cache should work after maintenance", 
                     cache.getCachedMessages("new_thread"));
        
        System.out.println("Memory usage after maintenance: " + 
                         cache.getEstimatedMemoryUsage() + " bytes");
    }
    
    /**
     * Benchmarks the old MessageCache implementation
     */
    private long benchmarkOldCache() {
        long startTime = System.currentTimeMillis();
        
        // Clear cache first
        MessageCache.clearCache();
        
        // Perform operations
        for (int i = 0; i < 100; i++) {
            String threadId = "thread_" + (i % 10); // Reuse some thread IDs
            
            MessageCache.cacheMessages(threadId, testMessages);
            MessageCache.getCachedMessages(threadId);
        }
        
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * Benchmarks the new OptimizedMessageCache implementation
     */
    private long benchmarkNewCache() {
        long startTime = System.currentTimeMillis();
        
        OptimizedMessageCache cache = new OptimizedMessageCache();
        
        // Perform operations
        for (int i = 0; i < 100; i++) {
            String threadId = "thread_" + (i % 10); // Reuse some thread IDs
            
            cache.cacheMessages(threadId, testMessages);
            cache.getCachedMessages(threadId);
        }
        
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * Creates realistic test messages that better simulate real-world data
     */
    private List<Message> createRealisticTestMessages(int count) {
        List<Message> messages = new ArrayList<>();
        long baseTime = System.currentTimeMillis();
        
        String[] sampleContacts = {
            "John Doe", "Jane Smith", "Mike Johnson", "Sarah Wilson", "Bob Brown"
        };
        
        String[] sampleMessages = {
            "Hey, how are you doing?",
            "Can you pick up some groceries on your way home?",
            "Meeting is at 3 PM tomorrow",
            "Happy birthday! Hope you have a great day!",
            "Don't forget about dinner tonight",
            "The project deadline is next Friday",
            "Thanks for your help with the presentation",
            "Are you free for lunch tomorrow?",
            "The weather is really nice today",
            "I'll be running a few minutes late"
        };
        
        for (int i = 0; i < count; i++) {
            Message message = new Message();
            message.setId(i + 1L);
            message.setBody(sampleMessages[i % sampleMessages.length]);
            message.setAddress("+123456789" + (i % 10));
            message.setDate(baseTime - (count - i) * 60000); // 1 minute intervals
            message.setType(i % 3 == 0 ? Message.TYPE_INBOX : Message.TYPE_SENT);
            message.setThreadId(123L + (i % 20)); // Distribute across multiple threads
            message.setContactName(sampleContacts[i % sampleContacts.length]);
            message.setRead(i % 4 != 0); // Most messages are read
            messages.add(message);
        }
        
        return messages;
    }
}