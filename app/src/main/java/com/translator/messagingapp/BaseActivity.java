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
        
        // Also invalidate options menu to refresh theme-dependent menu items
        invalidateOptionsMenu();
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
                case UserPreferences.THEME_CUSTOM:
                    setTheme(R.style.AppTheme_NoActionBar); // Base theme for custom
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
                case UserPreferences.THEME_CUSTOM:
                    setTheme(R.style.AppTheme); // Base theme for custom
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
        
        // Apply custom colors if using custom theme
        if (themeId == UserPreferences.THEME_CUSTOM) {
            applyCustomColors();
        }
    }
    
    /**
     * Apply special window configuration for the Black Glass theme
     * Uses safer methods to avoid layout overlap with system UI
     */
    private void applyBlackGlassWindowFlags() {
        // Use OpenGL-compatible system bar configuration to avoid overlap issues
        // This replaces the problematic FLAG_LAYOUT_NO_LIMITS
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            // Use safe system bar configuration that doesn't cause layout overlap
            OpenGLCompatibilityHelper.safelyConfigureSystemBars(
                this,
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            );
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
    
    /**
     * Force update theme without recreating activity (for menu and other UI elements)
     * Call this when theme changes and you want immediate visual feedback
     */
    protected void updateThemeImmediately() {
        // Update the current theme ID
        if (userPreferences != null) {
            currentThemeId = userPreferences.getThemeId();
        }
        
        // Invalidate options menu to apply theme changes
        invalidateOptionsMenu();
        
        // Notify subclasses to update their UI
        onThemeChanged();
    }
    
    /**
     * Override this method in subclasses to handle immediate theme changes
     * without activity recreation
     */
    protected void onThemeChanged() {
        // Default implementation does nothing
        // Subclasses can override to update their UI elements
    }
    
    /**
     * Apply custom colors when using custom theme
     */
    protected void applyCustomColors() {
        // This method will be called after setContentView() 
        // to apply custom colors to UI elements
        // Individual activities can override this to apply custom colors to their specific components
    }
    
    /**
     * Helper method to apply custom colors to common UI elements after view creation
     * Should be called from onCreate() after setContentView()
     */
    protected void applyCustomColorsToViews() {
        if (userPreferences.isUsingCustomTheme()) {
            // Apply custom colors to toolbar if present
            applyCustomColorsToToolbar();
        }
    }
    
    /**
     * Apply custom colors to toolbar
     */
    protected void applyCustomColorsToToolbar() {
        // Find toolbar and apply custom colors
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null && userPreferences.isUsingCustomTheme()) {
            int defaultColor = getResources().getColor(R.color.colorPrimary);
            int customTopBarColor = userPreferences.getCustomTopBarColor(defaultColor);
            toolbar.setBackgroundColor(customTopBarColor);
        }
    }
}


