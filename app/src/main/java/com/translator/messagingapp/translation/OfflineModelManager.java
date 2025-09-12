package com.translator.messagingapp.translation;

import com.translator.messagingapp.message.*;

import com.translator.messagingapp.translation.*;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manager for ML Kit offline translation models.
 * Handles downloading, deleting, and managing language models.
 */
public class OfflineModelManager {
    private static final String TAG = "OfflineModelManager";
    
    // Download timeouts - longer for first attempts due to ML Kit initialization
    private static final int DOWNLOAD_TIMEOUT_SECONDS = 60;
    private static final int FIRST_DOWNLOAD_TIMEOUT_SECONDS = 120;
    
    public interface DownloadListener {
        void onProgress(int progress);
        void onSuccess();
        void onError(String errorMessage);
    }
    
    public interface ModelStatusListener {
        void onStatusUpdated(List<OfflineLanguageModel> models);
        void onError(String errorMessage);
    }
    
    public static class OfflineLanguageModel {
        private final String languageCode;
        private final String displayName;
        private boolean isDownloaded;
        private boolean isDownloading;
        private int downloadProgress;
        
        public OfflineLanguageModel(String languageCode, String displayName) {
            this.languageCode = languageCode;
            this.displayName = displayName;
            this.isDownloaded = false;
            this.isDownloading = false;
            this.downloadProgress = 0;
        }
        
        // Getters and setters
        public String getLanguageCode() { return languageCode; }
        public String getDisplayName() { return displayName; }
        public boolean isDownloaded() { return isDownloaded; }
        public boolean isDownloading() { return isDownloading; }
        public int getDownloadProgress() { return downloadProgress; }
        
        public void setDownloaded(boolean downloaded) { this.isDownloaded = downloaded; }
        public void setDownloading(boolean downloading) { this.isDownloading = downloading; }
        public void setDownloadProgress(int progress) { this.downloadProgress = progress; }
    }
    
    private final Context context;
    private final RemoteModelManager modelManager;
    private final ExecutorService executorService;
    private final Map<String, OfflineLanguageModel> modelCache;
    private final List<String> modelOrder; // Maintains frequency-based ordering
    private final Map<String, Boolean> firstDownloadAttempts; // Track first-time downloads
    
    /**
     * Creates a new OfflineModelManager.
     *
     * @param context The application context
     */
    public OfflineModelManager(Context context) {
        this.context = context;
        this.modelManager = RemoteModelManager.getInstance();
        this.executorService = Executors.newCachedThreadPool();
        this.modelCache = new HashMap<>();
        this.modelOrder = new ArrayList<>();
        this.firstDownloadAttempts = new HashMap<>();
        
        initializeModels();
        Log.d(TAG, "OfflineModelManager initialized");
    }
    
    /**
     * Initialize the available language models.
     * Languages are ordered by global usage frequency (most common first).
     */
    private void initializeModels() {
        // Tier 1: Major Global Languages (>500M speakers)
        addModel("en", "English");        // 1.5B+ speakers, lingua franca
        addModel("zh", "Chinese");        // 1.1B+ native speakers
        addModel("hi", "Hindi");          // 600M+ speakers
        addModel("es", "Spanish");        // 500M+ speakers
        addModel("ar", "Arabic");         // 400M+ speakers
        
        // Tier 2: Major Regional Languages (100-500M speakers)
        addModel("pt", "Portuguese");     // 280M speakers
        addModel("fr", "French");         // 280M speakers
        addModel("ru", "Russian");        // 260M speakers
        addModel("ja", "Japanese");       // 125M speakers
        addModel("de", "German");         // 100M speakers
        
        // Tier 3: Significant Regional Languages (50-100M speakers)
        addModel("ko", "Korean");         // 82M speakers
        addModel("it", "Italian");        // 65M speakers
        addModel("tr", "Turkish");        // 84M speakers
        addModel("vi", "Vietnamese");     // 85M speakers
        addModel("pl", "Polish");         // 50M speakers
        addModel("uk", "Ukrainian");      // 40M speakers
        
        // Tier 4: Other Important Languages (10-50M speakers)
        addModel("nl", "Dutch");          // 25M speakers
        addModel("th", "Thai");           // 20M speakers
        addModel("cs", "Czech");          // 10M speakers
        addModel("hu", "Hungarian");      // 13M speakers
        addModel("ro", "Romanian");       // 24M speakers
        addModel("he", "Hebrew");         // 9M speakers
        addModel("sv", "Swedish");        // 10M speakers
        addModel("da", "Danish");         // 6M speakers
        addModel("no", "Norwegian");      // 5M speakers
        addModel("fi", "Finnish");        // 5M speakers
        addModel("bn", "Bengali");        // 230M speakers
        addModel("ur", "Urdu");           // 170M speakers
        addModel("id", "Indonesian");     // 200M speakers
        addModel("ms", "Malay");          // 20M speakers
        addModel("tl", "Filipino");       // 45M speakers
        addModel("fa", "Persian");        // 110M speakers
        addModel("ta", "Tamil");          // 78M speakers
        addModel("te", "Telugu");         // 75M speakers
        addModel("ml", "Malayalam");      // 35M speakers
        addModel("kn", "Kannada");        // 44M speakers
        addModel("gu", "Gujarati");       // 56M speakers
        addModel("pa", "Punjabi");        // 113M speakers
        
        // Tier 5: Smaller Languages (alphabetical order for consistency)
        addModel("af", "Afrikaans");
        addModel("sq", "Albanian");
        addModel("am", "Amharic");
        addModel("hy", "Armenian");
        addModel("az", "Azerbaijani");
        addModel("eu", "Basque");
        addModel("be", "Belarusian");
        addModel("bg", "Bulgarian");
        addModel("ca", "Catalan");
        addModel("hr", "Croatian");
        addModel("et", "Estonian");
        addModel("ka", "Georgian");
        addModel("el", "Greek");
        addModel("ga", "Irish");
        addModel("is", "Icelandic");
        addModel("lv", "Latvian");
        addModel("lt", "Lithuanian");
        addModel("mk", "Macedonian");
        addModel("mt", "Maltese");
        addModel("mn", "Mongolian");
        addModel("ne", "Nepali");
        addModel("si", "Sinhala");
        addModel("sk", "Slovak");
        addModel("sl", "Slovenian");
        addModel("sw", "Swahili");
        addModel("cy", "Welsh");
    }
    
    /**
     * Add a language model to the cache.
     */
    private void addModel(String languageCode, String displayName) {
        String mlkitCode = convertToMLKitLanguageCode(languageCode);
        if (mlkitCode != null) {
            modelCache.put(languageCode, new OfflineLanguageModel(languageCode, displayName));
            modelOrder.add(languageCode); // Maintain frequency-based ordering
        }
    }
    
    /**
     * Convert ISO language code to ML Kit language code.
     */
    private String convertToMLKitLanguageCode(String languageCode) {
        if (TextUtils.isEmpty(languageCode)) {
            return null;
        }
        
        try {
            return TranslateLanguage.fromLanguageTag(languageCode);
        } catch (Exception e) {
            Log.w(TAG, "Unsupported language code: " + languageCode);
            return null;
        }
    }
    
    /**
     * Downloads a language model.
     *
     * @param languageCode The language code to download
     * @param listener The download progress listener
     */
    public void downloadModel(String languageCode, DownloadListener listener) {
        executorService.execute(() -> {
            try {
                OfflineLanguageModel model = modelCache.get(languageCode);
                if (model == null) {
                    listener.onError("Unsupported language: " + languageCode);
                    return;
                }
                
                if (model.isDownloaded()) {
                    listener.onProgress(100);
                    listener.onSuccess();
                    return;
                }
                
                if (model.isDownloading()) {
                    listener.onError("Model is already downloading");
                    return;
                }
                
                String mlkitLanguageCode = convertToMLKitLanguageCode(languageCode);
                if (mlkitLanguageCode == null) {
                    listener.onError("Unsupported language: " + languageCode);
                    return;
                }
                
                // Check if this is a first-time download attempt
                boolean isFirstAttempt = !firstDownloadAttempts.getOrDefault(languageCode, false);
                if (isFirstAttempt) {
                    firstDownloadAttempts.put(languageCode, true);
                    Log.d(TAG, "First-time download attempt for: " + languageCode);
                }
                
                // Mark as downloading
                model.setDownloading(true);
                model.setDownloadProgress(10);
                listener.onProgress(10);
                
                // Create translator for manual download
                TranslatorOptions options = new TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.ENGLISH)
                        .setTargetLanguage(mlkitLanguageCode)
                        .build();
                
                Translator translator = Translation.getClient(options);
                
                try {
                    // Use less restrictive download conditions for better success rate
                    // Allow downloads on any network connection, not just WiFi
                    DownloadConditions conditions = new DownloadConditions.Builder()
                            .build(); // No restrictions - allow mobile data
                    
                    Task<Void> downloadTask = translator.downloadModelIfNeeded(conditions);
                    
                    // Use longer timeout for first attempts due to ML Kit initialization overhead
                    int timeoutSeconds = isFirstAttempt ? FIRST_DOWNLOAD_TIMEOUT_SECONDS : DOWNLOAD_TIMEOUT_SECONDS;
                    Log.d(TAG, "Using " + timeoutSeconds + "s timeout for " + 
                          (isFirstAttempt ? "first-time" : "retry") + " download of " + languageCode);
                    
                    // Wait for download completion
                    Tasks.await(downloadTask, timeoutSeconds, TimeUnit.SECONDS);
                    
                    // Update progress
                    model.setDownloadProgress(90);
                    listener.onProgress(90);
                    
                    // Verify download success
                    boolean isAvailable = isModelAvailableInMLKit(languageCode);
                    if (isAvailable) {
                        model.setDownloaded(true);
                        model.setDownloadProgress(100);
                        listener.onProgress(100);
                        listener.onSuccess();
                        Log.d(TAG, "Model downloaded successfully: " + languageCode + 
                              (isFirstAttempt ? " (first attempt)" : " (retry)"));
                    } else {
                        throw new Exception("Model download completed but verification failed");
                    }
                    
                } finally {
                    translator.close();
                    model.setDownloading(false);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error downloading model for " + languageCode, e);
                OfflineLanguageModel model = modelCache.get(languageCode);
                if (model != null) {
                    model.setDownloading(false);
                    model.setDownloadProgress(0);
                }
                
                String errorMessage = "Download failed: " + e.getMessage();
                if (e.getCause() != null && e.getCause().getMessage() != null) {
                    errorMessage += " (" + e.getCause().getMessage() + ")";
                }
                
                // For first-time failures, provide more helpful error message
                boolean wasFirstAttempt = firstDownloadAttempts.getOrDefault(languageCode, false);
                if (wasFirstAttempt && e.getMessage() != null && e.getMessage().contains("timeout")) {
                    errorMessage += ". First downloads may take longer - please try again.";
                }
                
                listener.onError(errorMessage);
            }
        });
    }
    
    /**
     * Deletes a language model.
     *
     * @param languageCode The language code to delete
     * @param callback Callback for completion
     */
    public void deleteModel(String languageCode, Runnable onSuccess, 
                           java.util.function.Consumer<String> onError) {
        executorService.execute(() -> {
            try {
                String mlkitLanguageCode = convertToMLKitLanguageCode(languageCode);
                if (mlkitLanguageCode == null) {
                    onError.accept("Unsupported language: " + languageCode);
                    return;
                }
                
                TranslateRemoteModel model = new TranslateRemoteModel.Builder(mlkitLanguageCode).build();
                Task<Void> deleteTask = modelManager.deleteDownloadedModel(model);
                
                Tasks.await(deleteTask, 30, TimeUnit.SECONDS);
                
                // Update cache
                OfflineLanguageModel cachedModel = modelCache.get(languageCode);
                if (cachedModel != null) {
                    cachedModel.setDownloaded(false);
                    cachedModel.setDownloadProgress(0);
                }
                
                onSuccess.run();
                Log.d(TAG, "Model deleted successfully: " + languageCode);
                
            } catch (Exception e) {
                Log.e(TAG, "Error deleting model for " + languageCode, e);
                onError.accept("Delete failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Gets the list of available language models with their status.
     * Models are returned in usage frequency order (most common first).
     */
    public void getAvailableModels(ModelStatusListener listener) {
        executorService.execute(() -> {
            try {
                updateModelStatuses();
                // Return models in frequency-based order
                List<OfflineLanguageModel> models = new ArrayList<>();
                for (String languageCode : modelOrder) {
                    OfflineLanguageModel model = modelCache.get(languageCode);
                    if (model != null) {
                        models.add(model);
                    }
                }
                listener.onStatusUpdated(models);
            } catch (Exception e) {
                Log.e(TAG, "Error getting available models", e);
                listener.onError("Failed to get models: " + e.getMessage());
            }
        });
    }
    
    /**
     * Updates the download status of all models.
     */
    private void updateModelStatuses() {
        for (OfflineLanguageModel model : modelCache.values()) {
            if (!model.isDownloading()) {
                boolean isDownloaded = isModelAvailableInMLKit(model.getLanguageCode());
                model.setDownloaded(isDownloaded);
                if (isDownloaded) {
                    model.setDownloadProgress(100);
                } else {
                    model.setDownloadProgress(0);
                }
            }
        }
    }
    
    /**
     * Checks if a model is available in ML Kit.
     */
    public boolean isModelAvailableInMLKit(String languageCode) {
        try {
            String mlkitLanguageCode = convertToMLKitLanguageCode(languageCode);
            if (mlkitLanguageCode == null) {
                return false;
            }
            
            TranslateRemoteModel model = new TranslateRemoteModel.Builder(mlkitLanguageCode).build();
            Task<Set<TranslateRemoteModel>> getModelsTask = modelManager.getDownloadedModels(TranslateRemoteModel.class);
            
            Set<TranslateRemoteModel> downloadedModels = Tasks.await(getModelsTask, 10, TimeUnit.SECONDS);
            
            for (TranslateRemoteModel downloadedModel : downloadedModels) {
                if (downloadedModel.getLanguage().equals(mlkitLanguageCode)) {
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            Log.w(TAG, "Error checking model availability for " + languageCode, e);
            return false;
        }
    }
    
    /**
     * Gets a specific model by language code.
     */
    public OfflineLanguageModel getModel(String languageCode) {
        return modelCache.get(languageCode);
    }
    
    /**
     * Cleans up resources.
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        Log.d(TAG, "OfflineModelManager cleaned up");
    }
}