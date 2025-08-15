package com.translator.messagingapp;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test for conversation snippet display functionality.
 * Tests the fix for issue #153 where snippets were not displaying properly.
 */
public class ConversationSnippetDisplayTest {

    @Test
    public void testSnippetFallbackLogic() {
        // Test the snippet fallback logic in Conversation class
        Conversation conversation = new Conversation();
        
        // Test 1: Both snippet and lastMessage are set
        conversation.setSnippet("Test snippet");
        conversation.setLastMessage("Test last message");
        assertEquals("Should return snippet when both are set", "Test snippet", conversation.getSnippet());
        
        // Test 2: Only lastMessage is set
        conversation.setSnippet(null);
        conversation.setLastMessage("Only last message");
        assertEquals("Should return lastMessage when snippet is null", "Only last message", conversation.getSnippet());
        
        // Test 3: Empty snippet, lastMessage set
        conversation.setSnippet("");
        conversation.setLastMessage("Only last message");
        assertEquals("Should return lastMessage when snippet is empty", "Only last message", conversation.getSnippet());
        
        // Test 4: Neither is set
        conversation.setSnippet(null);
        conversation.setLastMessage(null);
        assertEquals("Should return 'No messages' when both are null", "No messages", conversation.getSnippet());
        
        // Test 5: Both are empty
        conversation.setSnippet("");
        conversation.setLastMessage("");
        assertEquals("Should return 'No messages' when both are empty", "No messages", conversation.getSnippet());
    }
    
    @Test
    public void testConversationDisplayConsistency() {
        // Test that snippet and lastMessage are consistent after being set properly
        Conversation conversation = new Conversation();
        
        // Simulate what MessageService should do
        String messageText = "Hello, this is a test message";
        conversation.setSnippet(messageText);
        conversation.setLastMessage(messageText);
        
        // Verify both methods return the same value
        assertEquals("getSnippet and getLastMessage should return same value", 
                     conversation.getSnippet(), conversation.getLastMessage());
        
        // Verify the snippet display works
        assertEquals("Should display the actual message text", messageText, conversation.getSnippet());
    }
    
    @Test
    public void testMmsSnippetHandling() {
        // Test MMS snippet handling
        Conversation conversation = new Conversation();
        
        // Simulate MMS case where snippet might be null
        conversation.setSnippet("[MMS]");
        conversation.setLastMessage("[MMS]");
        
        assertEquals("Should display [MMS] for MMS messages", "[MMS]", conversation.getSnippet());
        assertEquals("Should display [MMS] for MMS messages", "[MMS]", conversation.getLastMessage());
    }
}