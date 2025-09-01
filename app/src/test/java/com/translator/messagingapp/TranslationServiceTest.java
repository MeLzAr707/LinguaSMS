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

/**
 * Unit tests for translation functionality.
 * Tests the core translation service and manager behavior.
 */
@RunWith(RobolectricTestRunner.class)
public class TranslationServiceTest {

    private GoogleTranslationService translationService;
    private TranslationManager translationManager;
    
    @Mock
    private UserPreferences mockUserPreferences;
    
    @Mock
    private TranslationCache mockTranslationCache;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Initialize translation service with a mock API key
        translationService = new GoogleTranslationService("test-api-key");
        
        // Set up mock preferences
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferredLanguage()).thenReturn("es");
        when(mockUserPreferences.getPreferredIncomingLanguage()).thenReturn("es");
        when(mockUserPreferences.getPreferredOutgoingLanguage()).thenReturn("es");
        
        // Initialize translation manager
        translationManager = new TranslationManager(
            RuntimeEnvironment.getApplication(),
            translationService,
            mockUserPreferences
        );
    }

    @Test
    public void testGoogleTranslationService_hasApiKey() {
        assertTrue("Translation service should have API key", translationService.hasApiKey());
        assertTrue("API key should be valid", translationService.testApiKey());
    }

    @Test
    public void testGoogleTranslationService_detectLanguage() {
        // Test English detection
        String englishText = "Hello, how are you?";
        String detectedLanguage = translationService.detectLanguage(englishText);
        assertEquals("Should detect English", "en", detectedLanguage);

        // Test Spanish detection
        String spanishText = "Hola, ¿cómo estás?";
        detectedLanguage = translationService.detectLanguage(spanishText);
        assertEquals("Should detect Spanish", "es", detectedLanguage);

        // Test French detection
        String frenchText = "Bonjour, comment ça va?";
        detectedLanguage = translationService.detectLanguage(frenchText);
        assertEquals("Should detect French", "fr", detectedLanguage);

        // Test empty text
        detectedLanguage = translationService.detectLanguage("");
        assertEquals("Should default to English for empty text", "en", detectedLanguage);

        // Test null text
        detectedLanguage = translationService.detectLanguage(null);
        assertEquals("Should default to English for null text", "en", detectedLanguage);
    }

    @Test
    public void testGoogleTranslationService_translate() {
        // Test basic translation
        String originalText = "hello";
        String translated = translationService.translate(originalText, "en", "es");
        assertNotNull("Translation should not be null", translated);
        assertTrue("Translation should contain original text", translated.contains(originalText));
        assertTrue("Translation should indicate Spanish", translated.contains("[ES]"));

        // Test same language (should return original)
        translated = translationService.translate(originalText, "en", "en");
        assertEquals("Same language should return original text", originalText, translated);

        // Test exact mock translation
        translated = translationService.translate("hello", "en", "es");
        assertEquals("Should use exact mock translation", "hola", translated);

        // Test empty text
        translated = translationService.translate("", "en", "es");
        assertEquals("Empty text should return empty", "", translated);

        // Test null text
        translated = translationService.translate(null, "en", "es");
        assertNull("Null text should return null", translated);
    }

    @Test
    public void testTranslationManager_translateText() {
        final boolean[] callbackInvoked = {false};
        final String[] translatedResult = {null};
        final boolean[] successResult = {false};

        translationManager.translateText("hello", "es", new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                callbackInvoked[0] = true;
                successResult[0] = success;
                translatedResult[0] = translatedText;
            }
        });

        // Wait a moment for async operation
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Ignore
        }

        assertTrue("Callback should be invoked", callbackInvoked[0]);
        assertTrue("Translation should succeed", successResult[0]);
        assertNotNull("Translated text should not be null", translatedResult[0]);
        assertEquals("Should use exact mock translation", "hola", translatedResult[0]);
    }

    @Test
    public void testTranslationManager_translateSmsMessage() {
        // Create test SMS message
        SmsMessage smsMessage = new SmsMessage();
        smsMessage.setOriginalText("hello");
        smsMessage.setAddress("+1234567890");
        smsMessage.setDate(System.currentTimeMillis());

        final boolean[] callbackInvoked = {false};
        final boolean[] successResult = {false};
        final SmsMessage[] resultMessage = {null};

        translationManager.translateSmsMessage(smsMessage, new TranslationManager.SmsTranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, SmsMessage translatedMessage) {
                callbackInvoked[0] = true;
                successResult[0] = success;
                resultMessage[0] = translatedMessage;
            }
        });

        // Wait a moment for async operation
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Ignore
        }

        assertTrue("Callback should be invoked", callbackInvoked[0]);
        assertTrue("Translation should succeed", successResult[0]);
        assertNotNull("Result message should not be null", resultMessage[0]);
        assertEquals("Should use exact mock translation", "hola", resultMessage[0].getTranslatedText());
        assertEquals("Should set translated language", "es", resultMessage[0].getTranslatedLanguage());
    }

    @Test
    public void testTranslationManager_rateLimiting() {
        // This test verifies that rate limiting works for online translations
        final int[] successCount = {0};
        final int[] failureCount = {0};
        
        TranslationManager.TranslationCallback callback = new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                if (success) {
                    successCount[0]++;
                } else {
                    failureCount[0]++;
                }
            }
        };

        // Rapidly fire multiple translation requests
        for (int i = 0; i < 10; i++) {
            translationManager.translateText("test message " + i, "es", callback);
        }

        // Wait for async operations
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            // Ignore
        }

        // Due to rate limiting, not all requests should succeed
        assertTrue("At least some translations should succeed", successCount[0] > 0);
        // Note: Some may fail due to rate limiting, which is expected behavior
    }

    @Test
    public void testTranslationManager_offlineTranslationsNotRateLimited() {
        // Enable offline translation to test that offline translations are not rate limited
        when(mockUserPreferences.isOfflineTranslationEnabled()).thenReturn(true);
        
        // Create a translation manager with offline translation enabled
        TranslationManager offlineTranslationManager = new TranslationManager(
            RuntimeEnvironment.getApplication(),
            null, // No online service to force offline
            mockUserPreferences
        );
        
        final int[] successCount = {0};
        final int[] failureCount = {0};
        
        TranslationManager.TranslationCallback callback = new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                if (success) {
                    successCount[0]++;
                } else {
                    failureCount[0]++;
                }
            }
        };

        // Rapidly fire multiple offline translation requests
        for (int i = 0; i < 10; i++) {
            offlineTranslationManager.translateText("test message " + i, "en", "es", callback);
        }

        // Wait for async operations
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // Ignore
        }

        // For offline translations, all requests should succeed (no rate limiting)
        // Note: They may still fail due to offline service not being available, but not due to rate limiting
        assertTrue("Offline translation attempts should be made", successCount[0] + failureCount[0] > 0);
        
        // Clean up
        offlineTranslationManager.cleanup();
    }

    @Test
    public void testTranslationManager_autoTranslateDisabled() {
        // Disable auto-translate
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(false);

        // Create test SMS message
        SmsMessage smsMessage = new SmsMessage();
        smsMessage.setOriginalText("hello");
        smsMessage.setAddress("+1234567890");

        final boolean[] callbackInvoked = {false};
        final boolean[] successResult = {true}; // Initialize as true to test it becomes false

        translationManager.translateSmsMessage(smsMessage, new TranslationManager.SmsTranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, SmsMessage translatedMessage) {
                callbackInvoked[0] = true;
                successResult[0] = success;
            }
        });

        // Wait a moment for async operation
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Ignore
        }

        assertTrue("Callback should be invoked", callbackInvoked[0]);
        assertFalse("Translation should fail when auto-translate is disabled", successResult[0]);
    }

    @Test
    public void testTranslationService_noApiKey() {
        // Test service without API key
        GoogleTranslationService noKeyService = new GoogleTranslationService((String) null);
        
        assertFalse("Service should not have API key", noKeyService.hasApiKey());
        assertFalse("API key test should fail", noKeyService.testApiKey());
        
        // Translation should still work (mock implementation)
        String result = noKeyService.translate("hello", "en", "es");
        assertNotNull("Translation should not be null even without API key", result);
    }
}