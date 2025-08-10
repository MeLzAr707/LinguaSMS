package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;

/**
 * Demo class for offline translation features.
 * Provides utilities to demonstrate and test offline translation functionality.
 */
public class OfflineTranslationDemo {
    private static final String TAG = "OfflineTranslationDemo";
    
    private final UserPreferences userPreferences;
    private final Context context;

    public OfflineTranslationDemo(Context context) {
        this.context = context;
        this.userPreferences = new UserPreferences(context);
    }

    /**
     * Shows the current translation mode status.
     *
     * @return Status string describing the current translation mode
     */
    public String getTranslationModeStatus() {
        StringBuilder status = new StringBuilder();
        
        try {
            int translationMode = userPreferences.getTranslationMode();
            
            status.append("Translation Mode: ");
            switch (translationMode) {
                case UserPreferences.TRANSLATION_MODE_ONLINE_ONLY:
                    status.append("Online Only");
                    break;
                case UserPreferences.TRANSLATION_MODE_OFFLINE_ONLY:
                    status.append("Offline Only");
                    break;
                case UserPreferences.TRANSLATION_MODE_AUTO:
                default:
                    status.append("Auto (Smart Mode)");
                    break;
            }
            status.append("\n");
            
            status.append("Prefer Offline: ").append(userPreferences.getPreferOfflineTranslation() ? "Yes" : "No").append("\n\n");
            
            // Add additional status information
            status.append("Preferred Language: ").append(userPreferences.getPreferredLanguage()).append("\n");
            status.append("Auto Translate: ").append(userPreferences.isAutoTranslateEnabled() ? "Enabled" : "Disabled").append("\n");
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting translation mode status", e);
            status.append("Error getting status: ").append(e.getMessage());
        }
        
        return status.toString();
    }

    /**
     * Demonstrates offline translation features.
     */
    public void demonstrateOfflineFeatures() {
        Log.d(TAG, "=== Offline Translation Demo ===");
        Log.d(TAG, getTranslationModeStatus());
    }

    /**
     * Sets up demo translation modes for testing.
     */
    public void setupDemoModes() {
        Log.d(TAG, "Setting up demo translation modes...");
        
        // Test different translation modes
        int[] modes = {
            UserPreferences.TRANSLATION_MODE_AUTO,
            UserPreferences.TRANSLATION_MODE_ONLINE_ONLY,
            UserPreferences.TRANSLATION_MODE_OFFLINE_ONLY
        };
        
        for (int mode : modes) {
            userPreferences.setTranslationMode(mode);
            Log.d(TAG, "Testing mode: " + getModeDescription(mode));
            
            // Test the mode
            testTranslationMode();
        }
        
        // Reset to auto mode
        userPreferences.setTranslationMode(UserPreferences.TRANSLATION_MODE_AUTO);
    }

    /**
     * Tests the current translation mode.
     */
    private void testTranslationMode() {
        try {
            int translationMode = userPreferences.getTranslationMode();
            
            Log.d(TAG, "Current mode: " + getModeDescription(translationMode));
            
            switch (translationMode) {
                case UserPreferences.TRANSLATION_MODE_ONLINE_ONLY:
                    Log.d(TAG, "Testing online-only translation...");
                    break;
                case UserPreferences.TRANSLATION_MODE_OFFLINE_ONLY:
                    Log.d(TAG, "Testing offline-only translation...");
                    break;
                case UserPreferences.TRANSLATION_MODE_AUTO:
                    Log.d(TAG, "Testing auto translation mode...");
                    if (userPreferences.getPreferOfflineTranslation()) {
                        Log.d(TAG, "Auto mode with offline preference");
                    } else {
                        Log.d(TAG, "Auto mode with online preference");
                    }
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error testing translation mode", e);
        }
    }

    /**
     * Gets a description of the translation mode.
     *
     * @param mode The translation mode
     * @return Description string
     */
    private String getModeDescription(int mode) {
        switch (mode) {
            case UserPreferences.TRANSLATION_MODE_ONLINE_ONLY:
                return "Online Only";
            case UserPreferences.TRANSLATION_MODE_OFFLINE_ONLY:
                return "Offline Only";
            case UserPreferences.TRANSLATION_MODE_AUTO:
            default:
                return "Auto (Smart Mode)";
        }
    }
}