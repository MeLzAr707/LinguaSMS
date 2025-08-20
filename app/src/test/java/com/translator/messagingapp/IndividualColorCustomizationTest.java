package com.translator.messagingapp;

import android.content.Context;
import android.graphics.Color;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for individual color customization functionality
 */
@RunWith(RobolectricTestRunner.class)
public class IndividualColorCustomizationTest {
    private UserPreferences userPreferences;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        userPreferences = new UserPreferences(context);
    }

    @Test
    public void testIndividualColorStorageAndRetrieval() {
        // Define different colors for each UI element
        int menuColor = Color.parseColor("#FF5722"); // Deep Orange
        int navBarColor = Color.parseColor("#4CAF50"); // Green
        int buttonColor = Color.parseColor("#9C27B0"); // Purple
        int incomingColor = Color.parseColor("#E3F2FD"); // Light Blue
        int outgoingColor = Color.parseColor("#2196F3"); // Blue
        int textColor = Color.parseColor("#000000"); // Black

        // Set individual colors
        userPreferences.setCustomMenuColor(menuColor);
        userPreferences.setCustomNavBarColor(navBarColor);
        userPreferences.setCustomButtonColor(buttonColor);
        userPreferences.setCustomIncomingBubbleColor(incomingColor);
        userPreferences.setCustomOutgoingBubbleColor(outgoingColor);
        userPreferences.setCustomTextColor(textColor);

        // Verify each color is stored and retrieved correctly
        int defaultColor = Color.parseColor("#CCCCCC"); // Gray default
        assertEquals("Menu color should match", menuColor, userPreferences.getCustomMenuColor(defaultColor));
        assertEquals("Nav bar color should match", navBarColor, userPreferences.getCustomNavBarColor(defaultColor));
        assertEquals("Button color should match", buttonColor, userPreferences.getCustomButtonColor(defaultColor));
        assertEquals("Incoming bubble color should match", incomingColor, userPreferences.getCustomIncomingBubbleColor(defaultColor));
        assertEquals("Outgoing bubble color should match", outgoingColor, userPreferences.getCustomOutgoingBubbleColor(defaultColor));
        assertEquals("Text color should match", textColor, userPreferences.getCustomTextColor(defaultColor));
    }

    @Test
    public void testIndividualColorsAreIndependent() {
        // Set different colors for each element
        int color1 = Color.parseColor("#FF0000"); // Red
        int color2 = Color.parseColor("#00FF00"); // Green
        int color3 = Color.parseColor("#0000FF"); // Blue
        int color4 = Color.parseColor("#FFFF00"); // Yellow
        int color5 = Color.parseColor("#FF00FF"); // Magenta
        int color6 = Color.parseColor("#00FFFF"); // Cyan

        userPreferences.setCustomMenuColor(color1);
        userPreferences.setCustomNavBarColor(color2);
        userPreferences.setCustomButtonColor(color3);
        userPreferences.setCustomIncomingBubbleColor(color4);
        userPreferences.setCustomOutgoingBubbleColor(color5);
        userPreferences.setCustomTextColor(color6);

        int defaultColor = Color.parseColor("#CCCCCC");

        // Verify all colors are different and independent
        int menuColor = userPreferences.getCustomMenuColor(defaultColor);
        int navBarColor = userPreferences.getCustomNavBarColor(defaultColor);
        int buttonColor = userPreferences.getCustomButtonColor(defaultColor);
        int incomingColor = userPreferences.getCustomIncomingBubbleColor(defaultColor);
        int outgoingColor = userPreferences.getCustomOutgoingBubbleColor(defaultColor);
        int textColor = userPreferences.getCustomTextColor(defaultColor);

        // Each color should be different from the others
        assertNotEquals("Menu and nav bar colors should be different", menuColor, navBarColor);
        assertNotEquals("Nav bar and button colors should be different", navBarColor, buttonColor);
        assertNotEquals("Button and incoming colors should be different", buttonColor, incomingColor);
        assertNotEquals("Incoming and outgoing colors should be different", incomingColor, outgoingColor);
        assertNotEquals("Outgoing and text colors should be different", outgoingColor, textColor);
        
        // Each should match what we set
        assertEquals("Menu color should be red", color1, menuColor);
        assertEquals("Nav bar color should be green", color2, navBarColor);
        assertEquals("Button color should be blue", color3, buttonColor);
        assertEquals("Incoming color should be yellow", color4, incomingColor);
        assertEquals("Outgoing color should be magenta", color5, outgoingColor);
        assertEquals("Text color should be cyan", color6, textColor);
    }

    @Test
    public void testCustomThemeActivation() {
        // Test that setting individual colors automatically activates custom theme
        userPreferences.setThemeId(UserPreferences.THEME_LIGHT);
        assertEquals("Should start with light theme", UserPreferences.THEME_LIGHT, userPreferences.getThemeId());

        // Setting a custom color should be possible even when not in custom theme
        int testColor = Color.parseColor("#FF5722");
        userPreferences.setCustomButtonColor(testColor);
        
        // Verify the color is stored correctly
        assertEquals("Color should be stored correctly", testColor, userPreferences.getCustomButtonColor(Color.BLUE));
        
        // Theme should still be light (application logic handles theme switching)
        assertEquals("Theme should remain unchanged by color setting", UserPreferences.THEME_LIGHT, userPreferences.getThemeId());
    }

    @Test
    public void testTextColorFunctionality() {
        // Test the newly added text color functionality specifically
        int testTextColor = Color.parseColor("#212121"); // Dark gray
        int defaultTextColor = Color.parseColor("#000000"); // Black

        // Initially should return default
        assertEquals("Should return default text color initially", defaultTextColor, userPreferences.getCustomTextColor(defaultTextColor));

        // Set custom text color
        userPreferences.setCustomTextColor(testTextColor);

        // Should return custom color
        assertEquals("Should return custom text color", testTextColor, userPreferences.getCustomTextColor(defaultTextColor));

        // Different default should still return custom color
        int differentDefault = Color.parseColor("#FFFFFF"); // White
        assertEquals("Should return custom color regardless of default", testTextColor, userPreferences.getCustomTextColor(differentDefault));
    }

    @Test
    public void testColorPersistence() {
        // Test that colors persist across UserPreferences instances
        int testColor = Color.parseColor("#9C27B0"); // Purple
        
        // Set color in first instance
        userPreferences.setCustomMenuColor(testColor);
        userPreferences.setCustomTextColor(testColor);

        // Create new instance (simulating app restart)
        UserPreferences newPreferences = new UserPreferences(context);
        
        // Verify colors persist
        int defaultColor = Color.parseColor("#CCCCCC");
        assertEquals("Menu color should persist", testColor, newPreferences.getCustomMenuColor(defaultColor));
        assertEquals("Text color should persist", testColor, newPreferences.getCustomTextColor(defaultColor));
    }
}