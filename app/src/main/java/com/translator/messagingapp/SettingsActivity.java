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
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import java.util.Locale;

public class SettingsActivity extends BaseActivity {
    private static final String TAG = "SettingsActivity";

    private EditText apiKeyInput;
    private Button selectIncomingLanguageButton;
    private Button selectOutgoingLanguageButton;
    private Button testApiKeyButton;
    private Button saveButton;
    private Button manageOfflineModelsButton;
    private Switch autoTranslateSwitch;
    private Switch ttsEnabledSwitch;
    private SeekBar ttsSpeedSeekBar;
    private TextView ttsSpeedLabel;
    private Button selectTTSLanguageButton;
    private TextView ttsLanguageText;
    private Switch ttsReadOriginalSwitch;
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
        
        // TTS components
        ttsEnabledSwitch = findViewById(R.id.tts_enabled_switch);
        ttsSpeedSeekBar = findViewById(R.id.tts_speed_seekbar);
        ttsSpeedLabel = findViewById(R.id.tts_speed_label);
        selectTTSLanguageButton = findViewById(R.id.select_tts_language_button);
        ttsLanguageText = findViewById(R.id.tts_language_text);
        ttsReadOriginalSwitch = findViewById(R.id.tts_read_original_switch);
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
        
        // Set up theme radio group listener
        themeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // If custom theme is selected, open color wheel activity
                if (checkedId == R.id.radio_custom) {
                    openColorWheelActivity();
                }
            }
        });
        
        // Set up TTS language selection button
        selectTTSLanguageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTTSLanguageSelectionDialog();
            }
        });
        
        // Set up TTS speed SeekBar
        ttsSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updateTTSSpeedLabel(progress);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not needed
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not needed
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
        
        // Load TTS settings
        ttsEnabledSwitch.setChecked(userPreferences.isTTSEnabled());
        
        // Load TTS speed
        float ttsSpeed = userPreferences.getTTSSpeechRate();
        int speedProgress = (int) ((ttsSpeed - 0.5f) / 1.5f * 150); // Convert 0.5-2.0 to 0-150
        ttsSpeedSeekBar.setProgress(speedProgress);
        updateTTSSpeedLabel(speedProgress);
        
        // Load TTS language
        String ttsLanguage = userPreferences.getTTSLanguage();
        if (!TextUtils.isEmpty(ttsLanguage)) {
            ttsLanguageText.setText(getLanguageName(ttsLanguage));
            ttsLanguageText.setVisibility(View.VISIBLE);
        } else {
            ttsLanguageText.setVisibility(View.GONE);
        }
        
        // Load TTS read original setting
        ttsReadOriginalSwitch.setChecked(userPreferences.shouldTTSReadOriginal());
    }

    private void showLanguageSelectionDialog(final String selectionType) {
        // Define language options
        final String[] languageCodes = {
                "en", "es", "fr", "de", "it", "pt", "ru", "zh", "ja", "ko", "ar", "hi"
        };

        final String[] languageNames = {
                "English", "Spanish", "French", "German", "Italian", "Portuguese",
                "Russian", "Chinese", "Japanese", "Korean", "Arabic", "Hindi"
        };

        // Create dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Language");

        // Set items
        builder.setItems(languageNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String selectedCode = languageCodes[which];
                String selectedName = languageNames[which];

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
        
        // Save TTS settings
        userPreferences.setTTSEnabled(ttsEnabledSwitch.isChecked());
        
        // Save TTS speed (convert seekbar progress 0-150 to speed 0.5-2.0)
        int speedProgress = ttsSpeedSeekBar.getProgress();
        float ttsSpeed = 0.5f + (speedProgress / 150.0f) * 1.5f;
        userPreferences.setTTSSpeechRate(ttsSpeed);
        
        userPreferences.setTTSReadOriginal(ttsReadOriginalSwitch.isChecked());

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
    
    /**
     * Opens the color wheel activity for custom theme color selection.
     */
    private void openColorWheelActivity() {
        Intent intent = new Intent(this, ColorWheelActivity.class);
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
        if (userPreferences != null && userPreferences.isUsingCustomTheme()) {
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
    
    /**
     * Updates the TTS speed label based on seekbar progress.
     */
    private void updateTTSSpeedLabel(int progress) {
        float speed = 0.5f + (progress / 150.0f) * 1.5f;
        String speedText;
        
        if (speed <= 0.6f) {
            speedText = getString(R.string.tts_speed_slow);
        } else if (speed <= 1.2f) {
            speedText = getString(R.string.tts_speed_normal);
        } else if (speed <= 1.7f) {
            speedText = getString(R.string.tts_speed_fast);
        } else {
            speedText = getString(R.string.tts_speed_very_fast);
        }
        
        speedText += String.format(" (%.1fx)", speed);
        ttsSpeedLabel.setText(speedText);
    }
    
    /**
     * Shows the TTS language selection dialog.
     */
    private void showTTSLanguageSelectionDialog() {
        // List of commonly supported TTS languages
        final String[] languageCodes = {
            "en", "es", "fr", "de", "it", "pt", "ja", "ko", "zh", "ar", "hi", "ru"
        };
        
        final String[] languageNames = {
            "English", "Spanish", "French", "German", "Italian", "Portuguese",
            "Japanese", "Korean", "Chinese", "Arabic", "Hindi", "Russian"
        };
        
        // Find current selection
        String currentLanguage = userPreferences.getTTSLanguage();
        int currentSelection = -1;
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentLanguage)) {
                currentSelection = i;
                break;
            }
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.select_tts_language));
        builder.setSingleChoiceItems(languageNames, currentSelection, 
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String selectedLanguage = languageCodes[which];
                    userPreferences.setTTSLanguage(selectedLanguage);
                    
                    // Update UI
                    ttsLanguageText.setText(languageNames[which]);
                    ttsLanguageText.setVisibility(View.VISIBLE);
                    
                    dialog.dismiss();
                }
            });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
