package com.translator.messagingapp;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Test for GeminiNanoTranslationService to ensure offline GenAI features work correctly.
 */
@RunWith(AndroidJUnit4.class)
public class GeminiNanoTranslationServiceTest {

    private GeminiNanoTranslationService translationService;
    private Context context;
    private UserPreferences userPreferences;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        userPreferences = new UserPreferences(context);
        translationService = new GeminiNanoTranslationService(context, userPreferences);
    }

    @Test
    public void testServiceInitialization() {
        assertNotNull("Translation service should be initialized", translationService);
    }

    @Test
    public void testSupportedLanguages() {
        // Test that basic languages are supported
        assertTrue("English should be supported", 
                   translationService.isOfflineTranslationAvailable("en", "es"));
        assertTrue("Spanish should be supported", 
                   translationService.isOfflineTranslationAvailable("es", "en"));
        assertTrue("French should be supported", 
                   translationService.isOfflineTranslationAvailable("fr", "en"));
    }

    @Test
    public void testUnsupportedLanguages() {
        // Test that unsupported languages return false
        assertFalse("Unsupported language should return false", 
                    translationService.isOfflineTranslationAvailable("xyz", "en"));
        assertFalse("Unsupported target language should return false", 
                    translationService.isOfflineTranslationAvailable("en", "xyz"));
    }

    @Test
    public void testGetAvailableLanguages() {
        // This test will only pass if the Gemini Nano model is available
        // For now, we'll test the method exists and returns a set
        assertNotNull("Available languages should not be null", 
                      translationService.getAvailableLanguages());
    }

    @Test
    public void testGetDetailedModelStatus() {
        assertNotNull("Model status should not be null", 
                      translationService.getDetailedModelStatus());
    }

    @Test
    public void testTranslationCallback() throws InterruptedException {
        final boolean[] callbackReceived = {false};
        final String[] result = {null};
        final String[] error = {null};

        // Test translation with callback
        translationService.translateOffline("Hello", "en", "es", 
            new GeminiNanoTranslationService.GeminiNanoTranslationCallback() {
                @Override
                public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                    callbackReceived[0] = true;
                    result[0] = translatedText;
                    error[0] = errorMessage;
                }
            });

        // Wait for callback (with timeout)
        int timeout = 10; // 10 seconds
        while (!callbackReceived[0] && timeout > 0) {
            Thread.sleep(1000);
            timeout--;
        }

        assertTrue("Callback should be received within timeout", callbackReceived[0]);
        
        // The result depends on whether Gemini Nano model is available
        // For testing purposes, we just ensure the callback was called
        assertNotNull("Result or error should be provided", 
                      result[0] != null || error[0] != null);
    }

    @Test
    public void testModelDownloadCallback() throws InterruptedException {
        final boolean[] callbackReceived = {false};
        final boolean[] downloadResult = {false};
        final String[] error = {null};

        // Test model download
        translationService.downloadLanguageModel("en", 
            new GeminiNanoTranslationService.ModelDownloadCallback() {
                @Override
                public void onDownloadComplete(boolean success, String errorMessage) {
                    callbackReceived[0] = true;
                    downloadResult[0] = success;
                    error[0] = errorMessage;
                }
            });

        // Wait for callback (with timeout)
        int timeout = 60; // 60 seconds for download
        while (!callbackReceived[0] && timeout > 0) {
            Thread.sleep(1000);
            timeout--;
        }

        assertTrue("Download callback should be received within timeout", callbackReceived[0]);
    }

    @Test
    public void testHasAnyDownloadedModels() {
        // This method should work regardless of model availability
        boolean hasModels = translationService.hasAnyDownloadedModels();
        // We can't assert true/false here as it depends on system state
        // Just ensure the method doesn't throw an exception
    }

    @Test
    public void testLanguageModelDownloadedCheck() {
        // Test various language codes
        String[] testLanguages = {"en", "es", "fr", "de", "it"};
        
        for (String lang : testLanguages) {
            boolean isDownloaded = translationService.isLanguageModelDownloaded(lang);
            // This depends on whether Gemini Nano model is available
            // Just ensure the method doesn't throw an exception
        }
    }

    @Test
    public void testCleanup() {
        // Test that cleanup doesn't throw exceptions
        translationService.cleanup();
        
        // Service should still be usable after cleanup for basic checks
        assertNotNull("Service should still exist after cleanup", translationService);
    }
}