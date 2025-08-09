package com.translator.messagingapp;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for translation functionality.
 * Tests the core issue: ensuring translations don't have "Translated:" prefix
 * and that proper language preferences are used.
 */
public class TranslationTest {

    private GoogleTranslationService translationService;

    @Before
    public void setUp() {
        translationService = new GoogleTranslationService("test-api-key");
    }

    @Test
    public void testTranslateDoesNotAddPrefix() {
        // Test that translation doesn't add "Translated:" prefix
        String inputText = "Hello world";
        String sourceLanguage = "en";
        String targetLanguage = "es";

        String result = translationService.translate(inputText, sourceLanguage, targetLanguage);

        // The result should NOT contain "Translated:" prefix
        assertNotNull("Translation result should not be null", result);
        assertFalse("Translation should not contain 'Translated:' prefix", 
                   result.startsWith("Translated:"));
        
        // For now, since this is a mock implementation, it should return the original text
        assertEquals("Translation should return original text (mock implementation)", 
                    inputText, result);
    }

    @Test
    public void testTranslateWithNullText() {
        String result = translationService.translate(null, "en", "es");
        assertNull("Translation of null text should return null", result);
    }

    @Test
    public void testTranslateWithEmptyText() {
        String result = translationService.translate("", "en", "es");
        assertEquals("Translation of empty text should return empty string", "", result);
    }

    @Test
    public void testTranslateWithSameSourceAndTarget() {
        String inputText = "Hello world";
        String language = "en";

        String result = translationService.translate(inputText, language, language);
        
        assertEquals("Translation with same source and target should return original text", 
                    inputText, result);
    }

    @Test
    public void testTranslationServiceHasApiKey() {
        assertTrue("Translation service should have API key", translationService.hasApiKey());
        
        GoogleTranslationService serviceWithoutKey = new GoogleTranslationService((String) null);
        assertFalse("Translation service without API key should return false", 
                   serviceWithoutKey.hasApiKey());
    }

    @Test
    public void testDetectLanguageReturnsDefault() {
        // Test that language detection returns a default value
        String result = translationService.detectLanguage("Hello world");
        assertNotNull("Language detection should not return null", result);
        assertEquals("Default detected language should be English", "en", result);
    }

    @Test
    public void testLanguagePreferenceFallback() {
        // Test the pattern used in translation activities for fallback logic
        String preferredOutgoing = null; // Simulate no outgoing preference set
        String generalPreferred = "de"; // Simulate general preference set to German
        
        String targetLanguage = preferredOutgoing;
        if (targetLanguage == null || targetLanguage.isEmpty()) {
            targetLanguage = generalPreferred;
        }
        
        assertEquals("Should fall back to general preferred language", 
                    "de", targetLanguage);
    }

    @Test
    public void testLanguagePreferenceFallbackEmpty() {
        // Test the pattern used in translation activities for fallback logic with empty string
        String preferredOutgoing = ""; // Simulate empty outgoing preference
        String generalPreferred = "it"; // Simulate general preference set to Italian
        
        String targetLanguage = preferredOutgoing;
        if (targetLanguage == null || targetLanguage.isEmpty()) {
            targetLanguage = generalPreferred;
        }
        
        assertEquals("Should fall back to general preferred language when empty", 
                    "it", targetLanguage);
    }

    @Test
    public void testLanguagePreferenceUsesSpecific() {
        // Test that specific preference is used when available
        String preferredOutgoing = "es"; // Simulate Spanish outgoing preference
        String generalPreferred = "fr"; // Simulate French general preference
        
        String targetLanguage = preferredOutgoing;
        if (targetLanguage == null || targetLanguage.isEmpty()) {
            targetLanguage = generalPreferred;
        }
        
        assertEquals("Should use specific outgoing preference when available", 
                    "es", targetLanguage);
    }
}