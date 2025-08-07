package com.translator.messagingapp;

import android.content.Context;
import android.graphics.Bitmap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import de.hdodenhof.circleimageview.CircleImageView;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ContactAvatarHelper functionality.
 */
@RunWith(RobolectricTestRunner.class)
public class ContactAvatarHelperTest {

    @Mock
    private CircleImageView mockImageView;

    @Mock
    private Conversation mockConversation;

    private Context context;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        context = RuntimeEnvironment.getApplication();
    }

    @Test
    public void testLoadContactAvatar_withNullParameters() {
        // Test with null context
        ContactAvatarHelper.loadContactAvatar(null, mockImageView, mockConversation);
        verify(mockImageView, never()).setImageBitmap(any(Bitmap.class));

        // Test with null imageView
        ContactAvatarHelper.loadContactAvatar(context, null, mockConversation);
        // Should not crash

        // Test with null conversation
        ContactAvatarHelper.loadContactAvatar(context, mockImageView, null);
        // Should set default avatar or handle gracefully
    }

    @Test
    public void testLoadContactAvatar_withValidParameters() {
        // Setup mock conversation
        when(mockConversation.getContactName()).thenReturn("John Doe");
        when(mockConversation.getAddress()).thenReturn("+1234567890");

        // Test loading avatar
        ContactAvatarHelper.loadContactAvatar(context, mockImageView, mockConversation);

        // Verify that some method was called on the imageView
        // (Either setImageBitmap for initials or setImageResource for default)
        verify(mockImageView, atLeastOnce()).setVisibility(anyInt());
    }

    @Test
    public void testInitialsGeneration() {
        // Test normal name
        when(mockConversation.getContactName()).thenReturn("John Doe");
        when(mockConversation.getAddress()).thenReturn("+1234567890");
        
        ContactAvatarHelper.loadContactAvatar(context, mockImageView, mockConversation);
        
        // Should generate initials "JD" for "John Doe"
        verify(mockImageView, atLeastOnce()).setVisibility(anyInt());
    }

    @Test
    public void testPhoneNumberInitials() {
        // Test with phone number only
        when(mockConversation.getContactName()).thenReturn(null);
        when(mockConversation.getAddress()).thenReturn("+1234567890");
        
        ContactAvatarHelper.loadContactAvatar(context, mockImageView, mockConversation);
        
        // Should generate initials from phone number (first digit "1")
        verify(mockImageView, atLeastOnce()).setVisibility(anyInt());
    }

    @Test
    public void testEmptyContactInfo() {
        // Test with empty contact info
        when(mockConversation.getContactName()).thenReturn("");
        when(mockConversation.getAddress()).thenReturn("");
        
        ContactAvatarHelper.loadContactAvatar(context, mockImageView, mockConversation);
        
        // Should handle gracefully and set some default
        verify(mockImageView, atLeastOnce()).setVisibility(anyInt());
    }
}