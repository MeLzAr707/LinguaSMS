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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * Test case to verify the fix for offline translation language detection issue.
 * 
 * This test ensures that when offline translation is preferred and source language
 * is not yet known (null), the system correctly tries offline language detection
 * instead of immediately falling back to online detection.
 * 
 * This fixes the issue where message bubble translation would fail with "models may
 * not be loaded correctly" errors due to inconsistent language detection logic.
 */
@RunWith(RobolectricTestRunner.class)
public class OfflineTranslationLanguageDetectionFixTest {

    private GoogleTranslationService mockTranslationService;
    private OfflineTranslationService mockOfflineService;
    private TranslationManager translationManager;
    
    @Mock
    private UserPreferences mockUserPreferences;
    
    @Mock
    private TranslationCache mockTranslationCache;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create mock translation services
        mockTranslationService = mock(GoogleTranslationService.class);
        mockOfflineService = mock(OfflineTranslationService.class);
        
        // Set up preferences where offline is preferred
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferredLanguage()).thenReturn("en"); 
        when(mockUserPreferences.getTranslationMode()).thenReturn(0); // AUTO mode
        when(mockUserPreferences.getPreferOfflineTranslation()).thenReturn(true);
        when(mockUserPreferences.isOfflineTranslationEnabled()).thenReturn(true);
        
        // Mock offline availability - Spanish to English models available
        when(mockOfflineService.isOfflineTranslationAvailable("es", "en")).thenReturn(true);
        when(mockOfflineService.isOfflineTranslationAvailable("en", "en")).thenReturn(true);
        
        // When source language is null, should not check availability yet
        when(mockOfflineService.isOfflineTranslationAvailable(isNull(), anyString())).thenReturn(false);
        
        // Initialize translation manager
        translationManager = new TranslationManager(
            RuntimeEnvironment.getApplication(),
            mockTranslationService,
            mockOfflineService,
            mockUserPreferences,
            mockTranslationCache
        );
    }

    @Test
    public void testOfflineLanguageDetectionIsPreferred() {
        // This test verifies that when offline translation is preferred and enabled,
        // the system tries offline language detection first (even when source language is null)
        
        String testText = "Hola mundo"; // Spanish text
        String targetLanguage = "en";   // English target
        
        // Mock offline translation to succeed
        doAnswer(invocation -> {
            OfflineTranslationService.OfflineTranslationCallback callback = 
                invocation.getArgument(3, OfflineTranslationService.OfflineTranslationCallback.class);
            callback.onTranslationComplete(true, "Hello world", null);
            return null;
        }).when(mockOfflineService).translateOffline(anyString(), eq("en"), eq("en"), any());
        
        // Perform translation
        final boolean[] translationCompleted = {false};
        final boolean[] translationSucceeded = {false};
        
        translationManager.translateText(testText, targetLanguage, new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                translationCompleted[0] = true;
                translationSucceeded[0] = success;
            }
        });
        
        // Wait for async operation
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // ignore
        }
        
        // Verify translation was attempted
        assertTrue("Translation should have completed", translationCompleted[0]);
        
        // The key test: verify that offline translation was attempted
        // This verifies that the fix allows offline detection to be tried when source language is null
        verify(mockOfflineService, atLeastOnce()).translateOffline(anyString(), anyString(), eq("en"), any());
        
        // Verify that online translation was NOT used as primary method
        // (it might be used as fallback, but shouldn't be the first choice)
        verify(mockTranslationService, never()).translateText(anyString(), anyString(), eq("en"));
    }

    @Test
    public void testConsistentLanguageDetectionBehavior() {
        // This test ensures that the language detection logic is consistent
        // between the initial availability check and the actual translation
        
        String testText = "Bonjour le monde"; // French text
        String targetLanguage = "en";          // English target
        
        // Mock offline availability for French to English
        when(mockOfflineService.isOfflineTranslationAvailable("fr", "en")).thenReturn(true);
        
        // Mock offline translation success
        doAnswer(invocation -> {
            OfflineTranslationService.OfflineTranslationCallback callback = 
                invocation.getArgument(3, OfflineTranslationService.OfflineTranslationCallback.class);
            callback.onTranslationComplete(true, "Hello world", null);
            return null;
        }).when(mockOfflineService).translateOffline(anyString(), anyString(), eq("en"), any());
        
        // Perform translation
        translationManager.translateText(testText, targetLanguage, new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                // Callback implementation
            }
        });
        
        // Wait for async operation
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // ignore
        }
        
        // Verify that offline translation was attempted
        verify(mockOfflineService, atLeastOnce()).translateOffline(anyString(), anyString(), eq("en"), any());
    }

    @Test
    public void testOfflineDetectionFallbackBehavior() {
        // Test that when offline translation is not available for the detected language pair,
        // it properly falls back to online translation without confusing error messages
        
        String testText = "Ciao mondo"; // Italian text
        String targetLanguage = "en";   // English target
        
        // Mock offline unavailable for Italian to English
        when(mockOfflineService.isOfflineTranslationAvailable("it", "en")).thenReturn(false);
        when(mockOfflineService.isOfflineTranslationAvailable("en", "en")).thenReturn(false);
        
        // Mock offline translation failure
        doAnswer(invocation -> {
            OfflineTranslationService.OfflineTranslationCallback callback = 
                invocation.getArgument(3, OfflineTranslationService.OfflineTranslationCallback.class);
            callback.onTranslationComplete(false, null, "Language models not downloaded");
            return null;
        }).when(mockOfflineService).translateOffline(anyString(), anyString(), eq("en"), any());
        
        // Mock online translation success
        when(mockTranslationService.hasApiKey()).thenReturn(true);
        when(mockTranslationService.detectLanguage(testText)).thenReturn("it");
        when(mockTranslationService.translateText(testText, "it", "en")).thenReturn("Hello world");
        
        // Perform translation
        final String[] resultError = {null};
        final boolean[] resultSuccess = {false};
        
        translationManager.translateText(testText, targetLanguage, new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                resultSuccess[0] = success;
                resultError[0] = errorMessage;
            }
        });
        
        // Wait for async operation
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            // ignore
        }
        
        // Verify that fallback to online translation worked
        verify(mockTranslationService, atLeastOnce()).translateText(anyString(), anyString(), eq("en"));
        
        // Should not have "models may not be loaded correctly" type errors
        // (the actual success/failure depends on the mock setup, but errors should be clear)
        if (!resultSuccess[0]) {
            assertNotNull("Should have a clear error message", resultError[0]);
            assertFalse("Error should not mention model loading issues when online fallback is available", 
                       resultError[0].toLowerCase().contains("models") && 
                       resultError[0].toLowerCase().contains("loaded"));
        }
    }
}