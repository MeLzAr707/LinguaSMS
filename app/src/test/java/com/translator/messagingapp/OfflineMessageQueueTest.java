package com.translator.messagingapp;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

/**
 * Unit tests for offline message queue functionality.
 */
@RunWith(RobolectricTestRunner.class)
public class OfflineMessageQueueTest {
    
    private Context context;
    private OfflineMessageQueue messageQueue;
    
    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        messageQueue = new OfflineMessageQueue(context);
    }
    
    @Test
    public void testQueueMessage() {
        // Test queuing a simple SMS message
        long messageId = messageQueue.queueMessage(
            "123-456-7890",
            "Test message",
            "thread_123",
            0, // SMS type
            null, // No attachments
            OfflineMessageQueue.PRIORITY_NORMAL
        );
        
        assertTrue("Message ID should be valid", messageId > 0);
        
        OfflineMessageQueue.QueueStatus status = messageQueue.getQueueStatus();
        assertEquals("Should have 1 message in queue", 1, status.totalCount);
        assertEquals("Should have 1 pending message", 1, status.pendingCount);
    }
    
    @Test
    public void testMessagePriority() {
        // Queue messages with different priorities
        long lowPriorityId = messageQueue.queueMessage(
            "123-456-7890", "Low priority", "thread_1", 0, null, OfflineMessageQueue.PRIORITY_LOW);
        
        long highPriorityId = messageQueue.queueMessage(
            "123-456-7890", "High priority", "thread_2", 0, null, OfflineMessageQueue.PRIORITY_HIGH);
        
        long normalPriorityId = messageQueue.queueMessage(
            "123-456-7890", "Normal priority", "thread_3", 0, null, OfflineMessageQueue.PRIORITY_NORMAL);
        
        OfflineMessageQueue.QueueStatus status = messageQueue.getQueueStatus();
        assertEquals("Should have 3 messages in queue", 3, status.totalCount);
        
        // High priority messages should be processed first, but we can't easily test
        // the internal queue order without accessing private fields
        assertTrue("All message IDs should be valid", 
            lowPriorityId > 0 && highPriorityId > 0 && normalPriorityId > 0);
    }
    
    @Test
    public void testMessageStates() {
        long messageId = messageQueue.queueMessage(
            "123-456-7890", "Test message", "thread_1", 0, null, OfflineMessageQueue.PRIORITY_NORMAL);
        
        // Test marking message as sent
        messageQueue.markMessageSent(messageId);
        
        OfflineMessageQueue.QueueStatus status = messageQueue.getQueueStatus();
        assertEquals("Should have no messages after marking as sent", 0, status.totalCount);
    }
    
    @Test
    public void testMessageRetry() {
        long messageId = messageQueue.queueMessage(
            "123-456-7890", "Test message", "thread_1", 0, null, OfflineMessageQueue.PRIORITY_NORMAL);
        
        // Test marking message as failed (should trigger retry)
        messageQueue.markMessageFailed(messageId, "Network error");
        
        OfflineMessageQueue.QueueStatus status = messageQueue.getQueueStatus();
        assertEquals("Should still have 1 message in queue for retry", 1, status.totalCount);
        assertEquals("Should have 1 message in retry state", 1, status.retryCount);
    }
    
    @Test
    public void testMaxRetryAttempts() {
        long messageId = messageQueue.queueMessage(
            "123-456-7890", "Test message", "thread_1", 0, null, OfflineMessageQueue.PRIORITY_NORMAL);
        
        // Fail the message multiple times to exceed max retry attempts
        for (int i = 0; i < 6; i++) { // More than MAX_RETRY_ATTEMPTS (5)
            messageQueue.markMessageFailed(messageId, "Persistent error");
        }
        
        OfflineMessageQueue.QueueStatus status = messageQueue.getQueueStatus();
        assertEquals("Should have 1 failed message", 1, status.failedCount);
    }
    
    @Test
    public void testQueuePersistence() {
        // Queue a message
        long messageId = messageQueue.queueMessage(
            "123-456-7890", "Persistent message", "thread_1", 0, null, OfflineMessageQueue.PRIORITY_NORMAL);
        
        // Create a new queue instance (simulating app restart)
        OfflineMessageQueue newQueue = new OfflineMessageQueue(context);
        
        OfflineMessageQueue.QueueStatus status = newQueue.getQueueStatus();
        assertEquals("Message should persist across queue instances", 1, status.totalCount);
        assertEquals("Message should be in pending state after restart", 1, status.pendingCount);
        
        // Clean up
        newQueue.markMessageSent(messageId);
    }
    
    @Test
    public void testQueuedMessageSerialization() {
        OfflineMessageQueue.QueuedMessage message = new OfflineMessageQueue.QueuedMessage();
        message.recipient = "123-456-7890";
        message.messageBody = "Test message";
        message.threadId = "thread_123";
        message.messageType = 0;
        message.state = OfflineMessageQueue.STATE_PENDING;
        message.priority = OfflineMessageQueue.PRIORITY_HIGH;
        message.attachmentUris = new String[]{"uri1", "uri2"};
        
        try {
            // Test JSON serialization
            org.json.JSONObject json = message.toJSON();
            assertNotNull("JSON should not be null", json);
            
            // Test JSON deserialization
            OfflineMessageQueue.QueuedMessage deserializedMessage = 
                OfflineMessageQueue.QueuedMessage.fromJSON(json);
            
            assertEquals("Recipient should match", message.recipient, deserializedMessage.recipient);
            assertEquals("Message body should match", message.messageBody, deserializedMessage.messageBody);
            assertEquals("Thread ID should match", message.threadId, deserializedMessage.threadId);
            assertEquals("Message type should match", message.messageType, deserializedMessage.messageType);
            assertEquals("State should match", message.state, deserializedMessage.state);
            assertEquals("Priority should match", message.priority, deserializedMessage.priority);
            assertEquals("Attachment URIs length should match", 
                message.attachmentUris.length, deserializedMessage.attachmentUris.length);
            
        } catch (org.json.JSONException e) {
            fail("JSON serialization/deserialization should not throw exception: " + e.getMessage());
        }
    }
    
    @Test
    public void testQueueStatus() {
        // Test empty queue
        OfflineMessageQueue.QueueStatus emptyStatus = messageQueue.getQueueStatus();
        assertFalse("Empty queue should not have messages", emptyStatus.hasMessages());
        assertFalse("Empty queue should not have failed messages", emptyStatus.hasFailedMessages());
        
        // Add various message states
        long messageId1 = messageQueue.queueMessage(
            "123-456-7890", "Message 1", "thread_1", 0, null, OfflineMessageQueue.PRIORITY_NORMAL);
        
        long messageId2 = messageQueue.queueMessage(
            "123-456-7890", "Message 2", "thread_2", 0, null, OfflineMessageQueue.PRIORITY_NORMAL);
        
        // Fail one message
        messageQueue.markMessageFailed(messageId2, "Test error");
        
        OfflineMessageQueue.QueueStatus status = messageQueue.getQueueStatus();
        assertTrue("Queue should have messages", status.hasMessages());
        assertEquals("Should have 2 total messages", 2, status.totalCount);
        assertEquals("Should have 1 pending message", 1, status.pendingCount);
        assertEquals("Should have 1 retry message", 1, status.retryCount);
        
        // Clean up
        messageQueue.markMessageSent(messageId1);
        messageQueue.markMessageSent(messageId2);
    }
    
    @Test
    public void testNetworkMonitoring() {
        // Test that network monitoring can be started and stopped without crashing
        try {
            messageQueue.startNetworkMonitoring();
            messageQueue.stopNetworkMonitoring();
            assertTrue("Network monitoring should complete without errors", true);
        } catch (Exception e) {
            fail("Network monitoring should not throw exception: " + e.getMessage());
        }
    }
    
    @Test
    public void testCleanup() {
        // Test that cleanup can be called without issues
        try {
            messageQueue.cleanup();
            assertTrue("Cleanup should complete without errors", true);
        } catch (Exception e) {
            fail("Cleanup should not throw exception: " + e.getMessage());
        }
    }
}