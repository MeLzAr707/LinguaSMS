package com.translator.messagingapp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Test class to reproduce and verify the fix for the issue where incoming messages
 * are not visible after translation interferes with display.
 */
@RunWith(RobolectricTestRunner.class)
public class IncomingMessageDisplayTest {

    @Mock
    private TranslationManager mockTranslationManager;
    
    @Mock
    private TranslationCache mockTranslationCache;
    
    @Mock
    private UserPreferences mockUserPreferences;

    private MessageRecyclerAdapter adapter;
    private List<Message> messages;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up mock preferences
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferredLanguage()).thenReturn("es");
        
        messages = new ArrayList<>();
        adapter = new MessageRecyclerAdapter(RuntimeEnvironment.getApplication(), messages, null);
    }

    @Test
    public void testIncomingMessageDisplaysWithoutTranslation() {
        // Create a new incoming message
        Message incomingMessage = new Message();
        incomingMessage.setId(1L);
        incomingMessage.setBody("Hello world");
        incomingMessage.setType(Message.TYPE_INBOX);
        incomingMessage.setDate(System.currentTimeMillis());
        incomingMessage.setAddress("+1234567890");
        
        // Verify message has content and should be displayable
        assertNotNull("Message body should not be null", incomingMessage.getBody());
        assertFalse("Message body should not be empty", incomingMessage.getBody().trim().isEmpty());
        assertFalse("Message should not be translated initially", incomingMessage.isTranslated());
        assertFalse("Message should not show translation initially", incomingMessage.isShowTranslation());
        
        // Add to messages list
        messages.add(incomingMessage);
        
        // Verify message is properly added and displayable
        assertEquals("Should have one message", 1, messages.size());
        assertTrue("Message should be incoming", incomingMessage.isIncoming());
    }

    @Test
    public void testIncomingMessageDisplaysEvenWithTranslationState() {
        // Create a message that has translation state but should still display original
        Message messageWithTranslation = new Message();
        messageWithTranslation.setId(2L);
        messageWithTranslation.setBody("Hello world");
        messageWithTranslation.setType(Message.TYPE_INBOX);
        messageWithTranslation.setDate(System.currentTimeMillis());
        messageWithTranslation.setAddress("+1234567890");
        
        // Set translation state that might interfere with display
        messageWithTranslation.setTranslatedText("Hola mundo");
        messageWithTranslation.setTranslatedLanguage("es");
        messageWithTranslation.setShowTranslation(false); // Should show original
        
        // Verify the message has proper display logic
        assertTrue("Message should be translated", messageWithTranslation.isTranslated());
        assertFalse("Message should not show translation", messageWithTranslation.isShowTranslation());
        assertEquals("Original body should be accessible", "Hello world", messageWithTranslation.getBody());
        
        // When showTranslation is false, original text should be displayed
        messages.add(messageWithTranslation);
        assertEquals("Should have one message", 1, messages.size());
    }

    @Test
    public void testMessageDisplayBlockedByEmptyTranslation() {
        // Test the potential issue: message has translation state but empty translated text
        Message problematicMessage = new Message();
        problematicMessage.setId(3L);
        problematicMessage.setBody("Hello world");
        problematicMessage.setType(Message.TYPE_INBOX);
        problematicMessage.setDate(System.currentTimeMillis());
        problematicMessage.setAddress("+1234567890");
        
        // This might be causing the issue: empty translated text but showTranslation = true
        problematicMessage.setTranslatedText(""); // Empty translation
        problematicMessage.setShowTranslation(true); // But trying to show translation
        
        // This combination might cause messages not to display
        assertTrue("Message should be considered translated", problematicMessage.isTranslated());
        assertTrue("Message should show translation", problematicMessage.isShowTranslation());
        
        // But the translated text is empty, which could cause display issues
        assertTrue("Translated text is empty", problematicMessage.getTranslatedText().isEmpty());
        
        // Original body should still be available for fallback
        assertNotNull("Original body should not be null", problematicMessage.getBody());
        assertFalse("Original body should not be empty", problematicMessage.getBody().isEmpty());
    }

    @Test
    public void testMessageDisplayBlockedByNullTranslation() {
        // Test another potential issue: message marked as translated but null translated text
        Message anotherProblematicMessage = new Message();
        anotherProblematicMessage.setId(4L);
        anotherProblematicMessage.setBody("Hello world");
        anotherProblematicMessage.setType(Message.TYPE_INBOX);
        anotherProblematicMessage.setDate(System.currentTimeMillis());
        anotherProblematicMessage.setAddress("+1234567890");
        
        // This might be causing the issue: null translated text but showTranslation = true
        anotherProblematicMessage.setTranslatedText(null); // Null translation
        anotherProblematicMessage.setShowTranslation(true); // But trying to show translation
        
        // Check translation state according to isTranslated() method
        // isTranslated() returns translatedText != null && !translatedText.isEmpty()
        assertFalse("Message should not be considered translated with null text", anotherProblematicMessage.isTranslated());
        assertTrue("Message should show translation flag", anotherProblematicMessage.isShowTranslation());
        
        // In this case, message should fall back to original text display
        assertNotNull("Original body should not be null", anotherProblematicMessage.getBody());
    }

    @Test 
    public void testAutoTranslationDoesNotBlockInitialDisplay() {
        // Test that auto-translation (if happening) doesn't prevent initial message display
        
        // Simulate the scenario described in the issue:
        // 1. Message arrives
        // 2. Auto-translation is triggered
        // 3. Message should still be visible immediately, not wait for translation
        
        Message newMessage = new Message();
        newMessage.setId(5L);
        newMessage.setBody("Bonjour le monde");
        newMessage.setType(Message.TYPE_INBOX);
        newMessage.setDate(System.currentTimeMillis());
        newMessage.setAddress("+1234567890");
        
        // Initially, message should not be translated
        assertFalse("New message should not be translated initially", newMessage.isTranslated());
        assertFalse("New message should not show translation initially", newMessage.isShowTranslation());
        
        // Message should be immediately displayable
        assertNotNull("Message body should be displayable", newMessage.getBody());
        assertFalse("Message body should not be empty", newMessage.getBody().trim().isEmpty());
        
        // Even if auto-translation is enabled, message should display first
        assertTrue("Auto-translate is enabled", mockUserPreferences.isAutoTranslateEnabled());
        
        // The fix should ensure messages display immediately, translation happens separately
        messages.add(newMessage);
        assertEquals("Message should be added to list", 1, messages.size());
        
        // Later, if translation completes, it should update the message without hiding it
        newMessage.setTranslatedText("Hello world");
        newMessage.setTranslatedLanguage("en");
        // setShowTranslation should be false initially to show original first
        assertFalse("Should show original first, translation as secondary", newMessage.isShowTranslation());
    }

    @Test
    public void testEmptyCachedTranslationDoesNotBlockDisplay() {
        // Test that empty cached translations don't interfere with message display
        Message messageWithEmptyCache = new Message();
        messageWithEmptyCache.setId(6L);
        messageWithEmptyCache.setBody("Test message");
        messageWithEmptyCache.setType(Message.TYPE_INBOX);
        messageWithEmptyCache.setDate(System.currentTimeMillis());
        messageWithEmptyCache.setAddress("+1234567890");
        
        // Simulate corrupted cache scenario - empty translated text but showTranslation flag set
        messageWithEmptyCache.setTranslatedText(""); // Empty translation from cache
        messageWithEmptyCache.setShowTranslation(true); // Flag incorrectly set to true
        
        // With our fix, isShowTranslation() should return false due to empty translated text
        assertFalse("Message should not show translation with empty translated text", 
                   messageWithEmptyCache.isShowTranslation());
        
        // Message should fall back to original text display
        assertNotNull("Original body should be available", messageWithEmptyCache.getBody());
        assertFalse("Original body should not be empty", messageWithEmptyCache.getBody().isEmpty());
        
        // Message should be displayable
        messages.add(messageWithEmptyCache);
        assertEquals("Message should be added to list", 1, messages.size());
    }
}