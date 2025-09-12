package com.translator.messagingapp;

import com.translator.messagingapp.message.*;
import com.translator.messagingapp.conversation.*;
import com.translator.messagingapp.mms.*;
import com.translator.messagingapp.contact.*;
import com.translator.messagingapp.notification.*;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class to verify country code fixes and conversation display improvements.
 */
public class CountryCodeFixTest {

    /**
     * Test phone number formatting without country codes.
     */
    @Test
    public void testPhoneNumberFormattingWithoutCountryCode() {
        ConversationRecyclerAdapter adapter = new ConversationRecyclerAdapter(null, null);
        
        // Test US number with country code
        String result1 = adapter.formatPhoneNumber("+15551234567");
        assertEquals("Should format US number without country code", "(555) 123-4567", result1);
        
        // Test US number with country code (no plus)
        String result2 = adapter.formatPhoneNumber("15551234567");
        assertEquals("Should format US number without country code", "(555) 123-4567", result2);
        
        // Test regular 10-digit US number
        String result3 = adapter.formatPhoneNumber("5551234567");
        assertEquals("Should format 10-digit number", "(555) 123-4567", result3);
        
        // Test 7-digit number
        String result4 = adapter.formatPhoneNumber("1234567");
        assertEquals("Should format 7-digit number", "123-4567", result4);
        
        // Test international number (remove country code for display)
        String result5 = adapter.formatPhoneNumber("+447123456789");
        assertEquals("Should format international number without country code", "(123) 456-789", result5);
        
        // Test null/empty
        String result6 = adapter.formatPhoneNumber(null);
        assertEquals("Should handle null", "Unknown", result6);
        
        String result7 = adapter.formatPhoneNumber("");
        assertEquals("Should handle empty string", "Unknown", result7);
    }

    /**
     * Test that phone numbers with spaces and formatting are cleaned properly.
     */
    @Test
    public void testPhoneNumberCleaning() {
        ConversationRecyclerAdapter adapter = new ConversationRecyclerAdapter(null, null);
        
        // Test number with spaces and formatting
        String result1 = adapter.formatPhoneNumber("+1 (555) 123-4567");
        assertEquals("Should clean and format properly", "(555) 123-4567", result1);
        
        // Test number with various formatting
        String result2 = adapter.formatPhoneNumber("1-555-123-4567");
        assertEquals("Should clean and format properly", "(555) 123-4567", result2);
    }
}