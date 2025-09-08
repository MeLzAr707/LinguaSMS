package com.translator.messagingapp;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Focused test for auto-translate language detection and conditional translation.
 * Tests the specific requirement from issue #546: automatically translate incoming messages
 * based on user preference, but skip translation if already in preferred language.
 */
@RunWith(RobolectricTestRunner.class)
public class AutoTranslateLanguageDetectionTest {

    private TranslationManager translationManager;
    
    @Mock
    private GoogleTranslationService mockTranslationService;
    
    @Mock
    private UserPreferences mockUserPreferences;
    
    @Mock
    private TranslationCache mockTranslationCache;
    
    @Mock
    private OfflineTranslationService mockOfflineTranslationService;
    
    @Mock
    private LanguageDetectionService mockLanguageDetectionService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        Context context = RuntimeEnvironment.getApplication();
        
        // Set up default mock behavior
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferredIncomingLanguage()).thenReturn("es"); // Spanish
        when(mockUserPreferences.getTranslationMode()).thenReturn(UserPreferences.TRANSLATION_MODE_AUTO);
        when(mockUserPreferences.isOfflineTranslationEnabled()).thenReturn(true);
        when(mockTranslationService.hasApiKey()).thenReturn(true);
        when(mockTranslationCache.get(anyString())).thenReturn(null); // No cache hits
        
        translationManager = new TranslationManager(context, mockTranslationService, mockUserPreferences,
                mockTranslationCache, mockOfflineTranslationService, mockLanguageDetectionService);
    }

    @Test
    public void testAutoTranslateSkipsWhenMessageAlreadyInPreferredLanguage() {
        // Given: Message is already in Spanish (preferred incoming language)
        when(mockLanguageDetectionService.detectLanguageSync("Hola mundo")).thenReturn("es");
        when(mockUserPreferences.getTranslationMode()).thenReturn(UserPreferences.TRANSLATION_MODE_AUTO);
        when(mockUserPreferences.isOfflineTranslationEnabled()).thenReturn(true);
        when(mockOfflineTranslationService.isLanguagePairSupported("es", "es")).thenReturn(true);
        
        SmsMessage message = new SmsMessage("1234567890", "Hola mundo", new Date());
        message.setIncoming(true);
        
        final boolean[] callbackCalled = {false};
        final boolean[] translationSuccessful = {true}; // Start as true to verify it becomes false
        
        // When: Auto-translate attempts to translate the message
        translationManager.translateSmsMessage(message, new TranslationManager.SmsTranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, SmsMessage translatedMessage) {
                callbackCalled[0] = true;
                translationSuccessful[0] = success;
            }
        });
        
        // Wait for async operation
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then: Translation should be skipped
        assertTrue("Callback should be called", callbackCalled[0]);
        assertFalse("Translation should be skipped when message is already in preferred language", translationSuccessful[0]);
        
        // Verify offline language detection was performed
        verify(mockLanguageDetectionService).detectLanguageSync("Hola mundo");
        
        // Verify no translation calls were made to either service
        verify(mockOfflineTranslationService, never()).translateText(anyString(), anyString(), anyString(), any());
        verify(mockTranslationService, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    public void testAutoTranslatePerformsTranslationWhenLanguagesDiffer() {
        // Given: Message is in English, but preferred incoming language is Spanish
        when(mockLanguageDetectionService.detectLanguageSync("Hello world")).thenReturn("en");
        when(mockUserPreferences.getTranslationMode()).thenReturn(UserPreferences.TRANSLATION_MODE_AUTO);
        when(mockUserPreferences.isOfflineTranslationEnabled()).thenReturn(true);
        when(mockOfflineTranslationService.isLanguagePairSupported("en", "es")).thenReturn(true);
        when(mockOfflineTranslationService.areModelsAvailable("en", "es")).thenReturn(true);
        
        SmsMessage message = new SmsMessage("1234567890", "Hello world", new Date());
        message.setIncoming(true);
        
        final boolean[] callbackCalled = {false};
        final boolean[] translationSuccessful = {false};
        final SmsMessage[] translatedMessage = {null};
        
        // Mock successful offline translation
        doAnswer(invocation -> {
            OfflineTranslationService.TranslationCallback callback = invocation.getArgument(3);
            callback.onTranslationComplete(true, "Hola mundo", null);
            return null;
        }).when(mockOfflineTranslationService).translateText(eq("Hello world"), eq("en"), eq("es"), any());
        
        // When: Auto-translate attempts to translate the message
        translationManager.translateSmsMessage(message, new TranslationManager.SmsTranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, SmsMessage result) {
                callbackCalled[0] = true;
                translationSuccessful[0] = success;
                translatedMessage[0] = result;
            }
        });
        
        // Wait for async operation
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then: Translation should be performed
        assertTrue("Callback should be called", callbackCalled[0]);
        assertTrue("Translation should be successful when languages differ", translationSuccessful[0]);
        assertNotNull("Translated message should not be null", translatedMessage[0]);
        
        // Verify offline language detection was performed
        verify(mockLanguageDetectionService).detectLanguageSync("Hello world");
        
        // Verify offline translation was called with correct parameters
        verify(mockOfflineTranslationService).translateText("Hello world", "en", "es", any());
        
        // Verify the translated message contains the expected content
        assertEquals("Translated text should be correct", "Hola mundo", translatedMessage[0].getTranslatedText());
        assertEquals("Translated language should be correct", "es", translatedMessage[0].getTranslatedLanguage());
        assertEquals("Original language should be correct", "en", translatedMessage[0].getOriginalLanguage());
    }

    @Test
    public void testAutoTranslateHandlesLanguageVariants() {
        // Given: Message is in Spanish (Spain) but preferred language is Spanish (general)
        // This should still be considered the same language and skip translation
        when(mockLanguageDetectionService.detectLanguageSync("Hola mundo")).thenReturn("es-ES"); // Spanish (Spain)
        when(mockUserPreferences.getTranslationMode()).thenReturn(UserPreferences.TRANSLATION_MODE_AUTO);
        when(mockUserPreferences.isOfflineTranslationEnabled()).thenReturn(true);
        // Preferred language is "es" (general Spanish)
        
        SmsMessage message = new SmsMessage("1234567890", "Hola mundo", new Date());
        message.setIncoming(true);
        
        final boolean[] callbackCalled = {false};
        final boolean[] translationSuccessful = {true}; // Start as true to verify it becomes false
        
        // When: Auto-translate attempts to translate the message
        translationManager.translateSmsMessage(message, new TranslationManager.SmsTranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, SmsMessage translatedMessage) {
                callbackCalled[0] = true;
                translationSuccessful[0] = success;
            }
        });
        
        // Wait for async operation
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then: Translation should be skipped (base languages match: es-ES vs es)
        assertTrue("Callback should be called", callbackCalled[0]);
        assertFalse("Translation should be skipped for language variants (es-ES vs es)", translationSuccessful[0]);
        
        // Verify offline language detection was performed
        verify(mockLanguageDetectionService).detectLanguageSync("Hola mundo");
        
        // Verify no translation calls were made to either service
        verify(mockOfflineTranslationService, never()).translateText(anyString(), anyString(), anyString(), any());
        verify(mockTranslationService, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    public void testAutoTranslateSkipsWhenDisabled() {
        // Given: Auto-translate is disabled
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(false);
        when(mockTranslationService.detectLanguage("Hello world")).thenReturn("en");
        
        SmsMessage message = new SmsMessage("1234567890", "Hello world", new Date());
        message.setIncoming(true);
        
        final boolean[] callbackCalled = {false};
        final boolean[] translationSuccessful = {true}; // Start as true to verify it becomes false
        
        // When: Auto-translate attempts to translate the message
        translationManager.translateSmsMessage(message, new TranslationManager.SmsTranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, SmsMessage translatedMessage) {
                callbackCalled[0] = true;
                translationSuccessful[0] = success;
            }
        });
        
        // Wait for async operation (though it should complete synchronously)
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then: Translation should be skipped
        assertTrue("Callback should be called", callbackCalled[0]);
        assertFalse("Translation should be skipped when auto-translate is disabled", translationSuccessful[0]);
        
        // Verify no language detection or translation calls were made to either service
        verify(mockLanguageDetectionService, never()).detectLanguageSync(anyString());
        verify(mockOfflineTranslationService, never()).translateText(anyString(), anyString(), anyString(), any());
        verify(mockTranslationService, never()).detectLanguage(anyString());
        verify(mockTranslationService, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    public void testAutoTranslateHandlesNullTargetLanguage() {
        // Given: Target language is null (should not happen in practice due to fallbacks)
        when(mockUserPreferences.getPreferredIncomingLanguage()).thenReturn(null);
        
        SmsMessage message = new SmsMessage("1234567890", "Hello world", new Date());
        message.setIncoming(true);
        
        final boolean[] callbackCalled = {false};
        final boolean[] translationSuccessful = {true}; // Start as true to verify it becomes false
        
        // When: Auto-translate attempts to translate the message
        translationManager.translateSmsMessage(message, new TranslationManager.SmsTranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, SmsMessage translatedMessage) {
                callbackCalled[0] = true;
                translationSuccessful[0] = success;
            }
        });
        
        // Wait for async operation (though it should complete synchronously)
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then: Translation should be skipped safely
        assertTrue("Callback should be called", callbackCalled[0]);
        assertFalse("Translation should be skipped when target language is null", translationSuccessful[0]);
        
        // Verify no language detection or translation calls were made to either service
        verify(mockLanguageDetectionService, never()).detectLanguageSync(anyString());
        verify(mockOfflineTranslationService, never()).translateText(anyString(), anyString(), anyString(), any());
        verify(mockTranslationService, never()).detectLanguage(anyString());
        verify(mockTranslationService, never()).translate(anyString(), anyString(), anyString());
    }

    @Test
    public void testAutoTranslateOfflineWithOnlineFallback() {
        // Given: Offline translation fails, should fallback to online
        when(mockLanguageDetectionService.detectLanguageSync("Hello world")).thenReturn("en");
        when(mockUserPreferences.getTranslationMode()).thenReturn(UserPreferences.TRANSLATION_MODE_AUTO);
        when(mockUserPreferences.isOfflineTranslationEnabled()).thenReturn(true);
        when(mockOfflineTranslationService.isLanguagePairSupported("en", "es")).thenReturn(true);
        when(mockOfflineTranslationService.areModelsAvailable("en", "es")).thenReturn(false);
        when(mockTranslationService.translate("Hello world", "en", "es")).thenReturn("Hola mundo");
        
        SmsMessage message = new SmsMessage("1234567890", "Hello world", new Date());
        message.setIncoming(true);
        
        final boolean[] callbackCalled = {false};
        final boolean[] translationSuccessful = {false};
        final SmsMessage[] translatedMessage = {null};
        
        // Mock offline translation failure
        doAnswer(invocation -> {
            OfflineTranslationService.TranslationCallback callback = invocation.getArgument(3);
            callback.onTranslationComplete(false, null, "Language models not downloaded");
            return null;
        }).when(mockOfflineTranslationService).translateText(eq("Hello world"), eq("en"), eq("es"), any());
        
        // When: Auto-translate attempts to translate the message
        translationManager.translateSmsMessage(message, new TranslationManager.SmsTranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, SmsMessage result) {
                callbackCalled[0] = true;
                translationSuccessful[0] = success;
                translatedMessage[0] = result;
            }
        });
        
        // Wait for async operation
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then: Translation should be successful via online fallback
        assertTrue("Callback should be called", callbackCalled[0]);
        assertTrue("Translation should be successful via online fallback", translationSuccessful[0]);
        assertNotNull("Translated message should not be null", translatedMessage[0]);
        
        // Verify offline language detection was performed
        verify(mockLanguageDetectionService).detectLanguageSync("Hello world");
        
        // Verify offline translation was attempted first
        verify(mockOfflineTranslationService).translateText("Hello world", "en", "es", any());
        
        // Verify online translation was used as fallback
        verify(mockTranslationService).translate("Hello world", "en", "es");
        
        // Verify the translated message contains the expected content
        assertEquals("Translated text should be correct", "Hola mundo", translatedMessage[0].getTranslatedText());
        assertEquals("Translated language should be correct", "es", translatedMessage[0].getTranslatedLanguage());
        assertEquals("Original language should be correct", "en", translatedMessage[0].getOriginalLanguage());
    }
}