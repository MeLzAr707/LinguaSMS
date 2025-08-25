package com.translator.messagingapp;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import java.util.Locale;

/**
 * Manager for Text-to-Speech functionality.
 * Handles TTS initialization and message reading operations.
 */
public class TTSManager implements TextToSpeech.OnInitListener {
    private static final String TAG = "TTSManager";
    
    private TextToSpeech textToSpeech;
    private boolean isInitialized = false;
    private TTSPlaybackListener playbackListener;
    
    /**
     * Default constructor with no arguments.
     * According to the build error, this is one expected constructor signature.
     */
    public TTSManager() {
        Log.d(TAG, "TTSManager initialized with default constructor");
    }
    
    /**
     * Constructor with context.
     * 
     * @param context The application context
     */
    public TTSManager(Context context) {
        Log.d(TAG, "TTSManager initialized with context");
        initialize(context);
    }
    
    /**
     * Constructor with context and listener.
     * 
     * @param context The application context
     * @param listener The TTS playback listener
     */
    public TTSManager(Context context, TTSPlaybackListener listener) {
        Log.d(TAG, "TTSManager initialized with context and listener");
        this.playbackListener = listener;
        initialize(context);
    }
    
    /**
     * Initialize TTS with context.
     * 
     * @param context The application context
     */
    public void initialize(Context context) {
        if (context != null) {
            textToSpeech = new TextToSpeech(context, this);
        }
    }
    
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true;
            textToSpeech.setLanguage(Locale.getDefault());
            Log.d(TAG, "TTS initialized successfully");
            
            if (playbackListener != null) {
                playbackListener.onTTSReady();
            }
        } else {
            Log.e(TAG, "TTS initialization failed");
            
            if (playbackListener != null) {
                playbackListener.onTTSError("TTS initialization failed");
            }
        }
    }
    
    /**
     * Speak the given text.
     * 
     * @param text The text to speak
     */
    public void speak(String text) {
        if (isInitialized && textToSpeech != null && text != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            Log.d(TAG, "Speaking text: " + text);
            
            if (playbackListener != null) {
                playbackListener.onTTSStarted();
            }
        } else {
            Log.w(TAG, "TTS not initialized or text is null");
        }
    }
    
    /**
     * Stop speaking.
     */
    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            Log.d(TAG, "TTS stopped");
            
            if (playbackListener != null) {
                playbackListener.onTTSStopped();
            }
        }
    }
    
    /**
     * Check if TTS is initialized.
     * 
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Set the playback listener.
     * 
     * @param listener The TTS playback listener
     */
    public void setPlaybackListener(TTSPlaybackListener listener) {
        this.playbackListener = listener;
    }
    
    /**
     * Release TTS resources.
     */
    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            isInitialized = false;
            Log.d(TAG, "TTS shutdown");
        }
    }
}