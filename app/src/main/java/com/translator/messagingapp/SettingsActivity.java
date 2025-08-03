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
                            requireActivity().recreate();
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
    }
}