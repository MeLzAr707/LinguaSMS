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
    private static final String KEY_OFFLINE_TRANSLATION_ENABLED = "offline_translation_enabled";
    private static final String KEY_PREFER_OFFLINE_TRANSLATION = "prefer_offline_translation";
    private static final String KEY_TRANSLATION_MODE = "translation_mode";
    private static final String KEY_THEME_ID = "theme_id";
    private static final String KEY_FIRST_RUN = "first_run";
    private static final String KEY_LAST_TRANSLATION_DATE = "last_translation_date";
    private static final String KEY_TRANSLATIONS_TODAY = "translations_today";
    private static final String KEY_MESSAGE_TEXT_SIZE = "message_text_size";

    // Theme constants
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_BLACK_GLASS = 2;
    public static final int THEME_SYSTEM = 3;
    public static final int THEME_CUSTOM = 4;
    
    // Translation mode constants
    public static final int TRANSLATION_MODE_ONLINE = 0;
    public static final int TRANSLATION_MODE_OFFLINE = 1;
    public static final int TRANSLATION_MODE_AUTO = 2; // Prefer offline, fallback to online
    
    // Custom theme color keys
    private static final String KEY_CUSTOM_PRIMARY_COLOR = "custom_primary_color";
    private static final String KEY_CUSTOM_NAV_BAR_COLOR = "custom_nav_bar_color";
    private static final String KEY_CUSTOM_TOP_BAR_COLOR = "custom_top_bar_color";
    private static final String KEY_CUSTOM_BUTTON_COLOR = "custom_button_color";
    private static final String KEY_CUSTOM_MENU_COLOR = "custom_menu_color";
    private static final String KEY_CUSTOM_INCOMING_BUBBLE_COLOR = "custom_incoming_bubble_color";
    private static final String KEY_CUSTOM_OUTGOING_BUBBLE_COLOR = "custom_outgoing_bubble_color";
    private static final String KEY_CUSTOM_BACKGROUND_COLOR = "custom_background_color";
    private static final String KEY_CUSTOM_TEXT_COLOR = "custom_text_color";
    private static final String KEY_CUSTOM_MESSAGE_VIEW_BACKGROUND_COLOR = "custom_message_view_background_color";
    private static final String KEY_CUSTOM_INCOMING_BUBBLE_TEXT_COLOR = "custom_incoming_bubble_text_color";
    private static final String KEY_CUSTOM_OUTGOING_BUBBLE_TEXT_COLOR = "custom_outgoing_bubble_text_color";

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
        return preferences.getString(KEY_PREFERRED_LANGUAGE, getDeviceLanguage());
    }
    
    /**
     * Gets the device's default language.
     * @return The device language code (e.g., "en", "es", "fr")
     */
    private String getDeviceLanguage() {
        return java.util.Locale.getDefault().getLanguage();
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

    /**
     * Gets the target language for translations.
     * This is an alias for getPreferredLanguage() to maintain compatibility.
     *
     * @return The target language code
     */
    public String getTargetLanguage() {
        return getPreferredLanguage();
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

        // For THEME_LIGHT and any other theme (including THEME_CUSTOM), 
        // always return false to ensure light theme override
        return false;
    }

    public boolean isUsingBlackGlassTheme() {
        return getThemeId() == THEME_BLACK_GLASS;
    }

    /**
     * Gets the current message text size.
     *
     * @return The message text size
     */
    public float getMessageTextSize() {
        return preferences.getFloat(KEY_MESSAGE_TEXT_SIZE, 14.0f);
    }

    /**
     * Sets the message text size.
     *
     * @param textSize The text size to set
     */
    public void setMessageTextSize(float textSize) {
        preferences.edit().putFloat(KEY_MESSAGE_TEXT_SIZE, textSize).apply();
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

    /**
     * Gets a string preference.
     *
     * @param key The preference key
     * @param defaultValue The default value
     * @return The preference value
     */
    public String getString(String key, String defaultValue) {
        return preferences.getString(key, defaultValue);
    }

    /**
     * Sets a string preference.
     *
     * @param key The preference key
     * @param value The preference value
     */
    public void setString(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }

    /**
     * Checks if the user is using a custom theme.
     *
     * @return true if using custom theme, false otherwise
     */
    public boolean isUsingCustomTheme() {
        return getThemeId() == THEME_CUSTOM;
    }

    /**
     * Gets the custom primary color.
     *
     * @param defaultColor The default color to return if not set
     * @return The custom primary color
     */
    public int getCustomPrimaryColor(int defaultColor) {
        return preferences.getInt(KEY_CUSTOM_PRIMARY_COLOR, defaultColor);
    }

    /**
     * Sets the custom primary color.
     *
     * @param color The color to set
     */
    public void setCustomPrimaryColor(int color) {
        preferences.edit().putInt(KEY_CUSTOM_PRIMARY_COLOR, color).apply();
    }

    /**
     * Gets the custom navigation bar color.
     *
     * @param defaultColor The default color to return if not set
     * @return The custom navigation bar color
     */
    public int getCustomNavBarColor(int defaultColor) {
        return preferences.getInt(KEY_CUSTOM_NAV_BAR_COLOR, defaultColor);
    }

    /**
     * Sets the custom navigation bar color.
     *
     * @param color The color to set
     */
    public void setCustomNavBarColor(int color) {
        preferences.edit().putInt(KEY_CUSTOM_NAV_BAR_COLOR, color).apply();
    }

    /**
     * Gets the custom top bar color.
     *
     * @param defaultColor The default color to return if not set
     * @return The custom top bar color
     */
    public int getCustomTopBarColor(int defaultColor) {
        return preferences.getInt(KEY_CUSTOM_TOP_BAR_COLOR, defaultColor);
    }

    /**
     * Sets the custom top bar color.
     *
     * @param color The color to set
     */
    public void setCustomTopBarColor(int color) {
        preferences.edit().putInt(KEY_CUSTOM_TOP_BAR_COLOR, color).apply();
    }

    /**
     * Gets the custom button color.
     *
     * @param defaultColor The default color to return if not set
     * @return The custom button color
     */
    public int getCustomButtonColor(int defaultColor) {
        return preferences.getInt(KEY_CUSTOM_BUTTON_COLOR, defaultColor);
    }

    /**
     * Sets the custom button color.
     *
     * @param color The color to set
     */
    public void setCustomButtonColor(int color) {
        preferences.edit().putInt(KEY_CUSTOM_BUTTON_COLOR, color).apply();
    }

    /**
     * Gets the custom menu color.
     *
     * @param defaultColor The default color to return if not set
     * @return The custom menu color
     */
    public int getCustomMenuColor(int defaultColor) {
        return preferences.getInt(KEY_CUSTOM_MENU_COLOR, defaultColor);
    }

    /**
     * Sets the custom menu color.
     *
     * @param color The color to set
     */
    public void setCustomMenuColor(int color) {
        preferences.edit().putInt(KEY_CUSTOM_MENU_COLOR, color).apply();
    }

    /**
     * Gets the custom incoming bubble color.
     *
     * @param defaultColor The default color to return if not set
     * @return The custom incoming bubble color
     */
    public int getCustomIncomingBubbleColor(int defaultColor) {
        return preferences.getInt(KEY_CUSTOM_INCOMING_BUBBLE_COLOR, defaultColor);
    }

    /**
     * Sets the custom incoming bubble color.
     *
     * @param color The color to set
     */
    public void setCustomIncomingBubbleColor(int color) {
        preferences.edit().putInt(KEY_CUSTOM_INCOMING_BUBBLE_COLOR, color).apply();
    }

    /**
     * Gets the custom outgoing bubble color.
     *
     * @param defaultColor The default color to return if not set
     * @return The custom outgoing bubble color
     */
    public int getCustomOutgoingBubbleColor(int defaultColor) {
        return preferences.getInt(KEY_CUSTOM_OUTGOING_BUBBLE_COLOR, defaultColor);
    }

    /**
     * Sets the custom outgoing bubble color.
     *
     * @param color The color to set
     */
    public void setCustomOutgoingBubbleColor(int color) {
        preferences.edit().putInt(KEY_CUSTOM_OUTGOING_BUBBLE_COLOR, color).apply();
    }

    /**
     * Gets the custom background color.
     *
     * @param defaultColor The default color to return if not set
     * @return The custom background color
     */
    public int getCustomBackgroundColor(int defaultColor) {
        return preferences.getInt(KEY_CUSTOM_BACKGROUND_COLOR, defaultColor);
    }

    /**
     * Sets the custom background color.
     *
     * @param color The color to set
     */
    public void setCustomBackgroundColor(int color) {
        preferences.edit().putInt(KEY_CUSTOM_BACKGROUND_COLOR, color).apply();
    }

    /**
     * Gets the custom text color.
     *
     * @param defaultColor The default color to return if not set
     * @return The custom text color
     */
    public int getCustomTextColor(int defaultColor) {
        return preferences.getInt(KEY_CUSTOM_TEXT_COLOR, defaultColor);
    }

    /**
     * Sets the custom text color.
     *
     * @param color The color to set
     */
    public void setCustomTextColor(int color) {
        preferences.edit().putInt(KEY_CUSTOM_TEXT_COLOR, color).apply();
    }

    // Offline Translation Settings

    /**
     * Checks if offline translation is enabled.
     *
     * @return true if offline translation is enabled
     */
    public boolean isOfflineTranslationEnabled() {
        return preferences.getBoolean(KEY_OFFLINE_TRANSLATION_ENABLED, true); // Default to enabled
    }

    /**
     * Sets offline translation enabled state.
     *
     * @param enabled true to enable offline translation
     */
    public void setOfflineTranslationEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_OFFLINE_TRANSLATION_ENABLED, enabled).apply();
    }

    /**
     * Checks if offline translation is preferred over online.
     *
     * @return true if offline translation should be preferred
     */
    public boolean getPreferOfflineTranslation() {
        return preferences.getBoolean(KEY_PREFER_OFFLINE_TRANSLATION, true); // Default to prefer offline
    }

    /**
     * Sets offline translation preference.
     *
     * @param prefer true to prefer offline translation
     */
    public void setPreferOfflineTranslation(boolean prefer) {
        preferences.edit().putBoolean(KEY_PREFER_OFFLINE_TRANSLATION, prefer).apply();
    }

    /**
     * Gets the translation mode.
     *
     * @return The translation mode (TRANSLATION_MODE_AUTO by default)
     */
    public int getTranslationMode() {
        return preferences.getInt(KEY_TRANSLATION_MODE, TRANSLATION_MODE_AUTO);
    }

    /**
     * Sets the translation mode.
     *
     * @param mode The translation mode to set
     */
    public void setTranslationMode(int mode) {
        preferences.edit().putInt(KEY_TRANSLATION_MODE, mode).apply();
    }

    /**
     * Gets the custom message view background color.
     *
     * @param defaultColor The default color to return if not set
     * @return The custom message view background color
     */
    public int getCustomMessageViewBackgroundColor(int defaultColor) {
        return preferences.getInt(KEY_CUSTOM_MESSAGE_VIEW_BACKGROUND_COLOR, defaultColor);
    }

    /**
     * Sets the custom message view background color.
     *
     * @param color The color to set
     */
    public void setCustomMessageViewBackgroundColor(int color) {
        preferences.edit().putInt(KEY_CUSTOM_MESSAGE_VIEW_BACKGROUND_COLOR, color).apply();
    }

    /**
     * Gets the custom incoming bubble text color.
     *
     * @param defaultColor The default color to return if not set
     * @return The custom incoming bubble text color
     */
    public int getCustomIncomingBubbleTextColor(int defaultColor) {
        return preferences.getInt(KEY_CUSTOM_INCOMING_BUBBLE_TEXT_COLOR, defaultColor);
    }

    /**
     * Sets the custom incoming bubble text color.
     *
     * @param color The color to set
     */
    public void setCustomIncomingBubbleTextColor(int color) {
        preferences.edit().putInt(KEY_CUSTOM_INCOMING_BUBBLE_TEXT_COLOR, color).apply();
    }

    /**
     * Gets the custom outgoing bubble text color.
     *
     * @param defaultColor The default color to return if not set
     * @return The custom outgoing bubble text color
     */
    public int getCustomOutgoingBubbleTextColor(int defaultColor) {
        return preferences.getInt(KEY_CUSTOM_OUTGOING_BUBBLE_TEXT_COLOR, defaultColor);
    }

    /**
     * Sets the custom outgoing bubble text color.
     *
     * @param color The color to set
     */
    public void setCustomOutgoingBubbleTextColor(int color) {
        preferences.edit().putInt(KEY_CUSTOM_OUTGOING_BUBBLE_TEXT_COLOR, color).apply();
    }
}







