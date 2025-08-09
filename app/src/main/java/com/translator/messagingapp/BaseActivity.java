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
        
        // Configure window for OpenGL compatibility
        OpenGLCompatibilityHelper.configureWindowForOpenGL(this);
        
        super.onCreate(savedInstanceState);
    }

    /**
     * Applies the appropriate theme based on user preferences.
     */
    private void applyTheme() {
        try {
            OptimizedTranslatorApp app = (OptimizedTranslatorApp) getApplication();
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
     * Configures status bar for Black Glass theme to prevent it from being behind pull-down menu.
     */
    private void configureBlackGlassStatusBar() {
        try {
            // Use the OpenGL-safe helper to configure system bars
            int statusBarColor = getResources().getColor(R.color.deep_dark_blue, getTheme());
            int navigationBarColor = getResources().getColor(R.color.darkBackground, getTheme());
            
            OpenGLCompatibilityHelper.safelyConfigureSystemBars(this, statusBarColor, navigationBarColor);
            
            // Set status bar content to light (white icons/text) without layout conflicts
            View decorView = getWindow().getDecorView();
            WindowInsetsControllerCompat windowInsetsController = 
                new WindowInsetsControllerCompat(getWindow(), decorView);
            windowInsetsController.setAppearanceLightStatusBars(false);
            windowInsetsController.setAppearanceLightNavigationBars(false);
            
        } catch (Exception e) {
            // Fallback with hardcoded colors using the safe helper
            try {
                OpenGLCompatibilityHelper.safelyConfigureSystemBars(this, 0xFF0D1A2D, 0xFF0D1A2D);
            } catch (Exception ex) {
                // Ultimate fallback - log the issue for debugging
                android.util.Log.e("BaseActivity", "Failed to configure Black Glass status bar", ex);
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
    
    /**
     * Debug method to log OpenGL configuration.
     * Can be called when troubleshooting rendering issues.
     */
    public void debugOpenGLConfiguration() {
        if (BuildConfig.DEBUG) {
            OpenGLCompatibilityHelper.logWindowConfiguration(this);
        }
    }
}
