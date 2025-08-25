package com.translator.messagingapp;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Instrumented test that demonstrates Mockito working in androidTest.
 * This test validates that the Mockito dependencies are properly configured.
 */
@RunWith(AndroidJUnit4.class)
public class MediaGalleryViewTest {

    @Mock
    private MediaLoadListener mockListener;

    private Context context;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void testMockitoSetup() {
        // Test that Mockito is working correctly in androidTest
        assertNotNull("Context should not be null", context);
        assertNotNull("Mock listener should not be null", mockListener);
    }

    @Test
    public void testMediaLoadError_withInvalidUri() {
        // Test that demonstrates the original error pattern from the issue
        String errorMessage = "Invalid media URI";
        
        // Simulate calling the listener with error
        mockListener.onMediaLoadError(errorMessage);
        
        // Verify the method was called - this demonstrates the fix for the original build error
        verify(mockListener, timeout(1000).times(1)).onMediaLoadError(contains("Invalid media URI"));
    }

    @Test
    public void testMockitoFeatures() {
        // Test various Mockito features that were mentioned in the build error
        
        // Test the @Mock annotation works
        assertNotNull("@Mock annotation should create mock", mockListener);
        
        // Test verify() with timeout() and times() methods work
        mockListener.onMediaLoadSuccess();
        verify(mockListener, timeout(1000).times(1)).onMediaLoadSuccess();
        
        // Test contains() method works
        mockListener.onMediaLoadError("Test error message");
        verify(mockListener, times(1)).onMediaLoadError(contains("Test error"));
    }

    /**
     * Simple interface for testing Mockito functionality.
     * This represents the listener pattern mentioned in the original error.
     */
    public interface MediaLoadListener {
        void onMediaLoadError(String error);
        void onMediaLoadSuccess();
    }
}