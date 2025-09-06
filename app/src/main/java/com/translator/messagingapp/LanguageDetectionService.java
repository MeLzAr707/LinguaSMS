package com.translator.messagingapp;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.nl.languageid.IdentifiedLanguage;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Service for detecting the language of text using ML Kit with Google API fallback.
 * Provides both synchronous and asynchronous language detection capabilities.
 */
public class LanguageDetectionService {
    private static final String TAG = "LanguageDetectionService";
    
    // Detection configuration
    private static final float MIN_CONFIDENCE_THRESHOLD = 0.5f;
    private static final int DETECTION_TIMEOUT_SECONDS = 10;
    
    public enum DetectionMethod {
        ML_KIT, ONLINE_API, FALLBACK
    }
    
    public interface LanguageDetectionCallback {
        void onDetectionComplete(boolean success, String languageCode, String errorMessage, DetectionMethod method);
    }
    
    private final Context context;
    private final GoogleTranslationService googleService;
    private final LanguageIdentifier mlkitIdentifier;
    private float minConfidenceThreshold;
    
    /**
     * Creates a new LanguageDetectionService.
     *
     * @param context The application context
     * @param googleService The Google translation service for fallback
     */
    public LanguageDetectionService(Context context, GoogleTranslationService googleService) {
        this.context = context;
        this.googleService = googleService;
        this.mlkitIdentifier = LanguageIdentification.getClient();
        this.minConfidenceThreshold = MIN_CONFIDENCE_THRESHOLD;
        
        Log.d(TAG, "LanguageDetectionService initialized with ML Kit and online fallback");
    }
    
    /**
     * Gets the minimum confidence threshold for ML Kit detection.
     *
     * @return The minimum confidence threshold
     */
    public float getMinConfidenceThreshold() {
        return minConfidenceThreshold;
    }
    
    /**
     * Sets the minimum confidence threshold for ML Kit detection.
     *
     * @param threshold The minimum confidence threshold (0.0 to 1.0)
     */
    public void setMinConfidenceThreshold(float threshold) {
        this.minConfidenceThreshold = Math.max(0.0f, Math.min(1.0f, threshold));
    }
    
    /**
     * Checks if online detection is available.
     *
     * @return True if online detection is available
     */
    public boolean isOnlineDetectionAvailable() {
        return googleService != null && googleService.hasApiKey();
    }
    
    /**
     * Detects the language of the given text synchronously.
     * Uses ML Kit first, falls back to online detection if needed.
     *
     * @param text The text to detect language for
     * @return The detected language code, or null if detection failed
     */
    public String detectLanguageSync(String text) {
        if (TextUtils.isEmpty(text)) {
            Log.w(TAG, "Empty text provided for language detection");
            return null;
        }
        
        // Try ML Kit detection first
        try {
            String mlkitResult = detectWithMLKitSync(text);
            if (mlkitResult != null) {
                Log.d(TAG, "ML Kit detected language: " + mlkitResult);
                return mlkitResult;
            }
        } catch (Exception e) {
            Log.w(TAG, "ML Kit detection failed: " + e.getMessage());
        }
        
        // Fallback to online detection if available
        if (isOnlineDetectionAvailable()) {
            try {
                String onlineResult = googleService.detectLanguage(text);
                Log.d(TAG, "Online detection result: " + onlineResult);
                return onlineResult;
            } catch (Exception e) {
                Log.w(TAG, "Online detection failed: " + e.getMessage());
            }
        }
        
        Log.w(TAG, "All language detection methods failed");
        return null;
    }
    
    /**
     * Detects the language of the given text asynchronously.
     *
     * @param text The text to detect language for
     * @param callback The callback to receive the result
     */
    public void detectLanguage(String text, LanguageDetectionCallback callback) {
        if (TextUtils.isEmpty(text)) {
            callback.onDetectionComplete(false, null, "Empty text provided", DetectionMethod.FALLBACK);
            return;
        }
        
        // Try ML Kit detection first
        Task<String> identifyTask = mlkitIdentifier.identifyLanguage(text);
        
        identifyTask.addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String languageCode) {
                if (!LanguageIdentification.UNDETERMINED_LANGUAGE_TAG.equals(languageCode)) {
                    // ML Kit succeeded
                    Log.d(TAG, "ML Kit detected language: " + languageCode);
                    callback.onDetectionComplete(true, languageCode, null, DetectionMethod.ML_KIT);
                } else {
                    // ML Kit returned undetermined, try online fallback
                    tryOnlineFallback(text, callback);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.w(TAG, "ML Kit detection failed: " + e.getMessage());
                // ML Kit failed, try online fallback
                tryOnlineFallback(text, callback);
            }
        });
    }
    
    /**
     * Attempts online detection as fallback.
     */
    private void tryOnlineFallback(String text, LanguageDetectionCallback callback) {
        if (isOnlineDetectionAvailable()) {
            try {
                // Use online detection
                String result = googleService.detectLanguage(text);
                if (result != null) {
                    Log.d(TAG, "Online fallback detected language: " + result);
                    callback.onDetectionComplete(true, result, null, DetectionMethod.ONLINE_API);
                } else {
                    callback.onDetectionComplete(false, null, "Online detection returned null", DetectionMethod.FALLBACK);
                }
            } catch (Exception e) {
                Log.w(TAG, "Online fallback failed: " + e.getMessage());
                callback.onDetectionComplete(false, null, "All detection methods failed: " + e.getMessage(), DetectionMethod.FALLBACK);
            }
        } else {
            callback.onDetectionComplete(false, null, "No online detection available and ML Kit failed", DetectionMethod.FALLBACK);
        }
    }
    
    /**
     * Detects language using ML Kit synchronously with confidence checking.
     */
    private String detectWithMLKitSync(String text) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] result = {null};
        final Exception[] exception = {null};
        
        Task<List<IdentifiedLanguage>> task = mlkitIdentifier.identifyPossibleLanguages(text);
        
        task.addOnSuccessListener(new OnSuccessListener<List<IdentifiedLanguage>>() {
            @Override
            public void onSuccess(List<IdentifiedLanguage> identifiedLanguages) {
                try {
                    if (identifiedLanguages != null && !identifiedLanguages.isEmpty()) {
                        IdentifiedLanguage topLanguage = identifiedLanguages.get(0);
                        if (topLanguage.getConfidence() >= minConfidenceThreshold) {
                            result[0] = topLanguage.getLanguageTag();
                            Log.d(TAG, "ML Kit detected: " + result[0] + " (confidence: " + topLanguage.getConfidence() + ")");
                        } else {
                            Log.d(TAG, "ML Kit confidence too low: " + topLanguage.getConfidence() + " < " + minConfidenceThreshold);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                exception[0] = e;
                latch.countDown();
            }
        });
        
        // Wait for completion with timeout
        if (!latch.await(DETECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new Exception("ML Kit detection timed out");
        }
        
        if (exception[0] != null) {
            throw exception[0];
        }
        
        return result[0];
    }
    
    /**
     * Cleans up resources.
     */
    public void cleanup() {
        if (mlkitIdentifier != null) {
            try {
                mlkitIdentifier.close();
                Log.d(TAG, "ML Kit identifier closed");
            } catch (Exception e) {
                Log.w(TAG, "Error closing ML Kit identifier: " + e.getMessage());
            }
        }
    }
}