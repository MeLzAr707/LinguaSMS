package com.translator.messagingapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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





