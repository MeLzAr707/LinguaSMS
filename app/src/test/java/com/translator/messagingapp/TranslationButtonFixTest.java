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
 * Test case to verify the fix for translation button issue.
 * Tests that UI-triggered translations use forceTranslation=true to work
 * even when detected language matches target language.
 */
@RunWith(RobolectricTestRunner.class)
public class TranslationButtonFixTest {

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
        
        // Set up mock preferences
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferredLanguage()).thenReturn("en");
        when(mockUserPreferences.isOfflineTranslationEnabled()).thenReturn(false);
        when(mockUserPreferences.getPreferOfflineTranslation()).thenReturn(false);
        
        // Initialize translation manager
        translationManager = new TranslationManager(
            RuntimeEnvironment.getApplication(),
            mockTranslationService,
            mockUserPreferences
        );
    }

    @Test
    public void testTranslationButtonAllowsForceTranslation() {
        // Setup: Text detected as English, target is English
        when(mockTranslationService.detectLanguage("Hello world")).thenReturn("en");
        when(mockTranslationService.translate("Hello world", "en", "en")).thenReturn("Hello world");
        when(mockTranslationCache.get(anyString())).thenReturn(null); // Not in cache
        
        final boolean[] callbackCalled = {false};
        final boolean[] translationSuccessful = {false};
        final String[] translatedResult = {null};
        final String[] errorResult = {null};
        
        // When: Call translateText with forceTranslation = true (simulating UI button click)
        translationManager.translateText("Hello world", "en", new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                callbackCalled[0] = true;
                translationSuccessful[0] = success;
                translatedResult[0] = translatedText;
                errorResult[0] = errorMessage;
            }
        }, true);
        
        // Wait a bit for async execution
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // ignore
        }
        
        // Then: Translation should succeed, not be skipped with "already in English"
        assertTrue("Callback should be called", callbackCalled[0]);
        assertTrue("Translation should succeed with force flag", translationSuccessful[0]);
        assertEquals("Should return translated text", "Hello world", translatedResult[0]);
        assertNull("Should not have error message", errorResult[0]);
    }

    @Test
    public void testNormalTranslationStillSkipsWhenLanguagesMatch() {
        // Setup: Text detected as English, target is English
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
        assertFalse("Translation should be skipped when languages match", translationSuccessful[0]);
        assertTrue("Should contain 'already in' message", errorResult[0] != null && errorResult[0].contains("already in"));
    }
}