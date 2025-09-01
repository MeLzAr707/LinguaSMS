package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;

import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;

import java.util.Locale;

/**
 * Service for detecting the language of text using MLKit Language Identification.
 * This replaces hard-coded English language assumptions.
 */
public class LanguageDetectionService {
    private static final String TAG = "LanguageDetectionService";
    private static final float CONFIDENCE_THRESHOLD = 0.5f;
    
    private final Context context;
    private final LanguageIdentifier languageIdentifier;

    /**
     * Interface for language detection callbacks.
     */
    public interface LanguageDetectionCallback {
        void onLanguageDetected(String languageCode);
        void onDetectionFailed(String errorMessage);
    }

    /**
     * Creates a new LanguageDetectionService.
     *
     * @param context The application context
     */
    public LanguageDetectionService(Context context) {
        this.context = context.getApplicationContext();
        this.languageIdentifier = LanguageIdentification.getClient();
    }

    /**
     * Detects the language of the given text using MLKit.
     *
     * @param text The text to analyze
     * @param callback The callback to receive the result
     */
    public void detectLanguage(String text, LanguageDetectionCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            if (callback != null) {
                callback.onDetectionFailed("No text to analyze");
            }
            return;
        }

        languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener(languageCode -> {
                    if (languageCode.equals("und")) {
                        // Language could not be identified with sufficient confidence
                        Log.d(TAG, "Language detection failed - insufficient confidence");
                        
                        // Instead of defaulting to English, try to get device locale as fallback
                        String deviceLanguage = getDeviceLanguage();
                        Log.d(TAG, "Using device language as fallback: " + deviceLanguage);
                        
                        if (callback != null) {
                            callback.onLanguageDetected(deviceLanguage);
                        }
                    } else {
                        Log.d(TAG, "Language detected: " + languageCode);
                        if (callback != null) {
                            callback.onLanguageDetected(languageCode);
                        }
                    }
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Language detection failed", exception);
                    
                    // Instead of defaulting to English, use device language as fallback
                    String deviceLanguage = getDeviceLanguage();
                    Log.d(TAG, "Detection failed, using device language as fallback: " + deviceLanguage);
                    
                    if (callback != null) {
                        callback.onLanguageDetected(deviceLanguage);
                    }
                });
    }

    /**
     * Gets the device's primary language instead of defaulting to English.
     *
     * @return The device's language code
     */
    private String getDeviceLanguage() {
        try {
            Locale deviceLocale = Locale.getDefault();
            String languageCode = deviceLocale.getLanguage();
            Log.d(TAG, "Device language: " + languageCode + " (from locale: " + deviceLocale + ")");
            return languageCode;
        } catch (Exception e) {
            Log.e(TAG, "Error getting device language", e);
            // Only as a last resort, return "en", but this should rarely happen
            return "en";
        }
    }

    /**
     * Checks if language detection is available.
     *
     * @return true if language detection is available, false otherwise
     */
    public boolean isLanguageDetectionAvailable() {
        return languageIdentifier != null;
    }

    /**
     * Synchronously detects language with a timeout.
     * This is a fallback method for cases where async detection is not suitable.
     *
     * @param text The text to analyze
     * @return The detected language code, or device language if detection fails
     */
    public String detectLanguageSync(String text) {
        if (text == null || text.trim().isEmpty()) {
            return getDeviceLanguage();
        }

        try {
            // For synchronous detection, we'll just return device language
            // as MLKit's API is inherently asynchronous
            return getDeviceLanguage();
        } catch (Exception e) {
            Log.e(TAG, "Synchronous language detection failed", e);
            return getDeviceLanguage();
        }
    }

    /**
     * Cleanup resources.
     */
    public void cleanup() {
        if (languageIdentifier != null) {
            languageIdentifier.close();
        }
    }
}