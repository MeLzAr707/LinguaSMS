package com.translator.messagingapp;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Base activity for all activities in the app.
 * Handles theme application based on user preferences.
 */
public class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before calling super.onCreate()
        applyTheme();
        super.onCreate(savedInstanceState);
    }
    
    /**
     * Applies the theme based on user preferences.
     */
    private void applyTheme() {
        try {
            TranslatorApp app = (TranslatorApp) getApplication();
            UserPreferences userPreferences = app.getUserPreferences();
            
            int themeId = userPreferences.getThemeId();
            int themeResId = getThemeResourceId(themeId);
            
            Log.d(TAG, "Applying theme: " + themeId + " -> " + themeResId);
            setTheme(themeResId);
            
            // Handle system theme for newer Android versions
            if (themeId == UserPreferences.THEME_SYSTEM) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                } else {
                    // For older versions, check system setting manually
                    boolean isDarkMode = (getResources().getConfiguration().uiMode & 
                        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
                    AppCompatDelegate.setDefaultNightMode(
                        isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error applying theme, falling back to default", e);
            setTheme(R.style.AppTheme);
        }
    }
    
    /**
     * Maps theme ID to theme resource ID.
     */
    private int getThemeResourceId(int themeId) {
        switch (themeId) {
            case UserPreferences.THEME_LIGHT:
                return R.style.AppTheme;
            case UserPreferences.THEME_DARK:
                return R.style.AppTheme_Dark;
            case UserPreferences.THEME_BLACK_GLASS:
                return R.style.AppTheme_BlackGlass;
            case UserPreferences.THEME_SYSTEM:
                // For system theme, check current system setting
                boolean isDarkMode = (getResources().getConfiguration().uiMode & 
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
                return isDarkMode ? R.style.AppTheme_Dark : R.style.AppTheme;
            default:
                Log.w(TAG, "Unknown theme ID: " + themeId + ", using default");
                return R.style.AppTheme;
        }
    }
    
    /**
     * Recreates the activity with fade animation.
     */
    protected void recreateWithFade() {
        // Simple recreation - the theme will be applied in onCreate
        recreate();
    }
}
