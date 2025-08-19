package com.translator.messagingapp;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;

/**
 * Test class for pinch-to-zoom text size functionality.
 * Validates that text size can be adjusted and persisted correctly.
 */
@RunWith(RobolectricTestRunner.class)
public class PinchToZoomTextSizeTest {

    private TextSizeManager textSizeManager;
    private UserPreferences userPreferences;
    private Context context;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        context = RuntimeEnvironment.getApplication();
        userPreferences = new UserPreferences(context);
        textSizeManager = new TextSizeManager(context);
    }

    @Test
    public void testDefaultTextSize() {
        // Reset to default
        textSizeManager.resetTextSize();
        
        // Verify default text size
        float defaultSize = textSizeManager.getCurrentTextSize();
        assertEquals(TextSizeManager.DEFAULT_TEXT_SIZE, defaultSize, 0.01f);
    }

    @Test
    public void testTextSizeConstraints() {
        // Test minimum constraint
        float actualMinSize = textSizeManager.setTextSize(5.0f); // Below minimum
        assertEquals(TextSizeManager.MIN_TEXT_SIZE, actualMinSize, 0.01f);

        // Test maximum constraint
        float actualMaxSize = textSizeManager.setTextSize(50.0f); // Above maximum
        assertEquals(TextSizeManager.MAX_TEXT_SIZE, actualMaxSize, 0.01f);

        // Test valid range
        float validSize = textSizeManager.setTextSize(18.0f);
        assertEquals(18.0f, validSize, 0.01f);
    }

    @Test
    public void testScaleGestureUpdate() {
        // Start with default size
        textSizeManager.resetTextSize();
        
        // Simulate pinch-to-zoom (scale up)
        float newSize = textSizeManager.updateTextSize(1.2f);
        assertEquals(TextSizeManager.DEFAULT_TEXT_SIZE * 1.2f, newSize, 0.01f);

        // Simulate pinch-to-zoom (scale down)
        float smallerSize = textSizeManager.updateTextSize(0.8f);
        assertEquals(newSize * 0.8f, smallerSize, 0.01f);
    }

    @Test
    public void testTextSizePersistence() {
        // Set a custom text size
        float customSize = 20.0f;
        textSizeManager.setTextSize(customSize);

        // Create a new instance to simulate app restart
        TextSizeManager newManager = new TextSizeManager(context);
        
        // Verify the size persisted
        assertEquals(customSize, newManager.getCurrentTextSize(), 0.01f);
    }

    @Test
    public void testUserPreferencesIntegration() {
        // Test direct UserPreferences integration
        float testSize = 22.0f;
        userPreferences.setMessageTextSize(testSize);
        
        assertEquals(testSize, userPreferences.getMessageTextSize(), 0.01f);
        
        // Verify TextSizeManager uses the same preference
        assertEquals(testSize, textSizeManager.getCurrentTextSize(), 0.01f);
    }

    @Test
    public void testCalculateScaledTextSize() {
        // Set a base text size
        textSizeManager.setTextSize(16.0f);
        
        // Test scaling calculations
        float scaled = textSizeManager.calculateScaledTextSize(1.5f);
        assertEquals(24.0f, scaled, 0.01f);
        
        // Test scaling with constraints
        float constrainedScaled = textSizeManager.calculateScaledTextSize(3.0f); // Would exceed max
        assertEquals(TextSizeManager.MAX_TEXT_SIZE, constrainedScaled, 0.01f);
    }
}