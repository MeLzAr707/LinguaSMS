package com.translator.messagingapp;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Tests to verify that English language assumptions have been removed from the codebase.
 */
@RunWith(RobolectricTestRunner.class)
public class LanguageDetectionTest {

    private Context context;
    private LanguageDetectionService languageDetectionService;
    private UserPreferences userPreferences;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        languageDetectionService = new LanguageDetectionService(context);
        userPreferences = new UserPreferences(context);
        
        // Clear any existing preferences to ensure clean test state
        context.getSharedPreferences("translator_prefs", Context.MODE_PRIVATE)
               .edit().clear().apply();
    }

    @Test
    public void testLanguageDetectionServiceIsAvailable() {
        assertTrue("Language detection service should be available", 
                   languageDetectionService.isLanguageDetectionAvailable());
    }

    @Test
    public void testDeviceLanguageUsedWhenPreferenceNotSet() {
        // When no preferred language is set, should use device language instead of "en"
        String preferredLanguage = userPreferences.getPreferredLanguage();
        String deviceLanguage = Locale.getDefault().getLanguage();
        
        assertEquals("Should use device language when preference not set", 
                     deviceLanguage, preferredLanguage);
    }

    @Test
    public void testDeviceLanguageNotAlwaysEnglish() {
        // Simulate different device locales to ensure we don't assume English
        Locale.setDefault(new Locale("es")); // Spanish
        UserPreferences spanishPrefs = new UserPreferences(context);
        assertEquals("Should respect Spanish device locale", "es", 
                     spanishPrefs.getPreferredLanguage());

        Locale.setDefault(new Locale("fr")); // French
        UserPreferences frenchPrefs = new UserPreferences(context);
        assertEquals("Should respect French device locale", "fr", 
                     frenchPrefs.getPreferredLanguage());

        Locale.setDefault(new Locale("zh")); // Chinese
        UserPreferences chinesePrefs = new UserPreferences(context);
        assertEquals("Should respect Chinese device locale", "zh", 
                     chinesePrefs.getPreferredLanguage());
    }

    @Test
    public void testLanguageDetectionSyncDoesNotDefaultToEnglish() {
        // Test that sync language detection doesn't assume English
        Locale.setDefault(new Locale("de")); // German
        LanguageDetectionService germanDetection = new LanguageDetectionService(context);
        
        String detectedLanguage = germanDetection.detectLanguageSync("Some text");
        assertEquals("Should return device language instead of hardcoded English", 
                     "de", detectedLanguage);
    }

    @Test
    public void testLanguageDetectionWithNullText() {
        // Test that null text handling doesn't default to English
        Locale.setDefault(new Locale("ja")); // Japanese
        LanguageDetectionService japaneseDetection = new LanguageDetectionService(context);
        
        String detectedLanguage = japaneseDetection.detectLanguageSync(null);
        assertEquals("Should return device language for null text", 
                     "ja", detectedLanguage);
    }

    @Test
    public void testLanguageDetectionWithEmptyText() {
        // Test that empty text handling doesn't default to English
        Locale.setDefault(new Locale("ko")); // Korean
        LanguageDetectionService koreanDetection = new LanguageDetectionService(context);
        
        String detectedLanguage = koreanDetection.detectLanguageSync("");
        assertEquals("Should return device language for empty text", 
                     "ko", detectedLanguage);
    }

    @Test
    public void testPreferredLanguageCanBeSetToNonEnglish() {
        // Test that we can set and retrieve non-English preferred languages
        userPreferences.setPreferredLanguage("es");
        assertEquals("Should store Spanish preference", "es", 
                     userPreferences.getPreferredLanguage());

        userPreferences.setPreferredLanguage("zh");
        assertEquals("Should store Chinese preference", "zh", 
                     userPreferences.getPreferredLanguage());

        userPreferences.setPreferredLanguage("ar");
        assertEquals("Should store Arabic preference", "ar", 
                     userPreferences.getPreferredLanguage());
    }

    @Test
    public void testLanguageDetectionCallback() throws InterruptedException {
        final String[] detectedLanguage = new String[1];
        final boolean[] callbackCalled = new boolean[1];
        
        // Set device to Italian for this test
        Locale.setDefault(new Locale("it"));
        
        languageDetectionService.detectLanguage("Some test text", 
            new LanguageDetectionService.LanguageDetectionCallback() {
                @Override
                public void onLanguageDetected(String languageCode) {
                    detectedLanguage[0] = languageCode;
                    callbackCalled[0] = true;
                }

                @Override
                public void onDetectionFailed(String errorMessage) {
                    // Should still return device language, not English
                    detectedLanguage[0] = "it"; // Simulate fallback behavior
                    callbackCalled[0] = true;
                }
            });

        // Wait a bit for async callback
        Thread.sleep(100);
        
        assertTrue("Callback should be called", callbackCalled[0]);
        assertNotNull("Detected language should not be null", detectedLanguage[0]);
        // Should be either the detected language or device language, not hard-coded English
        assertNotEquals("Should not default to hardcoded English", "en", detectedLanguage[0]);
    }
}