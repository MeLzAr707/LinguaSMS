package com.translator.messagingapp;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Test to verify that first-time language model downloads work correctly
 * with improved timeout handling and network conditions.
 */
@RunWith(AndroidJUnit4.class)
public class FirstTimeDownloadTest {

    private OfflineModelManager modelManager;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        modelManager = new OfflineModelManager(context);
    }

    @Test
    public void testFirstTimeDownloadUsesLongerTimeout() throws InterruptedException {
        // This test verifies that the system handles first-time downloads
        // with appropriate timeout values
        
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] successHolder = new boolean[1];
        final String[] errorHolder = new String[1];
        final long[] startTime = new long[1];
        
        startTime[0] = System.currentTimeMillis();
        
        // Test with a commonly supported language
        modelManager.downloadModel("es", new OfflineModelManager.DownloadListener() {
            @Override
            public void onProgress(int progress) {
                // Progress updates should be received
                assertTrue("Progress should be between 0 and 100", progress >= 0 && progress <= 100);
            }

            @Override
            public void onSuccess() {
                successHolder[0] = true;
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                errorHolder[0] = errorMessage;
                latch.countDown();
            }
        });

        // Wait for the download attempt to complete
        // Use longer timeout since first downloads can take time
        boolean completed = latch.await(150, TimeUnit.SECONDS);
        
        long elapsedTime = System.currentTimeMillis() - startTime[0];
        
        // The test should complete within reasonable time
        assertTrue("Download test should complete within timeout", completed);
        
        // If it failed, it should not be due to network restrictions
        if (!successHolder[0] && errorHolder[0] != null) {
            // Error message should not indicate WiFi requirement issues
            assertFalse("Should not fail due to WiFi requirement", 
                       errorHolder[0].toLowerCase().contains("wifi"));
            
            // Should provide helpful message for first-time downloads
            if (errorHolder[0].contains("timeout")) {
                assertTrue("Should provide helpful message for first-time timeouts",
                          errorHolder[0].contains("First downloads may take longer"));
            }
        }
        
        // Log results for debugging
        System.out.println("Download test completed in " + elapsedTime + "ms");
        if (successHolder[0]) {
            System.out.println("Download succeeded");
        } else {
            System.out.println("Download failed: " + errorHolder[0]);
        }
    }

    @Test
    public void testDownloadWithoutWifiRestriction() throws InterruptedException {
        // This test verifies that downloads don't require WiFi
        
        CountDownLatch latch = new CountDownLatch(1);
        final String[] errorHolder = new String[1];
        
        // Attempt to download a model
        modelManager.downloadModel("fr", new OfflineModelManager.DownloadListener() {
            @Override
            public void onProgress(int progress) {
                // Progress is good
            }

            @Override
            public void onSuccess() {
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                errorHolder[0] = errorMessage;
                latch.countDown();
            }
        });

        // Wait for completion
        boolean completed = latch.await(150, TimeUnit.SECONDS);
        assertTrue("Download attempt should complete", completed);
        
        // If it failed, it should not be due to network connection type
        if (errorHolder[0] != null) {
            assertFalse("Should not fail due to WiFi requirement", 
                       errorHolder[0].toLowerCase().contains("wifi"));
            assertFalse("Should not fail due to connection type", 
                       errorHolder[0].toLowerCase().contains("connection"));
        }
    }

    @Test
    public void testUnsupportedLanguageHandling() throws InterruptedException {
        // Test error handling for unsupported languages
        
        CountDownLatch latch = new CountDownLatch(1);
        final String[] errorHolder = new String[1];
        final boolean[] successHolder = new boolean[1];
        
        // Try to download an unsupported language
        modelManager.downloadModel("invalid-lang", new OfflineModelManager.DownloadListener() {
            @Override
            public void onProgress(int progress) {
                // Should not get progress for invalid language
            }

            @Override
            public void onSuccess() {
                successHolder[0] = true;
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                errorHolder[0] = errorMessage;
                latch.countDown();
            }
        });

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue("Error handling should complete quickly", completed);
        assertFalse("Should not succeed for invalid language", successHolder[0]);
        assertNotNull("Should provide error message", errorHolder[0]);
        assertTrue("Error should mention unsupported language", 
                  errorHolder[0].toLowerCase().contains("unsupported"));
    }
}