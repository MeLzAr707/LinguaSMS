package com.translator.messagingapp;

import org.junit.Test;
import java.util.Date;
import static org.junit.Assert.*;

/**
 * Test to verify that the SmsMessage class confusion fix is working correctly.
 * 
 * This test validates that:
 * 1. The custom SmsMessage class can be used without import conflicts
 * 2. The Android SmsMessage class can still be used when needed with full qualification
 * 3. The specific error patterns from the issue are resolved
 */
public class SmsMessageClassFixVerificationTest {

    @Test
    public void testCustomSmsMessageWorksWithoutImportConflict() {
        // This test verifies that we can create and use custom SmsMessage objects
        // without any class confusion issues
        
        String senderAddress = "+1234567890";
        String messageBody = "Test message body";
        long timestamp = System.currentTimeMillis();
        
        // Test the exact pattern that was failing in the original issue
        SmsMessage smsMessage = new SmsMessage(senderAddress, messageBody, new Date(timestamp));
        smsMessage.setIncoming(true);
        smsMessage.setRead(false); // Incoming messages are initially unread
        
        // Verify all properties are set correctly
        assertEquals("Address should be set correctly", senderAddress, smsMessage.getAddress());
        assertEquals("Original text should be set correctly", messageBody, smsMessage.getOriginalText());
        assertEquals("Timestamp should be set correctly", new Date(timestamp), smsMessage.getTimestamp());
        assertTrue("Should be marked as incoming", smsMessage.isIncoming());
        assertFalse("Should be marked as unread", smsMessage.isRead());
    }

    @Test
    public void testFullyQualifiedCustomSmsMessageUsage() {
        // Test using fully qualified names for our custom class
        // This pattern is used in the fix for MessageService
        
        String senderAddress = "+9876543210";
        String messageBody = "Another test message";
        long timestamp = System.currentTimeMillis();
        
        com.translator.messagingapp.sms.SmsMessage smsMessage = new com.translator.messagingapp.sms.SmsMessage(
            senderAddress, messageBody, new Date(timestamp)
        );
        smsMessage.setIncoming(true);
        smsMessage.setRead(false);
        
        assertNotNull("SmsMessage should be created successfully", smsMessage);
        assertEquals("Address should match", senderAddress, smsMessage.getAddress());
        assertEquals("Message body should match", messageBody, smsMessage.getOriginalText());
        assertTrue("Should be incoming", smsMessage.isIncoming());
        assertFalse("Should be unread", smsMessage.isRead());
    }

    @Test
    public void testAndroidSmsMessageStillAccessible() {
        // Verify that Android's SmsMessage can still be accessed with full qualification
        // This ensures our fix doesn't break legitimate usage of Android's class
        
        try {
            Class<?> androidSmsMessageClass = Class.forName("android.telephony.SmsMessage");
            assertNotNull("Android SmsMessage should still be accessible", androidSmsMessageClass);
            
            // Verify it has the createFromPdu method that MessageService uses
            boolean hasCreateFromPdu = false;
            try {
                androidSmsMessageClass.getMethod("createFromPdu", byte[].class, String.class);
                hasCreateFromPdu = true;
            } catch (NoSuchMethodException e) {
                // Try the older version without format parameter
                try {
                    androidSmsMessageClass.getMethod("createFromPdu", byte[].class);
                    hasCreateFromPdu = true;
                } catch (NoSuchMethodException e2) {
                    // Neither method found
                }
            }
            
            assertTrue("Android SmsMessage should have createFromPdu method", hasCreateFromPdu);
            
        } catch (ClassNotFoundException e) {
            fail("Android SmsMessage class should still be accessible");
        }
    }

    @Test
    public void testOriginalErrorPatternsAreFixed() {
        // Test that the specific patterns mentioned in the original error are now working
        
        // Original error pattern 1: constructor with 3 parameters
        String senderAddress = "1234567890";
        String messageBody = "Hello World";
        long timestamp = System.currentTimeMillis();
        
        try {
            SmsMessage smsMessage = new SmsMessage(senderAddress, messageBody, new Date(timestamp));
            assertNotNull("Constructor with timestamp should work", smsMessage);
        } catch (Exception e) {
            fail("Constructor with timestamp should not throw exception: " + e.getMessage());
        }
        
        // Original error pattern 2: setIncoming method
        try {
            SmsMessage smsMessage = new SmsMessage(senderAddress, messageBody);
            smsMessage.setIncoming(true);
            assertTrue("setIncoming should work", smsMessage.isIncoming());
        } catch (Exception e) {
            fail("setIncoming method should not throw exception: " + e.getMessage());
        }
        
        // Original error pattern 3: setRead method
        try {
            SmsMessage smsMessage = new SmsMessage(senderAddress, messageBody);
            smsMessage.setRead(false);
            assertFalse("setRead should work", smsMessage.isRead());
        } catch (Exception e) {
            fail("setRead method should not throw exception: " + e.getMessage());
        }
    }

    @Test
    public void testClassDistinction() {
        // Verify that our custom SmsMessage and Android's SmsMessage are indeed different classes
        
        Class<?> customSmsMessageClass = SmsMessage.class;
        
        try {
            Class<?> androidSmsMessageClass = Class.forName("android.telephony.SmsMessage");
            
            assertNotEquals("Custom and Android SmsMessage should be different classes",
                          customSmsMessageClass, androidSmsMessageClass);
            
            // Custom class should have our methods
            boolean customHasSetIncoming = false;
            boolean customHasSetRead = false;
            
            try {
                customSmsMessageClass.getMethod("setIncoming", boolean.class);
                customHasSetIncoming = true;
            } catch (NoSuchMethodException e) {
                // Method not found
            }
            
            try {
                customSmsMessageClass.getMethod("setRead", boolean.class);
                customHasSetRead = true;
            } catch (NoSuchMethodException e) {
                // Method not found
            }
            
            assertTrue("Custom SmsMessage should have setIncoming method", customHasSetIncoming);
            assertTrue("Custom SmsMessage should have setRead method", customHasSetRead);
            
            // Android class should NOT have our methods
            boolean androidHasSetIncoming = false;
            boolean androidHasSetRead = false;
            
            try {
                androidSmsMessageClass.getMethod("setIncoming", boolean.class);
                androidHasSetIncoming = true;
            } catch (NoSuchMethodException e) {
                // Expected - method not found
            }
            
            try {
                androidSmsMessageClass.getMethod("setRead", boolean.class);
                androidHasSetRead = true;
            } catch (NoSuchMethodException e) {
                // Expected - method not found
            }
            
            assertFalse("Android SmsMessage should not have setIncoming method", androidHasSetIncoming);
            assertFalse("Android SmsMessage should not have setRead method", androidHasSetRead);
            
        } catch (ClassNotFoundException e) {
            fail("Android SmsMessage class should be available for comparison");
        }
    }
}