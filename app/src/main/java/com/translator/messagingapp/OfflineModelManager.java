package com.translator.messagingapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages offline translation models including downloading and deletion.
 */
public class OfflineModelManager {
    private static final String TAG = "OfflineModelManager";
    private static final String PREFS_NAME = "offline_models";
    private static final String KEY_DOWNLOADED_MODELS = "downloaded_models";
    
    private Context context;
    private SharedPreferences preferences;
    private OfflineTranslationService offlineTranslationService;
    
    public interface DownloadListener {
        void onProgress(int progress);
        void onSuccess();
        void onError(String error);
    }
    
    public OfflineModelManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Initialize offline translation service for actual MLKit integration
        UserPreferences userPrefs = new UserPreferences(context);
        this.offlineTranslationService = new OfflineTranslationService(context, userPrefs);
    }
    
    /**
     * Get list of all available offline models.
     */
    public List<OfflineModelInfo> getAvailableModels() {
        List<OfflineModelInfo> models = new ArrayList<>();
        Set<String> downloadedModels = getDownloadedModelCodes();
        
        // Add models for common languages with realistic file sizes
        addModel(models, "en", "English", 25 * 1024 * 1024, downloadedModels.contains("en"));
        addModel(models, "es", "Spanish", 28 * 1024 * 1024, downloadedModels.contains("es"));
        addModel(models, "fr", "French", 27 * 1024 * 1024, downloadedModels.contains("fr"));
        addModel(models, "de", "German", 30 * 1024 * 1024, downloadedModels.contains("de"));
        addModel(models, "it", "Italian", 26 * 1024 * 1024, downloadedModels.contains("it"));
        addModel(models, "pt", "Portuguese", 27 * 1024 * 1024, downloadedModels.contains("pt"));
        addModel(models, "ru", "Russian", 32 * 1024 * 1024, downloadedModels.contains("ru"));
        addModel(models, "ja", "Japanese", 35 * 1024 * 1024, downloadedModels.contains("ja"));
        addModel(models, "ko", "Korean", 33 * 1024 * 1024, downloadedModels.contains("ko"));
        addModel(models, "zh-CN", "Chinese (Simplified)", 38 * 1024 * 1024, downloadedModels.contains("zh-CN"));
        addModel(models, "zh-TW", "Chinese (Traditional)", 38 * 1024 * 1024, downloadedModels.contains("zh-TW"));
        addModel(models, "ar", "Arabic", 30 * 1024 * 1024, downloadedModels.contains("ar"));
        addModel(models, "hi", "Hindi", 29 * 1024 * 1024, downloadedModels.contains("hi"));
        addModel(models, "nl", "Dutch", 25 * 1024 * 1024, downloadedModels.contains("nl"));
        addModel(models, "sv", "Swedish", 24 * 1024 * 1024, downloadedModels.contains("sv"));
        addModel(models, "fi", "Finnish", 26 * 1024 * 1024, downloadedModels.contains("fi"));
        addModel(models, "da", "Danish", 23 * 1024 * 1024, downloadedModels.contains("da"));
        addModel(models, "no", "Norwegian", 24 * 1024 * 1024, downloadedModels.contains("no"));
        addModel(models, "pl", "Polish", 28 * 1024 * 1024, downloadedModels.contains("pl"));
        addModel(models, "tr", "Turkish", 27 * 1024 * 1024, downloadedModels.contains("tr"));
        addModel(models, "el", "Greek", 26 * 1024 * 1024, downloadedModels.contains("el"));
        addModel(models, "th", "Thai", 31 * 1024 * 1024, downloadedModels.contains("th"));
        addModel(models, "vi", "Vietnamese", 28 * 1024 * 1024, downloadedModels.contains("vi"));
        addModel(models, "id", "Indonesian", 25 * 1024 * 1024, downloadedModels.contains("id"));
        addModel(models, "he", "Hebrew", 26 * 1024 * 1024, downloadedModels.contains("he"));
        
        return models;
    }
    
    private void addModel(List<OfflineModelInfo> models, String code, String name, long size, boolean isDownloaded) {
        OfflineModelInfo model = new OfflineModelInfo(code, name, size);
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
        Log.d(TAG, "Starting actual MLKit download for model: " + model.getLanguageCode());
        
        // Use OfflineTranslationService to actually download the MLKit model
        offlineTranslationService.downloadLanguageModel(model.getLanguageCode(), 
            new OfflineTranslationService.ModelDownloadCallback() {
                @Override
                public void onDownloadComplete(boolean success, String languageCode, String errorMessage) {
                    model.setDownloading(false);
                    
                    if (success) {
                        model.setDownloaded(true);
                        
                        // Save to preferences (this is handled by OfflineTranslationService, but ensure sync)
                        saveDownloadedModel(model.getLanguageCode());
                        
                        // Create placeholder file for UI consistency
                        createModelFile(model);
                        
                        Log.d(TAG, "MLKit model download completed for: " + languageCode);
                        
                        if (listener != null) {
                            listener.onSuccess();
                        }
                    } else {
                        Log.e(TAG, "MLKit model download failed for " + languageCode + ": " + errorMessage);
                        if (listener != null) {
                            listener.onError("Download failed: " + errorMessage);
                        }
                    }
                }
                
                @Override
                public void onDownloadProgress(String languageCode, int progress) {
                    // MLKit doesn't provide detailed progress, so simulate it
                    model.setDownloadProgress(progress);
                    if (listener != null) {
                        listener.onProgress(progress);
                    }
                }
            });
        
        // Since MLKit doesn't provide real-time progress, simulate progress updates
        new Thread(() -> {
            try {
                for (int progress = 10; progress <= 90; progress += 10) {
                    if (!model.isDownloading()) {
                        break; // Download completed or failed
                    }
                    Thread.sleep(1000); // Update every second
                    
                    final int currentProgress = progress;
                    model.setDownloadProgress(currentProgress);
                    
                    if (listener != null) {
                        listener.onProgress(currentProgress);
                    }
                }
            } catch (InterruptedException e) {
                Log.w(TAG, "Progress simulation interrupted", e);
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
            
            // Delete the actual MLKit model
            offlineTranslationService.deleteLanguageModel(model.getLanguageCode());
            
            // Remove from preferences
            removeDownloadedModel(model.getLanguageCode());
            
            // Delete placeholder model file
            deleteModelFile(model);
            
            Log.d(TAG, "Model deleted (including MLKit model): " + model.getLanguageCode());
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting model", e);
            return false;
        }
    }
    
    /**
     * Check if a model is downloaded.
     * This method now synchronizes with MLKit to ensure accuracy.
     */
    public boolean isModelDownloaded(String languageCode) {
        // Check both our preferences and the actual MLKit availability
        Set<String> downloadedModels = getDownloadedModelCodes();
        boolean inPrefs = downloadedModels.contains(languageCode);
        
        // Also check with OfflineTranslationService to ensure MLKit has the model
        boolean inMLKit = offlineTranslationService.isLanguageModelDownloaded(languageCode);
        
        // If there's a mismatch, log it and sync only if it's safe to do so
        if (inPrefs != inMLKit) {
            Log.w(TAG, "Model tracking mismatch for " + languageCode + 
                  ": prefs=" + inPrefs + ", MLKit=" + inMLKit);
            
            // Only sync if MLKit has the model but we don't track it
            // (Don't remove from prefs immediately as model might be downloading)
            if (inMLKit && !inPrefs) {
                saveDownloadedModel(languageCode);
                Log.d(TAG, "Synced model to preferences: " + languageCode);
            }
        }
        
        // Return the more authoritative source (MLKit)
        return inMLKit;
    }
    
    /**
     * Performs a full synchronization of model state with MLKit.
     * This should be called periodically or after download operations.
     */
    public void syncWithMLKit() {
        try {
            Set<String> prefsModels = new HashSet<>(getDownloadedModelCodes());
            Set<String> actualModels = new HashSet<>();
            
            // Check each language we support to see if MLKit has it
            String[] supportedLanguages = {"en", "es", "fr", "de", "it", "pt", "ru", "ja", "ko", 
                "zh", "ar", "hi", "nl", "sv", "fi", "da", "no", "pl", "tr", "el", "th", "vi", "id", "he"};
            
            for (String lang : supportedLanguages) {
                if (offlineTranslationService.isLanguageModelDownloaded(lang)) {
                    actualModels.add(lang);
                }
            }
            
            // Sync preferences to match MLKit reality
            for (String lang : actualModels) {
                if (!prefsModels.contains(lang)) {
                    saveDownloadedModel(lang);
                    Log.d(TAG, "Added missing model to preferences: " + lang);
                }
            }
            
            // Remove stale entries (models we think we have but MLKit doesn't)
            for (String lang : prefsModels) {
                if (!actualModels.contains(lang)) {
                    removeDownloadedModel(lang);
                    Log.d(TAG, "Removed stale model from preferences: " + lang);
                }
            }
            
            Log.d(TAG, "Sync complete. MLKit models: " + actualModels.size() + 
                      ", Preference models: " + actualModels.size());
                      
        } catch (Exception e) {
            Log.e(TAG, "Error during MLKit sync", e);
        }
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
    private void saveDownloadedModel(String languageCode) {
        Set<String> downloadedModels = new HashSet<>(getDownloadedModelCodes());
        downloadedModels.add(languageCode);
        preferences.edit().putStringSet(KEY_DOWNLOADED_MODELS, downloadedModels).apply();
    }
    
    /**
     * Remove a downloaded model from preferences.
     */
    private void removeDownloadedModel(String languageCode) {
        Set<String> downloadedModels = new HashSet<>(getDownloadedModelCodes());
        downloadedModels.remove(languageCode);
        preferences.edit().putStringSet(KEY_DOWNLOADED_MODELS, downloadedModels).apply();
    }
}