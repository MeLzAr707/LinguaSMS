package com.translator.messagingapp;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * Activity for selecting custom theme colors using individual color selection interface.
 * This activity allows users to customize various UI element colors for the custom theme.
 */
public class ColorWheelActivity extends BaseActivity {
    
    private UserPreferences userPreferences;
    
    // UI elements
    private View backgroundColorPreview;
    private View navBarColorPreview;
    private View buttonColorPreview;
    private View incomingBubbleColorPreview;
    private View outgoingBubbleColorPreview;
    private Button applyButton;
    private Button resetButton;
    
    // Current colors
    private int backgroundCurrentColor;
    private int navBarCurrentColor;
    private int buttonCurrentColor;
    private int incomingBubbleCurrentColor;
    private int outgoingBubbleCurrentColor;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_wheel);
        
        userPreferences = new UserPreferences(this);
        
        setupToolbar();
        findViews();
        setupClickListeners();
        loadCurrentColors();
        updatePreviews();
        
        // Apply custom colors to views if using custom theme
        applyCustomColorsToViews();
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
        backgroundColorPreview = findViewById(R.id.background_color_preview);
        navBarColorPreview = findViewById(R.id.nav_bar_color_preview);
        buttonColorPreview = findViewById(R.id.button_color_preview);
        incomingBubbleColorPreview = findViewById(R.id.incoming_bubble_color_preview);
        outgoingBubbleColorPreview = findViewById(R.id.outgoing_bubble_color_preview);
        applyButton = findViewById(R.id.apply_button);
        resetButton = findViewById(R.id.reset_button);
    }
    
    private void setupClickListeners() {
        // Background color selection
        backgroundColorPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPickerDialog("Background", backgroundCurrentColor, new ColorSelectedListener() {
                    @Override
                    public void onColorSelected(int color) {
                        backgroundCurrentColor = color;
                        updatePreviews();
                    }
                });
            }
        });
        
        // Navigation bar color selection
        navBarColorPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPickerDialog("Navigation Bar", navBarCurrentColor, new ColorSelectedListener() {
                    @Override
                    public void onColorSelected(int color) {
                        navBarCurrentColor = color;
                        updatePreviews();
                    }
                });
            }
        });
        
        // Button color selection
        buttonColorPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPickerDialog("Button", buttonCurrentColor, new ColorSelectedListener() {
                    @Override
                    public void onColorSelected(int color) {
                        buttonCurrentColor = color;
                        updatePreviews();
                    }
                });
            }
        });
        
        // Incoming bubble color selection
        incomingBubbleColorPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPickerDialog("Incoming Message", incomingBubbleCurrentColor, new ColorSelectedListener() {
                    @Override
                    public void onColorSelected(int color) {
                        incomingBubbleCurrentColor = color;
                        updatePreviews();
                    }
                });
            }
        });
        
        // Outgoing bubble color selection
        outgoingBubbleColorPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPickerDialog("Outgoing Message", outgoingBubbleCurrentColor, new ColorSelectedListener() {
                    @Override
                    public void onColorSelected(int color) {
                        outgoingBubbleCurrentColor = color;
                        updatePreviews();
                    }
                });
            }
        });
        
        // Apply button
        if (applyButton != null) {
            applyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applySelectedColors();
                }
            });
        }
        
        // Reset button
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
        // Load current colors or set defaults
        int defaultBlue = getResources().getColor(android.R.color.holo_blue_dark);
        int defaultBackground = Color.WHITE;
        int defaultIncoming = Color.parseColor("#E0E0E0");
        
        if (userPreferences.isUsingCustomTheme()) {
            backgroundCurrentColor = userPreferences.getCustomBackgroundColor(defaultBackground);
            navBarCurrentColor = userPreferences.getCustomNavBarColor(defaultBlue);
            buttonCurrentColor = userPreferences.getCustomButtonColor(defaultBlue);
            incomingBubbleCurrentColor = userPreferences.getCustomIncomingBubbleColor(defaultIncoming);
            outgoingBubbleCurrentColor = userPreferences.getCustomOutgoingBubbleColor(defaultBlue);
        } else {
            // Set defaults for new custom theme
            backgroundCurrentColor = defaultBackground;
            navBarCurrentColor = defaultBlue;
            buttonCurrentColor = defaultBlue;
            incomingBubbleCurrentColor = defaultIncoming;
            outgoingBubbleCurrentColor = defaultBlue;
        }
    }
    
    private void updatePreviews() {
        backgroundColorPreview.setBackgroundColor(backgroundCurrentColor);
        navBarColorPreview.setBackgroundColor(navBarCurrentColor);
        buttonColorPreview.setBackgroundColor(buttonCurrentColor);
        incomingBubbleColorPreview.setBackgroundColor(incomingBubbleCurrentColor);
        outgoingBubbleColorPreview.setBackgroundColor(outgoingBubbleCurrentColor);
        
        // Update button colors if using custom theme
        updateButtonColors();
    }
    
    private void showColorPickerDialog(String title, int currentColor, final ColorSelectedListener listener) {
        // Define a set of predefined colors
        final int[] colors = {
            Color.WHITE,
            Color.BLACK,
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.YELLOW,
            Color.CYAN,
            Color.MAGENTA,
            Color.parseColor("#FF5722"), // Deep Orange
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#9C27B0"), // Purple
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#607D8B"), // Blue Grey
            Color.parseColor("#795548"), // Brown
            Color.parseColor("#E0E0E0")  // Light Grey
        };
        
        final String[] colorNames = {
            "White", "Black", "Red", "Green", "Blue", "Yellow", 
            "Cyan", "Magenta", "Deep Orange", "Light Blue", "Light Green",
            "Purple", "Orange", "Blue Grey", "Brown", "Light Grey"
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select " + title + " Color");
        
        builder.setItems(colorNames, (dialog, which) -> {
            listener.onColorSelected(colors[which]);
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void applySelectedColors() {
        // Save all the custom colors
        userPreferences.setCustomBackgroundColor(backgroundCurrentColor);
        userPreferences.setCustomNavBarColor(navBarCurrentColor);
        userPreferences.setCustomTopBarColor(navBarCurrentColor); // Use same as nav bar
        userPreferences.setCustomButtonColor(buttonCurrentColor);
        userPreferences.setCustomMenuColor(navBarCurrentColor); // Use same as nav bar
        userPreferences.setCustomIncomingBubbleColor(incomingBubbleCurrentColor);
        userPreferences.setCustomOutgoingBubbleColor(outgoingBubbleCurrentColor);
        userPreferences.setCustomPrimaryColor(navBarCurrentColor);
        
        // Switch to custom theme
        userPreferences.setThemeId(UserPreferences.THEME_CUSTOM);
        
        Toast.makeText(this, "Custom theme applied!", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private void resetToDefaults() {
        int defaultBlue = getResources().getColor(android.R.color.holo_blue_dark);
        int defaultBackground = Color.WHITE;
        int defaultIncoming = Color.parseColor("#E0E0E0");
        
        // Reset all colors to defaults including background
        userPreferences.setCustomBackgroundColor(defaultBackground);
        userPreferences.setCustomPrimaryColor(defaultBlue);
        userPreferences.setCustomNavBarColor(defaultBlue);
        userPreferences.setCustomTopBarColor(defaultBlue);
        userPreferences.setCustomButtonColor(defaultBlue);
        userPreferences.setCustomMenuColor(defaultBlue);
        userPreferences.setCustomIncomingBubbleColor(defaultIncoming);
        userPreferences.setCustomOutgoingBubbleColor(defaultBlue);
        
        // Update local variables
        backgroundCurrentColor = defaultBackground;
        navBarCurrentColor = defaultBlue;
        buttonCurrentColor = defaultBlue;
        incomingBubbleCurrentColor = defaultIncoming;
        outgoingBubbleCurrentColor = defaultBlue;
        
        updatePreviews();
        Toast.makeText(this, "Colors reset to defaults", Toast.LENGTH_SHORT).show();
    }
    
    private void updateButtonColors() {
        if (applyButton != null) {
            applyButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(buttonCurrentColor));
        }
    }
    
    @Override
    protected void onThemeChanged() {
        super.onThemeChanged();
        updateButtonColors();
    }
    
    private interface ColorSelectedListener {
        void onColorSelected(int color);
    }
}