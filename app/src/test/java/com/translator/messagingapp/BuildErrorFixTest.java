package com.translator.messagingapp;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class to verify the build error fixes are working correctly.
 * This validates that the missing methods have been properly implemented.
 */
public class BuildErrorFixTest {

    @Test
    public void testUserPreferencesHasMessageTextSizeMethods() {
        try {
            // Verify that the getMessageTextSize method exists
            UserPreferences.class.getMethod("getMessageTextSize");
            assertTrue("getMessageTextSize method should exist in UserPreferences", true);
        } catch (NoSuchMethodException e) {
            fail("getMessageTextSize method should exist in UserPreferences: " + e.getMessage());
        }

        try {
            // Verify that the setMessageTextSize method exists
            UserPreferences.class.getMethod("setMessageTextSize", float.class);
            assertTrue("setMessageTextSize method should exist in UserPreferences", true);
        } catch (NoSuchMethodException e) {
            fail("setMessageTextSize method should exist in UserPreferences: " + e.getMessage());
        }
    }

    @Test
    public void testTextSizeManagerClassExists() {
        try {
            // Verify that TextSizeManager class exists
            Class.forName("com.translator.messagingapp.TextSizeManager");
            assertTrue("TextSizeManager class should exist", true);
        } catch (ClassNotFoundException e) {
            fail("TextSizeManager class should exist: " + e.getMessage());
        }
    }

    @Test
    public void testTextSizeManagerHasRequiredMethods() {
        try {
            // Verify that TextSizeManager has getCurrentTextSize method
            TextSizeManager.class.getMethod("getCurrentTextSize");
            assertTrue("getCurrentTextSize method should exist in TextSizeManager", true);
        } catch (NoSuchMethodException e) {
            fail("getCurrentTextSize method should exist in TextSizeManager: " + e.getMessage());
        }

        try {
            // Verify that TextSizeManager has setTextSize method
            TextSizeManager.class.getMethod("setTextSize", float.class);
            assertTrue("setTextSize method should exist in TextSizeManager", true);
        } catch (NoSuchMethodException e) {
            fail("setTextSize method should exist in TextSizeManager: " + e.getMessage());
        }
    }

    @Test
    public void testThemeCustomConstantExists() {
        try {
            // Verify that THEME_CUSTOM constant exists
            UserPreferences.class.getField("THEME_CUSTOM");
            assertTrue("THEME_CUSTOM constant should exist in UserPreferences", true);
        } catch (NoSuchFieldException e) {
            fail("THEME_CUSTOM constant should exist in UserPreferences: " + e.getMessage());
        }
    }

    @Test
    public void testThemeConstants() {
        // Test that all theme constants have the correct values
        assertEquals("THEME_LIGHT should be 0", 0, UserPreferences.THEME_LIGHT);
        assertEquals("THEME_DARK should be 1", 1, UserPreferences.THEME_DARK);
        assertEquals("THEME_BLACK_GLASS should be 2", 2, UserPreferences.THEME_BLACK_GLASS);
        assertEquals("THEME_SYSTEM should be 3", 3, UserPreferences.THEME_SYSTEM);
        assertEquals("THEME_CUSTOM should be 4", 4, UserPreferences.THEME_CUSTOM);
    }
}