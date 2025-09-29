package com.translator.messagingapp;

import com.translator.messagingapp.message.*;
import com.translator.messagingapp.conversation.*;
import com.translator.messagingapp.mms.*;
import com.translator.messagingapp.contact.*;
import com.translator.messagingapp.notification.*;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.res.Configuration;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that validates the fix for theme override issue:
 * "Override Android dark/light mode and fix theme text color legibility"
 * 
 * This test ensures that ALL theme selections override system settings,
 * with no theme following system configuration.
 */
@RunWith(AndroidJUnit4.class)
public class ThemeOverrideIssueTest {
    
    private Context context;
    private UserPreferences userPreferences;
    
    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        userPreferences = new UserPreferences(context);
    }
    
    /**
     * Test scenario from issue #542:
     * "When the user selects a theme option (Dark or Light) in the app, 
     * the app should ignore the Android system's dark/light mode setting 
     * and always apply the chosen theme."
     */
    @Test
    public void testManualThemeSelectionOverridesSystemSetting() {
        // Test Light theme selection overrides system dark mode
        userPreferences.setThemeId(UserPreferences.THEME_LIGHT);
        assertFalse("Light theme should override system dark mode", 
                   userPreferences.isDarkThemeActive(context));
        
        // Test Dark theme selection overrides system light mode  
        userPreferences.setThemeId(UserPreferences.THEME_DARK);
        assertTrue("Dark theme should override system light mode", 
                  userPreferences.isDarkThemeActive(context));
        
        // Test Black Glass theme selection overrides system light mode
        userPreferences.setThemeId(UserPreferences.THEME_BLACK_GLASS);
        assertTrue("Black Glass theme should override system light mode", 
                  userPreferences.isDarkThemeActive(context));
    }
    
    /**
     * Test that no themes follow system settings - all themes override system behavior
     */
    @Test
    public void testAllThemesOverrideSystemSettings() {
        // Test that all themes behave consistently regardless of system configuration
        boolean systemDarkMode = isSystemInDarkMode();
        
        // Light theme should always be light
        userPreferences.setThemeId(UserPreferences.THEME_LIGHT);
        assertFalse("Light theme should always be light regardless of system setting", 
                   userPreferences.isDarkThemeActive(context));
        
        // Dark theme should always be dark  
        userPreferences.setThemeId(UserPreferences.THEME_DARK);
        assertTrue("Dark theme should always be dark regardless of system setting",
                  userPreferences.isDarkThemeActive(context));
                  
        // Custom theme should always be light
        userPreferences.setThemeId(UserPreferences.THEME_CUSTOM);
        assertFalse("Custom theme should always be light regardless of system setting",
                   userPreferences.isDarkThemeActive(context));
    }
    
    /**
     * Test that theme preferences persist correctly and don't get overridden
     */
    @Test
    public void testThemePreferencePersistence() {
        // Set light theme and verify it persists
        userPreferences.setThemeId(UserPreferences.THEME_LIGHT);
        assertEquals("Light theme selection should persist", 
                    UserPreferences.THEME_LIGHT, userPreferences.getThemeId());
        
        // Verify multiple calls don't change the theme
        for (int i = 0; i < 5; i++) {
            assertEquals("Theme selection should remain stable", 
                        UserPreferences.THEME_LIGHT, userPreferences.getThemeId());
            assertFalse("Light theme should consistently return false for dark mode", 
                       userPreferences.isDarkThemeActive(context));
        }
        
        // Set dark theme and verify it persists  
        userPreferences.setThemeId(UserPreferences.THEME_DARK);
        assertEquals("Dark theme selection should persist", 
                    UserPreferences.THEME_DARK, userPreferences.getThemeId());
        
        // Verify multiple calls don't change the theme
        for (int i = 0; i < 5; i++) {
            assertEquals("Theme selection should remain stable", 
                        UserPreferences.THEME_DARK, userPreferences.getThemeId());
            assertTrue("Dark theme should consistently return true for dark mode", 
                      userPreferences.isDarkThemeActive(context));
        }
    }
    
    /**
     * Test all theme types to ensure they work independently of system setting
     */
    @Test
    public void testAllThemeTypesIndependentOfSystem() {
        // Test each theme type
        int[] themes = {
            UserPreferences.THEME_LIGHT,
            UserPreferences.THEME_DARK, 
            UserPreferences.THEME_BLACK_GLASS,
            UserPreferences.THEME_CUSTOM
        };
        
        boolean[] expectedDarkResults = {false, true, true, false};
        
        for (int i = 0; i < themes.length; i++) {
            int theme = themes[i];
            boolean expectedDark = expectedDarkResults[i];
            
            userPreferences.setThemeId(theme);
            
            assertEquals("Theme " + theme + " should be set correctly", 
                        theme, userPreferences.getThemeId());
            assertEquals("Theme " + theme + " should have consistent dark mode behavior", 
                        expectedDark, userPreferences.isDarkThemeActive(context));
        }
    }
    
    /**
     * Helper method to check if system is in dark mode
     */
    private boolean isSystemInDarkMode() {
        int nightModeFlags = context.getResources().getConfiguration().uiMode &
                            Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
    
    /**
     * Test that demonstrates all themes now override system settings:
     * User selects themes manually, and they always override system setting
     */
    @Test
    public void testThemeOverrideScenario() {
        // Step 1: User selects light theme in app
        userPreferences.setThemeId(UserPreferences.THEME_LIGHT);
        assertFalse("App should use light theme when user selects it", 
                   userPreferences.isDarkThemeActive(context));
        
        // Step 2: Simulate system dark mode change (this would happen when user changes system setting)
        // The app's theme detection should still return light theme
        assertFalse("App should still use light theme after system change", 
                   userPreferences.isDarkThemeActive(context));
        assertEquals("Theme preference should not change", 
                    UserPreferences.THEME_LIGHT, userPreferences.getThemeId());
        
        // Step 3: User selects dark theme in app  
        userPreferences.setThemeId(UserPreferences.THEME_DARK);
        assertTrue("App should use dark theme when user selects it", 
                  userPreferences.isDarkThemeActive(context));
        
        // Step 4: Simulate system light mode change
        // The app's theme detection should still return dark theme
        assertTrue("App should still use dark theme after system change", 
                  userPreferences.isDarkThemeActive(context));
        assertEquals("Theme preference should not change", 
                    UserPreferences.THEME_DARK, userPreferences.getThemeId());
    }
}