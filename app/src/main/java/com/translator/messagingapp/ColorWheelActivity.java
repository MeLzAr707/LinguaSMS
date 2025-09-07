package com.translator.messagingapp;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for granular theme customization with dropdown color selection.
 * This activity allows users to customize individual UI element colors with color preview.
 */
public class ColorWheelActivity extends BaseActivity {
    private static final String TAG = "ColorWheelActivity";
    
    // UI components - Spinners
    private Spinner conversationBgSpinner;
    private Spinner messageViewBgSpinner;
    private Spinner mainTextSpinner;
    private Spinner buttonColorSpinner;
    private Spinner incomingTextSpinner;
    private Spinner outgoingTextSpinner;
    private Spinner incomingBubbleSpinner;
    private Spinner outgoingBubbleSpinner;
    private Spinner navHeaderSpinner;
    
    // UI components - Color previews
    private View conversationBgPreview;
    private View messageViewBgPreview;
    private View mainTextPreview;
    private View buttonColorPreview;
    private View incomingTextPreview;
    private View outgoingTextPreview;
    private View incomingBubblePreview;
    private View outgoingBubblePreview;
    private View navHeaderPreview;
    
    // UI components - Preview section
    private LinearLayout navPreview;
    private LinearLayout conversationPreview;
    private LinearLayout messageViewPreview;
    private LinearLayout incomingBubblePreviewContainer;
    private LinearLayout outgoingBubblePreviewContainer;
    private TextView incomingTextPreviewText;
    private TextView outgoingTextPreviewText;
    private TextView mainTextPreviewText;
    private Button buttonPreview;
    
    // UI components - Buttons
    private Button applyButton;
    private Button resetButton;
    
    // Data
    private UserPreferences userPreferences;
    
    // Selected colors
    private int selectedConversationBgColor = 0xFF3F51B5;
    private int selectedMessageViewBgColor = 0xFFFFFFFF;
    private int selectedMainTextColor = 0xFF000000;
    private int selectedButtonColor = 0xFF2196F3;
    private int selectedIncomingTextColor = 0xFF000000;
    private int selectedOutgoingTextColor = 0xFFFFFFFF;
    private int selectedIncomingBubbleColor = 0xFFE1F5FE;
    private int selectedOutgoingBubbleColor = 0xFFDCEDC8;
    private int selectedNavHeaderColor = 0xFF3F51B5;
    
    // Color options for dropdowns
    private static final ColorOption[] COLOR_OPTIONS = {
            new ColorOption("Blue", 0xFF2196F3),
            new ColorOption("Green", 0xFF4CAF50),
            new ColorOption("Orange", 0xFFFF9800),
            new ColorOption("Pink", 0xFFE91E63),
            new ColorOption("Purple", 0xFF9C27B0),
            new ColorOption("Blue Gray", 0xFF607D8B),
            new ColorOption("Brown", 0xFF795548),
            new ColorOption("Deep Orange", 0xFFFF5722),
            new ColorOption("Dark Blue", 0xFF1976D2),
            new ColorOption("Dark Green", 0xFF388E3C),
            new ColorOption("Dark Orange", 0xFFF57C00),
            new ColorOption("Dark Pink", 0xFFC2185B),
            new ColorOption("Dark Purple", 0xFF7B1FA2),
            new ColorOption("Red", 0xFFF44336),
            new ColorOption("Indigo", 0xFF3F51B5),
            new ColorOption("Teal", 0xFF009688),
            new ColorOption("Cyan", 0xFF00BCD4),
            new ColorOption("Lime", 0xFFCDDC39),
            new ColorOption("Yellow", 0xFFFFEB3B),
            new ColorOption("Amber", 0xFFFFC107),
            new ColorOption("Deep Purple", 0xFF673AB7),
            new ColorOption("Light Blue", 0xFF03A9F4),
            new ColorOption("Light Green", 0xFF8BC34A),
            new ColorOption("Light Gray", 0xFFE0E0E0),
            new ColorOption("Gray", 0xFF9E9E9E),
            new ColorOption("Dark Gray", 0xFF424242),
            new ColorOption("Black", 0xFF000000),
            new ColorOption("White", 0xFFFFFFFF)
    };
    
    private static class ColorOption {
        final String name;
        final int color;
        
        ColorOption(String name, int color) {
            this.name = name;
            this.color = color;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Ensure safe theme application for ColorWheelActivity
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_color_wheel);
        } catch (Exception e) {
            // If theme application fails, fallback to light theme but don't recreate
            // to avoid infinite loops. Instead, show error and finish.
            Log.e(TAG, "Error applying custom theme in ColorWheelActivity", e);
            if (userPreferences != null) {
                userPreferences.setThemeId(UserPreferences.THEME_LIGHT);
            }
            
            // Show error message and finish activity instead of recreating
            android.widget.Toast.makeText(this, 
                "Error loading custom theme. Returning to settings.", 
                android.widget.Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        userPreferences = new UserPreferences(this);
        
        setupToolbar();
        findViews();
        setupSpinners();
        setupClickListeners();
        loadCurrentColors();
        updatePreviews();
    }
    
    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Granular Theme Customization");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
    }
    
    private void findViews() {
        // Spinners
        conversationBgSpinner = findViewById(R.id.conversation_bg_spinner);
        messageViewBgSpinner = findViewById(R.id.message_view_bg_spinner);
        mainTextSpinner = findViewById(R.id.main_text_spinner);
        buttonColorSpinner = findViewById(R.id.button_color_spinner);
        incomingTextSpinner = findViewById(R.id.incoming_text_spinner);
        outgoingTextSpinner = findViewById(R.id.outgoing_text_spinner);
        incomingBubbleSpinner = findViewById(R.id.incoming_bubble_spinner);
        outgoingBubbleSpinner = findViewById(R.id.outgoing_bubble_spinner);
        navHeaderSpinner = findViewById(R.id.nav_header_spinner);
        
        // Color previews
        conversationBgPreview = findViewById(R.id.conversation_bg_preview);
        messageViewBgPreview = findViewById(R.id.message_view_bg_preview);
        mainTextPreview = findViewById(R.id.main_text_preview);
        buttonColorPreview = findViewById(R.id.button_color_preview);
        incomingTextPreview = findViewById(R.id.incoming_text_preview);
        outgoingTextPreview = findViewById(R.id.outgoing_text_preview);
        incomingBubblePreview = findViewById(R.id.incoming_bubble_preview);
        outgoingBubblePreview = findViewById(R.id.outgoing_bubble_preview);
        navHeaderPreview = findViewById(R.id.nav_header_preview);
        
        // Preview section
        navPreview = findViewById(R.id.nav_preview);
        conversationPreview = findViewById(R.id.conversation_preview);
        messageViewPreview = findViewById(R.id.message_view_preview);
        incomingBubblePreviewContainer = findViewById(R.id.incoming_bubble_preview_container);
        outgoingBubblePreviewContainer = findViewById(R.id.outgoing_bubble_preview_container);
        incomingTextPreviewText = findViewById(R.id.incoming_text_preview_text);
        outgoingTextPreviewText = findViewById(R.id.outgoing_text_preview_text);
        mainTextPreviewText = findViewById(R.id.main_text_preview_text);
        buttonPreview = findViewById(R.id.button_preview);
        
        // Buttons
        applyButton = findViewById(R.id.apply_button);
        resetButton = findViewById(R.id.reset_button);
    }
    
    private void setupSpinners() {
        ArrayAdapter<ColorOption> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, COLOR_OPTIONS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        // Set adapters for all spinners
        conversationBgSpinner.setAdapter(adapter);
        messageViewBgSpinner.setAdapter(adapter);
        mainTextSpinner.setAdapter(adapter);
        buttonColorSpinner.setAdapter(adapter);
        incomingTextSpinner.setAdapter(adapter);
        outgoingTextSpinner.setAdapter(adapter);
        incomingBubbleSpinner.setAdapter(adapter);
        outgoingBubbleSpinner.setAdapter(adapter);
        navHeaderSpinner.setAdapter(adapter);
        
        // Set up listeners
        setupSpinnerListener(conversationBgSpinner, (color) -> {
            selectedConversationBgColor = color;
            updatePreviews();
        });
        
        setupSpinnerListener(messageViewBgSpinner, (color) -> {
            selectedMessageViewBgColor = color;
            updatePreviews();
        });
        
        setupSpinnerListener(mainTextSpinner, (color) -> {
            selectedMainTextColor = color;
            updatePreviews();
        });
        
        setupSpinnerListener(buttonColorSpinner, (color) -> {
            selectedButtonColor = color;
            updatePreviews();
        });
        
        setupSpinnerListener(incomingTextSpinner, (color) -> {
            selectedIncomingTextColor = color;
            updatePreviews();
        });
        
        setupSpinnerListener(outgoingTextSpinner, (color) -> {
            selectedOutgoingTextColor = color;
            updatePreviews();
        });
        
        setupSpinnerListener(incomingBubbleSpinner, (color) -> {
            selectedIncomingBubbleColor = color;
            updatePreviews();
        });
        
        setupSpinnerListener(outgoingBubbleSpinner, (color) -> {
            selectedOutgoingBubbleColor = color;
            updatePreviews();
        });
        
        setupSpinnerListener(navHeaderSpinner, (color) -> {
            selectedNavHeaderColor = color;
            updatePreviews();
        });
    }
    
    private void setupSpinnerListener(Spinner spinner, ColorChangeListener listener) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ColorOption selectedOption = COLOR_OPTIONS[position];
                listener.onColorChanged(selectedOption.color);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    private interface ColorChangeListener {
        void onColorChanged(int color);
    }
    
    private void setupClickListeners() {
        applyButton.setOnClickListener(v -> applySelectedColors());
        resetButton.setOnClickListener(v -> resetToDefaults());
    }
    
    private void loadCurrentColors() {
        if (userPreferences == null) return;
        
        // Load current colors from preferences
        selectedConversationBgColor = userPreferences.getCustomBackgroundColor(0xFF3F51B5);
        selectedMessageViewBgColor = userPreferences.getCustomMessageViewBackgroundColor(0xFFFFFFFF);
        selectedMainTextColor = userPreferences.getCustomTextColor(0xFF000000);
        selectedButtonColor = userPreferences.getCustomButtonColor(0xFF2196F3);
        selectedIncomingTextColor = userPreferences.getCustomIncomingBubbleTextColor(0xFF000000);
        selectedOutgoingTextColor = userPreferences.getCustomOutgoingBubbleTextColor(0xFFFFFFFF);
        selectedIncomingBubbleColor = userPreferences.getCustomIncomingBubbleColor(0xFFE1F5FE);
        selectedOutgoingBubbleColor = userPreferences.getCustomOutgoingBubbleColor(0xFFDCEDC8);
        selectedNavHeaderColor = userPreferences.getCustomNavBarColor(0xFF3F51B5);
        
        // Set spinner selections
        setSpinnerSelection(conversationBgSpinner, selectedConversationBgColor);
        setSpinnerSelection(messageViewBgSpinner, selectedMessageViewBgColor);
        setSpinnerSelection(mainTextSpinner, selectedMainTextColor);
        setSpinnerSelection(buttonColorSpinner, selectedButtonColor);
        setSpinnerSelection(incomingTextSpinner, selectedIncomingTextColor);
        setSpinnerSelection(outgoingTextSpinner, selectedOutgoingTextColor);
        setSpinnerSelection(incomingBubbleSpinner, selectedIncomingBubbleColor);
        setSpinnerSelection(outgoingBubbleSpinner, selectedOutgoingBubbleColor);
        setSpinnerSelection(navHeaderSpinner, selectedNavHeaderColor);
    }
    
    private void setSpinnerSelection(Spinner spinner, int color) {
        for (int i = 0; i < COLOR_OPTIONS.length; i++) {
            if (COLOR_OPTIONS[i].color == color) {
                spinner.setSelection(i);
                break;
            }
        }
    }
    
    private void updatePreviews() {
        // Update color preview squares
        if (conversationBgPreview != null) {
            conversationBgPreview.setBackgroundColor(selectedConversationBgColor);
        }
        if (messageViewBgPreview != null) {
            messageViewBgPreview.setBackgroundColor(selectedMessageViewBgColor);
        }
        if (mainTextPreview != null) {
            mainTextPreview.setBackgroundColor(selectedMainTextColor);
        }
        if (buttonColorPreview != null) {
            buttonColorPreview.setBackgroundColor(selectedButtonColor);
        }
        if (incomingTextPreview != null) {
            incomingTextPreview.setBackgroundColor(selectedIncomingTextColor);
        }
        if (outgoingTextPreview != null) {
            outgoingTextPreview.setBackgroundColor(selectedOutgoingTextColor);
        }
        if (incomingBubblePreview != null) {
            incomingBubblePreview.setBackgroundColor(selectedIncomingBubbleColor);
        }
        if (outgoingBubblePreview != null) {
            outgoingBubblePreview.setBackgroundColor(selectedOutgoingBubbleColor);
        }
        if (navHeaderPreview != null) {
            navHeaderPreview.setBackgroundColor(selectedNavHeaderColor);
        }
        
        // Update main preview section
        if (navPreview != null) {
            navPreview.setBackgroundColor(selectedNavHeaderColor);
        }
        if (conversationPreview != null) {
            conversationPreview.setBackgroundColor(selectedConversationBgColor);
        }
        if (messageViewPreview != null) {
            messageViewPreview.setBackgroundColor(selectedMessageViewBgColor);
        }
        if (incomingBubblePreviewContainer != null) {
            incomingBubblePreviewContainer.setBackgroundColor(selectedIncomingBubbleColor);
        }
        if (outgoingBubblePreviewContainer != null) {
            outgoingBubblePreviewContainer.setBackgroundColor(selectedOutgoingBubbleColor);
        }
        if (incomingTextPreviewText != null) {
            incomingTextPreviewText.setTextColor(selectedIncomingTextColor);
        }
        if (outgoingTextPreviewText != null) {
            outgoingTextPreviewText.setTextColor(selectedOutgoingTextColor);
        }
        if (mainTextPreviewText != null) {
            mainTextPreviewText.setTextColor(selectedMainTextColor);
        }
        if (buttonPreview != null) {
            buttonPreview.setBackgroundTintList(android.content.res.ColorStateList.valueOf(selectedButtonColor));
        }
    }
    
    private void applySelectedColors() {
        if (userPreferences == null) return;
        
        // Apply the selected colors to user preferences
        userPreferences.setCustomBackgroundColor(selectedConversationBgColor);
        userPreferences.setCustomMessageViewBackgroundColor(selectedMessageViewBgColor);
        userPreferences.setCustomTextColor(selectedMainTextColor);
        userPreferences.setCustomButtonColor(selectedButtonColor);
        userPreferences.setCustomIncomingBubbleTextColor(selectedIncomingTextColor);
        userPreferences.setCustomOutgoingBubbleTextColor(selectedOutgoingTextColor);
        userPreferences.setCustomIncomingBubbleColor(selectedIncomingBubbleColor);
        userPreferences.setCustomOutgoingBubbleColor(selectedOutgoingBubbleColor);
        userPreferences.setCustomNavBarColor(selectedNavHeaderColor);
        
        // Also set as primary color for compatibility
        userPreferences.setCustomPrimaryColor(selectedNavHeaderColor);
        
        // Set theme to custom
        userPreferences.setThemeId(UserPreferences.THEME_CUSTOM);
        
        android.widget.Toast.makeText(this, "Colors applied successfully!", 
            android.widget.Toast.LENGTH_SHORT).show();
        
        // Restart activity to show theme changes
        recreate();
    }
    
    private void resetToDefaults() {
        // Reset to default colors
        selectedConversationBgColor = 0xFF3F51B5;
        selectedMessageViewBgColor = 0xFFFFFFFF;
        selectedMainTextColor = 0xFF000000;
        selectedButtonColor = 0xFF2196F3;
        selectedIncomingTextColor = 0xFF000000;
        selectedOutgoingTextColor = 0xFFFFFFFF;
        selectedIncomingBubbleColor = 0xFFE1F5FE;
        selectedOutgoingBubbleColor = 0xFFDCEDC8;
        selectedNavHeaderColor = 0xFF3F51B5;
        
        // Update spinner selections
        setSpinnerSelection(conversationBgSpinner, selectedConversationBgColor);
        setSpinnerSelection(messageViewBgSpinner, selectedMessageViewBgColor);
        setSpinnerSelection(mainTextSpinner, selectedMainTextColor);
        setSpinnerSelection(buttonColorSpinner, selectedButtonColor);
        setSpinnerSelection(incomingTextSpinner, selectedIncomingTextColor);
        setSpinnerSelection(outgoingTextSpinner, selectedOutgoingTextColor);
        setSpinnerSelection(incomingBubbleSpinner, selectedIncomingBubbleColor);
        setSpinnerSelection(outgoingBubbleSpinner, selectedOutgoingBubbleColor);
        setSpinnerSelection(navHeaderSpinner, selectedNavHeaderColor);
        
        updatePreviews();
    }
    
    @Override
    protected void onThemeChanged() {
        super.onThemeChanged();
        updatePreviews();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}