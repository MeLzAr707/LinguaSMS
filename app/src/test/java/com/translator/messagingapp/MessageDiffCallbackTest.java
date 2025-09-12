package com.translator.messagingapp;

import com.translator.messagingapp.message.*;
import com.translator.messagingapp.conversation.*;
import com.translator.messagingapp.mms.*;
import com.translator.messagingapp.contact.*;
import com.translator.messagingapp.notification.*;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for MessageDiffCallback to verify DiffUtil optimization works correctly.
 */
public class MessageDiffCallbackTest {
    
    private List<Message> oldMessages;
    private List<Message> newMessages;
    
    @Before
    public void setUp() {
        oldMessages = new ArrayList<>();
        newMessages = new ArrayList<>();
    }
    
    @Test
    public void testListSizes() {
        // Create test messages
        oldMessages.add(createTestMessage(1L, "Hello"));
        oldMessages.add(createTestMessage(2L, "World"));
        
        newMessages.add(createTestMessage(1L, "Hello"));
        newMessages.add(createTestMessage(2L, "World"));
        newMessages.add(createTestMessage(3L, "New message"));
        
        MessageDiffCallback callback = new MessageDiffCallback(oldMessages, newMessages);
        
        assertEquals("Old list size should be 2", 2, callback.getOldListSize());
        assertEquals("New list size should be 3", 3, callback.getNewListSize());
    }
    
    @Test
    public void testItemsTheSame() {
        Message message1Old = createTestMessage(1L, "Hello");
        Message message1New = createTestMessage(1L, "Hello Updated");
        
        oldMessages.add(message1Old);
        newMessages.add(message1New);
        
        MessageDiffCallback callback = new MessageDiffCallback(oldMessages, newMessages);
        
        assertTrue("Messages with same ID should be considered the same item", 
                  callback.areItemsTheSame(0, 0));
    }
    
    @Test
    public void testItemsNotTheSame() {
        Message message1 = createTestMessage(1L, "Hello");
        Message message2 = createTestMessage(2L, "World");
        
        oldMessages.add(message1);
        newMessages.add(message2);
        
        MessageDiffCallback callback = new MessageDiffCallback(oldMessages, newMessages);
        
        assertFalse("Messages with different IDs should not be considered the same item", 
                   callback.areItemsTheSame(0, 0));
    }
    
    @Test
    public void testContentsTheSame() {
        Message message1 = createTestMessage(1L, "Hello");
        Message message2 = createTestMessage(1L, "Hello");
        
        oldMessages.add(message1);
        newMessages.add(message2);
        
        MessageDiffCallback callback = new MessageDiffCallback(oldMessages, newMessages);
        
        assertTrue("Messages with identical content should be considered the same", 
                  callback.areContentsTheSame(0, 0));
    }
    
    @Test
    public void testContentsNotTheSame() {
        Message message1 = createTestMessage(1L, "Hello");
        Message message2 = createTestMessage(1L, "Hello Updated");
        
        oldMessages.add(message1);
        newMessages.add(message2);
        
        MessageDiffCallback callback = new MessageDiffCallback(oldMessages, newMessages);
        
        assertFalse("Messages with different content should not be considered the same", 
                   callback.areContentsTheSame(0, 0));
    }
    
    @Test
    public void testTranslationStateChange() {
        Message message1 = createTestMessage(1L, "Hello");
        message1.setTranslatedText(null);
        
        Message message2 = createTestMessage(1L, "Hello");
        message2.setTranslatedText("Hola");
        
        oldMessages.add(message1);
        newMessages.add(message2);
        
        MessageDiffCallback callback = new MessageDiffCallback(oldMessages, newMessages);
        
        assertTrue("Messages should be the same item (same ID)", 
                  callback.areItemsTheSame(0, 0));
        assertFalse("Messages with different translation state should have different content", 
                   callback.areContentsTheSame(0, 0));
    }
    
    @Test
    public void testEmptyLists() {
        MessageDiffCallback callback = new MessageDiffCallback(oldMessages, newMessages);
        
        assertEquals("Empty old list should have size 0", 0, callback.getOldListSize());
        assertEquals("Empty new list should have size 0", 0, callback.getNewListSize());
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