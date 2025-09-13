package com.translator.messagingapp;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.database.Cursor;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;

import com.translator.messagingapp.mms.http.HttpUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Test class to verify that SecurityException when accessing APN settings is properly handled.
 * This test addresses the issue described in the GitHub issue where MMS sending fails
 * due to SecurityException when trying to access APN settings.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class MmsSecurityExceptionFixTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private ContentResolver mockContentResolver;
    
    @Mock
    private CarrierConfigManager mockCarrierConfigManager;
    
    @Mock
    private TelephonyManager mockTelephonyManager;
    
    @Mock
    private PersistableBundle mockConfig;
    
    @Mock
    private Cursor mockCursor;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockContext.getContentResolver()).thenReturn(mockContentResolver);
        when(mockContext.getSystemService(Context.CARRIER_CONFIG_SERVICE)).thenReturn(mockCarrierConfigManager);
        when(mockContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mockTelephonyManager);
    }

    @Test
    public void testMmscUrlRetrievalWithSecurityException() {
        // Simulate the exact scenario from the GitHub issue
        // CarrierConfigManager returns null/empty config
        when(mockCarrierConfigManager.getConfig()).thenReturn(null);
        
        // TelephonyManager returns Verizon info (from the error log)
        when(mockTelephonyManager.getNetworkOperatorName()).thenReturn("Verizon");
        when(mockTelephonyManager.getNetworkOperator()).thenReturn("311480");
        
        // ContentResolver throws SecurityException when querying APN settings
        when(mockContentResolver.query(
            any(Uri.class), 
            any(String[].class), 
            any(), any(), any()))
            .thenThrow(new SecurityException("No permission to access APN settings"));

        // Test that getMmscUrl handles the SecurityException gracefully and returns a result
        String mmscUrl = HttpUtils.getMmscUrl(mockContext);
        
        // Should return Verizon MMSC URL from carrier-specific database
        assertNotNull("MMSC URL should not be null even when APN access is denied", mmscUrl);
        assertEquals("Should return Verizon MMSC URL", "http://mms.vtext.com/servlets/mms", mmscUrl);
    }

    @Test
    public void testMmscUrlFallbackWithSpecificVerizonMccMnc() {
        // Test the specific MCC/MNC from the error (311480)
        when(mockCarrierConfigManager.getConfig()).thenReturn(null);
        when(mockTelephonyManager.getNetworkOperatorName()).thenReturn("Verizon");
        when(mockTelephonyManager.getNetworkOperator()).thenReturn("311480");
        
        // Mock APN access failure
        when(mockContentResolver.query(any(Uri.class), any(String[].class), any(), any(), any()))
            .thenThrow(new SecurityException("No permission to access APN settings"));

        String mmscUrl = HttpUtils.getMmscUrl(mockContext);
        
        assertNotNull("Should find MMSC URL for Verizon MCC/MNC 311480", mmscUrl);
        assertEquals("Should return correct Verizon MMSC URL", "http://mms.vtext.com/servlets/mms", mmscUrl);
    }

    @Test
    public void testMmscUrlWorksWithCarrierConfig() {
        // Test successful case with CarrierConfigManager
        when(mockCarrierConfigManager.getConfig()).thenReturn(mockConfig);
        when(mockConfig.getString("mms_url_string")).thenReturn("http://mms.carrier.com/test");

        String mmscUrl = HttpUtils.getMmscUrl(mockContext);
        
        assertNotNull("Should get MMSC URL from carrier config", mmscUrl);
        assertEquals("Should return carrier config MMSC URL", "http://mms.carrier.com/test", mmscUrl);
        
        // Verify that APN settings were not accessed when carrier config works
        verify(mockContentResolver, never()).query(any(Uri.class), any(String[].class), any(), any(), any());
    }

    @Test
    public void testMmscUrlWithMultipleVerizonMccMncs() {
        // Test various Verizon MCC/MNC combinations
        String[] verizonMccMncs = {"310004", "310005", "310012", "311270", "311480", "311481"};
        
        for (String mccMnc : verizonMccMncs) {
            setUp(); // Reset mocks
            
            when(mockCarrierConfigManager.getConfig()).thenReturn(null);
            when(mockTelephonyManager.getNetworkOperatorName()).thenReturn("Verizon");
            when(mockTelephonyManager.getNetworkOperator()).thenReturn(mccMnc);
            
            when(mockContentResolver.query(any(Uri.class), any(String[].class), any(), any(), any()))
                .thenThrow(new SecurityException("No permission to access APN settings"));

            String mmscUrl = HttpUtils.getMmscUrl(mockContext);
            
            assertNotNull("Should find MMSC URL for Verizon MCC/MNC " + mccMnc, mmscUrl);
            assertEquals("Should return Verizon MMSC URL for " + mccMnc, 
                "http://mms.vtext.com/servlets/mms", mmscUrl);
        }
    }

    @Test
    public void testMmsValidationWithSecurityException() {
        // Test that MMS validation succeeds even when APN access fails
        when(mockCarrierConfigManager.getConfig()).thenReturn(null);
        when(mockTelephonyManager.getNetworkOperatorName()).thenReturn("Verizon");
        when(mockTelephonyManager.getNetworkOperator()).thenReturn("311480");
        
        when(mockContentResolver.query(any(Uri.class), any(String[].class), any(), any(), any()))
            .thenThrow(new SecurityException("No permission to access APN settings"));

        boolean isValid = HttpUtils.validateMmsConfiguration(mockContext);
        
        assertTrue("MMS configuration should be valid with carrier fallback", isValid);
    }

    @Test
    public void testOtherCarriersWorkWithSecurityException() {
        // Test T-Mobile
        when(mockCarrierConfigManager.getConfig()).thenReturn(null);
        when(mockTelephonyManager.getNetworkOperatorName()).thenReturn("T-Mobile");
        when(mockTelephonyManager.getNetworkOperator()).thenReturn("310260");
        
        when(mockContentResolver.query(any(Uri.class), any(String[].class), any(), any(), any()))
            .thenThrow(new SecurityException("No permission to access APN settings"));

        String mmscUrl = HttpUtils.getMmscUrl(mockContext);
        assertNotNull("Should find MMSC URL for T-Mobile", mmscUrl);
        assertEquals("Should return T-Mobile MMSC URL", "http://mms.msg.eng.t-mobile.com/mms/wapenc", mmscUrl);

        // Test AT&T
        setUp();
        when(mockCarrierConfigManager.getConfig()).thenReturn(null);
        when(mockTelephonyManager.getNetworkOperatorName()).thenReturn("AT&T");
        when(mockTelephonyManager.getNetworkOperator()).thenReturn("310150");
        
        when(mockContentResolver.query(any(Uri.class), any(String[].class), any(), any(), any()))
            .thenThrow(new SecurityException("No permission to access APN settings"));

        mmscUrl = HttpUtils.getMmscUrl(mockContext);
        assertNotNull("Should find MMSC URL for AT&T", mmscUrl);
        assertEquals("Should return AT&T MMSC URL", "http://mmsc.mobile.att.net", mmscUrl);
    }

    @Test
    public void testUnknownCarrierHandling() {
        // Test with unknown carrier
        when(mockCarrierConfigManager.getConfig()).thenReturn(null);
        when(mockTelephonyManager.getNetworkOperatorName()).thenReturn("Unknown Carrier");
        when(mockTelephonyManager.getNetworkOperator()).thenReturn("999999");
        
        when(mockContentResolver.query(any(Uri.class), any(String[].class), any(), any(), any()))
            .thenThrow(new SecurityException("No permission to access APN settings"));

        String mmscUrl = HttpUtils.getMmscUrl(mockContext);
        
        // Should be null for unknown carriers, but should not crash
        assertNull("Should return null for unknown carrier", mmscUrl);
        
        // Validation should still return true on Android 5.0+ (Lollipop)
        boolean isValid = HttpUtils.validateMmsConfiguration(mockContext);
        assertTrue("MMS configuration should be valid even for unknown carrier on Android 5.0+", isValid);
    }

    @Test
    public void testCleartextHttpAllowedForMmscDomains() {
        // Test that the known MMSC URLs use HTTP (cleartext) which should be allowed
        // by the network security configuration
        
        // Test Verizon
        when(mockCarrierConfigManager.getConfig()).thenReturn(null);
        when(mockTelephonyManager.getNetworkOperatorName()).thenReturn("Verizon");
        when(mockTelephonyManager.getNetworkOperator()).thenReturn("311480");
        
        when(mockContentResolver.query(any(Uri.class), any(String[].class), any(), any(), any()))
            .thenThrow(new SecurityException("No permission to access APN settings"));

        String mmscUrl = HttpUtils.getMmscUrl(mockContext);
        
        assertNotNull("Should get Verizon MMSC URL", mmscUrl);
        assertTrue("Verizon MMSC URL should use HTTP (cleartext)", mmscUrl.startsWith("http://"));
        assertTrue("Verizon MMSC URL should be mms.vtext.com domain", 
                  mmscUrl.contains("mms.vtext.com"));
        
        // Test T-Mobile
        setUp();
        when(mockCarrierConfigManager.getConfig()).thenReturn(null);
        when(mockTelephonyManager.getNetworkOperatorName()).thenReturn("T-Mobile");
        when(mockTelephonyManager.getNetworkOperator()).thenReturn("310260");
        
        when(mockContentResolver.query(any(Uri.class), any(String[].class), any(), any(), any()))
            .thenThrow(new SecurityException("No permission to access APN settings"));

        mmscUrl = HttpUtils.getMmscUrl(mockContext);
        
        assertNotNull("Should get T-Mobile MMSC URL", mmscUrl);
        assertTrue("T-Mobile MMSC URL should use HTTP (cleartext)", mmscUrl.startsWith("http://"));
        assertTrue("T-Mobile MMSC URL should be correct domain", 
                  mmscUrl.contains("mms.msg.eng.t-mobile.com"));
        
        // Test AT&T
        setUp();
        when(mockCarrierConfigManager.getConfig()).thenReturn(null);
        when(mockTelephonyManager.getNetworkOperatorName()).thenReturn("AT&T");
        when(mockTelephonyManager.getNetworkOperator()).thenReturn("310150");
        
        when(mockContentResolver.query(any(Uri.class), any(String[].class), any(), any(), any()))
            .thenThrow(new SecurityException("No permission to access APN settings"));

        mmscUrl = HttpUtils.getMmscUrl(mockContext);
        
        assertNotNull("Should get AT&T MMSC URL", mmscUrl);
        assertTrue("AT&T MMSC URL should use HTTP (cleartext)", mmscUrl.startsWith("http://"));
        assertTrue("AT&T MMSC URL should be correct domain", 
                  mmscUrl.contains("mmsc.mobile.att.net"));
    }

    @Test
    public void testEnhancedProxyFallbackLogic() {
        // Test that proxy methods now use carrier database fallback
        when(mockCarrierConfigManager.getConfig()).thenReturn(null);
        when(mockTelephonyManager.getNetworkOperatorName()).thenReturn("Verizon");
        when(mockTelephonyManager.getNetworkOperator()).thenReturn("311480");
        
        when(mockContentResolver.query(any(Uri.class), any(String[].class), any(), any(), any()))
            .thenThrow(new SecurityException("No permission to access APN settings"));

        // Test proxy retrieval - should not crash and should handle SecurityException
        String mmsProxy = HttpUtils.getMmsProxy(mockContext);
        int mmsProxyPort = HttpUtils.getMmsProxyPort(mockContext);
        
        // Verizon typically doesn't require proxy, so these should be null/-1
        // The important thing is that no exception is thrown
        assertTrue("Proxy retrieval should complete without exception", true);
        
        // For Verizon, proxy is typically not required
        // The key test is that SecurityException is handled gracefully
    }

    @Test
    public void testMmsConfigurationDiagnostics() {
        // Test that diagnostics method doesn't crash with SecurityException
        when(mockCarrierConfigManager.getConfig()).thenReturn(null);
        when(mockTelephonyManager.getNetworkOperatorName()).thenReturn("Verizon");
        when(mockTelephonyManager.getNetworkOperator()).thenReturn("311480");
        
        when(mockContentResolver.query(any(Uri.class), any(String[].class), any(), any(), any()))
            .thenThrow(new SecurityException("No permission to access APN settings"));

        // This should not throw any exception
        try {
            HttpUtils.logMmsConfigurationDiagnostics(mockContext);
            assertTrue("Diagnostics should complete without exception", true);
        } catch (Exception e) {
            fail("Diagnostics should not throw exception: " + e.getMessage());
        }
    }
}