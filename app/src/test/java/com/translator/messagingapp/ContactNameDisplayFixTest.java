package com.translator.messagingapp;

import com.translator.messagingapp.message.*;
import com.translator.messagingapp.conversation.*;
import com.translator.messagingapp.mms.*;
import com.translator.messagingapp.contact.*;
import com.translator.messagingapp.notification.*;

import android.content.Context;
import android.text.TextUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test class to verify the fix for group messages showing 'Unknown' or 'Unk-nown'
 * instead of contact names or phone numbers.
 */
@RunWith(RobolectricTestRunner.class)
public class ContactNameDisplayFixTest {

    @Mock
    private Context mockContext;

    private ConversationRecyclerAdapter adapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test that the adapter never returns "Unknown" when phone numbers are available.
     */
    @Test
    public void testNeverShowsUnknownWhenPhoneNumberAvailable() {
        // Create test conversations with different scenarios
        List<Conversation> testConversations = Arrays.asList(
            // Single conversation with no contact name but valid phone number
            createTestConversation("1", "+1234567890", null),
            
            // Group conversation with multiple phone numbers
            createTestConversation("2", "+1234567890,+0987654321", null),
            
            // Conversation with empty contact name
            createTestConversation("3", "+5555555555", ""),
            
            // Conversation with "null" as contact name
            createTestConversation("4", "+1111111111", "null")
        );

        adapter = new ConversationRecyclerAdapter(mockContext, testConversations);

        // Test phone number formatting directly
        String formatted1 = adapter.formatPhoneNumber("+1234567890");
        assertNotNull("Should not be null", formatted1);
        assertNotEquals("Should not be 'Unknown'", "Unknown", formatted1);
        assertFalse("Should not contain 'Unknown'", formatted1.toLowerCase().contains("unknown"));
        assertEquals("Should format as (123) 456-7890", "(123) 456-7890", formatted1);

        String formatted2 = adapter.formatPhoneNumber("");
        assertEquals("Should be 'No Number' for empty string", "No Number", formatted2);

        String formatted3 = adapter.formatPhoneNumber(null);
        assertEquals("Should be 'No Number' for null", "No Number", formatted3);
    }

    /**
     * Test that group conversations show meaningful participant information.
     */
    @Test
    public void testGroupConversationsShowParticipantInfo() {
        // Create group conversation
        Conversation groupConv = createTestConversation("group1", "+1234567890,+0987654321,+5555555555", null);
        
        List<Conversation> conversations = Arrays.asList(groupConv);
        adapter = new ConversationRecyclerAdapter(mockContext, conversations);

        // The actual display logic would be tested through the adapter's private methods
        // but we can verify the phone formatting works correctly
        String phone1 = adapter.formatPhoneNumber("+1234567890");
        String phone2 = adapter.formatPhoneNumber("+0987654321");
        
        assertNotEquals("First phone should not be Unknown", "Unknown", phone1);
        assertNotEquals("Second phone should not be Unknown", "Unknown", phone2);
        
        // Should format properly
        assertEquals("Should format first phone correctly", "(123) 456-7890", phone1);
        assertEquals("Should format second phone correctly", "(098) 765-4321", phone2);
    }

    /**
     * Test various edge cases that could cause display issues.
     */
    @Test
    public void testEdgeCases() {
        adapter = new ConversationRecyclerAdapter(mockContext, Arrays.asList());
        
        // Test short numbers
        String short1 = adapter.formatPhoneNumber("123");
        assertEquals("Short number should return as-is", "123", short1);
        
        // Test 7-digit number
        String seven = adapter.formatPhoneNumber("5551234");
        assertEquals("7-digit should format correctly", "555-1234", seven);
        
        // Test 11-digit with country code
        String eleven = adapter.formatPhoneNumber("12345678901");
        assertEquals("11-digit should format without country code", "(234) 567-8901", eleven);
        
        // Test invalid but non-empty
        String invalid = adapter.formatPhoneNumber("abc123");
        assertEquals("Invalid should return as-is", "abc123", invalid);
    }

    /**
     * Test that no corruption occurs that could lead to "unk-nown" display.
     */
    @Test
    public void testNoCorruptionToUnkNown() {
        adapter = new ConversationRecyclerAdapter(mockContext, Arrays.asList());
        
        // Test various inputs that could potentially be corrupted
        String[] testInputs = {
            "uk", "unknown", "unk", "ukn", "un", "u",
            "+uk", "uk1234", "unknown123", null, "", " "
        };
        
        for (String input : testInputs) {
            String result = adapter.formatPhoneNumber(input);
            
            // Most important: never get "unk-nown"
            assertNotEquals("Should never produce 'unk-nown' for input: " + input, 
                          "unk-nown", result);
            
            // Should also not be exactly "Unknown"
            if (input != null && !input.trim().isEmpty()) {
                assertNotEquals("Should not be 'Unknown' when input available: " + input, 
                              "Unknown", result);
            }
        }
    }

    /**
     * Helper method to create a test conversation.
     */
    private Conversation createTestConversation(String threadId, String address, String contactName) {
        Conversation conversation = new Conversation();
        conversation.setThreadId(threadId);
        conversation.setAddress(address);
        conversation.setContactName(contactName);
        conversation.setSnippet("Test message");
        conversation.setDate(System.currentTimeMillis());
        return conversation;
    }
}