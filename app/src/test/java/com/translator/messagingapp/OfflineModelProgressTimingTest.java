package com.translator.messagingapp;

import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Test to verify that the offline model download progress reaches 100% before success callback.
 * This test addresses issue #353 where the progress bar stalls before reaching 100%.
 */
@RunWith(RobolectricTestRunner.class)
public class OfflineModelProgressTimingTest {

    private OfflineModelManager modelManager;
    private Context context;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        modelManager = new OfflineModelManager(context);
    }

    @Test
    public void testProgressReaches100BeforeSuccess() throws InterruptedException {
        // Create a test model
        OfflineModelInfo testModel = new OfflineModelInfo("test", "Test Language", 1024 * 1024);
        
        // Track the progress values and success callback timing
        final AtomicInteger lastProgressBeforeSuccess = new AtomicInteger(-1);
        final AtomicReference<Boolean> successCalled = new AtomicReference<>(false);
        final CountDownLatch downloadComplete = new CountDownLatch(1);
        
        // Create a listener that tracks progress and success timing
        OfflineModelManager.DownloadListener listener = new OfflineModelManager.DownloadListener() {
            @Override
            public void onProgress(int progress) {
                // If success hasn't been called yet, update the last progress value
                if (!successCalled.get()) {
                    lastProgressBeforeSuccess.set(progress);
                }
            }
            
            @Override
            public void onSuccess() {
                // Mark success as called
                successCalled.set(true);
                downloadComplete.countDown();
            }
            
            @Override
            public void onError(String error) {
                fail("Download should not fail: " + error);
                downloadComplete.countDown();
            }
        };
        
        // Start the download
        modelManager.downloadModel(testModel, listener);
        
        // Wait for download to complete (with timeout)
        assertTrue("Download should complete within 15 seconds", 
                   downloadComplete.await(15, TimeUnit.SECONDS));
        
        // Verify that progress reached 100% before success was called
        assertEquals("Progress should reach 100% before success callback", 
                     100, lastProgressBeforeSuccess.get());
        
        // Verify the model state is correct after download
        assertTrue("Model should be marked as downloaded", testModel.isDownloaded());
        assertFalse("Model should not be in downloading state", testModel.isDownloading());
        assertEquals("Model progress should be 100%", 100, testModel.getDownloadProgress());
    }
    
    @Test
    public void testProgressUpdateLogicInAdapter() {
        // Create test adapter and model
        OfflineModelAdapter adapter = new OfflineModelAdapter(
                java.util.Arrays.asList(), 
                null
        );
        
        OfflineModelInfo testModel = new OfflineModelInfo("test", "Test Language", 1024 * 1024);
        java.util.List<OfflineModelInfo> models = new java.util.ArrayList<>();
        models.add(testModel);
        adapter.updateModels(models);
        
        // Test progress updates
        testModel.setDownloading(true);
        
        // Update to 50% - should remain downloading
        adapter.updateProgress(testModel, 50);
        assertEquals("Progress should be 50%", 50, testModel.getDownloadProgress());
        assertTrue("Model should still be downloading at 50%", testModel.isDownloading());
        
        // Update to 100% - should remain downloading until success callback
        adapter.updateProgress(testModel, 100);
        assertEquals("Progress should be 100%", 100, testModel.getDownloadProgress());
        assertTrue("Model should still be downloading at 100% until success callback", testModel.isDownloading());
        
        // Simulate success callback setting final state
        testModel.setDownloading(false);
        testModel.setDownloaded(true);
        assertFalse("Model should not be downloading after success", testModel.isDownloading());
        assertTrue("Model should be downloaded after success", testModel.isDownloaded());
    }
}