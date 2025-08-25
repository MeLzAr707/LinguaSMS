package com.translator.messagingapp;

/**
 * Listener interface for TTS playback events.
 */
public interface TTSPlaybackListener {
    /**
     * Called when TTS playback starts.
     */
    void onPlaybackStarted();
    
    /**
     * Called when TTS playback completes successfully.
     */
    void onPlaybackCompleted();
    
    /**
     * Called when TTS playback encounters an error.
     * @param error The error message
     */
    void onPlaybackError(String error);
    
    /**
     * Called when TTS playback is stopped.
     */
    void onPlaybackStopped();
}