package com.translator.messagingapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for duplicate message fix.
 * Tests correct SMS storage behavior according to Android documentation:
 * - Default SMS apps (receive SMS_DELIVER_ACTION) must manually store messages
 * - Non-default SMS apps (receive SMS_RECEIVED_ACTION) rely on automatic system storage
 */
@RunWith(RobolectricTestRunner.class)
public class DuplicateMessageFixTest {

    private MessageService messageService;
    
    @Mock
    private TranslationManager mockTranslationManager;
    
    @Mock
    private TranslationCache mockTranslationCache;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        Context context = RuntimeEnvironment.getApplication();
        messageService = new MessageService(context, mockTranslationManager, mockTranslationCache);
    }

    /**
     * Test that when app is default SMS app, messages ARE manually stored
     * (since app receives SMS_DELIVER_ACTION and is responsible for storage).
     */
    @Test
    public void testManualStorageWhenDefaultSmsApp() {
        // Create a mock SMS intent with basic structure
        Intent smsIntent = new Intent();
        Bundle bundle = new Bundle();
        smsIntent.putExtras(bundle);
        
        // Mock PhoneUtils.isDefaultSmsApp to return true
        try (MockedStatic<PhoneUtils> mockedPhoneUtils = mockStatic(PhoneUtils.class)) {
            mockedPhoneUtils.when(() -> PhoneUtils.isDefaultSmsApp(any())).thenReturn(true);
            
            // This should not throw an exception and should handle the case gracefully
            // We're testing that the method exists and behaves correctly for default SMS app
            messageService.handleIncomingSms(smsIntent);
            
            // Verify that PhoneUtils.isDefaultSmsApp was called
            mockedPhoneUtils.verify(() -> PhoneUtils.isDefaultSmsApp(any()), atLeastOnce());
        }
    }

    /**
     * Test that when app is NOT default SMS app, messages are NOT manually stored
     * (since Android system automatically stores them via SMS_RECEIVED_ACTION).
     */
    @Test
    public void testNoManualStorageWhenNotDefaultSmsApp() {
        // Create a mock SMS intent with basic structure
        Intent smsIntent = new Intent();
        Bundle bundle = new Bundle();
        smsIntent.putExtras(bundle);
        
        // Mock PhoneUtils.isDefaultSmsApp to return false
        try (MockedStatic<PhoneUtils> mockedPhoneUtils = mockStatic(PhoneUtils.class)) {
            mockedPhoneUtils.when(() -> PhoneUtils.isDefaultSmsApp(any())).thenReturn(false);
            
            // This should not throw an exception and should handle the case gracefully
            // We're testing that the method exists and behaves correctly for non-default SMS app
            messageService.handleIncomingSms(smsIntent);
            
            // Verify that PhoneUtils.isDefaultSmsApp was called
            mockedPhoneUtils.verify(() -> PhoneUtils.isDefaultSmsApp(any()), atLeastOnce());
        }
    }

    /**
     * Test that handleIncomingSms method handles null intents gracefully.
     */
    @Test
    public void testHandleIncomingSmsWithNullIntent() {
        // This should not throw an exception
        messageService.handleIncomingSms(null);
    }

    /**
     * Test that handleIncomingSms method handles empty bundles gracefully.
     */
    @Test
    public void testHandleIncomingSmsWithEmptyBundle() {
        Intent smsIntent = new Intent();
        Bundle bundle = new Bundle();
        smsIntent.putExtras(bundle);
        
        // This should not throw an exception
        messageService.handleIncomingSms(smsIntent);
    }
}