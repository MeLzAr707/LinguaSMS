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
 * Test case to verify the fix for language detection bug where text detected as English 
 * prevents translation and returns original text.
 * 
 * This reproduces the issue described in the bug report where non-English text
 * incorrectly detected as English results in no translation.
 */
@RunWith(RobolectricTestRunner.class)
public class TranslationLanguageDetectionBugTest {

    private GoogleTranslationService mockTranslationService;
    private OfflineTranslationService mockOfflineTranslationService;
    private TranslationManager translationManager;
    
    @Mock
    private UserPreferences mockUserPreferences;
    
    @Mock
    private TranslationCache mockTranslationCache;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create mock translation service
        mockTranslationService = mock(GoogleTranslationService.class);
        when(mockTranslationService.hasApiKey()).thenReturn(true);
        
        // Set up mock preferences for online translation
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferredLanguage()).thenReturn("en");
        when(mockUserPreferences.getTranslationMode()).thenReturn(UserPreferences.TRANSLATION_MODE_ONLINE_ONLY);
        when(mockUserPreferences.getPreferOfflineTranslation()).thenReturn(false);
        
        // Initialize translation manager
        translationManager = new TranslationManager(
            RuntimeEnvironment.getApplication(),
            mockTranslationService,
            mockUserPreferences
        );
    }

    @Test
    public void testNonEnglishTextIncorrectlyDetectedAsEnglishShouldTranslate() {
        // Given: Non-English text (e.g., Spanish) that is incorrectly detected as English
        String spanishText = "Hola, ¿cómo estás?";
        String expectedTranslation = "Hello, how are you?";
        
        // Mock incorrect language detection (the bug scenario)
        when(mockTranslationService.detectLanguage(spanishText)).thenReturn("en");
        // Mock successful translation when forced
        when(mockTranslationService.translate(spanishText, "en", "en")).thenReturn(expectedTranslation);
        when(mockTranslationCache.get(anyString())).thenReturn(null); // Not in cache
        
        final boolean[] callbackCalled = {false};
        final boolean[] translationSuccessful = {false};
        final String[] translatedResult = {null};
        final String[] errorResult = {null};
        
        // When: Translating with force translation (simulating UI button click)
        translationManager.translateText(spanishText, "en", new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                callbackCalled[0] = true;
                translationSuccessful[0] = success;
                translatedResult[0] = translatedText;
                errorResult[0] = errorMessage;
            }
        }, true); // Force translation to bypass language matching check
        
        // Wait a bit for async execution
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // ignore
        }
        
        // Then: Translation should succeed even with incorrect language detection
        assertTrue("Callback should be called", callbackCalled[0]);
        assertTrue("Translation should succeed with force flag even when incorrectly detected", translationSuccessful[0]);
        assertEquals("Should return translated text", expectedTranslation, translatedResult[0]);
        assertNull("Should not have error message", errorResult[0]);
        
        // Verify that translation was actually called
        verify(mockTranslationService).translate(spanishText, "en", "en");
    }

    @Test
    public void testOfflineTranslationWithIncorrectLanguageDetection() {
        // Setup for offline translation scenario
        when(mockUserPreferences.getTranslationMode()).thenReturn(UserPreferences.TRANSLATION_MODE_OFFLINE_ONLY);
        when(mockUserPreferences.isOfflineTranslationEnabled()).thenReturn(true);
        
        // Create a new translation manager with offline settings
        translationManager = new TranslationManager(
            RuntimeEnvironment.getApplication(),
            mockTranslationService,
            mockUserPreferences
        );
        
        // Given: Non-English text that offline service will process
        String foreignText = "Bonjour le monde";
        
        when(mockTranslationCache.get(anyString())).thenReturn(null); // Not in cache
        
        final boolean[] callbackCalled = {false};
        final boolean[] translationSuccessful = {false};
        final String[] translatedResult = {null};
        final String[] errorResult = {null};
        
        // When: Translating with auto-detection (no source language specified)
        translationManager.translateText(foreignText, "en", new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                callbackCalled[0] = true;
                translationSuccessful[0] = success;
                translatedResult[0] = translatedText;
                errorResult[0] = errorMessage;
            }
        }, true); // Force translation
        
        // Wait a bit for async execution
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // ignore
        }
        
        // Then: Should attempt translation even in offline mode
        assertTrue("Callback should be called", callbackCalled[0]);
        // Note: This test mainly ensures we don't crash and attempt the translation
        // The actual offline translation behavior depends on MLKit availability
    }

    @Test
    public void testWithoutForceTranslationShouldStillSkipWhenLanguagesMatch() {
        // Given: Text detected as English, target is English
        when(mockTranslationService.detectLanguage("Hello world")).thenReturn("en");
        when(mockTranslationCache.get(anyString())).thenReturn(null); // Not in cache
        
        final boolean[] callbackCalled = {false};
        final boolean[] translationSuccessful = {false};
        final String[] errorResult = {null};
        
        // When: Call translateText with forceTranslation = false (normal scenario)
        translationManager.translateText("Hello world", "en", new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                callbackCalled[0] = true;
                translationSuccessful[0] = success;
                errorResult[0] = errorMessage;
            }
        }, false);
        
        // Wait a bit for async execution
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // ignore
        }
        
        // Then: Translation should be skipped with "already in English" message
        assertTrue("Callback should be called", callbackCalled[0]);
        assertFalse("Translation should be skipped when languages match without force", translationSuccessful[0]);
        assertTrue("Should contain 'already in' message", errorResult[0] != null && errorResult[0].contains("already in"));
    }
}