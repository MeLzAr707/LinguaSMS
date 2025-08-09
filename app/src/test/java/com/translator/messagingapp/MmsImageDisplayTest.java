package com.translator.messagingapp;

import static org.junit.Assert.*;

import android.net.Uri;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Simple test class for verifying MMS image display functionality.
 * Tests the core logic without UI components.
 */
public class MmsImageDisplayTest {

    private MmsMessage testMmsMessage;

    @Before
    public void setUp() {
        // Create test MMS message with image attachment
        testMmsMessage = new MmsMessage("test-id", "Test message with image", System.currentTimeMillis(), Message.TYPE_INBOX);
        
        // Create image attachment
        Uri testUri = Uri.parse("content://media/test/image.jpg");
        MmsMessage.Attachment imageAttachment = new MmsMessage.Attachment();
        imageAttachment.setUri(testUri);
        imageAttachment.setContentType("image/jpeg");
        imageAttachment.setFileName("test_image.jpg");
        imageAttachment.setSize(1024);
        
        testMmsMessage.addAttachment(imageAttachment);
    }

    @Test
    public void testMmsMessageHasAttachments() {
        assertTrue("MMS message should have attachments", testMmsMessage.hasAttachments());
        assertEquals("Should have one attachment", 1, testMmsMessage.getAttachmentObjects().size());
    }

    @Test
    public void testImageAttachmentIsIdentifiedCorrectly() {
        MmsMessage.Attachment attachment = testMmsMessage.getAttachmentObjects().get(0);
        assertTrue("Attachment should be identified as image", attachment.isImage());
        assertFalse("Attachment should not be identified as video", attachment.isVideo());
        assertFalse("Attachment should not be identified as audio", attachment.isAudio());
    }

    @Test
    public void testImageAttachmentProperties() {
        MmsMessage.Attachment attachment = testMmsMessage.getAttachmentObjects().get(0);
        assertEquals("Content type should be image/jpeg", "image/jpeg", attachment.getContentType());
        assertEquals("File name should match", "test_image.jpg", attachment.getFileName());
        assertEquals("Size should match", 1024, attachment.getSize());
        assertNotNull("URI should not be null", attachment.getUri());
    }

    /**
     * Test that MMS message returns correct attachment URIs.
     */
    @Test
    public void testMmsAttachmentUris() {
        List<Uri> attachmentUris = testMmsMessage.getAttachments();
        assertEquals("Should return one URI", 1, attachmentUris.size());
        assertEquals("URI should match", "content://media/test/image.jpg", attachmentUris.get(0).toString());
    }

    /**
     * Test that MMS message is correctly identified as MMS.
     */
    @Test
    public void testMmsMessageIdentification() {
        assertTrue("Message should be identified as MMS", testMmsMessage.isMms());
        assertEquals("Message type should be MMS", Message.MESSAGE_TYPE_MMS, testMmsMessage.getMessageType());
    }

    /**
     * Test attachment content type detection methods.
     */
    @Test
    public void testAttachmentContentTypeDetection() {
        // Test image attachment
        MmsMessage.Attachment imageAttachment = new MmsMessage.Attachment();
        imageAttachment.setContentType("image/jpeg");
        assertTrue("Should detect image content type", imageAttachment.isImage());
        assertFalse("Should not detect as video", imageAttachment.isVideo());
        assertFalse("Should not detect as audio", imageAttachment.isAudio());

        // Test video attachment
        MmsMessage.Attachment videoAttachment = new MmsMessage.Attachment();
        videoAttachment.setContentType("video/mp4");
        assertFalse("Should not detect as image", videoAttachment.isImage());
        assertTrue("Should detect video content type", videoAttachment.isVideo());
        assertFalse("Should not detect as audio", videoAttachment.isAudio());

        // Test audio attachment
        MmsMessage.Attachment audioAttachment = new MmsMessage.Attachment();
        audioAttachment.setContentType("audio/mp3");
        assertFalse("Should not detect as image", audioAttachment.isImage());
        assertFalse("Should not detect as video", audioAttachment.isVideo());
        assertTrue("Should detect audio content type", audioAttachment.isAudio());
    }

    /**
     * Test multiple attachments handling
     */
    @Test
    public void testMultipleAttachments() {
        // Add a second attachment
        MmsMessage.Attachment videoAttachment = new MmsMessage.Attachment();
        videoAttachment.setUri(Uri.parse("content://media/test/video.mp4"));
        videoAttachment.setContentType("video/mp4");
        videoAttachment.setFileName("test_video.mp4");
        videoAttachment.setSize(2048);
        
        testMmsMessage.addAttachment(videoAttachment);
        
        assertEquals("Should have two attachments", 2, testMmsMessage.getAttachmentObjects().size());
        assertEquals("Should return two URIs", 2, testMmsMessage.getAttachments().size());
        
        List<MmsMessage.Attachment> attachments = testMmsMessage.getAttachmentObjects();
        assertTrue("First attachment should be image", attachments.get(0).isImage());
        assertTrue("Second attachment should be video", attachments.get(1).isVideo());
    }
}