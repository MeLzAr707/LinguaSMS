package com.translator.messagingapp;

import com.translator.messagingapp.message.*;
import com.translator.messagingapp.conversation.*;
import com.translator.messagingapp.mms.*;
import com.translator.messagingapp.contact.*;
import com.translator.messagingapp.notification.*;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import android.content.Context;

/**
 * Test to verify granular theme customization functionality
 */
@RunWith(RobolectricTestRunner.class)
public class GranularThemeCustomizationTest {
    
    private Context context;
    private UserPreferences userPreferences;
    
    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        userPreferences = new UserPreferences(context);
    }
    
    @Test
    public void testNewColorPreferenceMethods() {
        // Test the new preference methods for granular customization
        int testColor = 0xFF123456;
        
        // Test message view background color
        userPreferences.setCustomMessageViewBackgroundColor(testColor);
        int retrievedMessageViewBg = userPreferences.getCustomMessageViewBackgroundColor(0xFF000000);
        assertEquals("Message view background color should be set and retrieved correctly", 
            testColor, retrievedMessageViewBg);
        
        // Test incoming bubble text color
        userPreferences.setCustomIncomingBubbleTextColor(testColor);
        int retrievedIncomingText = userPreferences.getCustomIncomingBubbleTextColor(0xFF000000);
        assertEquals("Incoming bubble text color should be set and retrieved correctly", 
            testColor, retrievedIncomingText);
        
        // Test outgoing bubble text color
        userPreferences.setCustomOutgoingBubbleTextColor(testColor);
        int retrievedOutgoingText = userPreferences.getCustomOutgoingBubbleTextColor(0xFF000000);
        assertEquals("Outgoing bubble text color should be set and retrieved correctly", 
            testColor, retrievedOutgoingText);
    }
    
    @Test
    public void testAllNineUIElementColors() {
        // Test all 9 UI element colors that should be customizable
        int baseColor = 0xFF000000;
        
        // 1. Conversation view background (maps to custom background color)
        userPreferences.setCustomBackgroundColor(baseColor + 1);
        assertEquals("Conversation view background should be customizable", 
            baseColor + 1, userPreferences.getCustomBackgroundColor(0));
        
        // 2. Message view background 
        userPreferences.setCustomMessageViewBackgroundColor(baseColor + 2);
        assertEquals("Message view background should be customizable", 
            baseColor + 2, userPreferences.getCustomMessageViewBackgroundColor(0));
        
        // 3. Main UI text color
        userPreferences.setCustomTextColor(baseColor + 3);
        assertEquals("Main UI text color should be customizable", 
            baseColor + 3, userPreferences.getCustomTextColor(0));
        
        // 4. Button color
        userPreferences.setCustomButtonColor(baseColor + 4);
        assertEquals("Button color should be customizable", 
            baseColor + 4, userPreferences.getCustomButtonColor(0));
        
        // 5. Incoming message bubble text color
        userPreferences.setCustomIncomingBubbleTextColor(baseColor + 5);
        assertEquals("Incoming bubble text color should be customizable", 
            baseColor + 5, userPreferences.getCustomIncomingBubbleTextColor(0));
        
        // 6. Outgoing message bubble text color
        userPreferences.setCustomOutgoingBubbleTextColor(baseColor + 6);
        assertEquals("Outgoing bubble text color should be customizable", 
            baseColor + 6, userPreferences.getCustomOutgoingBubbleTextColor(0));
        
        // 7. Incoming chat bubble color
        userPreferences.setCustomIncomingBubbleColor(baseColor + 7);
        assertEquals("Incoming chat bubble color should be customizable", 
            baseColor + 7, userPreferences.getCustomIncomingBubbleColor(0));
        
        // 8. Outgoing chat bubble color
        userPreferences.setCustomOutgoingBubbleColor(baseColor + 8);
        assertEquals("Outgoing chat bubble color should be customizable", 
            baseColor + 8, userPreferences.getCustomOutgoingBubbleColor(0));
        
        // 9. Navigation header background color
        userPreferences.setCustomNavBarColor(baseColor + 9);
        assertEquals("Navigation header background color should be customizable", 
            baseColor + 9, userPreferences.getCustomNavBarColor(0));
    }
    
    @Test
    public void testDefaultColorValues() {
        // Test that default color values are returned when preferences are not set
        int defaultColor = 0xFFFFFFFF;
        
        // Clear any existing preferences by creating a new instance
        UserPreferences freshPrefs = new UserPreferences(context);
        
        assertEquals("Should return default for message view background", 
            defaultColor, freshPrefs.getCustomMessageViewBackgroundColor(defaultColor));
        assertEquals("Should return default for incoming bubble text", 
            defaultColor, freshPrefs.getCustomIncomingBubbleTextColor(defaultColor));
        assertEquals("Should return default for outgoing bubble text", 
            defaultColor, freshPrefs.getCustomOutgoingBubbleTextColor(defaultColor));
    }
    
    @Test
    public void testColorPreferencesPersistence() {
        // Test that color preferences persist across UserPreferences instances
        int testColor1 = 0xFF111111;
        int testColor2 = 0xFF222222;
        int testColor3 = 0xFF333333;
        
        // Set colors in first instance
        userPreferences.setCustomMessageViewBackgroundColor(testColor1);
        userPreferences.setCustomIncomingBubbleTextColor(testColor2);
        userPreferences.setCustomOutgoingBubbleTextColor(testColor3);
        
        // Create new instance and verify persistence
        UserPreferences newPrefs = new UserPreferences(context);
        assertEquals("Message view background should persist", 
            testColor1, newPrefs.getCustomMessageViewBackgroundColor(0));
        assertEquals("Incoming bubble text should persist", 
            testColor2, newPrefs.getCustomIncomingBubbleTextColor(0));
        assertEquals("Outgoing bubble text should persist", 
            testColor3, newPrefs.getCustomOutgoingBubbleTextColor(0));
    }
    
    @Test
    public void testCustomThemeWithGranularColors() {
        // Test that custom theme can be enabled and granular colors work together
        userPreferences.setThemeId(UserPreferences.THEME_CUSTOM);
        assertTrue("Custom theme should be enabled", userPreferences.isUsingCustomTheme());
        
        // Set various granular colors
        userPreferences.setCustomBackgroundColor(0xFF001122);
        userPreferences.setCustomMessageViewBackgroundColor(0xFF112233);
        userPreferences.setCustomTextColor(0xFF223344);
        userPreferences.setCustomButtonColor(0xFF334455);
        userPreferences.setCustomIncomingBubbleTextColor(0xFF445566);
        userPreferences.setCustomOutgoingBubbleTextColor(0xFF556677);
        userPreferences.setCustomIncomingBubbleColor(0xFF667788);
        userPreferences.setCustomOutgoingBubbleColor(0xFF778899);
        userPreferences.setCustomNavBarColor(0xFF8899AA);
        
        // Verify all colors are stored correctly while custom theme is active
        assertTrue("Custom theme should remain enabled", userPreferences.isUsingCustomTheme());
        assertEquals("All granular colors should work with custom theme", 
            0xFF001122, userPreferences.getCustomBackgroundColor(0));
        assertEquals("All granular colors should work with custom theme", 
            0xFF112233, userPreferences.getCustomMessageViewBackgroundColor(0));
        assertEquals("All granular colors should work with custom theme", 
            0xFF223344, userPreferences.getCustomTextColor(0));
    }
}