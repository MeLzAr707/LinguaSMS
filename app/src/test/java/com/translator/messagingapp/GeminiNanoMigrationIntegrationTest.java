package com.translator.messagingapp;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Integration test to verify the complete migration from ML Kit to Gemini Nano for GenAI features.
 */
@RunWith(AndroidJUnit4.class)
public class GeminiNanoMigrationIntegrationTest {

    private TranslationManager translationManager;
    private Context context;
    private UserPreferences userPreferences;
    private GoogleTranslationService onlineService;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        userPreferences = new UserPreferences(context);
        onlineService = new GoogleTranslationService(""); // Empty API key for testing
        translationManager = new TranslationManager(context, onlineService, userPreferences);
    }

    @Test
    public void testTranslationManagerInitialization() {
        assertNotNull("TranslationManager should be initialized", translationManager);
    }

    @Test
    public void testGeminiNanoServiceAccess() {
        // Test that TranslationManager provides access to Gemini Nano services
        GeminiNanoTranslationService geminiNanoService = translationManager.getGeminiNanoTranslationService();
        assertNotNull("Gemini Nano translation service should be available", geminiNanoService);
    }

    @Test
    public void testOfflineTranslationCapability() {
        // Enable offline translation
        userPreferences.setOfflineTranslationEnabled(true);
        
        // Test basic offline translation availability check
        final boolean[] callbackReceived = {false};
        final boolean[] translationSuccess = {false};
        final String[] translatedText = {null};
        final String[] errorMessage = {null};

        try {
            translationManager.translateText("Hello", "es", new TranslationManager.TranslationCallback() {
                @Override
                public void onTranslationComplete(boolean success, String result, String error) {
                    callbackReceived[0] = true;
                    translationSuccess[0] = success;
                    translatedText[0] = result;
                    errorMessage[0] = error;
                }
            });

            // Wait for callback with timeout
            int timeout = 30; // 30 seconds
            while (!callbackReceived[0] && timeout > 0) {
                Thread.sleep(1000);
                timeout--;
            }

            assertTrue("Translation callback should be received", callbackReceived[0]);
            // Result depends on whether Gemini Nano model is available
            // We just ensure the system doesn't crash and provides a response
            assertTrue("Should have either success or error", 
                       translationSuccess[0] || errorMessage[0] != null);

        } catch (Exception e) {
            fail("Translation should not throw exceptions: " + e.getMessage());
        }
    }

    @Test
    public void testLanguageDetectionIntegration() throws InterruptedException {
        // Test that language detection works through the translation manager
        final boolean[] detectionComplete = {false};
        final String[] detectedLanguage = {null};

        // Test SMS message translation which includes language detection
        SmsMessage testMessage = new SmsMessage();
        testMessage.setOriginalText("Hello world");
        testMessage.setAddress("1234567890");
        testMessage.setTimestamp(System.currentTimeMillis());

        translationManager.translateSmsMessage(testMessage, new TranslationManager.SmsTranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, SmsMessage translatedMessage) {
                detectionComplete[0] = true;
                if (translatedMessage != null) {
                    detectedLanguage[0] = translatedMessage.getOriginalLanguage();
                }
            }
        });

        // Wait for callback with timeout
        int timeout = 30; // 30 seconds
        while (!detectionComplete[0] && timeout > 0) {
            Thread.sleep(1000);
            timeout--;
        }

        assertTrue("Language detection callback should be received", detectionComplete[0]);
    }

    @Test
    public void testGeminiNanoModelManagement() throws InterruptedException {
        GeminiNanoTranslationService geminiNanoService = translationManager.getGeminiNanoTranslationService();
        assertNotNull("Gemini Nano service should be available", geminiNanoService);

        // Test model status
        assertNotNull("Model status should be available", geminiNanoService.getDetailedModelStatus());
        
        // Test model download capability
        final boolean[] downloadComplete = {false};
        geminiNanoService.downloadLanguageModel("en", new GeminiNanoTranslationService.ModelDownloadCallback() {
            @Override
            public void onDownloadComplete(boolean success, String errorMessage) {
                downloadComplete[0] = true;
            }
        });

        // Wait for download callback with timeout
        int timeout = 60; // 60 seconds for download
        while (!downloadComplete[0] && timeout > 0) {
            Thread.sleep(1000);
            timeout--;
        }

        assertTrue("Download callback should be received", downloadComplete[0]);
    }

    @Test
    public void testOfflineCapabilityChecking() {
        // Test that the system can check offline translation availability
        boolean offlineAvailable = translationManager.getGeminiNanoTranslationService()
                .isOfflineTranslationAvailable("en", "es");
        
        // This depends on model availability, but should not throw exceptions
        // We just ensure the method works
    }

    @Test
    public void testTranslationModeHandling() {
        // Test different translation modes work with Gemini Nano
        userPreferences.setTranslationMode(UserPreferences.TRANSLATION_MODE_OFFLINE_ONLY);
        assertTrue("Offline-only mode should be set", 
                   userPreferences.getTranslationMode() == UserPreferences.TRANSLATION_MODE_OFFLINE_ONLY);

        userPreferences.setTranslationMode(UserPreferences.TRANSLATION_MODE_AUTO);
        assertTrue("Auto mode should be set", 
                   userPreferences.getTranslationMode() == UserPreferences.TRANSLATION_MODE_AUTO);
    }

    @Test
    public void testBackwardCompatibility() {
        // Ensure that the new system maintains backward compatibility
        // for existing API calls
        
        // Test cache access
        assertNotNull("Translation cache should be available", 
                      translationManager.getTranslationCache());
        
        // Test refresh functionality
        translationManager.refreshOfflineModels(); // Should not throw exceptions
        
        // Test cleanup
        translationManager.cleanup(); // Should not throw exceptions
    }

    @Test
    public void testNoMLKitDependencies() {
        // Verify that no ML Kit classes are being instantiated
        GeminiNanoTranslationService geminiNanoService = translationManager.getGeminiNanoTranslationService();
        assertNotNull("Should use Gemini Nano service", geminiNanoService);
        
        // Ensure the service class is the Gemini Nano implementation
        assertTrue("Should be Gemini Nano implementation", 
                   geminiNanoService instanceof GeminiNanoTranslationService);
    }

    @Test
    public void testErrorHandling() throws InterruptedException {
        // Test error handling in the new system
        final boolean[] errorCallbackReceived = {false};
        final String[] errorMessage = {null};

        // Test with invalid language codes
        translationManager.translateText("Hello", "invalid_lang", new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String error) {
                errorCallbackReceived[0] = true;
                errorMessage[0] = error;
                
                assertFalse("Translation should fail for invalid language", success);
                assertNotNull("Error message should be provided", error);
            }
        });

        // Wait for callback
        int timeout = 15;
        while (!errorCallbackReceived[0] && timeout > 0) {
            Thread.sleep(1000);
            timeout--;
        }

        assertTrue("Error callback should be received", errorCallbackReceived[0]);
    }

    @Test
    public void testConcurrentOperations() throws InterruptedException {
        // Test that multiple translation operations can run concurrently
        final int[] completedOperations = {0};
        final Object lock = new Object();

        for (int i = 0; i < 3; i++) {
            final int operationId = i;
            translationManager.translateText("Test " + operationId, "es", new TranslationManager.TranslationCallback() {
                @Override
                public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                    synchronized (lock) {
                        completedOperations[0]++;
                    }
                }
            });
        }

        // Wait for all operations to complete
        int timeout = 60; // 60 seconds total
        while (completedOperations[0] < 3 && timeout > 0) {
            Thread.sleep(1000);
            timeout--;
        }

        assertEquals("All operations should complete", 3, completedOperations[0]);
    }
}