package com.translator.messagingapp;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for ConversationDiffCallback to verify DiffUtil optimization works correctly.
 */
public class ConversationDiffCallbackTest {
    
    private List<Conversation> oldConversations;
    private List<Conversation> newConversations;
    
    @Before
    public void setUp() {
        oldConversations = new ArrayList<>();
        newConversations = new ArrayList<>();
    }
    
    @Test
    public void testListSizes() {
        // Create test conversations
        oldConversations.add(createTestConversation("1", "John", "Hello"));
        oldConversations.add(createTestConversation("2", "Jane", "Hi there"));
        
        newConversations.add(createTestConversation("1", "John", "Hello"));
        newConversations.add(createTestConversation("2", "Jane", "Hi there"));
        newConversations.add(createTestConversation("3", "Bob", "New conversation"));
        
        ConversationDiffCallback callback = new ConversationDiffCallback(oldConversations, newConversations);
        
        assertEquals("Old list size should be 2", 2, callback.getOldListSize());
        assertEquals("New list size should be 3", 3, callback.getNewListSize());
    }
    
    @Test
    public void testItemsTheSame() {
        Conversation conv1Old = createTestConversation("1", "John", "Hello");
        Conversation conv1New = createTestConversation("1", "John", "Hello Updated");
        
        oldConversations.add(conv1Old);
        newConversations.add(conv1New);
        
        ConversationDiffCallback callback = new ConversationDiffCallback(oldConversations, newConversations);
        
        assertTrue("Conversations with same thread ID should be considered the same item", 
                  callback.areItemsTheSame(0, 0));
    }
    
    @Test
    public void testItemsNotTheSame() {
        Conversation conv1 = createTestConversation("1", "John", "Hello");
        Conversation conv2 = createTestConversation("2", "Jane", "Hi");
        
        oldConversations.add(conv1);
        newConversations.add(conv2);
        
        ConversationDiffCallback callback = new ConversationDiffCallback(oldConversations, newConversations);
        
        assertFalse("Conversations with different thread IDs should not be considered the same item", 
                   callback.areItemsTheSame(0, 0));
    }
    
    @Test
    public void testContentsTheSame() {
        Conversation conv1 = createTestConversation("1", "John", "Hello");
        Conversation conv2 = createTestConversation("1", "John", "Hello");
        
        oldConversations.add(conv1);
        newConversations.add(conv2);
        
        ConversationDiffCallback callback = new ConversationDiffCallback(oldConversations, newConversations);
        
        assertTrue("Conversations with identical content should be considered the same", 
                  callback.areContentsTheSame(0, 0));
    }
    
    @Test
    public void testContentsNotTheSame() {
        Conversation conv1 = createTestConversation("1", "John", "Hello");
        Conversation conv2 = createTestConversation("1", "John", "Hello Updated");
        
        oldConversations.add(conv1);
        newConversations.add(conv2);
        
        ConversationDiffCallback callback = new ConversationDiffCallback(oldConversations, newConversations);
        
        assertFalse("Conversations with different content should not be considered the same", 
                   callback.areContentsTheSame(0, 0));
    }
    
    @Test
    public void testUnreadStateChange() {
        Conversation conv1 = createTestConversation("1", "John", "Hello");
        conv1.setRead(false);
        
        Conversation conv2 = createTestConversation("1", "John", "Hello");
        conv2.setRead(true);
        
        oldConversations.add(conv1);
        newConversations.add(conv2);
        
        ConversationDiffCallback callback = new ConversationDiffCallback(oldConversations, newConversations);
        
        assertTrue("Conversations should be the same item (same thread ID)", 
                  callback.areItemsTheSame(0, 0));
        assertFalse("Conversations with different read state should have different content", 
                   callback.areContentsTheSame(0, 0));
    }
    
    @Test
    public void testMessageCountChange() {
        Conversation conv1 = createTestConversation("1", "John", "Hello");
        conv1.setMessageCount(5);
        
        Conversation conv2 = createTestConversation("1", "John", "Hello");
        conv2.setMessageCount(6);
        
        oldConversations.add(conv1);
        newConversations.add(conv2);
        
        ConversationDiffCallback callback = new ConversationDiffCallback(oldConversations, newConversations);
        
        assertTrue("Conversations should be the same item (same thread ID)", 
                  callback.areItemsTheSame(0, 0));
        assertFalse("Conversations with different message count should have different content", 
                   callback.areContentsTheSame(0, 0));
    }
    
    @Test
    public void testEmptyLists() {
        ConversationDiffCallback callback = new ConversationDiffCallback(oldConversations, newConversations);
        
        assertEquals("Empty old list should have size 0", 0, callback.getOldListSize());
        assertEquals("Empty new list should have size 0", 0, callback.getNewListSize());
    }
    
    /**
     * Helper method to create a test conversation with basic properties.
     */
    private Conversation createTestConversation(String threadId, String contactName, String snippet) {
        Conversation conversation = new Conversation();
        conversation.setThreadId(threadId);
        conversation.setContactName(contactName);
        conversation.setSnippet(snippet);
        conversation.setDate(System.currentTimeMillis());
        conversation.setRead(true);
        conversation.setMessageCount(1);
        conversation.setAddress("1234567890");
        return conversation;
    }
}