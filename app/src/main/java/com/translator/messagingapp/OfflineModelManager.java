package com.translator.messagingapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

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
     * Download an offline model using MLKit.
     */
    public void downloadModel(OfflineModelInfo model, DownloadListener listener) {
        if (model.isDownloaded()) {
            if (listener != null) {
                listener.onError("Model already downloaded");
            }
            return;
        }
        
        String mlkitLanguageCode = convertToMLKitLanguageCode(model.getLanguageCode());
        if (mlkitLanguageCode == null) {
            if (listener != null) {
                listener.onError("Unsupported language: " + model.getLanguageCode());
            }
            return;
        }
        
        model.setDownloading(true);
        Log.d(TAG, "Starting MLKit download for model: " + model.getLanguageCode() + " -> " + mlkitLanguageCode);
        
        // Create translator to trigger actual MLKit model download
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(mlkitLanguageCode)
                .setTargetLanguage(TranslateLanguage.ENGLISH) // Use English as reference target
                .build();
        
        Translator translator = Translation.getClient(options);
        
        // Trigger progress updates while MLKit downloads
        new Thread(() -> {
            try {
                // Simulate progress while waiting for MLKit
                for (int progress = 10; progress <= 90; progress += 20) {
                    Thread.sleep(1000);
                    final int currentProgress = progress;
                    model.setDownloadProgress(currentProgress);
                    
                    if (listener != null) {
                        listener.onProgress(currentProgress);
                    }
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "Progress thread interrupted");
            }
        }).start();
        
        // Start actual MLKit model download
        translator.downloadModelIfNeeded()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "MLKit model downloaded successfully: " + mlkitLanguageCode);
                    
                    // Mark as downloaded
                    model.setDownloading(false);
                    model.setDownloaded(true);
                    model.setDownloadProgress(100);
                    
                    // Save to preferences
                    saveDownloadedModelPrivate(model.getLanguageCode());
                    
                    // Create marker file for verification
                    createModelFile(model);
                    
                    Log.d(TAG, "Download completed for model: " + model.getLanguageCode());
                    
                    if (listener != null) {
                        listener.onProgress(100);
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "MLKit model download failed: " + mlkitLanguageCode, exception);
                    
                    model.setDownloading(false);
                    model.setDownloaded(false);
                    
                    String errorMessage = "Download failed";
                    if (exception.getMessage() != null) {
                        errorMessage = "Download failed: " + exception.getMessage();
                    }
                    
                    if (listener != null) {
                        listener.onError(errorMessage);
                    }
                });
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
    private void removeDownloadedModel(String languageCode) {
        Set<String> downloadedModels = new HashSet<>(getDownloadedModelCodes());
        downloadedModels.remove(languageCode);
        preferences.edit().putStringSet(KEY_DOWNLOADED_MODELS, downloadedModels).apply();
    }
    
    /**
     * Converts standard language codes to MLKit language codes.
     * This method duplicates the conversion logic from OfflineTranslationService
     * to ensure consistent language code handling.
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
}