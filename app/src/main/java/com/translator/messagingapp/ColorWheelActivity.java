package com.translator.messagingapp;

import android.graphics.Color;
import android.os.Bundle;
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
    private GridLayout menuColorPaletteGrid;
    private GridLayout navColorPaletteGrid;
    private GridLayout buttonColorPaletteGrid;
    private GridLayout incomingColorPaletteGrid;
    private GridLayout outgoingColorPaletteGrid;
    private LinearLayout combinedPreview;
    private TextView previewText;
    
    // Data
    private UserPreferences userPreferences;
    
    // Individual color selections
    private int selectedMenuColor = 0xFF3F51B5; // Default blue color
    private int selectedNavBarColor = 0xFF3F51B5; // Default blue color
    private int selectedButtonColor = 0xFF3F51B5; // Default blue color
    private int selectedIncomingColor = 0xFFE3F2FD; // Light blue for incoming
    private int selectedOutgoingColor = 0xFF3F51B5; // Default blue for outgoing
    
    // Color types for individual customization
    private static final int COLOR_TYPE_MENU = 1;
    private static final int COLOR_TYPE_NAV_BAR = 2;
    private static final int COLOR_TYPE_BUTTON = 3;
    private static final int COLOR_TYPE_INCOMING = 4;
    private static final int COLOR_TYPE_OUTGOING = 5;
    
    // Color palettes
    private static final int[] UI_COLORS = {
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
    
    private static final int[] MESSAGE_COLORS = {
            0xFFE3F2FD, // Light Blue
            0xFFE8F5E8, // Light Green
            0xFFFFF3E0, // Light Orange
            0xFFFCE4EC, // Light Pink
            0xFFF3E5F5, // Light Purple
            0xFFECEFF1, // Light Blue Gray
            0xFFEFEBE9, // Light Brown
            0xFFFBE9E7, // Light Deep Orange
            // Medium tones for contrast
            0xFFBBDEFB, // Medium Blue
            0xFFC8E6C9, // Medium Green
            0xFFFFE0B2, // Medium Orange
            0xFFF8BBD9, // Medium Pink
            0xFFE1BEE7, // Medium Purple
            0xFFCFD8DC, // Medium Blue Gray
            0xFFD7CCC8, // Medium Brown
            0xFFFFCCBC  // Medium Deep Orange
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_wheel);
        
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
        menuColorPaletteGrid = findViewById(R.id.menu_color_palette_grid);
        navColorPaletteGrid = findViewById(R.id.nav_color_palette_grid);
        buttonColorPaletteGrid = findViewById(R.id.button_color_palette_grid);
        incomingColorPaletteGrid = findViewById(R.id.incoming_color_palette_grid);
        outgoingColorPaletteGrid = findViewById(R.id.outgoing_color_palette_grid);
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
        setupMenuColorPalette();
        setupNavBarColorPalette();
        setupButtonColorPalette();
        setupIncomingColorPalette();
        setupOutgoingColorPalette();
    }
    
    private void setupMenuColorPalette() {
        if (menuColorPaletteGrid == null) return;
        
        for (int color : UI_COLORS) {
            View colorView = createColorView(color, COLOR_TYPE_MENU);
            menuColorPaletteGrid.addView(colorView);
        }
    }
    
    private void setupNavBarColorPalette() {
        if (navColorPaletteGrid == null) return;
        
        for (int color : UI_COLORS) {
            View colorView = createColorView(color, COLOR_TYPE_NAV_BAR);
            navColorPaletteGrid.addView(colorView);
        }
    }
    
    private void setupButtonColorPalette() {
        if (buttonColorPaletteGrid == null) return;
        
        for (int color : UI_COLORS) {
            View colorView = createColorView(color, COLOR_TYPE_BUTTON);
            buttonColorPaletteGrid.addView(colorView);
        }
    }
    
    private void setupIncomingColorPalette() {
        if (incomingColorPaletteGrid == null) return;
        
        for (int color : MESSAGE_COLORS) {
            View colorView = createColorView(color, COLOR_TYPE_INCOMING);
            incomingColorPaletteGrid.addView(colorView);
        }
    }
    
    private void setupOutgoingColorPalette() {
        if (outgoingColorPaletteGrid == null) return;
        
        for (int color : MESSAGE_COLORS) {
            View colorView = createColorView(color, COLOR_TYPE_OUTGOING);
            outgoingColorPaletteGrid.addView(colorView);
        }
    }
    
    private View createColorView(final int color, final int colorType) {
        View colorView = new View(this);
        
        // Set size (convert dp to pixels) - reduced from 60dp to 40dp for smaller squares
        int sizeInDp = 40;
        int sizeInPx = (int) (sizeInDp * getResources().getDisplayMetrics().density);
        int marginInDp = 6;
        int marginInPx = (int) (marginInDp * getResources().getDisplayMetrics().density);
        
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = sizeInPx;
        params.height = sizeInPx;
        params.setMargins(marginInPx, marginInPx, marginInPx, marginInPx);
        colorView.setLayoutParams(params);
        
        // Set background color
        colorView.setBackgroundColor(color);
        
        // Add selection border (initially transparent)
        colorView.setPadding(4, 4, 4, 4);
        
        // Set click listener
        colorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (colorType) {
                    case COLOR_TYPE_MENU:
                        selectedMenuColor = color;
                        break;
                    case COLOR_TYPE_NAV_BAR:
                        selectedNavBarColor = color;
                        break;
                    case COLOR_TYPE_BUTTON:
                        selectedButtonColor = color;
                        break;
                    case COLOR_TYPE_INCOMING:
                        selectedIncomingColor = color;
                        break;
                    case COLOR_TYPE_OUTGOING:
                        selectedOutgoingColor = color;
                        break;
                }
                updatePreviews();
                updateColorSelection();
            }
        });
        
        return colorView;
    }
    
    private void updateColorSelection() {
        updateGridSelection(menuColorPaletteGrid, UI_COLORS, selectedMenuColor);
        updateGridSelection(navColorPaletteGrid, UI_COLORS, selectedNavBarColor);
        updateGridSelection(buttonColorPaletteGrid, UI_COLORS, selectedButtonColor);
        updateGridSelection(incomingColorPaletteGrid, MESSAGE_COLORS, selectedIncomingColor);
        updateGridSelection(outgoingColorPaletteGrid, MESSAGE_COLORS, selectedOutgoingColor);
    }
    
    private void updateGridSelection(GridLayout grid, int[] colors, int selectedColor) {
        if (grid == null) return;
        
        for (int i = 0; i < grid.getChildCount() && i < colors.length; i++) {
            View child = grid.getChildAt(i);
            if (colors[i] == selectedColor) {
                child.setBackground(getResources().getDrawable(android.R.drawable.btn_default));
                child.setBackgroundColor(selectedColor);
                child.setPadding(8, 8, 8, 8);
            } else {
                child.setBackgroundColor(colors[i]);
                child.setPadding(4, 4, 4, 4);
            }
        }
    }
    
    private void updatePreviews() {
        if (combinedPreview != null && previewText != null) {
            // Use the navigation bar color as the main background for preview
            combinedPreview.setBackgroundColor(selectedNavBarColor);
            // Use white text for visibility
            previewText.setTextColor(0xFFFFFFFF);
        }
    }
    
    private void loadCurrentColors() {
        if (userPreferences.isUsingCustomTheme()) {
            int defaultColor = getResources().getColor(android.R.color.holo_blue_dark);
            int defaultLightColor = getResources().getColor(R.color.background_light);
            
            selectedMenuColor = userPreferences.getCustomMenuColor(defaultColor);
            selectedNavBarColor = userPreferences.getCustomNavBarColor(defaultColor);
            selectedButtonColor = userPreferences.getCustomButtonColor(defaultColor);
            selectedIncomingColor = userPreferences.getCustomIncomingBubbleColor(defaultLightColor);
            selectedOutgoingColor = userPreferences.getCustomOutgoingBubbleColor(defaultColor);
        }
    }
    
    private void applySelectedColors() {
        // Apply the individual selected colors to their respective UI components
        if (userPreferences.isUsingCustomTheme()) {
            // Apply individual colors to their specific components
            userPreferences.setCustomMenuColor(selectedMenuColor);
            userPreferences.setCustomNavBarColor(selectedNavBarColor);  
            userPreferences.setCustomTopBarColor(selectedNavBarColor); // Use nav bar color for consistency
            userPreferences.setCustomButtonColor(selectedButtonColor);
            userPreferences.setCustomIncomingBubbleColor(selectedIncomingColor);
            userPreferences.setCustomOutgoingBubbleColor(selectedOutgoingColor);
        }
        
        // Set the nav bar color as primary color for backwards compatibility
        userPreferences.setCustomPrimaryColor(selectedNavBarColor);
        
        // Switch to custom theme if not already using it
        if (userPreferences.getThemeId() != UserPreferences.THEME_CUSTOM) {
            userPreferences.setThemeId(UserPreferences.THEME_CUSTOM);
        }
        
        finish();
    }
    
    private void resetToDefaults() {
        int defaultColor = getResources().getColor(android.R.color.holo_blue_dark);
        int defaultLightColor = getResources().getColor(R.color.background_light);
        
        // Reset all individual colors to defaults
        userPreferences.setCustomMenuColor(defaultColor);
        userPreferences.setCustomNavBarColor(defaultColor);
        userPreferences.setCustomTopBarColor(defaultColor);
        userPreferences.setCustomButtonColor(defaultColor);
        userPreferences.setCustomIncomingBubbleColor(defaultLightColor);
        userPreferences.setCustomOutgoingBubbleColor(defaultColor);
        userPreferences.setCustomPrimaryColor(defaultColor);
        
        // Reset local selections
        selectedMenuColor = defaultColor;
        selectedNavBarColor = defaultColor;
        selectedButtonColor = defaultColor;
        selectedIncomingColor = defaultLightColor;
        selectedOutgoingColor = defaultColor;
        
        // Update UI to reflect reset
        updatePreviews();
        updateColorSelection();
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