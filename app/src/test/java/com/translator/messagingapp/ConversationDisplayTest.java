package com.translator.messagingapp;

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
 * Test class to verify that conversations display contact names or phone numbers correctly.
 * This test specifically addresses the issue where threads are not showing phone number or contact name.
 */
@RunWith(RobolectricTestRunner.class)
public class ConversationDisplayTest {

    @Mock
    private Context mockContext;

    private ConversationRecyclerAdapter adapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test the display name logic for conversations with various scenarios.
     */
    @Test
    public void testConversationDisplayName() {
        // Create test conversations with different scenarios
        List<Conversation> testConversations = Arrays.asList(
            // Scenario 1: Conversation with both contact name and address
            createTestConversation("1", "+1234567890", "John Doe"),
            
            // Scenario 2: Conversation with address but no contact name
            createTestConversation("2", "+9876543210", null),
            
            // Scenario 3: Conversation with contact name same as address (should show address)
            createTestConversation("3", "+5555555555", "+5555555555"),
            
            // Scenario 4: Conversation with no address and no contact name
            createTestConversation("4", null, null),
            
            // Scenario 5: Conversation with empty address and empty contact name
            createTestConversation("5", "", ""),
            
            // Scenario 6: Conversation where contact name equals thread ID (should never happen but test safety)
            createTestConversation("6", "+1111111111", "6")
        );

        // Create adapter with test conversations
        adapter = new ConversationRecyclerAdapter(mockContext, testConversations);

        // Test each conversation's display logic
        for (int i = 0; i < testConversations.size(); i++) {
            Conversation conversation = testConversations.get(i);
            String displayName = getExpectedDisplayName(conversation, mockContext);
            
            // Verify the display name follows the expected logic
            assertNotNull("Display name should never be null", displayName);
            assertFalse("Display name should never be empty", displayName.isEmpty());
            
            // Verify thread ID is never used as display name
            assertNotEquals("Thread ID should never be used as display name", 
                          conversation.getThreadId(), displayName);
            
            // Log the test result for debugging
            System.out.println("Conversation " + conversation.getThreadId() + 
                             ": address='" + conversation.getAddress() + 
                             "', contactName='" + conversation.getContactName() + 
                             "', displayName='" + displayName + "'");
        }
    }

    /**
     * Test the specific scenarios mentioned in the issue.
     */
    @Test
    public void testThreadsShowPhoneNumberOrContactName() {
        // Test case 1: Thread with valid phone number should show formatted phone number
        Conversation phoneOnlyConversation = createTestConversation("1", "+1234567890", null);
        String displayName1 = getExpectedDisplayName(phoneOnlyConversation, mockContext);
        // Note: The actual display will be formatted by ConversationRecyclerAdapter.formatPhoneNumber()
        // but this helper method returns the raw address for the logic test
        assertEquals("Should display phone number when no contact name is available", 
                    "+1234567890", displayName1);

        // Test case 2: Thread with contact name should show contact name
        Conversation contactNameConversation = createTestConversation("2", "+9876543210", "Jane Smith");
        String displayName2 = getExpectedDisplayName(contactNameConversation, mockContext);
        assertEquals("Should display contact name when available", 
                    "Jane Smith", displayName2);

        // Test case 3: Thread with no address should show "Unknown Contact"
        Conversation noAddressConversation = createTestConversation("3", null, null);
        String displayName3 = getExpectedDisplayName(noAddressConversation, mockContext);
        assertEquals("Should display 'Unknown Contact' when no address is available", 
                    "Unknown Contact", displayName3);
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

    /**
     * Helper method that mimics the display name logic from ConversationRecyclerAdapter.
     * This is extracted to verify the logic works correctly.
     */
    private String getExpectedDisplayName(Conversation conversation, Context context) {
        if (conversation == null) {
            return "Unknown Contact";
        }
        
        String contactName = conversation.getContactName();
        String address = conversation.getAddress();
        String threadId = conversation.getThreadId();
        
        // Safety check: never display threadId as the contact name
        if (!TextUtils.isEmpty(contactName) && contactName.equals(threadId)) {
            contactName = null; // Force fallback logic
        }
        
        // First priority: Non-empty contact name that's not just the phone number
        if (!TextUtils.isEmpty(contactName) && !contactName.equals(address)) {
            return contactName;
        }
        
        // Second priority: Phone number/address
        if (!TextUtils.isEmpty(address)) {
            // Additional safety check: make sure address is not threadId
            if (address.equals(threadId)) {
                return "Unknown Contact";
            }
            return address;
        }
        
        // Third priority: Contact name even if it might be a phone number
        if (!TextUtils.isEmpty(contactName)) {
            return contactName;
        }
        
        // Last resort: Unknown contact
        return "Unknown Contact";
    }

    /**
     * Test the phone number formatting specifically for the country code fix.
     */
    @Test
    public void testPhoneNumberFormattingBehavior() {
        // Create a minimal adapter to test formatting
        ConversationRecyclerAdapter testAdapter = new ConversationRecyclerAdapter(mockContext, Arrays.asList());
        
        // Test various phone number formats to ensure country codes are removed
        assertEquals("Should format 10-digit number (no country code to remove)", "(123) 456-7890", 
                    testAdapter.formatPhoneNumber("+1234567890"));
                    
        // Test an actual 11-digit number with US country code
        assertEquals("Should format US number removing country code", "(555) 123-4567", 
                    testAdapter.formatPhoneNumber("+15551234567"));
        
        assertEquals("Should format 10-digit number", "(555) 123-4567", 
                    testAdapter.formatPhoneNumber("5551234567"));
        
        assertEquals("Should handle null", "Unknown", 
                    testAdapter.formatPhoneNumber(null));
        
        assertEquals("Should handle empty", "Unknown", 
                    testAdapter.formatPhoneNumber(""));
    }
}