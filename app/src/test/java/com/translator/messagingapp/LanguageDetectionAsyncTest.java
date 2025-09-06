package com.translator.messagingapp;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test class to verify the async language detection functionality.
 * Specifically tests the CompletableFuture fix for the thenAccept compilation error.
 */
@RunWith(AndroidJUnit4.class)
public class LanguageDetectionAsyncTest {

    @Mock
    private GoogleTranslationService mockOnlineService;

    private LanguageDetectionService languageDetectionService;
    private Context context;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = ApplicationProvider.getApplicationContext();
        languageDetectionService = new LanguageDetectionService(context, mockOnlineService);
    }

    @Test
    public void testAsyncDetectionCallback_withOnlineSuccess() throws InterruptedException {
        // Setup mock to simulate online service availability and successful detection
        when(mockOnlineService.hasApiKey()).thenReturn(true);
        
        // Create a mock Future that returns "en" for English
        Future<String> mockFuture = CompletableFuture.completedFuture("en");
        when(mockOnlineService.detectLanguageAsync(anyString())).thenReturn(mockFuture);

        // Setup countdown latch to wait for async callback
        CountDownLatch latch = new CountDownLatch(1);
        final String[] resultLanguage = new String[1];
        final Boolean[] resultSuccess = new Boolean[1];

        // Create callback to capture results
        LanguageDetectionService.DetailedLanguageDetectionCallback callback = 
            new LanguageDetectionService.DetailedLanguageDetectionCallback() {
                @Override
                public void onDetectionComplete(boolean success, String languageCode, 
                                              String errorMessage, LanguageDetectionService.DetectionMethod method) {
                    resultSuccess[0] = success;
                    resultLanguage[0] = languageCode;
                    latch.countDown();
                }
            };

        // Call the async detection method
        languageDetectionService.detectLanguage("Hello world", callback);

        // Wait for callback (with timeout)
        boolean callbackReceived = latch.await(5, TimeUnit.SECONDS);
        
        assertTrue("Callback should be received within timeout", callbackReceived);
        assertTrue("Detection should be successful", resultSuccess[0]);
        assertEquals("Language should be detected as English", "en", resultLanguage[0]);
    }

    @Test
    public void testAsyncDetectionCallback_withOnlineFailure() throws InterruptedException {
        // Setup mock to simulate online service failure
        when(mockOnlineService.hasApiKey()).thenReturn(true);
        
        // Create a mock Future that throws an exception
        Future<String> mockFuture = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("Network error");
        });
        when(mockOnlineService.detectLanguageAsync(anyString())).thenReturn(mockFuture);

        // Setup countdown latch to wait for async callback
        CountDownLatch latch = new CountDownLatch(1);
        final Boolean[] resultSuccess = new Boolean[1];
        final String[] resultError = new String[1];

        // Create callback to capture results
        LanguageDetectionService.DetailedLanguageDetectionCallback callback = 
            new LanguageDetectionService.DetailedLanguageDetectionCallback() {
                @Override
                public void onDetectionComplete(boolean success, String languageCode, 
                                              String errorMessage, LanguageDetectionService.DetectionMethod method) {
                    resultSuccess[0] = success;
                    resultError[0] = errorMessage;
                    latch.countDown();
                }
            };

        // Call the async detection method
        languageDetectionService.detectLanguage("Hello world", callback);

        // Wait for callback (with timeout)
        boolean callbackReceived = latch.await(5, TimeUnit.SECONDS);
        
        assertTrue("Callback should be received within timeout", callbackReceived);
        assertFalse("Detection should fail", resultSuccess[0]);
        assertNotNull("Error message should be provided", resultError[0]);
    }

    @Test
    public void testAsyncDetectionCallback_noOnlineService() throws InterruptedException {
        // Test with no online service available
        when(mockOnlineService.hasApiKey()).thenReturn(false);

        // Setup countdown latch to wait for async callback
        CountDownLatch latch = new CountDownLatch(1);
        final Boolean[] resultSuccess = new Boolean[1];

        // Create callback to capture results
        LanguageDetectionService.DetailedLanguageDetectionCallback callback = 
            new LanguageDetectionService.DetailedLanguageDetectionCallback() {
                @Override
                public void onDetectionComplete(boolean success, String languageCode, 
                                              String errorMessage, LanguageDetectionService.DetectionMethod method) {
                    resultSuccess[0] = success;
                    latch.countDown();
                }
            };

        // Call the async detection method
        languageDetectionService.detectLanguage("Hello world", callback);

        // Wait for callback (with timeout)
        boolean callbackReceived = latch.await(5, TimeUnit.SECONDS);
        
        assertTrue("Callback should be received within timeout", callbackReceived);
        // Result depends on whether ML Kit succeeds, but callback should be called
        assertNotNull("Success status should be set", resultSuccess[0]);
    }
}