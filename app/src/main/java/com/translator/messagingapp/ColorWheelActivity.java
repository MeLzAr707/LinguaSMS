package com.translator.messagingapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Activity for selecting custom theme colors using a color wheel/picker interface.
 * This activity allows users to customize various UI element colors for the custom theme.
 */
public class ColorWheelActivity extends BaseActivity {
    private static final String TAG = "ColorWheelActivity";
    
    // UI components
    private Button applyButton;
    private Button resetButton;
    
    // Data
    private UserPreferences userPreferences;
    private int selectedColor = 0xFF3F51B5; // Default blue color
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_wheel);
        
        userPreferences = new UserPreferences(this);
        
        setupToolbar();
        findViews();
        setupClickListeners();
        loadCurrentColors();
    }
    
    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Custom Colors");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
    }
    
    private void findViews() {
        applyButton = findViewById(R.id.apply_button);
        resetButton = findViewById(R.id.reset_button);
    }
    
    private void setupClickListeners() {
        if (applyButton != null) {
            applyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applySelectedColor();
                }
            });
        }
        
        if (resetButton != null) {
            resetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetToDefaults();
                }
            });
        }
    }
    
    private void loadCurrentColors() {
        if (userPreferences.isUsingCustomTheme()) {
            int defaultColor = getResources().getColor(android.R.color.holo_blue_dark);
            selectedColor = userPreferences.getCustomPrimaryColor(defaultColor);
        }
    }
    
    private void applySelectedColor() {
        // Apply the selected color to various UI components
        if (userPreferences.isUsingCustomTheme()) {
            // Apply to navigation bar
            userPreferences.setCustomNavBarColor(selectedColor);
            
            // Apply to top bar  
            userPreferences.setCustomTopBarColor(selectedColor);
            
            // Apply to buttons
            userPreferences.setCustomButtonColor(selectedColor);
            
            // Apply to menu
            userPreferences.setCustomMenuColor(selectedColor);
            
            // Apply to message bubbles
            userPreferences.setCustomIncomingBubbleColor(selectedColor);
            userPreferences.setCustomOutgoingBubbleColor(selectedColor);
        }
        
        // Set as primary color
        userPreferences.setCustomPrimaryColor(selectedColor);
        
        // Switch to custom theme if not already using it
        if (userPreferences.getThemeId() != UserPreferences.THEME_CUSTOM) {
            userPreferences.setThemeId(UserPreferences.THEME_CUSTOM);
        }
        
        finish();
    }
    
    private void resetToDefaults() {
        int defaultColor = getResources().getColor(android.R.color.holo_blue_dark);
        
        userPreferences.setCustomPrimaryColor(defaultColor);
        userPreferences.setCustomNavBarColor(defaultColor);
        userPreferences.setCustomTopBarColor(defaultColor);
        userPreferences.setCustomButtonColor(defaultColor);
        userPreferences.setCustomMenuColor(defaultColor);
        userPreferences.setCustomIncomingBubbleColor(getResources().getColor(R.color.background_light));
        userPreferences.setCustomOutgoingBubbleColor(defaultColor);
        
        selectedColor = defaultColor;
        
        // Update UI to reflect reset
        updateButtonColors();
    }
    
    private void updateButtonColors() {
        if (userPreferences.isUsingCustomTheme()) {
            int defaultColor = getResources().getColor(android.R.color.holo_blue_dark);
            int customButtonColor = userPreferences.getCustomButtonColor(defaultColor);
            
            if (applyButton != null) {
                applyButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customButtonColor));
            }
            
            if (resetButton != null) {
                resetButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customButtonColor));
            }
        }
    }
    
    @Override
    protected void onThemeChanged() {
        super.onThemeChanged();
        updateButtonColors();
    }
}