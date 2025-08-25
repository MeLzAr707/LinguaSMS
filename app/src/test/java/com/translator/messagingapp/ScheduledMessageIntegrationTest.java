package com.translator.messagingapp;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Integration test for the complete scheduled messages feature.
 * Tests the interaction between all components.
 */
public class ScheduledMessageIntegrationTest {
    
    private ScheduledMessage testMessage;
    private static final String TEST_RECIPIENT = "+1234567890";
    private static final String TEST_BODY = "Test scheduled message";
    private static final long TEST_FUTURE_TIME = System.currentTimeMillis() + 3600000; // 1 hour from now
    
    @Before
    public void setUp() {
        testMessage = new ScheduledMessage(TEST_RECIPIENT, TEST_BODY, TEST_FUTURE_TIME, "thread123");
        testMessage.setId(1L);
    }
    
    @Test
    public void testScheduledMessageDataIntegrity() {
        // Test that all data is preserved correctly
        assertEquals(TEST_RECIPIENT, testMessage.getRecipient());
        assertEquals(TEST_BODY, testMessage.getMessageBody());
        assertEquals(TEST_FUTURE_TIME, testMessage.getScheduledTime());
        assertEquals("thread123", testMessage.getThreadId());
        assertEquals(1L, testMessage.getId());
        assertFalse(testMessage.isDelivered());
        assertTrue(testMessage.getCreatedTime() > 0);
    }
    
    @Test
    public void testSchedulingLogic() {
        // Test future scheduling
        assertTrue("Message scheduled for future should be scheduled for future", 
                   testMessage.isScheduledForFuture());
        assertFalse("Message scheduled for future should not be ready to send", 
                    testMessage.isReadyToSend());
        
        // Test past scheduling
        testMessage.setScheduledTime(System.currentTimeMillis() - 1000);
        assertFalse("Message scheduled for past should not be scheduled for future", 
                    testMessage.isScheduledForFuture());
        assertTrue("Message scheduled for past should be ready to send", 
                   testMessage.isReadyToSend());
        
        // Test delivered message
        testMessage.setDelivered(true);
        assertFalse("Delivered message should not be ready to send", 
                    testMessage.isReadyToSend());
        assertFalse("Delivered message should not be scheduled for future", 
                    testMessage.isScheduledForFuture());
    }
    
    @Test
    public void testEdgeCases() {
        // Test boundary conditions
        long currentTime = System.currentTimeMillis();
        
        // Message scheduled exactly now
        testMessage.setScheduledTime(currentTime);
        testMessage.setDelivered(false);
        
        // Should be ready to send (currentTime >= scheduledTime)
        assertTrue("Message scheduled for current time should be ready to send", 
                   testMessage.isReadyToSend());
        
        // Test 1 millisecond in future
        testMessage.setScheduledTime(currentTime + 1);
        assertFalse("Message scheduled 1ms in future should not be ready to send", 
                    testMessage.isReadyToSend());
        assertTrue("Message scheduled 1ms in future should be scheduled for future", 
                   testMessage.isScheduledForFuture());
        
        // Test 1 millisecond in past
        testMessage.setScheduledTime(currentTime - 1);
        assertTrue("Message scheduled 1ms in past should be ready to send", 
                   testMessage.isReadyToSend());
        assertFalse("Message scheduled 1ms in past should not be scheduled for future", 
                    testMessage.isScheduledForFuture());
    }
    
    @Test
    public void testTimeValidation() {
        // Test that very old and very future times work correctly
        long veryOldTime = 1000000000L; // Year 2001
        long veryFutureTime = 4000000000000L; // Year 2096
        
        testMessage.setScheduledTime(veryOldTime);
        testMessage.setDelivered(false);
        assertTrue("Very old message should be ready to send", testMessage.isReadyToSend());
        
        testMessage.setScheduledTime(veryFutureTime);
        assertTrue("Very future message should be scheduled for future", testMessage.isScheduledForFuture());
        assertFalse("Very future message should not be ready to send", testMessage.isReadyToSend());
    }
    
    @Test
    public void testMessageStateTransitions() {
        // Test the lifecycle of a scheduled message
        
        // 1. Initially scheduled for future
        assertTrue("New message should be scheduled for future", testMessage.isScheduledForFuture());
        assertFalse("New message should not be delivered", testMessage.isDelivered());
        assertFalse("New message should not be ready to send", testMessage.isReadyToSend());
        
        // 2. Time passes, message becomes ready
        testMessage.setScheduledTime(System.currentTimeMillis() - 1000);
        assertTrue("Message should now be ready to send", testMessage.isReadyToSend());
        assertFalse("Message should not be scheduled for future", testMessage.isScheduledForFuture());
        assertFalse("Message should still not be delivered", testMessage.isDelivered());
        
        // 3. Message is delivered
        testMessage.setDelivered(true);
        assertFalse("Delivered message should not be ready to send", testMessage.isReadyToSend());
        assertFalse("Delivered message should not be scheduled for future", testMessage.isScheduledForFuture());
        assertTrue("Message should be delivered", testMessage.isDelivered());
    }
    
    @Test
    public void testDataValidation() {
        // Test that the message handles various types of input correctly
        
        // Empty recipient
        testMessage.setRecipient("");
        assertEquals("", testMessage.getRecipient());
        
        // Null recipient (should not crash)
        testMessage.setRecipient(null);
        assertNull(testMessage.getRecipient());
        
        // Very long message body
        String longBody = "a".repeat(1000);
        testMessage.setMessageBody(longBody);
        assertEquals(longBody, testMessage.getMessageBody());
        
        // Empty message body
        testMessage.setMessageBody("");
        assertEquals("", testMessage.getMessageBody());
        
        // Special characters
        String specialChars = "Hello! 👋 Testing émojis and spëcial chars: @#$%^&*()";
        testMessage.setMessageBody(specialChars);
        assertEquals(specialChars, testMessage.getMessageBody());
    }
    
    @Test
    public void testToStringContainsEssentialInfo() {
        String toString = testMessage.toString();
        
        // Verify toString contains key information
        assertTrue("toString should contain recipient", toString.contains(TEST_RECIPIENT));
        assertTrue("toString should contain message body", toString.contains(TEST_BODY));
        assertTrue("toString should contain scheduled time", toString.contains(String.valueOf(TEST_FUTURE_TIME)));
        assertTrue("toString should contain thread ID", toString.contains("thread123"));
        assertTrue("toString should contain ID", toString.contains("id=1"));
        assertTrue("toString should contain delivery status", toString.contains("isDelivered=false"));
    }
}