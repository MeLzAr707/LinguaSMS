package com.translator.messagingapp;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive test for the improved message caching system.
 * Tests intelligent caching, compression, and background loading features.
 */
public class ImprovedMessageCacheTest {
    
    private OptimizedMessageCache cache;
    private List<Message> testMessages;
    
    @Before
    public void setUp() {
        cache = new OptimizedMessageCache();
        testMessages = createTestMessages(10);
    }
    
    @Test
    public void testIntelligentCaching() {
        String threadId = "thread_123";
        
        // Cache initial messages
        cache.cacheMessages(threadId, testMessages);
        
        // Verify caching
        List<Message> cached = cache.getCachedMessages(threadId);
        assertNotNull("Cached messages should not be null", cached);
        assertEquals("Should cache correct number of messages", testMessages.size(), cached.size());
        
        // Test multiple accesses to increase frequency
        for (int i = 0; i < 6; i++) {
            cache.getCachedMessages(threadId);
        }
        
        // Verify access tracking
        String stats = cache.getCacheStats();
        assertNotNull("Cache stats should not be null", stats);
        assertTrue("Stats should contain hit rate", stats.contains("Hit Rate"));
        
        // Test cache hit rate calculation
        double hitRate = cache.getCacheHitRate();
        assertTrue("Hit rate should be positive", hitRate > 0);
        assertTrue("Hit rate should be reasonable", hitRate <= 100);
    }
    
    @Test
    public void testFrequencyBasedPrioritization() {
        String highFreqThread = "high_freq_thread";
        String lowFreqThread = "low_freq_thread";
        
        // Cache messages for both threads
        cache.cacheMessages(highFreqThread, testMessages);
        cache.cacheMessages(lowFreqThread, testMessages);
        
        // Access high frequency thread multiple times
        for (int i = 0; i < 8; i++) {
            cache.getCachedMessages(highFreqThread);
        }
        
        // Access low frequency thread only once
        cache.getCachedMessages(lowFreqThread);
        
        // Both should still be in cache
        assertNotNull("High frequency thread should be cached", cache.getCachedMessages(highFreqThread));
        assertNotNull("Low frequency thread should be cached", cache.getCachedMessages(lowFreqThread));
        
        // Verify stats show different access patterns
        String stats = cache.getCacheStats();
        assertTrue("Stats should show high frequency items", stats.contains("High-Freq Items"));
    }
    
    @Test
    public void testCacheCompression() {
        // Create larger dataset to test compression
        List<Message> largeMessageSet = createTestMessages(100);
        
        // Test compression utility
        CacheCompressionUtils.CompressedMessageData compressed = 
            CacheCompressionUtils.compressMessages(largeMessageSet);
        
        assertNotNull("Compressed data should not be null", compressed);
        assertTrue("Should have some data", compressed.compressedData.length > 0);
        
        // Test decompression
        List<Message> decompressed = CacheCompressionUtils.decompressMessages(compressed);
        assertNotNull("Decompressed messages should not be null", decompressed);
        assertEquals("Should decompress to same number of messages", 
                    largeMessageSet.size(), decompressed.size());
        
        // Verify message content integrity
        for (int i = 0; i < largeMessageSet.size(); i++) {
            Message original = largeMessageSet.get(i);
            Message decompressedMsg = decompressed.get(i);
            
            assertEquals("Message ID should match", original.getId(), decompressedMsg.getId());
            assertEquals("Message body should match", original.getBody(), decompressedMsg.getBody());
            assertEquals("Message date should match", original.getDate(), decompressedMsg.getDate());
        }
    }
    
    @Test
    public void testCompressionBenefits() {
        List<Message> messages = createTestMessages(50);
        
        CacheCompressionUtils.CompressedMessageData compressed = 
            CacheCompressionUtils.compressMessages(messages);
        
        if (compressed.isCompressed) {
            assertTrue("Compression should save space", compressed.getSpaceSaved() > 0);
            assertTrue("Compression ratio should be reasonable", 
                      compressed.getCompressionRatio() < 100.0);
            
            String stats = CacheCompressionUtils.getCompressionStats(compressed);
            assertNotNull("Compression stats should not be null", stats);
            assertTrue("Stats should contain compression info", stats.contains("Compressed"));
        }
    }
    
    @Test
    public void testSmallDataCompressionSkipping() {
        // Create very small dataset
        List<Message> smallMessages = createTestMessages(2);
        
        CacheCompressionUtils.CompressedMessageData result = 
            CacheCompressionUtils.compressMessages(smallMessages);
        
        // Small data should not be compressed
        assertFalse("Small data should not be compressed", result.isCompressed);
        assertEquals("Space saved should be zero", 0, result.getSpaceSaved());
    }
    
    @Test
    public void testBackgroundLoaderIntegration() {
        // Test that background loader can be instantiated without errors
        // Note: We can't fully test background loading without a real MessageService
        try {
            BackgroundMessageLoader loader = new BackgroundMessageLoader(null, null, cache);
            assertNotNull("Background loader should be created", loader);
            
            // Test enabling/disabling
            loader.setEnabled(false);
            assertFalse("Should be disabled", loader.isEnabled());
            
            loader.setEnabled(true);
            assertTrue("Should be enabled", loader.isEnabled());
            
            // Test active prefetch count
            assertEquals("Should have no active prefetches", 0, loader.getActivePrefetchCount());
            
            // Clean up
            loader.shutdown();
            
        } catch (Exception e) {
            fail("Background loader should be creatable: " + e.getMessage());
        }
    }
    
    @Test
    public void testCacheMemoryManagement() {
        // Test memory usage estimation
        cache.cacheMessages("thread1", testMessages);
        
        int memoryUsage = cache.getEstimatedMemoryUsage();
        assertTrue("Memory usage should be positive", memoryUsage > 0);
        
        // Test maintenance
        cache.performMaintenance();
        
        // Cache should still work after maintenance
        assertNotNull("Cache should work after maintenance", 
                     cache.getCachedMessages("thread1"));
    }
    
    @Test
    public void testCacheEvictionBehavior() {
        // Fill cache with multiple threads
        for (int i = 0; i < 10; i++) {
            String threadId = "thread_" + i;
            cache.cacheMessages(threadId, createTestMessages(20));
        }
        
        // All should be cached initially
        for (int i = 0; i < 10; i++) {
            String threadId = "thread_" + i;
            assertNotNull("Thread " + i + " should be cached", 
                         cache.getCachedMessages(threadId));
        }
        
        // Access some threads more frequently
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 10; j++) {
                cache.getCachedMessages("thread_" + i);
            }
        }
        
        // Verify high-frequency items are tracked
        String stats = cache.getCacheStats();
        assertTrue("Should have high-frequency items", stats.contains("High-Freq Items"));
    }
    
    @Test
    public void testCacheClearOperations() {
        // Cache some data
        cache.cacheMessages("thread1", testMessages);
        cache.cacheMessages("thread2", testMessages);
        
        // Verify data is cached
        assertNotNull("Thread1 should be cached", cache.getCachedMessages("thread1"));
        assertNotNull("Thread2 should be cached", cache.getCachedMessages("thread2"));
        
        // Clear specific thread
        cache.clearCacheForThread("thread1");
        assertNull("Thread1 should be cleared", cache.getCachedMessages("thread1"));
        assertNotNull("Thread2 should still be cached", cache.getCachedMessages("thread2"));
        
        // Clear all cache
        cache.clearCache();
        assertNull("Thread2 should be cleared", cache.getCachedMessages("thread2"));
        
        // Verify stats are reset
        assertEquals("Hit rate should be reset", 0.0, cache.getCacheHitRate(), 0.01);
    }
    
    @Test
    public void testErrorHandling() {
        // Test null inputs
        cache.cacheMessages(null, testMessages);
        cache.cacheMessages("thread", null);
        cache.cacheMessages("", testMessages);
        
        // Should not crash
        assertNull("Should handle null thread ID", cache.getCachedMessages(null));
        assertNull("Should handle empty thread ID", cache.getCachedMessages(""));
        
        // Test compression with null/empty data
        CacheCompressionUtils.CompressedMessageData result = 
            CacheCompressionUtils.compressMessages(null);
        assertNotNull("Should handle null messages", result);
        assertFalse("Should not be compressed", result.isCompressed);
        
        result = CacheCompressionUtils.compressMessages(new ArrayList<>());
        assertNotNull("Should handle empty messages", result);
        assertFalse("Should not be compressed", result.isCompressed);
    }
    
    /**
     * Helper method to create test messages
     */
    private List<Message> createTestMessages(int count) {
        List<Message> messages = new ArrayList<>();
        long baseTime = System.currentTimeMillis();
        
        for (int i = 0; i < count; i++) {
            Message message = new Message();
            message.setId(i + 1L);
            message.setBody("Test message " + (i + 1) + " with some content to make it realistic");
            message.setAddress("+1234567890");
            message.setDate(baseTime + i * 1000);
            message.setType(i % 2 == 0 ? Message.TYPE_INBOX : Message.TYPE_SENT);
            message.setThreadId(123L);
            message.setContactName("Test Contact " + (i % 3)); // Vary contact names
            message.setRead(i % 3 == 0); // Vary read status
            messages.add(message);
        }
        
        return messages;
    }
}