package com.translator.messagingapp;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Unit test for translation original message display functionality.
 * Tests the core logic for displaying both original and translated text.
 */
public class TranslationOriginalDisplayTest {

    private Message testMessage;
    private MockTranslationCache mockCache;

    @Before
    public void setUp() {
        testMessage = new Message();
        testMessage.setId(12345);
        testMessage.setBody("Hello world");
        testMessage.setAddress("555-1234");
        testMessage.setType(Message.TYPE_INBOX);
        
        mockCache = new MockTranslationCache();
    }

    @Test
    public void testMessageTranslationDisplay() {
        // Initially, message should not be translated
        assertFalse("Message should not be translated initially", testMessage.isTranslated());
        assertFalse("Show translation should be false initially", testMessage.isShowTranslation());
        
        // Set translated text
        testMessage.setTranslatedText("Hola mundo");
        testMessage.setShowTranslation(true);
        testMessage.setTranslatedLanguage("es");
        
        // Now message should be considered translated
        assertTrue("Message should be translated", testMessage.isTranslated());
        assertTrue("Show translation should be true", testMessage.isShowTranslation());
        assertEquals("Translated text should match", "Hola mundo", testMessage.getTranslatedText());
    }

    @Test
    public void testTranslationStatePersistence() {
        // Set up a translated message
        testMessage.setTranslatedText("Bonjour le monde");
        testMessage.setOriginalLanguage("en");
        testMessage.setTranslatedLanguage("fr");
        testMessage.setShowTranslation(true);
        
        // Save translation state
        testMessage.saveTranslationState(mockCache);
        
        // Clear the message translation data
        testMessage.setTranslatedText(null);
        testMessage.setShowTranslation(false);
        testMessage.setOriginalLanguage(null);
        testMessage.setTranslatedLanguage(null);
        
        // Verify it's cleared
        assertFalse("Message should not be translated after clearing", testMessage.isTranslated());
        assertFalse("Show translation should be false after clearing", testMessage.isShowTranslation());
        
        // Restore translation state
        boolean restored = testMessage.restoreTranslationState(mockCache);
        
        // Verify restoration worked
        assertTrue("Translation state should be restored", restored);
        assertTrue("Message should be translated after restoration", testMessage.isTranslated());
        assertTrue("Show translation should be true after restoration", testMessage.isShowTranslation());
        assertEquals("Translated text should be restored", "Bonjour le monde", testMessage.getTranslatedText());
        assertEquals("Original language should be restored", "en", testMessage.getOriginalLanguage());
        assertEquals("Translated language should be restored", "fr", testMessage.getTranslatedLanguage());
    }

    @Test
    public void testToggleTranslationDisplay() {
        // Set up a translated message
        testMessage.setTranslatedText("Guten Tag Welt");
        testMessage.setShowTranslation(true);
        
        assertTrue("Show translation should be true initially", testMessage.isShowTranslation());
        
        // Toggle off
        testMessage.setShowTranslation(false);
        assertFalse("Show translation should be false after toggle", testMessage.isShowTranslation());
        
        // Toggle back on
        testMessage.setShowTranslation(true);
        assertTrue("Show translation should be true after toggle back", testMessage.isShowTranslation());
    }

    @Test
    public void testTranslationWithEmptyOriginalText() {
        // Test with empty original text
        testMessage.setBody("");
        testMessage.setTranslatedText("Texto traducido");
        testMessage.setShowTranslation(true);
        
        assertTrue("Message should be considered translated", testMessage.isTranslated());
        assertTrue("Show translation should be true", testMessage.isShowTranslation());
        assertEquals("Original body should be empty", "", testMessage.getBody());
        assertEquals("Translated text should be set", "Texto traducido", testMessage.getTranslatedText());
    }

    @Test
    public void testTranslationCacheKeyGeneration() {
        // Test that different messages generate different cache keys
        Message message1 = new Message();
        message1.setId(1);
        message1.setTranslatedText("Test1");
        
        Message message2 = new Message();
        message2.setId(2);
        message2.setTranslatedText("Test2");
        
        // Save both to cache
        message1.saveTranslationState(mockCache);
        message2.saveTranslationState(mockCache);
        
        // Both should be in cache with different keys
        assertTrue("Cache should contain entries", mockCache.size() > 0);
        
        // Clear first message and restore
        message1.setTranslatedText(null);
        message1.setShowTranslation(false);
        
        boolean restored1 = message1.restoreTranslationState(mockCache);
        assertTrue("Message 1 should be restored", restored1);
        assertEquals("Message 1 translation should be restored", "Test1", message1.getTranslatedText());
        
        // Message 2 should still have its translation
        assertEquals("Message 2 should still have its translation", "Test2", message2.getTranslatedText());
    }

    /**
     * Mock implementation of TranslationCache for testing
     */
    private static class MockTranslationCache extends TranslationCache {
        private java.util.Map<String, String> cache = new java.util.HashMap<>();
        
        public MockTranslationCache() {
            super(null); // Pass null context for testing
        }
        
        @Override
        public void put(String key, String value) {
            cache.put(key, value);
        }
        
        @Override
        public String get(String key) {
            return cache.get(key);
        }
        
        @Override
        public void delete(String key) {
            cache.remove(key);
        }
        
        public int size() {
            return cache.size();
        }
    }
}