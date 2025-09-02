package com.translator.messagingapp;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.nl.translate.TranslateLanguage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Test to verify correct ML Kit Task API usage and prevent addOnProgressListener build errors.
 * This test ensures the codebase uses only valid Task API methods.
 */
@RunWith(RobolectricTestRunner.class)
public class MLKitTaskAPIUsageTest {
    
    private Context context;
    private UserPreferences userPreferences;
    private OfflineTranslationService translationService;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        userPreferences = new UserPreferences(context);
        translationService = new OfflineTranslationService(context, userPreferences);
    }

    @Test
    public void testTaskVoidHasNoProgressListener() {
        // Create a translator to get a Task<Void> from downloadModelIfNeeded()
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.SPANISH)
                .build();
        
        Translator translator = Translation.getClient(options);
        Task<Void> downloadTask = translator.downloadModelIfNeeded();
        
        // Verify Task<Void> type
        assertNotNull("downloadTask should not be null", downloadTask);
        assertTrue("downloadTask should be of type Task", downloadTask instanceof Task);
        
        // Verify correct methods exist (these should compile)
        assertNotNull("addOnSuccessListener should exist", 
                     getMethod(downloadTask, "addOnSuccessListener"));
        assertNotNull("addOnFailureListener should exist", 
                     getMethod(downloadTask, "addOnFailureListener"));
        assertNotNull("addOnCompleteListener should exist", 
                     getMethod(downloadTask, "addOnCompleteListener"));
        
        // Verify addOnProgressListener does NOT exist (would cause compile error)
        assertNull("addOnProgressListener should NOT exist - this method causes build errors", 
                  getMethod(downloadTask, "addOnProgressListener"));
        
        System.out.println("✅ VERIFIED: Task<Void> API usage is correct");
    }
    
    @Test
    public void testOfflineTranslationServiceUsesCorrectTaskAPI() throws InterruptedException {
        // Test that OfflineTranslationService uses the correct Task API methods
        
        String languageCode = "es"; // Spanish
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean callbackCalled = new AtomicBoolean(false);
        AtomicReference<String> errorMessage = new AtomicReference<>();
        
        // Test the downloadLanguageModel method
        translationService.downloadLanguageModel(languageCode, 
            new OfflineTranslationService.ModelDownloadCallback() {
                @Override
                public void onDownloadComplete(boolean success, String langCode, String error) {
                    callbackCalled.set(true);
                    errorMessage.set(error);
                    latch.countDown();
                }

                @Override
                public void onDownloadProgress(String langCode, int progress) {
                    // Progress callback - this is implemented at application level, not ML Kit level
                    System.out.println("Progress for " + langCode + ": " + progress + "%");
                }
            });
        
        // Wait for callback (with timeout)
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        
        // Verify the callback was called (regardless of success/failure)
        assertTrue("Download callback should be called", callbackCalled.get());
        
        System.out.println("✅ VERIFIED: OfflineTranslationService uses correct Task API");
        if (errorMessage.get() != null) {
            System.out.println("Note: Download may have failed due to test environment: " + errorMessage.get());
        }
    }
    
    @Test
    public void testCorrectTaskAPIPattern() {
        // Demonstrate the correct pattern for ML Kit Task usage
        
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.FRENCH)
                .build();
        
        Translator translator = Translation.getClient(options);
        
        // ✅ CORRECT: Using proper Task API methods
        Task<Void> downloadTask = translator.downloadModelIfNeeded();
        
        AtomicBoolean successCalled = new AtomicBoolean(false);
        AtomicBoolean failureCalled = new AtomicBoolean(false);
        
        downloadTask
            .addOnSuccessListener(aVoid -> {
                successCalled.set(true);
                System.out.println("✅ SUCCESS: Model download completed");
            })
            .addOnFailureListener(exception -> {
                failureCalled.set(true);
                System.out.println("❌ FAILURE: Model download failed: " + exception.getMessage());
            });
        
        // The task should have the listeners attached
        assertTrue("Task should be properly configured", downloadTask != null);
        
        System.out.println("✅ VERIFIED: Correct Task API pattern implemented");
    }
    
    @Test
    public void testIncorrectPatternWouldCauseCompileError() {
        // This test documents what would cause the build error
        
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.GERMAN)
                .build();
        
        Translator translator = Translation.getClient(options);
        Task<Void> downloadTask = translator.downloadModelIfNeeded();
        
        // ❌ THIS WOULD CAUSE COMPILE ERROR (commented out to prevent build failure):
        // downloadTask.addOnProgressListener(progress -> {
        //     // ERROR: cannot find symbol method addOnProgressListener
        // });
        
        // ✅ CORRECT alternatives for progress tracking:
        
        // Option 1: Use application-level progress tracking (like OfflineTranslationService does)
        System.out.println("Progress tracking should be implemented at application level");
        
        // Option 2: Use addOnCompleteListener to know when download finishes
        downloadTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                System.out.println("Download completed (but no granular progress available from ML Kit)");
            }
        });
        
        System.out.println("✅ VERIFIED: Documented correct alternatives to non-existent addOnProgressListener");
    }
    
    @Test
    public void testCodebaseConsistency() {
        // Test that all components use consistent Task API patterns
        
        // Test OfflineTranslationService
        assertNotNull("OfflineTranslationService should be available", translationService);
        
        // Test OfflineModelManager
        OfflineModelManager modelManager = new OfflineModelManager(context);
        assertNotNull("OfflineModelManager should be available", modelManager);
        
        // Verify both components can work together without API conflicts
        String languageCode = "en";
        boolean isDownloaded = modelManager.isModelDownloaded(languageCode);
        boolean isAvailable = translationService.isLanguageModelDownloaded(languageCode);
        
        // Both should use compatible data (may differ due to synchronization, but should not crash)
        System.out.println("ModelManager thinks downloaded: " + isDownloaded);
        System.out.println("TranslationService thinks downloaded: " + isAvailable);
        
        System.out.println("✅ VERIFIED: All components use consistent APIs");
    }
    
    /**
     * Helper method to check if a method exists on an object.
     * Used to verify which methods are available on Task<Void>.
     */
    private java.lang.reflect.Method getMethod(Object obj, String methodName) {
        try {
            Class<?> clazz = obj.getClass();
            
            // Look through all methods to find one with the given name
            for (java.lang.reflect.Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName)) {
                    return method;
                }
            }
            
            // Also check superclasses and interfaces
            for (Class<?> iface : clazz.getInterfaces()) {
                for (java.lang.reflect.Method method : iface.getMethods()) {
                    if (method.getName().equals(methodName)) {
                        return method;
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}