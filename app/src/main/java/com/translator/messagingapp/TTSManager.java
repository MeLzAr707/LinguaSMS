package com.translator.messagingapp;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import java.util.Locale;

/**
 * Manager for Text-to-Speech functionality.
 * Handles TTS initialization, playback, and listener callbacks.
 */
public class TTSManager {
    private static final String TAG = "TTSManager";
    
    private TextToSpeech tts;
    private Context context;
    private boolean isInitialized = false;
    
    /**
     * Interface for TTS playback callbacks.
     */
    public interface TTSPlaybackListener {
        void onStart();
        void onDone();
        void onError(String error);
    }
    
    /**
     * Creates a new TTSManager.
     * 
     * @param context The context
     * @param listener The playback listener
     */
    public TTSManager(Context context, TTSPlaybackListener listener) {
        this.context = context;
        
        // Initialize TextToSpeech
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true;
                Log.d(TAG, "TTS initialized successfully");
            } else {
                Log.e(TAG, "TTS initialization failed");
                if (listener != null) {
                    listener.onError("TTS initialization failed");
                }
            }
        });
    }
    
    /**
     * Speaks the given text in the specified language.
     * 
     * @param text The text to speak
     * @param languageCode The language code (e.g., "en", "fr", "es")
     * @param listener The playback listener for callbacks
     */
    public void speak(String text, String languageCode, TTSPlaybackListener listener) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized yet");
            if (listener != null) {
                listener.onError("TTS not initialized");
            }
            return;
        }
        
        if (text == null || text.trim().isEmpty()) {
            Log.w(TAG, "Empty text provided");
            if (listener != null) {
                listener.onError("Empty text");
            }
            return;
        }
        
        try {
            // Set language
            if (languageCode != null) {
                Locale locale = new Locale(languageCode);
                int result = tts.setLanguage(locale);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Language not supported: " + languageCode);
                    // Continue with default language
                }
            }
            
            // Start speaking
            if (listener != null) {
                listener.onStart();
            }
            
            int result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_message");
            
            if (result == TextToSpeech.ERROR) {
                Log.e(TAG, "Error speaking text");
                if (listener != null) {
                    listener.onError("TTS playback error");
                }
            } else {
                Log.d(TAG, "Speaking text: " + text.substring(0, Math.min(text.length(), 50)) + "...");
                // Note: onDone callback would be handled by TTS completion callback in a full implementation
                if (listener != null) {
                    // For now, call onDone immediately since we don't have completion callback set up
                    listener.onDone();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during TTS playback", e);
            if (listener != null) {
                listener.onError("TTS error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Stops TTS playback.
     */
    public void stop() {
        if (tts != null && isInitialized) {
            tts.stop();
        }
    }
    
    /**
     * Shuts down the TTS engine.
     * Should be called when the manager is no longer needed.
     */
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            isInitialized = false;
        }
    }
    
    /**
     * Checks if TTS is currently speaking.
     * 
     * @return true if speaking, false otherwise
     */
    public boolean isSpeaking() {
        return tts != null && tts.isSpeaking();
    }
    
    /**
     * Checks if TTS is initialized and ready.
     * 
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return isInitialized;
    }
}