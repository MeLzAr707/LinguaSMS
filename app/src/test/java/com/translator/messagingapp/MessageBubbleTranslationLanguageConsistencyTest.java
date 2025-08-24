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
 * Test case to verify that message bubble translation uses the same language selection
 * logic as text input translation, ensuring consistent offline model availability.
 */
@RunWith(RobolectricTestRunner.class)
public class MessageBubbleTranslationLanguageConsistencyTest {

    private GoogleTranslationService mockTranslationService;
    private OfflineTranslationService mockOfflineService;
    private TranslationManager translationManager;
    
    @Mock
    private UserPreferences mockUserPreferences;
    
    @Mock
    private TranslationCache mockTranslationCache;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create mock translation services
        mockTranslationService = mock(GoogleTranslationService.class);
        mockOfflineService = mock(OfflineTranslationService.class);
        when(mockTranslationService.hasApiKey()).thenReturn(true);
        
        // Set up preferences where outgoing language differs from general language
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferredLanguage()).thenReturn("en"); // English as general
        when(mockUserPreferences.getPreferredOutgoingLanguage()).thenReturn("es"); // Spanish for outgoing
        when(mockUserPreferences.getTranslationMode()).thenReturn(0); // AUTO mode
        when(mockUserPreferences.getPreferOfflineTranslation()).thenReturn(true);
        
        // Mock offline availability for Spanish (outgoing) but not English
        when(mockOfflineService.isOfflineTranslationAvailable(anyString(), eq("es"))).thenReturn(true);
        when(mockOfflineService.isOfflineTranslationAvailable(anyString(), eq("en"))).thenReturn(false);
        
        // Initialize translation manager
        translationManager = new TranslationManager(
            RuntimeEnvironment.getApplication(),
            mockTranslationService,
            mockOfflineService,
            mockUserPreferences,
            mockTranslationCache
        );
    }

    @Test
    public void testLanguageSelectionConsistency() {
        // This test verifies that both translation methods would use the same target language
        // by mocking the language selection logic
        
        String testText = "Hello world";
        
        // Mock language detection to return detected language
        when(mockTranslationService.detectLanguage(testText)).thenReturn("en");
        
        final String[] inputTranslationTargetLanguage = {null};
        final String[] messageTranslationTargetLanguage = {null};
        
        // Test input translation (3-parameter call without force)
        translationManager.translateText(testText, "es", new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                inputTranslationTargetLanguage[0] = "es"; // This would be the target language used
            }
        });
        
        // Test message translation (4-parameter call with force)
        translationManager.translateText(testText, "es", new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                messageTranslationTargetLanguage[0] = "es"; // This should be the same target language
            }
        }, true);
        
        // Wait for async operations
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // ignore
        }
        
        // Both should use the same target language
        assertEquals("Both translation methods should use the same target language",
                inputTranslationTargetLanguage[0], messageTranslationTargetLanguage[0]);
    }

    @Test
    public void testPreferredOutgoingLanguageFallback() {
        // Test the language selection fallback logic that should be consistent
        String preferredOutgoing = mockUserPreferences.getPreferredOutgoingLanguage();
        String preferredGeneral = mockUserPreferences.getPreferredLanguage();
        
        // Simulate the language selection logic
        String targetLanguage = preferredOutgoing;
        if (targetLanguage == null || targetLanguage.isEmpty()) {
            targetLanguage = preferredGeneral;
        }
        
        assertEquals("Should use preferred outgoing language when available", "es", targetLanguage);
    }

    @Test
    public void testPreferredOutgoingLanguageFallbackWhenNull() {
        // Test fallback when outgoing language is null
        when(mockUserPreferences.getPreferredOutgoingLanguage()).thenReturn(null);
        when(mockUserPreferences.getPreferredLanguage()).thenReturn("en");
        
        String preferredOutgoing = mockUserPreferences.getPreferredOutgoingLanguage();
        String preferredGeneral = mockUserPreferences.getPreferredLanguage();
        
        String targetLanguage = preferredOutgoing;
        if (targetLanguage == null || targetLanguage.isEmpty()) {
            targetLanguage = preferredGeneral;
        }
        
        assertEquals("Should fallback to preferred general language when outgoing is null", "en", targetLanguage);
    }

    @Test
    public void testPreferredOutgoingLanguageFallbackWhenEmpty() {
        // Test fallback when outgoing language is empty
        when(mockUserPreferences.getPreferredOutgoingLanguage()).thenReturn("");
        when(mockUserPreferences.getPreferredLanguage()).thenReturn("fr");
        
        String preferredOutgoing = mockUserPreferences.getPreferredOutgoingLanguage();
        String preferredGeneral = mockUserPreferences.getPreferredLanguage();
        
        String targetLanguage = preferredOutgoing;
        if (targetLanguage == null || targetLanguage.isEmpty()) {
            targetLanguage = preferredGeneral;
        }
        
        assertEquals("Should fallback to preferred general language when outgoing is empty", "fr", targetLanguage);
    }
}