package com.translator.messagingapp;

import android.content.Context;
import android.graphics.Color;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test for custom theme color wheel functionality
 */
@RunWith(RobolectricTestRunner.class)
public class CustomThemeColorTest {
    private UserPreferences userPreferences;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        userPreferences = new UserPreferences(context);
    }

    @Test
    public void testCustomThemeConstants() {
        // Test that custom theme constant is properly defined
        assertEquals(4, UserPreferences.THEME_CUSTOM);
    }

    @Test
    public void testCustomThemeSelection() {
        // Test setting custom theme
        userPreferences.setThemeId(UserPreferences.THEME_CUSTOM);
        assertEquals(UserPreferences.THEME_CUSTOM, userPreferences.getThemeId());
        assertTrue(userPreferences.isUsingCustomTheme());
    }

    @Test
    public void testCustomColorStorage() {
        // Test custom color storage and retrieval
        int testColor = Color.parseColor("#FF5722"); // Deep Orange
        int defaultColor = Color.parseColor("#2196F3"); // Blue

        // Test primary color
        userPreferences.setCustomPrimaryColor(testColor);
        assertEquals(testColor, userPreferences.getCustomPrimaryColor(defaultColor));

        // Test nav bar color
        userPreferences.setCustomNavBarColor(testColor);
        assertEquals(testColor, userPreferences.getCustomNavBarColor(defaultColor));

        // Test button color
        userPreferences.setCustomButtonColor(testColor);
        assertEquals(testColor, userPreferences.getCustomButtonColor(defaultColor));

        // Test top bar color
        userPreferences.setCustomTopBarColor(testColor);
        assertEquals(testColor, userPreferences.getCustomTopBarColor(defaultColor));

        // Test incoming bubble color
        userPreferences.setCustomIncomingBubbleColor(testColor);
        assertEquals(testColor, userPreferences.getCustomIncomingBubbleColor(defaultColor));

        // Test outgoing bubble color
        userPreferences.setCustomOutgoingBubbleColor(testColor);
        assertEquals(testColor, userPreferences.getCustomOutgoingBubbleColor(defaultColor));

        // Test menu color
        userPreferences.setCustomMenuColor(testColor);
        assertEquals(testColor, userPreferences.getCustomMenuColor(defaultColor));

        // Test background color
        userPreferences.setCustomBackgroundColor(testColor);
        assertEquals(testColor, userPreferences.getCustomBackgroundColor(defaultColor));
    }

    @Test
    public void testCustomColorDefaults() {
        // Test that default colors are returned when no custom colors are set
        int defaultColor = Color.parseColor("#2196F3"); // Blue

        assertEquals(defaultColor, userPreferences.getCustomPrimaryColor(defaultColor));
        assertEquals(defaultColor, userPreferences.getCustomNavBarColor(defaultColor));
        assertEquals(defaultColor, userPreferences.getCustomButtonColor(defaultColor));
        assertEquals(defaultColor, userPreferences.getCustomTopBarColor(defaultColor));
        assertEquals(defaultColor, userPreferences.getCustomIncomingBubbleColor(defaultColor));
        assertEquals(defaultColor, userPreferences.getCustomOutgoingBubbleColor(defaultColor));
        assertEquals(defaultColor, userPreferences.getCustomMenuColor(defaultColor));
        assertEquals(defaultColor, userPreferences.getCustomBackgroundColor(defaultColor));
    }

    @Test
    public void testNonCustomThemeDetection() {
        // Test that other themes are not detected as custom
        userPreferences.setThemeId(UserPreferences.THEME_LIGHT);
        assertFalse(userPreferences.isUsingCustomTheme());

        userPreferences.setThemeId(UserPreferences.THEME_DARK);
        assertFalse(userPreferences.isUsingCustomTheme());

        userPreferences.setThemeId(UserPreferences.THEME_BLACK_GLASS);
        assertFalse(userPreferences.isUsingCustomTheme());

        userPreferences.setThemeId(UserPreferences.THEME_SYSTEM);
        assertFalse(userPreferences.isUsingCustomTheme());
    }

    @Test
    public void testCustomThemeWorkflow() {
        // Test complete workflow of setting custom theme and colors
        
        // Start with light theme
        userPreferences.setThemeId(UserPreferences.THEME_LIGHT);
        assertFalse(userPreferences.isUsingCustomTheme());

        // Switch to custom theme
        userPreferences.setThemeId(UserPreferences.THEME_CUSTOM);
        assertTrue(userPreferences.isUsingCustomTheme());

        // Set custom colors
        int customPrimary = Color.parseColor("#9C27B0"); // Purple
        int customButton = Color.parseColor("#E91E63"); // Pink
        int customNavBar = Color.parseColor("#FF5722"); // Deep Orange

        userPreferences.setCustomPrimaryColor(customPrimary);
        userPreferences.setCustomButtonColor(customButton);
        userPreferences.setCustomNavBarColor(customNavBar);

        // Verify colors are stored
        int defaultColor = Color.parseColor("#000000"); // Black as default
        assertEquals(customPrimary, userPreferences.getCustomPrimaryColor(defaultColor));
        assertEquals(customButton, userPreferences.getCustomButtonColor(defaultColor));
        assertEquals(customNavBar, userPreferences.getCustomNavBarColor(defaultColor));

        // Switch back to light theme
        userPreferences.setThemeId(UserPreferences.THEME_LIGHT);
        assertFalse(userPreferences.isUsingCustomTheme());

        // Colors should still be stored
        assertEquals(customPrimary, userPreferences.getCustomPrimaryColor(defaultColor));
        assertEquals(customButton, userPreferences.getCustomButtonColor(defaultColor));
        assertEquals(customNavBar, userPreferences.getCustomNavBarColor(defaultColor));
    }
}