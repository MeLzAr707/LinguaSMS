package com.translator.messagingapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Base activity class that handles common functionality like theme application
 * and error handling.
 */
public class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";
    protected UserPreferences userPreferences;

    // Add this flag to determine if NoActionBar should be used
    protected boolean useNoActionBar = false;
    
    // Track current theme to detect changes
    private int currentThemeId = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Initialize preferences
        userPreferences = new UserPreferences(this);

        // Apply theme before calling super.onCreate()
        applyTheme();

        super.onCreate(savedInstanceState);

        // Set up uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(this::handleUncaughtException);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Check if theme has changed while activity was in background
        if (userPreferences != null) {
            int newThemeId = userPreferences.getThemeId();
            if (currentThemeId != -1 && currentThemeId != newThemeId) {
                // Theme has changed, apply it dynamically
                recreateWithFade();
            }
        }
    }
    
    /**
     * Recreate the activity with a fade animation to make theme transitions smoother
     */
    protected void recreateWithFade() {
        // Use a fade animation for smoother transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        recreate();
    }

    /**
     * Apply the appropriate theme based on user preferences
     */
    protected void applyTheme() {
        int themeId = userPreferences.getThemeId();
        // Store the current theme ID to detect changes
        currentThemeId = themeId;

        if (useNoActionBar) {
            // Apply NoActionBar variants
            switch (themeId) {
                case UserPreferences.THEME_DARK:
                    setTheme(R.style.AppTheme_Dark_NoActionBar);
                    break;
                case UserPreferences.THEME_BLACK_GLASS:
                    setTheme(R.style.AppTheme_BlackGlass_NoActionBar);
                    // Apply additional window flags for Black Glass theme
                    applyBlackGlassWindowFlags();
                    break;
                case UserPreferences.THEME_SYSTEM:
                    // Use system default
                    if (userPreferences.isDarkThemeActive(this)) {
                        setTheme(R.style.AppTheme_Dark_NoActionBar);
                    } else {
                        setTheme(R.style.AppTheme_NoActionBar);
                    }
                    break;
                case UserPreferences.THEME_LIGHT:
                default:
                    setTheme(R.style.AppTheme_NoActionBar);
                    break;
            }
        } else {
            // Apply regular themes
            switch (themeId) {
                case UserPreferences.THEME_DARK:
                    setTheme(R.style.AppTheme_Dark);
                    break;
                case UserPreferences.THEME_BLACK_GLASS:
                    setTheme(R.style.AppTheme_BlackGlass);
                    // Apply additional window flags for Black Glass theme
                    applyBlackGlassWindowFlags();
                    break;
                case UserPreferences.THEME_SYSTEM:
                    // Use system default
                    if (userPreferences.isDarkThemeActive(this)) {
                        setTheme(R.style.AppTheme_Dark);
                    } else {
                        setTheme(R.style.AppTheme);
                    }
                    break;
                case UserPreferences.THEME_LIGHT:
                default:
                    setTheme(R.style.AppTheme);
                    break;
            }
        }
    }
    
    /**
     * Apply special window flags for the Black Glass theme
     */
    private void applyBlackGlassWindowFlags() {
        // These flags need to be set programmatically in addition to the theme attributes
        getWindow().setFlags(
            android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        
        // For Android 5.0+ devices, we need to handle the status bar and navigation bar properly
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        }
    }

    /**
     * Set whether this activity should use a NoActionBar theme
     */
    protected void setUseNoActionBar(boolean useNoActionBar) {
        this.useNoActionBar = useNoActionBar;
    }

    /**
     * Check if the black glass theme is being used
     */
    protected boolean isUsingBlackGlassTheme() {
        return userPreferences != null && userPreferences.isUsingBlackGlassTheme();
    }

    /**
     * Handle uncaught exceptions to prevent app crashes
     */
    private void handleUncaughtException(Thread thread, Throwable throwable) {
        Log.e(TAG, "Uncaught exception", throwable);
        try {
            // Show error message
            runOnUiThread(() -> {
                Toast.makeText(this,
                        "Error: " + throwable.getMessage(),
                        Toast.LENGTH_LONG).show();
            });

            // Log the error
            Log.e(TAG, "Uncaught exception", throwable);
        } catch (Exception e) {
            Log.e(TAG, "Error handling uncaught exception", e);
        }
    }
}


