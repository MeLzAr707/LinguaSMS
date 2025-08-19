package com.translator.messagingapp;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Integration test for end-to-end translation display and persistence functionality.
 */
public class TranslationEndToEndTest {

    private Message testMessage;
    private MockTranslationCache mockCache;
    private TestMessageAdapter adapter;

    @Before
    public void setUp() {
        testMessage = new Message();
        testMessage.setId(12345);
        testMessage.setBody("Hello world");
        testMessage.setAddress("555-1234");
        testMessage.setType(Message.TYPE_INBOX);
        
        mockCache = new MockTranslationCache();
        adapter = new TestMessageAdapter();
    }

    @Test
    public void testCompleteTranslationWorkflow() {
        // Step 1: Initial state - no translation
        assertFalse("Message should not be translated initially", testMessage.isTranslated());
        assertFalse("Show translation should be false initially", testMessage.isShowTranslation());
        
        TestDisplayResult initialResult = adapter.testDisplayLogic(testMessage);
        assertEquals("Should show original text", "Hello world", initialResult.mainText);
        assertFalse("Should not show original text separately", initialResult.showOriginalText);
        
        // Step 2: Translate the message
        testMessage.setTranslatedText("Hola mundo");
        testMessage.setOriginalLanguage("en");
        testMessage.setTranslatedLanguage("es");
        testMessage.setShowTranslation(true);
        
        // Step 3: Verify dual display
        TestDisplayResult translatedResult = adapter.testDisplayLogic(testMessage);
        assertEquals("Should show translated text as main", "Hola mundo", translatedResult.mainText);
        assertTrue("Should show original text separately", translatedResult.showOriginalText);
        assertEquals("Should show original with prefix", "Original: Hello world", translatedResult.originalText);
        
        // Step 4: Save translation state
        testMessage.saveTranslationState(mockCache);
        assertTrue("Cache should contain the translation", mockCache.size() > 0);
        
        // Step 5: Simulate app restart - clear message state
        testMessage.setTranslatedText(null);
        testMessage.setOriginalLanguage(null);
        testMessage.setTranslatedLanguage(null);
        testMessage.setShowTranslation(false);
        
        // Verify cleared state
        assertFalse("Message should not be translated after clearing", testMessage.isTranslated());
        assertFalse("Show translation should be false after clearing", testMessage.isShowTranslation());
        
        TestDisplayResult clearedResult = adapter.testDisplayLogic(testMessage);
        assertEquals("Should show original text after clearing", "Hello world", clearedResult.mainText);
        assertFalse("Should not show original text separately after clearing", clearedResult.showOriginalText);
        
        // Step 6: Restore translation state (simulate app restart loading)
        boolean restored = testMessage.restoreTranslationState(mockCache);
        assertTrue("Translation state should be restored", restored);
        
        // Step 7: Verify restoration worked
        assertTrue("Message should be translated after restoration", testMessage.isTranslated());
        assertTrue("Show translation should be true after restoration", testMessage.isShowTranslation());
        assertEquals("Translated text should be restored", "Hola mundo", testMessage.getTranslatedText());
        assertEquals("Original language should be restored", "en", testMessage.getOriginalLanguage());
        assertEquals("Translated language should be restored", "es", testMessage.getTranslatedLanguage());
        
        // Step 8: Verify dual display is restored
        TestDisplayResult restoredResult = adapter.testDisplayLogic(testMessage);
        assertEquals("Should show translated text as main after restoration", "Hola mundo", restoredResult.mainText);
        assertTrue("Should show original text separately after restoration", restoredResult.showOriginalText);
        assertEquals("Should show original with prefix after restoration", "Original: Hello world", restoredResult.originalText);
    }

    @Test
    public void testTranslationTogglePersistence() {
        // Set up translated message
        testMessage.setTranslatedText("Bonjour le monde");
        testMessage.setShowTranslation(true);
        
        // Save initial state
        testMessage.saveTranslationState(mockCache);
        
        // Toggle translation off
        testMessage.setShowTranslation(false);
        testMessage.saveTranslationState(mockCache); // Save the toggle state
        
        // Verify toggle effect
        TestDisplayResult toggledResult = adapter.testDisplayLogic(testMessage);
        assertEquals("Should show original text when toggled off", "Hello world", toggledResult.mainText);
        assertFalse("Should not show original text separately when toggled off", toggledResult.showOriginalText);
        
        // Simulate app restart
        testMessage.setTranslatedText(null);
        testMessage.setShowTranslation(true); // Reset to opposite state
        
        // Restore state
        testMessage.restoreTranslationState(mockCache);
        
        // Verify toggle state was preserved
        assertFalse("Show translation should be false after restore (toggle state preserved)", testMessage.isShowTranslation());
        assertTrue("Message should still be translated", testMessage.isTranslated());
        
        TestDisplayResult restoredToggleResult = adapter.testDisplayLogic(testMessage);
        assertEquals("Should show original text (toggle state preserved)", "Hello world", restoredToggleResult.mainText);
        assertFalse("Should not show original text separately (toggle state preserved)", restoredToggleResult.showOriginalText);
    }

    @Test
    public void testMmsWithTranslation() {
        // Create MMS message
        MmsMessage mmsMessage = new MmsMessage();
        mmsMessage.setId(54321);
        mmsMessage.setBody("Photo caption");
        mmsMessage.setMessageType(Message.MESSAGE_TYPE_MMS);
        
        // Translate it
        mmsMessage.setTranslatedText("Pie de foto");
        mmsMessage.setShowTranslation(true);
        
        TestDisplayResult mmsResult = adapter.testDisplayLogic(mmsMessage);
        assertEquals("Should show translated MMS text", "Pie de foto", mmsResult.mainText);
        assertTrue("Should show original MMS text separately", mmsResult.showOriginalText);
        assertEquals("Should show original MMS with prefix", "Original: Photo caption", mmsResult.originalText);
    }

    /**
     * Test adapter for testing display logic
     */
    private static class TestMessageAdapter {
        public TestDisplayResult testDisplayLogic(Message message) {
            TestDisplayResult result = new TestDisplayResult();
            
            if (message.isShowTranslation() && message.isTranslated()) {
                String originalBody = getOriginalTextForMessage(message);
                String translatedText = message.getTranslatedText();
                
                result.originalText = "Original: " + originalBody;
                result.showOriginalText = true;
                result.mainText = translatedText;
            } else {
                String displayText = getOriginalTextForMessage(message);
                result.mainText = displayText;
                result.showOriginalText = false;
                result.originalText = null;
            }
            
            return result;
        }
        
        private String getOriginalTextForMessage(Message message) {
            String body = message.getBody();

            if (message.isMms()) {
                boolean hasAttachments = message.hasAttachments();
                boolean hasText = body != null && !body.trim().isEmpty();
                
                if (hasText && hasAttachments) {
                    return body + " 📎";
                } else if (hasText) {
                    return body;
                } else if (hasAttachments) {
                    return "[Media Message]";
                } else {
                    return "[MMS Message]";
                }
            }

            if (body == null || body.trim().isEmpty()) {
                if (message.isRcs()) {
                    return "[RCS Message - Content not available]";
                } else {
                    return "[Empty Message]";
                }
            }

            return body;
        }
    }
    
    private static class TestDisplayResult {
        public String mainText;
        public String originalText;
        public boolean showOriginalText;
    }
    
    private static class MockTranslationCache extends TranslationCache {
        private java.util.Map<String, String> cache = new java.util.HashMap<>();
        
        public MockTranslationCache() {
            super(null);
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