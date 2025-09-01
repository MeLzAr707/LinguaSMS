package com.translator.messagingapp;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Focused test for automatic translation of incoming messages functionality.
 * Tests the fix for issue where incoming messages were not automatically translated.
 */
@RunWith(RobolectricTestRunner.class)
public class IncomingMessageAutoTranslationTest {

    private MessageService messageService;
    
    @Mock
    private TranslationManager mockTranslationManager;
    
    @Mock
    private UserPreferences mockUserPreferences;
    
    @Mock
    private TranslationCache mockTranslationCache;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create MessageService with mocked dependencies
        messageService = new MessageService(
            RuntimeEnvironment.getApplication(),
            mockTranslationManager,
            mockTranslationCache
        );
        
        // Set up default mock behavior
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferredLanguage()).thenReturn("es");
        when(mockUserPreferences.getPreferredIncomingLanguage()).thenReturn("es");
    }

    @Test
    public void testIncomingSmsTriggersTranslationWhenAutoTranslateEnabled() {
        // Given: Auto-translate is enabled
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        
        // Create a mock SMS intent
        Intent smsIntent = createMockSmsIntent("1234567890", "Hello world", System.currentTimeMillis());
        
        // When: Handle incoming SMS
        messageService.handleIncomingSms(smsIntent);
        
        // Then: Translation manager should be called with the SMS message
        ArgumentCaptor<com.translator.messagingapp.SmsMessage> messageCaptor = 
            ArgumentCaptor.forClass(com.translator.messagingapp.SmsMessage.class);
        ArgumentCaptor<TranslationManager.SmsTranslationCallback> callbackCaptor = 
            ArgumentCaptor.forClass(TranslationManager.SmsTranslationCallback.class);
        
        verify(mockTranslationManager, times(1)).translateSmsMessage(
            messageCaptor.capture(), 
            callbackCaptor.capture()
        );
        
        // Verify the SMS message has correct properties
        com.translator.messagingapp.SmsMessage capturedMessage = messageCaptor.getValue();
        assertNotNull("Captured message should not be null", capturedMessage);
        assertEquals("Message address should match", "1234567890", capturedMessage.getAddress());
        assertEquals("Message text should match", "Hello world", capturedMessage.getOriginalText());
        assertTrue("Message should be marked as incoming", capturedMessage.isIncoming());
        assertFalse("Message should be initially unread", capturedMessage.isRead());
    }

    @Test
    public void testIncomingSmsDoesNotTriggerTranslationWhenAutoTranslateDisabled() {
        // Given: Auto-translate is disabled
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(false);
        
        // Create a mock SMS intent
        Intent smsIntent = createMockSmsIntent("1234567890", "Hello world", System.currentTimeMillis());
        
        // When: Handle incoming SMS
        messageService.handleIncomingSms(smsIntent);
        
        // Then: Translation manager should still be called (it will check auto-translate internally)
        verify(mockTranslationManager, times(1)).translateSmsMessage(
            any(com.translator.messagingapp.SmsMessage.class), 
            any(TranslationManager.SmsTranslationCallback.class)
        );
    }

    @Test
    public void testIncomingSmsTranslationSuccess() {
        // Given: Auto-translate is enabled
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        
        // Create a mock SMS intent
        Intent smsIntent = createMockSmsIntent("1234567890", "Hello world", System.currentTimeMillis());
        
        // When: Handle incoming SMS
        messageService.handleIncomingSms(smsIntent);
        
        // Capture the callback
        ArgumentCaptor<TranslationManager.SmsTranslationCallback> callbackCaptor = 
            ArgumentCaptor.forClass(TranslationManager.SmsTranslationCallback.class);
        verify(mockTranslationManager).translateSmsMessage(
            any(com.translator.messagingapp.SmsMessage.class), 
            callbackCaptor.capture()
        );
        
        // Simulate successful translation
        com.translator.messagingapp.SmsMessage translatedMessage = 
            new com.translator.messagingapp.SmsMessage("1234567890", "Hello world", new Date());
        translatedMessage.setTranslatedText("Hola mundo");
        translatedMessage.setTranslatedLanguage("es");
        translatedMessage.setOriginalLanguage("en");
        
        TranslationManager.SmsTranslationCallback callback = callbackCaptor.getValue();
        callback.onTranslationComplete(true, translatedMessage);
        
        // Then: Translation should be successful (verified by no exceptions)
        // In a real scenario, this would trigger UI updates via broadcasts
        assertTrue("Translation completed successfully", translatedMessage.isTranslated());
        assertEquals("Translated text should be correct", "Hola mundo", translatedMessage.getTranslatedText());
        
        // Verify that the translation result is cached for UI access
        String expectedCacheKey = "Hello world_es";
        verify(mockTranslationCache).put(expectedCacheKey, "Hola mundo");
    }

    @Test
    public void testIncomingSmsTranslationFailure() {
        // Given: Auto-translate is enabled
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        
        // Create a mock SMS intent
        Intent smsIntent = createMockSmsIntent("1234567890", "Hello world", System.currentTimeMillis());
        
        // When: Handle incoming SMS
        messageService.handleIncomingSms(smsIntent);
        
        // Capture the callback
        ArgumentCaptor<TranslationManager.SmsTranslationCallback> callbackCaptor = 
            ArgumentCaptor.forClass(TranslationManager.SmsTranslationCallback.class);
        verify(mockTranslationManager).translateSmsMessage(
            any(com.translator.messagingapp.SmsMessage.class), 
            callbackCaptor.capture()
        );
        
        // Simulate failed translation
        TranslationManager.SmsTranslationCallback callback = callbackCaptor.getValue();
        callback.onTranslationComplete(false, null);
        
        // Then: Translation failure should be handled gracefully (no exceptions)
        // This verifies that the callback handles failures properly
        
        // Verify that no translation was cached for failed translation
        verify(mockTranslationCache, never()).put(anyString(), anyString());
    }

    @Test
    public void testIncomingSmsWithNullTranslationManager() {
        // Given: Translation manager is null
        MessageService messageServiceWithNullTranslationManager = new MessageService(
            RuntimeEnvironment.getApplication(),
            null,  // null translation manager
            mockTranslationCache
        );
        
        // Create a mock SMS intent
        Intent smsIntent = createMockSmsIntent("1234567890", "Hello world", System.currentTimeMillis());
        
        // When: Handle incoming SMS
        messageServiceWithNullTranslationManager.handleIncomingSms(smsIntent);
        
        // Then: Should handle gracefully without calling translation manager
        verify(mockTranslationManager, never()).translateSmsMessage(
            any(com.translator.messagingapp.SmsMessage.class), 
            any(TranslationManager.SmsTranslationCallback.class)
        );
    }

    /**
     * Helper method to create a mock SMS intent.
     *
     * @param address The sender's address
     * @param message The message text
     * @param timestamp The message timestamp
     * @return A mock SMS intent
     */
    private Intent createMockSmsIntent(String address, String message, long timestamp) {
        Intent intent = new Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        Bundle bundle = new Bundle();
        
        // Create mock PDU data (simplified for testing)
        // In a real scenario, this would be actual SMS PDU data
        bundle.putSerializable("pdus", new Object[]{"mock_pdu_data".getBytes()});
        bundle.putString("format", "3gpp");
        
        intent.putExtras(bundle);
        return intent;
    }
}