package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service for offline language detection using Google ML Kit Language Identification.
 * Provides accurate offline language detection capabilities.
 */
public class OfflineLanguageDetectionService {
    private static final String TAG = "OfflineLanguageDetection";
    private static final int DETECTION_TIMEOUT_SECONDS = 10;
    private static final float CONFIDENCE_THRESHOLD = 0.5f;

    private final Context context;
    private final LanguageIdentifier languageIdentifier;

    /**
     * Interface for language detection callbacks.
     */
    public interface LanguageDetectionCallback {
        void onDetectionComplete(boolean success, String languageCode, float confidence, String errorMessage);
    }

    /**
     * Creates a new OfflineLanguageDetectionService.
     *
     * @param context The application context
     */
    public OfflineLanguageDetectionService(Context context) {
        this.context = context.getApplicationContext();
        this.languageIdentifier = LanguageIdentification.getClient();
    }

    /**
     * Detects the language of the given text using ML Kit.
     *
     * @param text The text to detect language for
     * @return The detected language code, or null if detection failed
     */
    public String detectLanguage(String text) {
        if (text == null || text.trim().isEmpty()) {
            Log.d(TAG, "Empty text provided for language detection");
            return null;
        }

        try {
            Task<String> detectionTask = languageIdentifier.identifyLanguage(text);
            
            // Wait for the detection to complete with timeout
            String result = Tasks.await(detectionTask, DETECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            // Check if the result is undetermined 
            if ("und".equals(result)) {
                Log.d(TAG, "Language detection returned undetermined");
                return null;
            }
            
            Log.d(TAG, "Language detected: " + result + " for text: " + text.substring(0, Math.min(50, text.length())));
            return result;
            
        } catch (TimeoutException e) {
            Log.w(TAG, "Language detection timeout", e);
            return null;
        } catch (ExecutionException e) {
            Log.e(TAG, "Language detection execution error", e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Language detection interrupted", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during language detection", e);
            return null;
        }
    }

    /**
     * Detects the language of the given text asynchronously.
     *
     * @param text The text to detect language for
     * @param callback The callback to receive the result
     */
    public void detectLanguageAsync(String text, LanguageDetectionCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            if (callback != null) {
                callback.onDetectionComplete(false, null, 0.0f, "Empty text provided");
            }
            return;
        }

        languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener(languageCode -> {
                    if ("und".equals(languageCode)) {
                        Log.d(TAG, "Language detection returned undetermined");
                        if (callback != null) {
                            callback.onDetectionComplete(false, null, 0.0f, "Language could not be determined");
                        }
                    } else {
                        Log.d(TAG, "Language detected: " + languageCode);
                        if (callback != null) {
                            // ML Kit doesn't provide confidence score directly for identifyLanguage
                            // We assume high confidence if a specific language is returned
                            callback.onDetectionComplete(true, languageCode, 0.8f, null);
                        }
                    }
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Language detection failed", exception);
                    if (callback != null) {
                        callback.onDetectionComplete(false, null, 0.0f, exception.getMessage());
                    }
                });
    }

    /**
     * Detects the language with confidence information using possible languages.
     *
     * @param text The text to detect language for
     * @param callback The callback to receive the result with confidence
     */
    public void detectLanguageWithConfidence(String text, LanguageDetectionCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            if (callback != null) {
                callback.onDetectionComplete(false, null, 0.0f, "Empty text provided");
            }
            return;
        }

        languageIdentifier.identifyPossibleLanguages(text)
                .addOnSuccessListener(identifiedLanguages -> {
                    if (identifiedLanguages.isEmpty()) {
                        Log.d(TAG, "No languages identified");
                        if (callback != null) {
                            callback.onDetectionComplete(false, null, 0.0f, "No languages identified");
                        }
                        return;
                    }

                    // Get the most confident language
                    var mostConfidentLanguage = identifiedLanguages.get(0);
                    String languageCode = mostConfidentLanguage.getLanguageTag();
                    float confidence = mostConfidentLanguage.getConfidence();

                    Log.d(TAG, "Language detected with confidence: " + languageCode + " (confidence: " + confidence + ")");

                    // Check if confidence meets threshold
                    if (confidence >= CONFIDENCE_THRESHOLD) {
                        if (callback != null) {
                            callback.onDetectionComplete(true, languageCode, confidence, null);
                        }
                    } else {
                        if (callback != null) {
                            callback.onDetectionComplete(false, languageCode, confidence, "Low confidence detection");
                        }
                    }
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Language detection with confidence failed", exception);
                    if (callback != null) {
                        callback.onDetectionComplete(false, null, 0.0f, exception.getMessage());
                    }
                });
    }

    /**
     * Checks if the service is available.
     *
     * @return true if ML Kit Language Identification is available
     */
    public boolean isAvailable() {
        return languageIdentifier != null;
    }

    /**
     * Gets the confidence threshold used for language detection.
     *
     * @return The confidence threshold
     */
    public float getConfidenceThreshold() {
        return CONFIDENCE_THRESHOLD;
    }

    /**
     * Cleanup resources.
     */
    public void cleanup() {
        if (languageIdentifier != null) {
            languageIdentifier.close();
        }
        Log.d(TAG, "OfflineLanguageDetectionService cleanup complete");
    }
}