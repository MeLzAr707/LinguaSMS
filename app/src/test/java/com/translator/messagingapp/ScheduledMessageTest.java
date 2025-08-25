package com.translator.messagingapp;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the ScheduledMessage class to verify basic functionality.
 */
public class ScheduledMessageTest {
    
    @Test
    public void testScheduledMessageCreation() {
        // Test basic creation
        ScheduledMessage message = new ScheduledMessage();
        assertNotNull(message);
        assertFalse(message.isDelivered());
        assertTrue(message.getCreatedTime() > 0);
    }
    
    @Test
    public void testScheduledMessageWithParameters() {
        String recipient = "+1234567890";
        String body = "Test message";
        long scheduledTime = System.currentTimeMillis() + 3600000; // 1 hour from now
        String threadId = "thread123";
        
        ScheduledMessage message = new ScheduledMessage(recipient, body, scheduledTime, threadId);
        
        assertEquals(recipient, message.getRecipient());
        assertEquals(body, message.getMessageBody());
        assertEquals(scheduledTime, message.getScheduledTime());
        assertEquals(threadId, message.getThreadId());
        assertFalse(message.isDelivered());
    }
    
    @Test
    public void testIsReadyToSend() {
        ScheduledMessage message = new ScheduledMessage();
        
        // Message scheduled for past should be ready to send
        message.setScheduledTime(System.currentTimeMillis() - 1000);
        assertTrue(message.isReadyToSend());
        
        // Message scheduled for future should not be ready
        message.setScheduledTime(System.currentTimeMillis() + 3600000);
        assertFalse(message.isReadyToSend());
        
        // Delivered message should not be ready even if time has passed
        message.setScheduledTime(System.currentTimeMillis() - 1000);
        message.setDelivered(true);
        assertFalse(message.isReadyToSend());
    }
    
    @Test
    public void testIsScheduledForFuture() {
        ScheduledMessage message = new ScheduledMessage();
        
        // Message scheduled for future
        message.setScheduledTime(System.currentTimeMillis() + 3600000);
        assertTrue(message.isScheduledForFuture());
        
        // Message scheduled for past
        message.setScheduledTime(System.currentTimeMillis() - 1000);
        assertFalse(message.isScheduledForFuture());
        
        // Delivered message should not be scheduled for future
        message.setScheduledTime(System.currentTimeMillis() + 3600000);
        message.setDelivered(true);
        assertFalse(message.isScheduledForFuture());
    }
    
    @Test
    public void testSettersAndGetters() {
        ScheduledMessage message = new ScheduledMessage();
        
        message.setId(123L);
        assertEquals(123L, message.getId());
        
        message.setRecipient("+1234567890");
        assertEquals("+1234567890", message.getRecipient());
        
        message.setMessageBody("Test body");
        assertEquals("Test body", message.getMessageBody());
        
        long now = System.currentTimeMillis();
        message.setScheduledTime(now);
        assertEquals(now, message.getScheduledTime());
        
        message.setCreatedTime(now);
        assertEquals(now, message.getCreatedTime());
        
        message.setDelivered(true);
        assertTrue(message.isDelivered());
        
        message.setThreadId("thread456");
        assertEquals("thread456", message.getThreadId());
    }
    
    @Test
    public void testToString() {
        ScheduledMessage message = new ScheduledMessage("test@example.com", "Hello", 123456789L, "thread1");
        message.setId(1L);
        
        String toString = message.toString();
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("recipient='test@example.com'"));
        assertTrue(toString.contains("messageBody='Hello'"));
        assertTrue(toString.contains("scheduledTime=123456789"));
    }
}