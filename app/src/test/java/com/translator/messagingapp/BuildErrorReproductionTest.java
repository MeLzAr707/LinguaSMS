package com.translator.messagingapp;

import org.junit.Test;
import java.util.Date;

/**
 * Test class to reproduce the exact build errors described in the issue.
 * This demonstrates the class confusion between Android's SmsMessage and custom SmsMessage.
 */
public class BuildErrorReproductionTest {

    @Test
    public void testReproduceSmsMessageConstructorError() {
        // This test reproduces the exact error pattern from the issue:
        // "constructor SmsMessage in class SmsMessage cannot be applied to given types"
        
        String senderAddress = "+1234567890";
        String messageBody = "Test message body";
        long timestamp = System.currentTimeMillis();
        
        // If we accidentally import android.telephony.SmsMessage instead of our custom one,
        // this would fail with the exact error from the issue:
        // "required: no arguments, found: String,String,Date"
        
        try {
            // This should work with our custom SmsMessage class
            SmsMessage smsMessage = new SmsMessage(senderAddress, messageBody, new Date(timestamp));
            
            // These should also work with our custom class
            smsMessage.setIncoming(true);
            smsMessage.setRead(false); // Incoming messages are initially unread
            
            // Verify the object was created correctly
            assert smsMessage.getAddress().equals(senderAddress);
            assert smsMessage.getOriginalText().equals(messageBody);
            assert smsMessage.isIncoming() == true;
            assert smsMessage.isRead() == false;
            
        } catch (Exception e) {
            // If this fails, it indicates the class confusion issue exists
            throw new AssertionError("SmsMessage class confusion detected: " + e.getMessage(), e);
        }
    }
}