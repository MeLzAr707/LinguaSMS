package com.translator.messagingapp;

import com.translator.messagingapp.message.*;
import com.translator.messagingapp.conversation.*;
import com.translator.messagingapp.mms.*;
import com.translator.messagingapp.contact.*;
import com.translator.messagingapp.notification.*;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class to verify contact name resolution improvements.
 */
public class ContactNameResolutionTest {

    /**
     * Test that conversation objects properly store contact names.
     */
    @Test
    public void testConversationContactNameSetting() {
        Conversation conversation = new Conversation();
        
        // Test setting contact name
        conversation.setContactName("John Doe");
        assertEquals("Should store contact name", "John Doe", conversation.getContactName());
        
        // Test setting address
        conversation.setAddress("+15551234567");
        assertEquals("Should store address", "+15551234567", conversation.getAddress());
        
        // Verify contact name doesn't equal thread ID (preventing the known issue)
        conversation.setThreadId("12345");
        assertNotEquals("Contact name should never equal thread ID", 
                       conversation.getThreadId(), conversation.getContactName());
    }

    /**
     * Test that contact name is properly handled when null or empty.
     */
    @Test
    public void testContactNameNullHandling() {
        Conversation conversation = new Conversation();
        
        // Test null contact name
        conversation.setContactName(null);
        conversation.setAddress("+15551234567");
        
        // In the UI, this should fall back to formatted phone number
        assertNull("Should handle null contact name", conversation.getContactName());
        assertNotNull("Should have address for fallback", conversation.getAddress());
        
        // Test empty contact name
        conversation.setContactName("");
        assertTrue("Empty contact name should be handled", 
                  conversation.getContactName() == null || conversation.getContactName().isEmpty());
    }

    /**
     * Test conversation date handling for sorting.
     */
    @Test
    public void testConversationDateSorting() {
        Conversation conv1 = new Conversation();
        Conversation conv2 = new Conversation();
        
        // Set dates - conv2 should be newer
        conv1.setDate(1000000L);
        conv2.setDate(2000000L);
        
        // Test that newer conversation has later date
        assertTrue("Conv2 should be newer than conv1", 
                  conv2.getDate().getTime() > conv1.getDate().getTime());
    }
}