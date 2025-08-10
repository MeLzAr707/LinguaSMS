package com.translator.messagingapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

public class UserPreferences {
    private static final String PREFS_NAME = "translator_prefs";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_PREFERRED_LANGUAGE = "preferred_language";
    private static final String KEY_PREFERRED_INCOMING_LANGUAGE = "preferred_incoming_language";
    private static final String KEY_PREFERRED_OUTGOING_LANGUAGE = "preferred_outgoing_language";
    private static final String KEY_AUTO_TRANSLATE = "auto_translate";
    private static final String KEY_THEME_ID = "theme_id";
    private static final String KEY_FIRST_RUN = "first_run";
    private static final String KEY_LAST_TRANSLATION_DATE = "last_translation_date";
    private static final String KEY_TRANSLATIONS_TODAY = "translations_today";

    // Theme constants
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_BLACK_GLASS = 2;
    public static final int THEME_SYSTEM = 3;

    private final SharedPreferences preferences;

    public UserPreferences(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getApiKey() {
        return preferences.getString(KEY_API_KEY, "");
    }

    public void setApiKey(String apiKey) {
        preferences.edit().putString(KEY_API_KEY, apiKey).apply();
    }

    public String getPreferredLanguage() {
        return preferences.getString(KEY_PREFERRED_LANGUAGE, "en");
    }

    public void setPreferredLanguage(String language) {
        preferences.edit().putString(KEY_PREFERRED_LANGUAGE, language).apply();
    }

    public String getPreferredIncomingLanguage() {
        return preferences.getString(KEY_PREFERRED_INCOMING_LANGUAGE, getPreferredLanguage());
    }

    public void setPreferredIncomingLanguage(String language) {
        preferences.edit().putString(KEY_PREFERRED_INCOMING_LANGUAGE, language).apply();
    }

    public String getPreferredOutgoingLanguage() {
        return preferences.getString(KEY_PREFERRED_OUTGOING_LANGUAGE, getPreferredLanguage());
    }

    public void setPreferredOutgoingLanguage(String language) {
        preferences.edit().putString(KEY_PREFERRED_OUTGOING_LANGUAGE, language).apply();
    }

    public boolean isAutoTranslateEnabled() {
        return preferences.getBoolean(KEY_AUTO_TRANSLATE, false);
    }

    /**
     * Alias for isAutoTranslateEnabled() to maintain compatibility with existing code
     */
    public boolean getAutoTranslateSms() {
        return isAutoTranslateEnabled();
    }

    public void setAutoTranslateEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_AUTO_TRANSLATE, enabled).apply();
    }

    public int getThemeId() {
        return preferences.getInt(KEY_THEME_ID, THEME_LIGHT);
    }

    public void setThemeId(int themeId) {
        preferences.edit().putInt(KEY_THEME_ID, themeId).apply();
    }

    public boolean isDarkThemeActive(Context context) {
        int themeId = getThemeId();

        if (themeId == THEME_DARK || themeId == THEME_BLACK_GLASS) {
            return true;
        } else if (themeId == THEME_SYSTEM) {
            // Check system night mode
            int nightModeFlags = context.getResources().getConfiguration().uiMode &
                    Configuration.UI_MODE_NIGHT_MASK;
            return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        }

        return false;
    }

    public boolean isUsingBlackGlassTheme() {
        return getThemeId() == THEME_BLACK_GLASS;
    }

    /**
     * Checks if this is the first run of the app.
     *
     * @return true if this is the first run, false otherwise
     */
    public boolean isFirstRun() {
        return preferences.getBoolean(KEY_FIRST_RUN, true);
    }

    /**
     * Sets the first run flag.
     *
     * @param isFirstRun true if this is the first run, false otherwise
     */
    public void setFirstRun(boolean isFirstRun) {
        preferences.edit().putBoolean(KEY_FIRST_RUN, isFirstRun).apply();
    }

    /**
     * Gets the last translation date.
     *
     * @return The timestamp of the last translation
     */
    public long getLastTranslationDate() {
        return preferences.getLong(KEY_LAST_TRANSLATION_DATE, 0);
    }

    /**
     * Sets the last translation date.
     *
     * @param timestamp The timestamp to set
     */
    public void setLastTranslationDate(long timestamp) {
        preferences.edit().putLong(KEY_LAST_TRANSLATION_DATE, timestamp).apply();
    }

    /**
     * Gets the number of translations performed today.
     *
     * @return The number of translations today
     */
    public int getTranslationsToday() {
        return preferences.getInt(KEY_TRANSLATIONS_TODAY, 0);
    }

    /**
     * Sets the number of translations performed today.
     *
     * @param count The count to set
     */
    public void setTranslationsToday(int count) {
        preferences.edit().putInt(KEY_TRANSLATIONS_TODAY, count).apply();
    }

    /**
     * Increments the number of translations performed today.
     */
    public void incrementTranslationsToday() {
        int current = getTranslationsToday();
        setTranslationsToday(current + 1);
    }

    /**
     * Gets a boolean preference.
     *
     * @param key The preference key
     * @param defaultValue The default value
     * @return The preference value
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }

    /**
     * Sets a boolean preference.
     *
     * @param key The preference key
     * @param value The preference value
     */
    public void setBoolean(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
    }

    /**
     * Checks if dark theme is enabled.
     *
     * @return true if dark theme is enabled, false otherwise
     */
    public boolean isDarkThemeEnabled() {
        int themeId = getThemeId();
        return themeId == THEME_DARK || themeId == THEME_BLACK_GLASS;
    }
}






