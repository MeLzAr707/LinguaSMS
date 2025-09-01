package com.translator.messagingapp;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Test class to verify offline model download integrity verification
 * and error handling capabilities.
 */
@RunWith(RobolectricTestRunner.class)
public class OfflineModelIntegrityTest {

    private Context context;
    private OfflineModelManager modelManager;
    
    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        modelManager = new OfflineModelManager(context);
        
        // Clean up any existing test data
        cleanupTestData();
    }
    
    @Test
    public void testModelIntegrityVerification() {
        // Test that model integrity verification works correctly
        
        // Create a test model file with correct content
        File modelDir = createTestModelDirectory();
        File testModel = new File(modelDir, "en.model");
        
        try {
            FileWriter writer = new FileWriter(testModel);
            writer.write("OFFLINE_MODEL_EN_v1.0");
            writer.close();
            
            // Test verification of correct model
            assertTrue("Model with correct content should pass verification",
                    modelManager.verifyModelIntegrity("en"));
            
            // Test verification of corrupted model
            writer = new FileWriter(testModel);
            writer.write("CORRUPTED_CONTENT");
            writer.close();
            
            assertFalse("Model with corrupted content should fail verification",
                    modelManager.verifyModelIntegrity("en"));
            
        } catch (IOException e) {
            fail("Failed to create test model file: " + e.getMessage());
        } finally {
            testModel.delete();
        }
    }
    
    @Test
    public void testDownloadWithIntegrityCheck() throws InterruptedException {
        // Test that download process includes integrity verification
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean downloadSuccess = new AtomicBoolean(false);
        AtomicReference<String> errorMessage = new AtomicReference<>();
        
        // Get an English model for testing
        OfflineModelInfo englishModel = getModelByLanguageCode("en");
        assertNotNull("English model should be available", englishModel);
        
        OfflineModelManager.DownloadListener listener = new OfflineModelManager.DownloadListener() {
            @Override
            public void onProgress(int progress) {
                // Progress callback - no action needed for this test
            }
            
            @Override
            public void onSuccess() {
                downloadSuccess.set(true);
                latch.countDown();
            }
            
            @Override
            public void onError(String error) {
                errorMessage.set(error);
                latch.countDown();
            }
        };
        
        // Start download
        modelManager.downloadModel(englishModel, listener);
        
        // Wait for download to complete
        assertTrue("Download should complete within timeout", 
                latch.await(30, TimeUnit.SECONDS));
        
        // Verify download was successful and model was verified
        assertTrue("Download should succeed with integrity check", downloadSuccess.get());
        assertTrue("Model should be marked as downloaded after verification",
                modelManager.isModelDownloaded("en"));
        assertTrue("Model should pass integrity verification",
                modelManager.verifyModelIntegrity("en"));
    }
    
    @Test
    public void testCorruptedModelDetection() {
        // Test detection of corrupted models during download verification
        
        // Create a corrupted model file manually
        File modelDir = createTestModelDirectory();
        File corruptedModel = new File(modelDir, "es.model");
        
        try {
            FileWriter writer = new FileWriter(corruptedModel);
            writer.write("CORRUPTED_MODEL_DATA");
            writer.close();
            
            // Mark as downloaded in preferences
            SharedPreferences prefs = context.getSharedPreferences("offline_models", Context.MODE_PRIVATE);
            prefs.edit().putStringSet("downloaded_models", 
                    java.util.Collections.singleton("es")).apply();
            
            // Verify that corrupted model is detected
            assertTrue("Model should be marked as downloaded in preferences",
                    modelManager.isModelDownloaded("es"));
            assertFalse("Corrupted model should fail integrity verification",
                    modelManager.verifyModelIntegrity("es"));
            assertFalse("isModelDownloadedAndVerified should return false for corrupted model",
                    modelManager.isModelDownloadedAndVerified("es"));
            
        } catch (IOException e) {
            fail("Failed to create corrupted test model: " + e.getMessage());
        } finally {
            corruptedModel.delete();
        }
    }
    
    @Test
    public void testModelStatusMap() {
        // Test the getModelStatusMap functionality
        
        Map<String, OfflineModelManager.ModelStatus> statusMap = modelManager.getModelStatusMap();
        
        assertNotNull("Status map should not be null", statusMap);
        assertTrue("Status map should contain entries", statusMap.size() > 0);
        
        // Check that all common languages are included
        assertTrue("Status map should include English", statusMap.containsKey("en"));
        assertTrue("Status map should include Spanish", statusMap.containsKey("es"));
        assertTrue("Status map should include French", statusMap.containsKey("fr"));
        
        // Verify initial state (no models downloaded)
        for (Map.Entry<String, OfflineModelManager.ModelStatus> entry : statusMap.entrySet()) {
            OfflineModelManager.ModelStatus status = entry.getValue();
            assertFalse("Initially no models should be downloaded: " + entry.getKey(),
                    status.isDownloaded);
            assertFalse("Initially no models should be verified: " + entry.getKey(),
                    status.isVerified);
        }
    }
    
    @Test
    public void testMissingModelFile() {
        // Test behavior when model file is missing but marked as downloaded
        
        // Mark English as downloaded without creating file
        SharedPreferences prefs = context.getSharedPreferences("offline_models", Context.MODE_PRIVATE);
        prefs.edit().putStringSet("downloaded_models", 
                java.util.Collections.singleton("en")).apply();
        
        assertTrue("Model should be marked as downloaded in preferences",
                modelManager.isModelDownloaded("en"));
        assertFalse("Missing model file should fail verification",
                modelManager.verifyModelIntegrity("en"));
        assertFalse("isModelDownloadedAndVerified should return false for missing model",
                modelManager.isModelDownloadedAndVerified("en"));
    }
    
    @Test
    public void testUnsupportedLanguageVerification() {
        // Test verification of unsupported language (should pass gracefully)
        
        File modelDir = createTestModelDirectory();
        File unsupportedModel = new File(modelDir, "xx.model");
        
        try {
            FileWriter writer = new FileWriter(unsupportedModel);
            writer.write("UNSUPPORTED_LANGUAGE_MODEL");
            writer.close();
            
            // Verification should pass for unsupported languages (no expected checksum)
            assertTrue("Unsupported language should pass verification gracefully",
                    modelManager.verifyModelIntegrity("xx"));
            
        } catch (IOException e) {
            fail("Failed to create unsupported language test model: " + e.getMessage());
        } finally {
            unsupportedModel.delete();
        }
    }
    
    @Test
    public void testChecksumStorage() {
        // Test that checksums are properly stored and retrieved
        
        File modelDir = createTestModelDirectory();
        File testModel = new File(modelDir, "fr.model");
        
        try {
            FileWriter writer = new FileWriter(testModel);
            writer.write("OFFLINE_MODEL_FR_v1.0");
            writer.close();
            
            // First verification should calculate and store checksum
            assertTrue("First verification should pass and store checksum",
                    modelManager.verifyModelIntegrity("fr"));
            
            // Second verification should use stored checksum
            assertTrue("Second verification should use stored checksum",
                    modelManager.verifyModelIntegrity("fr"));
            
            // Modify file and verify it fails
            writer = new FileWriter(testModel);
            writer.write("MODIFIED_CONTENT");
            writer.close();
            
            assertFalse("Modified file should fail verification",
                    modelManager.verifyModelIntegrity("fr"));
            
        } catch (IOException e) {
            fail("Failed to create test model for checksum testing: " + e.getMessage());
        } finally {
            testModel.delete();
        }
    }
    
    private File createTestModelDirectory() {
        File modelDir = new File(context.getFilesDir(), "offline_models");
        if (!modelDir.exists()) {
            modelDir.mkdirs();
        }
        return modelDir;
    }
    
    private OfflineModelInfo getModelByLanguageCode(String languageCode) {
        for (OfflineModelInfo model : modelManager.getAvailableModels()) {
            if (languageCode.equals(model.getLanguageCode())) {
                return model;
            }
        }
        return null;
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