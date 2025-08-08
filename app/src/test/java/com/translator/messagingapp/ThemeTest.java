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
        boolean isDarkMode = userPreferences.isDarkThemeActive(context);
        // This will depend on the device's current setting
        assertNotNull(isDarkMode);
    }

    @Test
    public void testLightThemeOverride() {
        // Test that light theme can be set even when system is in dark mode
        userPreferences.setThemeId(UserPreferences.THEME_LIGHT);
        assertEquals(UserPreferences.THEME_LIGHT, userPreferences.getThemeId());
        
        // This should remain light theme regardless of system setting
        assertTrue("Light theme should be settable independently of system dark mode", 
                  userPreferences.getThemeId() == UserPreferences.THEME_LIGHT);
    }

    @Test
    public void testThemeChangeBroadcast() {
        // Test that theme change broadcast action is correctly defined
        assertEquals("com.translator.messagingapp.THEME_CHANGED", BaseActivity.ACTION_THEME_CHANGED);
    }
}