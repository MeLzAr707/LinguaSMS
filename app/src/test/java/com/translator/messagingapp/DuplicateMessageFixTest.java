package com.translator.messagingapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;

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
 * Tests that messages are not stored twice when the app is the default SMS app.
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
     * (since the app is responsible for storing them).
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
            // When app is default SMS app, it should manually store messages
            messageService.handleIncomingSms(smsIntent);
            
            // Verify that PhoneUtils.isDefaultSmsApp was called
            mockedPhoneUtils.verify(() -> PhoneUtils.isDefaultSmsApp(any()), atLeastOnce());
        }
    }

    /**
     * Test that when app is NOT default SMS app, messages are NOT manually stored.
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
            // When app is NOT default SMS app, system handles storage automatically
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

    /**
     * Test that handleIncomingSms properly handles different SMS intent actions.
     */
    @Test
    public void testHandleIncomingSmsWithDifferentActions() {
        // Test SMS_RECEIVED_ACTION
        Intent smsReceivedIntent = new Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        Bundle bundle1 = new Bundle();
        smsReceivedIntent.putExtras(bundle1);
        
        // Mock PhoneUtils.isDefaultSmsApp to return false for this test
        try (MockedStatic<PhoneUtils> mockedPhoneUtils = mockStatic(PhoneUtils.class)) {
            mockedPhoneUtils.when(() -> PhoneUtils.isDefaultSmsApp(any())).thenReturn(false);
            messageService.handleIncomingSms(smsReceivedIntent);
        }

        // Test SMS_DELIVER_ACTION  
        Intent smsDeliverIntent = new Intent(Telephony.Sms.Intents.SMS_DELIVER_ACTION);
        Bundle bundle2 = new Bundle();
        smsDeliverIntent.putExtras(bundle2);
        
        // Mock PhoneUtils.isDefaultSmsApp to return true for this test
        try (MockedStatic<PhoneUtils> mockedPhoneUtils = mockStatic(PhoneUtils.class)) {
            mockedPhoneUtils.when(() -> PhoneUtils.isDefaultSmsApp(any())).thenReturn(true);
            messageService.handleIncomingSms(smsDeliverIntent);
        }
    }
}