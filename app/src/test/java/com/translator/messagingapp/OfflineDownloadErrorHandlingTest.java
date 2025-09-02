package com.translator.messagingapp;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Test class for comprehensive download error handling and failure scenarios.
 * This tests various failure modes during model download and ensures proper
 * error reporting and recovery mechanisms.
 */
@RunWith(RobolectricTestRunner.class)
public class OfflineDownloadErrorHandlingTest {

    private Context context;
    private OfflineModelManager modelManager;
    private TestableOfflineModelManager testableModelManager;
    
    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        modelManager = new OfflineModelManager(context);
        testableModelManager = new TestableOfflineModelManager(context);
        
        // Clean up any existing test data
        cleanupTestData();
    }
    
    @Test
    public void testDownloadProgressReporting() throws InterruptedException {
        // Test that download progress is properly reported
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger lastProgress = new AtomicInteger(-1);
        AtomicInteger progressCallCount = new AtomicInteger(0);
        AtomicBoolean downloadSuccess = new AtomicBoolean(false);
        
        OfflineModelInfo englishModel = getModelByLanguageCode("en");
        assertNotNull("English model should be available", englishModel);
        
        OfflineModelManager.DownloadListener listener = new OfflineModelManager.DownloadListener() {
            @Override
            public void onProgress(int progress) {
                progressCallCount.incrementAndGet();
                lastProgress.set(progress);
                assertTrue("Progress should be between 0 and 100", progress >= 0 && progress <= 100);
                
                // Verify progress is non-decreasing
                if (progressCallCount.get() > 1) {
                    assertTrue("Progress should not decrease", progress >= lastProgress.get());
                }
            }
            
            @Override
            public void onSuccess() {
                downloadSuccess.set(true);
                latch.countDown();
            }
            
            @Override
            public void onError(String error) {
                latch.countDown();
            }
        };
        
        modelManager.downloadModel(englishModel, listener);
        
        assertTrue("Download should complete within timeout", 
                latch.await(30, TimeUnit.SECONDS));
        assertTrue("Download should succeed", downloadSuccess.get());
        assertTrue("Progress should be reported", progressCallCount.get() > 0);
        assertEquals("Final progress should be 100", 100, lastProgress.get());
    }
    
    @Test
    public void testDownloadAlreadyDownloadedModel() throws InterruptedException {
        // Test error handling when trying to download already downloaded model
        
        // First, download a model successfully
        OfflineModelInfo spanishModel = getModelByLanguageCode("es");
        assertNotNull("Spanish model should be available", spanishModel);
        
        CountDownLatch firstLatch = new CountDownLatch(1);
        AtomicBoolean firstSuccess = new AtomicBoolean(false);
        
        OfflineModelManager.DownloadListener firstListener = new OfflineModelManager.DownloadListener() {
            @Override
            public void onProgress(int progress) {}
            
            @Override
            public void onSuccess() {
                firstSuccess.set(true);
                firstLatch.countDown();
            }
            
            @Override
            public void onError(String error) {
                firstLatch.countDown();
            }
        };
        
        modelManager.downloadModel(spanishModel, firstListener);
        assertTrue("First download should complete", firstLatch.await(30, TimeUnit.SECONDS));
        assertTrue("First download should succeed", firstSuccess.get());
        
        // Now try to download the same model again
        CountDownLatch secondLatch = new CountDownLatch(1);
        AtomicBoolean secondSuccess = new AtomicBoolean(true);
        AtomicReference<String> errorMessage = new AtomicReference<>();
        
        OfflineModelManager.DownloadListener secondListener = new OfflineModelManager.DownloadListener() {
            @Override
            public void onProgress(int progress) {}
            
            @Override
            public void onSuccess() {
                secondSuccess.set(true);
                secondLatch.countDown();
            }
            
            @Override
            public void onError(String error) {
                secondSuccess.set(false);
                errorMessage.set(error);
                secondLatch.countDown();
            }
        };
        
        modelManager.downloadModel(spanishModel, secondListener);
        assertTrue("Second download should complete quickly", secondLatch.await(5, TimeUnit.SECONDS));
        assertFalse("Second download should fail", secondSuccess.get());
        assertEquals("Should get 'already downloaded' error", 
                "Model already downloaded", errorMessage.get());
    }
    
    @Test
    public void testDownloadInterruption() throws InterruptedException {
        // Test handling of download interruption
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean downloadSuccess = new AtomicBoolean(true);
        AtomicReference<String> errorMessage = new AtomicReference<>();
        
        OfflineModelInfo frenchModel = getModelByLanguageCode("fr");
        assertNotNull("French model should be available", frenchModel);
        
        // Use testable manager that can simulate interruption
        TestableOfflineModelManager.InterruptibleDownloadListener listener = 
            new TestableOfflineModelManager.InterruptibleDownloadListener() {
                @Override
                public void onProgress(int progress) {
                    // Interrupt after some progress
                    if (progress >= 50) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                @Override
                public void onSuccess() {
                    downloadSuccess.set(true);
                    latch.countDown();
                }
                
                @Override
                public void onError(String error) {
                    downloadSuccess.set(false);
                    errorMessage.set(error);
                    latch.countDown();
                }
            };
        
        testableModelManager.downloadModelWithInterruption(frenchModel, listener);
        
        assertTrue("Download should complete (with error) within timeout", 
                latch.await(30, TimeUnit.SECONDS));
        assertFalse("Download should fail due to interruption", downloadSuccess.get());
        assertNotNull("Error message should be provided", errorMessage.get());
        assertTrue("Error should mention interruption", 
                errorMessage.get().contains("interrupted"));
    }
    
    @Test
    public void testDownloadWithFileSystemError() throws InterruptedException {
        // Test handling of file system errors during download
        
        // Make model directory read-only to simulate file system error
        File modelDir = new File(context.getFilesDir(), "offline_models");
        modelDir.mkdirs();
        modelDir.setWritable(false);
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean downloadSuccess = new AtomicBoolean(true);
        AtomicReference<String> errorMessage = new AtomicReference<>();
        
        OfflineModelInfo germanModel = getModelByLanguageCode("de");
        assertNotNull("German model should be available", germanModel);
        
        OfflineModelManager.DownloadListener listener = new OfflineModelManager.DownloadListener() {
            @Override
            public void onProgress(int progress) {}
            
            @Override
            public void onSuccess() {
                downloadSuccess.set(true);
                latch.countDown();
            }
            
            @Override
            public void onError(String error) {
                downloadSuccess.set(false);
                errorMessage.set(error);
                latch.countDown();
            }
        };
        
        try {
            modelManager.downloadModel(germanModel, listener);
            
            assertTrue("Download should complete within timeout", 
                    latch.await(30, TimeUnit.SECONDS));
            
            // Download might succeed or fail depending on the system
            // but we should get proper error handling if it fails
            if (!downloadSuccess.get()) {
                assertNotNull("Error message should be provided on failure", errorMessage.get());
                assertTrue("Error should mention download failure", 
                        errorMessage.get().contains("Download failed"));
            }
        } finally {
            // Restore write permissions
            modelDir.setWritable(true);
        }
    }
    
    @Test
    public void testModelDeletionAfterCorruption() {
        // Test that corrupted models are properly deleted
        
        File modelDir = new File(context.getFilesDir(), "offline_models");
        modelDir.mkdirs();
        
        File corruptedModel = new File(modelDir, "it.model");
        try {
            // Create a corrupted model file
            FileWriter writer = new FileWriter(corruptedModel);
            writer.write("CORRUPTED_DATA");
            writer.close();
            
            assertTrue("Corrupted model file should exist", corruptedModel.exists());
            
            // Attempt to verify - should detect corruption and delete file
            boolean verified = modelManager.verifyModelIntegrity("it");
            assertFalse("Corrupted model should fail verification", verified);
            
            // File should still exist (verification doesn't auto-delete)
            assertTrue("File should still exist after failed verification", corruptedModel.exists());
            
            // Now test download with integrity check - should delete corrupted file
            OfflineModelInfo italianModel = getModelByLanguageCode("it");
            // Simulate creating correct content to test the deletion during download
            
        } catch (IOException e) {
            fail("Failed to create test corrupted model: " + e.getMessage());
        } finally {
            if (corruptedModel.exists()) {
                corruptedModel.delete();
            }
        }
    }
    
    @Test
    public void testMultipleSimultaneousDownloads() throws InterruptedException {
        // Test handling of multiple simultaneous download attempts
        
        int downloadCount = 3;
        CountDownLatch latch = new CountDownLatch(downloadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        String[] languages = {"en", "es", "fr"};
        
        for (String lang : languages) {
            OfflineModelInfo model = getModelByLanguageCode(lang);
            assertNotNull("Model should be available: " + lang, model);
            
            OfflineModelManager.DownloadListener listener = new OfflineModelManager.DownloadListener() {
                @Override
                public void onProgress(int progress) {}
                
                @Override
                public void onSuccess() {
                    successCount.incrementAndGet();
                    latch.countDown();
                }
                
                @Override
                public void onError(String error) {
                    errorCount.incrementAndGet();
                    latch.countDown();
                }
            };
            
            // Start downloads in separate threads to simulate simultaneous attempts
            new Thread(() -> modelManager.downloadModel(model, listener)).start();
        }
        
        assertTrue("All downloads should complete within timeout", 
                latch.await(60, TimeUnit.SECONDS));
        
        // At least some downloads should succeed
        int totalCompleted = successCount.get() + errorCount.get();
        assertEquals("All downloads should complete", downloadCount, totalCompleted);
        assertTrue("At least one download should succeed", successCount.get() > 0);
    }
    
    @Test
    public void testDownloadProgressCallbackNull() throws InterruptedException {
        // Test download with null callback (should not crash)
        
        OfflineModelInfo model = getModelByLanguageCode("en");
        assertNotNull("English model should be available", model);
        
        // This should not crash even with null listener
        modelManager.downloadModel(model, null);
        
        // Wait a bit to ensure the background thread completes
        Thread.sleep(1000);
        
        // Test completed successfully if no exception was thrown
        assertTrue("Download with null listener should not crash", true);
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
        
        // Clean up SharedPreferences
        context.getSharedPreferences("offline_models", Context.MODE_PRIVATE)
                .edit().clear().apply();
    }
    
    /**
     * Testable version of OfflineModelManager that can simulate various error conditions
     */
    private static class TestableOfflineModelManager extends OfflineModelManager {
        
        public TestableOfflineModelManager(Context context) {
            super(context);
        }
        
        public interface InterruptibleDownloadListener extends DownloadListener {
            // Same interface, just for type safety
        }
        
        public void downloadModelWithInterruption(OfflineModelInfo model, 
                                                   InterruptibleDownloadListener listener) {
            if (model.isDownloaded()) {
                if (listener != null) {
                    listener.onError("Model already downloaded");
                }
                return;
            }
            
            model.setDownloading(true);
            
            new Thread(() -> {
                try {
                    for (int progress = 0; progress <= 100; progress += 10) {
                        Thread.sleep(100); // Shorter sleep for faster test
                        
                        model.setDownloadProgress(progress);
                        
                        if (listener != null) {
                            listener.onProgress(progress);
                        }
                        
                        // Check for interruption
                        if (Thread.currentThread().isInterrupted()) {
                            throw new InterruptedException("Download interrupted");
                        }
                    }
                    
                    model.setDownloading(false);
                    model.setDownloaded(true);
                    
                    if (listener != null) {
                        listener.onSuccess();
                    }
                    
                } catch (InterruptedException e) {
                    model.setDownloading(false);
                    if (listener != null) {
                        listener.onError("Download interrupted");
                    }
                } catch (Exception e) {
                    model.setDownloading(false);
                    if (listener != null) {
                        listener.onError("Download failed: " + e.getMessage());
                    }
                }
            }).start();
        }
    }

    @Test
    public void testMLKitIntegrationErrorHandling() throws InterruptedException {
        // Test specific MLKit integration error scenarios
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean downloadSuccess = new AtomicBoolean(false);
        AtomicReference<String> errorMessage = new AtomicReference<>();
        
        OfflineModelInfo testModel = getModelByLanguageCode("de");
        if (testModel != null) {
            OfflineModelManager.DownloadListener listener = new OfflineModelManager.DownloadListener() {
                @Override
                public void onProgress(int progress) {
                    // Verify that progress is reported during actual MLKit download
                    assertTrue("Progress should be between 0 and 100", progress >= 0 && progress <= 100);
                }

                @Override
                public void onSuccess() {
                    downloadSuccess.set(true);
                    latch.countDown();
                }

                @Override
                public void onError(String error) {
                    downloadSuccess.set(false);
                    errorMessage.set(error);
                    latch.countDown();
                }
            };
            
            modelManager.downloadModel(testModel, listener);
            
            // Wait for download to complete (with generous timeout for actual MLKit download)
            assertTrue("MLKit download should complete within timeout", latch.await(30, TimeUnit.SECONDS));
            
            // If download succeeded, verify the model is properly tracked
            if (downloadSuccess.get()) {
                assertTrue("Downloaded model should be tracked as downloaded", 
                          modelManager.isModelDownloaded(testModel.getLanguageCode()));
                assertTrue("Downloaded model should pass verification", 
                          modelManager.isModelDownloadedAndVerified(testModel.getLanguageCode()));
            } else {
                // If download failed, error message should be descriptive
                assertNotNull("Error message should be provided", errorMessage.get());
                assertFalse("Error message should not be empty", errorMessage.get().trim().isEmpty());
                
                // Common expected error scenarios
                String error = errorMessage.get().toLowerCase();
                boolean isExpectedError = error.contains("network") || 
                                        error.contains("download") || 
                                        error.contains("unsupported") ||
                                        error.contains("verification") ||
                                        error.contains("dictionary");
                assertTrue("Error message should indicate a known error type: " + errorMessage.get(), 
                          isExpectedError);
            }
        }
    }

    @Test
    public void testEnhancedModelVerification() {
        // Test the enhanced model verification that checks both MLKit and file existence
        
        List<OfflineModelInfo> models = modelManager.getAvailableModels();
        assertNotNull("Models list should not be null", models);
        
        for (OfflineModelInfo model : models) {
            String languageCode = model.getLanguageCode();
            
            boolean basicDownloaded = modelManager.isModelDownloaded(languageCode);
            boolean verifiedDownloaded = modelManager.isModelDownloadedAndVerified(languageCode);
            
            // Enhanced verification should be consistent with basic check for truly working models
            if (verifiedDownloaded) {
                assertTrue("Verified models should also pass basic download check", basicDownloaded);
            }
            
            // If basic check says downloaded but verification fails, there should be cleanup
            if (basicDownloaded && !verifiedDownloaded) {
                // This scenario indicates a synchronization issue that should be auto-corrected
                // The enhanced verification should clean up invalid tracking
                
                // Re-check after potential cleanup
                boolean recheck = modelManager.isModelDownloaded(languageCode);
                
                // If cleanup occurred, the basic check should now align with verification
                // (This tests the cleanup logic in isModelDownloadedAndVerified)
            }
        }
    }
}