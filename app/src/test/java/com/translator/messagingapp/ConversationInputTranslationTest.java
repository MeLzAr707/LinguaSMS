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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * Test case to verify that ConversationActivity input translation works like NewMessageActivity
 * Tests that English text can be translated to outgoing language selection using force translation.
 */
@RunWith(RobolectricTestRunner.class)
public class ConversationInputTranslationTest {

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
        
        // Set up mock preferences - English user wanting to translate to Spanish
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferredLanguage()).thenReturn("en");
        when(mockUserPreferences.getPreferredOutgoingLanguage()).thenReturn("es");
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
    public void testConversationInputTranslationShouldUseOutgoingLanguage() {
        // Given: English text that should be translated to Spanish (outgoing language)
        String englishText = "Hello world";
        String expectedSpanishText = "Hola mundo";
        
        // Mock language detection to return English
        when(mockTranslationService.detectLanguage(englishText)).thenReturn("en");
        // Mock translation to return Spanish
        when(mockTranslationService.translate(englishText, "en", "es")).thenReturn(expectedSpanishText);
        
        final boolean[] callbackCalled = {false};
        final boolean[] translationSuccessful = {false};
        final String[] translatedResult = {null};
        final String[] errorResult = {null};
        
        // When: Translating with force translation (like ConversationActivity should do)
        translationManager.translateText(englishText, "es", new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                callbackCalled[0] = true;
                translationSuccessful[0] = success;
                translatedResult[0] = translatedText;
                errorResult[0] = errorMessage;
            }
        }, true); // Force translation for outgoing messages
        
        // Wait a bit for async execution
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // ignore
        }
        
        // Then: Translation should succeed, not be skipped with "already in English"
        assertTrue("Callback should be called", callbackCalled[0]);
        assertTrue("Translation should succeed for outgoing messages with force flag", translationSuccessful[0]);
        assertEquals("Should return translated text", expectedSpanishText, translatedResult[0]);
        assertNull("Should not have error message", errorResult[0]);
    }

    @Test
    public void testConversationInputTranslationWithoutForceFails() {
        // Given: English text that should fail translation without force flag
        String englishText = "Hello world";
        
        // Mock language detection to return English
        when(mockTranslationService.detectLanguage(englishText)).thenReturn("en");
        
        final boolean[] callbackCalled = {false};
        final boolean[] translationSuccessful = {false};
        final String[] errorResult = {null};
        
        // When: Translating without force translation (current ConversationActivity behavior)
        translationManager.translateText(englishText, "en", new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                callbackCalled[0] = true;
                translationSuccessful[0] = success;
                errorResult[0] = errorMessage;
            }
        }, false); // No force translation
        
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