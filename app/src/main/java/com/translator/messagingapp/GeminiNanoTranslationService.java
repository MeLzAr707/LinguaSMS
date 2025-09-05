package com.translator.messagingapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for offline translation using Gemini Nano.
 * Provides offline GenAI translation capabilities with downloadable language models.
 * Replaces ML Kit implementation with Gemini Nano for better offline GenAI features.
 */
public class GeminiNanoTranslationService {
    private static final String TAG = "GeminiNanoTranslation";
    private static final int OPERATION_TIMEOUT_SECONDS = 30;
    
    // Use same preferences structure for compatibility
    private static final String OFFLINE_MODELS_PREFS = "gemini_nano_models";
    private static final String KEY_DOWNLOADED_MODELS = "downloaded_models";

    private final Context context;
    private final UserPreferences userPreferences;
    private final Set<String> downloadedModels;
    private GeminiNanoModelManager modelManager;
    private final ExecutorService executorService;

    /**
     * Interface for translation callbacks.
     */
    public interface GeminiNanoTranslationCallback {
        void onTranslationComplete(boolean success, String translatedText, String errorMessage);
    }

    /**
     * Interface for model download callbacks.
     */
    public interface ModelDownloadCallback {
        void onDownloadComplete(boolean success, String errorMessage);
    }

    /**
     * Creates a new GeminiNanoTranslationService.
     *
     * @param context The application context
     * @param userPreferences The user preferences
     */
    public GeminiNanoTranslationService(Context context, UserPreferences userPreferences) {
        this.context = context;
        this.userPreferences = userPreferences;
        this.executorService = Executors.newFixedThreadPool(2);
        
        // Initialize model tracking
        SharedPreferences prefs = context.getSharedPreferences(OFFLINE_MODELS_PREFS, Context.MODE_PRIVATE);
        this.downloadedModels = prefs.getStringSet(KEY_DOWNLOADED_MODELS, new HashSet<>());
        
        // Initialize Gemini Nano model manager
        this.modelManager = new GeminiNanoModelManager(context);
        
        Log.d(TAG, "GeminiNanoTranslationService initialized");
    }

    /**
     * Translates text offline using Gemini Nano.
     *
     * @param text The text to translate
     * @param sourceLanguage The source language code
     * @param targetLanguage The target language code
     * @param callback The callback for translation results
     */
    public void translateOffline(String text, String sourceLanguage, String targetLanguage, 
                                GeminiNanoTranslationCallback callback) {
        executorService.execute(() -> {
            try {
                // Check if models are available
                if (!isOfflineTranslationAvailable(sourceLanguage, targetLanguage)) {
                    callback.onTranslationComplete(false, null, 
                        "Gemini Nano models not available for language pair: " + sourceLanguage + " -> " + targetLanguage);
                    return;
                }

                // Use Gemini Nano for translation
                String translatedText = performGeminiNanoTranslation(text, sourceLanguage, targetLanguage);
                
                if (translatedText != null && !translatedText.isEmpty()) {
                    callback.onTranslationComplete(true, translatedText, null);
                } else {
                    callback.onTranslationComplete(false, null, "Gemini Nano translation failed");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during Gemini Nano translation", e);
                callback.onTranslationComplete(false, null, "Translation error: " + e.getMessage());
            }
        });
    }

    /**
     * Performs the actual Gemini Nano translation using prompting.
     */
    private String performGeminiNanoTranslation(String text, String sourceLanguage, String targetLanguage) {
        try {
            // Create translation prompt for Gemini Nano
            String prompt = createTranslationPrompt(text, sourceLanguage, targetLanguage);
            
            // Use Gemini Nano to process the translation prompt
            return modelManager.generateResponse(prompt);
        } catch (Exception e) {
            Log.e(TAG, "Gemini Nano translation failed", e);
            return null;
        }
    }

    /**
     * Creates a translation prompt for Gemini Nano.
     */
    private String createTranslationPrompt(String text, String sourceLanguage, String targetLanguage) {
        String sourceLangName = getLanguageName(sourceLanguage);
        String targetLangName = getLanguageName(targetLanguage);
        
        return String.format(
            "Translate the following text from %s to %s. " +
            "Only return the translated text, nothing else.\n\n" +
            "Text to translate: %s",
            sourceLangName, targetLangName, text
        );
    }

    /**
     * Gets the human-readable language name from language code.
     */
    private String getLanguageName(String languageCode) {
        Map<String, String> languageNames = new HashMap<>();
        languageNames.put("en", "English");
        languageNames.put("es", "Spanish");
        languageNames.put("fr", "French");
        languageNames.put("de", "German");
        languageNames.put("it", "Italian");
        languageNames.put("pt", "Portuguese");
        languageNames.put("ru", "Russian");
        languageNames.put("zh", "Chinese");
        languageNames.put("ja", "Japanese");
        languageNames.put("ko", "Korean");
        languageNames.put("ar", "Arabic");
        languageNames.put("hi", "Hindi");
        // Add more language mappings as needed
        
        return languageNames.getOrDefault(languageCode, languageCode);
    }

    /**
     * Checks if offline translation is available for the given language pair.
     */
    public boolean isOfflineTranslationAvailable(String sourceLanguage, String targetLanguage) {
        // For Gemini Nano, we primarily need the base model to be available
        // Individual language models are not needed as Gemini Nano is multilingual
        return modelManager.isGeminiNanoModelAvailable() && 
               isSupportedLanguage(sourceLanguage) && 
               isSupportedLanguage(targetLanguage);
    }

    /**
     * Checks if a language is supported by Gemini Nano.
     */
    private boolean isSupportedLanguage(String languageCode) {
        // Gemini Nano supports major languages
        Set<String> supportedLanguages = new HashSet<>();
        supportedLanguages.add("en"); // English
        supportedLanguages.add("es"); // Spanish
        supportedLanguages.add("fr"); // French
        supportedLanguages.add("de"); // German
        supportedLanguages.add("it"); // Italian
        supportedLanguages.add("pt"); // Portuguese
        supportedLanguages.add("ru"); // Russian
        supportedLanguages.add("zh"); // Chinese
        supportedLanguages.add("ja"); // Japanese
        supportedLanguages.add("ko"); // Korean
        supportedLanguages.add("ar"); // Arabic
        supportedLanguages.add("hi"); // Hindi
        supportedLanguages.add("nl"); // Dutch
        supportedLanguages.add("sv"); // Swedish
        supportedLanguages.add("da"); // Danish
        supportedLanguages.add("no"); // Norwegian
        supportedLanguages.add("fi"); // Finnish
        supportedLanguages.add("pl"); // Polish
        // Add more as needed
        
        return supportedLanguages.contains(languageCode);
    }

    /**
     * Checks if language model is downloaded (compatibility method).
     */
    public boolean isLanguageModelDownloaded(String languageCode) {
        // For Gemini Nano, individual language models are not downloaded separately
        // The main model supports multiple languages
        return modelManager.isGeminiNanoModelAvailable() && isSupportedLanguage(languageCode);
    }

    /**
     * Downloads language model (compatibility method).
     */
    public void downloadLanguageModel(String languageCode, ModelDownloadCallback callback) {
        // For Gemini Nano, we download the main model rather than individual language models
        if (modelManager.isGeminiNanoModelAvailable()) {
            callback.onDownloadComplete(true, null);
            return;
        }
        
        executorService.execute(() -> {
            try {
                boolean success = modelManager.downloadGeminiNanoModel();
                if (success) {
                    callback.onDownloadComplete(true, null);
                } else {
                    callback.onDownloadComplete(false, "Failed to download Gemini Nano model");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error downloading Gemini Nano model", e);
                callback.onDownloadComplete(false, "Download error: " + e.getMessage());
            }
        });
    }

    /**
     * Checks if any models are downloaded.
     */
    public boolean hasAnyDownloadedModels() {
        return modelManager.isGeminiNanoModelAvailable();
    }

    /**
     * Gets available languages.
     */
    public Set<String> getAvailableLanguages() {
        if (!modelManager.isGeminiNanoModelAvailable()) {
            return new HashSet<>();
        }
        
        Set<String> languages = new HashSet<>();
        languages.add("en"); // English
        languages.add("es"); // Spanish
        languages.add("fr"); // French
        languages.add("de"); // German
        languages.add("it"); // Italian
        languages.add("pt"); // Portuguese
        languages.add("ru"); // Russian
        languages.add("zh"); // Chinese
        languages.add("ja"); // Japanese
        languages.add("ko"); // Korean
        languages.add("ar"); // Arabic
        languages.add("hi"); // Hindi
        languages.add("nl"); // Dutch
        languages.add("sv"); // Swedish
        languages.add("da"); // Danish
        languages.add("no"); // Norwegian
        languages.add("fi"); // Finnish
        languages.add("pl"); // Polish
        
        return languages;
    }

    /**
     * Gets detailed model status.
     */
    public Map<String, String> getDetailedModelStatus() {
        Map<String, String> status = new HashMap<>();
        
        if (modelManager.isGeminiNanoModelAvailable()) {
            status.put("gemini_nano", "Available and ready");
            for (String lang : getAvailableLanguages()) {
                status.put(lang, "Supported by Gemini Nano");
            }
        } else {
            status.put("gemini_nano", "Not available - download required");
        }
        
        return status;
    }

    /**
     * Cleanup resources.
     */
    public void cleanup() {
        if (modelManager != null) {
            modelManager.cleanup();
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}