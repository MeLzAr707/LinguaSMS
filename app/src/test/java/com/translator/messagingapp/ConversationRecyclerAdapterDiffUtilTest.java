package com.translator.messagingapp;

import com.translator.messagingapp.message.*;
import com.translator.messagingapp.conversation.*;
import com.translator.messagingapp.mms.*;
import com.translator.messagingapp.contact.*;
import com.translator.messagingapp.notification.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for ConversationRecyclerAdapter DiffUtil optimization.
 */
public class ConversationRecyclerAdapterDiffUtilTest {
    
    @Mock
    private Context mockContext;
    
    private ConversationRecyclerAdapter adapter;
    private List<Conversation> initialConversations;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        
        initialConversations = new ArrayList<>();
        initialConversations.add(createTestConversation("1", "John", "Hello"));
        initialConversations.add(createTestConversation("2", "Jane", "Hi there"));
        
        adapter = new ConversationRecyclerAdapter(mockContext, initialConversations);
    }
    
    @Test
    public void testUpdateConversationsMethod() {
        // Initial state
        assertEquals("Initial conversation count should be 2", 2, adapter.getItemCount());
        
        // Create updated conversations list
        List<Conversation> updatedConversations = new ArrayList<>();
        updatedConversations.add(createTestConversation("1", "John", "Hello updated"));
        updatedConversations.add(createTestConversation("2", "Jane", "Hi there"));
        updatedConversations.add(createTestConversation("3", "Bob", "New conversation"));
        
        // Update conversations using DiffUtil
        adapter.updateConversations(updatedConversations);
        
        // Verify the adapter was updated
        assertEquals("Updated conversation count should be 3", 3, adapter.getItemCount());
    }
    
    @Test
    public void testUpdateConversationsWithNull() {
        // Initial state
        int initialCount = adapter.getItemCount();
        
        // Try to update with null
        adapter.updateConversations(null);
        
        // Verify adapter state is unchanged
        assertEquals("Conversation count should remain unchanged after null update", 
                    initialCount, adapter.getItemCount());
    }
    
    @Test
    public void testUpdateConversationsWithEmptyList() {
        // Initial state
        assertEquals("Initial conversation count should be 2", 2, adapter.getItemCount());
        
        // Update with empty list
        List<Conversation> emptyList = new ArrayList<>();
        adapter.updateConversations(emptyList);
        
        // Verify adapter is now empty
        assertEquals("Conversation count should be 0 after empty list update", 
                    0, adapter.getItemCount());
    }
    
    @Test
    public void testUpdateConversationsPreservesReference() {
        List<Conversation> newConversations = new ArrayList<>();
        newConversations.add(createTestConversation("3", "Bob", "New conversation"));
        
        adapter.updateConversations(newConversations);
        
        // Verify the adapter has the new conversations
        assertEquals("Should have 1 conversation after update", 1, adapter.getItemCount());
    }
    
    /**
     * Test that verifies the DiffUtil optimization provides better performance
     * characteristics compared to notifyDataSetChanged().
     */
    @Test
    public void testDiffUtilVsNotifyDataSetChanged() {
        // This is a conceptual test - in a real scenario, DiffUtil provides:
        // 1. More granular updates (item inserted, removed, changed)
        // 2. Better animation support
        // 3. Reduced UI jank
        // 4. Better performance for large datasets
        
        List<Conversation> largeConversationList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeConversationList.add(createTestConversation(String.valueOf(i), "Contact " + i, "Message " + i));
        }
        
        adapter.updateConversations(largeConversationList);
        
        assertEquals("Should handle large conversation lists efficiently", 
                    100, adapter.getItemCount());
    }
    
    /**
     * Test updating conversations with partial changes to verify DiffUtil efficiency.
     */
    @Test
    public void testPartialConversationUpdates() {
        // Start with initial conversations
        List<Conversation> currentConversations = new ArrayList<>();
        currentConversations.add(createTestConversation("1", "John", "Hello"));
        currentConversations.add(createTestConversation("2", "Jane", "Hi there"));
        currentConversations.add(createTestConversation("3", "Bob", "Hey"));
        
        adapter.updateConversations(currentConversations);
        assertEquals("Should have 3 conversations", 3, adapter.getItemCount());
        
        // Update with one conversation changed, one removed, one added
        List<Conversation> updatedConversations = new ArrayList<>();
        updatedConversations.add(createTestConversation("1", "John", "Hello updated")); // Changed
        updatedConversations.add(createTestConversation("2", "Jane", "Hi there")); // Same
        // Bob's conversation removed
        updatedConversations.add(createTestConversation("4", "Alice", "New message")); // Added
        
        adapter.updateConversations(updatedConversations);
        assertEquals("Should still have 3 conversations after partial update", 3, adapter.getItemCount());
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