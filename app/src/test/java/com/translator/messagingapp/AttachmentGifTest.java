package com.translator.messagingapp;

import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

/**
 * Test for the isGif() method in Attachment class
 */
@RunWith(MockitoJUnitRunner.class)
public class AttachmentGifTest {

    @Mock
    private Uri mockUri;

    @Test
    public void testIsGif_WithGifContentType_ReturnsTrue() {
        // Given
        Attachment attachment = new Attachment(mockUri, "image/gif", "test.gif", 1000);
        
        // When
        boolean result = attachment.isGif();
        
        // Then
        assertTrue("Should return true for image/gif content type", result);
    }
    
    @Test
    public void testIsGif_WithJpegContentType_ReturnsFalse() {
        // Given
        Attachment attachment = new Attachment(mockUri, "image/jpeg", "test.jpg", 1000);
        
        // When
        boolean result = attachment.isGif();
        
        // Then
        assertFalse("Should return false for image/jpeg content type", result);
    }
    
    @Test
    public void testIsGif_WithPngContentType_ReturnsFalse() {
        // Given
        Attachment attachment = new Attachment(mockUri, "image/png", "test.png", 1000);
        
        // When
        boolean result = attachment.isGif();
        
        // Then
        assertFalse("Should return false for image/png content type", result);
    }
    
    @Test
    public void testIsGif_WithVideoContentType_ReturnsFalse() {
        // Given
        Attachment attachment = new Attachment(mockUri, "video/mp4", "test.mp4", 1000);
        
        // When
        boolean result = attachment.isGif();
        
        // Then
        assertFalse("Should return false for video content type", result);
    }
    
    @Test
    public void testIsGif_WithNullContentType_ReturnsFalse() {
        // Given
        Attachment attachment = new Attachment(mockUri, null, "test.gif", 1000);
        
        // When
        boolean result = attachment.isGif();
        
        // Then
        assertFalse("Should return false for null content type", result);
    }
    
    @Test
    public void testIsGif_WithEmptyContentType_ReturnsFalse() {
        // Given
        Attachment attachment = new Attachment(mockUri, "", "test.gif", 1000);
        
        // When
        boolean result = attachment.isGif();
        
        // Then
        assertFalse("Should return false for empty content type", result);
    }
}