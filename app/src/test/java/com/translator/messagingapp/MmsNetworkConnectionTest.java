package com.translator.messagingapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.translator.messagingapp.message.MessageService;

/**
 * Test class to verify improved network connection detection for MMS sending.
 * This addresses the issue where users encounter "Failed to send MMS: Network connection error"
 * even when network appears to be active.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class MmsNetworkConnectionTest {

    @Mock
    private Context mockContext;

    @Mock
    private ConnectivityManager mockConnectivityManager;

    @Mock
    private Network mockNetwork;

    @Mock
    private NetworkCapabilities mockNetworkCapabilities;

    @Mock
    private NetworkInfo mockNetworkInfo;

    private MessageService messageService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mockConnectivityManager);
        
        messageService = new MessageService(mockContext);
    }

    @Test
    public void testNetworkAvailableWithModernApi() {
        // Setup: Modern API with internet capability
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork)).thenReturn(mockNetworkCapabilities);
        when(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)).thenReturn(true);
        when(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(true);

        // Test: Should detect network as available for MMS
        boolean result = callIsNetworkAvailableForMms();
        
        assertTrue("Network should be detected as available for MMS with modern API", result);
    }

    @Test
    public void testNetworkAvailableWithLegacyApi() {
        // Setup: Legacy API with connected mobile network
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(null); // Force fallback to legacy API
        when(mockConnectivityManager.getActiveNetworkInfo()).thenReturn(mockNetworkInfo);
        when(mockNetworkInfo.isConnected()).thenReturn(true);
        when(mockNetworkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
        when(mockNetworkInfo.getTypeName()).thenReturn("MOBILE");

        // Test: Should detect network as available for MMS
        boolean result = callIsNetworkAvailableForMms();
        
        assertTrue("Network should be detected as available for MMS with legacy API", result);
    }

    @Test
    public void testNetworkUnavailableScenario() {
        // Setup: No network connection
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(null);
        when(mockConnectivityManager.getActiveNetworkInfo()).thenReturn(null);

        // Test: Should detect no network
        boolean result = callIsNetworkAvailableForMms();
        
        assertFalse("Should detect no network when none is available", result);
    }

    @Test
    public void testNetworkAvailableWithWifi() {
        // Setup: WiFi connection (should be allowed for MMS)
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(mockNetwork);
        when(mockConnectivityManager.getNetworkCapabilities(mockNetwork)).thenReturn(mockNetworkCapabilities);
        when(mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)).thenReturn(true);
        when(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true);
        when(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(false);

        // Test: Should accept WiFi for MMS
        boolean result = callIsNetworkAvailableForMms();
        
        assertTrue("WiFi should be accepted for MMS sending", result);
    }

    @Test
    public void testExceptionHandlingInNetworkCheck() {
        // Setup: Exception during network check
        when(mockContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenThrow(new RuntimeException("Test exception"));

        // Test: Should gracefully handle exceptions
        boolean result = callIsNetworkAvailableForMms();
        
        // Should assume network is available when check fails to avoid false negatives
        assertTrue("Should assume network is available when check fails", result);
    }

    @Test
    public void testLenientNetworkTypeSupport() {
        // Setup: Network with uncommon type (should be more lenient)
        when(mockConnectivityManager.getActiveNetwork()).thenReturn(null);
        when(mockConnectivityManager.getActiveNetworkInfo()).thenReturn(mockNetworkInfo);
        when(mockNetworkInfo.isConnected()).thenReturn(true);
        when(mockNetworkInfo.getType()).thenReturn(ConnectivityManager.TYPE_ETHERNET); // Uncommon but valid
        when(mockNetworkInfo.getTypeName()).thenReturn("ETHERNET");

        // Test: Should accept ethernet connection
        boolean result = callIsNetworkAvailableForMms();
        
        assertTrue("Should accept ethernet connection for MMS", result);
    }

    /**
     * Helper method to test the private isNetworkAvailableForMms method via reflection
     */
    private boolean callIsNetworkAvailableForMms() {
        try {
            java.lang.reflect.Method method = MessageService.class.getDeclaredMethod("isNetworkAvailableForMms");
            method.setAccessible(true);
            return (Boolean) method.invoke(messageService);
        } catch (Exception e) {
            fail("Failed to call isNetworkAvailableForMms via reflection: " + e.getMessage());
            return false;
        }
    }
}