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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Manages offline translation models including downloading and deletion.
 */
public class OfflineModelManager {
    private static final String TAG = "OfflineModelManager";
    private static final String PREFS_NAME = "offline_models";
    private static final String KEY_DOWNLOADED_MODELS = "downloaded_models";
    
    private Context context;
    private SharedPreferences preferences;
    
    public interface DownloadListener {
        void onProgress(int progress);
        void onSuccess();
        void onError(String error);
    }
    
    /**
     * Represents the status of an offline model.
     */
    public static class ModelStatus {
        public static final String DOWNLOADED = "downloaded";
        public static final String NOT_DOWNLOADED = "not_downloaded";
        public static final String DOWNLOADING = "downloading";
        public static final String ERROR = "error";
        
        private String status;
        private boolean verified;
        private String errorMessage;
        
        public ModelStatus(String status, boolean verified) {
            this.status = status;
            this.verified = verified;
            this.errorMessage = null;
        }
        
        public ModelStatus(String status, boolean verified, String errorMessage) {
            this.status = status;
            this.verified = verified;
            this.errorMessage = errorMessage;
        }
        
        public String getStatus() {
            return status;
        }
        
        public boolean isVerified() {
            return verified;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public boolean isDownloaded() {
            return DOWNLOADED.equals(status);
        }
        
        public boolean isDownloading() {
            return DOWNLOADING.equals(status);
        }
    }
    
    public OfflineModelManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Get list of all available offline models with updated status.
     * This method combines internal tracking with ML Kit verification for accuracy.
     */
    public List<OfflineModelInfo> getAvailableModels() {
        List<OfflineModelInfo> models = new ArrayList<>();
        Set<String> downloadedModels = getDownloadedModelCodes();
        
        // Add models for ML Kit supported languages with realistic file sizes
        // Core languages
        addModel(models, "en", "English", 25 * 1024 * 1024, downloadedModels, true);
        addModel(models, "es", "Spanish", 28 * 1024 * 1024, downloadedModels, true);
        addModel(models, "fr", "French", 27 * 1024 * 1024, downloadedModels, true);
        addModel(models, "de", "German", 30 * 1024 * 1024, downloadedModels, true);
        addModel(models, "it", "Italian", 26 * 1024 * 1024, downloadedModels, true);
        addModel(models, "pt", "Portuguese", 27 * 1024 * 1024, downloadedModels, true);
        addModel(models, "ru", "Russian", 32 * 1024 * 1024, downloadedModels, true);
        
        // Asian languages
        addModel(models, "ja", "Japanese", 35 * 1024 * 1024, downloadedModels, true);
        addModel(models, "ko", "Korean", 33 * 1024 * 1024, downloadedModels, true);
        addModel(models, "zh", "Chinese (Simplified)", 38 * 1024 * 1024, downloadedModels, true);
        addModel(models, "th", "Thai", 31 * 1024 * 1024, downloadedModels, true);
        addModel(models, "vi", "Vietnamese", 28 * 1024 * 1024, downloadedModels, true);
        addModel(models, "id", "Indonesian", 25 * 1024 * 1024, downloadedModels, true);
        
        // Middle Eastern and Indian languages
        addModel(models, "ar", "Arabic", 30 * 1024 * 1024, downloadedModels, true);
        addModel(models, "hi", "Hindi", 29 * 1024 * 1024, downloadedModels, true);
        addModel(models, "he", "Hebrew", 26 * 1024 * 1024, downloadedModels, true);
        addModel(models, "fa", "Persian", 28 * 1024 * 1024, downloadedModels, true);
        addModel(models, "ur", "Urdu", 27 * 1024 * 1024, downloadedModels, true);
        
        // European languages
        addModel(models, "nl", "Dutch", 25 * 1024 * 1024, downloadedModels, true);
        addModel(models, "sv", "Swedish", 24 * 1024 * 1024, downloadedModels, true);
        addModel(models, "fi", "Finnish", 26 * 1024 * 1024, downloadedModels, true);
        addModel(models, "da", "Danish", 23 * 1024 * 1024, downloadedModels, true);
        addModel(models, "no", "Norwegian", 24 * 1024 * 1024, downloadedModels, true);
        addModel(models, "pl", "Polish", 28 * 1024 * 1024, downloadedModels, true);
        addModel(models, "tr", "Turkish", 27 * 1024 * 1024, downloadedModels, true);
        addModel(models, "el", "Greek", 26 * 1024 * 1024, downloadedModels, true);
        addModel(models, "cs", "Czech", 25 * 1024 * 1024, downloadedModels, true);
        addModel(models, "hu", "Hungarian", 26 * 1024 * 1024, downloadedModels, true);
        addModel(models, "ro", "Romanian", 25 * 1024 * 1024, downloadedModels, true);
        addModel(models, "bg", "Bulgarian", 25 * 1024 * 1024, downloadedModels, true);
        addModel(models, "hr", "Croatian", 24 * 1024 * 1024, downloadedModels, true);
        addModel(models, "sk", "Slovak", 24 * 1024 * 1024, downloadedModels, true);
        addModel(models, "sl", "Slovenian", 23 * 1024 * 1024, downloadedModels, true);
        addModel(models, "et", "Estonian", 22 * 1024 * 1024, downloadedModels, true);
        addModel(models, "lv", "Latvian", 22 * 1024 * 1024, downloadedModels, true);
        addModel(models, "lt", "Lithuanian", 23 * 1024 * 1024, downloadedModels, true);
        addModel(models, "uk", "Ukrainian", 27 * 1024 * 1024, downloadedModels, true);
        
        return models;
    }
    
    private void addModel(List<OfflineModelInfo> models, String code, String name, long size, Set<String> downloadedModels, boolean checkMLKit) {
        OfflineModelInfo model = new OfflineModelInfo(code, name, size);
        
        boolean isTrackedAsDownloaded = downloadedModels.contains(code);
        boolean isActuallyAvailable = false;
        
        if (checkMLKit) {
            // Verify with ML Kit for more accurate status
            isActuallyAvailable = isModelAvailableInMLKit(code);
            
            // Sync internal tracking if there's a mismatch
            if (isActuallyAvailable && !isTrackedAsDownloaded) {
                // Model is available in ML Kit but not tracked - update tracking
                saveDownloadedModel(code);
                Log.d(TAG, "Synced tracking for available model: " + code);
            } else if (!isActuallyAvailable && isTrackedAsDownloaded) {
                // Model is tracked but not actually available - update tracking
                removeDownloadedModel(code);
                Log.d(TAG, "Corrected tracking for unavailable model: " + code);
            }
        }
        
        // Use ML Kit status if available, otherwise fall back to internal tracking
        boolean isDownloaded = checkMLKit ? isActuallyAvailable : isTrackedAsDownloaded;
        model.setDownloaded(isDownloaded);
        models.add(model);
    }
    
    /**
     * Download an offline model using ML Kit's actual download API.
     */
    public void downloadModel(OfflineModelInfo model, DownloadListener listener) {
        if (model.isDownloaded()) {
            if (listener != null) {
                listener.onError("Model already downloaded");
            }
            return;
        }
        
        String languageCode = model.getLanguageCode();
        String mlKitCode = convertToMLKitLanguageCode(languageCode);
        
        if (mlKitCode == null) {
            if (listener != null) {
                listener.onError("Unsupported language: " + languageCode);
            }
            return;
        }
        
        model.setDownloading(true);
        
        // Use ML Kit's actual download API in a background thread
        new Thread(() -> {
            try {
                Log.d(TAG, "Starting actual ML Kit download for model: " + languageCode);
                
                // Create translator with English as target (commonly available)
                TranslatorOptions options = new TranslatorOptions.Builder()
                        .setSourceLanguage(mlKitCode)
                        .setTargetLanguage(TranslateLanguage.ENGLISH)
                        .build();

                Translator translator = Translation.getClient(options);
                
                try {
                    // Report initial progress
                    if (listener != null) {
                        listener.onProgress(10);
                    }
                    
                    // Use ML Kit's downloadModelIfNeeded API
                    Task<Void> downloadTask = translator.downloadModelIfNeeded();
                    
                    // Report progress during download
                    if (listener != null) {
                        listener.onProgress(50);
                    }
                    
                    // Wait for download to complete with timeout
                    Tasks.await(downloadTask, 60, TimeUnit.SECONDS);
                    
                    // Report completion progress
                    if (listener != null) {
                        listener.onProgress(90);
                    }
                    
                    // Mark as downloaded and save to preferences
                    model.setDownloading(false);
                    model.setDownloaded(true);
                    saveDownloadedModelPrivate(languageCode);
                    
                    // Report final progress
                    if (listener != null) {
                        listener.onProgress(100);
                    }
                    
                    Log.d(TAG, "ML Kit model download completed for: " + languageCode);
                    
                    if (listener != null) {
                        listener.onSuccess();
                    }
                    
                } catch (TimeoutException e) {
                    Log.e(TAG, "Download timeout for model: " + languageCode, e);
                    model.setDownloading(false);
                    if (listener != null) {
                        listener.onError("Download timeout. Please check your internet connection.");
                    }
                } catch (ExecutionException e) {
                    Log.e(TAG, "Download execution failed for model: " + languageCode, e);
                    model.setDownloading(false);
                    String errorMsg = "Download failed";
                    if (e.getCause() != null && e.getCause().getMessage() != null) {
                        errorMsg = "Download failed: " + e.getCause().getMessage();
                    }
                    if (listener != null) {
                        listener.onError(errorMsg);
                    }
                } finally {
                    // Clean up translator resource
                    try {
                        translator.close();
                    } catch (Exception e) {
                        Log.w(TAG, "Error closing translator during cleanup", e);
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during model download: " + languageCode, e);
                model.setDownloading(false);
                if (listener != null) {
                    listener.onError("Download failed: " + e.getMessage());
                }
            }
        }).start();
    }
    
    /**
     * Delete an offline model using ML Kit's proper model management.
     */
    public boolean deleteModel(OfflineModelInfo model) {
        try {
            if (!model.isDownloaded()) {
                Log.d(TAG, "Model not downloaded, nothing to delete: " + model.getLanguageCode());
                return false;
            }
            
            String languageCode = model.getLanguageCode();
            String mlKitCode = convertToMLKitLanguageCode(languageCode);
            
            if (mlKitCode == null) {
                Log.w(TAG, "Cannot delete unsupported language model: " + languageCode);
                return false;
            }
            
            // Create translator to get access to model management
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(mlKitCode)
                    .setTargetLanguage(TranslateLanguage.ENGLISH)
                    .build();

            Translator translator = Translation.getClient(options);
            
            try {
                // Use ML Kit's model deletion API
                Task<Void> deleteTask = translator.deleteDownloadedModel();
                
                // Wait for deletion to complete
                Tasks.await(deleteTask, 30, TimeUnit.SECONDS);
                
                // Remove from preferences
                removeDownloadedModel(languageCode);
                
                // Clean up local tracking file if it exists
                deleteModelFile(model);
                
                // Update model state
                model.setDownloaded(false);
                
                Log.d(TAG, "Model deleted successfully: " + languageCode);
                return true;
                
            } catch (Exception e) {
                Log.e(TAG, "Error deleting ML Kit model for " + languageCode, e);
                return false;
            } finally {
                // Clean up translator
                try {
                    translator.close();
                } catch (Exception e) {
                    Log.w(TAG, "Error closing translator during model deletion", e);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting model", e);
            return false;
        }
    }
    
    /**
     * Check if a model is downloaded.
     */
    public boolean isModelDownloaded(String languageCode) {
        Set<String> downloadedModels = getDownloadedModelCodes();
        return downloadedModels.contains(languageCode);
    }
    
    /**
     * Check if a model is downloaded and verified using ML Kit's approach.
     * This extends the basic download check with ML Kit verification.
     */
    public boolean isModelDownloadedAndVerified(String languageCode) {
        if (!isModelDownloaded(languageCode)) {
            return false;
        }
        
        // Verify with ML Kit that the model is actually available
        return isModelAvailableInMLKit(languageCode);
    }
    
    /**
     * Get the status map for all available models.
     * @return Map of language code to ModelStatus
     */
    public Map<String, ModelStatus> getModelStatusMap() {
        Map<String, ModelStatus> statusMap = new HashMap<>();
        List<OfflineModelInfo> availableModels = getAvailableModels();
        
        for (OfflineModelInfo model : availableModels) {
            String languageCode = model.getLanguageCode();
            ModelStatus status;
            
            if (model.isDownloading()) {
                status = new ModelStatus(ModelStatus.DOWNLOADING, false);
            } else if (model.isDownloaded()) {
                boolean verified = isModelDownloadedAndVerified(languageCode);
                status = new ModelStatus(ModelStatus.DOWNLOADED, verified);
            } else {
                status = new ModelStatus(ModelStatus.NOT_DOWNLOADED, false);
            }
            
            statusMap.put(languageCode, status);
        }
        
        return statusMap;
    }
    
    /**
     * Get offline model directory for legacy tracking files.
     * @deprecated This is only used for cleanup of old placeholder files
     */
    private File getModelDirectory() {
        File modelDir = new File(context.getFilesDir(), "offline_models");
        if (!modelDir.exists()) {
            modelDir.mkdirs();
        }
        return modelDir;
    }
    
    /**
     * Delete legacy placeholder model file if it exists.
     * @deprecated This is only used for cleanup of old placeholder files
     */
    private void deleteModelFile(OfflineModelInfo model) {
        try {
            File modelDir = getModelDirectory();
            File modelFile = new File(modelDir, model.getLanguageCode() + ".model");
            
            if (modelFile.exists() && modelFile.delete()) {
                Log.d(TAG, "Legacy model file deleted: " + modelFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.d(TAG, "No legacy model file to delete for " + model.getLanguageCode());
        }
    }
    
    /**
     * Get set of downloaded model codes from preferences.
     */
    private Set<String> getDownloadedModelCodes() {
        return preferences.getStringSet(KEY_DOWNLOADED_MODELS, new HashSet<>());
    }
    
    /**
     * Save a downloaded model to preferences.
     */
    private void saveDownloadedModelPrivate(String languageCode) {
        Set<String> downloadedModels = new HashSet<>(getDownloadedModelCodes());
        downloadedModels.add(languageCode);
        preferences.edit().putStringSet(KEY_DOWNLOADED_MODELS, downloadedModels).apply();
    }
    
    /**
     * Save a downloaded model to preferences (public method for external access).
     */
    public void saveDownloadedModel(String languageCode) {
        saveDownloadedModelPrivate(languageCode);
    }
    
    /**
     * Remove a downloaded model from preferences.
     */
    public void removeDownloadedModel(String languageCode) {
        Set<String> downloadedModels = new HashSet<>(getDownloadedModelCodes());
        downloadedModels.remove(languageCode);
        preferences.edit().putStringSet(KEY_DOWNLOADED_MODELS, downloadedModels).apply();
    }
    
    /**
     * Checks if a language model is actually available in ML Kit.
     * Uses ML Kit's model checking approach for better accuracy.
     * 
     * @param languageCode The language code to check
     * @return true if the model is available in ML Kit, false otherwise
     */
    private boolean isModelAvailableInMLKit(String languageCode) {
        try {
            String mlkitCode = convertToMLKitLanguageCode(languageCode);
            if (mlkitCode == null) {
                Log.d(TAG, "Language code not supported by ML Kit: " + languageCode);
                return false;
            }
            
            // Create translator options with the language and English
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(mlkitCode)
                    .setTargetLanguage(TranslateLanguage.ENGLISH)
                    .build();

            Translator translator = Translation.getClient(options);
            
            try {
                // Test if model is available by attempting a very short translation with timeout
                // This is the recommended approach for checking model availability
                Task<String> testTask = translator.translate("test");
                
                // Use a short timeout - if models are downloaded, this should complete quickly
                Tasks.await(testTask, 3, TimeUnit.SECONDS);
                
                // If we get here without exception, models are available
                Log.d(TAG, "Model availability confirmed for " + languageCode);
                return true;
                
            } catch (TimeoutException e) {
                // Timeout typically means models need to be downloaded
                Log.d(TAG, "Model not available (timeout) for " + languageCode);
                return false;
            } catch (ExecutionException e) {
                // Check for model-related errors
                if (e.getCause() != null) {
                    String errorMsg = e.getCause().getMessage();
                    if (errorMsg != null) {
                        String lowerMsg = errorMsg.toLowerCase();
                        if (lowerMsg.contains("model") && (lowerMsg.contains("download") || lowerMsg.contains("not available"))) {
                            Log.d(TAG, "Model not available (needs download) for " + languageCode);
                            return false;
                        }
                    }
                }
                Log.d(TAG, "Model availability check failed for " + languageCode + ": " + e.getMessage());
                return false;
            } finally {
                // Clean up translator
                try {
                    translator.close();
                } catch (Exception e) {
                    Log.w(TAG, "Error closing translator during availability check", e);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking ML Kit model availability for " + languageCode, e);
            return false;
        }
    }
    
    /**
     * Converts a standard language code to ML Kit format.
     * Based on the mapping used in OfflineTranslationService with improvements.
     */
    private String convertToMLKitLanguageCode(String languageCode) {
        if (languageCode == null) return null;
        
        // Handle common mappings - be consistent with ML Kit supported languages
        switch (languageCode.toLowerCase()) {
            // Chinese variants - ML Kit only supports simplified Chinese
            case "zh-cn": 
            case "zh": 
                return TranslateLanguage.CHINESE;
            case "zh-tw": 
                // Traditional Chinese is not supported by ML Kit, log warning
                Log.w(TAG, "Traditional Chinese (zh-TW) not supported by ML Kit, using Simplified Chinese");
                return TranslateLanguage.CHINESE;
                
            // European languages
            case "en": return TranslateLanguage.ENGLISH;
            case "es": return TranslateLanguage.SPANISH;
            case "fr": return TranslateLanguage.FRENCH;
            case "de": return TranslateLanguage.GERMAN;
            case "it": return TranslateLanguage.ITALIAN;
            case "pt": return TranslateLanguage.PORTUGUESE;
            case "ru": return TranslateLanguage.RUSSIAN;
            case "nl": return TranslateLanguage.DUTCH;
            case "sv": return TranslateLanguage.SWEDISH;
            case "fi": return TranslateLanguage.FINNISH;
            case "da": return TranslateLanguage.DANISH;
            case "no": return TranslateLanguage.NORWEGIAN;
            case "pl": return TranslateLanguage.POLISH;
            case "tr": return TranslateLanguage.TURKISH;
            case "el": return TranslateLanguage.GREEK;
            
            // Asian languages
            case "ja": return TranslateLanguage.JAPANESE;
            case "ko": return TranslateLanguage.KOREAN;
            case "th": return TranslateLanguage.THAI;
            case "vi": return TranslateLanguage.VIETNAMESE;
            case "id": return TranslateLanguage.INDONESIAN;
            case "ms": return TranslateLanguage.MALAY;
            
            // Middle Eastern and Indian languages
            case "ar": return TranslateLanguage.ARABIC;
            case "hi": return TranslateLanguage.HINDI;
            case "he": return TranslateLanguage.HEBREW;
            case "fa": return TranslateLanguage.PERSIAN;
            case "ur": return TranslateLanguage.URDU;
            case "bn": return TranslateLanguage.BENGALI;
            case "gu": return TranslateLanguage.GUJARATI;
            case "kn": return TranslateLanguage.KANNADA;
            case "ml": return TranslateLanguage.MALAYALAM;
            case "mr": return TranslateLanguage.MARATHI;
            case "pa": return TranslateLanguage.PUNJABI;
            case "ta": return TranslateLanguage.TAMIL;
            case "te": return TranslateLanguage.TELUGU;
            
            // Eastern European languages
            case "bg": return TranslateLanguage.BULGARIAN;
            case "hr": return TranslateLanguage.CROATIAN;
            case "cs": return TranslateLanguage.CZECH;
            case "et": return TranslateLanguage.ESTONIAN;
            case "lv": return TranslateLanguage.LATVIAN;
            case "lt": return TranslateLanguage.LITHUANIAN;
            case "hu": return TranslateLanguage.HUNGARIAN;
            case "ro": return TranslateLanguage.ROMANIAN;
            case "sk": return TranslateLanguage.SLOVAK;
            case "sl": return TranslateLanguage.SLOVENIAN;
            case "uk": return TranslateLanguage.UKRAINIAN;
            
            // African languages
            case "af": return TranslateLanguage.AFRIKAANS;
            case "sw": return TranslateLanguage.SWAHILI;
            
            // Other languages
            case "eu": return TranslateLanguage.BASQUE;
            case "be": return TranslateLanguage.BELARUSIAN;
            case "ca": return TranslateLanguage.CATALAN;
            case "eo": return TranslateLanguage.ESPERANTO;
            case "gl": return TranslateLanguage.GALICIAN;
            case "ga": return TranslateLanguage.IRISH;
            case "is": return TranslateLanguage.ICELANDIC;
            case "mt": return TranslateLanguage.MALTESE;
            case "cy": return TranslateLanguage.WELSH;
            
            default: 
                Log.w(TAG, "Language code not supported by ML Kit: " + languageCode);
                return null;
        }
    }
    
    /**
     * Validates if a language code is supported by ML Kit.
     * @param languageCode The language code to validate
     * @return true if supported, false otherwise
     */
    public boolean isLanguageSupported(String languageCode) {
        return convertToMLKitLanguageCode(languageCode) != null;
    }
    
    /**
     * Gets a list of all supported language codes.
     * @return Set of supported language codes
     */
    public Set<String> getSupportedLanguageCodes() {
        Set<String> supported = new HashSet<>();
        // Add all languages from our model list that are actually supported
        List<OfflineModelInfo> models = getAvailableModels();
        for (OfflineModelInfo model : models) {
            if (isLanguageSupported(model.getLanguageCode())) {
                supported.add(model.getLanguageCode());
            }
        }
        return supported;
    }
}