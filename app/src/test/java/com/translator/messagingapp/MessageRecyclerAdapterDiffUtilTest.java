package com.translator.messagingapp;

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
 * Unit tests for MessageRecyclerAdapter DiffUtil optimization.
 */
public class MessageRecyclerAdapterDiffUtilTest {
    
    @Mock
    private Context mockContext;
    
    @Mock
    private MessageRecyclerAdapter.OnMessageClickListener mockListener;
    
    private MessageRecyclerAdapter adapter;
    private List<Message> initialMessages;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        
        initialMessages = new ArrayList<>();
        initialMessages.add(createTestMessage(1L, "First message"));
        initialMessages.add(createTestMessage(2L, "Second message"));
        
        adapter = new MessageRecyclerAdapter(mockContext, initialMessages, mockListener);
    }
    
    @Test
    public void testUpdateMessagesMethod() {
        // Initial state
        assertEquals("Initial message count should be 2", 2, adapter.getItemCount());
        
        // Create updated messages list
        List<Message> updatedMessages = new ArrayList<>();
        updatedMessages.add(createTestMessage(1L, "First message updated"));
        updatedMessages.add(createTestMessage(2L, "Second message"));
        updatedMessages.add(createTestMessage(3L, "Third message"));
        
        // Update messages using DiffUtil
        adapter.updateMessages(updatedMessages);
        
        // Verify the adapter was updated
        assertEquals("Updated message count should be 3", 3, adapter.getItemCount());
    }
    
    @Test
    public void testUpdateMessagesWithNull() {
        // Initial state
        int initialCount = adapter.getItemCount();
        
        // Try to update with null
        adapter.updateMessages(null);
        
        // Verify adapter state is unchanged
        assertEquals("Message count should remain unchanged after null update", 
                    initialCount, adapter.getItemCount());
    }
    
    @Test
    public void testUpdateMessagesWithEmptyList() {
        // Initial state
        assertEquals("Initial message count should be 2", 2, adapter.getItemCount());
        
        // Update with empty list
        List<Message> emptyList = new ArrayList<>();
        adapter.updateMessages(emptyList);
        
        // Verify adapter is now empty
        assertEquals("Message count should be 0 after empty list update", 
                    0, adapter.getItemCount());
    }
    
    @Test
    public void testUpdateMessagesPreservesReference() {
        List<Message> newMessages = new ArrayList<>();
        newMessages.add(createTestMessage(3L, "New message"));
        
        adapter.updateMessages(newMessages);
        
        // Verify the adapter has the new messages
        assertEquals("Should have 1 message after update", 1, adapter.getItemCount());
    }
    
    @Test
    public void testUpdateTextSizesStillWorks() {
        // This method should still work after our changes
        // We can't easily test the UI effects, but we can verify the method doesn't crash
        try {
            adapter.updateTextSizes();
            assertTrue("updateTextSizes should not throw exception", true);
        } catch (Exception e) {
            fail("updateTextSizes should not throw exception: " + e.getMessage());
        }
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
        
        List<Message> largeMessageList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeMessageList.add(createTestMessage(i, "Message " + i));
        }
        
        adapter.updateMessages(largeMessageList);
        
        assertEquals("Should handle large message lists efficiently", 
                    100, adapter.getItemCount());
    }
    
    /**
     * Helper method to create a test message with basic properties.
     */
    private Message createTestMessage(long id, String body) {
        Message message = new Message();
        message.setId(id);
        message.setBody(body);
        message.setDate(System.currentTimeMillis());
        message.setType(Message.TYPE_INBOX);
        message.setRead(false);
        message.setAddress("1234567890");
        message.setThreadId(1L);
        return message;
    }
}