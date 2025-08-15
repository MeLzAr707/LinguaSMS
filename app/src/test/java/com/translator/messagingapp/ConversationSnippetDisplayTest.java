package com.translator.messagingapp;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test for Conversation snippet display fixes.
 * Tests the improved snippet handling and display logic.
 */
public class ConversationSnippetDisplayTest {

    @Test
    public void testImprovedGetSnippetMethod() {
        Conversation conversation = new Conversation();
        
        // Test with dedicated snippet
        conversation.setSnippet("Test snippet");
        conversation.setLastMessage("Last message");
        assertEquals("Should return dedicated snippet", "Test snippet", conversation.getSnippet());
        
        // Test fallback to lastMessage when snippet is null
        conversation.setSnippet(null);
        assertEquals("Should fallback to last message", "Last message", conversation.getSnippet());
        
        // Test fallback to lastMessage when snippet is empty
        conversation.setSnippet("");
        assertEquals("Should fallback to last message when snippet is empty", "Last message", conversation.getSnippet());
        
        // Test default message when both are null
        conversation.setSnippet(null);
        conversation.setLastMessage(null);
        assertEquals("Should show default message", "No messages", conversation.getSnippet());
        
        // Test default message when both are empty
        conversation.setSnippet("");
        conversation.setLastMessage("");
        assertEquals("Should show default message when both empty", "No messages", conversation.getSnippet());
    }

    @Test
    public void testSetLastMessageUpdatesSnippet() {
        Conversation conversation = new Conversation();
        
        // Test that setting lastMessage updates snippet
        conversation.setLastMessage("New message");
        assertEquals("Snippet should be updated when lastMessage is set", "New message", conversation.getSnippet());
        
        // Test that empty lastMessage doesn't override existing snippet
        conversation.setSnippet("Existing snippet");
        conversation.setLastMessage("");
        assertEquals("Empty lastMessage should not override existing snippet", "Existing snippet", conversation.getSnippet());
        
        // Test that null lastMessage doesn't override existing snippet
        conversation.setSnippet("Another snippet");
        conversation.setLastMessage(null);
        assertEquals("Null lastMessage should not override existing snippet", "Another snippet", conversation.getSnippet());
    }

    @Test
    public void testConstructorInitializesSnippet() {
        // Test constructor with all fields
        Conversation conversation = new Conversation("123", "+1234567890", "John Doe", "Hello world", System.currentTimeMillis(), 1, 0);
        assertEquals("Constructor should initialize snippet with lastMessage", "Hello world", conversation.getSnippet());
        
        // Test constructor with null lastMessage
        Conversation conversation2 = new Conversation("124", "+1234567891", "Jane Doe", null, System.currentTimeMillis(), 1, 0);
        assertEquals("Constructor with null lastMessage should show default", "No messages", conversation2.getSnippet());
        
        // Test constructor with empty lastMessage
        Conversation conversation3 = new Conversation("125", "+1234567892", "Bob Smith", "", System.currentTimeMillis(), 1, 0);
        assertEquals("Constructor with empty lastMessage should show default", "No messages", conversation3.getSnippet());
    }

    @Test
    public void testBackwardCompatibility() {
        Conversation conversation = new Conversation();
        
        // Test that existing code using only setLastMessage still works
        conversation.setLastMessage("Message from old code");
        assertEquals("Old code using setLastMessage should still work", "Message from old code", conversation.getSnippet());
        
        // Test that setting snippet explicitly takes precedence
        conversation.setSnippet("Explicit snippet");
        assertEquals("Explicit snippet should take precedence", "Explicit snippet", conversation.getSnippet());
        
        // Test that updating lastMessage doesn't override explicit snippet
        String originalSnippet = conversation.getSnippet();
        conversation.setLastMessage("New last message");
        // The snippet should be updated since lastMessage was set
        assertEquals("Setting lastMessage should update snippet", "New last message", conversation.getSnippet());
    }

    @Test
    public void testEmptyStringHandling() {
        Conversation conversation = new Conversation();
        
        // Test various empty string scenarios
        conversation.setSnippet("   ");  // whitespace only
        conversation.setLastMessage("Valid message");
        // Note: TextUtils.isEmpty() considers whitespace-only strings as non-empty
        assertEquals("Whitespace-only snippet should be returned", "   ", conversation.getSnippet());
        
        // Test completely empty
        conversation.setSnippet("");
        assertEquals("Empty snippet should fallback to lastMessage", "Valid message", conversation.getSnippet());
        
        // Test both empty
        conversation.setLastMessage("");
        assertEquals("Both empty should show default", "No messages", conversation.getSnippet());
    }
}