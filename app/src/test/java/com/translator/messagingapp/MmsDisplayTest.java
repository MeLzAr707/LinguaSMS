package com.translator.messagingapp;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
 * Test class for MMS display functionality.
 * Tests the fixes for MMS photo display and video thumbnail generation.
 */
@RunWith(MockitoJUnitRunner.class)
public class MmsDisplayTest {

    @Mock
    private Uri mockUri;

    private MmsMessage mmsMessage;
    private MmsMessage.Attachment imageAttachment;
    private MmsMessage.Attachment videoAttachment;
    private MmsMessage.Attachment audioAttachment;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create test MMS message
        mmsMessage = new MmsMessage();
        mmsMessage.setId(1);
        mmsMessage.setBody("Test MMS message");
        mmsMessage.setDate(System.currentTimeMillis());
        mmsMessage.setType(Message.TYPE_INBOX);

        // Create test attachments
        imageAttachment = new MmsMessage.Attachment();
        imageAttachment.setUri(mockUri);
        imageAttachment.setContentType("image/jpeg");
        imageAttachment.setFileName("test_image.jpg");
        imageAttachment.setSize(1024);

        videoAttachment = new MmsMessage.Attachment();
        videoAttachment.setUri(mockUri);
        videoAttachment.setContentType("video/mp4");
        videoAttachment.setFileName("test_video.mp4");
        videoAttachment.setSize(2048);

        audioAttachment = new MmsMessage.Attachment();
        audioAttachment.setUri(mockUri);
        audioAttachment.setContentType("audio/mpeg");
        audioAttachment.setFileName("test_audio.mp3");
        audioAttachment.setSize(512);
    }

    @Test
    public void testMmsMessageHasCorrectType() {
        assertTrue("MMS message should be identified as MMS", mmsMessage.isMms());
        assertEquals("Message type should be MMS", Message.MESSAGE_TYPE_MMS, mmsMessage.getMessageType());
    }

    @Test
    public void testImageAttachmentIsIdentifiedCorrectly() {
        assertTrue("Image attachment should be identified as image", imageAttachment.isImage());
        assertFalse("Image attachment should not be identified as video", imageAttachment.isVideo());
        assertFalse("Image attachment should not be identified as audio", imageAttachment.isAudio());
    }

    @Test
    public void testVideoAttachmentIsIdentifiedCorrectly() {
        assertFalse("Video attachment should not be identified as image", videoAttachment.isImage());
        assertTrue("Video attachment should be identified as video", videoAttachment.isVideo());
        assertFalse("Video attachment should not be identified as audio", videoAttachment.isAudio());
    }

    @Test
    public void testAudioAttachmentIsIdentifiedCorrectly() {
        assertFalse("Audio attachment should not be identified as image", audioAttachment.isImage());
        assertFalse("Audio attachment should not be identified as video", audioAttachment.isVideo());
        assertTrue("Audio attachment should be identified as audio", audioAttachment.isAudio());
    }

    @Test
    public void testMmsMessageWithAttachmentsHasAttachments() {
        // Initially no attachments
        assertFalse("New MMS message should not have attachments", mmsMessage.hasAttachments());
        
        // Add an attachment
        mmsMessage.addAttachment(imageAttachment);
        assertTrue("MMS message with attachment should have attachments", mmsMessage.hasAttachments());
        
        // Test attachment retrieval
        List<MmsMessage.Attachment> attachments = mmsMessage.getAttachmentObjects();
        assertNotNull("Attachment list should not be null", attachments);
        assertEquals("Should have exactly one attachment", 1, attachments.size());
        assertEquals("Should return the correct attachment", imageAttachment, attachments.get(0));
    }

    @Test
    public void testMmsMessageWithMultipleAttachments() {
        // Add multiple attachments
        mmsMessage.addAttachment(imageAttachment);
        mmsMessage.addAttachment(videoAttachment);
        mmsMessage.addAttachment(audioAttachment);
        
        assertTrue("MMS message with multiple attachments should have attachments", mmsMessage.hasAttachments());
        
        List<MmsMessage.Attachment> attachments = mmsMessage.getAttachmentObjects();
        assertEquals("Should have three attachments", 3, attachments.size());
        
        // Test URI list
        List<Uri> uris = mmsMessage.getAttachments();
        assertEquals("Should have three URIs", 3, uris.size());
    }

    @Test
    public void testAttachmentContentTypes() {
        assertEquals("Image content type should match", "image/jpeg", imageAttachment.getContentType());
        assertEquals("Video content type should match", "video/mp4", videoAttachment.getContentType());
        assertEquals("Audio content type should match", "audio/mpeg", audioAttachment.getContentType());
    }

    @Test
    public void testAttachmentURIs() {
        assertEquals("Image URI should match", mockUri, imageAttachment.getUri());
        assertEquals("Video URI should match", mockUri, videoAttachment.getUri());
        assertEquals("Audio URI should match", mockUri, audioAttachment.getUri());
    }

    @Test
    public void testNullAttachmentHandling() {
        // Adding null attachment should be handled gracefully
        mmsMessage.addAttachment(null);
        assertFalse("MMS message should not have attachments after adding null", mmsMessage.hasAttachments());
        
        List<MmsMessage.Attachment> attachments = mmsMessage.getAttachmentObjects();
        assertTrue("Attachment list should be empty", attachments.isEmpty());
    }

    @Test
    public void testAttachmentWithNullUri() {
        MmsMessage.Attachment nullUriAttachment = new MmsMessage.Attachment();
        nullUriAttachment.setContentType("image/jpeg");
        nullUriAttachment.setUri(null);
        
        mmsMessage.addAttachment(nullUriAttachment);
        
        // Should have attachment object but URI list should be empty
        assertTrue("Should have attachment object", mmsMessage.hasAttachments());
        List<Uri> uris = mmsMessage.getAttachments();
        assertTrue("URI list should be empty when attachment has null URI", uris.isEmpty());
    }
}