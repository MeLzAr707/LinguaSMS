package com.translator.messagingapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service for offline translation using Google MLKit.
 * Provides offline translation capabilities with downloadable language models.
 */
public class OfflineTranslationService {
    private static final String TAG = "OfflineTranslationService";
    private static final int OPERATION_TIMEOUT_SECONDS = 30;
    
    // Use same preferences as OfflineModelManager for synchronization
    private static final String OFFLINE_MODELS_PREFS = "offline_models";
    private static final String KEY_DOWNLOADED_MODELS = "downloaded_models";

    private final Context context;
    private final UserPreferences userPreferences;
    private final Set<String> downloadedModels;

    /**
     * Interface for translation callbacks.
     */
    public interface OfflineTranslationCallback {
        void onTranslationComplete(boolean success, String translatedText, String errorMessage);
    }

    /**
     * Interface for model download callbacks.
     */
    public interface ModelDownloadCallback {
        void onDownloadComplete(boolean success, String languageCode, String errorMessage);
        void onDownloadProgress(String languageCode, int progress);
    }

    /**
     * Creates a new OfflineTranslationService.
     *
     * @param context The application context
     * @param userPreferences The user preferences
     */
    public OfflineTranslationService(Context context, UserPreferences userPreferences) {
        this.context = context.getApplicationContext();
        this.userPreferences = userPreferences;
        this.downloadedModels = new HashSet<>();
        
        // Load list of downloaded models from preferences
        loadDownloadedModels();
    }

    /**
     * Checks if offline translation is available for the given language pair.
     *
     * @param sourceLanguage The source language code
     * @param targetLanguage The target language code
     * @return true if offline translation is available, false otherwise
     */
    public boolean isOfflineTranslationAvailable(String sourceLanguage, String targetLanguage) {
        if (sourceLanguage == null || targetLanguage == null) {
            return false;
        }

        // Convert language codes to MLKit format if needed
        String sourceMLKit = convertToMLKitLanguageCode(sourceLanguage);
        String targetMLKit = convertToMLKitLanguageCode(targetLanguage);

        if (sourceMLKit == null || targetMLKit == null) {
            return false;
        }

        // Check if both language models are downloaded (internal tracking)
        boolean internalTracking = downloadedModels.contains(sourceMLKit) && downloadedModels.contains(targetMLKit);
        
        // If internal tracking says models are available, verify with MLKit
        if (internalTracking) {
            return verifyModelAvailabilityWithMLKit(sourceMLKit, targetMLKit);
        }
        
        // If internal tracking says not available, also check with MLKit in case tracking is out of sync
        boolean mlkitAvailable = verifyModelAvailabilityWithMLKit(sourceMLKit, targetMLKit);
        if (mlkitAvailable) {
            // Update our internal tracking to sync with MLKit
            downloadedModels.add(sourceMLKit);
            downloadedModels.add(targetMLKit);
            saveDownloadedModels();
            Log.d(TAG, "Synced internal tracking with MLKit - models were available but not tracked");
        }
        
        return mlkitAvailable;
    }

    /**
     * Verifies model availability directly with MLKit.
     * This is more reliable than our internal tracking.
     *
     * @param sourceMLKit The source language code in MLKit format
     * @param targetMLKit The target language code in MLKit format
     * @return true if both models are available in MLKit, false otherwise
     */
    private boolean verifyModelAvailabilityWithMLKit(String sourceMLKit, String targetMLKit) {
        try {
            // Create translator to check model availability
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(sourceMLKit)
                    .setTargetLanguage(targetMLKit)
                    .build();

            Translator translator = Translation.getClient(options);
            
            // Try a simple translation to test if models are available
            // We'll use a very short timeout to avoid waiting for downloads
            try {
                Task<String> translateTask = translator.translate("test");
                
                // Wait briefly to see if translation can complete immediately
                String result = Tasks.await(translateTask, 2, TimeUnit.SECONDS);
                
                // If we got a result, models are available
                Log.d(TAG, "MLKit models verified available: " + sourceMLKit + " -> " + targetMLKit);
                return true;
                
            } catch (TimeoutException e) {
                // Timeout likely means models need to be downloaded
                Log.d(TAG, "MLKit model verification timeout (models likely not downloaded): " + sourceMLKit + " -> " + targetMLKit);
                return false;
            } catch (ExecutionException e) {
                // Check if the error indicates missing models
                if (e.getCause() != null && e.getCause().getMessage() != null) {
                    String errorMsg = e.getCause().getMessage().toLowerCase();
                    if (errorMsg.contains("model") && errorMsg.contains("download")) {
                        Log.d(TAG, "MLKit indicates models not downloaded: " + e.getCause().getMessage());
                        return false;
                    }
                }
                Log.d(TAG, "MLKit model verification failed: " + e.getMessage());
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error verifying model availability with MLKit", e);
            return false;
        }
    }

    /**
     * Translates text using offline models with enhanced handling for complex sentences.
     *
     * @param text The text to translate
     * @param sourceLanguage The source language code
     * @param targetLanguage The target language code
     * @param callback The callback to receive the result
     */
    public void translateOffline(String text, String sourceLanguage, String targetLanguage, 
                               OfflineTranslationCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            if (callback != null) {
                callback.onTranslationComplete(false, null, "No text to translate");
            }
            return;
        }

        // Convert language codes to MLKit format
        String sourceMLKit = convertToMLKitLanguageCode(sourceLanguage);
        String targetMLKit = convertToMLKitLanguageCode(targetLanguage);

        if (sourceMLKit == null || targetMLKit == null) {
            if (callback != null) {
                callback.onTranslationComplete(false, null, "Unsupported language pair");
            }
            return;
        }

        // Check if models are available
        if (!isOfflineTranslationAvailable(sourceLanguage, targetLanguage)) {
            if (callback != null) {
                callback.onTranslationComplete(false, null, "Language models not downloaded");
            }
            return;
        }

        // For complex or long text, use enhanced translation with chunking
        if (isComplexText(text)) {
            translateComplexTextOffline(text, sourceMLKit, targetMLKit, callback);
        } else {
            // Use standard translation for simple text
            translateSimpleTextOffline(text, sourceMLKit, targetMLKit, callback);
        }
    }

    /**
     * Translates simple text using standard MLKit translation.
     */
    private void translateSimpleTextOffline(String text, String sourceMLKit, String targetMLKit, 
                                          OfflineTranslationCallback callback) {
        // Create translator
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceMLKit)
                .setTargetLanguage(targetMLKit)
                .build();

        Translator translator = Translation.getClient(options);

        // Perform translation
        translator.translate(text)
                .addOnSuccessListener(translatedText -> {
                    Log.d(TAG, "Offline translation successful");
                    if (callback != null) {
                        callback.onTranslationComplete(true, translatedText, null);
                    }
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Offline translation failed", exception);
                    if (callback != null) {
                        callback.onTranslationComplete(false, null, exception.getMessage());
                    }
                });
    }

    /**
     * Translates complex text by breaking it into chunks and preserving context.
     */
    private void translateComplexTextOffline(String text, String sourceMLKit, String targetMLKit, 
                                           OfflineTranslationCallback callback) {
        Log.d(TAG, "Using enhanced translation for complex text");
        
        // Split text into sentences while preserving context
        java.util.List<String> sentences = splitIntoSentences(text);
        
        if (sentences.size() <= 1) {
            // If only one sentence, use simple translation
            translateSimpleTextOffline(text, sourceMLKit, targetMLKit, callback);
            return;
        }

        // Create translator
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceMLKit)
                .setTargetLanguage(targetMLKit)
                .build();

        Translator translator = Translation.getClient(options);
        
        // Translate sentences with context preservation
        translateSentencesWithContext(translator, sentences, callback);
    }

    /**
     * Determines if text is complex enough to require enhanced translation.
     */
    private boolean isComplexText(String text) {
        if (text == null) return false;
        
        // Consider text complex if it has:
        // 1. Multiple sentences (contains sentence-ending punctuation)
        // 2. Is longer than 100 characters
        // 3. Contains complex punctuation or formatting
        
        int length = text.length();
        boolean hasMultipleSentences = text.split("[.!?]+").length > 1;
        boolean isLong = length > 100;
        boolean hasComplexPunctuation = text.contains(";") || text.contains(":") || 
                                       text.contains("\"") || text.contains("'");
        
        return hasMultipleSentences || isLong || hasComplexPunctuation;
    }

    /**
     * Splits text into sentences while preserving punctuation and context.
     */
    private java.util.List<String> splitIntoSentences(String text) {
        java.util.List<String> sentences = new java.util.ArrayList<>();
        
        // Enhanced sentence splitting that preserves context
        String[] parts = text.split("(?<=[.!?])\\s+");
        
        StringBuilder currentSentence = new StringBuilder();
        for (String part : parts) {
            if (currentSentence.length() + part.length() > 150) {
                // If adding this part would make the sentence too long, finish current sentence
                if (currentSentence.length() > 0) {
                    sentences.add(currentSentence.toString().trim());
                    currentSentence = new StringBuilder();
                }
            }
            
            if (currentSentence.length() > 0) {
                currentSentence.append(" ");
            }
            currentSentence.append(part);
        }
        
        // Add remaining text
        if (currentSentence.length() > 0) {
            sentences.add(currentSentence.toString().trim());
        }
        
        // If no proper sentences found, split by length
        if (sentences.isEmpty() && text.length() > 150) {
            sentences = splitByLength(text, 150);
        } else if (sentences.isEmpty()) {
            sentences.add(text);
        }
        
        return sentences;
    }

    /**
     * Splits text by character length while trying to preserve word boundaries.
     */
    private java.util.List<String> splitByLength(String text, int maxLength) {
        java.util.List<String> chunks = new java.util.ArrayList<>();
        
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxLength, text.length());
            
            // Try to split at word boundary
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
            }
            
            chunks.add(text.substring(start, end).trim());
            start = end;
            
            // Skip any leading whitespace
            while (start < text.length() && Character.isWhitespace(text.charAt(start))) {
                start++;
            }
        }
        
        return chunks;
    }

    /**
     * Translates sentences with context preservation and combines results.
     */
    private void translateSentencesWithContext(Translator translator, java.util.List<String> sentences, 
                                             OfflineTranslationCallback callback) {
        java.util.concurrent.atomic.AtomicInteger completedCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicBoolean hasError = new java.util.concurrent.atomic.AtomicBoolean(false);
        String[] translatedSentences = new String[sentences.size()];
        
        for (int i = 0; i < sentences.size(); i++) {
            final int index = i;
            String sentence = sentences.get(i);
            
            translator.translate(sentence)
                    .addOnSuccessListener(translatedText -> {
                        synchronized (translatedSentences) {
                            translatedSentences[index] = translatedText;
                            
                            if (completedCount.incrementAndGet() == sentences.size() && !hasError.get()) {
                                // All sentences translated successfully
                                String finalResult = combineTranslatedSentences(translatedSentences);
                                if (callback != null) {
                                    callback.onTranslationComplete(true, finalResult, null);
                                }
                            }
                        }
                    })
                    .addOnFailureListener(exception -> {
                        if (hasError.compareAndSet(false, true)) {
                            Log.e(TAG, "Complex translation failed at sentence " + index, exception);
                            if (callback != null) {
                                callback.onTranslationComplete(false, null, 
                                    "Translation failed: " + exception.getMessage());
                            }
                        }
                    });
        }
    }

    /**
     * Combines translated sentences back into coherent text.
     */
    private String combineTranslatedSentences(String[] translatedSentences) {
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < translatedSentences.length; i++) {
            if (translatedSentences[i] != null) {
                if (result.length() > 0) {
                    // Add appropriate spacing between sentences
                    if (!result.toString().endsWith(" ")) {
                        result.append(" ");
                    }
                }
                result.append(translatedSentences[i]);
            }
        }
        
        return result.toString().trim();
    }

    /**
     * Downloads a language model for offline translation.
     *
     * @param languageCode The language code
     * @param callback The callback to receive download progress and result
     */
    public void downloadLanguageModel(String languageCode, ModelDownloadCallback callback) {
        String mlkitLanguageCode = convertToMLKitLanguageCode(languageCode);
        if (mlkitLanguageCode == null) {
            if (callback != null) {
                callback.onDownloadComplete(false, languageCode, "Unsupported language");
            }
            return;
        }

        Log.d(TAG, "Starting download for language model: " + mlkitLanguageCode);

        // Create a translator to trigger model download
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH) // Use English as default source
                .setTargetLanguage(mlkitLanguageCode)
                .build();

        Translator translator = Translation.getClient(options);

        // Download the model
        translator.downloadModelIfNeeded()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Language model downloaded successfully: " + mlkitLanguageCode);
                    downloadedModels.add(mlkitLanguageCode);
                    saveDownloadedModels();
                    
                    // Refresh the model list to ensure synchronization
                    loadDownloadedModels();
                    
                    if (callback != null) {
                        callback.onDownloadComplete(true, languageCode, null);
                    }
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Failed to download language model: " + mlkitLanguageCode, exception);
                    if (callback != null) {
                        callback.onDownloadComplete(false, languageCode, exception.getMessage());
                    }
                });
    }

    /**
     * Deletes a downloaded language model.
     *
     * @param languageCode The language code
     */
    public void deleteLanguageModel(String languageCode) {
        String mlkitLanguageCode = convertToMLKitLanguageCode(languageCode);
        if (mlkitLanguageCode == null) {
            return;
        }

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(mlkitLanguageCode)
                .build();

        Translator translator = Translation.getClient(options);
        translator.close();

        downloadedModels.remove(mlkitLanguageCode);
        saveDownloadedModels();
        Log.d(TAG, "Language model deleted: " + mlkitLanguageCode);
    }

    /**
     * Gets the list of supported languages for offline translation.
     *
     * @return Array of supported language codes
     */
    public String[] getSupportedLanguages() {
        return new String[]{
                "en", "ar", "bg", "bn", "ca", "cs", "cy", "da", "de", "el", "eo", "es", "et", "fa", "fi", "fr", "ga", "gl", "gu", "he", "hi", "hr", "hu", "id", "is", "it", "ja", "ka", "kn", "ko", "lt", "lv", "mk", "mr", "ms", "mt", "nl", "no", "pl", "pt", "ro", "ru", "sk", "sl", "sq", "sv", "sw", "ta", "te", "th", "tl", "tr", "uk", "ur", "vi", "zh"
        };
    }

    /**
     * Gets the list of downloaded language models.
     *
     * @return Set of downloaded language codes (MLKit format)
     */
    public Set<String> getDownloadedModels() {
        return new HashSet<>(downloadedModels);
    }

    /**
     * Checks if a specific language model is downloaded.
     *
     * @param languageCode The language code
     * @return true if the model is downloaded, false otherwise
     */
    public boolean isLanguageModelDownloaded(String languageCode) {
        String mlkitLanguageCode = convertToMLKitLanguageCode(languageCode);
        return mlkitLanguageCode != null && downloadedModels.contains(mlkitLanguageCode);
    }

    /**
     * Checks if any offline models are downloaded.
     *
     * @return true if at least one language model is downloaded, false otherwise
     */
    public boolean hasAnyDownloadedModels() {
        return !downloadedModels.isEmpty();
    }

    /**
     * Converts standard language codes to MLKit language codes.
     *
     * @param languageCode The standard language code
     * @return The MLKit language code, or null if not supported
     */
    private String convertToMLKitLanguageCode(String languageCode) {
        if (languageCode == null) {
            return null;
        }

        // Remove region code if present (e.g., "en-US" -> "en")
        String baseCode = languageCode.split("-")[0].toLowerCase();

        // Map common language codes to MLKit codes
        switch (baseCode) {
            case "en": return TranslateLanguage.ENGLISH;
            case "es": return TranslateLanguage.SPANISH;
            case "fr": return TranslateLanguage.FRENCH;
            case "de": return TranslateLanguage.GERMAN;
            case "it": return TranslateLanguage.ITALIAN;
            case "pt": return TranslateLanguage.PORTUGUESE;
            case "ru": return TranslateLanguage.RUSSIAN;
            case "zh": return TranslateLanguage.CHINESE;
            case "ja": return TranslateLanguage.JAPANESE;
            case "ko": return TranslateLanguage.KOREAN;
            case "ar": return TranslateLanguage.ARABIC;
            case "hi": return TranslateLanguage.HINDI;
            case "nl": return TranslateLanguage.DUTCH;
            case "sv": return TranslateLanguage.SWEDISH;
            case "da": return TranslateLanguage.DANISH;
            case "no": return TranslateLanguage.NORWEGIAN;
            case "fi": return TranslateLanguage.FINNISH;
            case "pl": return TranslateLanguage.POLISH;
            case "cs": return TranslateLanguage.CZECH;
            case "sk": return TranslateLanguage.SLOVAK;
            case "hu": return TranslateLanguage.HUNGARIAN;
            case "ro": return TranslateLanguage.ROMANIAN;
            case "bg": return TranslateLanguage.BULGARIAN;
            case "hr": return TranslateLanguage.CROATIAN;
            case "sl": return TranslateLanguage.SLOVENIAN;
            case "et": return TranslateLanguage.ESTONIAN;
            case "lv": return TranslateLanguage.LATVIAN;
            case "lt": return TranslateLanguage.LITHUANIAN;
            case "th": return TranslateLanguage.THAI;
            case "vi": return TranslateLanguage.VIETNAMESE;
            case "id": return TranslateLanguage.INDONESIAN;
            case "ms": return TranslateLanguage.MALAY;
            case "tl": return TranslateLanguage.TAGALOG;
            case "sw": return TranslateLanguage.SWAHILI;
            case "tr": return TranslateLanguage.TURKISH;
            case "he": return TranslateLanguage.HEBREW;
            case "fa": return TranslateLanguage.PERSIAN;
            case "ur": return TranslateLanguage.URDU;
            case "bn": return TranslateLanguage.BENGALI;
            case "gu": return TranslateLanguage.GUJARATI;
            case "kn": return TranslateLanguage.KANNADA;
            case "mr": return TranslateLanguage.MARATHI;
            case "ta": return TranslateLanguage.TAMIL;
            case "te": return TranslateLanguage.TELUGU;
            default: return null; // Unsupported language
        }
    }

    /**
     * Converts MLKit language codes back to standard language codes.
     *
     * @param mlkitLanguageCode The MLKit language code
     * @return The standard language code, or null if not recognized
     */
    private String convertFromMLKitLanguageCode(String mlkitLanguageCode) {
        if (mlkitLanguageCode == null) {
            return null;
        }

        // Map MLKit codes back to standard language codes
        switch (mlkitLanguageCode) {
            case TranslateLanguage.ENGLISH: return "en";
            case TranslateLanguage.SPANISH: return "es";
            case TranslateLanguage.FRENCH: return "fr";
            case TranslateLanguage.GERMAN: return "de";
            case TranslateLanguage.ITALIAN: return "it";
            case TranslateLanguage.PORTUGUESE: return "pt";
            case TranslateLanguage.RUSSIAN: return "ru";
            case TranslateLanguage.CHINESE: return "zh";
            case TranslateLanguage.JAPANESE: return "ja";
            case TranslateLanguage.KOREAN: return "ko";
            case TranslateLanguage.ARABIC: return "ar";
            case TranslateLanguage.HINDI: return "hi";
            case TranslateLanguage.DUTCH: return "nl";
            case TranslateLanguage.SWEDISH: return "sv";
            case TranslateLanguage.DANISH: return "da";
            case TranslateLanguage.NORWEGIAN: return "no";
            case TranslateLanguage.FINNISH: return "fi";
            case TranslateLanguage.POLISH: return "pl";
            case TranslateLanguage.CZECH: return "cs";
            case TranslateLanguage.SLOVAK: return "sk";
            case TranslateLanguage.HUNGARIAN: return "hu";
            case TranslateLanguage.ROMANIAN: return "ro";
            case TranslateLanguage.BULGARIAN: return "bg";
            case TranslateLanguage.CROATIAN: return "hr";
            case TranslateLanguage.SLOVENIAN: return "sl";
            case TranslateLanguage.ESTONIAN: return "et";
            case TranslateLanguage.LATVIAN: return "lv";
            case TranslateLanguage.LITHUANIAN: return "lt";
            case TranslateLanguage.THAI: return "th";
            case TranslateLanguage.VIETNAMESE: return "vi";
            case TranslateLanguage.INDONESIAN: return "id";
            case TranslateLanguage.MALAY: return "ms";
            case TranslateLanguage.TAGALOG: return "tl";
            case TranslateLanguage.SWAHILI: return "sw";
            case TranslateLanguage.TURKISH: return "tr";
            case TranslateLanguage.HEBREW: return "he";
            case TranslateLanguage.PERSIAN: return "fa";
            case TranslateLanguage.URDU: return "ur";
            case TranslateLanguage.BENGALI: return "bn";
            case TranslateLanguage.GUJARATI: return "gu";
            case TranslateLanguage.KANNADA: return "kn";
            case TranslateLanguage.MARATHI: return "mr";
            case TranslateLanguage.TAMIL: return "ta";
            case TranslateLanguage.TELUGU: return "te";
            default: return null; // Unknown MLKit language code
        }
    }
   //  * Now uses the same SharedPreferences as OfflineModelManager for synchronization.

    private void loadDownloadedModels() {
        try {
            // Use same SharedPreferences as OfflineModelManager
            SharedPreferences modelPrefs = context.getSharedPreferences(OFFLINE_MODELS_PREFS, Context.MODE_PRIVATE);
            Set<String> rawDownloadedModels = modelPrefs.getStringSet(KEY_DOWNLOADED_MODELS, new HashSet<>());
            
            // Convert raw language codes to MLKit format for internal use
            downloadedModels.clear();
            for (String rawCode : rawDownloadedModels) {
                String mlkitCode = convertToMLKitLanguageCode(rawCode);
                if (mlkitCode != null) {
                    downloadedModels.add(mlkitCode);
                    Log.d(TAG, "Loaded model: " + rawCode + " -> " + mlkitCode);
                }
            }
            
            Log.d(TAG, "Loaded " + downloadedModels.size() + " downloaded models from OfflineModelManager prefs");
        } catch (Exception e) {
            Log.e(TAG, "Error loading downloaded models", e);
        }
    }

    /**
     * Saves the list of downloaded models to preferences.
     * Now saves to the same SharedPreferences as OfflineModelManager for synchronization.
     */
    private void saveDownloadedModels() {
        try {
            // Convert MLKit codes back to raw language codes for storage
            Set<String> rawCodes = new HashSet<>();
            for (String mlkitCode : downloadedModels) {
                String rawCode = convertFromMLKitLanguageCode(mlkitCode);
                if (rawCode != null) {
                    rawCodes.add(rawCode);
                }
            }
            
            // Save to same SharedPreferences as OfflineModelManager
            SharedPreferences modelPrefs = context.getSharedPreferences(OFFLINE_MODELS_PREFS, Context.MODE_PRIVATE);
            modelPrefs.edit().putStringSet(KEY_DOWNLOADED_MODELS, rawCodes).apply();
            
            Log.d(TAG, "Saved " + rawCodes.size() + " downloaded models to OfflineModelManager prefs");
        } catch (Exception e) {
            Log.e(TAG, "Error saving downloaded models", e);
        }
    }

    /**
     * Refreshes the list of downloaded models from SharedPreferences.
     * This method should be called when models are downloaded/deleted via OfflineModelManager
     * to ensure synchronization between the two systems.
     */
    public void refreshDownloadedModels() {
        loadDownloadedModels();
        Log.d(TAG, "Refreshed downloaded models list");
    }

    /**
     * Cleanup resources.
     */
    public void cleanup() {
        // Clean up any resources if needed
        Log.d(TAG, "OfflineTranslationService cleanup complete");
    }
}