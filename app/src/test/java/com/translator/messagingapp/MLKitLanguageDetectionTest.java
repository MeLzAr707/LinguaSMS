package com.translator.messagingapp;

import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;

/**
 * Test class for ML Kit language detection integration.
 */
@RunWith(RobolectricTestRunner.class)
public class MLKitLanguageDetectionTest {

    private Context context;
    private LanguageDetectionService languageDetectionService;
    private TranslationManager translationManager;
    private UserPreferences userPreferences;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        languageDetectionService = new LanguageDetectionService(context);
        userPreferences = new UserPreferences(context);
        translationManager = new TranslationManager(context, null, userPreferences);
    }

    @Test
    public void testLanguageDetectionServiceInitialization() {
        assertNotNull("Language detection service should be initialized", languageDetectionService);
        assertTrue("Language detection service should be available", languageDetectionService.isAvailable());
    }

    @Test
    public void testTranslationManagerHasLanguageDetectionService() {
        assertNotNull("Translation manager should have language detection service", 
                     translationManager.getLanguageDetectionService());
    }

    @Test
    public void testLanguageDetectionServiceIntegration() {
        // Test that the service can handle empty/null input gracefully
        String result = languageDetectionService.detectLanguage(null);
        assertNull("Detection should return null for null input", result);
        
        result = languageDetectionService.detectLanguage("");
        assertNull("Detection should return null for empty input", result);
        
        result = languageDetectionService.detectLanguage("   ");
        assertNull("Detection should return null for whitespace-only input", result);
    }

    @Test
    public void testConfidenceThreshold() {
        assertEquals("Confidence threshold should be 0.5", 0.5f, 
                    languageDetectionService.getConfidenceThreshold(), 0.001f);
    }

    @Test
    public void testCleanup() {
        // Test that cleanup doesn't throw exceptions
        languageDetectionService.cleanup();
        translationManager.cleanup();
    }
}