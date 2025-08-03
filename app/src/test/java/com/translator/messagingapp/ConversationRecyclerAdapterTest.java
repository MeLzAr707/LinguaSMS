package com.translator.messagingapp;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for ConversationRecyclerAdapter avatar functionality.
 * Tests the logic for generating contact avatars and initials.
 */
public class ConversationRecyclerAdapterTest {

    private List<Conversation> testConversations;

    @Before
    public void setUp() {
        testConversations = new ArrayList<>();
        
        // Test conversation with contact name
        Conversation conv1 = new Conversation("1", "+1234567890", "John Doe");
        conv1.setSnippet("Hello there!");
        conv1.setDate(System.currentTimeMillis());
        testConversations.add(conv1);
        
        // Test conversation with only phone number
        Conversation conv2 = new Conversation("2", "+9876543210", null);
        conv2.setSnippet("How are you?");
        conv2.setDate(System.currentTimeMillis() - 3600000); // 1 hour ago
        testConversations.add(conv2);
        
        // Test conversation with empty contact name
        Conversation conv3 = new Conversation("3", "+5555551234", "");
        conv3.setSnippet("See you soon");
        conv3.setDate(System.currentTimeMillis() - 86400000); // 1 day ago
        testConversations.add(conv3);
    }

    @Test
    public void conversationList_notNull() {
        assertNotNull(testConversations);
        assertEquals(3, testConversations.size());
    }

    @Test
    public void conversation_withContactName_hasCorrectData() {
        Conversation conv = testConversations.get(0);
        assertEquals("John Doe", conv.getContactName());
        assertEquals("+1234567890", conv.getAddress());
        assertEquals("Hello there!", conv.getSnippet());
    }

    @Test
    public void conversation_withoutContactName_usesPhoneNumber() {
        Conversation conv = testConversations.get(1);
        assertNull(conv.getContactName());
        assertEquals("+9876543210", conv.getAddress());
        
        // Test the display name logic similar to adapter
        String displayName = conv.getContactName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = conv.getAddress();
        }
        assertEquals("+9876543210", displayName);
    }

    @Test
    public void conversation_withEmptyContactName_usesPhoneNumber() {
        Conversation conv = testConversations.get(2);
        assertEquals("", conv.getContactName());
        assertEquals("+5555551234", conv.getAddress());
        
        // Test the display name logic similar to adapter
        String displayName = conv.getContactName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = conv.getAddress();
        }
        assertEquals("+5555551234", displayName);
    }

    @Test
    public void contactInitials_extractedCorrectly() {
        // Test initials extraction for different scenarios
        assertEquals("J", ContactUtils.getContactInitial("John Doe"));
        assertEquals("9", ContactUtils.getContactInitial("+9876543210"));
        assertEquals("5", ContactUtils.getContactInitial("+5555551234"));
    }

    @Test
    public void contactColors_areConsistent() {
        // Same name should always return same color
        String name = "John Doe";
        int color1 = ContactUtils.getContactColor(name);
        int color2 = ContactUtils.getContactColor(name);
        assertEquals(color1, color2);
    }
}