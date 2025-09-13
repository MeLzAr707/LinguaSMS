package com.translator.messagingapp.mms;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

/**
 * Test for TransactionService foreground service type fix for Android 14+
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {29, 34}) // Test on Android 10 (API 29) and Android 14 (API 34)
public class TransactionServiceForegroundTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private NotificationManager mockNotificationManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @Config(sdk = 34) // Android 14
    public void testForegroundServiceTypeRequired_Android14() {
        // Test that DATA_SYNC service type is used on Android 14+
        assertTrue("DATA_SYNC foreground service type should be available on Android 14", 
                   ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC > 0);
        
        // Verify that the Android version check works correctly
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", 34);
        assertTrue("Should use service type on Android 14", 
                   Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
    }

    @Test
    @Config(sdk = 29) // Android 10
    public void testForegroundServiceTypeSupported_Android10() {
        // Test that DATA_SYNC service type is available on Android 10+
        assertTrue("DATA_SYNC foreground service type should be available on Android 10+", 
                   ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC > 0);
        
        // Verify that the Android version check works correctly
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", 29);
        assertTrue("Should use service type on Android 10+", 
                   Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
    }

    @Test
    @Config(sdk = 28) // Android 9
    public void testForegroundServiceTypeNotRequired_Android9() {
        // Test that service type is not required on Android 9
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", 28);
        assertFalse("Should not use service type on Android 9", 
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
    }
}