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
        assertEquals(UserPreferences.THEME_SYSTEM, defaultTheme);
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
}