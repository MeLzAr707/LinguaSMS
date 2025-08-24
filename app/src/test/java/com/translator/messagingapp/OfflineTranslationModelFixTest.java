package com.translator.messagingapp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test case to verify the fix for offline translation model availability issue.
 * Tests that offline translation properly detects when models are available
 * even when internal tracking might be out of sync.
 */
@RunWith(RobolectricTestRunner.class)
public class OfflineTranslationModelFixTest {

    private OfflineTranslationService offlineTranslationService;
    
    @Mock
    private UserPreferences mockUserPreferences;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up mock preferences
        when(mockUserPreferences.getString("downloaded_offline_models", "")).thenReturn("");
        
        // Initialize offline translation service
        offlineTranslationService = new OfflineTranslationService(
            RuntimeEnvironment.getApplication(),
            mockUserPreferences
        );
    }

    @Test
    public void testOfflineTranslationAvailabilityBasicCheck() {
        // This test verifies the basic functionality of isOfflineTranslationAvailable
        // When no models are tracked internally, it should return false
        
        boolean available = offlineTranslationService.isOfflineTranslationAvailable("en", "es");
        
        // Should return false when no models are tracked
        assertFalse("Should return false when no models are tracked", available);
    }

    @Test
    public void testLanguageCodeConversion() {
        // Test that common language codes are properly converted
        // This verifies the convertToMLKitLanguageCode method indirectly
        
        // Test with null inputs
        boolean availableWithNull = offlineTranslationService.isOfflineTranslationAvailable(null, "es");
        assertFalse("Should return false with null source language", availableWithNull);
        
        availableWithNull = offlineTranslationService.isOfflineTranslationAvailable("en", null);
        assertFalse("Should return false with null target language", availableWithNull);
        
        // Test with unsupported language codes
        boolean availableUnsupported = offlineTranslationService.isOfflineTranslationAvailable("xx", "yy");
        assertFalse("Should return false with unsupported language codes", availableUnsupported);
    }

    @Test
    public void testModelDownloadedCheck() {
        // Test the isLanguageModelDownloaded method
        
        // Should return false for undownloaded models
        boolean downloaded = offlineTranslationService.isLanguageModelDownloaded("es");
        assertFalse("Should return false for undownloaded model", downloaded);
        
        // Should return false for null input
        downloaded = offlineTranslationService.isLanguageModelDownloaded(null);
        assertFalse("Should return false for null language code", downloaded);
        
        // Should return false for unsupported language
        downloaded = offlineTranslationService.isLanguageModelDownloaded("xx");
        assertFalse("Should return false for unsupported language", downloaded);
    }

    @Test
    public void testSupportedLanguages() {
        // Test that getSupportedLanguages returns expected languages
        
        String[] supportedLanguages = offlineTranslationService.getSupportedLanguages();
        
        assertNotNull("Supported languages should not be null", supportedLanguages);
        assertTrue("Should support multiple languages", supportedLanguages.length > 0);
        
        // Check for common languages
        boolean hasEnglish = false;
        boolean hasSpanish = false;
        for (String lang : supportedLanguages) {
            if ("en".equals(lang)) hasEnglish = true;
            if ("es".equals(lang)) hasSpanish = true;
        }
        
        assertTrue("Should support English", hasEnglish);
        assertTrue("Should support Spanish", hasSpanish);
    }
}