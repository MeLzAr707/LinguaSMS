package com.translator.messagingapp;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.ContentResolver;
import android.database.Cursor;
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
 * Test class for the MMS empty message fix.
 * Tests that MMS messages with attachments are properly loaded and displayed.
 */
@RunWith(MockitoJUnitRunner.class)
public class MmsEmptyMessageFixTest {

    @Mock
    private ContentResolver mockContentResolver;
    
    @Mock
    private Cursor mockCursor;
    
    @Mock
    private Uri mockUri;

    private MmsMessage mmsMessage;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create test MMS message
        mmsMessage = new MmsMessage();
        mmsMessage.setId(1);
        mmsMessage.setBody(""); // Empty body to simulate the problem
        mmsMessage.setDate(System.currentTimeMillis());
        mmsMessage.setType(Message.TYPE_INBOX);
    }

    @Test
    public void testMmsMessageWithAttachmentsDoesNotShowEmptyMessage() {
        // Given: An MMS message with no text but with attachments
        mmsMessage.setBody(null);
        
        // Add a mock attachment
        MmsMessage.Attachment attachment = new MmsMessage.Attachment();
        attachment.setUri(mockUri);
        attachment.setContentType("image/jpeg");
        attachment.setFileName("test.jpg");
        attachment.setSize(1024);
        
        mmsMessage.addAttachment(attachment);
        
        // When: Checking if message has attachments
        boolean hasAttachments = mmsMessage.hasAttachments();
        
        // Then: Should have attachments and not show as empty
        assertTrue("MMS message should have attachments", hasAttachments);
        assertFalse("MMS message body should be empty", mmsMessage.getBody() != null && !mmsMessage.getBody().trim().isEmpty());
        assertTrue("MMS message should be identified as MMS", mmsMessage.isMms());
    }

    @Test
    public void testMmsMessageWithTextAndAttachments() {
        // Given: An MMS message with both text and attachments
        mmsMessage.setBody("Check out this photo!");
        
        MmsMessage.Attachment attachment = new MmsMessage.Attachment();
        attachment.setUri(mockUri);
        attachment.setContentType("image/jpeg");
        mmsMessage.addAttachment(attachment);
        
        // When: Checking message properties
        boolean hasAttachments = mmsMessage.hasAttachments();
        String body = mmsMessage.getBody();
        
        // Then: Should have both text and attachments
        assertTrue("MMS message should have attachments", hasAttachments);
        assertNotNull("MMS message should have text body", body);
        assertFalse("MMS message body should not be empty", body.trim().isEmpty());
    }

    @Test
    public void testMmsMessageWithOnlyText() {
        // Given: An MMS message with only text, no attachments
        mmsMessage.setBody("This is a text-only MMS");
        
        // When: Checking message properties  
        boolean hasAttachments = mmsMessage.hasAttachments();
        String body = mmsMessage.getBody();
        
        // Then: Should have text but no attachments
        assertFalse("MMS message should not have attachments", hasAttachments);
        assertNotNull("MMS message should have text body", body);
        assertFalse("MMS message body should not be empty", body.trim().isEmpty());
    }

    @Test
    public void testAttachmentCreationWithNullValues() {
        // Test that attachments handle null values gracefully
        MmsMessage.Attachment attachment = new MmsMessage.Attachment();
        attachment.setUri(null);
        attachment.setContentType("image/jpeg");
        
        mmsMessage.addAttachment(attachment);
        
        // Should still be considered as having attachments (object exists)
        assertTrue("MMS message should have attachment object", mmsMessage.hasAttachments());
        
        // But URI list should be empty since URI is null
        List<Uri> uriList = mmsMessage.getAttachments();
        assertTrue("URI list should be empty when attachment has null URI", uriList.isEmpty());
    }

    @Test
    public void testAttachmentContentTypeDetection() {
        // Test different attachment types
        MmsMessage.Attachment imageAttachment = new MmsMessage.Attachment();
        imageAttachment.setContentType("image/jpeg");
        assertTrue("Should detect image content type", imageAttachment.isImage());
        assertFalse("Should not detect as video", imageAttachment.isVideo());
        assertFalse("Should not detect as audio", imageAttachment.isAudio());

        MmsMessage.Attachment videoAttachment = new MmsMessage.Attachment();
        videoAttachment.setContentType("video/mp4");
        assertFalse("Should not detect as image", videoAttachment.isImage());
        assertTrue("Should detect video content type", videoAttachment.isVideo());
        assertFalse("Should not detect as audio", videoAttachment.isAudio());

        MmsMessage.Attachment audioAttachment = new MmsMessage.Attachment();
        audioAttachment.setContentType("audio/mpeg");
        assertFalse("Should not detect as image", audioAttachment.isImage());
        assertFalse("Should not detect as video", audioAttachment.isVideo());
        assertTrue("Should detect audio content type", audioAttachment.isAudio());
    }

    @Test
    public void testMmsMessageTypeDetection() {
        // Test that MMS message is properly identified
        assertTrue("MMS message should be identified as MMS", mmsMessage.isMms());
        assertEquals("Message type should be MMS", Message.MESSAGE_TYPE_MMS, mmsMessage.getMessageType());
        assertFalse("MMS message should not be identified as RCS", mmsMessage.isRcs());
    }
}