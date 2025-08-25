package com.translator.messagingapp;

import android.content.Context;
import android.content.SharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test class for contact language preferences functionality.
 * Tests the core feature: storing and retrieving language preferences per contact.
 */
@RunWith(RobolectricTestRunner.class)
public class ContactLanguagePreferencesTest {

    private UserPreferences userPreferences;
    private Context context;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        userPreferences = new UserPreferences(context);
    }

    @Test
    public void testSetAndGetContactLanguagePreference() {
        String phoneNumber = "1234567890";
        String languageCode = "es";

        // Set a language preference for a contact
        userPreferences.setContactLanguagePreference(phoneNumber, languageCode);

        // Verify we can retrieve it
        String retrievedLanguage = userPreferences.getContactLanguagePreference(phoneNumber);
        assertEquals("Should retrieve the same language code that was set", languageCode, retrievedLanguage);
    }

    @Test
    public void testContactLanguagePreferenceWithNullPhoneNumber() {
        // Setting preference with null phone number should not crash
        userPreferences.setContactLanguagePreference(null, "es");

        // Getting preference with null phone number should return null
        String retrievedLanguage = userPreferences.getContactLanguagePreference(null);
        assertNull("Should return null for null phone number", retrievedLanguage);
    }

    @Test
    public void testContactLanguagePreferenceWithEmptyPhoneNumber() {
        // Setting preference with empty phone number should not crash
        userPreferences.setContactLanguagePreference("", "es");
        userPreferences.setContactLanguagePreference("   ", "fr");

        // Getting preference with empty phone number should return null
        String retrievedLanguage1 = userPreferences.getContactLanguagePreference("");
        String retrievedLanguage2 = userPreferences.getContactLanguagePreference("   ");
        assertNull("Should return null for empty phone number", retrievedLanguage1);
        assertNull("Should return null for whitespace-only phone number", retrievedLanguage2);
    }

    @Test
    public void testRemoveContactLanguagePreference() {
        String phoneNumber = "1234567890";
        String languageCode = "de";

        // Set a preference
        userPreferences.setContactLanguagePreference(phoneNumber, languageCode);
        assertTrue("Should have preference set", userPreferences.hasContactLanguagePreference(phoneNumber));

        // Remove the preference
        userPreferences.removeContactLanguagePreference(phoneNumber);
        assertFalse("Should not have preference after removal", userPreferences.hasContactLanguagePreference(phoneNumber));
        assertNull("Should return null after removal", userPreferences.getContactLanguagePreference(phoneNumber));
    }

    @Test
    public void testSetContactLanguagePreferenceWithNullLanguage() {
        String phoneNumber = "1234567890";

        // Set a preference first
        userPreferences.setContactLanguagePreference(phoneNumber, "es");
        assertTrue("Should have preference set", userPreferences.hasContactLanguagePreference(phoneNumber));

        // Set with null language should remove the preference
        userPreferences.setContactLanguagePreference(phoneNumber, null);
        assertFalse("Should not have preference after setting null", userPreferences.hasContactLanguagePreference(phoneNumber));
    }

    @Test
    public void testEffectiveOutgoingLanguageForContact() {
        String phoneNumber = "1234567890";

        // Set global preferences
        userPreferences.setPreferredLanguage("en");
        userPreferences.setPreferredOutgoingLanguage("fr");

        // No contact-specific preference should return global outgoing preference
        String effectiveLanguage = userPreferences.getEffectiveOutgoingLanguageForContact(phoneNumber);
        assertEquals("Should return global outgoing preference when no contact preference", "fr", effectiveLanguage);

        // Set contact-specific preference
        userPreferences.setContactLanguagePreference(phoneNumber, "es");
        effectiveLanguage = userPreferences.getEffectiveOutgoingLanguageForContact(phoneNumber);
        assertEquals("Should return contact preference when set", "es", effectiveLanguage);

        // Remove contact preference
        userPreferences.removeContactLanguagePreference(phoneNumber);
        effectiveLanguage = userPreferences.getEffectiveOutgoingLanguageForContact(phoneNumber);
        assertEquals("Should return global outgoing preference after removing contact preference", "fr", effectiveLanguage);
    }

    @Test
    public void testEffectiveLanguageFallbackToGeneralPreference() {
        String phoneNumber = "1234567890";

        // Set only general preference
        userPreferences.setPreferredLanguage("de");

        // Should fall back to general preference when no outgoing or contact preference
        String effectiveLanguage = userPreferences.getEffectiveOutgoingLanguageForContact(phoneNumber);
        assertEquals("Should fall back to general preference", "de", effectiveLanguage);
    }

    @Test
    public void testPhoneNumberNormalization() {
        String phoneNumber1 = "1234567890";
        String phoneNumber2 = "(123) 456-7890";
        String phoneNumber3 = "+1-123-456-7890";
        String phoneNumber4 = "123.456.7890";

        String languageCode = "ja";

        // Set preference using different formats
        userPreferences.setContactLanguagePreference(phoneNumber1, languageCode);

        // All formats should retrieve the same preference due to normalization
        assertEquals("Formatted number should retrieve same preference", 
                languageCode, userPreferences.getContactLanguagePreference(phoneNumber2));
        assertEquals("International format should retrieve same preference", 
                languageCode, userPreferences.getContactLanguagePreference(phoneNumber3));
        assertEquals("Dot-separated format should retrieve same preference", 
                languageCode, userPreferences.getContactLanguagePreference(phoneNumber4));
    }

    @Test
    public void testMultipleContactsWithDifferentLanguages() {
        String phone1 = "1234567890";
        String phone2 = "9876543210";
        String phone3 = "5555555555";

        String lang1 = "es";
        String lang2 = "fr";
        String lang3 = "de";

        // Set different languages for different contacts
        userPreferences.setContactLanguagePreference(phone1, lang1);
        userPreferences.setContactLanguagePreference(phone2, lang2);
        userPreferences.setContactLanguagePreference(phone3, lang3);

        // Verify each contact has the correct language
        assertEquals("Contact 1 should have Spanish", lang1, userPreferences.getContactLanguagePreference(phone1));
        assertEquals("Contact 2 should have French", lang2, userPreferences.getContactLanguagePreference(phone2));
        assertEquals("Contact 3 should have German", lang3, userPreferences.getContactLanguagePreference(phone3));

        // Verify all contacts have preferences set
        assertTrue("Contact 1 should have preference", userPreferences.hasContactLanguagePreference(phone1));
        assertTrue("Contact 2 should have preference", userPreferences.hasContactLanguagePreference(phone2));
        assertTrue("Contact 3 should have preference", userPreferences.hasContactLanguagePreference(phone3));
    }

    @Test
    public void testContactUtilsHelperMethods() {
        String phoneNumber = "1234567890";
        String languageCode = "ko";

        // Test setting through ContactUtils
        ContactUtils.setContactLanguagePreference(context, phoneNumber, languageCode);

        // Test getting through ContactUtils
        String retrievedLanguage = ContactUtils.getContactLanguagePreference(context, phoneNumber);
        assertEquals("ContactUtils should set and get correctly", languageCode, retrievedLanguage);

        // Test checking if preference exists
        assertTrue("ContactUtils should confirm preference exists", 
                ContactUtils.hasContactLanguagePreference(context, phoneNumber));

        // Test getting effective language
        String effectiveLanguage = ContactUtils.getEffectiveOutgoingLanguageForContact(context, phoneNumber);
        assertEquals("ContactUtils should return effective language", languageCode, effectiveLanguage);

        // Test removing through ContactUtils
        ContactUtils.removeContactLanguagePreference(context, phoneNumber);
        assertFalse("ContactUtils should remove preference", 
                ContactUtils.hasContactLanguagePreference(context, phoneNumber));
    }

    @Test
    public void testContactUtilsWithNullContext() {
        String phoneNumber = "1234567890";
        String languageCode = "ar";

        // All ContactUtils methods should handle null context gracefully
        ContactUtils.setContactLanguagePreference(null, phoneNumber, languageCode);
        
        String retrievedLanguage = ContactUtils.getContactLanguagePreference(null, phoneNumber);
        assertNull("Should return null for null context", retrievedLanguage);

        boolean hasPreference = ContactUtils.hasContactLanguagePreference(null, phoneNumber);
        assertFalse("Should return false for null context", hasPreference);

        String effectiveLanguage = ContactUtils.getEffectiveOutgoingLanguageForContact(null, phoneNumber);
        assertEquals("Should return default language for null context", "en", effectiveLanguage);

        // Remove should not crash with null context
        ContactUtils.removeContactLanguagePreference(null, phoneNumber);
    }

    @Test
    public void testLanguageCodeTrimming() {
        String phoneNumber = "1234567890";
        String languageCodeWithSpaces = "  hi  ";
        String expectedLanguageCode = "hi";

        // Set language with spaces
        userPreferences.setContactLanguagePreference(phoneNumber, languageCodeWithSpaces);

        // Should retrieve trimmed language code
        String retrievedLanguage = userPreferences.getContactLanguagePreference(phoneNumber);
        assertEquals("Should trim language code", expectedLanguageCode, retrievedLanguage);
    }

    @Test
    public void testBackwardCompatibility() {
        // Test that existing methods still work as expected
        userPreferences.setPreferredLanguage("en");
        userPreferences.setPreferredOutgoingLanguage("es");
        userPreferences.setPreferredIncomingLanguage("fr");

        assertEquals("General preference should work", "en", userPreferences.getPreferredLanguage());
        assertEquals("Outgoing preference should work", "es", userPreferences.getPreferredOutgoingLanguage());
        assertEquals("Incoming preference should work", "fr", userPreferences.getPreferredIncomingLanguage());
        assertEquals("Target language should work", "en", userPreferences.getTargetLanguage());
    }
}