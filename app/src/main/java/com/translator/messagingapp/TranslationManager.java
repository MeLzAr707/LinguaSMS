package com.translator.messagingapp;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manager class for handling translation functionality.
 * Consolidates functionality from MessageTranslator and parts of SmsReceiver.
 */
public class TranslationManager {
    private static final String TAG = "TranslationManager";

    // Rate limiting parameters
    private static final Object SYNC_OBJECT = new Object();
    private static long lastTranslationTime = 0;
    private static final long MIN_TRANSLATION_INTERVAL = 5000; // 5 seconds minimum between translations
    private static int translationsToday = 0;
    private static long dayStartTime = System.currentTimeMillis();
    private static final int MAX_TRANSLATIONS_PER_DAY = 100; // Maximum translations per day

    // Cache for recently translated messages to avoid duplicates
    private static final ConcurrentHashMap<String, Long> recentlyTranslatedMessages = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 50;

    // Counter for generating unique notification IDs
    private static final AtomicInteger notificationIdCounter = new AtomicInteger(1001);

    private final Context context;
    private final GoogleTranslationService translationService;
    private final OfflineTranslationService offlineTranslationService;
    private final UserPreferences userPreferences;
    private final ExecutorService executorService;
    private final TranslationCache translationCache;

    /**
     * Creates a new TranslationManager.
     *
     * @param context The application context
     * @param translationService The translation service
     * @param userPreferences The user preferences
     */
    public TranslationManager(Context context, GoogleTranslationService translationService, UserPreferences userPreferences) {
        this.context = context;
        this.translationService = translationService;
        this.offlineTranslationService = new OfflineTranslationService(context, userPreferences);
        this.userPreferences = userPreferences;
        this.executorService = Executors.newCachedThreadPool();
        this.translationCache = new TranslationCache(context);
    }

    /**
     * Gets the translation cache instance.
     *
     * @return The TranslationCache instance
     */
    public TranslationCache getTranslationCache() {
        return translationCache;
    }

    /**
     * Gets a translated text from the cache.
     *
     * @param originalText The original text
     * @param targetLanguage The target language
     * @return The translated text, or null if not found in cache
     */
    public String getTranslatedText(String originalText, String targetLanguage) {
        String cacheKey = originalText + "_" + targetLanguage;
        return translationCache.get(cacheKey);
    }

    /**
     * Interface for translation callbacks.
     */
    public interface TranslationCallback {
        void onTranslationComplete(boolean success, String translatedText, String errorMessage);
    }

    /**
     * Interface for SMS message translation callbacks.
     */
    public interface SmsTranslationCallback {
        void onTranslationComplete(boolean success, SmsMessage translatedMessage);
    }

    /**
     * Translates text from one language to another.
     *
     * @param text The text to translate
     * @param targetLanguage The target language code
     * @param callback The callback to receive the result
     */
    public void translateText(String text, String targetLanguage, TranslationCallback callback) {
        translateText(text, null, targetLanguage, callback);
    }

    /**
     * Translates text from one language to another with force translation option.
     *
     * @param text The text to translate
     * @param targetLanguage The target language code
     * @param callback The callback to receive the result
     * @param forceTranslation Whether to force translation even if source and target languages match
     */
    public void translateText(String text, String targetLanguage, TranslationCallback callback, boolean forceTranslation) {
        translateText(text, null, targetLanguage, callback, forceTranslation);
    }

    /**
     * Translates text from one language to another with source language.
     *
     * @param text The text to translate
     * @param sourceLanguage The source language code (can be null for auto-detect)
     * @param targetLanguage The target language code
     * @param callback The callback to receive the result
     */
    public void translateText(String text, String sourceLanguage, String targetLanguage, TranslationCallback callback) {
        translateText(text, sourceLanguage, targetLanguage, callback, false);
    }

    /**
     * Translates text from one language to another with source language and force translation option.
     *
     * @param text The text to translate
     * @param sourceLanguage The source language code (can be null for auto-detect)
     * @param targetLanguage The target language code
     * @param callback The callback to receive the result
     * @param forceTranslation Whether to force translation even if source and target languages match
     */
    public void translateText(String text, String sourceLanguage, String targetLanguage, TranslationCallback callback, boolean forceTranslation) {
        if (text == null || text.isEmpty()) {
            if (callback != null) {
                callback.onTranslationComplete(false, null, "No text to translate");
            }
            return;
        }

        // Generate cache key
        String cacheKey = text + "_" + (sourceLanguage != null ? sourceLanguage : "auto") + "_" + targetLanguage;

        // Check cache first
        String cachedTranslation = translationCache.get(cacheKey);
        if (cachedTranslation != null) {
            if (callback != null) {
                callback.onTranslationComplete(true, cachedTranslation, null);
            }
            return;
        }

        // Check rate limiting
        if (!checkRateLimiting()) {
            if (callback != null) {
                callback.onTranslationComplete(false, null, "Translation rate limit exceeded");
            }
            return;
        }

        // Determine which translation service to use based on user preferences
        int translationMode = userPreferences.getTranslationMode();
        boolean preferOffline = userPreferences.getPreferOfflineTranslation();

        // Translate in background
        executorService.execute(() -> {
            try {
                String finalSourceLanguage = sourceLanguage;
                
                // If source language is not provided, try to detect it
                if (finalSourceLanguage == null) {
                    // First try offline detection if available, then fall back to online
                    if (shouldUseOfflineTranslation(translationMode, preferOffline, null, targetLanguage)) {
                        // For offline, we'll try English as source and let MLKit handle detection
                        finalSourceLanguage = "en";
                    } else if (translationService != null && translationService.hasApiKey()) {
                        finalSourceLanguage = translationService.detectLanguage(text);
                        if (finalSourceLanguage == null) {
                            if (callback != null) {
                                callback.onTranslationComplete(false, null, "Could not detect language");
                            }
                            return;
                        }
                    } else {
                        // If no API key is available, check if we can use offline translation
                        // This handles OFFLINE_ONLY mode and cases where offline is enabled but not preferred
                        if (translationMode == UserPreferences.TRANSLATION_MODE_OFFLINE_ONLY || 
                            (userPreferences.isOfflineTranslationEnabled() && offlineTranslationService != null)) {
                            // Try offline translation as fallback
                            finalSourceLanguage = "en"; // Let MLKit handle detection
                        } else {
                            if (callback != null) {
                                callback.onTranslationComplete(false, null, "No translation service available");
                            }
                            return;
                        }
                    }
                }

                // Skip if already in target language (comparing base language codes)
                // unless forceTranslation is true (for outgoing messages)
                String baseDetected = finalSourceLanguage.split("-")[0];
                String baseTarget = targetLanguage.split("-")[0];

                if (baseDetected.equals(baseTarget) && !forceTranslation) {
                    if (callback != null) {
                        callback.onTranslationComplete(false, null, "Text is already in " + getLanguageName(baseTarget));
                    }
                    return;
                }

                // Try translation based on mode and availability
                if (shouldUseOfflineTranslation(translationMode, preferOffline, finalSourceLanguage, targetLanguage)) {
                    // Try offline translation first
                    translateOffline(text, finalSourceLanguage, targetLanguage, cacheKey, callback);
                } else if (translationService != null && translationService.hasApiKey()) {
                    // Use online translation
                    translateOnline(text, finalSourceLanguage, targetLanguage, cacheKey, callback);
                } else {
                    // No API key available, check if we can use offline translation as fallback
                    if (translationMode == UserPreferences.TRANSLATION_MODE_OFFLINE_ONLY || 
                        (userPreferences.isOfflineTranslationEnabled() && offlineTranslationService != null)) {
                        // Try offline translation as fallback
                        translateOffline(text, finalSourceLanguage, targetLanguage, cacheKey, callback);
                    } else {
                        if (callback != null) {
                            callback.onTranslationComplete(false, null, "No translation service available");
                        }
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error translating text", e);
                if (callback != null) {
                    callback.onTranslationComplete(false, null, e.getMessage());
                }
            }
        });
    }

    /**
     * Translates an SMS message.
     *
     * @param message The SMS message to translate
     * @param callback The callback to receive the result
     */
    public void translateSmsMessage(SmsMessage message, SmsTranslationCallback callback) {
        if (message == null || TextUtils.isEmpty(message.getOriginalText())) {
            if (callback != null) {
                callback.onTranslationComplete(false, null);
            }
            return;
        }

        // Check if auto-translate is enabled
        if (!userPreferences.isAutoTranslateEnabled()) {
            if (callback != null) {
                callback.onTranslationComplete(false, null);
            }
            return;
        }

        // Check if translation service is available
        // For offline-only mode or when offline is enabled, we don't require an API key
        if (translationService == null || !translationService.hasApiKey()) {
            int translationMode = userPreferences.getTranslationMode();
            if (translationMode != UserPreferences.TRANSLATION_MODE_OFFLINE_ONLY && 
                !userPreferences.isOfflineTranslationEnabled()) {
                if (callback != null) {
                    callback.onTranslationComplete(false, null);
                }
                return;
            }
        }

        // Check rate limiting
        if (!checkRateLimiting()) {
            if (callback != null) {
                callback.onTranslationComplete(false, null);
            }
            return;
        }

        // Create a message ID for deduplication
        String messageId = message.getAddress() + ":" + message.getOriginalText().hashCode();

        // Check if we've recently translated this message
        if (recentlyTranslatedMessages.containsKey(messageId)) {
            if (callback != null) {
                callback.onTranslationComplete(false, null);
            }
            return;
        }

        // Add to recently translated messages
        recentlyTranslatedMessages.put(messageId, System.currentTimeMillis());

        // Clean up cache if it gets too large
        if (recentlyTranslatedMessages.size() > MAX_CACHE_SIZE) {
            cleanupMessageCache();
        }

        // Generate cache key
        String targetLanguage = userPreferences.getPreferredLanguage();
        String cacheKey = message.getOriginalText() + "_" + targetLanguage;

        // Check cache first
        String cachedTranslation = translationCache.get(cacheKey);
        if (cachedTranslation != null) {
            message.setTranslatedText(cachedTranslation);
            message.setTranslatedLanguage(targetLanguage);
            if (callback != null) {
                callback.onTranslationComplete(true, message);
            }
            return;
        }

        // Translate in background
        executorService.execute(() -> {
            try {
                // Determine translation mode and whether to use offline
                int translationMode = userPreferences.getTranslationMode();
                boolean preferOffline = userPreferences.getPreferOfflineTranslation();
                
                String detectedLanguage = null;
                
                // Detect language based on available service
                if (translationService != null && translationService.hasApiKey()) {
                    detectedLanguage = translationService.detectLanguage(message.getOriginalText());
                } else if (translationMode == UserPreferences.TRANSLATION_MODE_OFFLINE_ONLY || 
                          userPreferences.isOfflineTranslationEnabled()) {
                    // For offline mode, assume English as source for now and let offline service handle it
                    detectedLanguage = "en";
                }
                
                if (detectedLanguage == null) {
                    if (callback != null) {
                        callback.onTranslationComplete(false, null);
                    }
                    return;
                }
                
                message.setOriginalLanguage(detectedLanguage);

                // Skip translation if already in user's language (comparing base language codes)
                String baseDetected = detectedLanguage.split("-")[0];
                String baseTarget = targetLanguage.split("-")[0];

                if (baseDetected.equals(baseTarget)) {
                    if (callback != null) {
                        callback.onTranslationComplete(false, null);
                    }
                    return;
                }

                // Choose translation method based on availability and preferences
                if (shouldUseOfflineTranslation(translationMode, preferOffline, detectedLanguage, targetLanguage)) {
                    // Use offline translation
                    offlineTranslationService.translateOffline(message.getOriginalText(), detectedLanguage, targetLanguage,
                        new OfflineTranslationService.OfflineTranslationCallback() {
                            @Override
                            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                                if (success) {
                                    // Update message with translation
                                    message.setTranslatedText(translatedText);
                                    message.setTranslatedLanguage(targetLanguage);

                                    // Cache the translation
                                    translationCache.put(cacheKey, translatedText);

                                    // Update translation counters
                                    updateTranslationCounters();

                                    // Return result
                                    if (callback != null) {
                                        callback.onTranslationComplete(true, message);
                                    }
                                } else {
                                    if (callback != null) {
                                        callback.onTranslationComplete(false, null);
                                    }
                                }
                            }
                        });
                } else if (translationService != null && translationService.hasApiKey()) {
                    // Use online translation
                    String translatedText = translationService.translate(
                            message.getOriginalText(), detectedLanguage, targetLanguage);

                    // Update message with translation
                    message.setTranslatedText(translatedText);
                    message.setTranslatedLanguage(targetLanguage);

                    // Cache the translation
                    translationCache.put(cacheKey, translatedText);

                    // Update translation counters
                    updateTranslationCounters();

                    // Return result
                    if (callback != null) {
                        callback.onTranslationComplete(true, message);
                    }
                } else {
                    // No translation service available
                    if (callback != null) {
                        callback.onTranslationComplete(false, null);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error translating SMS message", e);
                if (callback != null) {
                    callback.onTranslationComplete(false, null);
                }
            }
        });
    }

    /**
     * Translates a Message object.
     *
     * @param message The Message to translate
     * @param callback The callback to receive the result
     */
    public void translateMessage(Message message, TranslationCallback callback) {
        if (message == null || TextUtils.isEmpty(message.getBody())) {
            if (callback != null) {
                callback.onTranslationComplete(false, null, "No text to translate");
            }
            return;
        }

        // Get user's preferred language
        String targetLanguage = message.isIncoming() ?
                userPreferences.getPreferredIncomingLanguage() :
                userPreferences.getPreferredOutgoingLanguage();

        // If not set, fall back to general preferred language
        if (targetLanguage == null || targetLanguage.isEmpty()) {
            targetLanguage = userPreferences.getPreferredLanguage();
        }

        final String finalTargetLanguage = targetLanguage;

        // Generate cache key
        String cacheKey = message.getBody() + "_" + targetLanguage;

        // Check cache first
        String cachedTranslation = translationCache.get(cacheKey);
        if (cachedTranslation != null && !cachedTranslation.trim().isEmpty()) {
            message.setTranslatedText(cachedTranslation);
            message.setTranslatedLanguage(targetLanguage);
            message.setShowTranslation(true);
            if (callback != null) {
                callback.onTranslationComplete(true, cachedTranslation, null);
            }
            return;
        }

        // Check rate limiting
        if (!checkRateLimiting()) {
            if (callback != null) {
                callback.onTranslationComplete(false, null, "Translation rate limit exceeded");
            }
            return;
        }

        // Translate in background
        executorService.execute(() -> {
            try {
                // Detect language
                String detectedLanguage = translationService.detectLanguage(message.getBody());
                if (detectedLanguage == null) {
                    if (callback != null) {
                        callback.onTranslationComplete(false, null, "Could not detect language");
                    }
                    return;
                }

                // Skip if already in target language (comparing base language codes)
                String baseDetected = detectedLanguage.split("-")[0];
                String baseTarget = finalTargetLanguage.split("-")[0];

                if (baseDetected.equals(baseTarget)) {
                    if (callback != null) {
                        callback.onTranslationComplete(false, null, "Text is already in " + getLanguageName(baseTarget));
                    }
                    return;
                }

                // Translate
                String translatedText = translationService.translate(
                        message.getBody(), detectedLanguage, finalTargetLanguage);

                if (translatedText == null || translatedText.trim().isEmpty()) {
                    if (callback != null) {
                        callback.onTranslationComplete(false, null, "Translation failed or returned empty");
                    }
                    return;
                }

                // Update message
                message.setTranslatedText(translatedText);
                message.setOriginalLanguage(detectedLanguage);
                message.setTranslatedLanguage(finalTargetLanguage);
                message.setShowTranslation(true);

                // Cache the translation
                translationCache.put(cacheKey, translatedText);

                // Update translation counters
                updateTranslationCounters();

                // Return result
                if (callback != null) {
                    callback.onTranslationComplete(true, translatedText, null);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error translating message", e);
                if (callback != null) {
                    callback.onTranslationComplete(false, null, e.getMessage());
                }
            }
        });
    }

    /**
     * Checks if translation rate limiting allows a new translation.
     *
     * @return true if a new translation is allowed, false otherwise
     */
    private boolean checkRateLimiting() {
        long currentTime = System.currentTimeMillis();
        synchronized (SYNC_OBJECT) {
            // Reset daily counter if a new day has started
            if (currentTime - dayStartTime > 24 * 60 * 60 * 1000) { // 24 hours
                dayStartTime = currentTime;
                translationsToday = 0;
                Log.d(TAG, "Reset daily translation counter");
            }

            // Check daily translation limit
            if (translationsToday >= MAX_TRANSLATIONS_PER_DAY) {
                Log.d(TAG, "Daily translation limit reached: " + translationsToday + " translations today");
                return false;
            }

            // Rate limiting - check if we've translated recently
            if (currentTime - lastTranslationTime < MIN_TRANSLATION_INTERVAL) {
                Log.d(TAG, "Skipping translation due to rate limiting (too frequent)");
                return false;
            }

            return true;
        }
    }

    /**
     * Updates translation counters after a successful translation.
     */
    private void updateTranslationCounters() {
        long currentTime = System.currentTimeMillis();
        synchronized (SYNC_OBJECT) {
            lastTranslationTime = currentTime;
            translationsToday++;
            Log.d(TAG, "Translation completed. Total today: " + translationsToday);
        }
    }

    /**
     * Cleans up the message cache by removing older entries.
     */
    private void cleanupMessageCache() {
        // Find the oldest entry
        long oldestTime = Long.MAX_VALUE;
        String oldestKey = null;

        for (ConcurrentHashMap.Entry<String, Long> entry : recentlyTranslatedMessages.entrySet()) {
            if (entry.getValue() < oldestTime) {
                oldestTime = entry.getValue();
                oldestKey = entry.getKey();
            }
        }

        // Remove the oldest entry
        if (oldestKey != null) {
            recentlyTranslatedMessages.remove(oldestKey);
        }
    }

    /**
     * Determines whether to use offline translation based on user preferences and availability.
     *
     * @param translationMode The current translation mode
     * @param preferOffline Whether user prefers offline translation
     * @param sourceLanguage The source language
     * @param targetLanguage The target language
     * @return true if offline translation should be used
     */
    private boolean shouldUseOfflineTranslation(int translationMode, boolean preferOffline, String sourceLanguage, String targetLanguage) {
        switch (translationMode) {
            case UserPreferences.TRANSLATION_MODE_OFFLINE_ONLY:
                return true;
            case UserPreferences.TRANSLATION_MODE_ONLINE_ONLY:
                return false;
            case UserPreferences.TRANSLATION_MODE_AUTO:
            default:
                // In auto mode, check if offline is available and preferred
                if (preferOffline && offlineTranslationService != null) {
                    // If source language is not yet known (null), return true to try offline detection
                    // The actual availability check will happen later when both languages are known
                    if (sourceLanguage == null) {
                        return true;
                    }
                    return offlineTranslationService.isOfflineTranslationAvailable(sourceLanguage, targetLanguage);
                }
                return false;
        }
    }

    /**
     * Performs offline translation using MLKit.
     */
    private void translateOffline(String text, String sourceLanguage, String targetLanguage, String cacheKey, TranslationCallback callback) {
        offlineTranslationService.translateOffline(text, sourceLanguage, targetLanguage, new OfflineTranslationService.OfflineTranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                if (success) {
                    // Cache the translation
                    translationCache.put(cacheKey, translatedText);
                    // Update translation counters
                    updateTranslationCounters();
                    
                    if (callback != null) {
                        callback.onTranslationComplete(true, translatedText, null);
                    }
                } else {
                    // If offline fails and we're in auto mode, try online as fallback
                    int translationMode = userPreferences.getTranslationMode();
                    if (translationMode == UserPreferences.TRANSLATION_MODE_AUTO && 
                        translationService != null && translationService.hasApiKey()) {
                        Log.d(TAG, "Offline translation failed, falling back to online: " + errorMessage);
                        translateOnline(text, sourceLanguage, targetLanguage, cacheKey, callback);
                    } else {
                        if (callback != null) {
                            callback.onTranslationComplete(false, null, "Offline translation failed: " + errorMessage);
                        }
                    }
                }
            }
        });
    }

    /**
     * Performs online translation using Google Translate API.
     */
    private void translateOnline(String text, String sourceLanguage, String targetLanguage, String cacheKey, TranslationCallback callback) {
        if (translationService == null || !translationService.hasApiKey()) {
            if (callback != null) {
                callback.onTranslationComplete(false, null, "Online translation service not available");
            }
            return;
        }

        try {
            // Translate using online service
            String translatedText = translationService.translate(text, sourceLanguage, targetLanguage);
            if (translatedText == null) {
                if (callback != null) {
                    callback.onTranslationComplete(false, null, "Online translation failed");
                }
                return;
            }

            // Cache the translation
            translationCache.put(cacheKey, translatedText);
            
            // Update translation counters
            updateTranslationCounters();

            // Return result
            if (callback != null) {
                callback.onTranslationComplete(true, translatedText, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Online translation error", e);
            if (callback != null) {
                callback.onTranslationComplete(false, null, "Online translation error: " + e.getMessage());
            }
        }
    }

    /**
     * Gets a human-readable language name from a language code.
     *
     * @param languageCode The language code (e.g., "en", "es")
     * @return A human-readable language name
     */
    public String getLanguageName(String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            return "Unknown";
        }

        // Strip region code if present
        String baseCode = languageCode.split("-")[0].toLowerCase();

        // Map language codes to names
        switch (baseCode) {
            case "en": return "English";
            case "es": return "Spanish";
            case "fr": return "French";
            case "de": return "German";
            case "it": return "Italian";
            case "pt": return "Portuguese";
            case "nl": return "Dutch";
            case "ru": return "Russian";
            case "zh": return "Chinese";
            case "ja": return "Japanese";
            case "ko": return "Korean";
            case "ar": return "Arabic";
            case "hi": return "Hindi";
            default: return languageCode; // Return the code if name not known
        }
    }

    /**
     * Gets the offline translation service.
     *
     * @return The OfflineTranslationService instance
     */
    public OfflineTranslationService getOfflineTranslationService() {
        return offlineTranslationService;
    }
    /**
     * Translates a Message object and saves its state to the cache.
     *
     * @param message The Message to translate
     * @param callback The callback to receive the result
     */
    public void translateMessageAndSave(Message message, TranslationCallback callback) {
        translateMessage(message, (success, translatedText, errorMessage) -> {
            if (success) {
                // Save the message state to ensure persistence
                message.saveTranslationState(translationCache);
            }

            // Forward the result to the original callback
            if (callback != null) {
                callback.onTranslationComplete(success, translatedText, errorMessage);
            }
        });
    }
    /**
     * Gets translation cache statistics.
     *
     * @return A string containing cache statistics
     */
    public String getCacheStatistics() {
        return translationCache.getStatistics();
    }

    /**
     * Clears the translation cache.
     */
    public void clearCache() {
        translationCache.clear();
    }

    /**
     * Refreshes the offline translation service to pick up any newly downloaded models.
     * This should be called after models are downloaded/deleted via OfflineModelManager.
     */
    public void refreshOfflineModels() {
        if (offlineTranslationService != null) {
            offlineTranslationService.refreshDownloadedModels();
        }
    }

    /**
     * Cleans up resources.
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        if (translationCache != null) {
            translationCache.close();
        }
        if (offlineTranslationService != null) {
            offlineTranslationService.cleanup();
        }
    }

}




