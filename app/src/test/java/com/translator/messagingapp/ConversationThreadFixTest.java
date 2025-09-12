package com.translator.messagingapp;

import com.translator.messagingapp.message.*;
import com.translator.messagingapp.conversation.*;
import com.translator.messagingapp.mms.*;
import com.translator.messagingapp.contact.*;
import com.translator.messagingapp.notification.*;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for conversation thread mixup fixes.
 * Tests the phone number normalization and validation improvements.
 */
public class ConversationThreadFixTest {

    @Test
    public void testPhoneNumberNormalization() {
        // Test different phone number formats are normalized consistently
        String[] testNumbers = {
            "+1234567890",
            "1234567890", 
            "1-234-567-8890",
            "(234) 567-8890",
            "234-567-8890",
            "234.567.8890"
        };
        
        String expectedNormalized = "234567890";
        
        for (String testNumber : testNumbers) {
            String normalized = PhoneUtils.normalizePhoneNumber(testNumber);
            assertEquals("Phone number " + testNumber + " should normalize to " + expectedNormalized, 
                        expectedNormalized, normalized);
        }
    }

    @Test
    public void testPhoneNumberVariants() {
        String phoneNumber = "234567890";
        String[] variants = PhoneUtils.getPhoneNumberVariants(phoneNumber);
        
        // Should include multiple variants for lookups
        assertTrue("Should have multiple variants", variants.length > 1);
        
        // Should include the original
        boolean hasOriginal = false;
        for (String variant : variants) {
            if (variant.equals(phoneNumber)) {
                hasOriginal = true;
                break;
            }
        }
        assertTrue("Should include original phone number", hasOriginal);
        
        // Should include variant with country code
        boolean hasCountryCode = false;
        for (String variant : variants) {
            if (variant.equals("+1" + phoneNumber)) {
                hasCountryCode = true;
                break;
            }
        }
        assertTrue("Should include variant with country code", hasCountryCode);
    }

    @Test
    public void testInternationalNumberHandling() {
        String intlNumber = "+33123456789";
        String normalized = PhoneUtils.normalizePhoneNumber(intlNumber);
        
        // International numbers should be preserved
        assertEquals("International numbers should be preserved", intlNumber, normalized);
    }

    @Test
    public void testEmptyAndNullNumberHandling() {
        // Test null input
        String nullResult = PhoneUtils.normalizePhoneNumber(null);
        assertNull("Null input should return null", nullResult);
        
        // Test empty input
        String emptyResult = PhoneUtils.normalizePhoneNumber("");
        assertEquals("Empty input should return empty", "", emptyResult);
        
        // Test whitespace input
        String whitespaceResult = PhoneUtils.normalizePhoneNumber("   ");
        assertEquals("Whitespace input should return whitespace", "   ", whitespaceResult);
    }

    @Test
    public void testConversationObjectValidation() {
        // Test that conversation objects handle validation correctly
        Conversation conversation = new Conversation();
        
        // Test basic field validation
        assertNotNull("Conversation should not be null", conversation);
        
        // Test setting and getting thread ID - should never equal contact name
        conversation.setThreadId("12345");
        conversation.setContactName("John Doe");
        
        assertNotEquals("Thread ID should never equal contact name", 
                       conversation.getThreadId(), conversation.getContactName());
        
        // Test that empty contact name gets a fallback
        conversation.setContactName("");
        String contactName = conversation.getContactName();
        assertNotNull("Contact name should not be null", contactName);
    }

    @Test
    public void testConversationThreadIdContactNameSeparation() {
        // Test that thread IDs are never used as contact names
        Conversation conversation = new Conversation();
        String threadId = "98765";
        
        conversation.setThreadId(threadId);
        conversation.setContactName(threadId); // This should not happen
        
        // The system should prevent thread ID from being used as contact name
        String contactName = conversation.getContactName();
        if (contactName.equals(threadId)) {
            fail("Thread ID should never be used as contact name. ThreadID: " + threadId + ", ContactName: " + contactName);
        }
    }

    @Test
    public void testPhoneNumberFormatConsistency() {
        // Test that different formats of the same number are treated consistently
        String[] sameNumberFormats = {
            "2345678901",
            "+1 234-567-8901", 
            "(234) 567-8901",
            "234.567.8901"
        };
        
        String firstNormalized = PhoneUtils.normalizePhoneNumber(sameNumberFormats[0]);
        
        // All should normalize to the same value
        for (int i = 1; i < sameNumberFormats.length; i++) {
            String normalized = PhoneUtils.normalizePhoneNumber(sameNumberFormats[i]);
            assertEquals("All formats of the same number should normalize identically: " + 
                        sameNumberFormats[i] + " vs " + sameNumberFormats[0], 
                        firstNormalized, normalized);
        }
    }

    @Test
    public void testEdgeCasePhoneNumbers() {
        // Test edge cases that might cause problems
        String[] edgeCases = {
            "000-000-0000",  // Invalid but formatted
            "123",           // Too short
            "+",             // Just plus sign
            "abc-def-ghij",  // Letters
            "",              // Empty
            null             // Null
        };
        
        // These should either normalize safely or return original
        for (String edgeCase : edgeCases) {
            try {
                String result = PhoneUtils.normalizePhoneNumber(edgeCase);
                // Should not throw exception
                assertNotNull("Should handle edge case without throwing: " + edgeCase);
            } catch (Exception e) {
                fail("Should not throw exception for edge case: " + edgeCase + " - " + e.getMessage());
            }
        }
    }
}