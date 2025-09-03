package com.translator.messagingapp;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Test class to validate the redesigned OfflineModelManager following ML Kit best practices.
 * This test verifies that the class properly integrates with ML Kit APIs and follows
 * recommended patterns for model management.
 */
@RunWith(RobolectricTestRunner.class)
public class OfflineModelManagerRedesignTest {

    private Context context;
    private OfflineModelManager modelManager;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        modelManager = new OfflineModelManager(context);
    }

    @Test
    public void testLanguageCodeValidation() {
        // Test supported languages
        assertTrue("English should be supported", modelManager.isLanguageSupported("en"));
        assertTrue("Spanish should be supported", modelManager.isLanguageSupported("es"));
        assertTrue("French should be supported", modelManager.isLanguageSupported("fr"));
        assertTrue("German should be supported", modelManager.isLanguageSupported("de"));
        assertTrue("Chinese should be supported", modelManager.isLanguageSupported("zh"));
        
        // Test unsupported language
        assertFalse("Klingon should not be supported", modelManager.isLanguageSupported("tlh"));
        assertFalse("Null should not be supported", modelManager.isLanguageSupported(null));
    }

    @Test
    public void testGetSupportedLanguageCodes() {
        Set<String> supportedCodes = modelManager.getSupportedLanguageCodes();
        
        assertNotNull("Supported codes should not be null", supportedCodes);
        assertFalse("Should have supported languages", supportedCodes.isEmpty());
        
        // Verify common languages are included
        assertTrue("Should support English", supportedCodes.contains("en"));
        assertTrue("Should support Spanish", supportedCodes.contains("es"));
        assertTrue("Should support French", supportedCodes.contains("fr"));
    }

    @Test
    public void testGetAvailableModelsContainsSupportedLanguages() {
        List<OfflineModelInfo> models = modelManager.getAvailableModels();
        
        assertNotNull("Available models should not be null", models);
        assertFalse("Should have available models", models.isEmpty());
        
        // Verify all models have supported language codes
        for (OfflineModelInfo model : models) {
            assertTrue("Model " + model.getLanguageCode() + " should be supported", 
                      modelManager.isLanguageSupported(model.getLanguageCode()));
        }
    }

    @Test
    public void testModelStatusMap() {
        Map<String, OfflineModelManager.ModelStatus> statusMap = modelManager.getModelStatusMap();
        
        assertNotNull("Status map should not be null", statusMap);
        assertFalse("Status map should not be empty", statusMap.isEmpty());
        
        // Verify status objects are properly created
        for (Map.Entry<String, OfflineModelManager.ModelStatus> entry : statusMap.entrySet()) {
            String languageCode = entry.getKey();
            OfflineModelManager.ModelStatus status = entry.getValue();
            
            assertNotNull("Status should not be null for " + languageCode, status);
            assertNotNull("Status string should not be null for " + languageCode, status.getStatus());
            
            // Status should be one of the valid states
            String statusStr = status.getStatus();
            assertTrue("Status should be valid for " + languageCode,
                      OfflineModelManager.ModelStatus.DOWNLOADED.equals(statusStr) ||
                      OfflineModelManager.ModelStatus.NOT_DOWNLOADED.equals(statusStr) ||
                      OfflineModelManager.ModelStatus.DOWNLOADING.equals(statusStr) ||
                      OfflineModelManager.ModelStatus.ERROR.equals(statusStr));
        }
    }

    @Test
    public void testDownloadModelWithUnsupportedLanguage() throws InterruptedException {
        // Create a model with unsupported language
        OfflineModelInfo model = new OfflineModelInfo("unsupported", "Unsupported Language", 1024);
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean errorReceived = new AtomicBoolean(false);
        AtomicReference<String> errorMessage = new AtomicReference<>();
        
        modelManager.downloadModel(model, new OfflineModelManager.DownloadListener() {
            @Override
            public void onProgress(int progress) {
                // Should not receive progress for unsupported language
            }
            
            @Override
            public void onSuccess() {
                // Should not succeed for unsupported language
                latch.countDown();
            }
            
            @Override
            public void onError(String error) {
                errorReceived.set(true);
                errorMessage.set(error);
                latch.countDown();
            }
        });
        
        assertTrue("Download should complete", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Should receive error for unsupported language", errorReceived.get());
        assertNotNull("Error message should not be null", errorMessage.get());
        assertTrue("Error message should mention unsupported language", 
                  errorMessage.get().toLowerCase().contains("unsupported"));
    }

    @Test
    public void testDownloadModelAlreadyDownloaded() throws InterruptedException {
        // Create a model and mark it as downloaded
        OfflineModelInfo model = new OfflineModelInfo("en", "English", 1024);
        model.setDownloaded(true);
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean errorReceived = new AtomicBoolean(false);
        AtomicReference<String> errorMessage = new AtomicReference<>();
        
        modelManager.downloadModel(model, new OfflineModelManager.DownloadListener() {
            @Override
            public void onProgress(int progress) {
                // Should not receive progress for already downloaded model
            }
            
            @Override
            public void onSuccess() {
                // Should not succeed for already downloaded model
                latch.countDown();
            }
            
            @Override
            public void onError(String error) {
                errorReceived.set(true);
                errorMessage.set(error);
                latch.countDown();
            }
        });
        
        assertTrue("Download should complete", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Should receive error for already downloaded model", errorReceived.get());
        assertNotNull("Error message should not be null", errorMessage.get());
        assertTrue("Error message should mention already downloaded", 
                  errorMessage.get().toLowerCase().contains("already downloaded"));
    }

    @Test
    public void testDeleteModelNotDownloaded() {
        // Create a model that is not downloaded
        OfflineModelInfo model = new OfflineModelInfo("es", "Spanish", 1024);
        model.setDownloaded(false);
        
        boolean result = modelManager.deleteModel(model);
        assertFalse("Should not delete model that is not downloaded", result);
    }

    @Test
    public void testDeleteModelUnsupportedLanguage() {
        // Create a model with unsupported language
        OfflineModelInfo model = new OfflineModelInfo("unsupported", "Unsupported Language", 1024);
        model.setDownloaded(true); // Mark as downloaded to bypass first check
        
        boolean result = modelManager.deleteModel(model);
        assertFalse("Should not delete unsupported language model", result);
    }

    @Test
    public void testIsModelDownloadedAndVerified() {
        // Test with a language that is not downloaded
        boolean result = modelManager.isModelDownloadedAndVerified("es");
        assertFalse("Spanish model should not be downloaded and verified initially", result);
        
        // Test with unsupported language
        result = modelManager.isModelDownloadedAndVerified("unsupported");
        assertFalse("Unsupported language should not be verified", result);
    }

    @Test
    public void testNullHandling() {
        // Test null language code handling
        assertFalse("Null language should not be supported", modelManager.isLanguageSupported(null));
        assertFalse("Null language should not be downloaded", modelManager.isModelDownloaded(null));
        assertFalse("Null language should not be verified", modelManager.isModelDownloadedAndVerified(null));
    }
}