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
 * 
 * @deprecated This service has been replaced by GeminiNanoTranslationService for better offline GenAI features.
 * Use GeminiNanoTranslationService instead for new implementations.
 * This class is maintained for backward compatibility and existing tests only.
 */
@Deprecated
public class OfflineTranslationService {
    private static final String TAG = "OfflineTranslationService";
    private static final int OPERATION_TIMEOUT_SECONDS = 30;
    
    // Use same preferences as OfflineModelManager for synchronization
    private static final String OFFLINE_MODELS_PREFS = "offline_models";
    private static final String KEY_DOWNLOADED_MODELS = "downloaded_models";

    private final Context context;
    private final UserPreferences userPreferences;
    // Internal model tracking - maintained for backward compatibility with existing API
    // TODO: Consider deprecating in favor of OfflineModelManager as single source of truth
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
     * Uses OfflineModelManager as the authoritative source for model availability.
     *
     * @param sourceLanguage The source language code
     * @param targetLanguage The target language code
     * @return true if offline translation is available, false otherwise
     */
    public boolean isOfflineTranslationAvailable(String sourceLanguage, String targetLanguage) {
        if (sourceLanguage == null || targetLanguage == null) {
            return false;
        }

        // Convert language codes to MLKit format to validate they're supported
        String sourceMLKit = LanguageCodeUtils.convertToMLKitLanguageCode(sourceLanguage);
        String targetMLKit = LanguageCodeUtils.convertToMLKitLanguageCode(targetLanguage);

        if (sourceMLKit == null || targetMLKit == null) {
            Log.d(TAG, "Unsupported language pair: " + sourceLanguage + " -> " + targetLanguage);
            return false;
        }

        // Use OfflineModelManager as the authoritative source for model availability
        boolean sourceAvailable = modelManager.isModelDownloadedAndVerified(sourceLanguage);
        boolean targetAvailable = modelManager.isModelDownloadedAndVerified(targetLanguage);
        
        if (sourceAvailable && targetAvailable) {
            Log.d(TAG, "Models verified by OfflineModelManager: " + sourceLanguage + " -> " + targetLanguage);
            return true;
        }

        Log.d(TAG, "Models not available for translation: " + sourceLanguage + " -> " + targetLanguage);
        return false;
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
        String sourceMLKit = LanguageCodeUtils.convertToMLKitLanguageCode(sourceLanguage);
        String targetMLKit = LanguageCodeUtils.convertToMLKitLanguageCode(targetLanguage);

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
     * Note: This method implements MLKit download logic directly.
     * Consider using OfflineModelManager.downloadModel() for new code.
     *
     * @param languageCode The language code
     * @param callback The callback to receive download progress and result
     */
    public void downloadLanguageModel(String languageCode, ModelDownloadCallback callback) {
        String mlkitLanguageCode = LanguageCodeUtils.convertToMLKitLanguageCode(languageCode);
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
     * Note: This method only closes translator and updates internal tracking.
     * It does not actually delete model files from disk.
     * Consider using OfflineModelManager.deleteModel() for full model removal.
     *
     * @param languageCode The language code
     */
    public void deleteLanguageModel(String languageCode) {
        String mlkitLanguageCode = LanguageCodeUtils.convertToMLKitLanguageCode(languageCode);
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
     * Note: This hardcoded list includes more languages than OfflineModelManager currently supports.
     * Consider using OfflineModelManager.getAvailableModels() for consistent model information.
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
     * Delegates to OfflineModelManager for authoritative model status.
     *
     * @return Set of downloaded language codes (standard format)
     */
    public Set<String> getDownloadedModels() {
        Set<String> downloadedModels = new HashSet<>();
        List<OfflineModelInfo> availableModels = modelManager.getAvailableModels();
        
        for (OfflineModelInfo model : availableModels) {
            if (model.isDownloaded()) {
                // Convert to MLKit format for backward compatibility
                String mlkitCode = LanguageCodeUtils.convertToMLKitLanguageCode(model.getLanguageCode());
                if (mlkitCode != null) {
                    downloadedModels.add(mlkitCode);
                }
            }
        }
        
        return downloadedModels;
    }

    /**
     * Checks if a specific language model is downloaded.
     * Delegates to OfflineModelManager for authoritative model status.
     *
     * @param languageCode The language code
     * @return true if the model is downloaded, false otherwise
     */
    public boolean isLanguageModelDownloaded(String languageCode) {
        return modelManager.isModelDownloaded(languageCode);
    }

    /**
     * Checks if any offline models are downloaded.
     * Delegates to OfflineModelManager for authoritative model status.
     *
     * @return true if at least one language model is downloaded, false otherwise
     */
    public boolean hasAnyDownloadedModels() {
        List<OfflineModelInfo> availableModels = modelManager.getAvailableModels();
        return availableModels.stream().anyMatch(OfflineModelInfo::isDownloaded);
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
     * Loads the list of downloaded models from preferences.
     * Now uses the same SharedPreferences as OfflineModelManager for synchronization.
     */
    private void loadDownloadedModels() {
        try {
            // Use same SharedPreferences as OfflineModelManager
            SharedPreferences modelPrefs = context.getSharedPreferences(OFFLINE_MODELS_PREFS, Context.MODE_PRIVATE);
            Set<String> rawDownloadedModels = modelPrefs.getStringSet(KEY_DOWNLOADED_MODELS, new HashSet<>());
            
            // Convert raw language codes to MLKit format for internal use
            downloadedModels.clear();
            for (String rawCode : rawDownloadedModels) {
                String mlkitCode = LanguageCodeUtils.convertToMLKitLanguageCode(rawCode);
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
                String rawCode = LanguageCodeUtils.convertFromMLKitLanguageCode(mlkitCode);
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
            // Wait a moment before retry
            Thread.sleep(500);
            
            // Create a new translator instance
            TranslatorOptions retryOptions = new TranslatorOptions.Builder()
                    .setSourceLanguage(sourceMLKit)
                    .setTargetLanguage(targetMLKit)
                    .build();
            
            Translator retryTranslator = Translation.getClient(retryOptions);
            
            // Try translation again
            retryTranslator.translate(text)
                    .addOnSuccessListener(translatedText -> {
                        Log.d(TAG, "Dictionary error recovery successful");
                        if (callback != null) {
                            callback.onTranslationComplete(true, translatedText, null);
                        }
                    })
                    .addOnFailureListener(retryException -> {
                        Log.e(TAG, "Dictionary error recovery failed", retryException);
                        String enhancedErrorMessage = enhanceErrorMessage(retryException.getMessage());
                        if (callback != null) {
                            callback.onTranslationComplete(false, null, enhancedErrorMessage);
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