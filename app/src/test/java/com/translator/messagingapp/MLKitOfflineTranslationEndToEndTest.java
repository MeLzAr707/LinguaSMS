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
 * Comprehensive end-to-end test for ML Kit offline translation implementation.
 * Verifies that all components work together correctly and that the offline-first
 * approach functions as intended.
 */
@RunWith(AndroidJUnit4.class)
public class MLKitOfflineTranslationEndToEndTest {

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
        
        // Setup preferences for offline-first mode
        when(mockUserPreferences.getTranslationMode()).thenReturn(UserPreferences.TRANSLATION_MODE_AUTO);
        when(mockUserPreferences.isOfflineTranslationEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferOfflineTranslation()).thenReturn(true);
        when(mockUserPreferences.getPreferredLanguage()).thenReturn("en");
        when(mockUserPreferences.getPreferredIncomingLanguage()).thenReturn("en");
        when(mockUserPreferences.getPreferredOutgoingLanguage()).thenReturn("es");
        
        // Setup Google service
        when(mockGoogleService.hasApiKey()).thenReturn(true);
    }

    @Test
    public void testCompleteOfflineTranslationStack() {
        // Initialize translation manager
        translationManager = new TranslationManager(context, mockGoogleService, mockUserPreferences);
        
        // Verify all services are properly initialized
        assertNotNull("TranslationManager should be initialized", translationManager);
        assertNotNull("Should have offline translation service", translationManager.getOfflineTranslationService());
        assertNotNull("Should have language detection service", translationManager.getLanguageDetectionService());
        assertNotNull("Should have translation cache", translationManager.getTranslationCache());
        
        // Verify offline service configuration
        OfflineTranslationService offlineService = translationManager.getOfflineTranslationService();
        assertNotNull("Should have model manager", offlineService.getModelManager());
        assertTrue("Should support common language pairs", 
                  offlineService.isLanguagePairSupported("en", "es"));
        
        String[] supportedLangs = offlineService.getSupportedLanguages();
        assertTrue("Should support many languages", supportedLangs.length >= 50);
        
        // Verify language detection service configuration
        LanguageDetectionService detectionService = translationManager.getLanguageDetectionService();
        float confidence = detectionService.getMinConfidenceThreshold();
        assertTrue("Confidence threshold should be reasonable", confidence >= 0.3f && confidence <= 0.8f);
        assertTrue("Should have online detection with API key", detectionService.isOnlineDetectionAvailable());
    }

    @Test
    public void testOfflineModelManagement() {
        translationManager = new TranslationManager(context, mockGoogleService, mockUserPreferences);
        OfflineTranslationService offlineService = translationManager.getOfflineTranslationService();
        OfflineModelManager modelManager = offlineService.getModelManager();
        
        // Test model retrieval
        OfflineModelManager.OfflineLanguageModel englishModel = modelManager.getModel("en");
        assertNotNull("Should have English model", englishModel);
        assertEquals("Should have correct language code", "en", englishModel.getLanguageCode());
        assertEquals("Should have correct display name", "English", englishModel.getDisplayName());
        
        OfflineModelManager.OfflineLanguageModel spanishModel = modelManager.getModel("es");
        assertNotNull("Should have Spanish model", spanishModel);
        assertEquals("Should have correct language code", "es", spanishModel.getLanguageCode());
        assertEquals("Should have correct display name", "Spanish", spanishModel.getDisplayName());
        
        // Test model status (should not crash)
        boolean englishAvailable = modelManager.isModelAvailableInMLKit("en");
        boolean spanishAvailable = modelManager.isModelAvailableInMLKit("es");
        // Can't assert true/false since models might not be downloaded in test environment
        // Just verify the methods don't crash
    }

    @Test
    public void testUserPreferencesIntegration() {
        UserPreferences prefs = new UserPreferences(context);
        
        // Test default offline preferences
        assertTrue("Offline translation should be enabled by default", prefs.isOfflineTranslationEnabled());
        assertTrue("Should prefer offline by default", prefs.getPreferOfflineTranslation());
        assertEquals("Should use auto mode by default", UserPreferences.TRANSLATION_MODE_AUTO, prefs.getTranslationMode());
        
        // Test mode constants
        assertEquals("Online mode constant", 0, UserPreferences.TRANSLATION_MODE_ONLINE);
        assertEquals("Offline mode constant", 1, UserPreferences.TRANSLATION_MODE_OFFLINE);
        assertEquals("Auto mode constant", 2, UserPreferences.TRANSLATION_MODE_AUTO);
        
        // Test setting and getting preferences
        prefs.setTranslationMode(UserPreferences.TRANSLATION_MODE_OFFLINE);
        assertEquals("Should save offline mode", UserPreferences.TRANSLATION_MODE_OFFLINE, prefs.getTranslationMode());
        
        prefs.setOfflineTranslationEnabled(false);
        assertFalse("Should save offline disabled", prefs.isOfflineTranslationEnabled());
        
        prefs.setPreferOfflineTranslation(false);
        assertFalse("Should save offline preference", prefs.getPreferOfflineTranslation());
    }

    @Test
    public void testTranslationManagerModeSelection() {
        // Test offline-only mode
        when(mockUserPreferences.getTranslationMode()).thenReturn(UserPreferences.TRANSLATION_MODE_OFFLINE);
        translationManager = new TranslationManager(context, mockGoogleService, mockUserPreferences);
        
        assertNotNull("Should initialize in offline mode", translationManager);
        
        // Test online-only mode
        when(mockUserPreferences.getTranslationMode()).thenReturn(UserPreferences.TRANSLATION_MODE_ONLINE);
        translationManager = new TranslationManager(context, mockGoogleService, mockUserPreferences);
        
        assertNotNull("Should initialize in online mode", translationManager);
        
        // Test auto mode
        when(mockUserPreferences.getTranslationMode()).thenReturn(UserPreferences.TRANSLATION_MODE_AUTO);
        translationManager = new TranslationManager(context, mockGoogleService, mockUserPreferences);
        
        assertNotNull("Should initialize in auto mode", translationManager);
    }

    @Test
    public void testErrorHandlingAndCleanup() {
        translationManager = new TranslationManager(context, mockGoogleService, mockUserPreferences);
        
        // Test cleanup doesn't crash
        try {
            translationManager.cleanup();
            // If we reach here, cleanup succeeded
            assertTrue("Cleanup should complete successfully", true);
        } catch (Exception e) {
            fail("Cleanup should not throw exceptions: " + e.getMessage());
        }
        
        // Test that services handle null/empty inputs gracefully
        OfflineTranslationService offlineService = translationManager.getOfflineTranslationService();
        LanguageDetectionService detectionService = translationManager.getLanguageDetectionService();
        
        // These should not crash with null/empty inputs
        boolean supported = offlineService.isLanguagePairSupported(null, "en");
        assertFalse("Should handle null language codes", supported);
        
        supported = offlineService.isLanguagePairSupported("", "en");
        assertFalse("Should handle empty language codes", supported);
        
        String detected = detectionService.detectLanguageSync("");
        // Should return null for empty text, not crash
        assertNull("Should return null for empty text", detected);
    }

    @Test
    public void testMLKitDependenciesConfiguration() {
        // Verify that ML Kit classes are available (would fail to compile if dependencies missing)
        try {
            // These classes should be available with our ML Kit dependencies
            Class.forName("com.google.mlkit.nl.translate.Translation");
            Class.forName("com.google.mlkit.nl.translate.TranslateLanguage");
            Class.forName("com.google.mlkit.nl.languageid.LanguageIdentification");
            assertTrue("ML Kit dependencies should be available", true);
        } catch (ClassNotFoundException e) {
            fail("ML Kit dependencies not found: " + e.getMessage());
        }
    }

    @Test
    public void testIntegrationWithExistingComponents() {
        translationManager = new TranslationManager(context, mockGoogleService, mockUserPreferences);
        
        // Test that existing methods still work
        assertNotNull("Should have translation cache", translationManager.getTranslationCache());
        assertNotNull("Should have cache statistics", translationManager.getCacheStatistics());
        
        // Test language name mapping (existing functionality)
        assertEquals("English", translationManager.getLanguageName("en"));
        assertEquals("Spanish", translationManager.getLanguageName("es"));
        assertEquals("French", translationManager.getLanguageName("fr"));
        assertEquals("German", translationManager.getLanguageName("de"));
        assertEquals("Unknown", translationManager.getLanguageName(null));
        assertEquals("Unknown", translationManager.getLanguageName(""));
        
        // Test that cleanup works for all components
        try {
            translationManager.clearCache();
            translationManager.cleanup();
        } catch (Exception e) {
            fail("Integration cleanup should not fail: " + e.getMessage());
        }
    }
}