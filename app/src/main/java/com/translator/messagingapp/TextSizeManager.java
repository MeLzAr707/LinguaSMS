package com.translator.messagingapp;

import android.content.Context;

/**
 * Utility class for managing text size preferences and calculations.
 */
public class TextSizeManager {
    
    // Text size constraints
    public static final float MIN_TEXT_SIZE = 10.0f; // Minimum readable text size
    public static final float MAX_TEXT_SIZE = 30.0f; // Maximum text size to prevent overflow
    public static final float DEFAULT_TEXT_SIZE = 16.0f; // Default text size
    
    // Scale factors for pinch gestures
    public static final float SCALE_SENSITIVITY = 0.5f; // Sensitivity for scale changes
    
    private final UserPreferences userPreferences;
    
    public TextSizeManager(Context context) {
        this.userPreferences = new UserPreferences(context);
    }
    
    /**
     * Gets the current text size.
     *
     * @return The current text size in SP
     */
    public float getCurrentTextSize() {
        return userPreferences.getMessageTextSize();
    }
    
    /**
     * Updates the text size based on a scale factor from pinch gesture.
     *
     * @param scaleFactor The scale factor from the gesture detector
     * @return The new text size after applying the scale
     */
    public float updateTextSize(float scaleFactor) {
        float currentSize = getCurrentTextSize();
        float newSize = currentSize * scaleFactor;
        
        // Apply constraints
        newSize = Math.max(MIN_TEXT_SIZE, Math.min(MAX_TEXT_SIZE, newSize));
        
        // Save the new size
        userPreferences.setMessageTextSize(newSize);
        
        return newSize;
    }
    
    /**
     * Sets the text size directly.
     *
     * @param textSize The text size in SP
     * @return The actual text size set (after applying constraints)
     */
    public float setTextSize(float textSize) {
        float constrainedSize = Math.max(MIN_TEXT_SIZE, Math.min(MAX_TEXT_SIZE, textSize));
        userPreferences.setMessageTextSize(constrainedSize);
        return constrainedSize;
    }
    
    /**
     * Resets the text size to default.
     */
    public void resetTextSize() {
        userPreferences.setMessageTextSize(DEFAULT_TEXT_SIZE);
    }
    
    /**
     * Applies the current text size to a text view with proper scaling.
     *
     * @param scaleFactor The scale factor to apply to the current text size
     * @return The calculated text size
     */
    public float calculateScaledTextSize(float scaleFactor) {
        float currentSize = getCurrentTextSize();
        float scaledSize = currentSize * scaleFactor;
        return Math.max(MIN_TEXT_SIZE, Math.min(MAX_TEXT_SIZE, scaledSize));
    }
}