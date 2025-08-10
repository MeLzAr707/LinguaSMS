package com.translator.messagingapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

/**
 * Activity for app settings.
 * CORRECTED VERSION: Updates preference titles and removes API service
 */
public class SettingsActivity extends BaseActivity {
    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_settings);
            
            // Add logging
            Log.d(TAG, "onCreate called");
            
            // Setup toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(R.string.settings);
            }
            
            // If using PreferenceFragment, make sure it's added correctly
            if (savedInstanceState == null) {
                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
                
                Log.d(TAG, "SettingsFragment added to container");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            // Show a simple layout with error message if the preferences setup fails
            setContentView(R.layout.error_layout);
            Toast.makeText(this, R.string.settings_error_message, Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Fragment for displaying preferences.
     */
    public static class SettingsFragment extends PreferenceFragmentCompat {
        private static final String TAG = "SettingsFragment";
        
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            try {
                Log.d(TAG, "onCreatePreferences called");
                setPreferencesFromResource(R.xml.preferences, rootKey);
                
                // Get user preferences
                UserPreferences userPreferences = ((TranslatorApp) requireActivity().getApplication()).getUserPreferences();
                
                // Setup auto-translate preference
                setupAutoTranslatePreference(userPreferences);
                
                // Setup preferred incoming language preference
                setupPreferredLanguagePreference(userPreferences);
                
                // Setup preferred outgoing language preference
                setupPreferredOutgoingLanguagePreference(userPreferences);
                
                // Setup theme preference
                setupThemePreference(userPreferences);
                
                // Setup API key preference
                setupApiKeyPreference(userPreferences);
                
                // Setup debug mode preference
                setupDebugModePreference(userPreferences);
                
                // Setup translation mode preference
                setupTranslationModePreference(userPreferences);
                
                // Setup prefer offline preference
                setupPreferOfflinePreference(userPreferences);
                
                // Setup manage offline models preference
                setupManageOfflineModelsPreference();
                
                Log.d(TAG, "Preferences setup complete");
            } catch (Exception e) {
                Log.e(TAG, "Error setting up preferences", e);
                Toast.makeText(getContext(), R.string.settings_error_message, Toast.LENGTH_LONG).show();
            }
        }
        
        private void setupAutoTranslatePreference(UserPreferences userPreferences) {
            SwitchPreferenceCompat autoTranslatePreference = findPreference("auto_translate");
            if (autoTranslatePreference != null) {
                try {
                    autoTranslatePreference.setChecked(userPreferences.isAutoTranslateEnabled());
                    autoTranslatePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        try {
                            boolean enabled = (Boolean) newValue;
                            userPreferences.setAutoTranslateEnabled(enabled);
                            return true;
                        } catch (Exception e) {
                            Log.e(TAG, "Error setting auto translate preference", e);
                            return false;
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing auto translate preference", e);
                }
            }
        }
        
        private void setupPreferredLanguagePreference(UserPreferences userPreferences) {
            ListPreference preferredLanguagePreference = findPreference("preferred_language");
            if (preferredLanguagePreference != null) {
                try {
                    String currentLanguage = userPreferences.getPreferredLanguage();
                    preferredLanguagePreference.setValue(currentLanguage);
                    preferredLanguagePreference.setSummary(getLanguageName(currentLanguage));
                    preferredLanguagePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        try {
                            String language = (String) newValue;
                            userPreferences.setPreferredLanguage(language);
                            preference.setSummary(getLanguageName(language));
                            return true;
                        } catch (Exception e) {
                            Log.e(TAG, "Error setting preferred language", e);
                            return false;
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing preferred language preference", e);
                }
            }
        }
        
        private void setupPreferredOutgoingLanguagePreference(UserPreferences userPreferences) {
            ListPreference preferredOutgoingLanguagePreference = findPreference("preferred_outgoing_language");
            if (preferredOutgoingLanguagePreference != null) {
                try {
                    // Get current outgoing language or default to English if not set
                    String currentLanguage = userPreferences.getPreferredOutgoingLanguage();
                    if (TextUtils.isEmpty(currentLanguage)) {
                        currentLanguage = "en";
                    }
                    
                    preferredOutgoingLanguagePreference.setValue(currentLanguage);
                    preferredOutgoingLanguagePreference.setSummary(getLanguageName(currentLanguage));
                    preferredOutgoingLanguagePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        try {
                            String language = (String) newValue;
                            userPreferences.setPreferredOutgoingLanguage(language);
                            preference.setSummary(getLanguageName(language));
                            return true;
                        } catch (Exception e) {
                            Log.e(TAG, "Error setting preferred outgoing language", e);
                            return false;
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing preferred outgoing language preference", e);
                }
            }
        }
        
        private void setupThemePreference(UserPreferences userPreferences) {
            ListPreference themePreference = findPreference("theme");
            if (themePreference != null) {
                try {
                    themePreference.setValue(String.valueOf(userPreferences.getThemeId()));
                    themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        try {
                            int themeId = Integer.parseInt((String) newValue);
                            userPreferences.setThemeId(themeId);
                            
                            // Refresh the current activity and then recreate to apply new theme
                            requireActivity().runOnUiThread(() -> {
                                try {
                                    // First recreate the settings activity
                                    requireActivity().recreate();
                                    
                                    // Also request refresh of main activity if it exists
                                    if (requireActivity().getParent() != null) {
                                        requireActivity().getParent().recreate();
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error recreating activities for theme change", e);
                                }
                            });
                            
                            return true;
                        } catch (Exception e) {
                            Log.e(TAG, "Error setting theme", e);
                            return false;
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing theme preference", e);
                }
            }
        }
        
        private void setupApiKeyPreference(UserPreferences userPreferences) {
            EditTextPreference apiKeyPreference = findPreference("api_key");
            if (apiKeyPreference != null) {
                try {
                    String currentApiKey = userPreferences.getApiKey();
                    if (!TextUtils.isEmpty(currentApiKey)) {
                        apiKeyPreference.setSummary(R.string.api_key_set);
                    }
                    
                    apiKeyPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        try {
                            String apiKey = (String) newValue;
                            userPreferences.setApiKey(apiKey);
                            
                            if (!TextUtils.isEmpty(apiKey)) {
                                preference.setSummary(R.string.api_key_set);
                            } else {
                                preference.setSummary(R.string.pref_api_key_summary);
                            }
                            return true;
                        } catch (Exception e) {
                            Log.e(TAG, "Error setting API key", e);
                            return false;
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing API key preference", e);
                }
            }
        }
        
        private void setupDebugModePreference(UserPreferences userPreferences) {
            SwitchPreferenceCompat debugModePreference = findPreference("debug_mode");
            if (debugModePreference != null) {
                try {
                    debugModePreference.setChecked(userPreferences.isDebugModeEnabled());
                    debugModePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        try {
                            boolean enabled = (Boolean) newValue;
                            userPreferences.setDebugModeEnabled(enabled);
                            return true;
                        } catch (Exception e) {
                            Log.e(TAG, "Error setting debug mode", e);
                            return false;
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing debug mode preference", e);
                }
            }
        }
        
        private void setupTranslationModePreference(UserPreferences userPreferences) {
            ListPreference translationModePreference = findPreference("translation_mode");
            if (translationModePreference != null) {
                try {
                    String currentMode = String.valueOf(userPreferences.getTranslationMode());
                    translationModePreference.setValue(currentMode);
                    translationModePreference.setSummary(getTranslationModeName(userPreferences.getTranslationMode()));
                    translationModePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        try {
                            int mode = Integer.parseInt((String) newValue);
                            userPreferences.setTranslationMode(mode);
                            preference.setSummary(getTranslationModeName(mode));
                            return true;
                        } catch (Exception e) {
                            Log.e(TAG, "Error setting translation mode", e);
                            return false;
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing translation mode preference", e);
                }
            }
        }
        
        private void setupPreferOfflinePreference(UserPreferences userPreferences) {
            SwitchPreferenceCompat preferOfflinePreference = findPreference("prefer_offline");
            if (preferOfflinePreference != null) {
                try {
                    preferOfflinePreference.setChecked(userPreferences.getPreferOfflineTranslation());
                    preferOfflinePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        try {
                            boolean prefer = (Boolean) newValue;
                            userPreferences.setPreferOfflineTranslation(prefer);
                            return true;
                        } catch (Exception e) {
                            Log.e(TAG, "Error setting prefer offline preference", e);
                            return false;
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing prefer offline preference", e);
                }
            }
        }
        
        private void setupManageOfflineModelsPreference() {
            Preference manageModelsPreference = findPreference("manage_offline_models");
            if (manageModelsPreference != null) {
                try {
                    manageModelsPreference.setOnPreferenceClickListener(preference -> {
                        try {
                            // Open offline models management activity
                            android.content.Intent intent = new android.content.Intent(getContext(), OfflineModelsActivity.class);
                            startActivity(intent);
                            return true;
                        } catch (Exception e) {
                            Log.e(TAG, "Error opening offline models manager", e);
                            Toast.makeText(getContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing manage offline models preference", e);
                }
            }
        }
        
        /**
         * Helper method to get language name from code
         */
        private String getLanguageName(String languageCode) {
            if (TextUtils.isEmpty(languageCode)) {
                return "English"; // Default
            }
            
            String[] languageCodes = getResources().getStringArray(R.array.language_codes);
            String[] languageNames = getResources().getStringArray(R.array.language_names);
            
            for (int i = 0; i < languageCodes.length; i++) {
                if (languageCodes[i].equals(languageCode)) {
                    return languageNames[i];
                }
            }
            
            return "English"; // Default if not found
        }
        
        /**
         * Helper method to get translation mode name from mode value
         */
        private String getTranslationModeName(int mode) {
            String[] modeNames = getResources().getStringArray(R.array.translation_mode_names);
            if (mode >= 0 && mode < modeNames.length) {
                return modeNames[mode];
            }
            return modeNames[2]; // Default to Auto mode
        }
    }
}