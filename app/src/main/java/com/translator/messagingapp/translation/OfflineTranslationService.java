package com.translator.messagingapp.translation;

import com.translator.messagingapp.message.*;

import com.translator.messagingapp.translation.*;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service for performing offline translation using ML Kit.
 * Handles text translation using downloaded language models.
 */
public class OfflineTranslationService {
    private static final String TAG = "OfflineTranslationService";
    
    // Translation timeout
    private static final int TRANSLATION_TIMEOUT_SECONDS = 30;
    
    public interface TranslationCallback {
        void onTranslationComplete(boolean success, String translatedText, String errorMessage);
    }
    
    public interface ModelDownloadCallback {
        void onDownloadProgress(int progress);
        void onDownloadComplete(boolean success, String errorMessage);
    }
    
    private final Context context;
    private final OfflineModelManager modelManager;
    private final ExecutorService executorService;
    
    /**
     * Creates a new OfflineTranslationService.
     *
     * @param context The application context
     */
    public OfflineTranslationService(Context context) {
        this.context = context;
        this.modelManager = new OfflineModelManager(context);
        this.executorService = Executors.newCachedThreadPool();
        
        Log.d(TAG, "OfflineTranslationService initialized");
    }
    
    /**
     * Translates text from source language to target language using offline models.
     *
     * @param text The text to translate
     * @param sourceLanguage The source language code
     * @param targetLanguage The target language code
     * @param callback The callback to receive the result
     */
    public void translateText(String text, String sourceLanguage, String targetLanguage, 
                             TranslationCallback callback) {
        if (TextUtils.isEmpty(text)) {
            callback.onTranslationComplete(false, null, "Empty text provided");
            return;
        }
        
        if (TextUtils.isEmpty(sourceLanguage) || TextUtils.isEmpty(targetLanguage)) {
            callback.onTranslationComplete(false, null, "Source or target language not specified");
            return;
        }
        
        if (sourceLanguage.equals(targetLanguage)) {
            // No translation needed
            callback.onTranslationComplete(true, text, null);
            return;
        }
        
        executorService.execute(() -> {
            try {
                // Check if models are available
                if (!areModelsAvailable(sourceLanguage, targetLanguage)) {
                    callback.onTranslationComplete(false, null, 
                        "Language models not downloaded for " + sourceLanguage + " -> " + targetLanguage);
                    return;
                }
                
                // Convert to ML Kit language codes
                String sourceMLKit = convertToMLKitLanguageCode(sourceLanguage);
                String targetMLKit = convertToMLKitLanguageCode(targetLanguage);
                
                if (sourceMLKit == null || targetMLKit == null) {
                    callback.onTranslationComplete(false, null, 
                        "Unsupported language codes: " + sourceLanguage + " -> " + targetLanguage);
                    return;
                }
                
                // Create translator
                TranslatorOptions options = new TranslatorOptions.Builder()
                        .setSourceLanguage(sourceMLKit)
                        .setTargetLanguage(targetMLKit)
                        .build();
                
                Translator translator = Translation.getClient(options);
                
                try {
                    // Perform translation
                    Task<String> translationTask = translator.translate(text);
                    String translatedText = Tasks.await(translationTask, TRANSLATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    
                    if (translatedText != null && !translatedText.isEmpty()) {
                        callback.onTranslationComplete(true, translatedText, null);
                        Log.d(TAG, "Translation successful: " + sourceLanguage + " -> " + targetLanguage);
                    } else {
                        callback.onTranslationComplete(false, null, "Translation returned empty result");
                    }
                    
                } finally {
                    translator.close();
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error during offline translation", e);
                String errorMessage = "Offline translation failed: " + e.getMessage();
                callback.onTranslationComplete(false, null, errorMessage);
            }
        });
    }
    
    /**
     * Downloads a language model.
     *
     * @param languageCode The language code to download
     * @param callback The download progress callback
     */
    public void downloadLanguageModel(String languageCode, ModelDownloadCallback callback) {
        modelManager.downloadModel(languageCode, new OfflineModelManager.DownloadListener() {
            @Override
            public void onProgress(int progress) {
                callback.onDownloadProgress(progress);
            }
            
            @Override
            public void onSuccess() {
                callback.onDownloadComplete(true, null);
            }
            
            @Override
            public void onError(String errorMessage) {
                callback.onDownloadComplete(false, errorMessage);
            }
        });
    }
    
    /**
     * Deletes a language model.
     *
     * @param languageCode The language code to delete
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    public void deleteLanguageModel(String languageCode, Runnable onSuccess, 
                                   java.util.function.Consumer<String> onError) {
        modelManager.deleteModel(languageCode, onSuccess, onError);
    }
    
    /**
     * Checks if the required models are available for translation.
     *
     * @param sourceLanguage The source language code
     * @param targetLanguage The target language code
     * @return True if both models are available
     */
    public boolean areModelsAvailable(String sourceLanguage, String targetLanguage) {
        try {
            boolean sourceAvailable = modelManager.isModelAvailableInMLKit(sourceLanguage);
            boolean targetAvailable = modelManager.isModelAvailableInMLKit(targetLanguage);
            
            Log.d(TAG, "Model availability - Source (" + sourceLanguage + "): " + sourceAvailable + 
                      ", Target (" + targetLanguage + "): " + targetAvailable);
            
            return sourceAvailable && targetAvailable;
        } catch (Exception e) {
            Log.w(TAG, "Error checking model availability", e);
            return false;
        }
    }
    
    /**
     * Checks if a specific language model is available.
     *
     * @param languageCode The language code to check
     * @return True if the model is available
     */
    public boolean isModelAvailable(String languageCode) {
        return modelManager.isModelAvailableInMLKit(languageCode);
    }
    
    /**
     * Gets the offline model manager.
     *
     * @return The OfflineModelManager instance
     */
    public OfflineModelManager getModelManager() {
        return modelManager;
    }
    
    /**
     * Checks if offline translation is supported for the given language pair.
     *
     * @param sourceLanguage The source language code
     * @param targetLanguage The target language code
     * @return True if the language pair is supported by ML Kit
     */
    public boolean isLanguagePairSupported(String sourceLanguage, String targetLanguage) {
        String sourceMLKit = convertToMLKitLanguageCode(sourceLanguage);
        String targetMLKit = convertToMLKitLanguageCode(targetLanguage);
        return sourceMLKit != null && targetMLKit != null;
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
     * Gets available language codes supported by ML Kit.
     *
     * @return Array of supported language codes
     */
    public String[] getSupportedLanguages() {
        return new String[]{
            "af", "sq", "am", "ar", "hy", "az", "eu", "be", "bn", "bg", "ca", "zh", "hr", "cs", 
            "da", "nl", "en", "et", "fi", "fr", "gl", "ka", "de", "el", "gu", "ht", "he", "hi", 
            "hu", "is", "id", "ga", "it", "ja", "kn", "kk", "ko", "ky", "lo", "lv", "lt", "mk", 
            "ms", "ml", "mt", "mr", "mn", "my", "ne", "no", "fa", "pl", "pt", "pa", "ro", "ru", 
            "sr", "sk", "sl", "es", "sw", "sv", "ta", "te", "th", "tr", "uk", "ur", "uz", "vi", 
            "cy", "yi"
        };
    }
    
    /**
     * Cleans up resources.
     */
    public void cleanup() {
        if (modelManager != null) {
            modelManager.cleanup();
        }
        
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
        
        Log.d(TAG, "OfflineTranslationService cleaned up");
    }
}