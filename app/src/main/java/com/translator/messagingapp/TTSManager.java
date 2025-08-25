package com.translator.messagingapp;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manages Text-to-Speech functionality for the messaging app.
 * Provides methods to speak messages and manage TTS settings.
 */
public class TTSManager {
    private static final String TAG = "TTSManager";
    private TextToSpeech textToSpeech;
    private Context context;
    private boolean isInitialized = false;
    private TTSPlaybackListener currentListener;
    
    public TTSManager(Context context) {
        this.context = context;
        initialize();
    }
    
    /**
     * Initialize the TTS engine.
     */
    private void initialize() {
        textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(Locale.getDefault());
                    isInitialized = true;
                    Log.d(TAG, "TTS initialized successfully");
                } else {
                    Log.e(TAG, "TTS initialization failed");
                }
            }
        });
        
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                if (currentListener != null) {
                    currentListener.onPlaybackStarted();
                }
            }
            
            @Override
            public void onDone(String utteranceId) {
                if (currentListener != null) {
                    currentListener.onPlaybackCompleted();
                }
            }
            
            @Override
            public void onError(String utteranceId) {
                if (currentListener != null) {
                    currentListener.onPlaybackError("TTS Error for utterance: " + utteranceId);
                }
            }
        });
    }
    
    /**
     * Speaks the given text.
     * @param text The text to speak
     */
    public void speak(String text) {
        speak(text, null);
    }
    
    /**
     * Speaks the given text with a listener.
     * @param text The text to speak
     * @param listener The listener for playback events
     */
    public void speak(String text, TTSPlaybackListener listener) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized, cannot speak");
            if (listener != null) {
                listener.onPlaybackError("TTS not initialized");
            }
            return;
        }
        
        this.currentListener = listener;
        Map<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utterance_" + System.currentTimeMillis());
        
        int result = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params);
        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "Error speaking text: " + text);
            if (listener != null) {
                listener.onPlaybackError("Failed to start TTS");
            }
        }
    }
    
    /**
     * Stops current TTS playback.
     */
    public void stop() {
        if (isInitialized && textToSpeech != null) {
            textToSpeech.stop();
            if (currentListener != null) {
                currentListener.onPlaybackStopped();
            }
        }
    }
    
    /**
     * Checks if TTS is currently speaking.
     * @return true if speaking
     */
    public boolean isSpeaking() {
        return isInitialized && textToSpeech != null && textToSpeech.isSpeaking();
    }
    
    /**
     * Sets the TTS language.
     * @param locale The locale to set
     * @return true if language is supported
     */
    public boolean setLanguage(Locale locale) {
        if (!isInitialized) {
            return false;
        }
        
        int result = textToSpeech.setLanguage(locale);
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
    }
    
    /**
     * Sets the speech rate.
     * @param rate The speech rate (1.0 is normal)
     */
    public void setSpeechRate(float rate) {
        if (isInitialized && textToSpeech != null) {
            textToSpeech.setSpeechRate(rate);
        }
    }
    
    /**
     * Sets the speech pitch.
     * @param pitch The speech pitch (1.0 is normal)
     */
    public void setPitch(float pitch) {
        if (isInitialized && textToSpeech != null) {
            textToSpeech.setPitch(pitch);
        }
    }
    
    /**
     * Checks if TTS is initialized.
     * @return true if initialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Shuts down the TTS manager.
     */
    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            isInitialized = false;
            Log.d(TAG, "TTS shut down");
        }
    }
}