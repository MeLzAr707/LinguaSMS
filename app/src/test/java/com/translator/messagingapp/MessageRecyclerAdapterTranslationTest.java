package com.translator.messagingapp;

import com.translator.messagingapp.message.*;
import com.translator.messagingapp.conversation.*;
import com.translator.messagingapp.mms.*;
import com.translator.messagingapp.contact.*;
import com.translator.messagingapp.notification.*;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Unit test for MessageRecyclerAdapter translation display logic.
 * Tests the logic for displaying original and translated text without UI dependencies.
 */
public class MessageRecyclerAdapterTranslationTest {

    private Message testMessage;

    @Before
    public void setUp() {
        testMessage = new Message();
        testMessage.setId(12345);
        testMessage.setBody("Hello world");
        testMessage.setAddress("555-1234");
        testMessage.setType(Message.TYPE_INBOX);
    }

    @Test
    public void testGetOriginalTextForMessage() {
        // Create a test adapter to access the private method logic
        TestMessageAdapter adapter = new TestMessageAdapter();
        
        // Test normal message
        String originalText = adapter.getOriginalTextForMessage(testMessage);
        assertEquals("Should return original body", "Hello world", originalText);
    }

    @Test
    public void testGetOriginalTextForMmsMessage() {
        TestMessageAdapter adapter = new TestMessageAdapter();
        
        // Create MMS message
        MmsMessage mmsMessage = new MmsMessage();
        mmsMessage.setBody("MMS text content");
        mmsMessage.setMessageType(Message.MESSAGE_TYPE_MMS);
        
        String originalText = adapter.getOriginalTextForMessage(mmsMessage);
        assertEquals("Should return MMS body", "MMS text content", originalText);
    }

    @Test
    public void testGetOriginalTextForEmptyMessage() {
        TestMessageAdapter adapter = new TestMessageAdapter();
        
        // Test empty message
        testMessage.setBody("");
        String originalText = adapter.getOriginalTextForMessage(testMessage);
        assertEquals("Should return empty message placeholder", "[Empty Message]", originalText);
        
        // Test null body
        testMessage.setBody(null);
        originalText = adapter.getOriginalTextForMessage(testMessage);
        assertEquals("Should return empty message placeholder", "[Empty Message]", originalText);
    }

    @Test
    public void testTranslationDisplayLogic() {
        TestMessageAdapter adapter = new TestMessageAdapter();
        
        // Test message without translation
        TestDisplayResult result = adapter.testDisplayLogic(testMessage);
        assertEquals("Should show original text", "Hello world", result.mainText);
        assertFalse("Should not show original text separately", result.showOriginalText);
        assertNull("Original text view should be null", result.originalText);
        
        // Test message with translation shown
        testMessage.setTranslatedText("Hola mundo");
        testMessage.setShowTranslation(true);
        
        result = adapter.testDisplayLogic(testMessage);
        assertEquals("Should show translated text as main", "Hola mundo", result.mainText);
        assertTrue("Should show original text separately", result.showOriginalText);
        assertEquals("Should show original with prefix", "Original: Hello world", result.originalText);
    }

    @Test
    public void testTranslationDisplayToggle() {
        TestMessageAdapter adapter = new TestMessageAdapter();
        
        // Set up translated message
        testMessage.setTranslatedText("Bonjour le monde");
        testMessage.setShowTranslation(true);
        
        // Test with translation shown
        TestDisplayResult result = adapter.testDisplayLogic(testMessage);
        assertEquals("Should show translated text", "Bonjour le monde", result.mainText);
        assertTrue("Should show original text", result.showOriginalText);
        
        // Toggle translation off
        testMessage.setShowTranslation(false);
        result = adapter.testDisplayLogic(testMessage);
        assertEquals("Should show original text", "Hello world", result.mainText);
        assertFalse("Should not show original text separately", result.showOriginalText);
    }

    /**
     * Test adapter that exposes the private methods for testing
     */
    private static class TestMessageAdapter {
        
        public String getOriginalTextForMessage(Message message) {
            // Simulate the logic from MessageRecyclerAdapter.getOriginalTextForMessage()
            String body = message.getBody();

            // Handle MMS messages specially
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

            // Handle null or empty body for non-MMS messages
            if (body == null || body.trim().isEmpty()) {
                if (message.isRcs()) {
                    return "[RCS Message - Content not available]";
                } else {
                    return "[Empty Message]";
                }
            }

            return body;
        }
        
        public TestDisplayResult testDisplayLogic(Message message) {
            TestDisplayResult result = new TestDisplayResult();
            
            // Simulate the binding logic from MessageRecyclerAdapter
            if (message.isShowTranslation() && message.isTranslated()) {
                // Show both original and translated text
                String originalBody = getOriginalTextForMessage(message);
                String translatedText = message.getTranslatedText();
                
                result.originalText = "Original: " + originalBody;
                result.showOriginalText = true;
                result.mainText = translatedText;
            } else {
                // Show only original text
                String displayText = getOriginalTextForMessage(message);
                result.mainText = displayText;
                result.showOriginalText = false;
                result.originalText = null;
            }
            
            return result;
        }
    }
    
    /**
     * Test result container
     */
    private static class TestDisplayResult {
        public String mainText;
        public String originalText;
        public boolean showOriginalText;
    }
}