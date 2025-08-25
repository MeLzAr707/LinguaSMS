package com.translator.messagingapp;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

/**
 * Manager for Text-to-Speech functionality.
 */
public class TTSManager {
    private static final String TAG = "TTSManager";
    
    private final Context context;
    private final UserPreferences userPreferences;
    private TextToSpeech textToSpeech;
    private boolean isInitialized = false;
    
    /**
     * Creates a new TTSManager.
     *
     * @param context The application context
     * @param userPreferences The user preferences instance
     */
    public TTSManager(Context context, UserPreferences userPreferences) {
        this.context = context;
        this.userPreferences = userPreferences;
        initializeTTS();
    }
    
    /**
     * Initializes the Text-to-Speech engine.
     */
    private void initializeTTS() {
        try {
            textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS) {
                        isInitialized = true;
                        configureLanguage();
                        configureSpeechRate();
                        Log.d(TAG, "TTS initialized successfully");
                    } else {
                        Log.e(TAG, "TTS initialization failed with status: " + status);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing TTS", e);
        }
    }
    
    /**
     * Configures the TTS language based on user preferences.
     */
    private void configureLanguage() {
        if (textToSpeech != null && userPreferences != null) {
            String languageCode = userPreferences.getTTSLanguage();
            Locale locale = new Locale(languageCode);
            int result = textToSpeech.setLanguage(locale);
            
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Language " + languageCode + " not supported, using default");
                textToSpeech.setLanguage(Locale.getDefault());
            }
        }
    }
    
    /**
     * Configures the TTS speech rate based on user preferences.
     */
    private void configureSpeechRate() {
        if (textToSpeech != null && userPreferences != null) {
            float speechRate = userPreferences.getTTSSpeechRate();
            textToSpeech.setSpeechRate(speechRate);
        }
    }
    
    /**
     * Speaks the given text.
     *
     * @param text The text to speak
     */
    public void speak(String text) {
        if (!isInitialized || textToSpeech == null) {
            Log.w(TAG, "TTS not initialized, cannot speak text");
            return;
        }
        
        if (!userPreferences.isTTSEnabled()) {
            Log.d(TAG, "TTS is disabled in preferences");
            return;
        }
        
        try {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } catch (Exception e) {
            Log.e(TAG, "Error speaking text", e);
        }
    }
    
    /**
     * Stops TTS if currently speaking.
     */
    public void stop() {
        if (textToSpeech != null && isInitialized) {
            textToSpeech.stop();
        }
    }
    
    /**
     * Checks if TTS is currently speaking.
     *
     * @return True if TTS is speaking, false otherwise
     */
    public boolean isSpeaking() {
        return textToSpeech != null && isInitialized && textToSpeech.isSpeaking();
    }
    
    /**
     * Checks if TTS is enabled in user preferences.
     *
     * @return True if TTS is enabled
     */
    public boolean isEnabled() {
        return userPreferences != null && userPreferences.isTTSEnabled();
    }
    
    /**
     * Updates TTS settings based on current user preferences.
     */
    public void updateSettings() {
        if (isInitialized) {
            configureLanguage();
            configureSpeechRate();
        }
    }
    
    /**
     * Cleans up TTS resources.
     */
    public void cleanup() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
            isInitialized = false;
        }
    }
}