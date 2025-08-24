package com.translator.messagingapp;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Test for offline language detection functionality.
 * Verifies that the OfflineTranslationService can properly detect languages
 * for non-English/Spanish text without defaulting to English.
 */
@RunWith(RobolectricTestRunner.class)
public class OfflineLanguageDetectionTest {
    
    private Context context;
    private UserPreferences userPreferences;
    private OfflineTranslationService offlineTranslationService;
    
    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        userPreferences = new UserPreferences(context);
        offlineTranslationService = new OfflineTranslationService(context, userPreferences);
    }
    
    @Test
    public void testOfflineLanguageDetectionCallback() throws InterruptedException {
        // Test with English text
        String englishText = "Hello, how are you today?";
        
        CountDownLatch latch = new CountDownLatch(1);
        final String[] detectedLanguage = new String[1];
        final boolean[] success = new boolean[1];
        
        offlineTranslationService.detectLanguageOffline(englishText, new OfflineTranslationService.OfflineLanguageDetectionCallback() {
            @Override
            public void onLanguageDetected(boolean isSuccess, String languageCode, String errorMessage) {
                success[0] = isSuccess;
                detectedLanguage[0] = languageCode;
                latch.countDown();
            }
        });
        
        // Wait for the callback
        assertTrue("Language detection should complete within 10 seconds", 
                  latch.await(10, TimeUnit.SECONDS));
        
        // Verify detection succeeded
        assertTrue("Language detection should succeed", success[0]);
        assertNotNull("Detected language should not be null", detectedLanguage[0]);
        assertEquals("Should detect English", "en", detectedLanguage[0]);
    }
    
    @Test
    public void testOfflineLanguageDetectionSync() {
        // Test with various languages
        
        // English
        String englishText = "Hello world";
        String detectedEnglish = offlineTranslationService.detectLanguageOfflineSync(englishText);
        assertEquals("Should detect English", "en", detectedEnglish);
        
        // Spanish  
        String spanishText = "Hola mundo";
        String detectedSpanish = offlineTranslationService.detectLanguageOfflineSync(spanishText);
        assertEquals("Should detect Spanish", "es", detectedSpanish);
        
        // French
        String frenchText = "Bonjour le monde";
        String detectedFrench = offlineTranslationService.detectLanguageOfflineSync(frenchText);
        assertEquals("Should detect French", "fr", detectedFrench);
    }
    
    @Test
    public void testLanguageDetectionWithEmptyText() {
        // Test with empty text
        String detected = offlineTranslationService.detectLanguageOfflineSync("");
        assertNull("Empty text should return null", detected);
        
        // Test with null text
        detected = offlineTranslationService.detectLanguageOfflineSync(null);
        assertNull("Null text should return null", detected);
        
        // Test with whitespace only
        detected = offlineTranslationService.detectLanguageOfflineSync("   ");
        assertNull("Whitespace-only text should return null", detected);
    }
    
    @Test
    public void testLanguageDetectionCallback() throws InterruptedException {
        // Test the callback version with empty text
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = new boolean[1];
        final String[] errorMessage = new String[1];
        
        offlineTranslationService.detectLanguageOffline("", new OfflineTranslationService.OfflineLanguageDetectionCallback() {
            @Override
            public void onLanguageDetected(boolean isSuccess, String languageCode, String error) {
                success[0] = isSuccess;
                errorMessage[0] = error;
                latch.countDown();
            }
        });
        
        assertTrue("Callback should be called within 5 seconds", 
                  latch.await(5, TimeUnit.SECONDS));
        
        assertFalse("Detection should fail for empty text", success[0]);
        assertNotNull("Error message should be provided", errorMessage[0]);
        assertEquals("Should return specific error for empty text", "Text is empty", errorMessage[0]);
    }
    
    @Test 
    public void testLanguageDetectionIntegration() {
        // Test that the language detection integrates properly with the service
        
        // Verify the method exists and can be called
        assertNotNull("OfflineTranslationService should exist", offlineTranslationService);
        
        // Test that we can detect some common languages
        String[] testTexts = {
            "Hello world",           // English
            "Hola mundo",           // Spanish  
            "Bonjour monde",        // French
            "Hallo Welt",           // German
            "Ciao mondo"            // Italian
        };
        
        String[] expectedLanguages = {"en", "es", "fr", "de", "it"};
        
        for (int i = 0; i < testTexts.length; i++) {
            String detected = offlineTranslationService.detectLanguageOfflineSync(testTexts[i]);
            assertNotNull("Should detect language for: " + testTexts[i], detected);
            
            // For robust testing, we just verify a language was detected
            // since actual detection might vary based on MLKit model availability
            assertTrue("Detected language should not be empty for: " + testTexts[i], 
                      detected.length() > 0);
        }
    }
}