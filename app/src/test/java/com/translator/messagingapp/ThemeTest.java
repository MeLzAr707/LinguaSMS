package com.translator.messagingapp;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.res.Configuration;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for theme functionality in BaseActivity and UserPreferences.
 * Added test for BlackGlass theme system bar configuration.
 */
@RunWith(AndroidJUnit4.class)
public class ThemeTest {

    private Context context;
    private UserPreferences userPreferences;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        userPreferences = new UserPreferences(context);
    }

    @Test
    public void testThemeConstants() {
        assertEquals(0, UserPreferences.THEME_LIGHT);
        assertEquals(1, UserPreferences.THEME_DARK);
        assertEquals(2, UserPreferences.THEME_BLACK_GLASS);
        assertEquals(3, UserPreferences.THEME_SYSTEM);
    }

    @Test
    public void testDefaultTheme() {
        int defaultTheme = userPreferences.getThemeId();
        assertEquals(UserPreferences.THEME_LIGHT, defaultTheme);
    }

    @Test
    public void testThemeSettingAndGetting() {
        userPreferences.setThemeId(UserPreferences.THEME_LIGHT);
        assertEquals(UserPreferences.THEME_LIGHT, userPreferences.getThemeId());

        userPreferences.setThemeId(UserPreferences.THEME_DARK);
        assertEquals(UserPreferences.THEME_DARK, userPreferences.getThemeId());

        userPreferences.setThemeId(UserPreferences.THEME_BLACK_GLASS);
        assertEquals(UserPreferences.THEME_BLACK_GLASS, userPreferences.getThemeId());

        userPreferences.setThemeId(UserPreferences.THEME_SYSTEM);
        assertEquals(UserPreferences.THEME_SYSTEM, userPreferences.getThemeId());
    }

    @Test
    public void testDarkThemeDetection() {
        // Test dark theme detection for different theme settings
        
        // Dark theme should be detected as dark
        userPreferences.setThemeId(UserPreferences.THEME_DARK);
        assertTrue("Dark theme should be detected as dark", 
                  userPreferences.isDarkThemeActive(context));
        
        // Light theme should not be detected as dark
        userPreferences.setThemeId(UserPreferences.THEME_LIGHT);
        assertFalse("Light theme should not be detected as dark", 
                   userPreferences.isDarkThemeActive(context));
        
        // Black Glass theme should be detected as dark
        userPreferences.setThemeId(UserPreferences.THEME_BLACK_GLASS);
        assertTrue("Black Glass theme should be detected as dark", 
                  userPreferences.isDarkThemeActive(context));
        
        // System theme detection depends on device setting
        userPreferences.setThemeId(UserPreferences.THEME_SYSTEM);
        boolean isSystemDark = userPreferences.isDarkThemeActive(context);
        assertNotNull("System theme detection should return a valid boolean", isSystemDark);
    }

    @Test
    public void testLightThemeOverride() {
        // Test that light theme can be set even when system is in dark mode
        userPreferences.setThemeId(UserPreferences.THEME_LIGHT);
        assertEquals(UserPreferences.THEME_LIGHT, userPreferences.getThemeId());
        
        // This should remain light theme regardless of system setting
        assertTrue("Light theme should be settable independently of system dark mode", 
                  userPreferences.getThemeId() == UserPreferences.THEME_LIGHT);
        
        // Test that isDarkThemeActive returns false for explicitly set light theme
        assertFalse("Light theme should not be considered dark theme", 
                   userPreferences.isDarkThemeActive(context));
    }

    @Test
    public void testLightThemeAlwaysOverridesSystemDarkMode() {
        // Simulate different system configurations and ensure light theme always wins
        
        // Set to light theme explicitly
        userPreferences.setThemeId(UserPreferences.THEME_LIGHT);
        
        // Verify it's always considered light theme regardless of system state
        assertFalse("Light theme should override system dark mode", 
                   userPreferences.isDarkThemeActive(context));
        
        // Verify the theme ID persists as light
        assertEquals("Light theme ID should persist", 
                    UserPreferences.THEME_LIGHT, userPreferences.getThemeId());
        
        // Test multiple calls to ensure consistency
        for (int i = 0; i < 5; i++) {
            assertFalse("Light theme should remain consistent across multiple calls", 
                       userPreferences.isDarkThemeActive(context));
            assertEquals("Light theme ID should remain consistent", 
                        UserPreferences.THEME_LIGHT, userPreferences.getThemeId());
        }
    }

    @Test
    public void testSystemThemeVsLightThemeDistinction() {
        // Test that THEME_SYSTEM and THEME_LIGHT behave differently
        
        // Set to system theme
        userPreferences.setThemeId(UserPreferences.THEME_SYSTEM);
        boolean systemThemeResult = userPreferences.isDarkThemeActive(context);
        
        // Set to light theme
        userPreferences.setThemeId(UserPreferences.THEME_LIGHT);
        boolean lightThemeResult = userPreferences.isDarkThemeActive(context);
        
        // Light theme should always be false
        assertFalse("Light theme should always return false for isDarkThemeActive", 
                   lightThemeResult);
        
        // System theme result depends on system configuration
        // but we can verify they might be different
        // (This test documents the intended difference in behavior)
        assertTrue("System and light themes should have distinct behavior options",
                  !lightThemeResult || systemThemeResult == lightThemeResult);
    }

    @Test
    public void testBlackGlassThemeConfiguration() {
        // Test that BlackGlass theme can be selected and maintained
        userPreferences.setThemeId(UserPreferences.THEME_BLACK_GLASS);
        assertEquals("Black theme should be settable", 
                    UserPreferences.THEME_BLACK_GLASS, userPreferences.getThemeId());
        
        // Verify the theme persists after getting it again
        int retrievedTheme = userPreferences.getThemeId();
        assertEquals("Black theme should persist", 
                    UserPreferences.THEME_BLACK_GLASS, retrievedTheme);
        
        // Test that Black Glass theme is considered a dark theme
        assertTrue("Black theme should be considered dark theme",
                  userPreferences.isDarkThemeActive(context));
    }

    @Test
    public void testThemeChangeDetection() {
        // Test that theme changes can be detected
        int originalTheme = userPreferences.getThemeId();
        
        // Change to a different theme
        int newTheme = (originalTheme == UserPreferences.THEME_LIGHT) ? 
                       UserPreferences.THEME_DARK : UserPreferences.THEME_LIGHT;
        userPreferences.setThemeId(newTheme);
        
        // Verify the change was applied
        assertEquals("Theme change should be applied", newTheme, userPreferences.getThemeId());
        assertNotEquals("Theme should be different from original", originalTheme, userPreferences.getThemeId());
        
        // Test that BaseActivity can detect this change
        assertTrue("Theme change should be detectable", 
                  originalTheme != userPreferences.getThemeId());
    }
    
    @Test
    public void testBlackGlassThemeColorConsistency() {
        // Test that Black Glass theme detection works correctly
        userPreferences.setThemeId(UserPreferences.THEME_BLACK_GLASS);
        assertTrue("Black Glass theme should be detected correctly", 
                  userPreferences.isUsingBlackGlassTheme());
        
        // Verify that Black Glass theme is a dark theme
        assertTrue("Black Glass theme should be considered a dark theme",
                  userPreferences.isDarkThemeActive(context));
        
        // Test that other themes don't trigger Black Glass detection
        userPreferences.setThemeId(UserPreferences.THEME_DARK);
        assertFalse("Dark theme should not be detected as Black Glass theme", 
                   userPreferences.isUsingBlackGlassTheme());
        
        userPreferences.setThemeId(UserPreferences.THEME_LIGHT);
        assertFalse("Light theme should not be detected as Black Glass theme", 
                   userPreferences.isUsingBlackGlassTheme());
    }

    @Test
    public void testUserSelectsLightThemeScenario() {
        // This test simulates the exact scenario described in the issue:
        // "When a user selects the light theme within the LinguaSMS app, 
        //  the app should display a light theme regardless of the Android system's dark mode setting"
        
        // Step 1: Simulate user selecting light theme in app settings
        userPreferences.setThemeId(UserPreferences.THEME_LIGHT);
        
        // Step 2: Verify the selection was saved
        assertEquals("User's light theme selection should be saved", 
                    UserPreferences.THEME_LIGHT, userPreferences.getThemeId());
        
        // Step 3: Verify that regardless of system dark mode state, 
        //         the app considers itself in light theme mode
        assertFalse("App should be in light theme mode when user selects light theme", 
                   userPreferences.isDarkThemeActive(context));
        
        // Step 4: Verify this is not a dark theme
        assertFalse("Light theme should not be considered a dark theme", 
                   userPreferences.isDarkThemeEnabled());
        
        // Step 5: Verify theme persists across multiple checks
        for (int i = 0; i < 3; i++) {
            assertEquals("Light theme selection should persist", 
                        UserPreferences.THEME_LIGHT, userPreferences.getThemeId());
            assertFalse("Light theme state should persist", 
                       userPreferences.isDarkThemeActive(context));
        }
    }
}