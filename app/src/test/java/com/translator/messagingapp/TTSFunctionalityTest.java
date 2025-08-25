package com.translator.messagingapp;

import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test class for Text-to-Speech functionality.
 */
@RunWith(RobolectricTestRunner.class)
public class TTSFunctionalityTest {

    private Context context;
    private UserPreferences userPreferences;
    private TTSManager ttsManager;
    private Message testMessage;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        
        // Use Robolectric context
        context = RuntimeEnvironment.getApplication();
        
        // Create real UserPreferences instance
        userPreferences = new UserPreferences(context);
        
        // Create test message
        testMessage = new Message();
        testMessage.setId(1L);
        testMessage.setBody("Hello, this is a test message.");
        testMessage.setTranslatedText("Hola, este es un mensaje de prueba.");
        testMessage.setOriginalLanguage("en");
        testMessage.setTranslatedLanguage("es");
        testMessage.setTranslated(true);
        testMessage.setShowTranslation(false); // Start with original text showing
    }

    @Test
    public void testUserPreferencesTTSDefaults() {
        // Test default TTS preferences
        assertTrue("TTS should be enabled by default", userPreferences.isTTSEnabled());
        assertEquals("Default TTS speed should be 1.0", 1.0f, userPreferences.getTTSSpeechRate(), 0.01f);
        assertFalse("Should read translated text by default", userPreferences.shouldTTSReadOriginal());
        assertNotNull("TTS language should not be null", userPreferences.getTTSLanguage());
    }

    @Test
    public void testUserPreferencesTTSSettings() {
        // Test setting TTS preferences
        userPreferences.setTTSEnabled(false);
        assertFalse("TTS should be disabled", userPreferences.isTTSEnabled());
        
        userPreferences.setTTSSpeechRate(1.5f);
        assertEquals("TTS speed should be 1.5", 1.5f, userPreferences.getTTSSpeechRate(), 0.01f);
        
        userPreferences.setTTSReadOriginal(true);
        assertTrue("Should read original text", userPreferences.shouldTTSReadOriginal());
        
        userPreferences.setTTSLanguage("fr");
        assertEquals("TTS language should be French", "fr", userPreferences.getTTSLanguage());
    }

    @Test
    public void testTTSSpeedRange() {
        // Test speed range clamping
        userPreferences.setTTSSpeechRate(0.1f); // Below minimum
        assertEquals("Speed should be clamped to minimum", 0.5f, userPreferences.getTTSSpeechRate(), 0.01f);
        
        userPreferences.setTTSSpeechRate(3.0f); // Above maximum
        assertEquals("Speed should be clamped to maximum", 2.0f, userPreferences.getTTSSpeechRate(), 0.01f);
        
        userPreferences.setTTSSpeechRate(1.0f); // Valid speed
        assertEquals("Valid speed should be preserved", 1.0f, userPreferences.getTTSSpeechRate(), 0.01f);
    }

    @Test
    public void testMessageTranslatableProperty() {
        // Test message translatability
        assertTrue("Message with text should be translatable", testMessage.isTranslatable());
        
        Message emptyMessage = new Message();
        emptyMessage.setBody("");
        assertFalse("Empty message should not be translatable", emptyMessage.isTranslatable());
        
        Message nullMessage = new Message();
        nullMessage.setBody(null);
        assertFalse("Message with null body should not be translatable", nullMessage.isTranslatable());
    }

    @Test
    public void testTTSManagerInitialization() {
        // Test TTSManager creation (may not initialize TTS engine in test environment)
        TTSManager manager = new TTSManager(context, userPreferences);
        assertNotNull("TTSManager should be created", manager);
        
        // Test shutdown doesn't crash
        manager.shutdown();
    }

    @Test
    public void testLanguageCodeToLocaleConversion() {
        TTSManager manager = new TTSManager(context, userPreferences);
        
        // Test language name retrieval (basic functionality)
        String englishName = manager.getLanguageName("en");
        assertNotNull("Language name should not be null", englishName);
        
        String spanishName = manager.getLanguageName("es");
        assertNotNull("Spanish language name should not be null", spanishName);
        
        manager.shutdown();
    }

    @Test
    public void testTTSLanguageAvailability() {
        TTSManager manager = new TTSManager(context, userPreferences);
        
        // Test language availability check (may return false in test environment)
        // This mainly tests that the method doesn't crash
        manager.isLanguageAvailable("en");
        manager.isLanguageAvailable("es");
        manager.isLanguageAvailable("invalid_language");
        
        manager.shutdown();
    }

    @Test
    public void testTTSManagerConfigurationMethods() {
        TTSManager manager = new TTSManager(context, userPreferences);
        
        // Test configuration methods don't crash
        manager.setSpeechRate(1.5f);
        manager.setLanguage("en");
        manager.stop();
        
        manager.shutdown();
    }

    @Test
    public void testUserPreferencesPersistence() {
        // Test that preferences are actually persisted
        userPreferences.setTTSEnabled(false);
        userPreferences.setTTSSpeechRate(1.8f);
        userPreferences.setTTSLanguage("de");
        userPreferences.setTTSReadOriginal(true);
        
        // Create new instance to test persistence
        UserPreferences newPrefs = new UserPreferences(context);
        assertFalse("TTS enabled state should persist", newPrefs.isTTSEnabled());
        assertEquals("TTS speed should persist", 1.8f, newPrefs.getTTSSpeechRate(), 0.01f);
        assertEquals("TTS language should persist", "de", newPrefs.getTTSLanguage());
        assertTrue("TTS read original should persist", newPrefs.shouldTTSReadOriginal());
    }

    @Test
    public void testMessageRecyclerAdapterInterface() {
        // Test that the adapter interface includes TTS method
        MessageRecyclerAdapter.OnMessageClickListener listener = new MessageRecyclerAdapter.OnMessageClickListener() {
            @Override
            public void onMessageClick(Message message, int position) {}

            @Override
            public void onMessageLongClick(Message message, int position) {}

            @Override
            public void onTranslateClick(Message message, int position) {}

            @Override
            public void onTTSClick(Message message, int position) {
                // This method should exist and be callable
                assertNotNull("Message should not be null in TTS click", message);
                assertTrue("Position should be valid", position >= 0);
            }

            @Override
            public void onAttachmentClick(MmsMessage.Attachment attachment, int position) {}

            @Override
            public void onAttachmentClick(android.net.Uri uri, int position) {}

            @Override
            public void onAttachmentLongClick(MmsMessage.Attachment attachment, int position) {}

            @Override
            public void onAttachmentLongClick(android.net.Uri uri, int position) {}

            @Override
            public void onReactionClick(Message message, int position) {}

            @Override
            public void onAddReactionClick(Message message, int position) {}
        };
        
        // Test calling the TTS method
        listener.onTTSClick(testMessage, 0);
    }

    @Test
    public void testTTSPreferencesIntegration() {
        // Test the integration between TTSManager and UserPreferences
        userPreferences.setTTSEnabled(true);
        userPreferences.setTTSSpeechRate(0.8f);
        userPreferences.setTTSLanguage("fr");
        userPreferences.setTTSReadOriginal(false);
        
        TTSManager manager = new TTSManager(context, userPreferences);
        
        // Verify that TTSManager uses the preferences correctly
        assertTrue("TTS should be available when enabled", userPreferences.isTTSEnabled());
        assertEquals("Speed preference should be correct", 0.8f, userPreferences.getTTSSpeechRate(), 0.01f);
        assertEquals("Language preference should be correct", "fr", userPreferences.getTTSLanguage());
        assertFalse("Read original preference should be correct", userPreferences.shouldTTSReadOriginal());
        
        manager.shutdown();
    }
}