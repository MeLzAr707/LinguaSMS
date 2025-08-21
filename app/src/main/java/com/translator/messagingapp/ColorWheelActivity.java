package com.translator.messagingapp;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Activity for selecting custom theme colors using a color wheel/picker interface.
 * This activity allows users to customize various UI element colors for the custom theme.
 */
public class ColorWheelActivity extends BaseActivity {
    private static final String TAG = "ColorWheelActivity";
    
    // UI components
    private Button applyButton;
    private Button resetButton;
    private GridLayout colorPaletteGrid;
    private GridLayout textColorPaletteGrid;
    private View backgroundColorPreview;
    private View textColorPreview;
    private LinearLayout combinedPreview;
    private TextView previewText;
    
    // Data
    private UserPreferences userPreferences;
    private int selectedBackgroundColor = 0xFF3F51B5; // Default blue color
    private int selectedTextColor = 0xFF000000; // Default black color
    
    // Color palettes
    private static final int[] BACKGROUND_COLORS = {
            0xFF2196F3, // Blue
            0xFF4CAF50, // Green
            0xFFFF9800, // Orange  
            0xFFE91E63, // Pink
            0xFF9C27B0, // Purple
            0xFF607D8B, // Blue Gray
            0xFF795548, // Brown
            0xFFFF5722, // Deep Orange
            // Darker shades
            0xFF1976D2, // Dark Blue
            0xFF388E3C, // Dark Green
            0xFFF57C00, // Dark Orange
            0xFFC2185B, // Dark Pink
            0xFF7B1FA2, // Dark Purple
            0xFF455A64, // Dark Blue Gray
            0xFF5D4037, // Dark Brown
            0xFFD84315  // Dark Deep Orange
    };
    
    private static final int[] TEXT_COLORS = {
            0xFF000000, // Black
            0xFF424242, // Dark Gray
            0xFF757575, // Gray
            0xFFFFFFFF, // White
            0xFF1976D2, // Dark Blue
            0xFF388E3C, // Dark Green
            0xFF7B1FA2, // Dark Purple
            0xFFD84315  // Dark Orange
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Ensure safe theme application for ColorWheelActivity
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_color_wheel);
        } catch (Exception e) {
            // If theme application fails, fallback to light theme and recreate
            Log.e(TAG, "Error applying custom theme, falling back to light theme", e);
            if (userPreferences != null) {
                userPreferences.setThemeId(UserPreferences.THEME_LIGHT);
                recreate();
                return;
            }
            throw e; // Re-throw if we can't handle it
        }
        
        userPreferences = new UserPreferences(this);
        
        setupToolbar();
        findViews();
        setupColorPalettes();
        setupClickListeners();
        loadCurrentColors();
        updatePreviews();
        updateColorSelection();
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
        colorPaletteGrid = findViewById(R.id.color_palette_grid);
        textColorPaletteGrid = findViewById(R.id.text_color_palette_grid);
        backgroundColorPreview = findViewById(R.id.background_color_preview);
        textColorPreview = findViewById(R.id.text_color_preview);
        combinedPreview = findViewById(R.id.combined_preview);
        previewText = findViewById(R.id.preview_text);
    }
    
    private void setupClickListeners() {
        if (applyButton != null) {
            applyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    applySelectedColors();
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
    
    private void setupColorPalettes() {
        setupBackgroundColorPalette();
        setupTextColorPalette();
    }
    
    private void setupBackgroundColorPalette() {
        if (colorPaletteGrid == null) return;
        
        for (int color : BACKGROUND_COLORS) {
            View colorView = createColorView(color, true);
            colorPaletteGrid.addView(colorView);
        }
    }
    
    private void setupTextColorPalette() {
        if (textColorPaletteGrid == null) return;
        
        for (int color : TEXT_COLORS) {
            View colorView = createColorView(color, false);
            textColorPaletteGrid.addView(colorView);
        }
    }
    
    private View createColorView(final int color, final boolean isBackgroundColor) {
        View colorView = new View(this);
        
        // Set size (convert dp to pixels) - making them slightly larger for better touch targets
        int sizeInDp = 72;
        int sizeInPx = (int) (sizeInDp * getResources().getDisplayMetrics().density);
        int marginInDp = 12;
        int marginInPx = (int) (marginInDp * getResources().getDisplayMetrics().density);
        
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = sizeInPx;
        params.height = sizeInPx;
        params.setMargins(marginInPx, marginInPx, marginInPx, marginInPx);
        colorView.setLayoutParams(params);
        
        // Create initial drawable with subtle border
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setStroke(2, android.graphics.Color.LTGRAY);
        drawable.setCornerRadius(8f);
        colorView.setBackground(drawable);
        
        // Add elevation for better visual depth
        colorView.setElevation(4f);
        
        // Set click listener
        colorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBackgroundColor) {
                    selectedBackgroundColor = color;
                } else {
                    selectedTextColor = color;
                }
                updatePreviews();
                updateColorSelection();
            }
        });
        
        return colorView;
    }
    
    private void updateColorSelection() {
        // Update background color grid selection
        for (int i = 0; i < colorPaletteGrid.getChildCount() && i < BACKGROUND_COLORS.length; i++) {
            View child = colorPaletteGrid.getChildAt(i);
            if (BACKGROUND_COLORS[i] == selectedBackgroundColor) {
                // Create a shape drawable with color and white border for selection
                android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
                drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                drawable.setColor(selectedBackgroundColor);
                drawable.setStroke(6, android.graphics.Color.WHITE);
                drawable.setCornerRadius(8f);
                child.setBackground(drawable);
                child.setElevation(8f);
            } else {
                // Create a shape drawable with color and subtle border for unselected
                android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
                drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                drawable.setColor(BACKGROUND_COLORS[i]);
                drawable.setStroke(2, android.graphics.Color.LTGRAY);
                drawable.setCornerRadius(8f);
                child.setBackground(drawable);
                child.setElevation(4f);
            }
        }
        
        // Update text color grid selection
        for (int i = 0; i < textColorPaletteGrid.getChildCount() && i < TEXT_COLORS.length; i++) {
            View child = textColorPaletteGrid.getChildAt(i);
            if (TEXT_COLORS[i] == selectedTextColor) {
                // Create a shape drawable with color and white border for selection
                android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
                drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                drawable.setColor(selectedTextColor);
                drawable.setStroke(6, android.graphics.Color.WHITE);
                drawable.setCornerRadius(8f);
                child.setBackground(drawable);
                child.setElevation(8f);
            } else {
                // Create a shape drawable with color and subtle border for unselected
                android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
                drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                drawable.setColor(TEXT_COLORS[i]);
                drawable.setStroke(2, android.graphics.Color.LTGRAY);
                drawable.setCornerRadius(8f);
                child.setBackground(drawable);
                child.setElevation(4f);
            }
        }
    }
    
    private void updatePreviews() {
        if (backgroundColorPreview != null) {
            backgroundColorPreview.setBackgroundColor(selectedBackgroundColor);
        }
        
        if (textColorPreview != null) {
            textColorPreview.setBackgroundColor(selectedTextColor);
        }
        
        if (combinedPreview != null && previewText != null) {
            combinedPreview.setBackgroundColor(selectedBackgroundColor);
            previewText.setTextColor(selectedTextColor);
        }
    }
    
    private void loadCurrentColors() {
        if (userPreferences != null && userPreferences.isUsingCustomTheme()) {
            int defaultBackgroundColor = getResources().getColor(android.R.color.holo_blue_dark);
            int defaultTextColor = getResources().getColor(android.R.color.black);
            
            selectedBackgroundColor = userPreferences.getCustomPrimaryColor(defaultBackgroundColor);
            selectedTextColor = userPreferences.getCustomTextColor(defaultTextColor);
        }
    }
    
    private void applySelectedColors() {
        // Apply the selected colors to various UI components
        if (userPreferences != null && userPreferences.isUsingCustomTheme()) {
            // Apply background color to navigation bar
            userPreferences.setCustomNavBarColor(selectedBackgroundColor);
            
            // Apply background color to top bar  
            userPreferences.setCustomTopBarColor(selectedBackgroundColor);
            
            // Apply background color to buttons
            userPreferences.setCustomButtonColor(selectedBackgroundColor);
            
            // Apply background color to menu
            userPreferences.setCustomMenuColor(selectedBackgroundColor);
            
            // Apply background color to message bubbles
            userPreferences.setCustomIncomingBubbleColor(selectedBackgroundColor);
            userPreferences.setCustomOutgoingBubbleColor(selectedBackgroundColor);
        }
        
        // Set as primary color
        userPreferences.setCustomPrimaryColor(selectedBackgroundColor);
        
        // Set text color
        userPreferences.setCustomTextColor(selectedTextColor);
        
        // Switch to custom theme if not already using it
        if (userPreferences.getThemeId() != UserPreferences.THEME_CUSTOM) {
            userPreferences.setThemeId(UserPreferences.THEME_CUSTOM);
        }
        
        finish();
    }
    
    private void resetToDefaults() {
        // Use safer color resolution methods with theme context
        int defaultBackgroundColor;
        int defaultTextColor;
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                defaultBackgroundColor = getResources().getColor(android.R.color.holo_blue_dark, getTheme());
                defaultTextColor = getResources().getColor(android.R.color.black, getTheme());
            } else {
                defaultBackgroundColor = getResources().getColor(android.R.color.holo_blue_dark);
                defaultTextColor = getResources().getColor(android.R.color.black);
            }
        } catch (Exception e) {
            // Fallback to hardcoded values if color resolution fails
            Log.w(TAG, "Failed to resolve default colors, using fallback values", e);
            defaultBackgroundColor = 0xFF33B5E5; // Holo blue dark equivalent
            defaultTextColor = 0xFF000000; // Black
        }
        
        userPreferences.setCustomPrimaryColor(defaultBackgroundColor);
        userPreferences.setCustomNavBarColor(defaultBackgroundColor);
        userPreferences.setCustomTopBarColor(defaultBackgroundColor);
        userPreferences.setCustomButtonColor(defaultBackgroundColor);
        userPreferences.setCustomMenuColor(defaultBackgroundColor);
        
        // Handle background_light color with safer resolution
        int backgroundLightColor;
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                backgroundLightColor = getResources().getColor(R.color.background_light, getTheme());
            } else {
                backgroundLightColor = getResources().getColor(R.color.background_light);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to resolve background_light color, using fallback", e);
            backgroundLightColor = 0xFFFFFFFF; // White fallback
        }
        
        userPreferences.setCustomIncomingBubbleColor(backgroundLightColor);
        userPreferences.setCustomOutgoingBubbleColor(defaultBackgroundColor);
        userPreferences.setCustomTextColor(defaultTextColor);
        
        selectedBackgroundColor = defaultBackgroundColor;
        selectedTextColor = defaultTextColor;
        
        // Update UI to reflect reset
        updatePreviews();
        updateColorSelection();
        updateButtonColors();
    }
    
    private void updateButtonColors() {
        if (userPreferences != null && userPreferences.isUsingCustomTheme()) {
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle back button properly
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Simply finish the activity to return to the previous screen
        finish();
    }
}