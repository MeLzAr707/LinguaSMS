package com.translator.messagingapp;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.net.Uri;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * Test class for the MMS display text logic fix.
 * Tests that MMS messages show proper display text instead of "[Empty Message]".
 */
@RunWith(MockitoJUnitRunner.class) 
public class MmsDisplayTextFixTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private Uri mockUri;

    private MessageRecyclerAdapter adapter;
    private List<Message> messages;
    private MessageRecyclerAdapter.OnMessageClickListener mockListener;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        messages = new ArrayList<>();
        mockListener = mock(MessageRecyclerAdapter.OnMessageClickListener.class);
        adapter = new MessageRecyclerAdapter(mockContext, messages, mockListener);
    }

    @Test
    public void testMmsWithOnlyAttachmentsShowsMediaMessage() {
        // Given: MMS message with no text but with attachments
        MmsMessage mmsMessage = new MmsMessage();
        mmsMessage.setBody(null);
        
        MmsMessage.Attachment attachment = new MmsMessage.Attachment();
        attachment.setUri(mockUri);
        attachment.setContentType("image/jpeg");
        mmsMessage.addAttachment(attachment);
        
        // When: Getting display text using reflection (since method is private)
        String displayText = getDisplayTextForMessage(mmsMessage);
        
        // Then: Should show media message instead of empty message
        assertEquals("Should show '[Media Message]' for MMS with only attachments", 
                     "[Media Message]", displayText);
    }

    @Test
    public void testMmsWithTextAndAttachmentsShowsTextWithIcon() {
        // Given: MMS message with both text and attachments
        MmsMessage mmsMessage = new MmsMessage();
        mmsMessage.setBody("Check out this photo!");
        
        MmsMessage.Attachment attachment = new MmsMessage.Attachment();
        attachment.setUri(mockUri);
        attachment.setContentType("image/jpeg");
        mmsMessage.addAttachment(attachment);
        
        // When: Getting display text
        String displayText = getDisplayTextForMessage(mmsMessage);
        
        // Then: Should show text with attachment icon
        assertEquals("Should show text with attachment icon", 
                     "Check out this photo! 📎", displayText);
    }

    @Test
    public void testMmsWithOnlyTextShowsText() {
        // Given: MMS message with only text
        MmsMessage mmsMessage = new MmsMessage();
        mmsMessage.setBody("This is a text-only MMS");
        
        // When: Getting display text
        String displayText = getDisplayTextForMessage(mmsMessage);
        
        // Then: Should show the text content
        assertEquals("Should show text content for text-only MMS", 
                     "This is a text-only MMS", displayText);
    }

    @Test
    public void testMmsWithNoContentShowsMmsMessage() {
        // Given: MMS message with no text and no attachments
        MmsMessage mmsMessage = new MmsMessage();
        mmsMessage.setBody(null);
        
        // When: Getting display text
        String displayText = getDisplayTextForMessage(mmsMessage);
        
        // Then: Should show generic MMS message instead of empty
        assertEquals("Should show '[MMS Message]' for MMS with no content", 
                     "[MMS Message]", displayText);
    }

    @Test
    public void testSmsWithNoContentShowsEmptyMessage() {
        // Given: Regular SMS message with no text
        Message smsMessage = new Message();
        smsMessage.setBody(null);
        smsMessage.setMessageType(Message.MESSAGE_TYPE_SMS);
        
        // When: Getting display text
        String displayText = getDisplayTextForMessage(smsMessage);
        
        // Then: Should still show empty message for SMS
        assertEquals("Should show '[Empty Message]' for SMS with no content", 
                     "[Empty Message]", displayText);
    }

    @Test
    public void testRcsWithNoContentShowsRcsMessage() {
        // Given: RCS message with no text
        RcsMessage rcsMessage = new RcsMessage();
        rcsMessage.setBody(null);
        
        // When: Getting display text
        String displayText = getDisplayTextForMessage(rcsMessage);
        
        // Then: Should show RCS message placeholder
        assertEquals("Should show RCS placeholder for RCS with no content", 
                     "[RCS Message - Content not available]", displayText);
    }

    /**
     * Helper method to test the private getDisplayTextForMessage method using reflection.
     * In a real implementation, you might want to make this method package-private for testing.
     */
    private String getDisplayTextForMessage(Message message) {
        try {
            java.lang.reflect.Method method = MessageRecyclerAdapter.class.getDeclaredMethod("getDisplayTextForMessage", Message.class);
            method.setAccessible(true);
            return (String) method.invoke(adapter, message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke getDisplayTextForMessage", e);
        }
    }
}