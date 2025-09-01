package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions;
import com.google.mlkit.nl.languageid.LanguageIdentifier;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service for language detection using ML Kit with online fallback.
 * Provides robust language detection with confidence-based fallback mechanism.
 */
public class LanguageDetectionService {
    private static final String TAG = "LanguageDetectionService";
    private static final float MIN_CONFIDENCE_THRESHOLD = 0.5f;
    private static final int DETECTION_TIMEOUT_SECONDS = 5;
    
    private final Context context;
    private final GoogleTranslationService onlineService;
    private final LanguageIdentifier languageIdentifier;
    
    /**
     * Interface for language detection callbacks.
     */
    public interface LanguageDetectionCallback {
        void onDetectionComplete(boolean success, String languageCode, String errorMessage, DetectionMethod method);
    }
    
    /**
     * Enum to track which detection method was used.
     */
    public enum DetectionMethod {
        ML_KIT_ON_DEVICE,
        ONLINE_FALLBACK,
        FAILED
    }
    
    /**
     * Constructor for LanguageDetectionService with context only.
     * Online fallback will not be available.
     *
     * @param context The application context
     */
    public LanguageDetectionService(Context context) {
        this(context, null);
    }
    
    /**
     * Constructor for LanguageDetectionService.
     *
     * @param context The application context
     * @param onlineService The Google Translation service for fallback
     */
    public LanguageDetectionService(Context context, GoogleTranslationService onlineService) {
        this.context = context;
        this.onlineService = onlineService;
        
        // Initialize ML Kit Language Identifier with options
        LanguageIdentificationOptions options = new LanguageIdentificationOptions.Builder()
            .setConfidenceThreshold(MIN_CONFIDENCE_THRESHOLD)
            .build();
        this.languageIdentifier = LanguageIdentification.getClient(options);
    }
    
    /**
     * Detects the language of the given text using ML Kit with online fallback.
     * 
     * @param text The text to detect language for
     * @param callback The callback to receive the result
     */
    public void detectLanguage(String text, LanguageDetectionCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            Log.w(TAG, "Language detection called with empty text");
            if (callback != null) {
                callback.onDetectionComplete(false, null, "Text is empty", DetectionMethod.FAILED);
            }
            return;
        }
        
        // Log the detection attempt for debugging
        Log.d(TAG, "Starting language detection for text: " + text.substring(0, Math.min(text.length(), 50)) + 
              (text.length() > 50 ? "..." : ""));
        
        // Try ML Kit detection first
        detectLanguageWithMLKit(text, callback);
    }
    
    /**
     * Detects language synchronously (blocking call).
     * Use this for cases where you need immediate result.
     * 
     * @param text The text to detect language for
     * @return The detected language code, or null if detection failed
     */
    public String detectLanguageSync(String text) {
        if (text == null || text.trim().isEmpty()) {
            Log.w(TAG, "Synchronous language detection called with empty text");
            return null;
        }
        
        // Log the detection attempt for debugging
        Log.d(TAG, "Starting synchronous language detection for text: " + 
              text.substring(0, Math.min(text.length(), 50)) + (text.length() > 50 ? "..." : ""));
        
        // Try ML Kit first
        String mlkitResult = detectLanguageWithMLKitSync(text);
        if (mlkitResult != null && !mlkitResult.equals("und")) {
            Log.d(TAG, "ML Kit detected language: " + mlkitResult);
            return mlkitResult;
        }
        
        // Fallback to online detection
        if (onlineService != null && onlineService.hasApiKey()) {
            Log.d(TAG, "ML Kit detection failed/undetermined, falling back to online detection");
            String onlineResult = onlineService.detectLanguage(text);
            if (onlineResult != null) {
                Log.d(TAG, "Online detection successful: " + onlineResult);
                return onlineResult;
            }
        }
        
        Log.w(TAG, "Both ML Kit and online detection failed for text: " + 
              text.substring(0, Math.min(text.length(), 20)) + (text.length() > 20 ? "..." : ""));
        return null;
    }
    
    /**
     * Detects language using ML Kit.
     */
    private void detectLanguageWithMLKit(String text, LanguageDetectionCallback callback) {
        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener(languageCode -> {
                if (languageCode.equals("und")) {
                    // ML Kit returned "undetermined", fallback to online
                    Log.d(TAG, "ML Kit returned 'undetermined', falling back to online detection");
                    detectLanguageWithOnlineFallback(text, callback);
                } else {
                    // ML Kit successfully detected language
                    Log.d(TAG, "ML Kit detected language: " + languageCode);
                    if (callback != null) {
                        callback.onDetectionComplete(true, languageCode, null, DetectionMethod.ML_KIT_ON_DEVICE);
                    }
                }
            })
            .addOnFailureListener(e -> {
                // ML Kit failed, fallback to online
                Log.w(TAG, "ML Kit language detection failed, falling back to online detection", e);
                detectLanguageWithOnlineFallback(text, callback);
            });
    }
    
    /**
     * Detects language using ML Kit synchronously.
     */
    private String detectLanguageWithMLKitSync(String text) {
        try {
            String languageCode = Tasks.await(languageIdentifier.identifyLanguage(text), DETECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return languageCode.equals("und") ? null : languageCode;
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            Log.w(TAG, "ML Kit language detection failed", e);
            return null;
        }
    }
    
    /**
     * Falls back to online language detection.
     */
    private void detectLanguageWithOnlineFallback(String text, LanguageDetectionCallback callback) {
        if (onlineService != null && onlineService.hasApiKey()) {
            // Use online detection as fallback
            onlineService.detectLanguageAsync(text)
                .thenAccept(result -> {
                    if (result != null) {
                        Log.d(TAG, "Online detection successful: " + result);
                        if (callback != null) {
                            callback.onDetectionComplete(true, result, null, DetectionMethod.ONLINE_FALLBACK);
                        }
                    } else {
                        Log.e(TAG, "Online detection also failed");
                        if (callback != null) {
                            callback.onDetectionComplete(false, null, "Both ML Kit and online detection failed", DetectionMethod.FAILED);
                        }
                    }
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Online detection error", throwable);
                    if (callback != null) {
                        callback.onDetectionComplete(false, null, "Both ML Kit and online detection failed: " + throwable.getMessage(), DetectionMethod.FAILED);
                    }
                    return null;
                });
        } else {
            // No online service available
            Log.e(TAG, "No online service available for fallback");
            if (callback != null) {
                callback.onDetectionComplete(false, null, "ML Kit detection failed and no online service available", DetectionMethod.FAILED);
            }
        }
    }
    
    /**
     * Checks if language detection is available (ML Kit is always available).
     */
    public boolean isLanguageDetectionAvailable() {
        return languageIdentifier != null;
    }
    
    /**
     * Checks if online detection is available.
     */
    public boolean isOnlineDetectionAvailable() {
        return onlineService != null && onlineService.hasApiKey();
    }
    
    /**
     * Gets the minimum confidence threshold used by ML Kit.
     */
    public float getMinConfidenceThreshold() {
        return MIN_CONFIDENCE_THRESHOLD;
    }
    
    /**
     * Cleanup resources when done.
     */
    public void cleanup() {
        if (languageIdentifier != null) {
            languageIdentifier.close();
        }
    }
}