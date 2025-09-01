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
    
    public interface DownloadListener {
        void onProgress(int progress);
        void onSuccess();
        void onError(String error);
    }
    
    public OfflineModelManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
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
        
        // Simulate download process in a background thread
        new Thread(() -> {
            try {
                Log.d(TAG, "Starting download for model: " + model.getLanguageCode());
                
                // Simulate download progress
                for (int progress = 0; progress <= 100; progress += 10) {
                    Thread.sleep(500); // Simulate download time
                    
                    final int currentProgress = progress;
                    model.setDownloadProgress(currentProgress);
                    
                    if (listener != null) {
                        listener.onProgress(currentProgress);
                    }
                }
                
                // Mark as downloaded
                model.setDownloading(false);
                model.setDownloaded(true);
                
                // Save to preferences
                saveDownloadedModelPrivate(model.getLanguageCode());
                
                // Create placeholder file
                createModelFile(model);
                
                Log.d(TAG, "Download completed for model: " + model.getLanguageCode());
                
                if (listener != null) {
                    listener.onSuccess();
                }
                
            } catch (InterruptedException e) {
                Log.e(TAG, "Download interrupted", e);
                model.setDownloading(false);
                if (listener != null) {
                    listener.onError("Download interrupted");
                }
            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
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
            
            // Remove from preferences
            removeDownloadedModel(model.getLanguageCode());
            
            // Delete model file
            deleteModelFile(model);
            
            Log.d(TAG, "Model deleted: " + model.getLanguageCode());
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
    private void removeDownloadedModel(String languageCode) {
        Set<String> downloadedModels = new HashSet<>(getDownloadedModelCodes());
        downloadedModels.remove(languageCode);
        preferences.edit().putStringSet(KEY_DOWNLOADED_MODELS, downloadedModels).apply();
    }
}