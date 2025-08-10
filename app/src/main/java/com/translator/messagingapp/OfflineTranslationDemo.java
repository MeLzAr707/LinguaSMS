package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;

/**
 * Utility class for demonstrating offline translation capabilities.
 */
public class OfflineTranslationDemo {
    private static final String TAG = "OfflineTranslationDemo";

    /**
     * Demonstrates offline translation functionality.
     * 
     * @param context Application context
     * @param userPreferences User preferences
     * @return A status message about offline translation capabilities
     */
    public static String demonstrateOfflineTranslation(Context context, UserPreferences userPreferences) {
        try {
            // Initialize offline translation service
            OfflineTranslationService offlineService = new OfflineTranslationService(context, userPreferences);
            
            StringBuilder status = new StringBuilder();
            status.append("Offline Translation Status:\n\n");
            
            // Check translation mode
            int translationMode = userPreferences.getTranslationMode();
            String modeName;
            switch (translationMode) {
                case UserPreferences.TRANSLATION_MODE_ONLINE_ONLY:
                    modeName = "Online Only";
                    break;
                case UserPreferences.TRANSLATION_MODE_OFFLINE_ONLY:
                    modeName = "Offline Only";
                    break;
                case UserPreferences.TRANSLATION_MODE_AUTO:
                default:
                    modeName = "Auto (Smart Fallback)";
                    break;
            }
            status.append("Translation Mode: ").append(modeName).append("\n");
            status.append("Prefer Offline: ").append(userPreferences.getPreferOfflineTranslation() ? "Yes" : "No").append("\n\n");
            
            // Check downloaded models
            int downloadedCount = offlineService.getDownloadedModels().size();
            status.append("Downloaded Language Models: ").append(downloadedCount).append("\n");
            
            // Check specific language availability
            String preferredLang = userPreferences.getPreferredLanguage();
            boolean hasPreferredModel = offlineService.isLanguageModelDownloaded(preferredLang);
            status.append("Preferred Language (").append(preferredLang).append(") Model: ");
            status.append(hasPreferredModel ? "Downloaded" : "Not Downloaded").append("\n\n");
            
            // Test offline availability for common language pairs
            status.append("Offline Translation Availability:\n");
            String[] testLanguages = {"en", "es", "fr", "de", "zh"};
            for (String lang : testLanguages) {
                boolean available = offlineService.isOfflineTranslationAvailable("en", lang);
                status.append("English to ").append(lang.toUpperCase()).append(": ");
                status.append(available ? "Available" : "Unavailable").append("\n");
            }
            
            status.append("\nSupported Languages: ").append(offlineService.getSupportedLanguages().length);
            
            return status.toString();
            
        } catch (Exception e) {
            Log.e(TAG, "Error demonstrating offline translation", e);
            return "Error checking offline translation status: " + e.getMessage();
        }
    }

    /**
     * Tests offline translation with a sample text.
     * 
     * @param context Application context
     * @param userPreferences User preferences
     * @param callback Callback to receive the result
     */
    public static void testOfflineTranslation(Context context, UserPreferences userPreferences, 
                                            OfflineTranslationService.OfflineTranslationCallback callback) {
        try {
            OfflineTranslationService offlineService = new OfflineTranslationService(context, userPreferences);
            
            // Test translation if English and Spanish models are available
            if (offlineService.isOfflineTranslationAvailable("en", "es")) {
                offlineService.translateOffline("Hello, how are you?", "en", "es", callback);
            } else {
                if (callback != null) {
                    callback.onTranslationComplete(false, null, "English or Spanish language models not downloaded");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error testing offline translation", e);
            if (callback != null) {
                callback.onTranslationComplete(false, null, e.getMessage());
            }
        }
    }

    /**
     * Gets a quick status message for the current translation setup.
     * 
     * @param context Application context
     * @param userPreferences User preferences
     * @return Short status message
     */
    public static String getQuickStatus(Context context, UserPreferences userPreferences) {
        try {
            OfflineTranslationService offlineService = new OfflineTranslationService(context, userPreferences);
            int translationMode = userPreferences.getTranslationMode();
            int downloadedModels = offlineService.getDownloadedModels().size();
            
            switch (translationMode) {
                case UserPreferences.TRANSLATION_MODE_ONLINE_ONLY:
                    return "Online translation only";
                case UserPreferences.TRANSLATION_MODE_OFFLINE_ONLY:
                    return downloadedModels > 0 ? 
                        "Offline mode (" + downloadedModels + " models)" : 
                        "Offline mode (no models)";
                case UserPreferences.TRANSLATION_MODE_AUTO:
                default:
                    return downloadedModels > 0 ? 
                        "Auto mode (" + downloadedModels + " offline models)" : 
                        "Auto mode (online only)";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting quick status", e);
            return "Translation status unknown";
        }
    }
}