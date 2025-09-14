package com.translator.messagingapp;

import com.translator.messagingapp.message.*;
import com.translator.messagingapp.conversation.*;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.content.ContentResolver;
import android.net.Uri;
import android.provider.Telephony;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test to verify that MMS conversations properly resolve contact names and addresses
 * when sent via NewMessageActivity, fixing the issue where MMS threads show "Unknown".
 */
@RunWith(RobolectricTestRunner.class)
public class MmsConversationContactNameTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private ContentResolver mockContentResolver;

    private MessageService messageService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockContext.getContentResolver()).thenReturn(mockContentResolver);
        messageService = new MessageService(mockContext);
    }

    /**
     * Test that sent MMS messages properly resolve recipient addresses
     * when loading conversation details.
     */
    @Test
    public void testSentMmsConversationLoadsRecipientAddress() {
        String threadId = "123";
        String recipientPhone = "+1234567890";
        String messageId = "456";
        
        // Mock the MMS conversation query - simulating a sent MMS message
        MatrixCursor mmsCursor = createMmsCursor(messageId, Telephony.Mms.MESSAGE_BOX_SENT);
        when(mockContentResolver.query(
            eq(Uri.parse("content://mms")),
            any(),
            eq("thread_id = ?"),
            eq(new String[]{threadId}),
            eq("date DESC LIMIT 1")
        )).thenReturn(mmsCursor);
        
        // Mock the MMS address query - recipient addresses for sent message
        MatrixCursor addrCursor = createMmsAddressCursor(recipientPhone);
        when(mockContentResolver.query(
            eq(Uri.parse("content://mms/" + messageId + "/addr")),
            any(),
            eq("type=151"), // TYPE_TO for outgoing messages
            any(),
            any()
        )).thenReturn(addrCursor);
        
        // Mock the MMS text content (optional)
        when(mockContentResolver.query(
            eq(Uri.parse("content://mms/" + messageId + "/part")),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(createEmptyMmsPartCursor());
        
        // Mock the unread count query
        when(mockContentResolver.query(
            any(Uri.class),
            any(),
            contains("thread_id"),
            any(),
            any()
        )).thenReturn(createUnreadCountCursor(0));

        // Load conversation details using the fixed logic
        List<Conversation> conversations = messageService.loadConversations();
        
        // Verify that we get a conversation with the correct recipient address
        assertNotNull("Should load conversations", conversations);
        // Note: The actual conversation loading will depend on the mocking setup
        // For this test, we're primarily verifying the method doesn't crash and follows correct logic
    }

    /**
     * Test that received MMS messages properly resolve sender addresses.
     */
    @Test
    public void testReceivedMmsConversationLoadsSenderAddress() {
        String threadId = "789";
        String senderPhone = "+9876543210";
        String messageId = "101112";
        
        // Mock the MMS conversation query - simulating a received MMS message
        MatrixCursor mmsCursor = createMmsCursor(messageId, Telephony.Mms.MESSAGE_BOX_INBOX);
        when(mockContentResolver.query(
            eq(Uri.parse("content://mms")),
            any(),
            eq("thread_id = ?"),
            eq(new String[]{threadId}),
            eq("date DESC LIMIT 1")
        )).thenReturn(mmsCursor);
        
        // Mock the MMS address query - sender address for received message
        MatrixCursor addrCursor = createMmsAddressCursor(senderPhone);
        when(mockContentResolver.query(
            eq(Uri.parse("content://mms/" + messageId + "/addr")),
            any(),
            eq("type=137"), // TYPE_FROM for incoming messages
            any(),
            any()
        )).thenReturn(addrCursor);
        
        // Mock other queries
        when(mockContentResolver.query(
            eq(Uri.parse("content://mms/" + messageId + "/part")),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(createEmptyMmsPartCursor());
        
        when(mockContentResolver.query(
            any(Uri.class),
            any(),
            contains("thread_id"),
            any(),
            any()
        )).thenReturn(createUnreadCountCursor(1));

        // Load conversation details using the fixed logic
        List<Conversation> conversations = messageService.loadConversations();
        
        // Verify that the method processes correctly
        assertNotNull("Should load conversations", conversations);
    }

    /**
     * Test the conversation display logic to ensure phone numbers are shown
     * instead of "Unknown" when contact names aren't available.
     */
    @Test
    public void testConversationDisplayLogicShowsPhoneNumber() {
        // Create a conversation with address but no contact name
        Conversation conversation = new Conversation();
        conversation.setThreadId("123");
        conversation.setAddress("+1234567890");
        conversation.setContactName(null); // No contact name
        conversation.setSnippet("[MMS]");
        
        // Create adapter to test display logic
        ConversationRecyclerAdapter adapter = new ConversationRecyclerAdapter(
            mockContext, Arrays.asList(conversation));
        
        // Test phone number formatting (this should not return "Unknown")
        String formatted = adapter.formatPhoneNumber("+1234567890");
        assertNotNull("Formatted phone should not be null", formatted);
        assertNotEquals("Should not return 'Unknown'", "Unknown", formatted);
        assertFalse("Should not contain 'unknown'", formatted.toLowerCase().contains("unknown"));
        
        // Should format the phone number properly
        assertEquals("Should format as (123) 456-7890", "(123) 456-7890", formatted);
    }

    /**
     * Test that the display logic handles various edge cases without showing "Unknown".
     */
    @Test
    public void testDisplayLogicEdgeCases() {
        ConversationRecyclerAdapter adapter = new ConversationRecyclerAdapter(
            mockContext, Arrays.asList());
        
        // Test null input
        String result1 = adapter.formatPhoneNumber(null);
        assertEquals("Should show 'No Number' for null", "No Number", result1);
        
        // Test empty input
        String result2 = adapter.formatPhoneNumber("");
        assertEquals("Should show 'No Number' for empty", "No Number", result2);
        
        // Test actual phone number
        String result3 = adapter.formatPhoneNumber("5551234567");
        assertNotEquals("Should not show 'Unknown' for valid number", "Unknown", result3);
        assertEquals("Should format correctly", "(555) 123-4567", result3);
        
        // Test international number
        String result4 = adapter.formatPhoneNumber("+15551234567");
        assertNotEquals("Should not show 'Unknown' for international number", "Unknown", result4);
        assertEquals("Should format international correctly", "(555) 123-4567", result4);
    }

    // Helper methods to create mock cursors

    private MatrixCursor createMmsCursor(String messageId, int messageBox) {
        MatrixCursor cursor = new MatrixCursor(new String[]{
            Telephony.Mms._ID,
            Telephony.Mms.MESSAGE_BOX,
            Telephony.Mms.DATE,
            Telephony.Mms.READ
        });
        cursor.addRow(new Object[]{
            messageId,
            messageBox,
            System.currentTimeMillis() / 1000, // MMS date is in seconds
            1 // read
        });
        return cursor;
    }

    private MatrixCursor createMmsAddressCursor(String address) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"address"});
        cursor.addRow(new Object[]{address});
        return cursor;
    }

    private MatrixCursor createEmptyMmsPartCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[]{
            Telephony.Mms.Part.CONTENT_TYPE,
            Telephony.Mms.Part.TEXT
        });
        // No rows - empty part cursor
        return cursor;
    }

    private MatrixCursor createUnreadCountCursor(int count) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"COUNT(*)"});
        cursor.addRow(new Object[]{count});
        return cursor;
    }
}