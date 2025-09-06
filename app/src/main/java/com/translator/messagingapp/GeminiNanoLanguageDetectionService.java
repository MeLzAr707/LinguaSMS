package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service for language detection using Gemini Nano.
 * Provides offline language detection capabilities using GenAI.
 * Replaces ML Kit language detection with Gemini Nano for better offline GenAI features.
 */
public class GeminiNanoLanguageDetectionService {
    private static final String TAG = "GeminiNanoLanguageDetection";
    private static final float MIN_CONFIDENCE_THRESHOLD = 0.5f;
    private static final int DETECTION_TIMEOUT_SECONDS = 10;

    private final Context context;
    private final GeminiNanoModelManager modelManager;
    private final ExecutorService executorService;
    private final GoogleTranslationService onlineService;

    /**
     * Interface for language detection callbacks.
     */
    public interface LanguageDetectionCallback {
        void onDetectionComplete(String languageCode, float confidence);
        void onDetectionError(String error);
    }

    /**
     * Creates a new GeminiNanoLanguageDetectionService.
     *
     * @param context The application context
     * @param onlineService The online service for fallback (can be null)
     */
    public GeminiNanoLanguageDetectionService(Context context, GoogleTranslationService onlineService) {
        this.context = context;
        this.onlineService = onlineService;
        this.modelManager = new GeminiNanoModelManager(context);
        this.executorService = Executors.newFixedThreadPool(2);
        
        Log.d(TAG, "GeminiNanoLanguageDetectionService initialized");
    }

    /**
     * Detects language using Gemini Nano asynchronously.
     */
    public void detectLanguage(String text, LanguageDetectionCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            callback.onDetectionError("Text is empty or null");
            return;
        }

        executorService.execute(() -> {
            try {
                // Try Gemini Nano detection first
                detectLanguageWithGeminiNano(text, callback);
            } catch (Exception e) {
                Log.e(TAG, "Error in language detection", e);
                callback.onDetectionError("Detection failed: " + e.getMessage());
            }
        });
    }

    /**
     * Detects language using Gemini Nano synchronously.
     */
    public String detectLanguage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        try {
            return detectLanguageWithGeminiNanoSync(text);
        } catch (Exception e) {
            Log.w(TAG, "Gemini Nano language detection failed", e);
            return null;
        }
    }

    /**
     * Detects language with confidence using Gemini Nano asynchronously.
     */
    public void detectLanguageWithConfidence(String text, LanguageDetectionCallback callback) {
        detectLanguage(text, callback);
    }

    /**
     * Detects language using Gemini Nano.
     */
    private void detectLanguageWithGeminiNano(String text, LanguageDetectionCallback callback) {
        try {
            if (!modelManager.isGeminiNanoModelAvailable()) {
                Log.w(TAG, "Gemini Nano model not available, falling back to online detection");
                detectLanguageWithOnlineFallback(text, callback);
                return;
            }

            String languageCode = detectLanguageWithGeminiNanoSync(text);
            
            if (languageCode != null && !languageCode.equals("und")) {
                // Gemini Nano detection successful
                callback.onDetectionComplete(languageCode, 0.9f); // High confidence for Gemini Nano
                Log.d(TAG, "Gemini Nano detected language: " + languageCode);
            } else {
                Log.w(TAG, "Gemini Nano language detection uncertain, falling back to online detection");
                detectLanguageWithOnlineFallback(text, callback);
            }
        } catch (Exception e) {
            Log.w(TAG, "Gemini Nano language detection failed, falling back to online detection", e);
            detectLanguageWithOnlineFallback(text, callback);
        }
    }

    /**
     * Detects language using Gemini Nano synchronously.
     */
    private String detectLanguageWithGeminiNanoSync(String text) {
        try {
            if (!modelManager.isGeminiNanoModelAvailable()) {
                return null;
            }

            // Create language detection prompt for Gemini Nano
            String prompt = createLanguageDetectionPrompt(text);
            
            // Use Gemini Nano to detect language
            String response = modelManager.generateResponse(prompt);
            
            // Parse the response to get language code
            return parseLanguageDetectionResponse(response);
        } catch (Exception e) {
            Log.w(TAG, "Gemini Nano language detection failed", e);
            return null;
        }
    }

    /**
     * Creates a language detection prompt for Gemini Nano.
     */
    private String createLanguageDetectionPrompt(String text) {
        return String.format(
            "Detect the language of the following text. " +
            "Respond with only the ISO 639-1 language code (e.g., 'en' for English, 'es' for Spanish, 'fr' for French). " +
            "If the language cannot be determined, respond with 'und'.\n\n" +
            "Text to analyze: %s",
            text
        );
    }

    /**
     * Parses the language detection response from Gemini Nano.
     */
    private String parseLanguageDetectionResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "und";
        }

        // Extract language code from response
        String trimmedResponse = response.trim().toLowerCase();
        
        // Check if it's a valid language code
        if (trimmedResponse.length() == 2 && trimmedResponse.matches("[a-z]{2}")) {
            return trimmedResponse;
        }
        
        // Try to extract language code from longer response
        String[] words = trimmedResponse.split("\\s+");
        for (String word : words) {
            if (word.length() == 2 && word.matches("[a-z]{2}")) {
                return word;
            }
        }
        
        return "und";
    }

    /**
     * Falls back to online language detection.
     */
    private void detectLanguageWithOnlineFallback(String text, LanguageDetectionCallback callback) {
        if (onlineService != null && onlineService.hasApiKey()) {
            try {
                // Use online service for detection
                onlineService.detectLanguage(text, new GoogleTranslationService.LanguageDetectionCallback() {
                    @Override
                    public void onDetectionComplete(String languageCode, float confidence) {
                        Log.d(TAG, "Online language detection: " + languageCode + " (confidence: " + confidence + ")");
                        callback.onDetectionComplete(languageCode, confidence);
                    }

                    @Override
                    public void onDetectionError(String error) {
                        Log.w(TAG, "Online language detection failed: " + error);
                        // Final fallback - assume English for offline-only mode
                        callback.onDetectionComplete("en", 0.3f);
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "Online detection failed", e);
                callback.onDetectionComplete("en", 0.3f);
            }
        } else {
            Log.d(TAG, "No online service available, assuming English");
            callback.onDetectionComplete("en", 0.3f);
        }
    }

    /**
     * Checks if language detection is available (Gemini Nano is always available when model is loaded).
     */
    public boolean isLanguageDetectionAvailable() {
        return modelManager.isGeminiNanoModelAvailable();
    }

    /**
     * Checks if online detection is available.
     */
    public boolean isOnlineDetectionAvailable() {
        return onlineService != null && onlineService.hasApiKey();
    }

    /**
     * Gets the minimum confidence threshold used by Gemini Nano.
     */
    public float getMinConfidenceThreshold() {
        return MIN_CONFIDENCE_THRESHOLD;
    }

    /**
     * Gets the model status.
     */
    public String getModelStatus() {
        return modelManager.getModelStatus();
    }

    /**
     * Downloads the Gemini Nano model if not available.
     */
    public void ensureModelAvailable(GeminiNanoModelManager.DownloadListener listener) {
        if (modelManager.isGeminiNanoModelAvailable()) {
            if (listener != null) {
                listener.onSuccess();
            }
            return;
        }

        Log.d(TAG, "Downloading Gemini Nano model for language detection");
        modelManager.downloadGeminiNanoModel(listener);
    }

    /**
     * Cleanup resources when done.
     */
    public void cleanup() {
        try {
            if (modelManager != null) {
                modelManager.cleanup();
            }
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            }
            Log.d(TAG, "GeminiNanoLanguageDetectionService cleanup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }
}