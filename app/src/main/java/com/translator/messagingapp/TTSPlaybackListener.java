package com.translator.messagingapp;

/**
 * Listener interface for TTS playback events.
 * Provides callbacks for various TTS states and events.
 */
public interface TTSPlaybackListener {
    
    /**
     * Called when TTS is ready for use.
     */
    void onTTSReady();
    
    /**
     * Called when TTS starts speaking.
     */
    void onTTSStarted();
    
    /**
     * Called when TTS stops speaking.
     */
    void onTTSStopped();
    
    /**
     * Called when TTS playback is completed.
     */
    void onTTSCompleted();
    
    /**
     * Called when there's an error with TTS.
     * 
     * @param error The error message
     */
    void onTTSError(String error);
}