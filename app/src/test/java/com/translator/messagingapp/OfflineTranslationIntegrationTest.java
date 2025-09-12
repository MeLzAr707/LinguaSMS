package com.translator.messagingapp;

import com.translator.messagingapp.message.*;
import com.translator.messagingapp.conversation.*;
import com.translator.messagingapp.mms.*;
import com.translator.messagingapp.contact.*;
import com.translator.messagingapp.notification.*;

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
 * Test for ML Kit offline translation integration.
 * Verifies that the offline translation services are properly integrated
 * and that the TranslationManager uses offline-first approach.
 */
@RunWith(AndroidJUnit4.class)
public class OfflineTranslationIntegrationTest {

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
        
        // Setup default preferences for offline-first mode
        when(mockUserPreferences.getTranslationMode()).thenReturn(UserPreferences.TRANSLATION_MODE_AUTO);
        when(mockUserPreferences.isOfflineTranslationEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferOfflineTranslation()).thenReturn(true);
        
        translationManager = new TranslationManager(context, mockGoogleService, mockUserPreferences);
    }

    @Test
    public void testTranslationManagerHasOfflineServices() {
        // Verify that TranslationManager includes offline services
        OfflineTranslationService offlineService = translationManager.getOfflineTranslationService();
        LanguageDetectionService detectionService = translationManager.getLanguageDetectionService();
        
        assertNotNull("TranslationManager should have OfflineTranslationService", offlineService);
        assertNotNull("TranslationManager should have LanguageDetectionService", detectionService);
    }

    @Test
    public void testOfflineTranslationServiceInitialization() {
        OfflineTranslationService offlineService = translationManager.getOfflineTranslationService();
        OfflineModelManager modelManager = offlineService.getModelManager();
        
        assertNotNull("OfflineTranslationService should have ModelManager", modelManager);
        
        // Test language pair support
        boolean supportedPair = offlineService.isLanguagePairSupported("en", "es");
        assertTrue("English-Spanish should be supported by ML Kit", supportedPair);
        
        String[] supportedLanguages = offlineService.getSupportedLanguages();
        assertTrue("Should support multiple languages", supportedLanguages.length > 10);
    }

    @Test
    public void testLanguageDetectionServiceConfiguration() {
        LanguageDetectionService detectionService = translationManager.getLanguageDetectionService();
        
        // Test confidence threshold configuration
        float threshold = detectionService.getMinConfidenceThreshold();
        assertTrue("Confidence threshold should be reasonable", threshold > 0 && threshold <= 1.0f);
        
        // Test online detection availability based on API key
        when(mockGoogleService.hasApiKey()).thenReturn(true);
        assertTrue("Should have online detection when API key is available", 
                  detectionService.isOnlineDetectionAvailable());
        
        when(mockGoogleService.hasApiKey()).thenReturn(false);
        assertFalse("Should not have online detection without API key", 
                   detectionService.isOnlineDetectionAvailable());
    }

    @Test
    public void testOfflineModelManagerConfiguration() {
        OfflineTranslationService offlineService = translationManager.getOfflineTranslationService();
        OfflineModelManager modelManager = offlineService.getModelManager();
        
        // Test model availability checking (should not crash)
        boolean modelAvailable = modelManager.isModelAvailableInMLKit("en");
        // We can't assert true/false since models may not be downloaded in test environment
        // Just verify the method doesn't crash
        
        OfflineModelManager.OfflineLanguageModel model = modelManager.getModel("en");
        assertNotNull("Should have English model configuration", model);
        assertEquals("Model should have correct language code", "en", model.getLanguageCode());
        assertEquals("Model should have correct display name", "English", model.getDisplayName());
    }

    @Test
    public void testUserPreferencesOfflineSettings() {
        // Test that UserPreferences has offline translation settings
        UserPreferences prefs = new UserPreferences(context);
        
        // Test default values
        assertTrue("Offline translation should be enabled by default", 
                  prefs.isOfflineTranslationEnabled());
        assertTrue("Should prefer offline translation by default", 
                  prefs.getPreferOfflineTranslation());
        assertEquals("Should use auto mode by default", 
                    UserPreferences.TRANSLATION_MODE_AUTO, prefs.getTranslationMode());
        
        // Test setting values
        prefs.setOfflineTranslationEnabled(false);
        assertFalse("Should save offline translation enabled state", 
                   prefs.isOfflineTranslationEnabled());
        
        prefs.setTranslationMode(UserPreferences.TRANSLATION_MODE_OFFLINE);
        assertEquals("Should save translation mode", 
                    UserPreferences.TRANSLATION_MODE_OFFLINE, prefs.getTranslationMode());
    }

    @Test
    public void testTranslationManagerCleanup() {
        // Test that cleanup doesn't crash and properly cleans up resources
        try {
            translationManager.cleanup();
            // If we get here without exception, cleanup succeeded
            assertTrue("Cleanup should complete without errors", true);
        } catch (Exception e) {
            fail("Cleanup should not throw exceptions: " + e.getMessage());
        }
    }
}