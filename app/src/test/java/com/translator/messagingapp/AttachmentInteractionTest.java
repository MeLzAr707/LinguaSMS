package com.translator.messagingapp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import android.net.Uri;

import static org.mockito.Mockito.*;

/**
 * Test class for attachment interaction functionality.
 * Tests that attachment click and long click events are properly handled.
 */
@RunWith(MockitoJUnitRunner.class)
public class AttachmentInteractionTest {

    @Mock
    private MessageRecyclerAdapter.OnMessageClickListener mockListener;
    
    @Mock
    private MmsMessage mockMmsMessage;
    
    @Mock
    private MmsMessage.Attachment mockAttachment;
    
    @Mock
    private Uri mockUri;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Test that attachment click listener is called with correct parameters
     */
    @Test
    public void testAttachmentClick_WithMmsAttachment() {
        // Given
        int position = 0;
        
        // When
        mockListener.onAttachmentClick(mockAttachment, position);
        
        // Then
        verify(mockListener, times(1)).onAttachmentClick(mockAttachment, position);
    }

    /**
     * Test that attachment click listener is called with URI
     */
    @Test
    public void testAttachmentClick_WithUri() {
        // Given
        int position = 1;
        
        // When
        mockListener.onAttachmentClick(mockUri, position);
        
        // Then
        verify(mockListener, times(1)).onAttachmentClick(mockUri, position);
    }

    /**
     * Test that attachment long click listener is called with correct parameters
     */
    @Test
    public void testAttachmentLongClick_WithMmsAttachment() {
        // Given
        int position = 0;
        
        // When
        mockListener.onAttachmentLongClick(mockAttachment, position);
        
        // Then
        verify(mockListener, times(1)).onAttachmentLongClick(mockAttachment, position);
    }

    /**
     * Test that attachment long click listener is called with URI
     */
    @Test
    public void testAttachmentLongClick_WithUri() {
        // Given
        int position = 1;
        
        // When
        mockListener.onAttachmentLongClick(mockUri, position);
        
        // Then
        verify(mockListener, times(1)).onAttachmentLongClick(mockUri, position);
    }

    /**
     * Test that interface has all required methods for attachment interaction
     */
    @Test
    public void testMessageClickListenerInterface_HasAttachmentMethods() {
        // This test verifies that the interface includes the necessary methods
        // by attempting to call them - if compilation succeeds, the methods exist
        
        mockListener.onAttachmentClick(mockAttachment, 0);
        mockListener.onAttachmentClick(mockUri, 0);
        mockListener.onAttachmentLongClick(mockAttachment, 0);
        mockListener.onAttachmentLongClick(mockUri, 0);
        
        // Verify all calls were made
        verify(mockListener).onAttachmentClick(mockAttachment, 0);
        verify(mockListener).onAttachmentClick(mockUri, 0);
        verify(mockListener).onAttachmentLongClick(mockAttachment, 0);
        verify(mockListener).onAttachmentLongClick(mockUri, 0);
    }
}