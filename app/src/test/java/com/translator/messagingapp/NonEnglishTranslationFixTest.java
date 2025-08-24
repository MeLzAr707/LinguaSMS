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
import static org.mockito.Mockito.*;

/**
 * Test case to verify the fix for non-English input translation issue.
 * Tests that non-English text can be properly translated using both online and offline methods.
 */
@RunWith(RobolectricTestRunner.class)
public class NonEnglishTranslationFixTest {

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
        
        // Create mock translation services
        mockTranslationService = mock(GoogleTranslationService.class);
        mockOfflineTranslationService = mock(OfflineTranslationService.class);
        when(mockTranslationService.hasApiKey()).thenReturn(true);
        
        // Set up mock preferences for offline translation
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferredLanguage()).thenReturn("en");
        when(mockUserPreferences.getPreferredOutgoingLanguage()).thenReturn("es");
        when(mockUserPreferences.getTranslationMode()).thenReturn(UserPreferences.TRANSLATION_MODE_AUTO);
        when(mockUserPreferences.getPreferOfflineTranslation()).thenReturn(false);
        when(mockUserPreferences.isOfflineTranslationEnabled()).thenReturn(true);
        
        // Initialize translation manager
        translationManager = new TranslationManager(
            RuntimeEnvironment.getApplication(),
            mockTranslationService,
            mockUserPreferences
        );
        
        // Set the offline translation service via reflection if needed
        try {
            java.lang.reflect.Field offlineServiceField = TranslationManager.class.getDeclaredField("offlineTranslationService");
            offlineServiceField.setAccessible(true);
            offlineServiceField.set(translationManager, mockOfflineTranslationService);
        } catch (Exception e) {
            // Handle reflection error
        }
    }

    @Test
    public void testNonEnglishInputWithOnlineDetection() {
        // Setup: Spanish text that should be detected and translated
        String spanishText = "Hola mundo";
        when(mockTranslationService.detectLanguage(spanishText)).thenReturn("es");
        when(mockTranslationService.translate(spanishText, "es", "en")).thenReturn("Hello world");
        when(mockTranslationCache.get(anyString())).thenReturn(null); // Not in cache
        
        final boolean[] callbackCalled = {false};
        final boolean[] translationSuccessful = {false};
        final String[] translatedResult = {null};
        final String[] errorResult = {null};
        
        // When: Translate Spanish text to English with force translation
        translationManager.translateText(spanishText, "en", new TranslationManager.TranslationCallback() {
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
        
        // Then: Translation should succeed with proper language detection
        assertTrue("Callback should be called", callbackCalled[0]);
        assertTrue("Translation should succeed for non-English input", translationSuccessful[0]);
        assertEquals("Should return translated text", "Hello world", translatedResult[0]);
        assertNull("Should not have error message", errorResult[0]);
    }

    @Test
    public void testNonEnglishInputWithOfflineInference() {
        // Setup: No API key available, French text, prefer offline translation
        when(mockTranslationService.hasApiKey()).thenReturn(false);
        when(mockUserPreferences.getTranslationMode()).thenReturn(UserPreferences.TRANSLATION_MODE_OFFLINE_ONLY);
        
        String frenchText = "Bonjour le monde";
        
        // Mock offline translation service to have French->English models available
        when(mockOfflineTranslationService.isOfflineTranslationAvailable("fr", "en")).thenReturn(true);
        when(mockOfflineTranslationService.isOfflineTranslationAvailable("en", "en")).thenReturn(false);
        when(mockOfflineTranslationService.isOfflineTranslationAvailable("es", "en")).thenReturn(false);
        when(mockOfflineTranslationService.isOfflineTranslationAvailable("de", "en")).thenReturn(false);
        when(mockOfflineTranslationService.isOfflineTranslationAvailable("it", "en")).thenReturn(false);
        when(mockOfflineTranslationService.isOfflineTranslationAvailable("pt", "en")).thenReturn(false);
        
        // Mock the offline translation to succeed
        doAnswer(invocation -> {
            OfflineTranslationService.OfflineTranslationCallback callback = invocation.getArgument(3);
            callback.onTranslationComplete(true, "Hello world", null);
            return null;
        }).when(mockOfflineTranslationService).translateOffline(eq(frenchText), eq("fr"), eq("en"), any());
        
        final boolean[] callbackCalled = {false};
        final boolean[] translationSuccessful = {false};
        final String[] translatedResult = {null};
        final String[] errorResult = {null};
        
        // When: Translate French text to English using offline mode
        translationManager.translateText(frenchText, "en", new TranslationManager.TranslationCallback() {
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
        
        // Then: Translation should succeed with proper language inference
        assertTrue("Callback should be called", callbackCalled[0]);
        assertTrue("Translation should succeed for non-English input with offline inference", translationSuccessful[0]);
        assertEquals("Should return translated text", "Hello world", translatedResult[0]);
        assertNull("Should not have error message", errorResult[0]);
    }

    @Test
    public void testIncomingMessageForceTranslation() {
        // Setup: Text that appears to be in target language but should still be translated
        String ambiguousText = "Hello world";
        when(mockTranslationService.detectLanguage(ambiguousText)).thenReturn("en");
        when(mockTranslationService.translate(ambiguousText, "en", "es")).thenReturn("Hola mundo");
        when(mockTranslationCache.get(anyString())).thenReturn(null);
        
        final boolean[] callbackCalled = {false};
        final boolean[] translationSuccessful = {false};
        final String[] translatedResult = {null};
        
        // When: Force translate message (simulating incoming message translation)
        translationManager.translateText(ambiguousText, "es", new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                callbackCalled[0] = true;
                translationSuccessful[0] = success;
                translatedResult[0] = translatedText;
            }
        }, true); // Force translation like incoming messages
        
        // Wait a bit for async execution
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // ignore
        }
        
        // Then: Translation should succeed even when languages appear to match
        assertTrue("Callback should be called", callbackCalled[0]);
        assertTrue("Force translation should work for incoming messages", translationSuccessful[0]);
        assertEquals("Should return translated text", "Hola mundo", translatedResult[0]);
    }
}