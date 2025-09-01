package com.translator.messagingapp;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for TranslationManager with new LanguageDetectionService.
 * Tests the integration between TranslationManager and the enhanced language detection.
 */
@RunWith(AndroidJUnit4.class)
public class TranslationManagerLanguageDetectionTest {

    @Mock
    private GoogleTranslationService mockGoogleService;
    
    @Mock
    private UserPreferences mockUserPreferences;

    private TranslationManager translationManager;
    private Context context;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = ApplicationProvider.getApplicationContext();
        
        // Setup default preferences
        when(mockUserPreferences.getTranslationMode()).thenReturn(UserPreferences.TRANSLATION_MODE_AUTO);
        when(mockUserPreferences.getPreferOfflineTranslation()).thenReturn(false);
        when(mockUserPreferences.isOfflineTranslationEnabled()).thenReturn(true);
        
        translationManager = new TranslationManager(context, mockGoogleService, mockUserPreferences);
    }

    @Test
    public void testTranslationManagerHasLanguageDetectionService() {
        LanguageDetectionService detectionService = translationManager.getLanguageDetectionService();
        assertNotNull("TranslationManager should have LanguageDetectionService", detectionService);
    }

    @Test
    public void testLanguageDetectionServiceConfiguration() {
        LanguageDetectionService detectionService = translationManager.getLanguageDetectionService();
        
        // Test that the service is properly configured
        float confidenceThreshold = detectionService.getMinConfidenceThreshold();
        assertTrue("Confidence threshold should be reasonable", confidenceThreshold > 0 && confidenceThreshold <= 1.0f);
    }

    @Test
    public void testOnlineDetectionAvailability_withApiKey() {
        when(mockGoogleService.hasApiKey()).thenReturn(true);
        
        LanguageDetectionService detectionService = translationManager.getLanguageDetectionService();
        assertTrue("Should have online detection when API key is available", 
                  detectionService.isOnlineDetectionAvailable());
    }

    @Test
    public void testOnlineDetectionAvailability_withoutApiKey() {
        when(mockGoogleService.hasApiKey()).thenReturn(false);
        
        LanguageDetectionService detectionService = translationManager.getLanguageDetectionService();
        assertFalse("Should not have online detection when API key is unavailable", 
                   detectionService.isOnlineDetectionAvailable());
    }

    @Test
    public void testTranslationManagerCleanup() {
        try {
            translationManager.cleanup();
            assertTrue("Cleanup should complete without error", true);
        } catch (Exception e) {
            fail("Cleanup should not throw exception: " + e.getMessage());
        }
    }

    @Test
    public void testTranslationWithoutSourceLanguage() {
        // Setup mock to have API key for online fallback
        when(mockGoogleService.hasApiKey()).thenReturn(true);
        when(mockGoogleService.detectLanguage(anyString())).thenReturn("en");
        when(mockGoogleService.translateText(anyString(), anyString(), anyString())).thenReturn("translated text");
        
        boolean[] callbackInvoked = {false};
        
        // Test translation without specifying source language (should trigger detection)
        translationManager.translateText("Hello world", null, "es", new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                callbackInvoked[0] = true;
                // Note: We can't assert success/failure here as it depends on actual ML Kit availability
                // But we can verify the callback was invoked
            }
        });
        
        // Wait a moment for async operation
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertTrue("Translation callback should be invoked", callbackInvoked[0]);
    }

    @Test 
    public void testGetLanguageName() {
        // Test language name mapping
        assertEquals("English", translationManager.getLanguageName("en"));
        assertEquals("Spanish", translationManager.getLanguageName("es"));
        assertEquals("French", translationManager.getLanguageName("fr"));
        assertEquals("German", translationManager.getLanguageName("de"));
        
        // Test unknown language code
        assertEquals("xyz", translationManager.getLanguageName("xyz"));
        
        // Test null/empty input
        assertEquals("Unknown", translationManager.getLanguageName(null));
        assertEquals("Unknown", translationManager.getLanguageName(""));
    }

    @Test
    public void testOfflineTranslationServiceIntegration() {
        OfflineTranslationService offlineService = translationManager.getOfflineTranslationService();
        assertNotNull("TranslationManager should have OfflineTranslationService", offlineService);
    }

    @Test
    public void testTranslationCacheIntegration() {
        TranslationCache cache = translationManager.getTranslationCache();
        assertNotNull("TranslationManager should have TranslationCache", cache);
    }
}