package com.translator.messagingapp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import android.net.Uri;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test class for GIF animation functionality.
 * Tests that GIF attachments are properly detected and handled.
 */
@RunWith(MockitoJUnitRunner.class)
public class GifAnimationTest {

    @Mock
    private Uri mockUri;

    private Attachment gifAttachment;
    private Attachment jpegAttachment;
    private Attachment pngAttachment;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create test attachments with different content types
        gifAttachment = new Attachment(mockUri, "image/gif", "test.gif", 1024);
        jpegAttachment = new Attachment(mockUri, "image/jpeg", "test.jpg", 2048);
        pngAttachment = new Attachment(mockUri, "image/png", "test.png", 1536);
    }

    /**
     * Test that GIF attachments are correctly identified
     */
    @Test
    public void testIsGif_WithGifContentType_ReturnsTrue() {
        // When & Then
        assertTrue("GIF attachment should be identified as GIF", gifAttachment.isGif());
    }

    /**
     * Test that non-GIF image attachments are not identified as GIFs
     */
    @Test
    public void testIsGif_WithJpegContentType_ReturnsFalse() {
        // When & Then
        assertFalse("JPEG attachment should not be identified as GIF", jpegAttachment.isGif());
    }

    /**
     * Test that non-GIF image attachments are not identified as GIFs
     */
    @Test
    public void testIsGif_WithPngContentType_ReturnsFalse() {
        // When & Then
        assertFalse("PNG attachment should not be identified as GIF", pngAttachment.isGif());
    }

    /**
     * Test that null content type doesn't cause crash
     */
    @Test
    public void testIsGif_WithNullContentType_ReturnsFalse() {
        // Given
        Attachment nullContentTypeAttachment = new Attachment(mockUri, null, "test.gif", 1024);
        
        // When & Then
        assertFalse("Attachment with null content type should not be identified as GIF", 
                   nullContentTypeAttachment.isGif());
    }

    /**
     * Test that empty content type doesn't cause crash
     */
    @Test
    public void testIsGif_WithEmptyContentType_ReturnsFalse() {
        // Given
        Attachment emptyContentTypeAttachment = new Attachment(mockUri, "", "test.gif", 1024);
        
        // When & Then
        assertFalse("Attachment with empty content type should not be identified as GIF", 
                   emptyContentTypeAttachment.isGif());
    }

    /**
     * Test that case sensitivity is handled correctly
     */
    @Test
    public void testIsGif_WithUppercaseContentType_ReturnsFalse() {
        // Given
        Attachment uppercaseAttachment = new Attachment(mockUri, "IMAGE/GIF", "test.gif", 1024);
        
        // When & Then
        assertFalse("Uppercase content type should not match exact case", 
                   uppercaseAttachment.isGif());
    }

    /**
     * Test that GIF attachment is also recognized as an image
     */
    @Test
    public void testIsImage_WithGifContentType_ReturnsTrue() {
        // When & Then
        assertTrue("GIF attachment should also be identified as image", gifAttachment.isImage());
    }

    /**
     * Test that the isGif method and isImage method are consistent
     */
    @Test
    public void testGifDetection_IsConsistentWithImageDetection() {
        // When
        boolean isGif = gifAttachment.isGif();
        boolean isImage = gifAttachment.isImage();
        
        // Then
        assertTrue("GIF should be detected as GIF", isGif);
        assertTrue("GIF should be detected as image", isImage);
        assertTrue("If something is a GIF, it should also be an image", !isGif || isImage);
    }
}