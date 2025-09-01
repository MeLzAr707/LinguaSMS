package com.translator.messagingapp;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Locale;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests to verify that TranslationManager no longer assumes English as default language.
 */
@RunWith(RobolectricTestRunner.class)
public class TranslationManagerLanguageTest {

    private Context context;
    private TranslationManager translationManager;
    private UserPreferences userPreferences;
    private GoogleTranslationService mockTranslationService;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        userPreferences = new UserPreferences(context);
        mockTranslationService = mock(GoogleTranslationService.class);
        
        // Clear any existing preferences
        context.getSharedPreferences("translator_prefs", Context.MODE_PRIVATE)
               .edit().clear().apply();
        
        translationManager = new TranslationManager(context, mockTranslationService, userPreferences);
    }

    @Test
    public void testTranslationManagerUsesDeviceLanguageNotEnglish() {
        // Set device to Spanish
        Locale.setDefault(new Locale("es"));
        
        // Create new instances to pick up locale change
        UserPreferences spanishPrefs = new UserPreferences(context);
        TranslationManager spanishManager = new TranslationManager(context, mockTranslationService, spanishPrefs);
        
        // The preferred language should be Spanish, not English
        assertEquals("Should use Spanish device language", "es", spanishPrefs.getPreferredLanguage());
    }

    @Test
    public void testOfflineTranslationDoesNotAssumeEnglish() {
        // Set device to French for this test
        Locale.setDefault(new Locale("fr"));
        
        UserPreferences frenchPrefs = new UserPreferences(context);
        when(mockTranslationService.hasApiKey()).thenReturn(false);
        
        // Set up for offline translation
        frenchPrefs.setTranslationMode(UserPreferences.TRANSLATION_MODE_OFFLINE_ONLY);
        frenchPrefs.setOfflineTranslationEnabled(true);
        
        TranslationManager frenchManager = new TranslationManager(context, mockTranslationService, frenchPrefs);
        
        // Target language should be French device language, not English
        assertEquals("Should use French device language", "fr", frenchPrefs.getPreferredLanguage());
    }

    @Test
    public void testIncomingLanguagePreferenceFallback() {
        // Set device to German
        Locale.setDefault(new Locale("de"));
        UserPreferences germanPrefs = new UserPreferences(context);
        
        // When no specific incoming language is set, should fall back to general preference (device language)
        String incomingLanguage = germanPrefs.getPreferredIncomingLanguage();
        String generalLanguage = germanPrefs.getPreferredLanguage();
        
        assertEquals("Incoming language should fall back to general preference", 
                     generalLanguage, incomingLanguage);
        assertEquals("General preference should be device language", "de", generalLanguage);
    }

    @Test
    public void testOutgoingLanguagePreferenceFallback() {
        // Set device to Italian
        Locale.setDefault(new Locale("it"));
        UserPreferences italianPrefs = new UserPreferences(context);
        
        // When no specific outgoing language is set, should fall back to general preference (device language)
        String outgoingLanguage = italianPrefs.getPreferredOutgoingLanguage();
        String generalLanguage = italianPrefs.getPreferredLanguage();
        
        assertEquals("Outgoing language should fall back to general preference", 
                     generalLanguage, outgoingLanguage);
        assertEquals("General preference should be device language", "it", generalLanguage);
    }

    @Test
    public void testTranslationWithVariousDeviceLanguages() {
        // Test with multiple device languages to ensure no English assumption
        String[] testLanguages = {"es", "fr", "de", "it", "pt", "zh", "ja", "ko", "ar", "hi"};
        
        for (String lang : testLanguages) {
            Locale.setDefault(new Locale(lang));
            
            // Create fresh preferences to pick up new locale
            Context freshContext = RuntimeEnvironment.getApplication();
            UserPreferences langPrefs = new UserPreferences(freshContext);
            
            assertEquals("Should use " + lang + " device language", 
                         lang, langPrefs.getPreferredLanguage());
        }
    }

    @Test
    public void testLanguageDetectionServiceIntegration() {
        // Verify that TranslationManager creates and uses LanguageDetectionService
        assertNotNull("TranslationManager should have been created", translationManager);
        
        // Test cleanup to ensure LanguageDetectionService is properly managed
        translationManager.cleanup(); // Should not throw exception
    }

    @Test
    public void testPreferencesFallbackChain() {
        // Test the fallback chain: specific -> general -> device language
        Locale.setDefault(new Locale("ru")); // Russian
        UserPreferences russianPrefs = new UserPreferences(context);
        
        // Initially, all should be device language (Russian)
        assertEquals("General should be device language", "ru", russianPrefs.getPreferredLanguage());
        assertEquals("Incoming should fall back to general", "ru", russianPrefs.getPreferredIncomingLanguage());
        assertEquals("Outgoing should fall back to general", "ru", russianPrefs.getPreferredOutgoingLanguage());
        
        // Set general to something else
        russianPrefs.setPreferredLanguage("es");
        assertEquals("General should be set to Spanish", "es", russianPrefs.getPreferredLanguage());
        assertEquals("Incoming should fall back to general (Spanish)", "es", russianPrefs.getPreferredIncomingLanguage());
        assertEquals("Outgoing should fall back to general (Spanish)", "es", russianPrefs.getPreferredOutgoingLanguage());
        
        // Set specific incoming language
        russianPrefs.setPreferredIncomingLanguage("fr");
        assertEquals("Incoming should be set to French", "fr", russianPrefs.getPreferredIncomingLanguage());
        assertEquals("Outgoing should still fall back to general (Spanish)", "es", russianPrefs.getPreferredOutgoingLanguage());
    }
}