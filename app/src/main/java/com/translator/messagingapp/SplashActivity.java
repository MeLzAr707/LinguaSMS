package com.translator.messagingapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Use the regular app theme
        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);

        // Initialize app components
        initializeApp();

        // Start main activity immediately
        Intent mainIntent = new Intent(this, MainActivity.class);
        startActivity(mainIntent);

        // Close this activity
        finish();
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
            }

        } catch (Exception e) {
            android.util.Log.e("SplashActivity", "Error initializing app", e);
        }
    }
}





