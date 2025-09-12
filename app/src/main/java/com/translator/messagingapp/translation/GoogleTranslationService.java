package com.translator.messagingapp.translation;

import com.translator.messagingapp.message.*;

import com.translator.messagingapp.translation.*;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Service for translating text using Google Cloud Translation API.
 */
public class GoogleTranslationService {
    private static final String TAG = "GoogleTranslationService";
    private static final String API_URL = "https://translation.googleapis.com/language/translate/v2";

    private String apiKey;
    private final ExecutorService executorService;

    /**
     * Default constructor.
     */
    public GoogleTranslationService() {
        this.apiKey = null;
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * Constructor with API key.
     *
     * @param apiKey The Google Cloud Translation API key
     */
    public GoogleTranslationService(String apiKey) {
        this.apiKey = apiKey;
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * Sets the API key.
     *
     * @param apiKey The Google Cloud Translation API key
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Gets the API key.
     *
     * @return The API key
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Checks if the service has a valid API key.
     *
     * @return true if the API key is set and not empty, false otherwise
     */
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isEmpty();
    }

    /**
     * Translates text from one language to another.
     *
     * @param text The text to translate
     * @param sourceLanguage The source language code (e.g., "en" for English)
     * @param targetLanguage The target language code (e.g., "es" for Spanish)
     * @return The translated text, or null if translation failed
     */
    public String translate(String text, String sourceLanguage, String targetLanguage) {
        if (!hasApiKey()) {
            Log.e(TAG, "API key not set");
            return null;
        }

        if (text == null || text.isEmpty()) {
            return text;
        }

        try {
            URL url = new URL(API_URL + "?key=" + apiKey);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            // Create request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("q", text);
            requestBody.put("target", targetLanguage);

            if (sourceLanguage != null && !sourceLanguage.isEmpty()) {
                requestBody.put("source", sourceLanguage);
            }

            // Write request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Read response
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    // Parse response
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONObject data = jsonResponse.getJSONObject("data");
                    JSONArray translations = data.getJSONArray("translations");
                    JSONObject translation = translations.getJSONObject(0);
                    return translation.getString("translatedText");
                }
            } else {
                Log.e(TAG, "Translation API error: " + responseCode);
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    Log.e(TAG, "Error response: " + response.toString());
                }
                return null;
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Translation error", e);
            return null;
        }
    }

    /**
     * Translates text asynchronously.
     *
     * @param text The text to translate
     * @param sourceLanguage The source language code
     * @param targetLanguage The target language code
     * @return A Future that will contain the translated text
     */
    public Future<String> translateAsync(final String text, final String sourceLanguage, final String targetLanguage) {
        return executorService.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return translate(text, sourceLanguage, targetLanguage);
            }
        });
    }

    /**
     * Detects the language of the given text.
     * 
     * Detects the language of the given text using Google Translate API.
     *
     * @param text The text to detect language for
     * @return The detected language code, or null if detection failed
     */
    public String detectLanguage(String text) {
        if (!hasApiKey()) {
            Log.e(TAG, "API key not set");
            return null;
        }

        if (text == null || text.isEmpty()) {
            return null;
        }

        try {
            URL url = new URL("https://translation.googleapis.com/language/translate/v2/detect?key=" + apiKey);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            // Create request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("q", text);

            // Write request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Read response
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    // Parse response
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONObject data = jsonResponse.getJSONObject("data");
                    JSONArray detections = data.getJSONArray("detections");
                    JSONArray detection = detections.getJSONArray(0);
                    JSONObject detectionData = detection.getJSONObject(0);
                    return detectionData.getString("language");
                }
            } else {
                Log.e(TAG, "Language detection API error: " + responseCode);
                return null;
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Language detection error", e);
            return null;
        }
    }

    /**
     * Detects language asynchronously using Google Translate API.
     * 
     * @param text The text to detect language for
     * @return A Future that will contain the detected language code
     */
    public Future<String> detectLanguageAsync(final String text) {
        return executorService.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return detectLanguage(text);
            }
        });
    }

    /**
     * Detects language asynchronously with callback.
     * 
     * NOTE: This method is now primarily used as a fallback when ML Kit
     * language detection fails. The primary language detection is handled
     * by OfflineLanguageDetectionService using ML Kit.
     *
     * @param text The text to detect language for
     * @param callback The callback to receive the result
     */
    public void detectLanguage(String text, LanguageDetectionCallback callback) {
        if (callback == null) {
            return;
        }

        executorService.execute(() -> {
            try {
                String languageCode = detectLanguage(text);
                if (languageCode != null) {
                    callback.onDetectionComplete(languageCode, 0.8f); // Default confidence
                } else {
                    callback.onDetectionError("Language detection failed");
                }
            } catch (Exception e) {
                callback.onDetectionError("Language detection error: " + e.getMessage());
            }
        });
    }

    /**
     * Gets the display name of a language in the current locale.
     *
     * @param languageCode The language code (e.g., "en", "es")
     * @return The display name of the language, or the code if not found
     */
    public String getLanguageDisplayName(String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            return "";
        }

        try {
            Locale locale = new Locale(languageCode);
            return locale.getDisplayLanguage();
        } catch (Exception e) {
            Log.e(TAG, "Error getting language display name", e);
            return languageCode;
        }
    }

    /**
     * Tests if the API key is valid by making a simple translation request.
     *
     * @return true if the API key is valid, false otherwise
     */
    public boolean testApiKey() {
        if (!hasApiKey()) {
            return false;
        }

        try {
            String result = translate("hello", "en", "es");
            return result != null && !result.isEmpty();
        } catch (Exception e) {
            Log.e(TAG, "API key test failed", e);
            return false;
        }
    }

    /**
     * Tests if the API key is valid by making a simple translation request.
     * The result is returned via a callback.
     *
     * @param callback The callback to receive the test result
     */
    public void testApiKey(ApiKeyTestCallback callback) {
        if (callback == null) {
            return;
        }

        executorService.execute(() -> {
            boolean isValid = testApiKey();
            callback.onTestComplete(isValid);
        });
    }

    /**
     * Interface for API key test callbacks.
     */
    public interface ApiKeyTestCallback {
        /**
         * Called when the API key test is complete.
         *
         * @param isValid true if the API key is valid, false otherwise
         */
        void onTestComplete(boolean isValid);
    }

    /**
     * Interface for language detection callbacks.
     */
    public interface LanguageDetectionCallback {
        /**
         * Called when language detection is complete.
         *
         * @param languageCode The detected language code
         * @param confidence The confidence level (0.0 to 1.0)
         */
        void onDetectionComplete(String languageCode, float confidence);

        /**
         * Called when language detection fails.
         *
         * @param error The error message
         */
        void onDetectionError(String error);
    }

    /**
     * Shuts down the executor service.
     */
    public void shutdown() {
        executorService.shutdown();
    }
}






