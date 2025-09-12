package com.translator.messagingapp;

import com.translator.messagingapp.message.*;
import com.translator.messagingapp.conversation.*;
import com.translator.messagingapp.mms.*;
import com.translator.messagingapp.contact.*;
import com.translator.messagingapp.notification.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for contact notification tone functionality.
 * Tests the UserPreferences contact notification tone methods and NotificationHelper integration.
 */
@RunWith(RobolectricTestRunner.class)
public class ContactNotificationToneTest {

    private UserPreferences userPreferences;
    private Context context;
    
    @Mock
    private SharedPreferences mockSharedPreferences;
    
    @Mock
    private SharedPreferences.Editor mockEditor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        userPreferences = new UserPreferences(context);
    }

    @Test
    public void testContactNotificationToneStorage() {
        // Test setting and getting notification tone for a contact
        String contactAddress = "1234567890";
        String customToneUri = "content://settings/system/notification_sound";
        
        // Set custom tone
        userPreferences.setContactNotificationTone(contactAddress, customToneUri);
        
        // Verify it's stored correctly
        String retrievedToneUri = userPreferences.getContactNotificationTone(contactAddress);
        assertEquals(customToneUri, retrievedToneUri);
        
        // Verify hasContactNotificationTone returns true
        assertTrue(userPreferences.hasContactNotificationTone(contactAddress));
    }

    @Test
    public void testContactNotificationToneRemoval() {
        // Test removing notification tone for a contact
        String contactAddress = "1234567890";
        String customToneUri = "content://settings/system/notification_sound";
        
        // Set custom tone first
        userPreferences.setContactNotificationTone(contactAddress, customToneUri);
        assertTrue(userPreferences.hasContactNotificationTone(contactAddress));
        
        // Remove the tone by setting it to null
        userPreferences.setContactNotificationTone(contactAddress, null);
        
        // Verify it's removed
        assertNull(userPreferences.getContactNotificationTone(contactAddress));
        assertFalse(userPreferences.hasContactNotificationTone(contactAddress));
    }

    @Test
    public void testPhoneNumberNormalization() {
        // Test that different formats of the same phone number are handled consistently
        String baseNumber = "1234567890";
        String customToneUri = "content://settings/system/notification_sound";
        
        // Set tone for one format
        userPreferences.setContactNotificationTone("(123) 456-7890", customToneUri);
        
        // Verify it can be retrieved using different formats
        assertEquals(customToneUri, userPreferences.getContactNotificationTone("123-456-7890"));
        assertEquals(customToneUri, userPreferences.getContactNotificationTone("1234567890"));
        assertEquals(customToneUri, userPreferences.getContactNotificationTone("+11234567890"));
        assertEquals(customToneUri, userPreferences.getContactNotificationTone("11234567890"));
    }

    @Test
    public void testNoCustomToneReturnsNull() {
        // Test that a contact without a custom tone returns null
        String contactAddress = "9876543210";
        
        // Should return null for contact without custom tone
        assertNull(userPreferences.getContactNotificationTone(contactAddress));
        assertFalse(userPreferences.hasContactNotificationTone(contactAddress));
    }

    @Test
    public void testEmptyAndNullAddresses() {
        // Test handling of empty and null addresses
        String customToneUri = "content://settings/system/notification_sound";
        
        // Setting tone for null address should not crash
        userPreferences.setContactNotificationTone(null, customToneUri);
        
        // Setting tone for empty address should not crash  
        userPreferences.setContactNotificationTone("", customToneUri);
        
        // Getting tone for null/empty should return null
        assertNull(userPreferences.getContactNotificationTone(null));
        assertNull(userPreferences.getContactNotificationTone(""));
        assertFalse(userPreferences.hasContactNotificationTone(null));
        assertFalse(userPreferences.hasContactNotificationTone(""));
    }

    @Test
    public void testNotificationHelperUsesCustomTone() {
        // Test that NotificationHelper uses the custom tone when available
        String contactAddress = "1234567890";
        String customToneUri = "content://settings/system/notification_sound";
        
        // Set custom tone
        userPreferences.setContactNotificationTone(contactAddress, customToneUri);
        
        // Create NotificationHelper
        NotificationHelper notificationHelper = new NotificationHelper(context);
        
        // This test verifies that the NotificationHelper can be created successfully
        // In a full test, we would mock the notification system to verify the correct tone is used
        assertNotNull(notificationHelper);
    }

    @Test
    public void testMultipleContactsWithDifferentTones() {
        // Test that multiple contacts can have different notification tones
        String contact1 = "1234567890";
        String contact2 = "0987654321";
        String tone1 = "content://settings/system/notification_sound_1";
        String tone2 = "content://settings/system/notification_sound_2";
        
        // Set different tones for different contacts
        userPreferences.setContactNotificationTone(contact1, tone1);
        userPreferences.setContactNotificationTone(contact2, tone2);
        
        // Verify each contact has their own tone
        assertEquals(tone1, userPreferences.getContactNotificationTone(contact1));
        assertEquals(tone2, userPreferences.getContactNotificationTone(contact2));
        
        // Verify both return true for hasContactNotificationTone
        assertTrue(userPreferences.hasContactNotificationTone(contact1));
        assertTrue(userPreferences.hasContactNotificationTone(contact2));
        
        // Verify a third contact returns null
        assertNull(userPreferences.getContactNotificationTone("5555555555"));
    }

    @Test
    public void testContactSettingsDialogCreation() {
        // Test that ContactSettingsDialog can be created without crashing
        String contactAddress = "1234567890";
        String contactName = "Test Contact";
        
        ContactSettingsDialog.OnToneSelectedListener mockListener = mock(ContactSettingsDialog.OnToneSelectedListener.class);
        
        ContactSettingsDialog dialog = new ContactSettingsDialog(context, contactAddress, contactName, mockListener);
        assertNotNull(dialog);
        
        // Test the static method
        assertEquals(1001, ContactSettingsDialog.getRingtonePickerRequestCode());
    }
}