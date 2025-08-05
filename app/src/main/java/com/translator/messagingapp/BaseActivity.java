package com.translator.messagingapp;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Base activity for all activities in the app.
 * Handles theme application and system UI configuration.
 */
public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before calling super.onCreate()
        applyTheme();
        super.onCreate(savedInstanceState);
    }

    /**
     * Applies the appropriate theme based on user preferences.
     */
    private void applyTheme() {
        try {
            TranslatorApp app = (TranslatorApp) getApplication();
            if (app != null) {
                UserPreferences userPreferences = app.getUserPreferences();
                int themeId = userPreferences.getThemeId();
                
                switch (themeId) {
                    case UserPreferences.THEME_LIGHT:
                        setTheme(R.style.AppTheme_NoActionBar);
                        break;
                    case UserPreferences.THEME_DARK:
                        setTheme(R.style.AppTheme_Dark_NoActionBar);
                        break;
                    case UserPreferences.THEME_BLACK_GLASS:
                        setTheme(R.style.AppTheme_BlackGlass_NoActionBar);
                        configureBlackGlassStatusBar();
                        break;
                    case UserPreferences.THEME_SYSTEM:
                    default:
                        // Follow system setting
                        if (isSystemInDarkMode()) {
                            setTheme(R.style.AppTheme_BlackGlass_NoActionBar);
                            configureBlackGlassStatusBar();
                        } else {
                            setTheme(R.style.AppTheme_NoActionBar);
                        }
                        break;
                }
            }
        } catch (Exception e) {
            // Fallback to default theme
            setTheme(R.style.AppTheme);
        }
    }

    /**
     * Configures status bar for Black Glass theme to prevent it from being blocked by system UI.
     */
    private void configureBlackGlassStatusBar() {
        try {
            // Set proper system bar colors without making them transparent
            getWindow().setStatusBarColor(getResources().getColor(R.color.deep_dark_blue, getTheme()));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.darkBackground, getTheme()));
            
            // Set status bar content to light (white icons/text) for dark theme
            View decorView = getWindow().getDecorView();
            WindowInsetsControllerCompat windowInsetsController = 
                new WindowInsetsControllerCompat(getWindow(), decorView);
            windowInsetsController.setAppearanceLightStatusBars(false);
            windowInsetsController.setAppearanceLightNavigationBars(false);
            
            // Ensure window handles system windows properly
            decorView.setSystemUiVisibility(0); // Clear any problematic flags
            
        } catch (Exception e) {
            // Fallback - just set status bar color
            try {
                getWindow().setStatusBarColor(getResources().getColor(R.color.deep_dark_blue, getTheme()));
                getWindow().setNavigationBarColor(getResources().getColor(R.color.darkBackground, getTheme()));
            } catch (Exception ex) {
                // Ultimate fallback with hardcoded colors
                getWindow().setStatusBarColor(0xFF0D1A2D); // deep_dark_blue
                getWindow().setNavigationBarColor(0xFF000000); // black
            }
        }
    }

    /**
     * Checks if the system is currently in dark mode.
     */
    private boolean isSystemInDarkMode() {
        try {
            int nightModeFlags = getResources().getConfiguration().uiMode & 
                Configuration.UI_MODE_NIGHT_MASK;
            return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Recreates the activity with fade animation.
     */
    protected void recreateWithFade() {
        try {
            // Apply transition animation
            recreate();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            // Fallback to simple recreate
            recreate();
        }
    }

    /**
     * Force refresh the theme for this activity.
     */
    public void refreshTheme() {
        recreateWithFade();
    }
}
