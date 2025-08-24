package com.translator.messagingapp;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Test to reproduce and verify the fix for the issue where message translation 
 * fails while input text translation works with offline models.
 */
@RunWith(RobolectricTestRunner.class)
public class MessageTranslationFailureTest {

    private Context context;
    private UserPreferences userPreferences;
    private TranslationManager translationManager;
    private GoogleTranslationService mockTranslationService;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        userPreferences = new UserPreferences(context);
        
        // Setup with AUTO mode (default) and offline not explicitly preferred
        // This simulates the typical user setup where offline models are downloaded
        // but user hasn't explicitly configured offline preference
        userPreferences.setTranslationMode(UserPreferences.TRANSLATION_MODE_AUTO);
        userPreferences.setOfflineTranslationEnabled(false); // Default: not explicitly enabled
        userPreferences.setPreferOfflineTranslation(false);  // Default: not preferred
        userPreferences.setPreferredLanguage("en");
        
        // Simulate downloaded offline models
        setupDownloadedModels();
        
        // Create translation manager without API key (offline only)
        mockTranslationService = null; // No online service available
        translationManager = new TranslationManager(context, mockTranslationService, userPreferences);
    }

    private void setupDownloadedModels() {
        // Simulate having Spanish and English models downloaded
        Set<String> downloadedModels = new HashSet<>();
        downloadedModels.add("en");
        downloadedModels.add("es");
        
        SharedPreferences modelPrefs = context.getSharedPreferences("offline_models", Context.MODE_PRIVATE);
        modelPrefs.edit().putStringSet("downloaded_models", downloadedModels).apply();
        
        // Refresh offline translation service to pick up the models
        translationManager.refreshOfflineModels();
    }

    @Test
    public void testMessageTranslationWithDefaultSettings() throws InterruptedException {
        // Create a test message in Spanish
        Message testMessage = new Message();
        testMessage.setBody("Hola mundo, ¿cómo estás?");
        testMessage.setType(Message.TYPE_INBOX);
        
        // Set up callback to capture result
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] translationSuccess = {false};
        final String[] translatedText = {null};
        final String[] errorMessage = {null};
        
        // Attempt to translate the message with default user settings
        // This reproduces the issue: user has models downloaded but hasn't explicitly
        // enabled offline translation, and no API key is configured
        translationManager.translateText(testMessage.getBody(), "en", new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String result, String error) {
                translationSuccess[0] = success;
                translatedText[0] = result;
                errorMessage[0] = error;
                latch.countDown();
            }
        }, true); // Force translation
        
        // Wait for translation to complete
        assertTrue("Translation should complete within 10 seconds", 
                  latch.await(10, TimeUnit.SECONDS));
        
        // Log results for debugging
        System.out.println("Translation success: " + translationSuccess[0]);
        System.out.println("Translated text: " + translatedText[0]);
        System.out.println("Error message: " + errorMessage[0]);
        
        // FIXED: With the fix, message translation should now succeed 
        // even with default settings when offline models are available
        assertTrue("Message translation should succeed with downloaded models (FIXED)", 
                  translationSuccess[0]);
        assertNotNull("Translated text should not be null", translatedText[0]);
        assertFalse("Translated text should not be empty", translatedText[0].trim().isEmpty());
    }

    @Test
    public void testInputTranslationWorking() throws InterruptedException {
        // Test that input translation works (to contrast with message translation)
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] translationSuccess = {false};
        final String[] translatedText = {null};
        final String[] errorMessage = {null};
        
        // Translate input text (this should work according to the issue description)
        translationManager.translateText("Hola mundo", "en", new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String result, String error) {
                translationSuccess[0] = success;
                translatedText[0] = result;
                errorMessage[0] = error;
                latch.countDown();
            }
        }, true); // Force translation like input translation does
        
        // Wait for translation to complete
        assertTrue("Translation should complete within 10 seconds", 
                  latch.await(10, TimeUnit.SECONDS));
        
        System.out.println("Input translation success: " + translationSuccess[0]);
        System.out.println("Input translated text: " + translatedText[0]);
        System.out.println("Input error message: " + errorMessage[0]);
        
        // According to the issue, input translation should work
        assertTrue("Input translation should work according to issue description", 
                  translationSuccess[0]);
    }

    @Test
    public void testOfflineModelAvailability() {
        // Test that offline models are properly detected
        OfflineTranslationService offlineService = translationManager.getOfflineTranslationService();
        
        // Check if models are reported as available
        boolean hasModels = offlineService.hasAnyDownloadedModels();
        System.out.println("Has any downloaded models: " + hasModels);
        
        boolean esEnAvailable = offlineService.isOfflineTranslationAvailable("es", "en");
        System.out.println("Spanish to English translation available: " + esEnAvailable);
        
        assertTrue("Should have downloaded models", hasModels);
        assertTrue("Spanish to English translation should be available", esEnAvailable);
    }

    @Test
    public void testExplicitOfflineMode() throws InterruptedException {
        // Test with explicit offline mode to ensure fix doesn't break existing functionality
        userPreferences.setTranslationMode(UserPreferences.TRANSLATION_MODE_OFFLINE_ONLY);
        
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] translationSuccess = {false};
        
        translationManager.translateText("Hola amigo", "en", new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String result, String error) {
                translationSuccess[0] = success;
                latch.countDown();
            }
        }, true);
        
        assertTrue("Translation should complete within 10 seconds", 
                  latch.await(10, TimeUnit.SECONDS));
        
        assertTrue("Explicit offline mode should work", translationSuccess[0]);
    }
}