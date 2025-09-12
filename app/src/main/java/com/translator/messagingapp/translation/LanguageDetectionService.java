package com.translator.messagingapp.translation;

import com.translator.messagingapp.message.*;

import com.translator.messagingapp.translation.*;

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
        void onLanguageDetected(String languageCode);
        void onDetectionFailed(String errorMessage);
    }
    
    /**
     * Advanced callback interface with more detailed information.
     */
    public interface DetailedLanguageDetectionCallback {
        void onDetectionComplete(boolean success, String languageCode, String errorMessage, DetectionMethod method);
    }
    
    private final Context context;
    private final GoogleTranslationService googleService;
    private final LanguageIdentifier mlkitIdentifier;
    private float minConfidenceThreshold;
    
    /**
     * Creates a new LanguageDetectionService with Google service fallback.
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
     * Creates a new LanguageDetectionService with ML Kit only (no online fallback).
     *
     * @param context The application context
     */
    public LanguageDetectionService(Context context) {
        this.context = context;
        this.googleService = null;
        this.mlkitIdentifier = LanguageIdentification.getClient();
        this.minConfidenceThreshold = MIN_CONFIDENCE_THRESHOLD;
        
        Log.d(TAG, "LanguageDetectionService initialized with ML Kit only");
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
     * Checks if language detection is available.
     * ML Kit language detection is always available offline.
     *
     * @return True since ML Kit is always available
     */
    public boolean isLanguageDetectionAvailable() {
        return true;
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
     * Returns device language for null/empty text.
     *
     * @param text The text to detect language for
     * @return The detected language code, or device language if detection failed
     */
    public String detectLanguageSync(String text) {
        if (TextUtils.isEmpty(text)) {
            Log.w(TAG, "Empty text provided for language detection, returning device language");
            return java.util.Locale.getDefault().getLanguage();
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
        
        Log.w(TAG, "All language detection methods failed, returning device language");
        return java.util.Locale.getDefault().getLanguage();
    }
    
    /**
     * Detects the language of the given text asynchronously.
     *
     * @param text The text to detect language for
     * @param callback The callback to receive the result
     */
    public void detectLanguage(String text, LanguageDetectionCallback callback) {
        if (TextUtils.isEmpty(text)) {
            String deviceLanguage = java.util.Locale.getDefault().getLanguage();
            callback.onLanguageDetected(deviceLanguage);
            return;
        }
        
        // Try ML Kit detection first
        Task<String> identifyTask = mlkitIdentifier.identifyLanguage(text);
        
        identifyTask.addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String languageCode) {
                if (!"und".equals(languageCode)) {
                    // ML Kit succeeded
                    Log.d(TAG, "ML Kit detected language: " + languageCode);
                    callback.onLanguageDetected(languageCode);
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
     * Detects the language of the given text asynchronously using detailed callback interface.
     *
     * @param text The text to detect language for
     * @param callback The detailed callback to receive the result
     */
    public void detectLanguage(String text, DetailedLanguageDetectionCallback callback) {
        detectLanguage(text, new LanguageDetectionCallback() {
            @Override
            public void onLanguageDetected(String languageCode) {
                callback.onDetectionComplete(true, languageCode, null, DetectionMethod.ML_KIT);
            }
            
            @Override
            public void onDetectionFailed(String errorMessage) {
                String deviceLanguage = java.util.Locale.getDefault().getLanguage();
                callback.onDetectionComplete(true, deviceLanguage, errorMessage, DetectionMethod.FALLBACK);
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
                    callback.onLanguageDetected(result);
                } else {
                    String deviceLanguage = java.util.Locale.getDefault().getLanguage();
                    Log.d(TAG, "Online detection returned null, using device language: " + deviceLanguage);
                    callback.onLanguageDetected(deviceLanguage);
                }
            } catch (Exception e) {
                Log.w(TAG, "Online fallback failed: " + e.getMessage());
                String deviceLanguage = java.util.Locale.getDefault().getLanguage();
                callback.onLanguageDetected(deviceLanguage);
            }
        } else {
            String deviceLanguage = java.util.Locale.getDefault().getLanguage();
            Log.d(TAG, "No online detection available, using device language: " + deviceLanguage);
            callback.onLanguageDetected(deviceLanguage);
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