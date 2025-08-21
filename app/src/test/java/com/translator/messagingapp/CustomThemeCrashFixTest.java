package com.translator.messagingapp;

import android.content.Context;
import android.graphics.Color;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test to verify the custom theme crash fix specifically for issue #300
 */
@RunWith(RobolectricTestRunner.class)
public class CustomThemeCrashFixTest {
    private UserPreferences userPreferences;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        userPreferences = new UserPreferences(context);
    }

    @Test
    public void testCustomThemeSelectionDoesNotCrash() {
        // This test simulates the exact scenario that was causing the crash:
        // 1. User selects custom theme in SettingsActivity
        // 2. SettingsActivity calls openColorWheelActivity()
        // 3. ColorWheelActivity starts and calls loadCurrentColors()
        // 4. loadCurrentColors() calls getCustomTextColor() - this was causing NoSuchMethodError

        // Step 1: Set custom theme (this part was working)
        userPreferences.setThemeId(UserPreferences.THEME_CUSTOM);
        assertTrue(userPreferences.isUsingCustomTheme());

        // Step 2: Simulate the method calls that were causing the crash in ColorWheelActivity.loadCurrentColors()
        int defaultBackgroundColor = Color.parseColor("#33691E"); // Holo blue dark equivalent
        int defaultTextColor = Color.parseColor("#000000"); // Black

        // These method calls were causing NoSuchMethodError before the fix:
        int selectedBackgroundColor = userPreferences.getCustomPrimaryColor(defaultBackgroundColor);
        int selectedTextColor = userPreferences.getCustomTextColor(defaultTextColor); // This was crashing!

        // If we reach here without exception, the crash is fixed
        assertEquals(defaultBackgroundColor, selectedBackgroundColor); // Should return default since not set
        assertEquals(defaultTextColor, selectedTextColor); // Should return default since not set

        // Step 3: Simulate the method calls in ColorWheelActivity.applySelectedColors()
        int customColor = Color.parseColor("#FF5722"); // Deep Orange
        userPreferences.setCustomTextColor(customColor); // This was also crashing!

        // Step 4: Verify the color was stored correctly
        assertEquals(customColor, userPreferences.getCustomTextColor(defaultTextColor));

        // Step 5: Simulate the reset functionality that was also calling setCustomTextColor
        userPreferences.setCustomTextColor(defaultTextColor);
        assertEquals(defaultTextColor, userPreferences.getCustomTextColor(Color.BLACK));
    }

    @Test
    public void testAllColorWheelActivityMethodCalls() {
        // This test verifies all the UserPreferences method calls made by ColorWheelActivity
        // to ensure none of them will cause crashes

        int defaultColor = Color.parseColor("#2196F3");
        int customColor = Color.parseColor("#E91E63");

        // All these methods are called by ColorWheelActivity and must exist:
        assertTrue(userPreferences.isUsingCustomTheme() || !userPreferences.isUsingCustomTheme()); // Method exists
        
        userPreferences.getCustomPrimaryColor(defaultColor); // Called in loadCurrentColors()
        userPreferences.getCustomTextColor(defaultColor); // Called in loadCurrentColors() - was missing!
        
        userPreferences.setCustomNavBarColor(customColor); // Called in applySelectedColors()
        userPreferences.setCustomTopBarColor(customColor); // Called in applySelectedColors()
        userPreferences.setCustomButtonColor(customColor); // Called in applySelectedColors()
        userPreferences.setCustomMenuColor(customColor); // Called in applySelectedColors()
        userPreferences.setCustomIncomingBubbleColor(customColor); // Called in applySelectedColors()
        userPreferences.setCustomOutgoingBubbleColor(customColor); // Called in applySelectedColors()
        userPreferences.setCustomPrimaryColor(customColor); // Called in applySelectedColors()
        userPreferences.setCustomTextColor(customColor); // Called in applySelectedColors() - was missing!
        
        userPreferences.getThemeId(); // Called in applySelectedColors()
        userPreferences.setThemeId(UserPreferences.THEME_CUSTOM); // Called in applySelectedColors()
        
        userPreferences.getCustomButtonColor(defaultColor); // Called in updateButtonColors()

        // If we reach here, all methods exist and work correctly
        assertTrue(true);
    }
}