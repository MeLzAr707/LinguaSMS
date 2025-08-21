package com.translator.messagingapp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration test to verify the automatic translation fix works with TranslationManager.
 * This test ensures the end-to-end flow from incoming SMS to translation completion.
 */
@RunWith(RobolectricTestRunner.class)
public class IncomingMessageTranslationIntegrationTest {

    private MessageService messageService;
    private TranslationManager translationManager;
    
    @Mock
    private GoogleTranslationService mockTranslationService;
    
    @Mock
    private UserPreferences mockUserPreferences;
    
    @Mock
    private TranslationCache mockTranslationCache;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create real TranslationManager with mocked dependencies
        translationManager = new TranslationManager(
            RuntimeEnvironment.getApplication(),
            mockTranslationService,
            mockUserPreferences
        );
        
        // Create MessageService with real TranslationManager
        messageService = new MessageService(
            RuntimeEnvironment.getApplication(),
            translationManager,
            mockTranslationCache
        );
        
        // Set up mock preferences for successful translation
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferredLanguage()).thenReturn("es");
        when(mockUserPreferences.getPreferredIncomingLanguage()).thenReturn("es");
        
        // Set up mock translation service
        when(mockTranslationService.hasApiKey()).thenReturn(true);
        when(mockTranslationService.detectLanguage("Hello world")).thenReturn("en");
        when(mockTranslationService.translate("Hello world", "en", "es")).thenReturn("Hola mundo");
    }

    @Test
    public void testEndToEndTranslationIntegration() {
        // Given: Auto-translate is enabled and translation service is ready
        assertTrue("Auto-translate should be enabled", mockUserPreferences.isAutoTranslateEnabled());
        assertTrue("Translation service should have API key", mockTranslationService.hasApiKey());
        
        // Create SmsMessage for translation
        SmsMessage smsMessage = new SmsMessage("1234567890", "Hello world", new Date());
        smsMessage.setIncoming(true);
        
        final boolean[] callbackInvoked = {false};
        final boolean[] translationSuccessful = {false};
        final SmsMessage[] resultMessage = {null};
        
        // When: Translate the SMS message using TranslationManager
        translationManager.translateSmsMessage(smsMessage, new TranslationManager.SmsTranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, SmsMessage translatedMessage) {
                callbackInvoked[0] = true;
                translationSuccessful[0] = success;
                resultMessage[0] = translatedMessage;
            }
        });
        
        // Wait for async operation
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        // Then: Translation should be successful
        assertTrue("Callback should be invoked", callbackInvoked[0]);
        assertTrue("Translation should be successful", translationSuccessful[0]);
        assertNotNull("Result message should not be null", resultMessage[0]);
        assertTrue("Message should be translated", resultMessage[0].isTranslated());
        assertEquals("Translated text should be correct", "Hola mundo", resultMessage[0].getTranslatedText());
        assertEquals("Original text should be preserved", "Hello world", resultMessage[0].getOriginalText());
    }

    @Test
    public void testTranslationDisabledIntegration() {
        // Given: Auto-translate is disabled
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(false);
        
        // Create SmsMessage for translation
        SmsMessage smsMessage = new SmsMessage("1234567890", "Hello world", new Date());
        smsMessage.setIncoming(true);
        
        final boolean[] callbackInvoked = {false};
        final boolean[] translationSuccessful = {true}; // Start as true to verify it becomes false
        
        // When: Attempt to translate the SMS message
        translationManager.translateSmsMessage(smsMessage, new TranslationManager.SmsTranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, SmsMessage translatedMessage) {
                callbackInvoked[0] = true;
                translationSuccessful[0] = success;
            }
        });
        
        // Wait for async operation
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        // Then: Translation should fail because auto-translate is disabled
        assertTrue("Callback should be invoked", callbackInvoked[0]);
        assertFalse("Translation should fail when auto-translate is disabled", translationSuccessful[0]);
    }

    @Test
    public void testTranslationServiceUnavailableIntegration() {
        // Given: Translation service is unavailable (no API key)
        when(mockTranslationService.hasApiKey()).thenReturn(false);
        
        // Create SmsMessage for translation
        SmsMessage smsMessage = new SmsMessage("1234567890", "Hello world", new Date());
        smsMessage.setIncoming(true);
        
        final boolean[] callbackInvoked = {false};
        final boolean[] translationSuccessful = {true}; // Start as true to verify it becomes false
        
        // When: Attempt to translate the SMS message
        translationManager.translateSmsMessage(smsMessage, new TranslationManager.SmsTranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, SmsMessage translatedMessage) {
                callbackInvoked[0] = true;
                translationSuccessful[0] = success;
            }
        });
        
        // Wait for async operation
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        // Then: Translation should fail because service is unavailable
        assertTrue("Callback should be invoked", callbackInvoked[0]);
        assertFalse("Translation should fail when service is unavailable", translationSuccessful[0]);
    }

    @Test
    public void testSameLanguageSkipsTranslation() {
        // Given: Message is already in target language
        when(mockTranslationService.detectLanguage("Hola mundo")).thenReturn("es");
        
        // Create SmsMessage in Spanish (same as target language)
        SmsMessage smsMessage = new SmsMessage("1234567890", "Hola mundo", new Date());
        smsMessage.setIncoming(true);
        
        final boolean[] callbackInvoked = {false};
        final boolean[] translationSuccessful = {true}; // Start as true to verify it becomes false
        
        // When: Attempt to translate the SMS message
        translationManager.translateSmsMessage(smsMessage, new TranslationManager.SmsTranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, SmsMessage translatedMessage) {
                callbackInvoked[0] = true;
                translationSuccessful[0] = success;
            }
        });
        
        // Wait for async operation
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        // Then: Translation should be skipped (returns success but no translation)
        assertTrue("Callback should be invoked", callbackInvoked[0]);
        // Note: TranslationManager may return success=true for same language (depends on implementation)
        // The key is that no actual translation service call should be made
        verify(mockTranslationService, never()).translate(eq("Hola mundo"), eq("es"), eq("es"));
    }
}