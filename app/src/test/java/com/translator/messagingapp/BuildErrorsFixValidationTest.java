package com.translator.messagingapp;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class to verify the build error fixes are working correctly.
 * This validates that the missing methods have been properly implemented.
 */
public class BuildErrorsFixValidationTest {

    @Test
    public void testUserPreferencesHasGetTargetLanguageMethod() {
        try {
            // Verify that the getTargetLanguage method exists and is accessible
            UserPreferences.class.getMethod("getTargetLanguage");
            assertTrue("getTargetLanguage method should exist in UserPreferences", true);
        } catch (NoSuchMethodException e) {
            fail("getTargetLanguage method should exist in UserPreferences: " + e.getMessage());
        }
    }

    @Test
    public void testOptimizedMessageCacheHasContextConstructor() {
        try {
            // Verify that the constructor with Context parameter exists
            OptimizedMessageCache.class.getConstructor(android.content.Context.class);
            assertTrue("OptimizedMessageCache should have constructor with Context parameter", true);
        } catch (NoSuchMethodException e) {
            fail("OptimizedMessageCache should have constructor with Context parameter: " + e.getMessage());
        }
    }

    @Test
    public void testOptimizedMessageCacheHasPerformMaintenanceMethod() {
        try {
            // Verify that the performMaintenance method exists
            OptimizedMessageCache.class.getMethod("performMaintenance");
            assertTrue("performMaintenance method should exist in OptimizedMessageCache", true);
        } catch (NoSuchMethodException e) {
            fail("performMaintenance method should exist in OptimizedMessageCache: " + e.getMessage());
        }
    }

    @Test 
    public void testOptimizedMessageCacheDefaultConstructorStillWorks() {
        try {
            // Verify that the default constructor still exists
            OptimizedMessageCache.class.getConstructor();
            assertTrue("OptimizedMessageCache should still have default constructor", true);
        } catch (NoSuchMethodException e) {
            fail("OptimizedMessageCache should still have default constructor: " + e.getMessage());
        }
    }
}