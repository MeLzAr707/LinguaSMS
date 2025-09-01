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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test for the enhanced offline translation verification system.
 * This test validates the complete integration of model integrity verification,
 * status reporting, and synchronization between components.
 */
@RunWith(RobolectricTestRunner.class)
public class OfflineTranslationVerificationTest {

    private Context context;
    private OfflineTranslationService offlineTranslationService;
    private OfflineModelManager offlineModelManager;
    
    @Mock
    private UserPreferences mockUserPreferences;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        
        // Set up mock preferences
        when(mockUserPreferences.isOfflineTranslationEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferOfflineTranslation()).thenReturn(true);
        
        // Initialize services
        offlineTranslationService = new OfflineTranslationService(context, mockUserPreferences);
        offlineModelManager = new OfflineModelManager(context);
        
        // Clean up any existing test data
        cleanupTestData();
    }
    
    @Test
    public void testCompleteModelVerificationFlow() {
        // Test the complete flow from download to verification to usage
        
        // 1. Initially no models should be available
        assertFalse("No models should be available initially",
                offlineTranslationService.isOfflineTranslationAvailable("en", "es"));
        
        // 2. Create verified models manually
        createVerifiedTestModel("en");
        createVerifiedTestModel("es");
        
        // 3. Update preferences to reflect downloaded models
        Set<String> downloadedModels = new HashSet<>();
        downloadedModels.add("en");
        downloadedModels.add("es");
        
        SharedPreferences modelPrefs = context.getSharedPreferences("offline_models", Context.MODE_PRIVATE);
        modelPrefs.edit().putStringSet("downloaded_models", downloadedModels).apply();
        
        // 4. Refresh services to pick up the models
        offlineTranslationService.refreshDownloadedModels();
        
        // 5. Verify models are now detected
        assertTrue("English model should be detected as downloaded",
                offlineModelManager.isModelDownloaded("en"));
        assertTrue("Spanish model should be detected as downloaded",
                offlineModelManager.isModelDownloaded("es"));
        
        // 6. Verify integrity checks pass
        assertTrue("English model should pass integrity verification",
                offlineModelManager.verifyModelIntegrity("en"));
        assertTrue("Spanish model should pass integrity verification",
                offlineModelManager.verifyModelIntegrity("es"));
        
        // 7. Verify combined verification
        assertTrue("English model should be downloaded and verified",
                offlineModelManager.isModelDownloadedAndVerified("en"));
        assertTrue("Spanish model should be downloaded and verified",
                offlineModelManager.isModelDownloadedAndVerified("es"));
    }
    
    @Test
    public void testDetailedModelStatusReporting() {
        // Test the detailed model status reporting functionality
        
        // Create one verified model and one corrupted model
        createVerifiedTestModel("en");
        createCorruptedTestModel("es");
        
        // Mark both as downloaded in preferences
        Set<String> downloadedModels = new HashSet<>();
        downloadedModels.add("en");
        downloadedModels.add("es");
        
        SharedPreferences modelPrefs = context.getSharedPreferences("offline_models", Context.MODE_PRIVATE);
        modelPrefs.edit().putStringSet("downloaded_models", downloadedModels).apply();
        
        offlineTranslationService.refreshDownloadedModels();
        
        // Get detailed status
        Map<String, OfflineTranslationService.DetailedModelStatus> statusMap = 
                offlineTranslationService.getDetailedModelStatus();
        
        assertNotNull("Status map should not be null", statusMap);
        
        // Check English model status
        OfflineTranslationService.DetailedModelStatus englishStatus = statusMap.get("en");
        assertNotNull("English status should be available", englishStatus);
        assertTrue("English should be downloaded", englishStatus.isDownloaded);
        assertTrue("English should be verified", englishStatus.isVerified);
        assertTrue("English should be tracked in service", englishStatus.isTrackedInService);
        assertTrue("English should be synchronized", englishStatus.isSynchronized);
        
        // Check Spanish model status (corrupted)
        OfflineTranslationService.DetailedModelStatus spanishStatus = statusMap.get("es");
        assertNotNull("Spanish status should be available", spanishStatus);
        assertTrue("Spanish should be marked as downloaded", spanishStatus.isDownloaded);
        assertFalse("Spanish should fail verification", spanishStatus.isVerified);
        assertTrue("Spanish should be tracked in service", spanishStatus.isTrackedInService);
        assertTrue("Spanish should be synchronized", spanishStatus.isSynchronized);
    }
    
    @Test
    public void testSynchronizationBetweenComponents() {
        // Test synchronization between OfflineModelManager and OfflineTranslationService
        
        // Create verified model using model manager
        createVerifiedTestModel("fr");
        
        // Mark as downloaded in model manager preferences
        Set<String> downloadedModels = new HashSet<>();
        downloadedModels.add("fr");
        
        SharedPreferences modelPrefs = context.getSharedPreferences("offline_models", Context.MODE_PRIVATE);
        modelPrefs.edit().putStringSet("downloaded_models", downloadedModels).apply();
        
        // Verify model manager sees it
        assertTrue("Model manager should see French model",
                offlineModelManager.isModelDownloaded("fr"));
        assertTrue("French model should pass integrity check",
                offlineModelManager.verifyModelIntegrity("fr"));
        
        // Refresh translation service
        offlineTranslationService.refreshDownloadedModels();
        
        // Verify translation service sees it
        assertTrue("Translation service should see French model",
                offlineTranslationService.isLanguageModelDownloaded("fr"));
        
        // Check detailed status shows synchronization
        Map<String, OfflineTranslationService.DetailedModelStatus> statusMap = 
                offlineTranslationService.getDetailedModelStatus();
        
        OfflineTranslationService.DetailedModelStatus frenchStatus = statusMap.get("fr");
        assertNotNull("French status should be available", frenchStatus);
        assertTrue("French should show as synchronized", frenchStatus.isSynchronized);
    }
    
    @Test
    public void testCorruptionDetectionAndRecovery() {
        // Test detection of corrupted models and recovery procedures
        
        // Create a corrupted model
        createCorruptedTestModel("de");
        
        // Mark as downloaded
        Set<String> downloadedModels = new HashSet<>();
        downloadedModels.add("de");
        
        SharedPreferences modelPrefs = context.getSharedPreferences("offline_models", Context.MODE_PRIVATE);
        modelPrefs.edit().putStringSet("downloaded_models", downloadedModels).apply();
        
        // Verify corruption is detected
        assertTrue("German model should be marked as downloaded",
                offlineModelManager.isModelDownloaded("de"));
        assertFalse("German model should fail integrity verification",
                offlineModelManager.verifyModelIntegrity("de"));
        assertFalse("German model should fail combined verification",
                offlineModelManager.isModelDownloadedAndVerified("de"));
        
        // Check status reflects corruption
        Map<String, OfflineModelManager.ModelStatus> managerStatus = 
                offlineModelManager.getModelStatusMap();
        
        OfflineModelManager.ModelStatus germanStatus = managerStatus.get("de");
        assertNotNull("German status should be available", germanStatus);
        assertTrue("German should be marked as downloaded", germanStatus.isDownloaded);
        assertFalse("German should fail verification", germanStatus.isVerified);
    }
    
    @Test
    public void testMultipleLanguageVerification() {
        // Test verification with multiple language models
        
        String[] languages = {"en", "es", "fr", "de", "it"};
        
        // Create verified models for all languages
        Set<String> downloadedModels = new HashSet<>();
        for (String lang : languages) {
            createVerifiedTestModel(lang);
            downloadedModels.add(lang);
        }
        
        // Update preferences
        SharedPreferences modelPrefs = context.getSharedPreferences("offline_models", Context.MODE_PRIVATE);
        modelPrefs.edit().putStringSet("downloaded_models", downloadedModels).apply();
        
        offlineTranslationService.refreshDownloadedModels();
        
        // Verify all models
        for (String lang : languages) {
            assertTrue("Model should be downloaded: " + lang,
                    offlineModelManager.isModelDownloaded(lang));
            assertTrue("Model should pass verification: " + lang,
                    offlineModelManager.verifyModelIntegrity(lang));
            assertTrue("Model should be downloadedAndVerified: " + lang,
                    offlineModelManager.isModelDownloadedAndVerified(lang));
        }
        
        // Test translation availability between language pairs
        assertTrue("EN-ES translation should be available",
                offlineTranslationService.isOfflineTranslationAvailable("en", "es"));
        assertTrue("FR-DE translation should be available",
                offlineTranslationService.isOfflineTranslationAvailable("fr", "de"));
        assertTrue("IT-EN translation should be available",
                offlineTranslationService.isOfflineTranslationAvailable("it", "en"));
    }
    
    @Test
    public void testModelStatusMapComprehensive() {
        // Test comprehensive model status mapping
        
        // Create mixed scenario: some verified, some corrupted, some missing
        createVerifiedTestModel("en");
        createCorruptedTestModel("es");
        // "fr" will be missing (not created)
        
        Set<String> downloadedModels = new HashSet<>();
        downloadedModels.add("en");
        downloadedModels.add("es");
        downloadedModels.add("fr"); // This will be missing file but marked as downloaded
        
        SharedPreferences modelPrefs = context.getSharedPreferences("offline_models", Context.MODE_PRIVATE);
        modelPrefs.edit().putStringSet("downloaded_models", downloadedModels).apply();
        
        // Get comprehensive status
        Map<String, OfflineModelManager.ModelStatus> managerStatus = 
                offlineModelManager.getModelStatusMap();
        
        // Verify English (good model)
        OfflineModelManager.ModelStatus englishStatus = managerStatus.get("en");
        assertNotNull("English status should exist", englishStatus);
        assertTrue("English should be downloaded", englishStatus.isDownloaded);
        assertTrue("English should be verified", englishStatus.isVerified);
        
        // Verify Spanish (corrupted model)
        OfflineModelManager.ModelStatus spanishStatus = managerStatus.get("es");
        assertNotNull("Spanish status should exist", spanishStatus);
        assertTrue("Spanish should be downloaded", spanishStatus.isDownloaded);
        assertFalse("Spanish should fail verification", spanishStatus.isVerified);
        
        // Verify French (missing file)
        OfflineModelManager.ModelStatus frenchStatus = managerStatus.get("fr");
        assertNotNull("French status should exist", frenchStatus);
        assertTrue("French should be marked as downloaded", frenchStatus.isDownloaded);
        assertFalse("French should fail verification (missing file)", frenchStatus.isVerified);
        
        // Verify German (not downloaded)
        OfflineModelManager.ModelStatus germanStatus = managerStatus.get("de");
        assertNotNull("German status should exist", germanStatus);
        assertFalse("German should not be downloaded", germanStatus.isDownloaded);
        assertFalse("German should not be verified", germanStatus.isVerified);
    }
    
    private void createVerifiedTestModel(String languageCode) {
        try {
            File modelDir = new File(context.getFilesDir(), "offline_models");
            modelDir.mkdirs();
            
            File modelFile = new File(modelDir, languageCode + ".model");
            FileWriter writer = new FileWriter(modelFile);
            writer.write("OFFLINE_MODEL_" + languageCode.toUpperCase() + "_v1.0");
            writer.close();
            
        } catch (IOException e) {
            fail("Failed to create test model for " + languageCode + ": " + e.getMessage());
        }
    }
    
    private void createCorruptedTestModel(String languageCode) {
        try {
            File modelDir = new File(context.getFilesDir(), "offline_models");
            modelDir.mkdirs();
            
            File modelFile = new File(modelDir, languageCode + ".model");
            FileWriter writer = new FileWriter(modelFile);
            writer.write("CORRUPTED_DATA_" + languageCode);
            writer.close();
            
        } catch (IOException e) {
            fail("Failed to create corrupted test model for " + languageCode + ": " + e.getMessage());
        }
    }
    
    private void cleanupTestData() {
        // Clean up SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("offline_models", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        
        // Clean up model directory
        File modelDir = new File(context.getFilesDir(), "offline_models");
        if (modelDir.exists()) {
            File[] files = modelDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }
}