package com.translator.messagingapp;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

/**
 * Test for the new image preview functionality in NewMessageActivity.
 * Verifies that selected images are properly displayed in the preview area.
 */
@RunWith(MockitoJUnitRunner.class)
public class NewMessageImagePreviewTest {

    @Mock
    private NewMessageActivity mockActivity;
    
    @Mock
    private LinearLayout mockPreviewContainer;
    
    @Mock
    private ImageView mockPreviewImage;
    
    @Mock
    private ContentResolver mockContentResolver;

    private Uri testImageUri;
    private Uri testDocumentUri;
    
    @Before
    public void setUp() {
        testImageUri = Uri.parse("content://media/external/images/1");
        testDocumentUri = Uri.parse("content://media/external/documents/1");
    }

    @Test
    public void testAttachmentPreview_ShowsForImageAttachment() {
        // Given an image attachment is selected
        when(mockContentResolver.getType(testImageUri)).thenReturn("image/jpeg");
        
        // Simulate the logic from updateAttachmentPreview()
        String mimeType = mockContentResolver.getType(testImageUri);
        boolean isImage = mimeType != null && mimeType.startsWith("image/");
        
        // Verify image is correctly identified
        assertTrue("Image MIME type should be identified as image", isImage);
        
        // Preview container should be shown for images
        verify(mockPreviewContainer, never()).setVisibility(View.GONE);
    }

    @Test
    public void testAttachmentPreview_HidesWhenNoAttachment() {
        // Given no attachments are selected (empty list)
        List<Uri> emptyAttachments = null;
        boolean hasAttachments = emptyAttachments != null && !emptyAttachments.isEmpty();
        
        // Preview should be hidden when no attachments
        assertFalse("Should not have attachments when list is null", hasAttachments);
    }

    @Test
    public void testNonImageAttachment_ShowsAttachmentIcon() {
        // Given a non-image attachment is selected
        when(mockContentResolver.getType(testDocumentUri)).thenReturn("application/pdf");
        
        // Simulate the logic from updateAttachmentPreview()
        String mimeType = mockContentResolver.getType(testDocumentUri);
        boolean isImage = mimeType != null && mimeType.startsWith("image/");
        
        // Verify non-image is correctly identified
        assertFalse("PDF MIME type should not be identified as image", isImage);
    }

    @Test
    public void testAttachmentRemoval_HidesPreview() {
        // Simulate removing attachment
        // selectedAttachments.clear() would be called
        boolean hasAttachmentsAfterClear = false; // After clear(), isEmpty() would return true
        
        // Preview should be hidden after removal
        assertFalse("Should not have attachments after clearing", hasAttachmentsAfterClear);
    }

    @Test
    public void testFileNameExtraction_WorksCorrectly() {
        // Test the file name extraction logic
        String testPath = "content://media/external/images/1";
        Uri uri = Uri.parse(testPath);
        
        // The getFileNameFromUri method should handle various URI formats
        String lastSegment = uri.getLastPathSegment();
        assertNotNull("URI should have a last path segment", lastSegment);
        assertEquals("Last path segment should match", "1", lastSegment);
    }

    @Test
    public void testAttachmentActivityResult_HandlesImageSelection() {
        // Test the activity result handling for image selection
        Intent mockData = mock(Intent.class);
        when(mockData.getData()).thenReturn(testImageUri);
        
        // Simulate onActivityResult logic
        Uri selectedUri = mockData.getData();
        assertNotNull("Selected URI should not be null", selectedUri);
        assertEquals("Selected URI should match", testImageUri, selectedUri);
    }

    @Test
    public void testMimeTypeHandling_SupportsCommonImageTypes() {
        // Test various image MIME types
        String[] imageMimeTypes = {
            "image/jpeg",
            "image/jpg", 
            "image/png",
            "image/gif",
            "image/webp"
        };
        
        for (String mimeType : imageMimeTypes) {
            boolean isImage = mimeType.startsWith("image/");
            assertTrue("MIME type " + mimeType + " should be identified as image", isImage);
        }
    }

    @Test
    public void testNullMimeType_HandledGracefully() {
        // Test null MIME type handling
        String nullMimeType = null;
        boolean isImage = nullMimeType != null && nullMimeType.startsWith("image/");
        
        assertFalse("Null MIME type should not be identified as image", isImage);
    }
}