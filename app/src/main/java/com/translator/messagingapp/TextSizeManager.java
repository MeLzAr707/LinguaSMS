package com.translator.messagingapp;

import android.content.Context;

/**
 * Manages text size preferences for the messaging app.
 * Provides methods to get, set, and validate text sizes.
 */
public class TextSizeManager {
    private static final float DEFAULT_TEXT_SIZE = 14.0f;
    private static final float MIN_TEXT_SIZE = 10.0f;
    private static final float MAX_TEXT_SIZE = 24.0f;
    
    private UserPreferences userPreferences;
    private Context context;

    public TextSizeManager(Context context) {
        this.context = context;
        this.userPreferences = new UserPreferences(context);
    }

    /**
     * Gets the current message text size from user preferences.
     *
     * @return The current text size
     */
    public float getCurrentTextSize() {
        return userPreferences.getMessageTextSize();
    }

    /**
     * Sets the message text size with validation.
     *
     * @param newSize The new text size to set
     * @return true if the size was set successfully, false if it was out of bounds
     */
    public boolean setTextSize(float newSize) {
        if (isValidTextSize(newSize)) {
            userPreferences.setMessageTextSize(newSize);
            return true;
        }
        return false;
    }

    /**
     * Increases the text size by a given amount.
     *
     * @param increment The amount to increase by
     * @return The new text size after increase
     */
    public float increaseTextSize(float increment) {
        float currentSize = getCurrentTextSize();
        float newSize = currentSize + increment;
        float constrainedSize = Math.min(newSize, MAX_TEXT_SIZE);
        userPreferences.setMessageTextSize(constrainedSize);
        return constrainedSize;
    }

    /**
     * Decreases the text size by a given amount.
     *
     * @param decrement The amount to decrease by
     * @return The new text size after decrease
     */
    public float decreaseTextSize(float decrement) {
        float currentSize = getCurrentTextSize();
        float newSize = currentSize - decrement;
        float constrainedSize = Math.max(newSize, MIN_TEXT_SIZE);
        userPreferences.setMessageTextSize(constrainedSize);
        return constrainedSize;
    }

    /**
     * Resets the text size to the default value.
     */
    public void resetToDefault() {
        userPreferences.setMessageTextSize(DEFAULT_TEXT_SIZE);
    }

    /**
     * Updates the text size based on a scale factor from pinch-to-zoom gesture.
     *
     * @param scaleFactor The scale factor from the gesture
     * @return The new text size after scaling
     */
    public float updateTextSize(float scaleFactor) {
        float currentSize = getCurrentTextSize();
        float newSize = currentSize * scaleFactor;
        float constrainedSize = Math.max(MIN_TEXT_SIZE, Math.min(newSize, MAX_TEXT_SIZE));
        userPreferences.setMessageTextSize(constrainedSize);
        return constrainedSize;
    }

    /**
     * Validates if a text size is within acceptable bounds.
     *
     * @param size The size to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidTextSize(float size) {
        return size >= MIN_TEXT_SIZE && size <= MAX_TEXT_SIZE;
    }

    /**
     * Gets the minimum allowed text size.
     *
     * @return The minimum text size
     */
    public float getMinTextSize() {
        return MIN_TEXT_SIZE;
    }

    /**
     * Gets the maximum allowed text size.
     *
     * @return The maximum text size
     */
    public float getMaxTextSize() {
        return MAX_TEXT_SIZE;
    }

    /**
     * Gets the default text size.
     *
     * @return The default text size
     */
    public float getDefaultTextSize() {
        return DEFAULT_TEXT_SIZE;
    }
}