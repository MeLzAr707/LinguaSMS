package com.translator.messagingapp;

import org.junit.Test;
import java.util.Date;
import static org.junit.Assert.*;

/**
 * Test to reproduce and verify the fix for SmsMessage class confusion issue.
 * 
 * The issue was that MessageService.java imports android.telephony.SmsMessage
 * but tries to use it as if it were the custom SmsMessage class.
 * 
 * The fix uses fully qualified names to distinguish between the two classes.
 */
public class SmsMessageClassConfusionTest {

    @Test
    public void testCustomSmsMessageConstructorWithTimestamp() {
        // Test that our custom SmsMessage class has the expected constructor
        String address = "+1234567890";
        String messageBody = "Test message";
        Date timestamp = new Date();
        
        // This should work with our custom SmsMessage class
        SmsMessage customSmsMessage = new SmsMessage(address, messageBody, timestamp);
        
        assertNotNull("SmsMessage should be created", customSmsMessage);
        assertEquals("Address should match", address, customSmsMessage.getAddress());
        assertEquals("Original text should match", messageBody, customSmsMessage.getOriginalText());
        assertEquals("Timestamp should match", timestamp, customSmsMessage.getTimestamp());
    }

    @Test
    public void testCustomSmsMessageHasSetIncomingMethod() {
        // Test that our custom SmsMessage has setIncoming method
        SmsMessage customSmsMessage = new SmsMessage("+1234567890", "Test");
        
        // These methods should exist on our custom class
        customSmsMessage.setIncoming(true);
        assertTrue("Should be marked as incoming", customSmsMessage.isIncoming());
        
        customSmsMessage.setIncoming(false);
        assertFalse("Should be marked as outgoing", customSmsMessage.isIncoming());
    }

    @Test
    public void testCustomSmsMessageHasSetReadMethod() {
        // Test that our custom SmsMessage has setRead method
        SmsMessage customSmsMessage = new SmsMessage("+1234567890", "Test");
        
        // These methods should exist on our custom class
        customSmsMessage.setRead(true);
        assertTrue("Should be marked as read", customSmsMessage.isRead());
        
        customSmsMessage.setRead(false);
        assertFalse("Should be marked as unread", customSmsMessage.isRead());
    }

    @Test
    public void testMessageServiceCreateCustomSmsMessage() {
        // Test the new method that demonstrates the fix
        // This requires a context, so we'll create a simple test
        
        String senderAddress = "+1234567890";
        String messageBody = "Test message body";
        long timestamp = System.currentTimeMillis();
        
        // This would previously fail due to class confusion, but now works with the fix
        // Note: We can't easily test MessageService.createCustomSmsMessage() without a Context
        // But we can test the pattern it uses
        
        com.translator.messagingapp.SmsMessage smsMessage = new com.translator.messagingapp.SmsMessage(
            senderAddress, messageBody, new Date(timestamp)
        );
        smsMessage.setIncoming(true);
        smsMessage.setRead(false);
        
        assertNotNull("SmsMessage should be created", smsMessage);
        assertEquals("Address should match", senderAddress, smsMessage.getAddress());
        assertEquals("Original text should match", messageBody, smsMessage.getOriginalText());
        assertTrue("Should be marked as incoming", smsMessage.isIncoming());
        assertFalse("Should be marked as unread", smsMessage.isRead());
    }

    @Test
    public void testAndroidSmsMessageClassExists() {
        // Verify that Android's SmsMessage class exists but is different
        // This would typically be imported as android.telephony.SmsMessage
        
        // We can't easily instantiate Android's SmsMessage in tests since it requires PDU data
        // But we can verify the class exists using reflection
        try {
            Class<?> androidSmsMessageClass = Class.forName("android.telephony.SmsMessage");
            assertNotNull("Android SmsMessage class should exist", androidSmsMessageClass);
            
            // Android's SmsMessage has a no-argument constructor (not the public one we need)
            // and doesn't have setIncoming/setRead methods
            boolean hasSetIncoming = false;
            boolean hasSetRead = false;
            
            try {
                androidSmsMessageClass.getMethod("setIncoming", boolean.class);
                hasSetIncoming = true;
            } catch (NoSuchMethodException e) {
                // Expected - Android's SmsMessage doesn't have this method
            }
            
            try {
                androidSmsMessageClass.getMethod("setRead", boolean.class);
                hasSetRead = true;
            } catch (NoSuchMethodException e) {
                // Expected - Android's SmsMessage doesn't have this method
            }
            
            assertFalse("Android SmsMessage should not have setIncoming method", hasSetIncoming);
            assertFalse("Android SmsMessage should not have setRead method", hasSetRead);
            
        } catch (ClassNotFoundException e) {
            fail("Android SmsMessage class should be available");
        }
    }
}