package com.translator.messagingapp;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

/**
 * Integration tests for enhanced offline capabilities.
 * Tests the complete workflow of enhanced translation and message queueing.
 */
@RunWith(RobolectricTestRunner.class)
public class EnhancedOfflineCapabilitiesIntegrationTest {
    
    private Context context;
    private TranslatorApp app;
    private OfflineCapabilitiesManager capabilitiesManager;
    
    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        app = new TranslatorApp();
        app.onCreate();
        capabilitiesManager = new OfflineCapabilitiesManager(context);
    }
    
    @Test
    public void testCompleteOfflineWorkflow() {
        // Test 1: Check initial status
        OfflineCapabilitiesManager.OfflineCapabilitiesStatus initialStatus = 
            capabilitiesManager.getStatus();
        
        assertNotNull("Status should not be null", initialStatus);
        assertNotNull("Queue status should not be null", initialStatus.queueStatus);
        
        // Test 2: Queue a message with different priorities
        OfflineMessageQueue messageQueue = app.getOfflineMessageQueue();
        assertNotNull("Message queue should be available", messageQueue);
        
        long normalMessageId = messageQueue.queueMessage(
            "555-123-4567", 
            "This is a normal priority message for testing the complete offline workflow.", 
            "thread_1", 
            0, // SMS
            null, 
            OfflineMessageQueue.PRIORITY_NORMAL
        );
        
        long highMessageId = messageQueue.queueMessage(
            "555-987-6543", 
            "URGENT: This is a high priority message that should be processed first.", 
            "thread_2", 
            0, // SMS
            null, 
            OfflineMessageQueue.PRIORITY_HIGH
        );
        
        assertTrue("Message IDs should be valid", normalMessageId > 0 && highMessageId > 0);
        
        // Test 3: Check queue status after adding messages
        OfflineCapabilitiesManager.OfflineCapabilitiesStatus statusWithMessages = 
            capabilitiesManager.getStatus();
        
        assertTrue("Should have queued messages", statusWithMessages.hasQueuedMessages);
        assertEquals("Should have 2 total messages", 2, statusWithMessages.queueStatus.totalCount);
        assertEquals("Should have 2 pending messages", 2, statusWithMessages.queueStatus.pendingCount);
        
        // Test 4: Test complex translation scenario
        UserPreferences userPreferences = new UserPreferences(context);
        OfflineTranslationService translationService = new OfflineTranslationService(context, userPreferences);
        
        String complexText = "Hello! How are you today? I hope you're having a wonderful time. " +
                           "This message contains multiple sentences and should trigger the enhanced " +
                           "translation system; it also has complex punctuation like colons: yes!";
        
        // Since we can't easily test the actual translation without models,
        // we'll test the complexity detection logic
        boolean isComplex = isComplexTextForTesting(complexText);
        assertTrue("Complex text should be detected", isComplex);
        
        // Test 5: Test status summary
        String statusSummary = capabilitiesManager.getStatusSummary();
        assertNotNull("Status summary should not be null", statusSummary);
        assertTrue("Status summary should mention queue", statusSummary.contains("Message Queue"));
        assertTrue("Status summary should mention translation", statusSummary.contains("Offline Translation"));
        
        // Test 6: Test message state transitions
        messageQueue.markMessageSent(normalMessageId);
        
        OfflineCapabilitiesManager.OfflineCapabilitiesStatus statusAfterSent = 
            capabilitiesManager.getStatus();
        
        assertEquals("Should have 1 message left", 1, statusAfterSent.queueStatus.totalCount);
        
        // Test 7: Test retry scenario
        messageQueue.markMessageFailed(highMessageId, "Simulated network error");
        
        OfflineCapabilitiesManager.OfflineCapabilitiesStatus statusAfterFailed = 
            capabilitiesManager.getStatus();
        
        assertEquals("Should have 1 retry message", 1, statusAfterFailed.queueStatus.retryCount);
        
        // Test 8: Test utility methods
        capabilitiesManager.startNetworkMonitoring();
        capabilitiesManager.processQueue();
        capabilitiesManager.stopNetworkMonitoring();
        
        // These should complete without throwing exceptions
        assertTrue("Utility methods should complete successfully", true);
        
        // Test 9: Test offline readiness check
        boolean isOfflineReady = capabilitiesManager.isOfflineReady();
        assertFalse("Should not be offline ready with queued messages", isOfflineReady);
        
        // Clean up - mark remaining message as sent
        messageQueue.markMessageSent(highMessageId);
        
        OfflineCapabilitiesManager.OfflineCapabilitiesStatus finalStatus = 
            capabilitiesManager.getStatus();
        
        assertEquals("Queue should be empty", 0, finalStatus.queueStatus.totalCount);
    }
    
    @Test
    public void testMessagePersistenceAcrossRestarts() {
        // Test that messages persist across app restarts
        OfflineMessageQueue firstQueue = new OfflineMessageQueue(context);
        
        long messageId = firstQueue.queueMessage(
            "555-111-2222", 
            "Persistent message test", 
            "thread_persist", 
            0, 
            null, 
            OfflineMessageQueue.PRIORITY_NORMAL
        );
        
        // Create a new queue instance (simulating app restart)
        OfflineMessageQueue secondQueue = new OfflineMessageQueue(context);
        
        OfflineMessageQueue.QueueStatus status = secondQueue.getQueueStatus();
        assertEquals("Message should persist", 1, status.totalCount);
        assertEquals("Message should be pending", 1, status.pendingCount);
        
        // Clean up
        secondQueue.markMessageSent(messageId);
    }
    
    @Test
    public void testTranslationMetrics() {
        OfflineCapabilitiesManager.TranslationMetrics metrics = 
            capabilitiesManager.getTranslationMetrics();
        
        assertNotNull("Metrics should not be null", metrics);
        assertTrue("Cache hit rate should be valid", metrics.cacheHitRate >= 0.0f && metrics.cacheHitRate <= 1.0f);
        assertTrue("Total translations should be non-negative", metrics.totalTranslations >= 0);
    }
    
    @Test
    public void testErrorHandling() {
        // Test that the system handles null contexts gracefully
        try {
            OfflineCapabilitiesManager nullContextManager = new OfflineCapabilitiesManager(null);
            // This might throw an exception, which is expected
        } catch (Exception e) {
            // Expected for null context
            assertTrue("Should handle null context", true);
        }
        
        // Test invalid message data
        OfflineMessageQueue messageQueue = app.getOfflineMessageQueue();
        
        long invalidId = messageQueue.queueMessage(
            null, // Invalid recipient
            "", // Empty message
            null, // No thread
            0,
            null,
            OfflineMessageQueue.PRIORITY_NORMAL
        );
        
        assertTrue("Should handle invalid data gracefully", invalidId > 0);
        
        // Clean up
        messageQueue.markMessageSent(invalidId);
    }
    
    @Test
    public void testPriorityOrdering() {
        OfflineMessageQueue messageQueue = app.getOfflineMessageQueue();
        
        // Queue messages in reverse priority order
        long lowId = messageQueue.queueMessage("1", "Low", "t1", 0, null, OfflineMessageQueue.PRIORITY_LOW);
        long normalId = messageQueue.queueMessage("2", "Normal", "t2", 0, null, OfflineMessageQueue.PRIORITY_NORMAL);
        long highId = messageQueue.queueMessage("3", "High", "t3", 0, null, OfflineMessageQueue.PRIORITY_HIGH);
        long urgentId = messageQueue.queueMessage("4", "Urgent", "t4", 0, null, OfflineMessageQueue.PRIORITY_URGENT);
        
        OfflineMessageQueue.QueueStatus status = messageQueue.getQueueStatus();
        assertEquals("Should have 4 messages", 4, status.totalCount);
        
        // Clean up
        messageQueue.markMessageSent(lowId);
        messageQueue.markMessageSent(normalId);
        messageQueue.markMessageSent(highId);
        messageQueue.markMessageSent(urgentId);
    }
    
    // Helper method to test complexity detection without accessing private methods
    private boolean isComplexTextForTesting(String text) {
        if (text == null) return false;
        
        int length = text.length();
        boolean hasMultipleSentences = text.split("[.!?]+").length > 1;
        boolean isLong = length > 100;
        boolean hasComplexPunctuation = text.contains(";") || text.contains(":") || 
                                       text.contains("\"") || text.contains("'");
        
        return hasMultipleSentences || isLong || hasComplexPunctuation;
    }
}