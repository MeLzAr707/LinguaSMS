package com.translator.messagingapp;

import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test for the newly implemented methods in OfflineModelManager
 * to fix build compilation errors.
 */
@RunWith(RobolectricTestRunner.class)
public class OfflineModelManagerBuildFixTest {

    private OfflineModelManager modelManager;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        modelManager = new OfflineModelManager(context);
    }

    @Test
    public void testIsModelDownloadedAndVerified() {
        // Test the newly implemented method
        String languageCode = "en";
        
        // Should return false for non-downloaded model
        boolean verified = modelManager.isModelDownloadedAndVerified(languageCode);
        assertFalse("Non-downloaded model should not be verified", verified);
        
        // Test with null input
        verified = modelManager.isModelDownloadedAndVerified(null);
        assertFalse("Null input should return false", verified);
    }

    @Test
    public void testGetModelStatusMap() {
        // Test the newly implemented method
        Map<String, OfflineModelManager.ModelStatus> statusMap = modelManager.getModelStatusMap();
        
        assertNotNull("Status map should not be null", statusMap);
        assertFalse("Status map should not be empty", statusMap.isEmpty());
        
        // Check that common language models are included
        assertTrue("English should be in status map", statusMap.containsKey("en"));
        assertTrue("Spanish should be in status map", statusMap.containsKey("es"));
        
        // Verify ModelStatus objects
        OfflineModelManager.ModelStatus englishStatus = statusMap.get("en");
        assertNotNull("English status should not be null", englishStatus);
        assertNotNull("Status should have a status string", englishStatus.getStatus());
        
        // For non-downloaded models, status should be NOT_DOWNLOADED
        assertEquals("Non-downloaded model should have NOT_DOWNLOADED status", 
                    OfflineModelManager.ModelStatus.NOT_DOWNLOADED, englishStatus.getStatus());
        assertFalse("Non-downloaded model should not be verified", englishStatus.isVerified());
    }

    @Test
    public void testModelStatusClass() {
        // Test the newly implemented ModelStatus class
        OfflineModelManager.ModelStatus status1 = new OfflineModelManager.ModelStatus(
            OfflineModelManager.ModelStatus.DOWNLOADED, true);
        
        assertEquals("Status should be DOWNLOADED", 
                    OfflineModelManager.ModelStatus.DOWNLOADED, status1.getStatus());
        assertTrue("Downloaded model should be verified", status1.isVerified());
        assertTrue("isDownloaded() should return true", status1.isDownloaded());
        assertFalse("isDownloading() should return false", status1.isDownloading());

        OfflineModelManager.ModelStatus status2 = new OfflineModelManager.ModelStatus(
            OfflineModelManager.ModelStatus.DOWNLOADING, false);
            
        assertEquals("Status should be DOWNLOADING", 
                    OfflineModelManager.ModelStatus.DOWNLOADING, status2.getStatus());
        assertFalse("Downloading model should not be verified", status2.isVerified());
        assertFalse("isDownloaded() should return false", status2.isDownloaded());
        assertTrue("isDownloading() should return true", status2.isDownloading());

        OfflineModelManager.ModelStatus status3 = new OfflineModelManager.ModelStatus(
            OfflineModelManager.ModelStatus.ERROR, false, "Network error");
            
        assertEquals("Status should be ERROR", 
                    OfflineModelManager.ModelStatus.ERROR, status3.getStatus());
        assertEquals("Error message should be preserved", 
                    "Network error", status3.getErrorMessage());
    }

    @Test
    public void testModelStatusConstants() {
        // Verify the constants are defined correctly
        assertEquals("DOWNLOADED constant", "downloaded", OfflineModelManager.ModelStatus.DOWNLOADED);
        assertEquals("NOT_DOWNLOADED constant", "not_downloaded", OfflineModelManager.ModelStatus.NOT_DOWNLOADED);
        assertEquals("DOWNLOADING constant", "downloading", OfflineModelManager.ModelStatus.DOWNLOADING);
        assertEquals("ERROR constant", "error", OfflineModelManager.ModelStatus.ERROR);
    }

    @Test
    public void testOfflineTranslationServiceIntegration() {
        // Test that OfflineTranslationService properly integrates with OfflineModelManager
        
        // Create both components
        OfflineModelManager modelManager = new OfflineModelManager(context);
        UserPreferences userPreferences = new UserPreferences(context);
        OfflineTranslationService translationService = new OfflineTranslationService(context, userPreferences);
        
        // Test the detailed model status method that uses getModelStatusMap
        Map<String, String> detailedStatus = translationService.getDetailedModelStatus();
        assertNotNull("Detailed status should not be null", detailedStatus);
        
        // Should contain common language models
        assertTrue("Should have status for English", detailedStatus.containsKey("en"));
        assertTrue("Should have status for Spanish", detailedStatus.containsKey("es"));
        
        // Status should be meaningful
        String englishStatus = detailedStatus.get("en");
        assertNotNull("English status should not be null", englishStatus);
        assertTrue("English status should indicate not downloaded", 
                  englishStatus.contains("not_downloaded"));
    }

    @Test
    public void testCompilationFix() {
        // This test verifies that the methods mentioned in the build error now exist
        // and can be called without compilation errors
        
        // These are the exact calls that were failing before:
        
        // Line 99-100 equivalent calls:
        String sourceLanguage = "en";
        String targetLanguage = "es";
        boolean sourceVerified = modelManager.isModelDownloadedAndVerified(sourceLanguage);
        boolean targetVerified = modelManager.isModelDownloadedAndVerified(targetLanguage);
        
        // Line 437 equivalent call:
        Map<String, OfflineModelManager.ModelStatus> managerStatus = modelManager.getModelStatusMap();
        
        // Line 441 equivalent call:
        String languageCode = "en";
        OfflineModelManager.ModelStatus managerStat = managerStatus.get(languageCode);
        
        // If we reach this point, compilation was successful
        assertTrue("Compilation test passed", true);
        
        // Verify the results make sense
        assertNotNull("Status map should not be null", managerStatus);
        assertNotNull("Status for 'en' should not be null", managerStat);
    }
}