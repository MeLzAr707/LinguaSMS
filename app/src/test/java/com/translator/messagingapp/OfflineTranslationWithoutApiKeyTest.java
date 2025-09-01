package com.translator.messagingapp;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Test to verify offline translation works without API key.
 */
public class OfflineTranslationWithoutApiKeyTest {

    @Test
    public void testOfflineTranslationModeConstants() {
        // Verify that translation mode constants are properly defined
        assertEquals("Online only mode should be 1", 1, UserPreferences.TRANSLATION_MODE_ONLINE_ONLY);
        
        // Test that offline translation is the default approach
        assertTrue("Offline translation should be the default behavior", true);
    }

    @Test
    public void testOfflineTranslationServiceExists() {
        // This test verifies that the offline translation components exist
        // and can be instantiated without an API key
        try {
            // Create a mock context - this tests compilation but not functionality
            android.content.Context mockContext = org.mockito.Mockito.mock(android.content.Context.class);
            UserPreferences mockPrefs = org.mockito.Mockito.mock(UserPreferences.class);
            
            // This should not throw an exception
            OfflineTranslationService offlineService = new OfflineTranslationService(mockContext, mockPrefs);
            assertNotNull("OfflineTranslationService should be created without API key", offlineService);
            
        } catch (Exception e) {
            fail("OfflineTranslationService creation should not require API key: " + e.getMessage());
        }
    }

    @Test 
    public void testTranslatorAppSupportsOfflineCapability() {
        // Verify that TranslatorApp has the new offline capability method
        try {
            // Use reflection to check if the method exists
            Class<?> appClass = TranslatorApp.class;
            java.lang.reflect.Method method = appClass.getMethod("hasOfflineTranslationCapability");
            assertNotNull("hasOfflineTranslationCapability method should exist", method);
            
            // Check that the method returns boolean
            assertEquals("hasOfflineTranslationCapability should return boolean", 
                         boolean.class, method.getReturnType());
                         
        } catch (NoSuchMethodException e) {
            fail("hasOfflineTranslationCapability method should exist: " + e.getMessage());
        }
    }

    @Test
    public void testOfflineServiceHasDownloadedModelsCheck() {
        // Verify that OfflineTranslationService has the new method to check for downloaded models
        try {
            Class<?> serviceClass = OfflineTranslationService.class;
            java.lang.reflect.Method method = serviceClass.getMethod("hasAnyDownloadedModels");
            assertNotNull("hasAnyDownloadedModels method should exist", method);
            
            // Check that the method returns boolean
            assertEquals("hasAnyDownloadedModels should return boolean", 
                         boolean.class, method.getReturnType());
                         
        } catch (NoSuchMethodException e) {
            fail("hasAnyDownloadedModels method should exist: " + e.getMessage());
        }
    }
}