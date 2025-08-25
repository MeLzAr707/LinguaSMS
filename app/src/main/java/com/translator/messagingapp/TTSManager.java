package com.translator.messagingapp;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Manager class for handling Text-to-Speech functionality.
 * Provides speech synthesis with configurable speed and accent selection.
 */
public class TTSManager {
    private static final String TAG = "TTSManager";
    
    private Context context;
    private TextToSpeech textToSpeech;
    private UserPreferences userPreferences;
    private boolean isInitialized = false;
    private TTSInitializationListener initializationListener;
    
    /**
     * Interface for TTS initialization callbacks.
     */
    public interface TTSInitializationListener {
        void onInitialized(boolean success);
    }
    
    /**
     * Interface for TTS playback callbacks.
     */
    public interface TTSPlaybackListener {
        void onStart();
        void onDone();
        void onError();
    }
    
    public TTSManager(Context context, UserPreferences userPreferences) {
        this.context = context;
        this.userPreferences = userPreferences;
        initializeTTS();
    }
    
    /**
     * Initialize the TextToSpeech engine.
     */
    private void initializeTTS() {
        textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    Log.d(TAG, "TTS initialized successfully");
                    isInitialized = true;
                    
                    // Set default language
                    setLanguage(userPreferences.getTTSLanguage());
                    
                    // Set default speech rate
                    setSpeechRate(userPreferences.getTTSSpeechRate());
                    
                    // Set utterance progress listener
                    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            Log.d(TAG, "TTS started for utterance: " + utteranceId);
                        }
                        
                        @Override
                        public void onDone(String utteranceId) {
                            Log.d(TAG, "TTS completed for utterance: " + utteranceId);
                        }
                        
                        @Override
                        public void onError(String utteranceId) {
                            Log.e(TAG, "TTS error for utterance: " + utteranceId);
                        }
                    });
                    
                    if (initializationListener != null) {
                        initializationListener.onInitialized(true);
                    }
                } else {
                    Log.e(TAG, "TTS initialization failed with status: " + status);
                    isInitialized = false;
                    if (initializationListener != null) {
                        initializationListener.onInitialized(false);
                    }
                }
            }
        });
    }
    
    /**
     * Set the initialization listener.
     */
    public void setInitializationListener(TTSInitializationListener listener) {
        this.initializationListener = listener;
    }
    
    /**
     * Check if TTS is initialized and ready to use.
     */
    public boolean isInitialized() {
        return isInitialized && textToSpeech != null;
    }
    
    /**
     * Speak the given text.
     * 
     * @param text The text to speak
     * @param languageCode The language code for the text (e.g., "en", "es")
     * @param listener Optional playback listener
     */
    public void speak(String text, String languageCode, TTSPlaybackListener listener) {
        if (!isInitialized()) {
            Log.w(TAG, "TTS not initialized, cannot speak text");
            if (listener != null) {
                listener.onError();
            }
            return;
        }
        
        if (text == null || text.trim().isEmpty()) {
            Log.w(TAG, "Empty text provided, cannot speak");
            if (listener != null) {
                listener.onError();
            }
            return;
        }
        
        // Set language for this utterance
        if (languageCode != null) {
            setLanguage(languageCode);
        }
        
        // Create parameters for the utterance
        String utteranceId = "tts_" + System.currentTimeMillis();
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
        
        // Set up custom utterance progress listener for this specific utterance
        if (listener != null) {
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String id) {
                    if (id.equals(utteranceId)) {
                        listener.onStart();
                    }
                }
                
                @Override
                public void onDone(String id) {
                    if (id.equals(utteranceId)) {
                        listener.onDone();
                    }
                }
                
                @Override
                public void onError(String id) {
                    if (id.equals(utteranceId)) {
                        listener.onError();
                    }
                }
            });
        }
        
        // Speak the text
        int result = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params);
        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "Failed to start TTS");
            if (listener != null) {
                listener.onError();
            }
        }
    }
    
    /**
     * Stop any currently playing speech.
     */
    public void stop() {
        if (isInitialized()) {
            textToSpeech.stop();
        }
    }
    
    /**
     * Set the speech rate.
     * 
     * @param rate Speech rate (0.5 = half speed, 1.0 = normal, 2.0 = double speed)
     */
    public void setSpeechRate(float rate) {
        if (isInitialized()) {
            textToSpeech.setSpeechRate(rate);
        }
    }
    
    /**
     * Set the language for TTS.
     * 
     * @param languageCode Language code (e.g., "en", "es", "fr")
     */
    public void setLanguage(String languageCode) {
        if (!isInitialized() || languageCode == null) {
            return;
        }
        
        Locale locale = getLocaleFromLanguageCode(languageCode);
        int result = textToSpeech.setLanguage(locale);
        
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language not supported: " + languageCode + ", falling back to default");
            // Fall back to English if language not supported
            textToSpeech.setLanguage(Locale.ENGLISH);
        } else {
            Log.d(TAG, "Language set to: " + languageCode);
        }
    }
    
    /**
     * Get available languages for TTS.
     */
    public Set<Locale> getAvailableLanguages() {
        if (isInitialized()) {
            return textToSpeech.getAvailableLanguages();
        }
        return null;
    }
    
    /**
     * Check if a specific language is available.
     */
    public boolean isLanguageAvailable(String languageCode) {
        if (!isInitialized()) {
            return false;
        }
        
        Locale locale = getLocaleFromLanguageCode(languageCode);
        int result = textToSpeech.isLanguageAvailable(locale);
        return result == TextToSpeech.LANG_AVAILABLE || 
               result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
               result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE;
    }
    
    /**
     * Convert language code to Locale.
     */
    private Locale getLocaleFromLanguageCode(String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            return Locale.getDefault();
        }
        
        // Handle language codes with region (e.g., "en-US", "es-ES")
        if (languageCode.contains("-")) {
            String[] parts = languageCode.split("-");
            if (parts.length >= 2) {
                return new Locale(parts[0], parts[1]);
            }
        }
        
        // Handle simple language codes (e.g., "en", "es")
        return new Locale(languageCode);
    }
    
    /**
     * Get human-readable language name from language code.
     */
    public String getLanguageName(String languageCode) {
        Locale locale = getLocaleFromLanguageCode(languageCode);
        return locale.getDisplayLanguage();
    }
    
    /**
     * Clean up TTS resources.
     */
    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
            isInitialized = false;
        }
    }
}