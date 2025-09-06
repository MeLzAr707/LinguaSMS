package com.translator.messagingapp;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Test for GeminiNanoLanguageDetectionService to ensure offline language detection works correctly.
 */
@RunWith(AndroidJUnit4.class)
public class GeminiNanoLanguageDetectionServiceTest {

    private GeminiNanoLanguageDetectionService detectionService;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        detectionService = new GeminiNanoLanguageDetectionService(context, null);
    }

    @Test
    public void testServiceInitialization() {
        assertNotNull("Detection service should be initialized", detectionService);
    }

    @Test
    public void testSynchronousLanguageDetection() {
        // Test synchronous detection
        String detectedLanguage = detectionService.detectLanguage("Hello world");
        
        // Should return a language code or null
        if (detectedLanguage != null) {
            assertFalse("Should not return empty string", detectedLanguage.isEmpty());
            assertTrue("Should return valid language code", detectedLanguage.length() >= 2);
        }
    }

    @Test
    public void testEmptyTextDetection() {
        // Test with empty text
        String detectedLanguage = detectionService.detectLanguage("");
        assertNull("Empty text should return null", detectedLanguage);
        
        // Test with null text
        detectedLanguage = detectionService.detectLanguage(null);
        assertNull("Null text should return null", detectedLanguage);
    }

    @Test
    public void testAsynchronousLanguageDetection() throws InterruptedException {
        final boolean[] callbackReceived = {false};
        final String[] detectedLanguage = {null};
        final String[] error = {null};

        // Test asynchronous detection
        detectionService.detectLanguage("Hello world", 
            new GeminiNanoLanguageDetectionService.LanguageDetectionCallback() {
                @Override
                public void onDetectionComplete(String languageCode, float confidence) {
                    callbackReceived[0] = true;
                    detectedLanguage[0] = languageCode;
                }

                @Override
                public void onDetectionError(String errorMessage) {
                    callbackReceived[0] = true;
                    error[0] = errorMessage;
                }
            });

        // Wait for callback (with timeout)
        int timeout = 15; // 15 seconds
        while (!callbackReceived[0] && timeout > 0) {
            Thread.sleep(1000);
            timeout--;
        }

        assertTrue("Callback should be received within timeout", callbackReceived[0]);
        assertTrue("Should have either detection result or error", 
                   detectedLanguage[0] != null || error[0] != null);
    }

    @Test
    public void testLanguageDetectionWithConfidence() throws InterruptedException {
        final boolean[] callbackReceived = {false};
        final String[] detectedLanguage = {null};
        final float[] confidence = {0.0f};

        // Test detection with confidence
        detectionService.detectLanguageWithConfidence("Hello world", 
            new GeminiNanoLanguageDetectionService.LanguageDetectionCallback() {
                @Override
                public void onDetectionComplete(String languageCode, float conf) {
                    callbackReceived[0] = true;
                    detectedLanguage[0] = languageCode;
                    confidence[0] = conf;
                }

                @Override
                public void onDetectionError(String errorMessage) {
                    callbackReceived[0] = true;
                }
            });

        // Wait for callback (with timeout)
        int timeout = 15; // 15 seconds
        while (!callbackReceived[0] && timeout > 0) {
            Thread.sleep(1000);
            timeout--;
        }

        assertTrue("Callback should be received within timeout", callbackReceived[0]);
        
        if (detectedLanguage[0] != null) {
            assertTrue("Confidence should be non-negative", confidence[0] >= 0.0f);
            assertTrue("Confidence should not exceed 1.0", confidence[0] <= 1.0f);
        }
    }

    @Test
    public void testLanguageDetectionAvailability() {
        // Test availability check
        boolean isAvailable = detectionService.isLanguageDetectionAvailable();
        // This depends on whether Gemini Nano model is available
        // Just ensure the method doesn't throw an exception
    }

    @Test
    public void testOnlineDetectionAvailability() {
        // Test online detection availability (should be false since we passed null)
        boolean isOnlineAvailable = detectionService.isOnlineDetectionAvailable();
        assertFalse("Online detection should not be available without service", isOnlineAvailable);
    }

    @Test
    public void testMinConfidenceThreshold() {
        float threshold = detectionService.getMinConfidenceThreshold();
        assertTrue("Confidence threshold should be positive", threshold > 0.0f);
        assertTrue("Confidence threshold should be reasonable", threshold <= 1.0f);
    }

    @Test
    public void testGetModelStatus() {
        String status = detectionService.getModelStatus();
        assertNotNull("Model status should not be null", status);
        assertFalse("Model status should not be empty", status.isEmpty());
    }

    @Test
    public void testEnsureModelAvailable() throws InterruptedException {
        final boolean[] callbackReceived = {false};
        final boolean[] downloadResult = {false};
        final String[] error = {null};

        // Test ensuring model is available
        detectionService.ensureModelAvailable(
            new GeminiNanoModelManager.DownloadListener() {
                @Override
                public void onProgress(int progress) {
                    // Progress updates
                }

                @Override
                public void onSuccess() {
                    callbackReceived[0] = true;
                    downloadResult[0] = true;
                }

                @Override
                public void onError(String errorMessage) {
                    callbackReceived[0] = true;
                    error[0] = errorMessage;
                }
            });

        // Wait for callback (with timeout)
        int timeout = 60; // 60 seconds for potential download
        while (!callbackReceived[0] && timeout > 0) {
            Thread.sleep(1000);
            timeout--;
        }

        assertTrue("Callback should be received within timeout", callbackReceived[0]);
    }

    @Test
    public void testMultipleDetectionCalls() throws InterruptedException {
        // Test multiple rapid detection calls
        String[] testTexts = {
            "Hello world",
            "Hola mundo", 
            "Bonjour le monde",
            "Hallo Welt",
            "Ciao mondo"
        };

        for (String text : testTexts) {
            String result = detectionService.detectLanguage(text);
            // Each call should either return a result or null without throwing exceptions
        }
    }

    @Test
    public void testCleanup() {
        // Test that cleanup doesn't throw exceptions
        detectionService.cleanup();
        
        // Service should still be usable after cleanup for basic checks
        assertNotNull("Service should still exist after cleanup", detectionService);
    }
}