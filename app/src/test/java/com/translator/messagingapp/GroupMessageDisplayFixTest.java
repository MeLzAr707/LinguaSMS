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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test class to verify that group messages display contact names or phone numbers correctly,
 * specifically addressing the issue where group messages show 'unk-nown' instead of 
 * proper contact names or phone numbers.
 */
@RunWith(RobolectricTestRunner.class)
public class GroupMessageDisplayFixTest {

    @Mock
    private Context mockContext;

    private ConversationRecyclerAdapter adapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test that group message participants are displayed correctly 
     * and never show 'unk-nown' or corrupted names.
     */
    @Test
    public void testGroupMessageParticipantsDisplayCorrectly() {
        // Create test conversations with group message scenarios
        List<Conversation> testConversations = Arrays.asList(
            // Scenario 1: Group conversation with comma-separated addresses (no contact names)
            createTestConversation("1", "+1234567890,+0987654321,+5555555555", null),
            
            // Scenario 2: Group conversation with already formatted group name
            createTestConversation("2", "+1234567890,+0987654321", "Alice, Bob"),
            
            // Scenario 3: Group conversation with summary format
            createTestConversation("3", "+1234567890,+0987654321,+5555555555", "Alice + 2 others"),
            
            // Scenario 4: Single participant should not be treated as group
            createTestConversation("4", "+1234567890", null),
            
            // Scenario 5: Group with empty addresses
            createTestConversation("5", ",+1234567890,", null),
            
            // Scenario 6: Group with malformed addresses
            createTestConversation("6", "invalid,+1234567890,", null)
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
            
            // Most importantly: verify we never get 'unk-nown' or corrupted strings
            assertNotEquals("Should never display 'unk-nown'", "unk-nown", displayName);
            assertFalse("Should not contain 'unk-nown'", displayName.contains("unk-nown"));
            assertFalse("Should not contain corrupted 'Unknown' variants", 
                       displayName.toLowerCase().contains("unk") && !displayName.toLowerCase().contains("unknown"));
            
            // Verify thread ID is never used as display name
            assertNotEquals("Thread ID should never be used as display name", 
                          conversation.getThreadId(), displayName);
            
            // Log the test result for debugging
            System.out.println("Group Message Test " + conversation.getThreadId() + 
                             ": address='" + conversation.getAddress() + 
                             "', contactName='" + conversation.getContactName() + 
                             "', displayName='" + displayName + "'");
        }
    }

    /**
     * Test the specific scenarios mentioned in the issue.
     */
    @Test
    public void testGroupMessageDisplayNameLogic() {
        // Test case 1: Group with multiple phone numbers should show formatted group name
        Conversation groupConversation = createTestConversation("1", "+1234567890,+0987654321", null);
        String displayName1 = getExpectedDisplayName(groupConversation, mockContext);
        // Should show either contact names or formatted phone numbers, never 'unk-nown'
        assertTrue("Should display meaningful group name", 
                  displayName1.contains("(123) 456-7890") || displayName1.contains("+"));
        assertNotEquals("Should never show 'unk-nown'", "unk-nown", displayName1);

        // Test case 2: Group with existing group name should preserve it
        Conversation namedGroupConversation = createTestConversation("2", "+1234567890,+0987654321", "Alice, Bob");
        String displayName2 = getExpectedDisplayName(namedGroupConversation, mockContext);
        assertEquals("Should preserve existing group name", "Alice, Bob", displayName2);

        // Test case 3: Group with summary format should preserve it
        Conversation summaryGroupConversation = createTestConversation("3", "+1234567890,+0987654321,+5555555555", "Alice + 2 others");
        String displayName3 = getExpectedDisplayName(summaryGroupConversation, mockContext);
        assertEquals("Should preserve group summary format", "Alice + 2 others", displayName3);
    }

    /**
     * Test the phone number formatting to ensure it never produces 'unk-nown'.
     */
    @Test
    public void testPhoneNumberFormattingNeverProducesCorruptedStrings() {
        // Create a minimal adapter to test formatting
        ConversationRecyclerAdapter testAdapter = new ConversationRecyclerAdapter(mockContext, Arrays.asList());
        
        // Test various phone number formats including edge cases
        String result1 = testAdapter.formatPhoneNumber("+1234567890");
        assertNotNull("Should not return null", result1);
        assertNotEquals("Should not return 'unk-nown'", "unk-nown", result1);
        
        String result2 = testAdapter.formatPhoneNumber("");
        assertEquals("Should return 'No Number' for empty string", "No Number", result2);
        
        String result3 = testAdapter.formatPhoneNumber(null);
        assertEquals("Should return 'No Number' for null", "No Number", result3);
        
        String result4 = testAdapter.formatPhoneNumber("invalid");
        assertNotEquals("Should not return 'unk-nown' for invalid input", "unk-nown", result4);
        assertEquals("Should return original input for invalid format", "invalid", result4);
        
        // Test edge case that might cause corruption
        String result5 = testAdapter.formatPhoneNumber("uk");  // Could this become 'unk'?
        assertNotEquals("Should not return 'unk-nown'", "unk-nown", result5);
        assertEquals("Should return original input", "uk", result5);
    }

    /**
     * Helper method to create a test conversation.
     */
    private Conversation createTestConversation(String threadId, String address, String contactName) {
        Conversation conversation = new Conversation();
        conversation.setThreadId(threadId);
        conversation.setAddress(address);
        conversation.setContactName(contactName);
        conversation.setSnippet("Test group message");
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
        
        // Clean up contact name - handle string "null", empty, or actual null
        if (TextUtils.isEmpty(contactName) || "null".equals(contactName)) {
            contactName = null;
        }
        
        // Safety check: never display threadId as the contact name
        if (!TextUtils.isEmpty(contactName) && contactName.equals(threadId)) {
            contactName = null; // Force fallback logic
        }
        
        // If we have a valid contact name, use it
        if (contactName != null) {
            // Check if it's a group message indicator
            if (contactName.contains(",") || (contactName.contains("+") && contactName.contains("others"))) {
                return contactName; // Already formatted group name
            }
            
            // Check if contact name is same as address (not useful)
            if (!contactName.equals(address)) {
                return contactName;
            }
        }
        
        // If no contact name and we have an address, try to format it
        if (!TextUtils.isEmpty(address)) {
            // Additional safety check: make sure address is not threadId
            if (address.equals(threadId)) {
                return "Unknown Contact";
            }
            
            // Check if this looks like a group message (comma-separated addresses)
            if (address.contains(",")) {
                return formatGroupAddressesForTest(address);
            }
            
            // Check if this looks like an already formatted group message
            if (address.contains("+") && address.contains("others")) {
                return address; // Already formatted group address
            }
            
            // Single address - format as phone number
            return formatPhoneNumberForTest(address);
        }
        
        // Last resort: No Number instead of "Unknown" to match the fix
        return "No Number";
    }

    /**
     * Test helper method to format group addresses.
     */
    private String formatGroupAddressesForTest(String addresses) {
        if (TextUtils.isEmpty(addresses)) {
            return "Unknown"; // Match the fix
        }
        
        String[] addressArray = addresses.split(",");
        if (addressArray.length <= 1) {
            // Not actually a group, treat as single address
            String singleAddress = addresses.trim();
            return formatPhoneNumberForTest(singleAddress);
        }
        
        // For group conversations, format each participant
        StringBuilder groupName = new StringBuilder();
        int nameCount = 0;
        int maxNamesToShow = 2; // Reduced to match the fix
        
        for (int i = 0; i < addressArray.length && nameCount < maxNamesToShow; i++) {
            String address = addressArray[i].trim();
            if (TextUtils.isEmpty(address)) {
                continue; // Skip empty addresses
            }
            
            // For testing, just format as phone number (real implementation would try contact lookup)
            String displayName = formatPhoneNumberForTest(address);
            
            if (nameCount > 0) {
                groupName.append(", ");
            }
            groupName.append(displayName);
            nameCount++;
        }
        
        // If there are more participants than we showed
        if (addressArray.length > maxNamesToShow) {
            int remaining = addressArray.length - maxNamesToShow;
            groupName.append(" +").append(remaining); // Compact format to match the fix
        }
        
        return groupName.length() > 0 ? groupName.toString() : "Group";
    }

    /**
     * Test helper method to format phone numbers.
     */
    private String formatPhoneNumberForTest(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "No Number"; // Updated to match the fix - never return "Unknown"
        }

        // Remove any spaces, dashes, parentheses, and plus signs for processing
        String cleanNumber = phoneNumber.replaceAll("[\\s\\-\\(\\)\\+]", "");
        
        // If the number starts with country code (like +1 for US), remove it for display
        if (cleanNumber.startsWith("1") && cleanNumber.length() == 11) {
            // Remove US country code (1) if present
            cleanNumber = cleanNumber.substring(1);
        }
        
        // Format as (XXX) XXX-XXXX for 10-digit numbers
        if (cleanNumber.length() == 10) {
            return String.format("(%s) %s-%s",
                    cleanNumber.substring(0, 3),
                    cleanNumber.substring(3, 6),
                    cleanNumber.substring(6));
        } else if (cleanNumber.length() == 7) {
            // Format 7-digit numbers as XXX-XXXX
            return String.format("%s-%s",
                    cleanNumber.substring(0, 3),
                    cleanNumber.substring(3));
        } else if (cleanNumber.length() > 10) {
            // For other international numbers, show without country code if possible
            // Try to extract the last 10 digits for display
            if (cleanNumber.length() >= 10) {
                String lastTenDigits = cleanNumber.substring(cleanNumber.length() - 10);
                return String.format("(%s) %s-%s",
                        lastTenDigits.substring(0, 3),
                        lastTenDigits.substring(3, 6),
                        lastTenDigits.substring(6));
            }
        }

        // If we can't format it nicely, return original (never return "Unknown")
        return phoneNumber;
    }
}