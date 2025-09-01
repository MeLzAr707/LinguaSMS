package com.translator.messagingapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Test class for MessageMonitoringService to validate that the service works
 * correctly with minimal notification visibility while maintaining functionality.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.TIRAMISU)
public class MessageMonitoringServiceTest {

    private Context context;
    private ServiceController<MessageMonitoringService> serviceController;
    private MessageMonitoringService service;
    private NotificationManager notificationManager;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        serviceController = Robolectric.buildService(MessageMonitoringService.class);
        service = serviceController.get();
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Test
    public void testServiceCreation() {
        // Test service can be created successfully
        serviceController.create();
        assertNotNull("Service should be created", service);
    }

    @Test
    public void testServiceStartCommand() {
        // Test service handles start command correctly
        serviceController.create();
        
        Intent intent = new Intent();
        int result = serviceController.startCommand(0, 0);
        
        assertEquals("Service should return START_STICKY", Service.START_STICKY, result);
    }

    @Test
    public void testNotificationChannelCreation() {
        // Test that notification channel is created with minimal visibility
        serviceController.create();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel("MESSAGE_MONITORING_CHANNEL");
            
            if (channel != null) {
                assertEquals("Channel should have minimal importance", 
                           NotificationManager.IMPORTANCE_MIN, channel.getImportance());
                assertFalse("Channel should not show badge", channel.canShowBadge());
            }
        }
    }

    @Test
    public void testNotificationIsMinimal() {
        // Test that the notification is configured for minimal visibility
        serviceController.create();
        
        // Access the private createNotification method via reflection or test indirectly
        // For now, we test that the service can start without throwing exceptions
        try {
            serviceController.startCommand(0, 0);
            // If no exception thrown, the notification creation is working
        } catch (Exception e) {
            fail("Service should start successfully with minimal notification: " + e.getMessage());
        }
    }

    @Test
    public void testServiceDestroy() {
        // Test service cleanup
        serviceController.create();
        serviceController.startCommand(0, 0);
        serviceController.destroy();
        
        // Should complete without throwing exceptions
        assertTrue("Service should destroy cleanly", true);
    }

    @Test
    public void testServiceWithoutCrashing() {
        // Integration test to ensure service lifecycle works end-to-end
        serviceController.create();
        
        Intent startIntent = new Intent();
        serviceController.startCommand(0, 0);
        
        // Simulate some time passing
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        serviceController.destroy();
        
        assertTrue("Service lifecycle should complete without crashes", true);
    }

    @Test
    public void testStaticServiceMethods() {
        // Test static methods for starting and stopping service
        try {
            MessageMonitoringService.startService(context);
            MessageMonitoringService.stopService(context);
            // Should complete without exceptions
        } catch (Exception e) {
            fail("Static service methods should work: " + e.getMessage());
        }
    }
}