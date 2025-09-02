package com.translator.messagingapp;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;

/**
 * Test to reproduce the offline model download issue described in the problem statement.
 * This test verifies the download flow and identifies the root cause of the problem.
 */
@RunWith(RobolectricTestRunner.class)
public class OfflineDownloadFlowTest {

    private Context context;
    private UserPreferences userPreferences;
    private OfflineModelManager modelManager;
    private OfflineTranslationService translationService;
    private TranslationManager translationManager;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        userPreferences = new UserPreferences(context);
        modelManager = new OfflineModelManager(context);
        
        // Create a minimal GoogleTranslationService for TranslationManager
        GoogleTranslationService googleService = new GoogleTranslationService("fake-api-key");
        translationManager = new TranslationManager(context, googleService, userPreferences);
        translationService = translationManager.getOfflineTranslationService();
    }

    @Test
    public void testOfflineDownloadFlowProblem() {
        // Reproduce the issue: "download completes instantly, indicating that no actual download occurs"
        
        String languageCode = "es"; // Spanish
        
        // 1. Verify model is not initially downloaded
        assertFalse("Model should not be downloaded initially", 
                   modelManager.isModelDownloaded(languageCode));
        assertFalse("Translation should not be available initially", 
                   translationService.isOfflineTranslationAvailable("en", languageCode));
        
        // 2. Simulate what happens when user clicks download button in UI
        // This mimics OfflineModelsActivity.downloadModel() behavior
        
        // Get the model info
        OfflineModelInfo model = findModelByLanguageCode(languageCode);
        assertNotNull("Model info should be found", model);
        assertFalse("Model should not be marked as downloaded", model.isDownloaded());
        
        // 3. Test the download process that's currently happening
        // This demonstrates the "instant completion" problem
        
        final boolean[] downloadCompleted = {false};
        final String[] errorMessage = {null};
        final boolean[] simulationUsed = {false};
        
        // Check if translationManager and its offline service are available
        if (translationManager != null && translationManager.getOfflineTranslationService() != null) {
            // This path should use real ML Kit download
            translationService.downloadLanguageModel(languageCode, 
                new OfflineTranslationService.ModelDownloadCallback() {
                    @Override
                    public void onDownloadComplete(boolean success, String langCode, String error) {
                        downloadCompleted[0] = true;
                        errorMessage[0] = error;
                        // Real ML Kit download path
                    }

                    @Override
                    public void onDownloadProgress(String langCode, int progress) {
                        // Real progress updates
                    }
                });
        } else {
            // This path uses simulation (the problem!)
            simulationUsed[0] = true;
            modelManager.downloadModel(model, new OfflineModelManager.DownloadListener() {
                @Override
                public void onProgress(int progress) {
                    // Simulated progress - this is the "instant completion" issue
                }

                @Override
                public void onSuccess() {
                    downloadCompleted[0] = true;
                    // Simulation marks as complete but no real download occurred
                }

                @Override
                public void onError(String error) {
                    downloadCompleted[0] = true;
                    errorMessage[0] = error;
                }
            });
        }
        
        // Wait for either real or simulated download to complete
        // In the problematic case, simulation completes in ~5 seconds
        try {
            Thread.sleep(6000); // Wait for simulated download
        } catch (InterruptedException e) {
            // Ignore
        }
        
        // 4. Verify the problem: download appears complete but models aren't actually available
        assertTrue("Download should have completed", downloadCompleted[0]);
        
        if (simulationUsed[0]) {
            // The problematic case: simulation was used
            System.out.println("PROBLEM IDENTIFIED: Download used simulation instead of real ML Kit download");
            
            // Simulation marks model as downloaded in preferences
            assertTrue("Simulation marks model as downloaded in OfflineModelManager", 
                      modelManager.isModelDownloaded(languageCode));
            
            // But the actual ML Kit models aren't available for translation
            assertFalse("Real ML Kit models are not available for translation", 
                       translationService.isOfflineTranslationAvailable("en", languageCode));
            
            System.out.println("ISSUE: OfflineModelManager thinks model is downloaded, but OfflineTranslationService knows it's not");
        } else {
            // The correct case: real ML Kit download was used
            System.out.println("SUCCESS: Real ML Kit download was used");
            
            if (errorMessage[0] == null) {
                // Real download succeeded
                assertTrue("Real download should make models available", 
                          translationService.isOfflineTranslationAvailable("en", languageCode));
            }
        }
    }
    
    @Test
    public void testDownloadBehaviorDiscrepancy() {
        // Test the specific discrepancy between simulation and real download
        
        String languageCode = "fr"; // French
        
        // 1. Use OfflineModelManager simulation
        OfflineModelInfo model = findModelByLanguageCode(languageCode);
        final boolean[] simulationComplete = {false};
        
        modelManager.downloadModel(model, new OfflineModelManager.DownloadListener() {
            @Override
            public void onProgress(int progress) {
                System.out.println("Simulation progress: " + progress + "%");
            }

            @Override
            public void onSuccess() {
                simulationComplete[0] = true;
                System.out.println("Simulation completed successfully");
            }

            @Override
            public void onError(String error) {
                simulationComplete[0] = true;
                System.out.println("Simulation failed: " + error);
            }
        });
        
        // Wait for simulation
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        assertTrue("Simulation should complete", simulationComplete[0]);
        assertTrue("Simulation marks model as downloaded", modelManager.isModelDownloaded(languageCode));
        
        // 2. Check if OfflineTranslationService sees the model
        // Refresh the service to pick up the change
        translationService.refreshDownloadedModels();
        
        // The key issue: does the translation service recognize the "downloaded" model?
        boolean isModelRecognized = translationService.isLanguageModelDownloaded(languageCode);
        boolean isTranslationAvailable = translationService.isOfflineTranslationAvailable("en", languageCode);
        
        System.out.println("Model recognized by OfflineTranslationService: " + isModelRecognized);
        System.out.println("Translation available: " + isTranslationAvailable);
        
        if (isModelRecognized && !isTranslationAvailable) {
            System.out.println("ISSUE: Model is tracked but not actually available for translation");
        } else if (!isModelRecognized) {
            System.out.println("ISSUE: Model download not properly synchronized between components");
        }
    }
    
    private OfflineModelInfo findModelByLanguageCode(String languageCode) {
        for (OfflineModelInfo model : modelManager.getAvailableModels()) {
            if (languageCode.equals(model.getLanguageCode())) {
                return model;
            }
        }
        return null;
    }
}