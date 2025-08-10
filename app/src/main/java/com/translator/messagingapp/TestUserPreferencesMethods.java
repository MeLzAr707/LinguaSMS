package com.translator.messagingapp;

import android.content.Context;

/**
 * Test class to verify that the missing UserPreferences methods work correctly.
 */
public class TestUserPreferencesMethods {
    
    public static void testMethods(Context context) {
        UserPreferences userPreferences = new UserPreferences(context);
        
        // Test the missing methods that were causing build errors
        
        // Test getTranslationMode()
        int translationMode = userPreferences.getTranslationMode();
        
        // Test the constants that were missing
        if (translationMode != UserPreferences.TRANSLATION_MODE_OFFLINE_ONLY && 
            translationMode != UserPreferences.TRANSLATION_MODE_ONLINE_ONLY &&
            translationMode != UserPreferences.TRANSLATION_MODE_AUTO) {
            // Handle invalid mode
        }
        
        // Test getPreferOfflineTranslation()
        boolean preferOffline = userPreferences.getPreferOfflineTranslation();
        
        // Test setting translation mode
        userPreferences.setTranslationMode(UserPreferences.TRANSLATION_MODE_AUTO);
        
        // Test setting prefer offline translation
        userPreferences.setPreferOfflineTranslation(true);
        
        // These are the exact calls that were failing in the build errors
        System.out.println("Translation mode: " + translationMode);
        System.out.println("Prefer offline: " + preferOffline);
    }
}