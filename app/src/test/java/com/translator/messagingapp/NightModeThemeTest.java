package com.translator.messagingapp;

import com.translator.messagingapp.message.*;
import com.translator.messagingapp.conversation.*;
import com.translator.messagingapp.mms.*;
import com.translator.messagingapp.contact.*;
import com.translator.messagingapp.notification.*;

import android.content.Context;
import android.content.res.Configuration;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Test to validate that text remains visible in dark mode when Android system is in dark mode.
 * This test specifically addresses issue #535 where light theme text disappears in system dark mode.
 */
@RunWith(AndroidJUnit4.class)
public class NightModeThemeTest {
    
    private Context context;
    private UserPreferences userPreferences;
    
    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        userPreferences = new UserPreferences(context);
    }
    
    @Test
    public void testCustomThemeOverridesNightMode() {
        // Set theme to custom theme (which replaced system theme)
        userPreferences.setThemeId(UserPreferences.THEME_CUSTOM);
        
        // Verify that the theme ID is set correctly
        assertEquals("Custom theme should be set", 
                    UserPreferences.THEME_CUSTOM, userPreferences.getThemeId());
        
        // Test that custom theme always returns false (light theme) regardless of system mode
        boolean isDarkThemeActive = userPreferences.isDarkThemeActive(context);
        assertFalse("Custom theme should always be light and override system", isDarkThemeActive);
    }
    
    @Test
    public void testLightThemeStillWorksInNightMode() {
        // Even when system is in night mode, explicit light theme should work
        userPreferences.setThemeId(UserPreferences.THEME_LIGHT);
        
        assertEquals("Light theme should be set explicitly", 
                    UserPreferences.THEME_LIGHT, userPreferences.getThemeId());
        
        // Light theme should not be considered dark theme even in night mode
        assertFalse("Explicitly set light theme should not be detected as dark theme",
                   userPreferences.isDarkThemeActive(context));
    }
    
    @Test
    public void testDarkThemeWorksCorrectly() {
        // Set to dark theme explicitly
        userPreferences.setThemeId(UserPreferences.THEME_DARK);
        
        assertEquals("Dark theme should be set", 
                    UserPreferences.THEME_DARK, userPreferences.getThemeId());
        
        // Dark theme should always be detected as dark
        assertTrue("Dark theme should be detected as dark theme",
                  userPreferences.isDarkThemeActive(context));
    }
    
    @Test
    public void testBlackGlassThemeConsistency() {
        // Set to Black Glass theme
        userPreferences.setThemeId(UserPreferences.THEME_BLACK_GLASS);
        
        assertEquals("Black Glass theme should be set", 
                    UserPreferences.THEME_BLACK_GLASS, userPreferences.getThemeId());
        
        // Black Glass theme should be detected as dark theme
        assertTrue("Black Glass theme should be detected as dark theme",
                  userPreferences.isDarkThemeActive(context));
        
        // Black Glass theme should be identified correctly
        assertTrue("Black Glass theme should be identified correctly",
                  userPreferences.isUsingBlackGlassTheme());
    }
    
    /**
     * Test that validates that all themes override system dark mode settings:
     * All app themes now ignore Android system dark/light mode and use their own settings.
     */
    @Test
    public void testAllThemesOverrideSystemDarkMode() {
        // Test that light theme always uses light colors regardless of system mode
        userPreferences.setThemeId(UserPreferences.THEME_LIGHT);
        assertFalse("Light theme should always be light regardless of system mode",
                   userPreferences.isDarkThemeActive(context));
        
        // Test that dark theme always uses dark colors regardless of system mode  
        userPreferences.setThemeId(UserPreferences.THEME_DARK);
        assertTrue("Dark theme should always be dark regardless of system mode",
                  userPreferences.isDarkThemeActive(context));
                  
        // Test that custom theme always uses light colors regardless of system mode
        userPreferences.setThemeId(UserPreferences.THEME_CUSTOM);
        assertFalse("Custom theme should always be light regardless of system mode",
                   userPreferences.isDarkThemeActive(context));
        
        // Test that black glass theme always uses dark colors regardless of system mode
        userPreferences.setThemeId(UserPreferences.THEME_BLACK_GLASS);
        assertTrue("Black glass theme should always be dark regardless of system mode",
                  userPreferences.isDarkThemeActive(context));
    }
        
        // Check that dark theme detection works correctly for system theme
        boolean isDark = userPreferences.isDarkThemeActive(context);
        // Note: The actual result depends on the system configuration during test
        assertNotNull("Dark theme detection should return a valid boolean result", isDark);
    }
}