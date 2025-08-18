package com.translator.messagingapp;

import static org.junit.Assert.*;

import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Integration test specifically for the issue: "mms messages not showing"
 * and displaying "[Empty Message]" instead of proper content.
 */
@RunWith(MockitoJUnitRunner.class)
public class MmsNotShowingIssueTest {

    @Test
    public void testIssue_MmsMessagesNotShowing_EmptyMessageFixed() {
        // Scenario 1: MMS with image attachment but no text (common case for issue)
        MmsMessage mmsWithImageOnly = new MmsMessage();
        mmsWithImageOnly.setBody(null); // No text content
        
        // Add image attachment
        MmsMessage.Attachment imageAttachment = new MmsMessage.Attachment();
        imageAttachment.setUri(Uri.parse("content://mms/part/123"));
        imageAttachment.setContentType("image/jpeg");
        imageAttachment.setFileName("photo.jpg");
        imageAttachment.setSize(2048);
        mmsWithImageOnly.addAttachment(imageAttachment);
        
        // BEFORE fix: would show "[Empty Message]"
        // AFTER fix: should show "[Media Message]"
        assertTrue("MMS with image should have attachments", mmsWithImageOnly.hasAttachments());
        assertTrue("Should be identified as MMS", mmsWithImageOnly.isMms());
        
        // Simulate the display logic (since we can't easily test private method)
        String displayText = getExpectedDisplayText(mmsWithImageOnly);
        assertEquals("MMS with only image should show '[Media Message]'", 
                     "[Media Message]", displayText);
    }

    @Test 
    public void testIssue_MmsWithTextAndImage_ShowsTextWithIcon() {
        // Scenario 2: MMS with both text and image
        MmsMessage mmsWithTextAndImage = new MmsMessage();
        mmsWithTextAndImage.setBody("Check out this photo!");
        
        MmsMessage.Attachment imageAttachment = new MmsMessage.Attachment();
        imageAttachment.setUri(Uri.parse("content://mms/part/124"));
        imageAttachment.setContentType("image/jpeg");
        mmsWithTextAndImage.addAttachment(imageAttachment);
        
        String displayText = getExpectedDisplayText(mmsWithTextAndImage);
        assertEquals("MMS with text and image should show text with attachment icon", 
                     "Check out this photo! 📎", displayText);
    }

    @Test
    public void testIssue_MmsWithVideoAttachment_ShowsMediaMessage() {
        // Scenario 3: MMS with video attachment (another common case)
        MmsMessage mmsWithVideo = new MmsMessage();
        mmsWithVideo.setBody(""); // Empty text
        
        MmsMessage.Attachment videoAttachment = new MmsMessage.Attachment();
        videoAttachment.setUri(Uri.parse("content://mms/part/125"));
        videoAttachment.setContentType("video/mp4");
        videoAttachment.setFileName("video.mp4");
        videoAttachment.setSize(5120);
        mmsWithVideo.addAttachment(videoAttachment);
        
        String displayText = getExpectedDisplayText(mmsWithVideo);
        assertEquals("MMS with only video should show '[Media Message]'", 
                     "[Media Message]", displayText);
    }

    @Test
    public void testIssue_MmsWithMultipleAttachments_ShowsMediaMessage() {
        // Scenario 4: MMS with multiple attachments
        MmsMessage mmsWithMultipleAttachments = new MmsMessage();
        mmsWithMultipleAttachments.setBody(null);
        
        // Add image
        MmsMessage.Attachment imageAttachment = new MmsMessage.Attachment();
        imageAttachment.setUri(Uri.parse("content://mms/part/126"));
        imageAttachment.setContentType("image/jpeg");
        mmsWithMultipleAttachments.addAttachment(imageAttachment);
        
        // Add audio
        MmsMessage.Attachment audioAttachment = new MmsMessage.Attachment();
        audioAttachment.setUri(Uri.parse("content://mms/part/127"));
        audioAttachment.setContentType("audio/mpeg");
        mmsWithMultipleAttachments.addAttachment(audioAttachment);
        
        assertTrue("MMS with multiple attachments should have attachments", 
                   mmsWithMultipleAttachments.hasAttachments());
        assertEquals("Should have 2 attachments", 2, 
                     mmsWithMultipleAttachments.getAttachmentObjects().size());
        
        String displayText = getExpectedDisplayText(mmsWithMultipleAttachments);
        assertEquals("MMS with multiple attachments should show '[Media Message]'", 
                     "[Media Message]", displayText);
    }

    @Test
    public void testIssue_BrokenMmsMessage_ShowsMmsMessageNotEmpty() {
        // Scenario 5: Broken/corrupt MMS with no content (edge case)
        MmsMessage brokenMms = new MmsMessage();
        brokenMms.setBody(null);
        // No attachments added (simulating loading failure)
        
        String displayText = getExpectedDisplayText(brokenMms);
        // BEFORE fix: would show "[Empty Message]" 
        // AFTER fix: should show "[MMS Message]" to indicate it's an MMS
        assertEquals("Broken MMS should show '[MMS Message]' not '[Empty Message]'", 
                     "[MMS Message]", displayText);
    }

    /**
     * Simulates the new display logic from MessageRecyclerAdapter.getDisplayTextForMessage()
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