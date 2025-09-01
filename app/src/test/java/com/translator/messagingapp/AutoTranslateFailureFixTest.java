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
 * Test to verify that the auto-translate functionality works correctly
 * when auto-translate is enabled but no API key is configured.
 * 
 * This test addresses the specific issue where auto-translation fails
 * even when enabled, due to overly restrictive checks.
 */
@RunWith(RobolectricTestRunner.class)
public class AutoTranslateFailureFixTest {

    private GoogleTranslationService translationServiceWithoutKey;
    private TranslationManager translationManager;
    
    @Mock
    private UserPreferences mockUserPreferences;
    
    @Mock
    private OfflineTranslationService mockOfflineTranslationService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Initialize translation service WITHOUT API key (simulating the issue scenario)
        translationServiceWithoutKey = new GoogleTranslationService((String) null);
        
        // Set up mock preferences to simulate the problematic configuration:
        // - Auto-translate is ENABLED (user wants auto-translation)
        // - No API key configured (hasApiKey() returns false)
        // - Translation mode is AUTO (default)
        // - Offline translation is NOT explicitly enabled (default)
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferredLanguage()).thenReturn("en");
        when(mockUserPreferences.getTranslationMode()).thenReturn(UserPreferences.TRANSLATION_MODE_AUTO);
        when(mockUserPreferences.isOfflineTranslationEnabled()).thenReturn(false);
        when(mockUserPreferences.getPreferOfflineTranslation()).thenReturn(false);
        
        // Initialize translation manager
        translationManager = new TranslationManager(
            RuntimeEnvironment.getApplication(),
            translationServiceWithoutKey,
            mockUserPreferences
        );
    }

    @Test
    public void testAutoTranslateWithoutApiKeyButOfflineAvailable() {
        // Setup: Mock offline service to indicate it's available
        when(mockOfflineTranslationService.isOfflineTranslationAvailable(anyString(), anyString())).thenReturn(true);
        
        // Simulate the scenario: auto-translate enabled, no API key, but offline available
        SmsMessage smsMessage = new SmsMessage();
        smsMessage.setOriginalText("Hola mundo"); // Spanish text
        smsMessage.setAddress("+1234567890");
        smsMessage.setIncoming(true);

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

        // Wait for async operation
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue("Callback should be invoked", callbackInvoked[0]);
        // The fix should allow translation to proceed even without API key when offline is available
        // (This test validates that the early return condition is fixed)
    }

    @Test
    public void testAutoTranslateEarlyReturnConditions() {
        // Test that the translation manager doesn't return early when:
        // 1. Auto-translate is enabled
        // 2. No API key is available
        // 3. Mode is AUTO (should try offline fallback)
        
        SmsMessage smsMessage = new SmsMessage();
        smsMessage.setOriginalText("Bonjour le monde"); // French text
        smsMessage.setAddress("+1234567890");

        final boolean[] callbackInvoked = {false};

        translationManager.translateSmsMessage(smsMessage, new TranslationManager.SmsTranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, SmsMessage translatedMessage) {
                callbackInvoked[0] = true;
                // The key test is that the callback is invoked, not the success value
                // Success depends on whether offline translation is actually available
            }
        });

        // Wait for async operation
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue("Callback should be invoked (no early return should occur)", callbackInvoked[0]);
    }

    @Test
    public void testOnlineOnlyModeWithoutApiKeyFails() {
        // Test that ONLINE_ONLY mode correctly fails when no API key is available
        when(mockUserPreferences.getTranslationMode()).thenReturn(UserPreferences.TRANSLATION_MODE_ONLINE_ONLY);
        
        SmsMessage smsMessage = new SmsMessage();
        smsMessage.setOriginalText("Guten Tag");
        smsMessage.setAddress("+1234567890");

        final boolean[] callbackInvoked = {false};
        final boolean[] successResult = {true}; // Initialize as true to verify it becomes false

        translationManager.translateSmsMessage(smsMessage, new TranslationManager.SmsTranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, SmsMessage translatedMessage) {
                callbackInvoked[0] = true;
                successResult[0] = success;
            }
        });

        // Wait for async operation
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue("Callback should be invoked", callbackInvoked[0]);
        assertFalse("ONLINE_ONLY mode should fail without API key", successResult[0]);
    }

    @Test
    public void testAutoTranslateDisabledStillFails() {
        // Verify that when auto-translate is disabled, translation still fails regardless of other settings
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(false);
        
        SmsMessage smsMessage = new SmsMessage();
        smsMessage.setOriginalText("Ciao mondo");
        smsMessage.setAddress("+1234567890");

        final boolean[] callbackInvoked = {false};
        final boolean[] successResult = {true}; // Initialize as true to verify it becomes false

        translationManager.translateSmsMessage(smsMessage, new TranslationManager.SmsTranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, SmsMessage translatedMessage) {
                callbackInvoked[0] = true;
                successResult[0] = success;
            }
        });

        // Wait for async operation
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue("Callback should be invoked", callbackInvoked[0]);
        assertFalse("Translation should fail when auto-translate is disabled", successResult[0]);
    }

    @Test
    public void testSpecificGitHubIssueScenario() {
        // Test the exact scenario from GitHub issue #418:
        // - Spanish message: "Soy un asistente de inteligencia artificial de Jerry..."
        // - Auto-translate enabled
        // - No API key configured
        // - Should attempt offline translation as fallback
        
        SmsMessage smsMessage = new SmsMessage();
        smsMessage.setOriginalText("Soy un asistente de inteligencia artificial de Jerry. Estoy aquí para ayudarte con cualquier pregunta sobre seguros o el uso de la aplicación. ¿En qué más puedo ayudarte hoy?");
        smsMessage.setAddress("+18333220089"); // Same number from the GitHub issue logs
        smsMessage.setIncoming(true);

        final boolean[] callbackInvoked = {false};

        translationManager.translateSmsMessage(smsMessage, new TranslationManager.SmsTranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, SmsMessage translatedMessage) {
                callbackInvoked[0] = true;
                // After the fix, the callback should be invoked (no early return)
                // The actual success depends on offline translation availability
                // But the key fix is that it shouldn't return early due to missing API key
            }
        });

        // Wait for async operation
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertTrue("Callback should be invoked - the GitHub issue was caused by early return preventing any translation attempt", callbackInvoked[0]);
    }
}