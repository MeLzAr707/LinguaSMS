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
import java.util.concurrent.TimeUnit;

/**
 * Manages offline translation models including downloading and deletion.
 * 
 * @deprecated This manager has been replaced by GeminiNanoModelManager for better offline GenAI features.
 * Use GeminiNanoModelManager instead for new implementations.
 * This class is maintained for backward compatibility and existing tests only.
 */
@Deprecated
public class OfflineModelManager {
    private static final String TAG = "OfflineModelManager";
    // Use same preferences as OfflineTranslationService for synchronization
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
        
        // Add models for common languages with realistic file sizes
        addModel(models, "en", "English", 25 * 1024 * 1024, downloadedModels, true);
        addModel(models, "es", "Spanish", 28 * 1024 * 1024, downloadedModels, true);
        addModel(models, "fr", "French", 27 * 1024 * 1024, downloadedModels, true);
        addModel(models, "de", "German", 30 * 1024 * 1024, downloadedModels, true);
        addModel(models, "it", "Italian", 26 * 1024 * 1024, downloadedModels, true);
        addModel(models, "pt", "Portuguese", 27 * 1024 * 1024, downloadedModels, true);
        addModel(models, "ru", "Russian", 32 * 1024 * 1024, downloadedModels, true);
        addModel(models, "ja", "Japanese", 35 * 1024 * 1024, downloadedModels, true);
        addModel(models, "ko", "Korean", 33 * 1024 * 1024, downloadedModels, true);
        addModel(models, "zh-CN", "Chinese (Simplified)", 38 * 1024 * 1024, downloadedModels, true);
        addModel(models, "zh-TW", "Chinese (Traditional)", 38 * 1024 * 1024, downloadedModels, true);
        addModel(models, "ar", "Arabic", 30 * 1024 * 1024, downloadedModels, true);
        addModel(models, "hi", "Hindi", 29 * 1024 * 1024, downloadedModels, true);
        addModel(models, "nl", "Dutch", 25 * 1024 * 1024, downloadedModels, true);
        addModel(models, "sv", "Swedish", 24 * 1024 * 1024, downloadedModels, true);
        addModel(models, "fi", "Finnish", 26 * 1024 * 1024, downloadedModels, true);
        addModel(models, "da", "Danish", 23 * 1024 * 1024, downloadedModels, true);
        addModel(models, "no", "Norwegian", 24 * 1024 * 1024, downloadedModels, true);
        addModel(models, "pl", "Polish", 28 * 1024 * 1024, downloadedModels, true);
        addModel(models, "tr", "Turkish", 27 * 1024 * 1024, downloadedModels, true);
        addModel(models, "el", "Greek", 26 * 1024 * 1024, downloadedModels, true);
        addModel(models, "th", "Thai", 31 * 1024 * 1024, downloadedModels, true);
        addModel(models, "vi", "Vietnamese", 28 * 1024 * 1024, downloadedModels, true);
        addModel(models, "id", "Indonesian", 25 * 1024 * 1024, downloadedModels, true);
        addModel(models, "he", "Hebrew", 26 * 1024 * 1024, downloadedModels, true);
        
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
     * Download an offline model.
     */
    public void downloadModel(OfflineModelInfo model, DownloadListener listener) {
        if (model.isDownloaded()) {
            listener.onError("Model already downloaded");
            return;
        }
        
        model.setDownloading(true);
        
        // Use ML Kit's actual download API in a background thread
        new Thread(() -> {
            try {
                Log.d(TAG, "Starting ML Kit download for model: " + model.getLanguageCode());
                
                // Convert language code to ML Kit format
                String mlkitCode = LanguageCodeUtils.convertToMLKitLanguageCode(model.getLanguageCode());
                if (mlkitCode == null) {
                    model.setDownloading(false);
                    if (listener != null) {
                        listener.onError("Language not supported by ML Kit: " + model.getLanguageCode());
                    }
                    return;
                }
                
                // Create translator for this language pair (download requires both source and target)
                // We'll use English as the source for download purposes
                String sourceLanguage = TranslateLanguage.ENGLISH;
                String targetLanguage = mlkitCode;
                
                TranslatorOptions options = new TranslatorOptions.Builder()
                        .setSourceLanguage(sourceLanguage)
                        .setTargetLanguage(targetLanguage)
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
                    
                    // Mark as downloaded
                    model.setDownloading(false);
                    model.setDownloaded(true);
                    
                    // Save to preferences
                    saveDownloadedModelPrivate(model.getLanguageCode());
                    
                    // Create placeholder file for compatibility
                    createModelFile(model);
                    
                    Log.d(TAG, "ML Kit download completed for model: " + model.getLanguageCode());
                    
                    if (listener != null) {
                        listener.onProgress(100);
                        listener.onSuccess();
                    }
                    
                } finally {
                    // Always close the translator
                    try {
                        translator.close();
                    } catch (Exception e) {
                        Log.w(TAG, "Error closing translator during cleanup", e);
                    }
                }
                
            } catch (TimeoutException e) {
                Log.e(TAG, "Download timeout", e);
                model.setDownloading(false);
                if (listener != null) {
                    listener.onError("Download timeout. Please check your internet connection.");
                }
            } catch (ExecutionException e) {
                Log.e(TAG, "Download execution failed", e);
                model.setDownloading(false);
                String errorMsg = "Download failed";
                if (e.getCause() != null && e.getCause().getMessage() != null) {
                    errorMsg = "Download failed: " + e.getCause().getMessage();
                }
                if (listener != null) {
                    listener.onError(errorMsg);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Download interrupted", e);
                model.setDownloading(false);
                if (listener != null) {
                    listener.onError("Download interrupted");
                }
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Log.e(TAG, "Download failed with unexpected error", e);
                model.setDownloading(false);
                if (listener != null) {
                    listener.onError("Download failed: " + e.getMessage());
                }
            }
        }).start();
    }
    
    /**
     * Delete an offline model.
     */
    public boolean deleteModel(OfflineModelInfo model) {
        try {
            if (!model.isDownloaded()) {
                return false;
            }
            
            Log.d(TAG, "Starting ML Kit model deletion for: " + model.getLanguageCode());
            
            // Convert language code to ML Kit format
            String mlkitCode = LanguageCodeUtils.convertToMLKitLanguageCode(model.getLanguageCode());
            if (mlkitCode == null) {
                Log.w(TAG, "Language not supported by ML Kit: " + model.getLanguageCode());
                // Still remove from our tracking
                removeDownloadedModel(model.getLanguageCode());
                deleteModelFile(model);
                return true;
            }
            
            // Create translator for this language to delete its model
            String sourceLanguage = TranslateLanguage.ENGLISH;
            String targetLanguage = mlkitCode;
            
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLanguage)
                    .setTargetLanguage(targetLanguage)
                    .build();
            
            Translator translator = Translation.getClient(options);
            
            try {
                // Close the translator to release resources
                // Note: ML Kit doesn't provide deleteDownloadedModel() method
                // The close() method releases resources but doesn't delete files
                translator.close();
                
                Log.d(TAG, "ML Kit translator closed for: " + model.getLanguageCode());
                
            } catch (Exception e) {
                Log.w(TAG, "Error closing translator for " + model.getLanguageCode() + ", continuing with local cleanup", e);
            } finally {
                // Always close the translator
                try {
                    translator.close();
                } catch (Exception e) {
                    Log.w(TAG, "Error closing translator during cleanup", e);
                }
            }
            
            // Remove from our preferences and files regardless of ML Kit result
            removeDownloadedModel(model.getLanguageCode());
            deleteModelFile(model);
            
            Log.d(TAG, "Model deleted from local tracking: " + model.getLanguageCode());
            return true;
            
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
     * Check if a model is downloaded and verified.
     * This extends the basic download check with additional verification.
     */
    public boolean isModelDownloadedAndVerified(String languageCode) {
        if (!isModelDownloaded(languageCode)) {
            return false;
        }
        
        // Additional verification - check if model file exists
        try {
            File modelDir = getModelDirectory();
            File modelFile = new File(modelDir, languageCode + ".model");
            return modelFile.exists() && modelFile.canRead();
        } catch (Exception e) {
            Log.e(TAG, "Error verifying model file for " + languageCode, e);
            return false;
        }
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
     * Get offline model directory.
     */
    private File getModelDirectory() {
        File modelDir = new File(context.getFilesDir(), "offline_models");
        if (!modelDir.exists()) {
            modelDir.mkdirs();
        }
        return modelDir;
    }
    
    /**
     * Create a placeholder model file.
     */
    private void createModelFile(OfflineModelInfo model) {
        try {
            File modelDir = getModelDirectory();
            File modelFile = new File(modelDir, model.getLanguageCode() + ".model");
            
            // Create placeholder file (in real implementation, this would be the actual model)
            if (modelFile.createNewFile()) {
                Log.d(TAG, "Model file created: " + modelFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating model file", e);
        }
    }
    
    /**
     * Delete model file.
     */
    private void deleteModelFile(OfflineModelInfo model) {
        try {
            File modelDir = getModelDirectory();
            File modelFile = new File(modelDir, model.getLanguageCode() + ".model");
            
            if (modelFile.exists() && modelFile.delete()) {
                Log.d(TAG, "Model file deleted: " + modelFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting model file", e);
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
     * This provides a more accurate status than internal tracking.
     * 
     * @param languageCode The language code to check
     * @return true if the model is available in ML Kit, false otherwise
     */
    private boolean isModelAvailableInMLKit(String languageCode) {
        try {
            String mlkitCode = LanguageCodeUtils.convertToMLKitLanguageCode(languageCode);
            if (mlkitCode == null) {
                return false;
            }
            
            // Create translator with the language as both source and target
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(mlkitCode)
                    .setTargetLanguage(mlkitCode)
                    .build();

            Translator translator = Translation.getClient(options);
            
            try {
                // Try a quick translation to test if the model is available
                Task<String> translateTask = translator.translate("test");
                
                // Wait briefly to see if translation can complete immediately
                Tasks.await(translateTask, 1, TimeUnit.SECONDS);
                
                // If we got here without exception, model is available
                return true;
                
            } catch (TimeoutException e) {
                // Timeout means model needs to be downloaded
                return false;
            } catch (ExecutionException e) {
                // Check if the error indicates missing models
                if (e.getCause() != null && e.getCause().getMessage() != null) {
                    String errorMsg = e.getCause().getMessage().toLowerCase();
                    if (errorMsg.contains("model") && errorMsg.contains("download")) {
                        return false;
                    }
                }
                return false;
            } finally {
                // Clean up translator
                try {
                    translator.close();
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking ML Kit model availability for " + languageCode, e);
            return false;
        }
    }
}