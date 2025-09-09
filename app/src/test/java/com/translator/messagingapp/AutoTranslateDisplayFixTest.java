package com.translator.messagingapp;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Test for the auto-translation display fix.
 * Verifies that messages with cached auto-translations display properly when loaded from database.
 */
@RunWith(RobolectricTestRunner.class)
public class AutoTranslateDisplayFixTest {
    
    private TranslationCache mockTranslationCache;
    private UserPreferences mockUserPreferences;
    private Message message;
    
    @Before
    public void setUp() {
        mockTranslationCache = mock(TranslationCache.class);
        mockUserPreferences = mock(UserPreferences.class);
        
        // Create a test message (incoming SMS in Spanish)
        message = new Message();
        message.setId(123L);
        message.setBody("Hola, ¿cómo estás?");
        message.setDate(System.currentTimeMillis());
        message.setType(Message.TYPE_INBOX); // Incoming message
        message.setAddress("+1234567890");
    }
    
    @Test
    public void testAutoTranslationStateRestoration() {
        // Given: Auto-translate is enabled and preferred language is English
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferredIncomingLanguage()).thenReturn("en");
        
        // Auto-translation cache entry exists (as would be created by MessageService)
        String expectedCacheKey = "Hola, ¿cómo estás?_en";
        String expectedTranslation = "Hello, how are you?";
        when(mockTranslationCache.get(expectedCacheKey)).thenReturn(expectedTranslation);
        
        // When: Message attempts to restore translation state
        boolean restored = message.restoreTranslationState(mockTranslationCache, mockUserPreferences);
        
        // Then: Translation state should be restored from auto-translation cache
        assertTrue("Translation state should be restored", restored);
        assertTrue("Message should be marked as translated", message.isTranslated());
        assertEquals("Translated text should match cached value", expectedTranslation, message.getTranslatedText());
        assertEquals("Target language should be set", "en", message.getTranslatedLanguage());
        assertTrue("Show translation should be enabled for auto-translated messages", message.isShowTranslation());
        
        // Verify the correct cache key was used
        verify(mockTranslationCache).get(expectedCacheKey);
    }
    
    @Test
    public void testAutoTranslationStateRestorationWithFallbackLanguage() {
        // Given: Auto-translate is enabled, no specific incoming language set, general preferred language is French
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferredIncomingLanguage()).thenReturn(null);
        when(mockUserPreferences.getPreferredLanguage()).thenReturn("fr");
        
        // Auto-translation cache entry exists for French
        String expectedCacheKey = "Hola, ¿cómo estás?_fr";
        String expectedTranslation = "Salut, comment ça va?";
        when(mockTranslationCache.get(expectedCacheKey)).thenReturn(expectedTranslation);
        
        // When: Message attempts to restore translation state
        boolean restored = message.restoreTranslationState(mockTranslationCache, mockUserPreferences);
        
        // Then: Translation state should be restored using fallback language
        assertTrue("Translation state should be restored", restored);
        assertEquals("Translated text should match cached value", expectedTranslation, message.getTranslatedText());
        assertEquals("Target language should be French", "fr", message.getTranslatedLanguage());
        assertTrue("Show translation should be enabled", message.isShowTranslation());
    }
    
    @Test
    public void testNoAutoTranslationCacheEntry() {
        // Given: Auto-translate is enabled but no cached translation exists
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferredIncomingLanguage()).thenReturn("en");
        when(mockTranslationCache.get(anyString())).thenReturn(null);
        
        // When: Message attempts to restore translation state
        boolean restored = message.restoreTranslationState(mockTranslationCache, mockUserPreferences);
        
        // Then: Translation state should not be restored
        assertFalse("Translation state should not be restored", restored);
        assertFalse("Message should not be marked as translated", message.isTranslated());
        assertNull("Translated text should be null", message.getTranslatedText());
        assertFalse("Show translation should be false", message.isShowTranslation());
    }
    
    @Test
    public void testManualTranslationTakesPrecedence() {
        // Given: Both manual translation state and auto-translation cache exist
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferredIncomingLanguage()).thenReturn("en");
        
        // Manual translation state in cache (JSON format)
        String manualCacheKey = "msg_123_translation_state";
        String manualTranslationJson = "{\"translatedText\":\"Manual translation\",\"originalLanguage\":\"es\",\"translatedLanguage\":\"en\",\"showTranslation\":true}";
        when(mockTranslationCache.get(manualCacheKey)).thenReturn(manualTranslationJson);
        
        // Auto-translation cache also exists
        String autoCacheKey = "Hola, ¿cómo estás?_en";
        when(mockTranslationCache.get(autoCacheKey)).thenReturn("Auto translation");
        
        // When: Message attempts to restore translation state
        boolean restored = message.restoreTranslationState(mockTranslationCache, mockUserPreferences);
        
        // Then: Manual translation should take precedence
        assertTrue("Translation state should be restored", restored);
        assertEquals("Manual translation should take precedence", "Manual translation", message.getTranslatedText());
        assertEquals("Original language should be set from manual translation", "es", message.getOriginalLanguage());
        
        // Verify manual translation was checked first
        verify(mockTranslationCache).get(manualCacheKey);
    }
    
    @Test
    public void testOutgoingMessageUsesOutgoingLanguagePreference() {
        // Given: Outgoing message with different language preference
        message.setType(Message.TYPE_SENT); // Outgoing message
        
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferredOutgoingLanguage()).thenReturn("de"); // German
        
        // Auto-translation cache entry for German
        String expectedCacheKey = "Hola, ¿cómo estás?_de";
        String expectedTranslation = "Hallo, wie gehts?";
        when(mockTranslationCache.get(expectedCacheKey)).thenReturn(expectedTranslation);
        
        // When: Message attempts to restore translation state
        boolean restored = message.restoreTranslationState(mockTranslationCache, mockUserPreferences);
        
        // Then: Should use outgoing language preference
        assertTrue("Translation state should be restored", restored);
        assertEquals("Should use German translation", expectedTranslation, message.getTranslatedText());
        assertEquals("Target language should be German", "de", message.getTranslatedLanguage());
        
        // Verify correct cache key was used
        verify(mockTranslationCache).get(expectedCacheKey);
    }
    
    @Test
    public void testEmptyMessageBodySkipsAutoTranslationCheck() {
        // Given: Message with empty body
        message.setBody("");
        
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferredIncomingLanguage()).thenReturn("en");
        
        // When: Message attempts to restore translation state
        boolean restored = message.restoreTranslationState(mockTranslationCache, mockUserPreferences);
        
        // Then: Should not attempt auto-translation restoration
        assertFalse("Translation state should not be restored for empty message", restored);
        
        // Verify no auto-translation cache lookup was attempted
        verify(mockTranslationCache, never()).get(contains("_en"));
    }
    
    @Test
    public void testNullUserPreferencesSkipsAutoTranslationCheck() {
        // When: Message attempts to restore translation state with null UserPreferences
        boolean restored = message.restoreTranslationState(mockTranslationCache, null);
        
        // Then: Should not attempt auto-translation restoration
        assertFalse("Translation state should not be restored with null UserPreferences", restored);
        
        // Should still check for manual translation state
        verify(mockTranslationCache).get("msg_123_translation_state");
    }
}