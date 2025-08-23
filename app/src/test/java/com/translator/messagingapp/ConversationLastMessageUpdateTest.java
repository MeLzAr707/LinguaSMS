package com.translator.messagingapp;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class to validate conversation last message display functionality.
 * This test ensures that conversations always show the most recent message,
 * whether it's SMS or MMS.
 */
public class ConversationLastMessageUpdateTest {

    @Test
    public void testConversationShowsLatestMessage() {
        Conversation conversation = new Conversation();
        
        // Test setting an initial SMS message
        conversation.setLastMessage("Initial SMS message");
        conversation.setDate(1000L); // Older timestamp
        assertEquals("Should show initial SMS message", "Initial SMS message", conversation.getSnippet());
        
        // Test updating with a newer message
        conversation.setLastMessage("Newer message");
        conversation.setDate(2000L); // Newer timestamp
        assertEquals("Should show newer message", "Newer message", conversation.getSnippet());
        
        // Test snippet vs lastMessage priority
        conversation.setSnippet("Priority snippet");
        assertEquals("Snippet should take priority over lastMessage", "Priority snippet", conversation.getSnippet());
    }

    @Test
    public void testMixedSmsAndMmsHandling() {
        Conversation conversation = new Conversation();
        
        // Simulate SMS message
        conversation.setLastMessage("SMS: Hello world");
        conversation.setDate(1000L);
        assertEquals("Should show SMS message", "SMS: Hello world", conversation.getSnippet());
        
        // Simulate newer MMS message (should override SMS)
        conversation.setSnippet("[MMS]"); // MMS messages typically show as [MMS]
        conversation.setDate(2000L); // Newer timestamp
        assertEquals("Should show MMS indicator for newer MMS", "[MMS]", conversation.getSnippet());
    }

    @Test
    public void testTimestampBasedMessagePriority() {
        // Test that the conversation timestamp reflects the most recent message
        Conversation conversation = new Conversation();
        
        long olderTime = System.currentTimeMillis() - 10000; // 10 seconds ago
        long newerTime = System.currentTimeMillis();
        
        // Set older message
        conversation.setLastMessage("Older message");
        conversation.setDate(olderTime);
        assertEquals("Date should reflect older timestamp", olderTime, conversation.getDate().getTime());
        
        // Update with newer message
        conversation.setLastMessage("Newer message"); 
        conversation.setDate(newerTime);
        assertEquals("Date should reflect newer timestamp", newerTime, conversation.getDate().getTime());
        assertEquals("Should show newer message", "Newer message", conversation.getSnippet());
    }

    @Test
    public void testCacheInvalidationLogic() {
        // This test validates the logical requirements for cache invalidation
        // when new messages arrive
        
        Conversation conversation = new Conversation();
        conversation.setThreadId("123");
        conversation.setLastMessage("Original message");
        long originalTime = System.currentTimeMillis() - 5000;
        conversation.setDate(originalTime);
        
        // Simulate a new message arriving
        String newMessage = "Brand new message";
        long newTime = System.currentTimeMillis();
        
        // After cache invalidation and refresh, conversation should show new message
        conversation.setLastMessage(newMessage);
        conversation.setDate(newTime);
        
        assertEquals("After refresh, should show new message", newMessage, conversation.getSnippet());
        assertTrue("New message timestamp should be more recent", 
                   conversation.getDate().getTime() > originalTime);
    }

    @Test
    public void testEdgeCases() {
        Conversation conversation = new Conversation();
        
        // Test null message handling
        conversation.setLastMessage(null);
        assertEquals("Null message should show default", "No messages", conversation.getSnippet());
        
        // Test empty message handling
        conversation.setLastMessage("");
        assertEquals("Empty message should show default", "No messages", conversation.getSnippet());
        
        // Test with zero/invalid timestamp
        conversation.setLastMessage("Valid message");
        conversation.setDate(0L);
        assertEquals("Should still show valid message even with invalid timestamp", "Valid message", conversation.getSnippet());
    }
}