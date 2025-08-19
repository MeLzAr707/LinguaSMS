package com.translator.messagingapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

public class ColorWheelActivity extends BaseActivity {
    private static final String TAG = "ColorWheelActivity";

    private View colorPreview;
    private TextView selectedColorText;
    private GridLayout colorGrid;
    private RadioGroup componentRadioGroup;
    private Button applyColorButton;
    private Button resetColorsButton;
    
    private int selectedColor = Color.parseColor("#2196F3"); // Default blue
    
    // Predefined color palette
    private final int[] colorPalette = {
        Color.parseColor("#F44336"), // Red
        Color.parseColor("#E91E63"), // Pink
        Color.parseColor("#9C27B0"), // Purple
        Color.parseColor("#673AB7"), // Deep Purple
        Color.parseColor("#3F51B5"), // Indigo
        Color.parseColor("#2196F3"), // Blue
        Color.parseColor("#03A9F4"), // Light Blue
        Color.parseColor("#00BCD4"), // Cyan
        Color.parseColor("#009688"), // Teal
        Color.parseColor("#4CAF50"), // Green
        Color.parseColor("#8BC34A"), // Light Green
        Color.parseColor("#CDDC39"), // Lime
        Color.parseColor("#FFEB3B"), // Yellow
        Color.parseColor("#FFC107"), // Amber
        Color.parseColor("#FF9800"), // Orange
        Color.parseColor("#FF5722"), // Deep Orange
        Color.parseColor("#795548"), // Brown
        Color.parseColor("#9E9E9E"), // Grey
        Color.parseColor("#607D8B"), // Blue Grey
        Color.parseColor("#000000"), // Black
        Color.parseColor("#424242"), // Dark Grey
        Color.parseColor("#FFFFFF"), // White
        Color.parseColor("#0D1A2D"), // Deep Dark Blue
        Color.parseColor("#23023d")  // Dark Purple
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_wheel);

        setupToolbar();
        findViews();
        setupClickListeners();
        setupColorGrid();
        updateColorPreview();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.custom_theme_colors);
            toolbar.setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    private void findViews() {
        colorPreview = findViewById(R.id.color_preview);
        selectedColorText = findViewById(R.id.selected_color_text);
        colorGrid = findViewById(R.id.color_grid);
        componentRadioGroup = findViewById(R.id.component_radio_group);
        applyColorButton = findViewById(R.id.apply_color_button);
        resetColorsButton = findViewById(R.id.reset_colors_button);
    }

    private void setupClickListeners() {
        applyColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applySelectedColor();
            }
        });

        resetColorsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetToDefaultColors();
            }
        });
    }

    private void setupColorGrid() {
        for (int color : colorPalette) {
            View colorView = new View(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 120;
            params.height = 120;
            params.setMargins(8, 8, 8, 8);
            colorView.setLayoutParams(params);
            colorView.setBackgroundColor(color);
            
            // Add click listener
            colorView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedColor = color;
                    updateColorPreview();
                }
            });
            
            colorGrid.addView(colorView);
        }
    }

    private void updateColorPreview() {
        colorPreview.setBackgroundColor(selectedColor);
        selectedColorText.setText(String.format("#%06X", (0xFFFFFF & selectedColor)));
    }

    private void applySelectedColor() {
        int checkedId = componentRadioGroup.getCheckedRadioButtonId();
        
        if (checkedId == R.id.radio_nav_bar) {
            userPreferences.setCustomNavBarColor(selectedColor);
            Toast.makeText(this, "Navigation bar color applied", Toast.LENGTH_SHORT).show();
        } else if (checkedId == R.id.radio_top_bar) {
            userPreferences.setCustomTopBarColor(selectedColor);
            Toast.makeText(this, "Top bar color applied", Toast.LENGTH_SHORT).show();
        } else if (checkedId == R.id.radio_buttons) {
            userPreferences.setCustomButtonColor(selectedColor);
            Toast.makeText(this, "Button color applied", Toast.LENGTH_SHORT).show();
        } else if (checkedId == R.id.radio_menu) {
            userPreferences.setCustomMenuColor(selectedColor);
            Toast.makeText(this, "Menu color applied", Toast.LENGTH_SHORT).show();
        } else if (checkedId == R.id.radio_incoming_bubbles) {
            userPreferences.setCustomIncomingBubbleColor(selectedColor);
            Toast.makeText(this, "Incoming bubble color applied", Toast.LENGTH_SHORT).show();
        } else if (checkedId == R.id.radio_outgoing_bubbles) {
            userPreferences.setCustomOutgoingBubbleColor(selectedColor);
            Toast.makeText(this, "Outgoing bubble color applied", Toast.LENGTH_SHORT).show();
        }
        
        // Set custom primary color as well for general theming
        userPreferences.setCustomPrimaryColor(selectedColor);
        
        // Switch to custom theme if not already selected
        if (userPreferences.getThemeId() != UserPreferences.THEME_CUSTOM) {
            userPreferences.setThemeId(UserPreferences.THEME_CUSTOM);
            Toast.makeText(this, "Switched to custom theme", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetToDefaultColors() {
        // Reset to light theme defaults
        int defaultColor = getResources().getColor(R.color.colorPrimary);
        
        userPreferences.setCustomPrimaryColor(defaultColor);
        userPreferences.setCustomNavBarColor(defaultColor);
        userPreferences.setCustomTopBarColor(defaultColor);
        userPreferences.setCustomButtonColor(defaultColor);
        userPreferences.setCustomMenuColor(defaultColor);
        userPreferences.setCustomIncomingBubbleColor(getResources().getColor(R.color.background_light));
        userPreferences.setCustomOutgoingBubbleColor(defaultColor);
        
        selectedColor = defaultColor;
        updateColorPreview();
        
        Toast.makeText(this, "Colors reset to default", Toast.LENGTH_SHORT).show();
    }
}