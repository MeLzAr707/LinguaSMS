package com.translator.messagingapp.mms;

import android.content.ContentValues;
import android.provider.Telephony;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

/**
 * Test class for MmsPart functionality.
 * Tests the integration improvements from Simple-SMS-Messenger.
 */
@RunWith(MockitoJUnitRunner.class)
public class MmsPartTest {

    private MmsPart mmsPart;

    @Before
    public void setUp() {
        mmsPart = new MmsPart();
    }

    @Test
    public void testDefaultConstructor() {
        MmsPart part = new MmsPart();
        assertNotNull("MmsPart should be created", part);
        assertEquals("Default sequence order should be 0", 0, part.getSequenceOrder());
    }

    @Test
    public void testConstructorWithParameters() {
        MmsPart part = new MmsPart("image/jpeg", 1);
        assertEquals("Content type should be set", "image/jpeg", part.getContentType());
        assertEquals("Sequence order should be set", 1, part.getSequenceOrder());
    }

    @Test
    public void testToContentValues() {
        mmsPart.setContentType("text/plain");
        mmsPart.setSequenceOrder(1);
        mmsPart.setText("Test message");
        mmsPart.setFilename("test.txt");

        ContentValues values = mmsPart.toContentValues();
        
        assertNotNull("ContentValues should not be null", values);
        assertEquals("Content type should be preserved", "text/plain", 
                    values.getAsString(Telephony.Mms.Part.CONTENT_TYPE));
        assertEquals("Sequence order should be preserved", Integer.valueOf(1), 
                    values.getAsInteger(Telephony.Mms.Part.SEQ));
        assertEquals("Text should be preserved", "Test message", 
                    values.getAsString(Telephony.Mms.Part.TEXT));
        assertEquals("Filename should be preserved", "test.txt", 
                    values.getAsString(Telephony.Mms.Part.FILENAME));
    }

    @Test
    public void testIsNonText_TextPart() {
        mmsPart.setText("Some text content");
        assertFalse("Part with text should not be non-text", mmsPart.isNonText());
    }

    @Test
    public void testIsNonText_TextContentType() {
        mmsPart.setContentType("text/plain");
        assertFalse("Text content type should not be non-text", mmsPart.isNonText());
    }

    @Test
    public void testIsNonText_SmilContentType() {
        mmsPart.setContentType("application/smil");
        assertFalse("SMIL content type should not be non-text", mmsPart.isNonText());
    }

    @Test
    public void testIsNonText_ImageContentType() {
        mmsPart.setContentType("image/jpeg");
        assertTrue("Image content type should be non-text", mmsPart.isNonText());
    }

    @Test
    public void testIsImage() {
        mmsPart.setContentType("image/jpeg");
        assertTrue("JPEG should be identified as image", mmsPart.isImage());
        
        mmsPart.setContentType("image/png");
        assertTrue("PNG should be identified as image", mmsPart.isImage());
        
        mmsPart.setContentType("video/mp4");
        assertFalse("Video should not be identified as image", mmsPart.isImage());
        
        mmsPart.setContentType(null);
        assertFalse("Null content type should not be identified as image", mmsPart.isImage());
    }

    @Test
    public void testIsVideo() {
        mmsPart.setContentType("video/mp4");
        assertTrue("MP4 should be identified as video", mmsPart.isVideo());
        
        mmsPart.setContentType("video/avi");
        assertTrue("AVI should be identified as video", mmsPart.isVideo());
        
        mmsPart.setContentType("image/jpeg");
        assertFalse("Image should not be identified as video", mmsPart.isVideo());
    }

    @Test
    public void testIsAudio() {
        mmsPart.setContentType("audio/mp3");
        assertTrue("MP3 should be identified as audio", mmsPart.isAudio());
        
        mmsPart.setContentType("audio/wav");
        assertTrue("WAV should be identified as audio", mmsPart.isAudio());
        
        mmsPart.setContentType("image/jpeg");
        assertFalse("Image should not be identified as audio", mmsPart.isAudio());
    }

    @Test
    public void testGettersAndSetters() {
        // Test all getters and setters
        mmsPart.setContentDisposition("attachment");
        assertEquals("attachment", mmsPart.getContentDisposition());
        
        mmsPart.setCharset("UTF-8");
        assertEquals("UTF-8", mmsPart.getCharset());
        
        mmsPart.setContentId("test-id");
        assertEquals("test-id", mmsPart.getContentId());
        
        mmsPart.setContentLocation("http://example.com/file");
        assertEquals("http://example.com/file", mmsPart.getContentLocation());
        
        mmsPart.setCtStart("test-start");
        assertEquals("test-start", mmsPart.getCtStart());
        
        mmsPart.setCtType("test-type");
        assertEquals("test-type", mmsPart.getCtType());
        
        mmsPart.setName("test-name");
        assertEquals("test-name", mmsPart.getName());
        
        mmsPart.setData("test-data");
        assertEquals("test-data", mmsPart.getData());
    }

    @Test
    public void testToString() {
        mmsPart.setContentType("image/jpeg");
        mmsPart.setFilename("test.jpg");
        mmsPart.setSequenceOrder(1);
        
        String result = mmsPart.toString();
        assertNotNull("toString should not return null", result);
        assertTrue("toString should contain content type", result.contains("image/jpeg"));
        assertTrue("toString should contain filename", result.contains("test.jpg"));
        assertTrue("toString should contain sequence order", result.contains("1"));
    }
}