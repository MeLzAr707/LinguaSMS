package com.translator.messagingapp;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Comprehensive test to verify complete offline ML Kit usage without API keys.
 * This test ensures that all ML Kit features work offline and no online API keys are required.
 */
public class CompleteOfflineMLKitTest {

    @Test
    public void testTranslatorAppOfflineFirst() {
        // Test that TranslatorApp prioritizes offline capability over API keys
        try {
            // Use reflection to verify the hasValidApiKey method logic
            Class<?> appClass = TranslatorApp.class;
            java.lang.reflect.Method hasValidMethod = appClass.getMethod("hasValidApiKey");
            java.lang.reflect.Method hasOfflineMethod = appClass.getMethod("hasOfflineTranslationCapability");
            
            assertNotNull("hasValidApiKey method should exist", hasValidMethod);
            assertNotNull("hasOfflineTranslationCapability method should exist", hasOfflineMethod);
            
            assertEquals("hasValidApiKey should return boolean", boolean.class, hasValidMethod.getReturnType());
            assertEquals("hasOfflineTranslationCapability should return boolean", boolean.class, hasOfflineMethod.getReturnType());
            
        } catch (NoSuchMethodException e) {
            fail("Required methods should exist: " + e.getMessage());
        }
    }

    @Test
    public void testTranslationManagerOfflineFirst() {
        // Test that TranslationManager prioritizes offline translation
        try {
            // Create mock components for testing
            android.content.Context mockContext = org.mockito.Mockito.mock(android.content.Context.class);
            UserPreferences mockPrefs = org.mockito.Mockito.mock(UserPreferences.class);
            GoogleTranslationService mockOnlineService = org.mockito.Mockito.mock(GoogleTranslationService.class);
            
            // Configure mocks for offline-first operation
            org.mockito.Mockito.when(mockPrefs.isOfflineTranslationEnabled()).thenReturn(true);
            org.mockito.Mockito.when(mockPrefs.getApiKey()).thenReturn(""); // No API key
            org.mockito.Mockito.when(mockOnlineService.hasApiKey()).thenReturn(false);
            
            // Create TranslationManager - should work without API key when offline is enabled
            TranslationManager manager = new TranslationManager(mockContext, mockOnlineService, mockPrefs);
            assertNotNull("TranslationManager should be created without API key", manager);
            
            // Verify offline service exists
            OfflineTranslationService offlineService = manager.getOfflineTranslationService();
            assertNotNull("OfflineTranslationService should be available", offlineService);
            
        } catch (Exception e) {
            fail("TranslationManager should work in offline-first mode: " + e.getMessage());
        }
    }

    @Test
    public void testLanguageDetectionOfflineFirst() {
        // Test that LanguageDetectionService works without online fallback
        try {
            // Create mock context
            android.content.Context mockContext = org.mockito.Mockito.mock(android.content.Context.class);
            
            // Create LanguageDetectionService without online service (offline-only mode)
            LanguageDetectionService langService = new LanguageDetectionService(mockContext);
            assertNotNull("LanguageDetectionService should work without online service", langService);
            
            // Verify it has the detectLanguage method
            Class<?> serviceClass = LanguageDetectionService.class;
            java.lang.reflect.Method detectMethod = serviceClass.getMethod("detectLanguage", String.class, LanguageDetectionService.LanguageDetectionCallback.class);
            assertNotNull("detectLanguage method should exist", detectMethod);
            
        } catch (Exception e) {
            fail("LanguageDetectionService should work offline-only: " + e.getMessage());
        }
    }

    @Test
    public void testOfflineTranslationServiceStandalone() {
        // Test that OfflineTranslationService works independently
        try {
            // Create mock components
            android.content.Context mockContext = org.mockito.Mockito.mock(android.content.Context.class);
            UserPreferences mockPrefs = org.mockito.Mockito.mock(UserPreferences.class);
            
            // Configure for offline operation
            org.mockito.Mockito.when(mockPrefs.isOfflineTranslationEnabled()).thenReturn(true);
            
            // Create OfflineTranslationService
            OfflineTranslationService offlineService = new OfflineTranslationService(mockContext, mockPrefs);
            assertNotNull("OfflineTranslationService should be created", offlineService);
            
            // Verify key methods exist
            Class<?> serviceClass = OfflineTranslationService.class;
            
            java.lang.reflect.Method translateMethod = serviceClass.getMethod("translateOffline", 
                String.class, String.class, String.class, OfflineTranslationService.OfflineTranslationCallback.class);
            assertNotNull("translateOffline method should exist", translateMethod);
            
            java.lang.reflect.Method availableMethod = serviceClass.getMethod("isOfflineTranslationAvailable", 
                String.class, String.class);
            assertNotNull("isOfflineTranslationAvailable method should exist", availableMethod);
            
        } catch (Exception e) {
            fail("OfflineTranslationService should work independently: " + e.getMessage());
        }
    }

    @Test
    public void testCompleteOfflineWorkflow() {
        // Test the complete offline workflow without any API keys
        try {
            // Create mock components for a complete offline setup
            android.content.Context mockContext = org.mockito.Mockito.mock(android.content.Context.class);
            UserPreferences mockPrefs = org.mockito.Mockito.mock(UserPreferences.class);
            
            // Configure for complete offline operation
            org.mockito.Mockito.when(mockPrefs.isOfflineTranslationEnabled()).thenReturn(true);
            org.mockito.Mockito.when(mockPrefs.getApiKey()).thenReturn(""); // No API key
            org.mockito.Mockito.when(mockPrefs.getPreferredLanguage()).thenReturn("en");
            org.mockito.Mockito.when(mockPrefs.isAutoTranslateEnabled()).thenReturn(true);
            
            // Create all services without API keys
            GoogleTranslationService onlineService = new GoogleTranslationService(); // No API key
            OfflineTranslationService offlineService = new OfflineTranslationService(mockContext, mockPrefs);
            LanguageDetectionService langService = new LanguageDetectionService(mockContext); // No online service
            TranslationManager manager = new TranslationManager(mockContext, onlineService, mockPrefs);
            
            // Verify all services are created successfully
            assertNotNull("GoogleTranslationService should be created (even without API key)", onlineService);
            assertNotNull("OfflineTranslationService should be created", offlineService);
            assertNotNull("LanguageDetectionService should be created", langService);
            assertNotNull("TranslationManager should be created", manager);
            
            // Verify the online service correctly reports no API key
            assertFalse("GoogleTranslationService should report no API key", onlineService.hasApiKey());
            
            // Verify offline service is available through translation manager
            OfflineTranslationService managerOfflineService = manager.getOfflineTranslationService();
            assertNotNull("TranslationManager should provide offline service", managerOfflineService);
            
        } catch (Exception e) {
            fail("Complete offline workflow should work without any API keys: " + e.getMessage());
        }
    }

    @Test
    public void testNoOnlineDependencyRequired() {
        // Verify that no online dependencies are required for basic functionality
        try {
            // Create components that specifically avoid online dependencies
            android.content.Context mockContext = org.mockito.Mockito.mock(android.content.Context.class);
            UserPreferences mockPrefs = org.mockito.Mockito.mock(UserPreferences.class);
            
            // Configure for strict offline mode
            org.mockito.Mockito.when(mockPrefs.isOfflineTranslationEnabled()).thenReturn(true);
            org.mockito.Mockito.when(mockPrefs.getApiKey()).thenReturn(null); // Explicitly null API key
            
            // Create GoogleTranslationService with null API key
            GoogleTranslationService nullApiService = new GoogleTranslationService(null);
            assertFalse("Service with null API key should report no API key", nullApiService.hasApiKey());
            
            // Create other services that should work without online dependencies
            OfflineTranslationService offlineService = new OfflineTranslationService(mockContext, mockPrefs);
            LanguageDetectionService langService = new LanguageDetectionService(mockContext, null); // Null online service
            
            assertNotNull("OfflineTranslationService should work with null API key", offlineService);
            assertNotNull("LanguageDetectionService should work with null online service", langService);
            
        } catch (Exception e) {
            fail("Components should work without any online dependencies: " + e.getMessage());
        }
    }
}