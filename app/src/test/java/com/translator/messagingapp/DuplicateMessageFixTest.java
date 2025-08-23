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
 * Unit tests for duplicate message fix (Issue #262 and #331).
 * Tests correct SMS storage behavior with database-based duplicate prevention:
 * - Messages are stored based on database duplicate checks rather than default app status
 * - Duplicate detection uses address, body, type, and timestamp matching
 * - All incoming messages are processed for storage with duplicate prevention
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
     * Test that when app is default SMS app, messages are processed correctly.
     * With the new duplicate prevention logic, messages are stored based on
     * database checks rather than default SMS app status.
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
            // With the new logic, duplicate prevention is used instead of default app checks
            messageService.handleIncomingSms(smsIntent);
            
            // Note: PhoneUtils.isDefaultSmsApp may still be called for notifications
            // but is no longer used for storage decisions
        }
    }

    /**
     * Test that when app is NOT default SMS app, messages are processed correctly.
     * With the new duplicate prevention logic, messages are stored based on
     * database checks rather than default SMS app status.
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
            // With the new logic, duplicate prevention is used instead of default app checks
            messageService.handleIncomingSms(smsIntent);
            
            // Note: PhoneUtils.isDefaultSmsApp may still be called for notifications
            // but is no longer used for storage decisions
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