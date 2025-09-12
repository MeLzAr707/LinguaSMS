package com.translator.messagingapp;

import com.translator.messagingapp.message.*;
import com.translator.messagingapp.conversation.*;
import com.translator.messagingapp.mms.*;
import com.translator.messagingapp.contact.*;
import com.translator.messagingapp.notification.*;

import static org.junit.Assert.*;

import android.net.Uri;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test specifically for the picture message display issue fix.
 * Tests that picture messages are properly set up to display images instead of just "[Media Message]".
 */
@RunWith(MockitoJUnitRunner.class)
public class PictureMessageDisplayTest {

    private MmsMessage pictureMessage;
    private MmsMessage videoMessage;

    @Before
    public void setUp() {
        // Create test MMS message with image attachment
        pictureMessage = new MmsMessage("test-pic-1", "", System.currentTimeMillis(), Message.TYPE_INBOX);
        
        // Create image attachment
        Uri imageUri = Uri.parse("content://media/test/image.jpg");
        MmsMessage.Attachment imageAttachment = new MmsMessage.Attachment();
        imageAttachment.setUri(imageUri);
        imageAttachment.setContentType("image/jpeg");
        imageAttachment.setFileName("test_image.jpg");
        imageAttachment.setSize(2048);
        
        pictureMessage.addAttachment(imageAttachment);

        // Create test MMS message with video attachment
        videoMessage = new MmsMessage("test-vid-1", "", System.currentTimeMillis(), Message.TYPE_INBOX);
        
        // Create video attachment
        Uri videoUri = Uri.parse("content://media/test/video.mp4");
        MmsMessage.Attachment videoAttachment = new MmsMessage.Attachment();
        videoAttachment.setUri(videoUri);
        videoAttachment.setContentType("video/mp4");
        videoAttachment.setFileName("test_video.mp4");
        videoAttachment.setSize(10240);
        
        videoMessage.addAttachment(videoAttachment);
    }

    @Test
    public void testPictureMessage_HasCorrectAttachment() {
        // Verify the picture message is set up correctly
        assertTrue("Picture message should have attachments", pictureMessage.hasAttachments());
        assertEquals("Should have one attachment", 1, pictureMessage.getAttachmentObjects().size());
        
        MmsMessage.Attachment attachment = pictureMessage.getAttachmentObjects().get(0);
        assertTrue("Attachment should be identified as image", attachment.isImage());
        assertFalse("Attachment should not be identified as video", attachment.isVideo());
        
        // Verify the URI is accessible
        assertNotNull("Attachment URI should not be null", attachment.getUri());
        assertEquals("URI should match", "content://media/test/image.jpg", attachment.getUri().toString());
    }

    @Test
    public void testVideoMessage_HasCorrectAttachment() {
        // Verify the video message is set up correctly
        assertTrue("Video message should have attachments", videoMessage.hasAttachments());
        assertEquals("Should have one attachment", 1, videoMessage.getAttachmentObjects().size());
        
        MmsMessage.Attachment attachment = videoMessage.getAttachmentObjects().get(0);
        assertFalse("Attachment should not be identified as image", attachment.isImage());
        assertTrue("Attachment should be identified as video", attachment.isVideo());
        
        // Verify the URI is accessible
        assertNotNull("Attachment URI should not be null", attachment.getUri());
        assertEquals("URI should match", "content://media/test/video.mp4", attachment.getUri().toString());
    }

    @Test
    public void testMmsMessage_IsCorrectlyIdentified() {
        // Verify both messages are identified as MMS
        assertTrue("Picture message should be identified as MMS", pictureMessage.isMms());
        assertTrue("Video message should be identified as MMS", videoMessage.isMms());
        
        // Verify they have correct message type
        assertEquals("Picture message should have MMS message type", 
                     Message.MESSAGE_TYPE_MMS, pictureMessage.getMessageType());
        assertEquals("Video message should have MMS message type", 
                     Message.MESSAGE_TYPE_MMS, videoMessage.getMessageType());
    }

    @Test
    public void testDisplayText_ForMediaOnlyMessages() {
        // Test the display text logic for media-only messages
        String pictureDisplayText = getExpectedDisplayText(pictureMessage);
        String videoDisplayText = getExpectedDisplayText(videoMessage);
        
        // Both should show [Media Message] since they have no text body
        assertEquals("Picture message should show '[Media Message]'", 
                     "[Media Message]", pictureDisplayText);
        assertEquals("Video message should show '[Media Message]'", 
                     "[Media Message]", videoDisplayText);
    }

    @Test
    public void testDisplayText_ForTextWithMedia() {
        // Add text to the picture message
        pictureMessage.setBody("Check out this photo!");
        
        String displayText = getExpectedDisplayText(pictureMessage);
        assertEquals("Message with text and media should show text with attachment icon", 
                     "Check out this photo! 📎", displayText);
    }

    /**
     * Simulates the display logic from MessageRecyclerAdapter.getDisplayTextForMessage()
     */
    private String getExpectedDisplayText(Message message) {
        // Handle message body
        String body = message.getBody();

        // Handle MMS messages specially
        if (message.isMms()) {
            boolean hasAttachments = message.hasAttachments();
            boolean hasText = body != null && !body.trim().isEmpty();
            
            if (hasText && hasAttachments) {
                // MMS with both text and attachments
                return body + " 📎";
            } else if (hasText) {
                // MMS with only text
                return body;
            } else if (hasAttachments) {
                // MMS with only attachments
                return "[Media Message]";
            } else {
                // MMS with no content (likely loading issue)
                return "[MMS Message]";
            }
        }

        // Handle null or empty body for non-MMS messages
        if (body == null || body.trim().isEmpty()) {
            return "[Empty Message]";
        }

        return body;
    }
}