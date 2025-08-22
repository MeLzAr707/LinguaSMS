package com.translator.messagingapp;

import android.content.Context;
import android.content.SharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test to verify that the SMS prompt respects user preferences and doesn't show repeatedly
 * after the user has declined or reached the maximum request count.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultSmsPromptFixTest {

    @Mock
    Context mockContext;

    @Mock
    SharedPreferences mockPrefs;

    @Mock
    SharedPreferences.Editor mockEditor;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(mockContext.getSharedPreferences("sms_app_prefs", Context.MODE_PRIVATE)).thenReturn(mockPrefs);
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor);
        when(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor);
    }

    @Test
    public void testShouldRequestDefaultSmsApp_FirstTime() {
        // Setup: No previous requests
        when(mockPrefs.getBoolean("user_declined_sms", false)).thenReturn(false);
        when(mockPrefs.getInt("default_sms_requested", 0)).thenReturn(0);

        // Test
        boolean shouldRequest = PhoneUtils.shouldRequestDefaultSmsApp(mockContext);

        // Verify
        assertTrue("Should request SMS app on first attempt", shouldRequest);
    }

    @Test
    public void testShouldRequestDefaultSmsApp_UserDeclined() {
        // Setup: User has permanently declined
        when(mockPrefs.getBoolean("user_declined_sms", false)).thenReturn(true);
        when(mockPrefs.getInt("default_sms_requested", 0)).thenReturn(2);

        // Test
        boolean shouldRequest = PhoneUtils.shouldRequestDefaultSmsApp(mockContext);

        // Verify
        assertFalse("Should not request SMS app when user has declined", shouldRequest);
    }

    @Test
    public void testShouldRequestDefaultSmsApp_MaxRequestsReached() {
        // Setup: Maximum requests reached but user hasn't explicitly declined
        when(mockPrefs.getBoolean("user_declined_sms", false)).thenReturn(false);
        when(mockPrefs.getInt("default_sms_requested", 0)).thenReturn(3);

        // Test
        boolean shouldRequest = PhoneUtils.shouldRequestDefaultSmsApp(mockContext);

        // Verify
        assertFalse("Should not request SMS app when max requests reached", shouldRequest);
    }

    @Test
    public void testShouldRequestDefaultSmsApp_WithinLimits() {
        // Setup: 2 previous requests, user hasn't declined
        when(mockPrefs.getBoolean("user_declined_sms", false)).thenReturn(false);
        when(mockPrefs.getInt("default_sms_requested", 0)).thenReturn(2);

        // Test
        boolean shouldRequest = PhoneUtils.shouldRequestDefaultSmsApp(mockContext);

        // Verify
        assertTrue("Should request SMS app when within limits and user hasn't declined", shouldRequest);
    }

    @Test
    public void testSetUserDeclinedDefaultSms() {
        // Test setting user declined
        PhoneUtils.setUserDeclinedDefaultSms(mockContext, true);

        // Verify
        verify(mockEditor).putBoolean("user_declined_sms", true);
        verify(mockEditor).apply();
    }

    @Test
    public void testHasUserDeclinedDefaultSms_True() {
        // Setup
        when(mockPrefs.getBoolean("user_declined_sms", false)).thenReturn(true);

        // Test
        boolean hasDeclined = PhoneUtils.hasUserDeclinedDefaultSms(mockContext);

        // Verify
        assertTrue("Should return true when user has declined", hasDeclined);
    }

    @Test
    public void testHasUserDeclinedDefaultSms_False() {
        // Setup
        when(mockPrefs.getBoolean("user_declined_sms", false)).thenReturn(false);

        // Test
        boolean hasDeclined = PhoneUtils.hasUserDeclinedDefaultSms(mockContext);

        // Verify
        assertFalse("Should return false when user has not declined", hasDeclined);
    }

    @Test
    public void testIncrementDefaultSmsRequestCount() {
        // Setup
        when(mockPrefs.getInt("default_sms_requested", 0)).thenReturn(1);

        // Test
        PhoneUtils.incrementDefaultSmsRequestCount(mockContext);

        // Verify
        verify(mockEditor).putInt("default_sms_requested", 2);
        verify(mockEditor).apply();
    }
}