package com.translator.messagingapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            // Use the regular app theme
            setTheme(R.style.AppTheme);
        } catch (Exception e) {
            android.util.Log.e("SplashActivity", "Error setting theme, using default", e);
            // Continue with default theme
        }

        super.onCreate(savedInstanceState);

        try {
            // Initialize app components
            initializeApp();
        } catch (Exception e) {
            android.util.Log.e("SplashActivity", "Error during app initialization", e);
            // Continue anyway - app might still be usable
        }

        try {
            // Start main activity immediately
            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
        } catch (Exception e) {
            android.util.Log.e("SplashActivity", "Error starting MainActivity", e);
            // This is critical - if we can't start MainActivity, show an error
            showErrorAndExit();
            return;
        }

        // Close this activity
        finish();
    }

    /**
     * Shows an error message and exits the app if critical initialization fails.
     */
    private void showErrorAndExit() {
        try {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Initialization Error")
                    .setMessage("The app failed to initialize properly. Please try restarting the app.")
                    .setPositiveButton("OK", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
        } catch (Exception e) {
            // If we can't even show a dialog, just finish
            finish();
        }
    }

    private void initializeApp() {
        // Initialize components that need to be ready before the main activity
        try {
            // Get user preferences
            UserPreferences userPreferences = new UserPreferences(this);

            // Apply theme based on user preference
            int themeId = userPreferences.getThemeId();
            if (themeId == UserPreferences.THEME_DARK ||
                    themeId == UserPreferences.THEME_BLACK_GLASS ||
                    (themeId == UserPreferences.THEME_SYSTEM && userPreferences.isDarkThemeActive(this))) {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
            }

            // Check if this is the first run
            if (userPreferences.getBoolean("is_first_run", true)) {
                userPreferences.setBoolean("is_first_run", false);
                String deviceLanguage = getResources().getConfiguration().locale.getLanguage();
                userPreferences.setPreferredLanguage(deviceLanguage);
                
                // On first run, ensure default SMS request will happen
                userPreferences.setBoolean("should_request_default_sms", true);
            }

        } catch (Exception e) {
            android.util.Log.e("SplashActivity", "Error initializing app", e);
        }
    }
}





