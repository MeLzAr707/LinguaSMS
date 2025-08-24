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

import static org.junit.Assert.*;

/**
 * Test to verify the fixes for offline translation synchronization issues.
 * This test validates that the language code mapping and synchronization
 * between OfflineModelManager and OfflineTranslationService works correctly.
 */
@RunWith(RobolectricTestRunner.class)
public class OfflineTranslationSyncFixTest {

    private Context context;
    private UserPreferences userPreferences;
    private OfflineModelManager modelManager;
    private OfflineTranslationService translationService;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        userPreferences = new UserPreferences(context);
        modelManager = new OfflineModelManager(context);
        translationService = new OfflineTranslationService(context, userPreferences);
        
        // Set up the model change listener for synchronization
        modelManager.setModelChangeListener(translationService);
    }

    @Test
    public void testChineseLanguageCodeMapping() {
        // Test that Chinese language code mapping works correctly
        // Previously had issues with zh-CN vs zh mapping
        
        // Simulate downloading Chinese model
        Set<String> downloadedModels = new HashSet<>();
        downloadedModels.add("zh");  // Use the simplified Chinese code
        
        SharedPreferences modelPrefs = context.getSharedPreferences("offline_models", Context.MODE_PRIVATE);
        modelPrefs.edit().putStringSet("downloaded_models", downloadedModels).apply();
        
        // Refresh the translation service
        translationService.refreshDownloadedModels();
        
        // Test if Chinese model is properly recognized
        boolean isChineseAvailable = translationService.isLanguageModelDownloaded("zh");
        assertTrue("Chinese model should be available after fix", isChineseAvailable);
        
        // Test translation availability
        boolean translationAvailable = translationService.isOfflineTranslationAvailable("en", "zh");
        // Note: This may return false if English is not also "downloaded" but language mapping should work
        
        // Verify the Chinese language code is in the supported languages
        String[] supportedLanguages = translationService.getSupportedLanguages();
        boolean hasChineseSupport = false;
        for (String lang : supportedLanguages) {
            if ("zh".equals(lang)) {
                hasChineseSupport = true;
                break;
            }
        }
        assertTrue("Chinese should be in supported languages", hasChineseSupport);
    }

    @Test
    public void testGreekLanguageCodeMapping() {
        // Test that Greek language code mapping works correctly
        // This was missing from the convertToMLKitLanguageCode method
        
        // Simulate downloading Greek model  
        Set<String> downloadedModels = new HashSet<>();
        downloadedModels.add("el");
        
        SharedPreferences modelPrefs = context.getSharedPreferences("offline_models", Context.MODE_PRIVATE);
        modelPrefs.edit().putStringSet("downloaded_models", downloadedModels).apply();
        
        // Refresh the translation service
        translationService.refreshDownloadedModels();
        
        // Test if Greek model is properly recognized
        boolean isGreekAvailable = translationService.isLanguageModelDownloaded("el");
        assertTrue("Greek model should be available after mapping fix", isGreekAvailable);
        
        // Verify the Greek language code is in the supported languages
        String[] supportedLanguages = translationService.getSupportedLanguages();
        boolean hasGreekSupport = false;
        for (String lang : supportedLanguages) {
            if ("el".equals(lang)) {
                hasGreekSupport = true;
                break;
            }
        }
        assertTrue("Greek should be in supported languages", hasGreekSupport);
    }

    @Test
    public void testSynchronizationWithModelChangeListener() {
        // Test that the new ModelChangeListener mechanism works
        
        // Initially no models
        assertFalse("Should have no models initially", translationService.hasAnyDownloadedModels());
        
        // Simulate model download via direct SharedPreferences update (as would happen in real download)
        Set<String> downloadedModels = new HashSet<>();
        downloadedModels.add("en");
        downloadedModels.add("es");
        
        SharedPreferences modelPrefs = context.getSharedPreferences("offline_models", Context.MODE_PRIVATE);
        modelPrefs.edit().putStringSet("downloaded_models", downloadedModels).apply();
        
        // Manually trigger the model change notification (simulating what happens when OfflineModelManager downloads)
        translationService.onModelDownloaded("en");
        translationService.onModelDownloaded("es");
        
        // Verify models are now recognized
        assertTrue("Should have downloaded models after notification", translationService.hasAnyDownloadedModels());
        assertTrue("English should be available", translationService.isLanguageModelDownloaded("en"));
        assertTrue("Spanish should be available", translationService.isLanguageModelDownloaded("es"));
        
        // Test deletion notification
        downloadedModels.remove("es");
        modelPrefs.edit().putStringSet("downloaded_models", downloadedModels).apply();
        translationService.onModelDeleted("es");
        
        // Verify Spanish is no longer available but English still is
        assertTrue("English should still be available", translationService.isLanguageModelDownloaded("en"));
        assertFalse("Spanish should no longer be available", translationService.isLanguageModelDownloaded("es"));
    }

    @Test
    public void testLanguageCodeConsistencyBetweenServices() {
        // Test that OfflineModelManager and OfflineTranslationService use consistent language codes
        
        // Get available models from OfflineModelManager
        var availableModels = modelManager.getAvailableModels();
        
        // Get supported languages from OfflineTranslationService
        String[] supportedLanguages = translationService.getSupportedLanguages();
        Set<String> supportedSet = new HashSet<>();
        for (String lang : supportedLanguages) {
            supportedSet.add(lang);
        }
        
        // Verify that all models in OfflineModelManager are supported by OfflineTranslationService
        for (var model : availableModels) {
            String languageCode = model.getLanguageCode();
            assertTrue("Language " + languageCode + " should be supported by translation service", 
                      supportedSet.contains(languageCode));
        }
    }

    @Test
    public void testNullAndEmptyLanguageCodeHandling() {
        // Test that null and empty language codes are handled gracefully
        
        assertFalse("Null source language should return false", 
                   translationService.isOfflineTranslationAvailable(null, "es"));
        assertFalse("Null target language should return false", 
                   translationService.isOfflineTranslationAvailable("en", null));
        assertFalse("Both null should return false", 
                   translationService.isOfflineTranslationAvailable(null, null));
        
        assertFalse("Empty source language should return false", 
                   translationService.isOfflineTranslationAvailable("", "es"));
        assertFalse("Empty target language should return false", 
                   translationService.isOfflineTranslationAvailable("en", ""));
        
        assertFalse("Null language code should return false for model check", 
                   translationService.isLanguageModelDownloaded(null));
        assertFalse("Empty language code should return false for model check", 
                   translationService.isLanguageModelDownloaded(""));
    }

    @Test
    public void testUnsupportedLanguageCodeHandling() {
        // Test that unsupported language codes are handled gracefully
        
        assertFalse("Unsupported language xx should return false", 
                   translationService.isOfflineTranslationAvailable("xx", "es"));
        assertFalse("Unsupported language yy should return false", 
                   translationService.isOfflineTranslationAvailable("en", "yy"));
        assertFalse("Both unsupported should return false", 
                   translationService.isOfflineTranslationAvailable("xx", "yy"));
        
        assertFalse("Unsupported language code should return false for model check", 
                   translationService.isLanguageModelDownloaded("xx"));
    }
}