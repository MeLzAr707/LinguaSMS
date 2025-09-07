package com.translator.messagingapp;

import android.content.Context;

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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test to verify the fix for issue #530: Incorrect language translation when Chinese selected
 * but translating English to English instead of English to the selected incoming language.
 */
@RunWith(RobolectricTestRunner.class)
public class IncomingLanguageTranslationTest {

    private TranslationManager translationManager;
    
    @Mock
    private GoogleTranslationService mockTranslationService;
    
    @Mock
    private UserPreferences mockUserPreferences;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        Context context = RuntimeEnvironment.getApplication();
        
        // Set up mock behavior
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferredLanguage()).thenReturn("en"); // General preference is English
        when(mockUserPreferences.getPreferredIncomingLanguage()).thenReturn("zh"); // Incoming preference is Chinese
        when(mockTranslationService.hasApiKey()).thenReturn(true);
        when(mockTranslationService.detectLanguage(anyString())).thenReturn("en");
        when(mockTranslationService.translate(anyString(), eq("en"), eq("zh"))).thenReturn("你好世界");
        
        translationManager = new TranslationManager(context, mockTranslationService, mockUserPreferences);
    }

    @Test
    public void testTranslateSmsMessageUsesIncomingLanguagePreference() {
        // Given: User has set incoming language preference to Chinese (zh)
        // And general language preference is English (en)
        SmsMessage message = new SmsMessage("1234567890", "Hello world", new Date());
        message.setIncoming(true);
        
        final boolean[] callbackCalled = {false};
        final boolean[] translationSuccessful = {false};
        final String[] targetLanguageUsed = {null};
        
        // When: Translating an incoming SMS message
        translationManager.translateSmsMessage(message, new TranslationManager.SmsTranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, SmsMessage translatedMessage) {
                callbackCalled[0] = true;
                translationSuccessful[0] = success;
                if (success && translatedMessage != null) {
                    targetLanguageUsed[0] = translatedMessage.getTranslatedLanguage();
                }
            }
        });
        
        // Wait for async translation to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then: Should use the incoming language preference (Chinese) not general preference (English)
        verify(mockUserPreferences, atLeastOnce()).getPreferredIncomingLanguage();
        verify(mockTranslationService).translate("Hello world", "en", "zh");
        
        assertTrue("Callback should be called", callbackCalled[0]);
        assertTrue("Translation should be successful", translationSuccessful[0]);
        assertEquals("Should translate to Chinese (incoming language preference)", "zh", targetLanguageUsed[0]);
    }

    @Test
    public void testTranslateSmsMessageDoesNotUseGeneralLanguagePreference() {
        // Given: User has set incoming language preference to Chinese (zh)
        // And general language preference is English (en)
        SmsMessage message = new SmsMessage("1234567890", "Hello world", new Date());
        message.setIncoming(true);
        
        // When: Translating an incoming SMS message
        translationManager.translateSmsMessage(message, new TranslationManager.SmsTranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, SmsMessage translatedMessage) {
                // Callback implementation for test
            }
        });
        
        // Wait for async translation to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then: Should NOT attempt to translate English to English (which would happen with general preference)
        verify(mockTranslationService, never()).translate("Hello world", "en", "en");
        // Should translate to Chinese instead
        verify(mockTranslationService).translate("Hello world", "en", "zh");
    }

    @Test
    public void testIncomingLanguagePreferenceFallsBackToGeneralPreference() {
        // Given: User has NOT set a specific incoming language preference
        // So it should fall back to general preference
        when(mockUserPreferences.getPreferredIncomingLanguage()).thenReturn("es"); // Falls back to Spanish general preference
        when(mockUserPreferences.getPreferredLanguage()).thenReturn("es");
        when(mockTranslationService.translate(anyString(), eq("en"), eq("es"))).thenReturn("Hola mundo");
        
        SmsMessage message = new SmsMessage("1234567890", "Hello world", new Date());
        message.setIncoming(true);
        
        // When: Translating an incoming SMS message
        translationManager.translateSmsMessage(message, new TranslationManager.SmsTranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, SmsMessage translatedMessage) {
                // Callback implementation for test
            }
        });
        
        // Wait for async translation to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then: Should use the fallback language (Spanish)
        verify(mockTranslationService).translate("Hello world", "en", "es");
    }
}