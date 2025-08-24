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
        
        // If internal tracking says models are available, trust it (with improved MLKit verification as fallback)
        if (internalTracking) {
            Log.d(TAG, "Models found in internal tracking: " + sourceMLKit + " -> " + targetMLKit);
            return verifyModelAvailabilityWithMLKit(sourceMLKit, targetMLKit);
        }
        
        // If internal tracking says not available, check with MLKit in case tracking is out of sync
        Log.d(TAG, "Models not in internal tracking, checking with MLKit: " + sourceMLKit + " -> " + targetMLKit);
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
            // Use a longer timeout to allow for MLKit initialization
            try {
                Task<String> translateTask = translator.translate("test");
                
                // Wait longer to allow MLKit to initialize if models are downloaded
                String result = Tasks.await(translateTask, 10, TimeUnit.SECONDS);
                
                // If we got a result, models are available
                Log.d(TAG, "MLKit models verified available: " + sourceMLKit + " -> " + targetMLKit);
                return true;
                
            } catch (TimeoutException e) {
                // Timeout could mean models need to be downloaded OR MLKit is still initializing
                // Let's try a different approach - check if models are downloaded first
                Log.d(TAG, "MLKit model verification timeout: " + sourceMLKit + " -> " + targetMLKit);
                
                // If we think models are downloaded based on our tracking, give MLKit more time
                if (downloadedModels.contains(sourceMLKit) && downloadedModels.contains(targetMLKit)) {
                    Log.d(TAG, "Models appear downloaded, assuming available despite timeout");
                    return true;
                }
                return false;
            } catch (ExecutionException e) {
                // Check if the error indicates missing models
                if (e.getCause() != null && e.getCause().getMessage() != null) {
                    String errorMsg = e.getCause().getMessage().toLowerCase();
                    if (errorMsg.contains("model") && (errorMsg.contains("download") || errorMsg.contains("not available"))) {
                        Log.d(TAG, "MLKit indicates models not downloaded: " + e.getCause().getMessage());
                        return false;
                    }
                }
                
                // For other execution exceptions, check our internal tracking
                // MLKit may fail for other reasons even when models are available
                if (downloadedModels.contains(sourceMLKit) && downloadedModels.contains(targetMLKit)) {
                    Log.d(TAG, "MLKit verification failed but models appear downloaded, assuming available: " + e.getMessage());
                    return true;
                }
                
                Log.d(TAG, "MLKit model verification failed: " + e.getMessage());
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error verifying model availability with MLKit", e);
            
            // As a fallback, check our internal tracking
            if (downloadedModels.contains(sourceMLKit) && downloadedModels.contains(targetMLKit)) {
                Log.d(TAG, "MLKit verification error but models appear downloaded, assuming available");
                return true;
            }
            return false;
        }
    }

    /**
     * Translates text using offline models.
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

        // Create translator
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceMLKit)
                .setTargetLanguage(targetMLKit)
                .build();

        Translator translator = Translation.getClient(options);

        // Perform translation
        translator.translate(text)
                .addOnSuccessListener(translatedText -> {
                    Log.d(TAG, "Offline translation successful: '" + text + "' -> '" + translatedText + "'");
                    
                    // Check if the translation is actually different from the original
                    // MLKit sometimes returns the original text when models aren't properly loaded
                    // But allow for legitimate cases where text might be similar (e.g., proper nouns)
                    if (translatedText != null && translatedText.trim().length() > 0) {
                        String originalTrimmed = text.trim().toLowerCase();
                        String translatedTrimmed = translatedText.trim().toLowerCase();
                        
                        // If text is exactly the same and longer than a few characters, it's suspicious
                        if (originalTrimmed.equals(translatedTrimmed) && originalTrimmed.length() > 3) {
                            Log.w(TAG, "Translation returned identical text for '" + text + "', likely model issue");
                            if (callback != null) {
                                callback.onTranslationComplete(false, null, "Translation returned original text - models may not be properly loaded");
                            }
                        } else {
                            // Translation looks valid (different or short text where identity is OK)
                            if (callback != null) {
                                callback.onTranslationComplete(true, translatedText, null);
                            }
                        }
                    } else {
                        Log.w(TAG, "Translation returned empty or null result");
                        if (callback != null) {
                            callback.onTranslationComplete(false, null, "Translation returned empty result");
                        }
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