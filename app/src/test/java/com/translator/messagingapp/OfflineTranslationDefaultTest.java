package com.translator.messagingapp;

import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;

/**
 * Test to verify offline translation defaults to enabled for new users.
 */
@RunWith(RobolectricTestRunner.class)
public class OfflineTranslationDefaultTest {

    private UserPreferences userPreferences;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        // Create fresh preferences to simulate new user
        userPreferences = new UserPreferences(context);
    }

    @Test
    public void testOfflineTranslationEnabledByDefault() {
        // For a new user (no preferences set), offline translation should be enabled by default
        boolean isEnabled = userPreferences.isOfflineTranslationEnabled();
        assertTrue("Offline translation should be enabled by default for new users", isEnabled);
    }

    @Test
    public void testOfflineTranslationCanBeDisabled() {
        // Verify that the setting can be changed
        userPreferences.setOfflineTranslationEnabled(false);
        assertFalse("Offline translation should be disabled when explicitly set to false", 
                   userPreferences.isOfflineTranslationEnabled());
    }

    @Test
    public void testOfflineTranslationCanBeReEnabled() {
        // Verify that the setting can be toggled
        userPreferences.setOfflineTranslationEnabled(false);
        userPreferences.setOfflineTranslationEnabled(true);
        assertTrue("Offline translation should be enabled when explicitly set to true", 
                  userPreferences.isOfflineTranslationEnabled());
    }
}