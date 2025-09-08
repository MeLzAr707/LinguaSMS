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
 * Provides online translation capabilities using Google Translate API.
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
    private final UserPreferences userPreferences;
    private final ExecutorService executorService;
    private final TranslationCache translationCache;
    private final OfflineTranslationService offlineTranslationService;
    private final LanguageDetectionService languageDetectionService;

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
        this.userPreferences = userPreferences;
        this.executorService = Executors.newCachedThreadPool();
        this.translationCache = new TranslationCache(context);
        this.offlineTranslationService = new OfflineTranslationService(context);
        this.languageDetectionService = new LanguageDetectionService(context, translationService);
        
        Log.d(TAG, "TranslationManager initialized with offline and online translation services");
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
     * Gets the offline translation service instance.
     *
     * @return The OfflineTranslationService instance
     */
    public OfflineTranslationService getOfflineTranslationService() {
        return offlineTranslationService;
    }

    /**
     * Gets the language detection service instance.
     *
     * @return The LanguageDetectionService instance
     */
    public LanguageDetectionService getLanguageDetectionService() {
        return languageDetectionService;
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
     * Enhanced interface for translation callbacks with activity context for showing dialogs.
     */
    public interface EnhancedTranslationCallback extends TranslationCallback {
        android.app.Activity getActivity();
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

        // Translate in background using offline-first approach
        executorService.execute(() -> {
            try {
                // Determine source language (detect if not provided)
                String detectedSourceLanguage = sourceLanguage;
                if (detectedSourceLanguage == null) {
                    detectedSourceLanguage = detectLanguage(text);
                    
                    if (detectedSourceLanguage == null) {
                        if (callback != null) {
                            callback.onTranslationComplete(false, null, "Could not detect language");
                        }
                        return;
                    }
                }
                final String finalSourceLanguage = detectedSourceLanguage;

                // Skip if already in target language (comparing base language codes)
                // unless forceTranslation is true (for UI-triggered translations)
                String baseDetected = finalSourceLanguage.split("-")[0];
                String baseTarget = targetLanguage.split("-")[0];

                if (baseDetected.equals(baseTarget) && !forceTranslation) {
                    if (callback != null) {
                        callback.onTranslationComplete(false, null, "Text is already in " + getLanguageName(baseTarget));
                    }
                    return;
                }

                // Try offline translation first if enabled and models are available
                if (shouldUseOfflineTranslation(finalSourceLanguage, targetLanguage)) {
                    translateOffline(text, finalSourceLanguage, targetLanguage, cacheKey, callback);
                } else {
                    // Fall back to online translation
                    if (translationService != null && translationService.hasApiKey()) {
                        if (!checkRateLimiting()) {
                            if (callback != null) {
                                callback.onTranslationComplete(false, null, "Translation rate limit exceeded");
                            }
                            return;
                        }
                        translateOnline(text, finalSourceLanguage, targetLanguage, cacheKey, callback);
                    } else {
                        if (callback != null) {
                            callback.onTranslationComplete(false, null, "No translation service available - offline models not downloaded and no API key");
                        }
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in translation process", e);
                if (callback != null) {
                    callback.onTranslationComplete(false, null, "Translation error: " + e.getMessage());
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
        if (translationService == null || !translationService.hasApiKey()) {
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
        String targetLanguage = userPreferences.getPreferredIncomingLanguage();
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
                // Use online language detection service
                final String detectedLanguage = translationService.detectLanguage(message.getOriginalText());
                
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

                // Check rate limiting for online translation
                if (!checkRateLimiting()) {
                    if (callback != null) {
                        callback.onTranslationComplete(false, null);
                    }
                    return;
                }

                // Use online translation
                if (translationService != null && translationService.hasApiKey()) {
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
        String cacheKey = message.getBody() + "_" + finalTargetLanguage;

        // Check cache first
        String cachedTranslation = translationCache.get(cacheKey);
        if (cachedTranslation != null && !cachedTranslation.trim().isEmpty()) {
            message.setTranslatedText(cachedTranslation);
            message.setTranslatedLanguage(finalTargetLanguage);
            message.setShowTranslation(true);
            if (callback != null) {
                callback.onTranslationComplete(true, cachedTranslation, null);
            }
            return;
        }

        // Check rate limiting - this method currently only uses online translation
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
                final String detectedLanguage = translationService.detectLanguage(message.getBody());
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
     * Determines if offline translation should be used for the given language pair.
     */
    private boolean shouldUseOfflineTranslation(String sourceLanguage, String targetLanguage) {
        // Check user preferences
        if (!userPreferences.isOfflineTranslationEnabled()) {
            return false;
        }
        
        int translationMode = userPreferences.getTranslationMode();
        if (translationMode == UserPreferences.TRANSLATION_MODE_ONLINE) {
            return false;
        }
        
        // Check if offline translation is supported for this language pair
        if (!offlineTranslationService.isLanguagePairSupported(sourceLanguage, targetLanguage)) {
            Log.d(TAG, "Language pair not supported by ML Kit: " + sourceLanguage + " -> " + targetLanguage);
            return false;
        }
        
        // For offline or auto modes, attempt offline translation even if models are not available
        // This allows the missing model prompt to be triggered
        if (translationMode == UserPreferences.TRANSLATION_MODE_OFFLINE || 
            translationMode == UserPreferences.TRANSLATION_MODE_AUTO) {
            
            boolean modelsAvailable = offlineTranslationService.areModelsAvailable(sourceLanguage, targetLanguage);
            Log.d(TAG, "Models available for " + sourceLanguage + " -> " + targetLanguage + ": " + modelsAvailable);
            
            // Return true to attempt offline translation, which will prompt for missing models if needed
            return true;
        }
        
        return false;
    }
    
    /**
     * Detects the language of the given text using the language detection service.
     */
    private String detectLanguage(String text) {
        try {
            return languageDetectionService.detectLanguageSync(text);
        } catch (Exception e) {
            Log.w(TAG, "Language detection failed: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Performs offline translation using ML Kit.
     */
    private void translateOffline(String text, String sourceLanguage, String targetLanguage, String cacheKey, TranslationCallback callback) {
        Log.d(TAG, "Attempting offline translation: " + sourceLanguage + " -> " + targetLanguage);
        
        offlineTranslationService.translateText(text, sourceLanguage, targetLanguage, 
            new OfflineTranslationService.TranslationCallback() {
                @Override
                public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                    if (success && translatedText != null) {
                        // Cache the translation
                        translationCache.put(cacheKey, translatedText);
                        Log.d(TAG, "Offline translation successful");
                        
                        if (callback != null) {
                            callback.onTranslationComplete(true, translatedText, null);
                        }
                    } else {
                        Log.w(TAG, "Offline translation failed: " + errorMessage);
                        
                        // Check if the error is due to missing models and if we can prompt for download
                        if (errorMessage != null && errorMessage.contains("Language models not downloaded") && 
                            callback instanceof EnhancedTranslationCallback) {
                            
                            EnhancedTranslationCallback enhancedCallback = (EnhancedTranslationCallback) callback;
                            android.app.Activity activity = enhancedCallback.getActivity();
                            
                            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                                // Prompt user to download missing models
                                promptForMissingModels(activity, text, sourceLanguage, targetLanguage, cacheKey, callback);
                                return;
                            }
                        }
                        
                        // Try online fallback if available and allowed
                        if (userPreferences.getTranslationMode() == UserPreferences.TRANSLATION_MODE_AUTO &&
                            translationService != null && translationService.hasApiKey()) {
                            
                            Log.d(TAG, "Falling back to online translation");
                            if (checkRateLimiting()) {
                                translateOnline(text, sourceLanguage, targetLanguage, cacheKey, callback);
                            } else {
                                if (callback != null) {
                                    callback.onTranslationComplete(false, null, "Offline translation failed and rate limit exceeded for online fallback");
                                }
                            }
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
     * Prompts the user to download missing language models and retries translation.
     */
    private void promptForMissingModels(android.app.Activity activity, String text, String sourceLanguage, 
                                       String targetLanguage, String cacheKey, TranslationCallback callback) {
        ModelDownloadPrompt.promptForMissingModel(activity, sourceLanguage, targetLanguage, this, 
                offlineTranslationService, new ModelDownloadPrompt.ModelDownloadCallback() {
            @Override
            public void onDownloadCompleted(boolean success, String errorMessage) {
                if (success) {
                    // Retry the translation now that models are downloaded
                    Log.d(TAG, "Models downloaded successfully, retrying translation");
                    translateOffline(text, sourceLanguage, targetLanguage, cacheKey, callback);
                } else {
                    // Download failed, report error
                    if (callback != null) {
                        callback.onTranslationComplete(false, null, 
                            "Model download failed: " + (errorMessage != null ? errorMessage : "Unknown error"));
                    }
                }
            }
            
            @Override
            public void onUserDeclined() {
                // User declined to download models
                if (callback != null) {
                    callback.onTranslationComplete(false, null, 
                        "Translation cancelled: Required language models not available");
                }
            }
        });
    }
    
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
     * Cleans up resources.
     */
    public void cleanup() {
        if (offlineTranslationService != null) {
            offlineTranslationService.cleanup();
        }
        if (languageDetectionService != null) {
            languageDetectionService.cleanup();
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        if (translationCache != null) {
            translationCache.close();
        }
    }

}




