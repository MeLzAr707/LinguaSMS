package com.translator.messagingapp;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test to verify synchronization between OfflineModelManager and OfflineTranslationService.
 * This test reproduces the issue where downloaded models are not recognized by the translation service.
 */
@RunWith(RobolectricTestRunner.class)
public class OfflineTranslationSyncTest {

    private Context context;
    private UserPreferences userPreferences;
    private OfflineModelManager modelManager;
    private OfflineTranslationService translationService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        userPreferences = new UserPreferences(context);
        modelManager = new OfflineModelManager(context);
        translationService = new OfflineTranslationService(context, userPreferences);
    }

    @Test
    public void testOfflineTranslationSyncFix() {
        // Simulate downloading a model via OfflineModelManager
        // This saves to SharedPreferences("offline_models") with key "downloaded_models"
        Set<String> downloadedModels = new HashSet<>();
        downloadedModels.add("en");
        downloadedModels.add("es");
        
        SharedPreferences modelPrefs = context.getSharedPreferences("offline_models", Context.MODE_PRIVATE);
        modelPrefs.edit().putStringSet("downloaded_models", downloadedModels).apply();
        
        // Verify models are marked as downloaded in OfflineModelManager
        assertTrue("English model should be downloaded", modelManager.isModelDownloaded("en"));
        assertTrue("Spanish model should be downloaded", modelManager.isModelDownloaded("es"));
        
        // Refresh the OfflineTranslationService to pick up the downloaded models
        translationService.refreshDownloadedModels();
        
        // Now check if OfflineTranslationService recognizes these models
        // This should now work after the synchronization fix
        boolean isAvailable = translationService.isOfflineTranslationAvailable("en", "es");
        
        // This assertion should now PASS after the fix
        assertTrue("Translation should be available after sync fix", isAvailable);
        
        // Verify what OfflineTranslationService actually sees
        Set<String> serviceModels = translationService.getDownloadedModels();
        assertFalse("Service should now see downloaded models", serviceModels.isEmpty());
        assertEquals("Service should see 2 models", 2, serviceModels.size());
    }

    @Test
    public void testLanguageCodeMappingFix() {
        // Test that language codes are handled consistently
        // OfflineModelManager stores raw codes like "en", "es"
        // OfflineTranslationService converts to MLKit format
        
        // Simulate downloading models with different language codes
        Set<String> downloadedModels = new HashSet<>();
        downloadedModels.add("en");
        downloadedModels.add("zh"); // Chinese simplified (raw format)
        
        SharedPreferences modelPrefs = context.getSharedPreferences("offline_models", Context.MODE_PRIVATE);
        modelPrefs.edit().putStringSet("downloaded_models", downloadedModels).apply();
        
        // Refresh the translation service
        translationService.refreshDownloadedModels();
        
        // Test if translation service can handle these language codes properly
        boolean isEnglishAvailable = translationService.isLanguageModelDownloaded("en");
        boolean isChineseAvailable = translationService.isLanguageModelDownloaded("zh");
        
        // These should now PASS after the synchronization fix
        assertTrue("English should be available after sync fix", isEnglishAvailable);
        assertTrue("Chinese should be available after sync fix", isChineseAvailable);
        
        // Test translation availability between these languages
        boolean translationAvailable = translationService.isOfflineTranslationAvailable("en", "zh");
        assertTrue("Translation between en and zh should be available", translationAvailable);
    }

    @Test
    public void testCompleteOfflineTranslationFlow() {
        // Test the complete flow from model availability check to translation attempt
        
        // 1. Simulate models being downloaded via OfflineModelManager
        Set<String> downloadedModels = new HashSet<>();
        downloadedModels.add("en");
        downloadedModels.add("es");
        
        SharedPreferences modelPrefs = context.getSharedPreferences("offline_models", Context.MODE_PRIVATE);
        modelPrefs.edit().putStringSet("downloaded_models", downloadedModels).apply();
        
        // 2. Refresh OfflineTranslationService
        translationService.refreshDownloadedModels();
        
        // 3. Verify models are available for translation
        assertTrue("EN-ES translation should be available", 
                  translationService.isOfflineTranslationAvailable("en", "es"));
        assertTrue("ES-EN translation should be available", 
                  translationService.isOfflineTranslationAvailable("es", "en"));
        
        // 4. Verify individual model availability  
        assertTrue("English model should be available", 
                  translationService.isLanguageModelDownloaded("en"));
        assertTrue("Spanish model should be available", 
                  translationService.isLanguageModelDownloaded("es"));
        
        // 5. Verify unavailable language pairs return false
        assertFalse("EN-FR should not be available (FR not downloaded)", 
                   translationService.isOfflineTranslationAvailable("en", "fr"));
        
        // 6. Test that the service correctly reports downloaded models
        Set<String> serviceModels = translationService.getDownloadedModels();
        assertEquals("Service should see 2 models", 2, serviceModels.size());
        
        // 7. The models should be in MLKit format internally
        // but the service should still handle raw language code queries correctly
        assertTrue("Service should handle raw language codes", 
                  translationService.isLanguageModelDownloaded("en"));
        assertTrue("Service should handle raw language codes", 
                  translationService.isLanguageModelDownloaded("es"));
    }
}