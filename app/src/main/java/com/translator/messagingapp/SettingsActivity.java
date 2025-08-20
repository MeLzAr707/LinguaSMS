package com.translator.messagingapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import java.util.Locale;

public class SettingsActivity extends BaseActivity {
    private static final String TAG = "SettingsActivity";

    // Language configuration constants
    private static final String[] LANGUAGE_CODES = {
            "en", "es", "fr", "de", "it", "pt", "ru", "zh", "ja", "ko", "ar", "hi"
    };

    private static final String[] LANGUAGE_NAMES = {
            "English", "Spanish", "French", "German", "Italian", "Portuguese",
            "Russian", "Chinese", "Japanese", "Korean", "Arabic", "Hindi"
    };

    private EditText apiKeyInput;
    private Button selectIncomingLanguageButton;
    private Button selectOutgoingLanguageButton;
    private Button testApiKeyButton;
    private Button saveButton;
    private Button manageOfflineModelsButton;
    private Switch autoTranslateSwitch;
    private RadioGroup themeRadioGroup;
    private TextView incomingLanguageText;
    private TextView outgoingLanguageText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // BaseActivity will handle theme application and userPreferences initialization
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Simple toolbar setup without setSupportActionBar
        setupToolbar();

        // Find and initialize all UI components
        findViews();

        // Set up click listeners
        setupClickListeners();

        // Load saved preferences
        loadPreferences();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            // Set title directly on toolbar
            toolbar.setTitle(R.string.settings);

            // Set up back button
            toolbar.setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish(); // Close activity when back button is pressed
                }
            });
        }
    }

    private void findViews() {
        apiKeyInput = findViewById(R.id.api_key_input);
        selectIncomingLanguageButton = findViewById(R.id.select_incoming_language_button);
        selectOutgoingLanguageButton = findViewById(R.id.select_outgoing_language_button);
        testApiKeyButton = findViewById(R.id.test_api_key_button);
        saveButton = findViewById(R.id.save_button);
        manageOfflineModelsButton = findViewById(R.id.manage_offline_models_button);
        autoTranslateSwitch = findViewById(R.id.auto_translate_switch);
        themeRadioGroup = findViewById(R.id.theme_radio_group);
        incomingLanguageText = findViewById(R.id.incoming_language_text);
        outgoingLanguageText = findViewById(R.id.outgoing_language_text);
    }

    private void setupClickListeners() {
        // Set up incoming language button
        selectIncomingLanguageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SettingsActivity.this, "Opening language selection...", Toast.LENGTH_SHORT).show();
                showLanguageSelectionDialog("incoming");
            }
        });

        // Set up outgoing language button
        selectOutgoingLanguageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SettingsActivity.this, "Opening language selection...", Toast.LENGTH_SHORT).show();
                showLanguageSelectionDialog("outgoing");
            }
        });

        // Set up test API key button
        testApiKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SettingsActivity.this, "Testing API key...", Toast.LENGTH_SHORT).show();
                testApiKey();
            }
        });

        // Set up save button
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SettingsActivity.this, "Saving settings...", Toast.LENGTH_SHORT).show();
                saveSettings();
            }
        });

        // Set up manage offline models button
        manageOfflineModelsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOfflineModelsActivity();
            }
        });
    }

    private void loadPreferences() {
        // Load API key
        apiKeyInput.setText(userPreferences.getApiKey());

        // Load language preferences
        String incomingLanguage = userPreferences.getPreferredIncomingLanguage();
        if (!TextUtils.isEmpty(incomingLanguage)) {
            incomingLanguageText.setText(getLanguageName(incomingLanguage));
            incomingLanguageText.setVisibility(View.VISIBLE);
        } else {
            incomingLanguageText.setVisibility(View.GONE);
        }

        String outgoingLanguage = userPreferences.getPreferredOutgoingLanguage();
        if (!TextUtils.isEmpty(outgoingLanguage)) {
            outgoingLanguageText.setText(getLanguageName(outgoingLanguage));
            outgoingLanguageText.setVisibility(View.VISIBLE);
        } else {
            outgoingLanguageText.setVisibility(View.GONE);
        }

        // Load auto-translate setting
        autoTranslateSwitch.setChecked(userPreferences.isAutoTranslateEnabled());

        // Load theme setting
        int themeId = userPreferences.getThemeId();
        int radioButtonId;
        switch (themeId) {
            case UserPreferences.THEME_DARK:
                radioButtonId = R.id.radio_dark;
                break;
            case UserPreferences.THEME_BLACK_GLASS:
                radioButtonId = R.id.radio_black_glass;
                break;
            case UserPreferences.THEME_SYSTEM:
                radioButtonId = R.id.radio_system;
                break;
            case UserPreferences.THEME_CUSTOM:
                radioButtonId = R.id.radio_custom;
                break;
            case UserPreferences.THEME_LIGHT:
            default:
                radioButtonId = R.id.radio_light;
                break;
        }
        themeRadioGroup.check(radioButtonId);
    }

    private void showLanguageSelectionDialog(final String selectionType) {
        // Create dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Language");

        // Set items
        builder.setItems(LANGUAGE_NAMES, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String selectedCode = LANGUAGE_CODES[which];
                String selectedName = LANGUAGE_NAMES[which];

                // Save the selected language
                if ("incoming".equals(selectionType)) {
                    userPreferences.setPreferredIncomingLanguage(selectedCode);
                    incomingLanguageText.setText(selectedName);
                    incomingLanguageText.setVisibility(View.VISIBLE);
                    Toast.makeText(SettingsActivity.this, "Incoming language set to " + selectedName, Toast.LENGTH_SHORT).show();
                } else {
                    userPreferences.setPreferredOutgoingLanguage(selectedCode);
                    outgoingLanguageText.setText(selectedName);
                    outgoingLanguageText.setVisibility(View.VISIBLE);
                    Toast.makeText(SettingsActivity.this, "Outgoing language set to " + selectedName, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Show dialog
        builder.show();
    }

    private String getLanguageName(String languageCode) {
        try {
            Locale locale = new Locale(languageCode);
            return locale.getDisplayLanguage();
        } catch (Exception e) {
            return languageCode;
        }
    }

    private void testApiKey() {
        // Get API key from input field
        String apiKey = apiKeyInput.getText().toString().trim();

        if (TextUtils.isEmpty(apiKey)) {
            Toast.makeText(this, "Please enter an API key", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get translation service
        GoogleTranslationService translationService =
                ((TranslatorApp) getApplication()).getTranslationService();

        if (translationService != null) {
            translationService.setApiKey(apiKey);

            // Simple synchronous test
            boolean isValid = translationService.testApiKey();

            if (isValid) {
                Toast.makeText(this, "API key is valid", Toast.LENGTH_SHORT).show();
                userPreferences.setApiKey(apiKey);
            } else {
                Toast.makeText(this, "Invalid API key", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Translation service not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveSettings() {
        // Save API key
        String apiKey = apiKeyInput.getText().toString().trim();
        userPreferences.setApiKey(apiKey);

        // Update translation service
        GoogleTranslationService translationService =
                ((TranslatorApp) getApplication()).getTranslationService();
        if (translationService != null) {
            translationService.setApiKey(apiKey);
        }

        // Check if theme is changing
        int checkedId = themeRadioGroup.getCheckedRadioButtonId();
        int newThemeId;
        if (checkedId == R.id.radio_dark) {
            newThemeId = UserPreferences.THEME_DARK;
        } else if (checkedId == R.id.radio_black_glass) {
            newThemeId = UserPreferences.THEME_BLACK_GLASS;
        } else if (checkedId == R.id.radio_system) {
            newThemeId = UserPreferences.THEME_SYSTEM;
        } else if (checkedId == R.id.radio_custom) {
            newThemeId = UserPreferences.THEME_CUSTOM;
        } else {
            newThemeId = UserPreferences.THEME_LIGHT;
        }

        // Check if theme has changed
        boolean themeChanged = (newThemeId != userPreferences.getThemeId());

        // Save theme setting after checking for changes
        userPreferences.setThemeId(newThemeId);

        // Save auto-translate setting
        userPreferences.setAutoTranslateEnabled(autoTranslateSwitch.isChecked());

        // Show success message
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();

        // Apply theme changes with a smooth transition if needed
        if (themeChanged) {
            // Use a fade animation for smoother transition
            recreateWithFade();
        }
    }

    /**
     * Recreate the activity with a fade animation for smoother theme transitions
     */
    public void recreateWithFade() {
        // Use a fade animation for smoother transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        recreate();
    }

    /**
     * Opens the offline models management activity.
     */
    private void openOfflineModelsActivity() {
        Intent intent = new Intent(this, OfflineModelsActivity.class);
        startActivity(intent);
    }
    
    @Override
    protected void onThemeChanged() {
        super.onThemeChanged();
        
        // Apply custom theme colors if using custom theme
        applyCustomButtonColors();
    }
    
    /**
     * Apply custom button colors if using custom theme
     */
    private void applyCustomButtonColors() {
        if (userPreferences.isUsingCustomTheme()) {
            int defaultColor = getResources().getColor(android.R.color.holo_blue_dark);
            int customButtonColor = userPreferences.getCustomButtonColor(defaultColor);
            
            // Apply custom color to save button
            if (saveButton != null) {
                saveButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customButtonColor));
            }
            
            // Apply custom color to test API key button
            if (testApiKeyButton != null) {
                testApiKeyButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customButtonColor));
            }
            
            // Apply custom color to manage offline models button
            if (manageOfflineModelsButton != null) {
                manageOfflineModelsButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customButtonColor));
            }
            
            // Apply custom color to language selection buttons
            if (selectIncomingLanguageButton != null) {
                selectIncomingLanguageButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customButtonColor));
            }
            
            if (selectOutgoingLanguageButton != null) {
                selectOutgoingLanguageButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customButtonColor));
            }
        }
    }
}
