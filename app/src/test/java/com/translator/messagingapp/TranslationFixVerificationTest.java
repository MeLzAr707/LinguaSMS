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
 * Simple test to verify the fix for translation language detection bug.
 * Tests that the fix prevents the default "en" assignment in offline mode.
 */
@RunWith(RobolectricTestRunner.class)
public class TranslationFixVerificationTest {

    private GoogleTranslationService mockTranslationService;
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
    public void testOnlineDetectionIsPreferredOverOfflineDefault() {
        // Given: Non-English text that should be detected properly
        String spanishText = "Hola, ¿cómo estás?";
        String expectedTranslation = "Hello, how are you?";
        
        // Mock proper language detection
        when(mockTranslationService.detectLanguage(spanishText)).thenReturn("es");
        // Mock successful translation
        when(mockTranslationService.translate(spanishText, "es", "en")).thenReturn(expectedTranslation);
        when(mockTranslationCache.get(anyString())).thenReturn(null); // Not in cache
        
        final boolean[] callbackCalled = {false};
        final boolean[] translationSuccessful = {false};
        final String[] translatedResult = {null};
        
        // When: Translating without specifying source language (auto-detect)
        translationManager.translateText(spanishText, "en", new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                callbackCalled[0] = true;
                translationSuccessful[0] = success;
                translatedResult[0] = translatedText;
            }
        }, true); // Force translation
        
        // Wait a bit for async execution
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // ignore
        }
        
        // Then: Should detect Spanish and translate properly
        assertTrue("Callback should be called", callbackCalled[0]);
        assertTrue("Translation should succeed with proper detection", translationSuccessful[0]);
        assertEquals("Should return translated text", expectedTranslation, translatedResult[0]);
        
        // Verify that detection was called and translation used correct languages
        verify(mockTranslationService).detectLanguage(spanishText);
        verify(mockTranslationService).translate(spanishText, "es", "en");
    }

    @Test
    public void testForceTranslationWorksEvenWithSameLanguageDetection() {
        // Given: Text detected as English (could be incorrect), target is English
        String text = "Text that might be incorrectly detected";
        String expectedTranslation = "Translated text";
        
        when(mockTranslationService.detectLanguage(text)).thenReturn("en");
        when(mockTranslationService.translate(text, "en", "en")).thenReturn(expectedTranslation);
        when(mockTranslationCache.get(anyString())).thenReturn(null);
        
        final boolean[] callbackCalled = {false};
        final boolean[] translationSuccessful = {false};
        final String[] translatedResult = {null};
        
        // When: Force translation even when detected==target
        translationManager.translateText(text, "en", new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                callbackCalled[0] = true;
                translationSuccessful[0] = success;
                translatedResult[0] = translatedText;
            }
        }, true); // Force translation
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // ignore
        }
        
        // Then: Should still translate because of forceTranslation=true
        assertTrue("Callback should be called", callbackCalled[0]);
        assertTrue("Force translation should work even when detected==target", translationSuccessful[0]);
        assertEquals("Should return translated text", expectedTranslation, translatedResult[0]);
        
        verify(mockTranslationService).translate(text, "en", "en");
    }
}