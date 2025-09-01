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
import java.util.Map;
import java.util.HashMap;
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
    private OfflineModelManager modelManager;

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
        this.modelManager = new OfflineModelManager(context);
        
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

        // First, check directly with MLKit as this is the most reliable method
        // This ensures we're checking actual ML Kit model availability including dictionary loading
        boolean mlkitAvailable = verifyModelAvailabilityWithMLKit(sourceMLKit, targetMLKit);
        if (mlkitAvailable) {
            // Update our internal tracking to sync with MLKit
            if (!downloadedModels.contains(sourceMLKit) || !downloadedModels.contains(targetMLKit)) {
                downloadedModels.add(sourceMLKit);
                downloadedModels.add(targetMLKit);
                saveDownloadedModels();
                Log.d(TAG, "Synced internal tracking with MLKit - models were available but not tracked");
            }
            return true;
        }

        // If MLKit verification fails, also check our internal tracking as a fallback
        // This helps detect cases where MLKit is temporarily having issues but models exist
        boolean internalTracking = downloadedModels.contains(sourceMLKit) && downloadedModels.contains(targetMLKit);
        
        if (internalTracking) {
            Log.d(TAG, "Internal tracking indicates models available, but MLKit verification failed - may indicate dictionary loading issues");
            // Don't return true here since MLKit verification should be authoritative for dictionary loading
            return false;
        }
        
        // Convert back to standard language codes for OfflineModelManager as final fallback
        String sourceStandard = convertFromMLKitLanguageCode(sourceMLKit);
        String targetStandard = convertFromMLKitLanguageCode(targetMLKit);

        // Use OfflineModelManager for verification as final check
        boolean sourceVerified = modelManager.isModelDownloadedAndVerified(sourceStandard);
        boolean targetVerified = modelManager.isModelDownloadedAndVerified(targetStandard);
        
        if (sourceVerified && targetVerified) {
            Log.d(TAG, "OfflineModelManager indicates models available, but MLKit verification failed - may indicate model synchronization issues");
            // Don't return true here either since we need actual ML Kit functionality
            return false;
        }
        
        return false;
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
            // Use longer timeout to allow for dictionary initialization
            try {
                Task<String> translateTask = translator.translate("test");
                
                // Wait longer to allow dictionary files to initialize properly
                String result = Tasks.await(translateTask, 10, TimeUnit.SECONDS);
                
                // If we got a result, models and dictionaries are working
                Log.d(TAG, "MLKit models verified available with dictionary loading: " + sourceMLKit + " -> " + targetMLKit);
                return true;
                
            } catch (TimeoutException e) {
                // Timeout likely means models need to be downloaded or dictionaries are not loading
                Log.d(TAG, "MLKit model verification timeout - models may not be downloaded or dictionaries not ready: " + sourceMLKit + " -> " + targetMLKit);
                return false;
            } catch (ExecutionException e) {
                // Check if the error indicates missing models or dictionary loading issues
                if (e.getCause() != null && e.getCause().getMessage() != null) {
                    String errorMsg = e.getCause().getMessage().toLowerCase();
                    if (errorMsg.contains("model") && errorMsg.contains("download")) {
                        Log.d(TAG, "MLKit indicates models not downloaded: " + e.getCause().getMessage());
                        return false;
                    } else if (errorMsg.contains("dictionary") || errorMsg.contains("dict")) {
                        Log.w(TAG, "MLKit dictionary loading failure detected, attempting recovery: " + e.getCause().getMessage());
                        // Try to recover from dictionary loading failure with enhanced retry
                        return retryTranslationWithDictionaryFix(translator, sourceMLKit, targetMLKit);
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
     * Attempts to recover from dictionary loading failures by retrying translation.
     * This method handles cases where models are present but dictionaries fail to load.
     *
     * @param translator The translator instance to retry with
     * @param sourceMLKit The source language code in MLKit format
     * @param targetMLKit The target language code in MLKit format
     * @return true if recovery was successful, false otherwise
     */
    private boolean retryTranslationWithDictionaryFix(Translator translator, String sourceMLKit, String targetMLKit) {
        Log.d(TAG, "Attempting dictionary loading recovery for: " + sourceMLKit + " -> " + targetMLKit);
        
        try {
            // Close and recreate translator to force reinitialization
            translator.close();
            
            // Wait longer for cleanup and dictionary initialization
            Thread.sleep(2000);
            
            // Recreate translator options and client
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(sourceMLKit)
                    .setTargetLanguage(targetMLKit)
                    .build();
            
            Translator newTranslator = Translation.getClient(options);
            
            // Try translation again with a much longer timeout for dictionary loading
            Task<String> retryTask = newTranslator.translate("test");
            String result = Tasks.await(retryTask, 15, TimeUnit.SECONDS);
            
            Log.d(TAG, "Dictionary loading recovery successful: " + sourceMLKit + " -> " + targetMLKit);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Dictionary loading recovery failed: " + sourceMLKit + " -> " + targetMLKit, e);
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
                    Log.d(TAG, "Offline translation successful");
                    if (callback != null) {
                        callback.onTranslationComplete(true, translatedText, null);
                    }
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Offline translation failed", exception);
                    
                    // Check if this is a dictionary loading error and attempt recovery
                    String errorMessage = exception.getMessage();
                    if (errorMessage != null && (errorMessage.toLowerCase().contains("dictionary") || 
                                               errorMessage.toLowerCase().contains("dict"))) {
                        Log.w(TAG, "Dictionary loading error detected, attempting recovery");
                        
                        // Attempt recovery by retrying with a new translator instance
                        retryTranslationAfterDictionaryError(text, sourceMLKit, targetMLKit, callback);
                    } else {
                        // For other errors, return immediately with specific error message
                        String enhancedErrorMessage = enhanceErrorMessage(errorMessage);
                        if (callback != null) {
                            callback.onTranslationComplete(false, null, enhancedErrorMessage);
                        }
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
        // Use the target language as both source and target to download the model
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(mlkitLanguageCode)
                .setTargetLanguage(mlkitLanguageCode)
                .build();

        Translator translator = Translation.getClient(options);

        // Download the model
        translator.downloadModelIfNeeded()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Language model downloaded successfully: " + mlkitLanguageCode);
                    
                    // Verify the download actually worked by testing translation
                    verifyModelDownloadSuccess(mlkitLanguageCode, languageCode, callback);
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Failed to download language model: " + mlkitLanguageCode, exception);
                    String enhancedErrorMessage = enhanceErrorMessage(exception.getMessage());
                    if (callback != null) {
                        callback.onDownloadComplete(false, languageCode, enhancedErrorMessage);
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
                .setSourceLanguage(mlkitLanguageCode)
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
     * Gets detailed status information for all available models.
     * This method uses OfflineModelManager to get comprehensive status data.
     *
     * @return Map of language codes to their detailed status information
     */
    public Map<String, String> getDetailedModelStatus() {
        Map<String, String> detailedStatus = new HashMap<>();
        
        try {
            // Get status from OfflineModelManager (this addresses the build error)
            Map<String, OfflineModelManager.ModelStatus> managerStatus = modelManager.getModelStatusMap();
            
            for (Map.Entry<String, OfflineModelManager.ModelStatus> entry : managerStatus.entrySet()) {
                String languageCode = entry.getKey();
                OfflineModelManager.ModelStatus managerStat = entry.getValue();
                
                if (managerStat != null) {
                    String statusText = managerStat.getStatus();
                    if (managerStat.isVerified()) {
                        statusText += " (verified)";
                    }
                    if (managerStat.getErrorMessage() != null) {
                        statusText += " - " + managerStat.getErrorMessage();
                    }
                    detailedStatus.put(languageCode, statusText);
                } else {
                    detailedStatus.put(languageCode, "unknown");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting detailed model status", e);
        }
        
        return detailedStatus;
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
     * Verifies that a downloaded model actually works by testing a simple translation.
     * This helps detect cases where models download but dictionaries fail to load.
     *
     * @param mlkitLanguageCode The MLKit language code that was downloaded
     * @param originalLanguageCode The original language code for the callback
     * @param callback The callback to receive the final download result
     */
    private void verifyModelDownloadSuccess(String mlkitLanguageCode, String originalLanguageCode, 
                                          ModelDownloadCallback callback) {
        Log.d(TAG, "Verifying model download success for: " + mlkitLanguageCode);
        
        try {
            // Create translator for verification
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(mlkitLanguageCode)
                    .setTargetLanguage(mlkitLanguageCode)
                    .build();
            
            Translator verifyTranslator = Translation.getClient(options);
            
            // Test with a simple translation to verify dictionaries load properly
            verifyTranslator.translate("test")
                    .addOnSuccessListener(result -> {
                        Log.d(TAG, "Model verification successful, dictionaries loaded properly: " + mlkitLanguageCode);
                        
                        // Only mark as downloaded if verification succeeds
                        downloadedModels.add(mlkitLanguageCode);
                        saveDownloadedModels();
                        
                        // Refresh the model list to ensure synchronization
                        loadDownloadedModels();
                        
                        if (callback != null) {
                            callback.onDownloadComplete(true, originalLanguageCode, null);
                        }
                    })
                    .addOnFailureListener(verifyException -> {
                        Log.e(TAG, "Model verification failed, dictionaries may not have loaded properly: " + mlkitLanguageCode, verifyException);
                        
                        String errorMessage = verifyException.getMessage();
                        if (errorMessage != null && (errorMessage.toLowerCase().contains("dictionary") || 
                                                   errorMessage.toLowerCase().contains("dict"))) {
                            Log.w(TAG, "Dictionary loading failure detected during model verification");
                            errorMessage = "Model downloaded but dictionary files failed to load. Please try downloading again or check available storage space.";
                        } else {
                            errorMessage = enhanceErrorMessage(errorMessage);
                        }
                        
                        if (callback != null) {
                            callback.onDownloadComplete(false, originalLanguageCode, errorMessage);
                        }
                    });
                    
        } catch (Exception e) {
            Log.e(TAG, "Error during model download verification", e);
            if (callback != null) {
                callback.onDownloadComplete(false, originalLanguageCode, 
                    "Model download verification failed: " + e.getMessage());
            }
        }
    }

    /**
     * Attempts to retry translation after a dictionary loading error.
     * This method creates a new translator instance and retries the translation.
     *
     * @param text The text to translate
     * @param sourceMLKit The source language code in MLKit format
     * @param targetMLKit The target language code in MLKit format
     * @param callback The callback to receive the result
     */
    private void retryTranslationAfterDictionaryError(String text, String sourceMLKit, String targetMLKit, 
                                                    OfflineTranslationCallback callback) {
        Log.d(TAG, "Retrying translation after dictionary error: " + sourceMLKit + " -> " + targetMLKit);
        
        try {
            // Wait longer before retry to allow dictionary initialization
            Thread.sleep(2000);
            
            // Create a new translator instance to force reinitialization
            TranslatorOptions retryOptions = new TranslatorOptions.Builder()
                    .setSourceLanguage(sourceMLKit)
                    .setTargetLanguage(targetMLKit)
                    .build();
            
            Translator retryTranslator = Translation.getClient(retryOptions);
            
            // Try translation again with enhanced error handling
            retryTranslator.translate(text)
                    .addOnSuccessListener(translatedText -> {
                        Log.d(TAG, "Dictionary error recovery successful");
                        if (callback != null) {
                            callback.onTranslationComplete(true, translatedText, null);
                        }
                    })
                    .addOnFailureListener(retryException -> {
                        Log.e(TAG, "Dictionary error recovery failed", retryException);
                        
                        // Check if this is still a dictionary error - if so, suggest redownload
                        String retryErrorMessage = retryException.getMessage();
                        if (retryErrorMessage != null && (retryErrorMessage.toLowerCase().contains("dictionary") || 
                                                         retryErrorMessage.toLowerCase().contains("dict"))) {
                            Log.w(TAG, "Persistent dictionary loading failure detected");
                            retryErrorMessage = "Dictionary files are corrupted or missing. Please delete and redownload the language models.";
                        } else {
                            retryErrorMessage = enhanceErrorMessage(retryErrorMessage);
                        }
                        
                        if (callback != null) {
                            callback.onTranslationComplete(false, null, retryErrorMessage);
                        }
                    });
                    
        } catch (Exception e) {
            Log.e(TAG, "Error during dictionary recovery retry", e);
            if (callback != null) {
                callback.onTranslationComplete(false, null, 
                    "Dictionary loading failed and recovery attempt unsuccessful. Please try redownloading the language models.");
            }
        }
    }

    /**
     * Enhances error messages to provide more user-friendly information.
     *
     * @param originalMessage The original error message
     * @return Enhanced error message with user-friendly information
     */
    private String enhanceErrorMessage(String originalMessage) {
        if (originalMessage == null || originalMessage.trim().isEmpty()) {
            return "Translation failed due to an unknown error";
        }
        
        String lowerMessage = originalMessage.toLowerCase();
        
        // Dictionary loading errors
        if (lowerMessage.contains("dictionary") || lowerMessage.contains("dict")) {
            return "Dictionary files failed to load. Please try redownloading the language models or check available storage space.";
        }
        
        // Model not found errors
        if (lowerMessage.contains("model") && (lowerMessage.contains("not found") || lowerMessage.contains("missing"))) {
            return "Language model not found. Please download the required language models for offline translation.";
        }
        
        // Generic model errors
        if (lowerMessage.contains("model")) {
            return "Language model error. Please try redownloading the language models.";
        }
        
        // Network/download errors
        if (lowerMessage.contains("network") || lowerMessage.contains("download")) {
            return "Network error during model download. Please check your internet connection and try again.";
        }
        
        // Storage errors
        if (lowerMessage.contains("storage") || lowerMessage.contains("space")) {
            return "Insufficient storage space for language models. Please free up some space and try again.";
        }
        
        // Return original message if no specific pattern matched
        return "Translation failed: " + originalMessage;
    }

    /**
     * Cleanup resources.
     */

    public void cleanup() {
        // Clean up any resources if needed
        Log.d(TAG, "OfflineTranslationService cleanup complete");
    }
}