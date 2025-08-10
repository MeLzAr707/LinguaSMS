package com.translator.messagingapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.util.Log;

/**
 * Manages user preferences for the app.
 * COMPLETE VERSION: Includes all required methods and constants
 */
public class UserPreferences {
    private static final String TAG = "UserPreferences";
    private static final String PREFS_NAME = "translator_prefs";
    
    // Theme constants
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_BLACK_GLASS = 2;
    public static final int THEME_SYSTEM = 3;
    
    // Preference keys
    private static final String KEY_AUTO_TRANSLATE = "auto_translate";
    private static final String KEY_PREFERRED_LANGUAGE = "preferred_language";
    private static final String KEY_PREFERRED_OUTGOING_LANGUAGE = "preferred_outgoing_language";
    private static final String KEY_THEME_ID = "theme";
    private static final String KEY_DEBUG_MODE = "debug_mode";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_API_SERVICE = "api_service";
    private static final String KEY_TRANSLATION_MODE = "translation_mode";
    private static final String KEY_PREFER_OFFLINE = "prefer_offline";
    
    // Translation mode constants
    public static final int TRANSLATION_MODE_ONLINE_ONLY = 0;
    public static final int TRANSLATION_MODE_OFFLINE_ONLY = 1;
    public static final int TRANSLATION_MODE_AUTO = 2; // Try offline first, fallback to online
    
    // Default values
    private static final boolean DEFAULT_AUTO_TRANSLATE = false;
    private static final String DEFAULT_PREFERRED_LANGUAGE = "en";
    private static final String DEFAULT_PREFERRED_OUTGOING_LANGUAGE = "en";
    private static final int DEFAULT_THEME_ID = THEME_SYSTEM; // System default
    private static final boolean DEFAULT_DEBUG_MODE = false;
    private static final String DEFAULT_API_SERVICE = "google";
    private static final int DEFAULT_TRANSLATION_MODE = TRANSLATION_MODE_AUTO;
    private static final boolean DEFAULT_PREFER_OFFLINE = true;
    
    private final SharedPreferences preferences;
    private final Context context;
    
    /**
     * Constructor.
     *
     * @param context The application context
     */
    public UserPreferences(Context context) {
        this.context = context.getApplicationContext();
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Checks if auto-translate is enabled.
     *
     * @return true if auto-translate is enabled, false otherwise
     */
    public boolean isAutoTranslateEnabled() {
        try {
            return preferences.getBoolean(KEY_AUTO_TRANSLATE, DEFAULT_AUTO_TRANSLATE);
        } catch (Exception e) {
            Log.e(TAG, "Error getting auto-translate preference", e);
            return DEFAULT_AUTO_TRANSLATE;
        }
    }
    
    /**
     * Sets the auto-translate preference.
     *
     * @param enabled true to enable auto-translate, false to disable
     */
    public void setAutoTranslateEnabled(boolean enabled) {
        try {
            preferences.edit().putBoolean(KEY_AUTO_TRANSLATE, enabled).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error setting auto-translate preference", e);
        }
    }
    
    /**
     * Gets the preferred language for incoming translations.
     * This is an alias for getPreferredLanguage() for backward compatibility.
     *
     * @return the preferred language code (e.g., "en" for English)
     */
    public String getPreferredIncomingLanguage() {
        return getPreferredLanguage();
    }
    
    /**
     * Gets the preferred language for translations.
     *
     * @return the preferred language code (e.g., "en" for English)
     */
    public String getPreferredLanguage() {
        try {
            return preferences.getString(KEY_PREFERRED_LANGUAGE, DEFAULT_PREFERRED_LANGUAGE);
        } catch (Exception e) {
            Log.e(TAG, "Error getting preferred language", e);
            return DEFAULT_PREFERRED_LANGUAGE;
        }
    }
    
    /**
     * Sets the preferred language for translations.
     *
     * @param languageCode the language code (e.g., "en" for English)
     */
    public void setPreferredLanguage(String languageCode) {
        try {
            preferences.edit().putString(KEY_PREFERRED_LANGUAGE, languageCode).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error setting preferred language", e);
        }
    }
    
    /**
     * Gets the preferred language for outgoing messages.
     *
     * @return the preferred outgoing language code (e.g., "en" for English)
     */
    public String getPreferredOutgoingLanguage() {
        try {
            return preferences.getString(KEY_PREFERRED_OUTGOING_LANGUAGE, DEFAULT_PREFERRED_OUTGOING_LANGUAGE);
        } catch (Exception e) {
            Log.e(TAG, "Error getting preferred outgoing language", e);
            return DEFAULT_PREFERRED_OUTGOING_LANGUAGE;
        }
    }
    
    /**
     * Sets the preferred language for outgoing messages.
     *
     * @param languageCode the language code (e.g., "en" for English)
     */
    public void setPreferredOutgoingLanguage(String languageCode) {
        try {
            preferences.edit().putString(KEY_PREFERRED_OUTGOING_LANGUAGE, languageCode).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error setting preferred outgoing language", e);
        }
    }
    
    /**
     * Gets the theme ID.
     *
     * @return the theme ID
     */
    public int getThemeId() {
        try {
            return preferences.getInt(KEY_THEME_ID, DEFAULT_THEME_ID);
        } catch (Exception e) {
            Log.e(TAG, "Error getting theme ID", e);
            return DEFAULT_THEME_ID;
        }
    }
    
    /**
     * Sets the theme ID.
     *
     * @param themeId the theme ID
     */
    public void setThemeId(int themeId) {
        try {
            preferences.edit().putInt(KEY_THEME_ID, themeId).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error setting theme ID", e);
        }
    }
    
    /**
     * Checks if the system theme is currently in dark mode.
     *
     * @param context The context
     * @return true if the system is in dark mode, false otherwise
     */
    public boolean isDarkThemeActive(Context context) {
        try {
            int nightModeFlags = context.getResources().getConfiguration().uiMode & 
                    Configuration.UI_MODE_NIGHT_MASK;
            return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        } catch (Exception e) {
            Log.e(TAG, "Error checking dark theme status", e);
            return false;
        }
    }
    
    /**
     * Checks if debug mode is enabled.
     *
     * @return true if debug mode is enabled, false otherwise
     */
    public boolean isDebugModeEnabled() {
        try {
            return preferences.getBoolean(KEY_DEBUG_MODE, DEFAULT_DEBUG_MODE);
        } catch (Exception e) {
            Log.e(TAG, "Error getting debug mode preference", e);
            return DEFAULT_DEBUG_MODE;
        }
    }
    
    /**
     * Sets the debug mode preference.
     *
     * @param enabled true to enable debug mode, false to disable
     */
    public void setDebugModeEnabled(boolean enabled) {
        try {
            preferences.edit().putBoolean(KEY_DEBUG_MODE, enabled).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error setting debug mode preference", e);
        }
    }
    
    /**
     * Gets the API key for translation services.
     *
     * @return the API key
     */
    public String getApiKey() {
        try {
            return preferences.getString(KEY_API_KEY, "");
        } catch (Exception e) {
            Log.e(TAG, "Error getting API key", e);
            return "";
        }
    }
    
    /**
     * Sets the API key for translation services.
     *
     * @param apiKey the API key
     */
    public void setApiKey(String apiKey) {
        try {
            preferences.edit().putString(KEY_API_KEY, apiKey).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error setting API key", e);
        }
    }
    
    /**
     * Gets the API service for translations.
     *
     * @return the API service name (e.g., "google", "microsoft")
     */
    public String getApiService() {
        try {
            return preferences.getString(KEY_API_SERVICE, DEFAULT_API_SERVICE);
        } catch (Exception e) {
            Log.e(TAG, "Error getting API service", e);
            return DEFAULT_API_SERVICE;
        }
    }
    
    /**
     * Sets the API service for translations.
     *
     * @param service the API service name (e.g., "google", "microsoft")
     */
    public void setApiService(String service) {
        try {
            preferences.edit().putString(KEY_API_SERVICE, service).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error setting API service", e);
        }
    }
    
    /**
     * Generic method to get a boolean preference.
     *
     * @param key The preference key
     * @param defaultValue The default value if the preference doesn't exist
     * @return The boolean value
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            return preferences.getBoolean(key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "Error getting boolean preference: " + key, e);
            return defaultValue;
        }
    }
    
    /**
     * Generic method to set a boolean preference.
     *
     * @param key The preference key
     * @param value The value to set
     */
    public void setBoolean(String key, boolean value) {
        try {
            preferences.edit().putBoolean(key, value).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error setting boolean preference: " + key, e);
        }
    }
    
    /**
     * Generic method to get a string preference.
     *
     * @param key The preference key
     * @param defaultValue The default value if the preference doesn't exist
     * @return The string value
     */
    public String getString(String key, String defaultValue) {
        try {
            return preferences.getString(key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "Error getting string preference: " + key, e);
            return defaultValue;
        }
    }
    
    /**
     * Generic method to set a string preference.
     *
     * @param key The preference key
     * @param value The value to set
     */
    public void setString(String key, String value) {
        try {
            preferences.edit().putString(key, value).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error setting string preference: " + key, e);
        }
    }
    
    /**
     * Generic method to get an integer preference.
     *
     * @param key The preference key
     * @param defaultValue The default value if the preference doesn't exist
     * @return The integer value
     */
    public int getInt(String key, int defaultValue) {
        try {
            return preferences.getInt(key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "Error getting int preference: " + key, e);
            return defaultValue;
        }
    }
    
    /**
     * Generic method to set an integer preference.
     *
     * @param key The preference key
     * @param value The value to set
     */
    public void setInt(String key, int value) {
        try {
            preferences.edit().putInt(key, value).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error setting int preference: " + key, e);
        }
    }
    
    /**
     * Gets the translation mode preference.
     *
     * @return the translation mode (TRANSLATION_MODE_ONLINE_ONLY, TRANSLATION_MODE_OFFLINE_ONLY, or TRANSLATION_MODE_AUTO)
     */
    public int getTranslationMode() {
        try {
            return preferences.getInt(KEY_TRANSLATION_MODE, DEFAULT_TRANSLATION_MODE);
        } catch (Exception e) {
            Log.e(TAG, "Error getting translation mode", e);
            return DEFAULT_TRANSLATION_MODE;
        }
    }
    
    /**
     * Sets the translation mode preference.
     *
     * @param mode the translation mode (TRANSLATION_MODE_ONLINE_ONLY, TRANSLATION_MODE_OFFLINE_ONLY, or TRANSLATION_MODE_AUTO)
     */
    public void setTranslationMode(int mode) {
        try {
            preferences.edit().putInt(KEY_TRANSLATION_MODE, mode).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error setting translation mode", e);
        }
    }
    
    /**
     * Gets whether to prefer offline translation when both are available.
     *
     * @return true if offline translation is preferred, false otherwise
     */
    public boolean getPreferOfflineTranslation() {
        try {
            return preferences.getBoolean(KEY_PREFER_OFFLINE, DEFAULT_PREFER_OFFLINE);
        } catch (Exception e) {
            Log.e(TAG, "Error getting prefer offline preference", e);
            return DEFAULT_PREFER_OFFLINE;
        }
    }
    
    /**
     * Sets whether to prefer offline translation when both are available.
     *
     * @param prefer true to prefer offline translation, false otherwise
     */
    public void setPreferOfflineTranslation(boolean prefer) {
        try {
            preferences.edit().putBoolean(KEY_PREFER_OFFLINE, prefer).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error setting prefer offline preference", e);
        }
    }
    
    /**
     * Clears all preferences.
     */
    public void clearAll() {
        try {
            preferences.edit().clear().apply();
        } catch (Exception e) {
            Log.e(TAG, "Error clearing preferences", e);
        }
    }
}