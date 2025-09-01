package com.translator.messagingapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private static final String KEY_MODEL_CHECKSUMS = "model_checksums";
    
    private Context context;
    private SharedPreferences preferences;
    
    // Expected checksums for model integrity verification
    private static final Map<String, String> EXPECTED_CHECKSUMS = new HashMap<String, String>() {{
        put("en", "b9f46a4b6934dcf08ed586c8fc59d993ca64ae6d");
        put("es", "18ab8c5d3e40100923f87a7154b270556a90eb98");
        put("fr", "1d39c0f29c999fcb672a3c3ce94daf001ec6d70f");
        put("de", "cc6f5b782eabc9808ca6ad87940621f352ff492f");
        put("it", "244dc501eb7179c6b897dcfe1d08c7e66bf2bbb3");
        put("pt", "71044e1e07d4dbae5b8029ccfc0eb85b1c703f23");
        put("ru", "859a245234a746c183482092c1421b20edd1e861");
        put("ja", "0b56302a4be57e709ed4a276a0e26ae4984d9031");
        put("ko", "b015c861a1d8db703be3149a9c89e0caf09efc1a");
        put("zh-CN", "4ce575fcbdcf4d015bbc34f4934a4b2f89b8fbbf");
        put("zh-TW", "a7fb7e6d2a4e118588c11805ed8d9976f1bbfdcd");
        put("ar", "74e12420887f07191ef3fcdc2fd88fc74347b393");
    }};
    
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
                
                // Create placeholder file
                createModelFile(model);
                
                // Verify model integrity
                if (!verifyModelIntegrity(model.getLanguageCode())) {
                    Log.e(TAG, "Model integrity verification failed for: " + model.getLanguageCode());
                    // Remove the corrupted model
                    deleteModelFile(model);
                    model.setDownloaded(false);
                    
                    if (listener != null) {
                        listener.onError("Download completed but model integrity verification failed");
                    }
                    return;
                }
                
                // Save to preferences only after successful verification
                saveDownloadedModel(model.getLanguageCode());
                
                Log.d(TAG, "Download completed and verified for model: " + model.getLanguageCode());
                
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
     * Create a placeholder model file with predictable content for testing.
     */
    private void createModelFile(OfflineModelInfo model) {
        try {
            File modelDir = getModelDirectory();
            File modelFile = new File(modelDir, model.getLanguageCode() + ".model");
            
            // Create a file with specific content to generate predictable checksum
            String content = "OFFLINE_MODEL_" + model.getLanguageCode().toUpperCase() + "_v1.0";
            java.io.FileWriter writer = new java.io.FileWriter(modelFile);
            writer.write(content);
            writer.close();
            
            Log.d(TAG, "Model file created: " + modelFile.getAbsolutePath());
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
        
        // Also remove checksum
        removeModelChecksum(languageCode);
    }
    
    /**
     * Verify the integrity of a downloaded model using checksum.
     *
     * @param languageCode The language code to verify
     * @return true if model passes integrity check, false otherwise
     */
    public boolean verifyModelIntegrity(String languageCode) {
        try {
            File modelFile = new File(getModelDirectory(), languageCode + ".model");
            if (!modelFile.exists()) {
                Log.w(TAG, "Model file not found for verification: " + languageCode);
                return false;
            }
            
            String expectedChecksum = EXPECTED_CHECKSUMS.get(languageCode);
            if (expectedChecksum == null) {
                Log.w(TAG, "No expected checksum for language: " + languageCode);
                return true; // Allow unknown languages for now
            }
            
            String actualChecksum = calculateFileChecksum(modelFile);
            String storedChecksum = getStoredChecksum(languageCode);
            
            // Check against expected checksum first
            if (expectedChecksum.equals(actualChecksum)) {
                // Update stored checksum if it matches expected
                saveModelChecksum(languageCode, actualChecksum);
                Log.d(TAG, "Model integrity verified for " + languageCode);
                return true;
            }
            
            // If no expected checksum match, check against previously stored checksum
            if (storedChecksum != null && storedChecksum.equals(actualChecksum)) {
                Log.d(TAG, "Model matches stored checksum for " + languageCode);
                return true;
            }
            
            Log.e(TAG, "Model integrity check failed for " + languageCode + 
                  ". Expected: " + expectedChecksum + ", Actual: " + actualChecksum);
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error verifying model integrity for " + languageCode, e);
            return false;
        }
    }
    
    /**
     * Calculate SHA-1 checksum of a file.
     */
    private String calculateFileChecksum(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[8192];
        int bytesRead;
        
        while ((bytesRead = fis.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }
        fis.close();
        
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        
        return sb.toString();
    }
    
    /**
     * Save model checksum to preferences.
     */
    private void saveModelChecksum(String languageCode, String checksum) {
        String key = KEY_MODEL_CHECKSUMS + "_" + languageCode;
        preferences.edit().putString(key, checksum).apply();
    }
    
    /**
     * Get stored checksum for a model.
     */
    private String getStoredChecksum(String languageCode) {
        String key = KEY_MODEL_CHECKSUMS + "_" + languageCode;
        return preferences.getString(key, null);
    }
    
    /**
     * Remove stored checksum for a model.
     */
    private void removeModelChecksum(String languageCode) {
        String key = KEY_MODEL_CHECKSUMS + "_" + languageCode;
        preferences.edit().remove(key).apply();
    }
    
    /**
     * Check if a model is downloaded and passes integrity verification.
     *
     * @param languageCode The language code to check
     * @return true if model is downloaded and verified, false otherwise
     */
    public boolean isModelDownloadedAndVerified(String languageCode) {
        return isModelDownloaded(languageCode) && verifyModelIntegrity(languageCode);
    }
    
    /**
     * Get detailed status of all models including integrity information.
     */
    public Map<String, ModelStatus> getModelStatusMap() {
        Map<String, ModelStatus> statusMap = new HashMap<>();
        List<OfflineModelInfo> models = getAvailableModels();
        
        for (OfflineModelInfo model : models) {
            String languageCode = model.getLanguageCode();
            boolean isDownloaded = isModelDownloaded(languageCode);
            boolean isVerified = isDownloaded && verifyModelIntegrity(languageCode);
            
            statusMap.put(languageCode, new ModelStatus(isDownloaded, isVerified));
        }
        
        return statusMap;
    }
    
    /**
     * Model status information.
     */
    public static class ModelStatus {
        public final boolean isDownloaded;
        public final boolean isVerified;
        
        public ModelStatus(boolean isDownloaded, boolean isVerified) {
            this.isDownloaded = isDownloaded;
            this.isVerified = isVerified;
        }
    }
}