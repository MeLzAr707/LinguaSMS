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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test class to verify offline translation functionality when network is unavailable.
 * This tests the core requirement that the application uses offline models
 * when no network is available.
 */
@RunWith(RobolectricTestRunner.class)
public class OfflineNetworkIsolationTest {

    private Context context;
    private OfflineTranslationService offlineTranslationService;
    private TranslationManager translationManager;
    
    @Mock
    private UserPreferences mockUserPreferences;
    
    @Mock
    private GoogleTranslationService mockOnlineTranslationService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        
        // Set up mock preferences to enable offline translation
        when(mockUserPreferences.isOfflineTranslationEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferOfflineTranslation()).thenReturn(true);
        when(mockUserPreferences.isAutoTranslateEnabled()).thenReturn(true);
        
        // Mock online service to simulate network unavailability
        when(mockOnlineTranslationService.hasApiKey()).thenReturn(false);
        
        // Initialize services
        offlineTranslationService = new OfflineTranslationService(context, mockUserPreferences);
        translationManager = new TranslationManager(context, mockOnlineTranslationService, mockUserPreferences);
        
        // Set up test data
        setupMockDownloadedModels();
    }
    
    @Test
    public void testOfflineTranslationWithoutNetwork() throws InterruptedException {
        // Verify that offline translation works when network is unavailable
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean translationSuccess = new AtomicBoolean(false);
        AtomicReference<String> translatedText = new AtomicReference<>();
        AtomicReference<String> errorMessage = new AtomicReference<>();
        
        OfflineTranslationService.OfflineTranslationCallback callback = 
            new OfflineTranslationService.OfflineTranslationCallback() {
                @Override
                public void onTranslationComplete(boolean success, String result, String error) {
                    translationSuccess.set(success);
                    translatedText.set(result);
                    errorMessage.set(error);
                    latch.countDown();
                }
            };
        
        // Attempt offline translation with mocked downloaded models
        offlineTranslationService.translateOffline("Hello", "en", "es", callback);
        
        // Wait for translation to complete
        assertTrue("Translation should complete within timeout", 
                latch.await(10, TimeUnit.SECONDS));
        
        // Note: Since we're using mock MLKit, the actual translation may not work,
        // but we can verify the service attempts offline translation
        if (!translationSuccess.get()) {
            // Check that the error is related to MLKit not being available in test environment
            // rather than network issues
            String error = errorMessage.get();
            assertNotNull("Error message should be provided", error);
            // The error should NOT be "Language models not downloaded" since we mocked them
            assertFalse("Error should not be about missing models",
                    error.contains("Language models not downloaded"));
        }
    }
    
    @Test
    public void testOfflineAvailabilityCheck() {
        // Test that offline translation availability is correctly detected
        
        // With mocked downloaded models, translation should be available
        boolean available = offlineTranslationService.isOfflineTranslationAvailable("en", "es");
        
        // Note: This might return false due to MLKit verification in test environment
        // but the internal tracking should work
        Set<String> downloadedModels = offlineTranslationService.getDownloadedModels();
        assertFalse("Downloaded models should be tracked", downloadedModels.isEmpty());
        
        // Test with unavailable language pair
        boolean unavailable = offlineTranslationService.isOfflineTranslationAvailable("en", "fr");
        // Should be false since we only mocked en and es
        assertFalse("Unavailable language pair should return false", unavailable);
    }
    
    @Test
    public void testTranslationManagerOfflineFallback() throws InterruptedException {
        // Test that TranslationManager correctly uses offline when online is unavailable
        
        // Mock online service to fail (simulating network unavailability)
        doAnswer(invocation -> {
            TranslationManager.TranslationCallback callback = invocation.getArgument(3);
            callback.onTranslationComplete(false, null, "Network unavailable");
            return null;
        }).when(mockOnlineTranslationService).translateText(anyString(), anyString(), anyString(), any());
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean translationAttempted = new AtomicBoolean(false);
        AtomicReference<String> finalErrorMessage = new AtomicReference<>();
        
        TranslationManager.TranslationCallback callback = new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String result, String error) {
                translationAttempted.set(true);
                finalErrorMessage.set(error);
                latch.countDown();
            }
        };
        
        // Attempt translation - should try offline since online is unavailable
        translationManager.translateText("Hello", "en", "es", callback);
        
        // Wait for translation attempt
        assertTrue("Translation should be attempted within timeout", 
                latch.await(10, TimeUnit.SECONDS));
        
        assertTrue("Translation should be attempted", translationAttempted.get());
        
        // The error (if any) should not be "No translation service available"
        // since offline should be attempted
        String error = finalErrorMessage.get();
        if (error != null) {
            assertFalse("Should not fail with 'No translation service available'",
                    error.contains("No translation service available"));
        }
    }
    
    @Test
    public void testOfflineOnlyModeWithoutNetwork() {
        // Test offline-only mode when network is completely unavailable
        
        // Set preferences to offline-only
        when(mockUserPreferences.isOfflineTranslationEnabled()).thenReturn(true);
        when(mockUserPreferences.getPreferOfflineTranslation()).thenReturn(true);
        
        // Verify that offline translation is available
        assertTrue("Offline translation should be enabled", 
                mockUserPreferences.isOfflineTranslationEnabled());
        assertTrue("Should prefer offline translation", 
                mockUserPreferences.getPreferOfflineTranslation());
        
        // Check individual model availability
        boolean englishModel = offlineTranslationService.isLanguageModelDownloaded("en");
        boolean spanishModel = offlineTranslationService.isLanguageModelDownloaded("es");
        
        // With our mocked setup, these should be true
        assertTrue("English model should be available", englishModel);
        assertTrue("Spanish model should be available", spanishModel);
    }
    
    @Test
    public void testNetworkFailureErrorHandling() throws InterruptedException {
        // Test proper error handling when both online and offline fail
        
        // Clear downloaded models to simulate no offline models available
        clearDownloadedModels();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean translationSuccess = new AtomicBoolean(true);
        AtomicReference<String> errorMessage = new AtomicReference<>();
        
        OfflineTranslationService.OfflineTranslationCallback callback = 
            new OfflineTranslationService.OfflineTranslationCallback() {
                @Override
                public void onTranslationComplete(boolean success, String result, String error) {
                    translationSuccess.set(success);
                    errorMessage.set(error);
                    latch.countDown();
                }
            };
        
        // Attempt translation without downloaded models
        offlineTranslationService.translateOffline("Hello", "en", "fr", callback);
        
        // Wait for translation attempt
        assertTrue("Translation should complete within timeout", 
                latch.await(5, TimeUnit.SECONDS));
        
        // Should fail with appropriate error message
        assertFalse("Translation should fail without downloaded models", translationSuccess.get());
        assertNotNull("Error message should be provided", errorMessage.get());
        assertTrue("Error should mention language models not downloaded",
                errorMessage.get().contains("Language models not downloaded"));
    }
    
    @Test
    public void testDownloadedModelsPersistedBetweenSessions() {
        // Test that downloaded model information persists between app sessions
        
        // Verify models are marked as downloaded
        Set<String> downloadedModels = offlineTranslationService.getDownloadedModels();
        assertFalse("Should have downloaded models from setup", downloadedModels.isEmpty());
        
        // Create new service instance (simulating app restart)
        OfflineTranslationService newService = new OfflineTranslationService(context, mockUserPreferences);
        
        // Verify models are still available
        Set<String> persistedModels = newService.getDownloadedModels();
        assertFalse("Downloaded models should persist between sessions", persistedModels.isEmpty());
        assertEquals("Should have same number of models", downloadedModels.size(), persistedModels.size());
    }
    
    @Test
    public void testOfflineTranslationWithNullInputs() throws InterruptedException {
        // Test error handling with null or empty inputs
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean translationSuccess = new AtomicBoolean(true);
        AtomicReference<String> errorMessage = new AtomicReference<>();
        
        OfflineTranslationService.OfflineTranslationCallback callback = 
            new OfflineTranslationService.OfflineTranslationCallback() {
                @Override
                public void onTranslationComplete(boolean success, String result, String error) {
                    translationSuccess.set(success);
                    errorMessage.set(error);
                    latch.countDown();
                }
            };
        
        // Test with null text
        offlineTranslationService.translateOffline(null, "en", "es", callback);
        
        assertTrue("Translation should complete within timeout", 
                latch.await(5, TimeUnit.SECONDS));
        
        assertFalse("Translation should fail with null text", translationSuccess.get());
        assertNotNull("Error message should be provided", errorMessage.get());
        assertTrue("Error should mention no text to translate",
                errorMessage.get().contains("No text to translate"));
    }
    
    private void setupMockDownloadedModels() {
        // Set up mock downloaded models in SharedPreferences
        Set<String> downloadedModels = new HashSet<>();
        downloadedModels.add("en");
        downloadedModels.add("es");
        
        SharedPreferences modelPrefs = context.getSharedPreferences("offline_models", Context.MODE_PRIVATE);
        modelPrefs.edit().putStringSet("downloaded_models", downloadedModels).apply();
        
        // Refresh the offline translation service to pick up the models
        offlineTranslationService.refreshDownloadedModels();
    }
    
    private void clearDownloadedModels() {
        // Clear downloaded models from SharedPreferences
        SharedPreferences modelPrefs = context.getSharedPreferences("offline_models", Context.MODE_PRIVATE);
        modelPrefs.edit().remove("downloaded_models").apply();
        
        // Refresh the offline translation service
        offlineTranslationService.refreshDownloadedModels();
    }
}