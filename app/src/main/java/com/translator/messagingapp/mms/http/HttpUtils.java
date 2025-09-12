package com.translator.messagingapp.mms.http;

import com.translator.messagingapp.message.*;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility class for HTTP communication with MMSC servers.
 */
public class HttpUtils {
    private static final String TAG = "HttpUtils";
    
    // HTTP methods
    public static final String HTTP_POST_METHOD = "POST";
    public static final String HTTP_GET_METHOD = "GET";
    
    // Timeouts
    private static final int HTTP_POST_TIMEOUT = 60 * 1000; // 60 seconds
    private static final int HTTP_GET_TIMEOUT = 30 * 1000;  // 30 seconds
    
    // Content types
    public static final String CONTENT_TYPE_MMS = "application/vnd.wap.mms-message";

    /**
     * Performs an HTTP connection to send or receive MMS data.
     *
     * @param context The application context
     * @param token The token for network operations
     * @param urlString The URL to connect to
     * @param pdu The PDU data to send (for POST), null for GET
     * @param contentType The content type
     * @return The response data, or null if failed
     */
    public static byte[] httpConnection(Context context, long token, String urlString, 
                                       byte[] pdu, String contentType) {
        return httpConnection(context, token, urlString, pdu, contentType, 
                            pdu != null ? HTTP_POST_METHOD : HTTP_GET_METHOD);
    }

    /**
     * Performs an HTTP connection with the specified method.
     *
     * @param context The application context
     * @param token The token for network operations
     * @param urlString The URL to connect to
     * @param pdu The PDU data to send (for POST), null for GET
     * @param contentType The content type
     * @param method The HTTP method (POST or GET)
     * @return The response data, or null if failed
     */
    public static byte[] httpConnection(Context context, long token, String urlString, 
                                       byte[] pdu, String contentType, String method) {
        if (urlString == null || urlString.isEmpty()) {
            Log.e(TAG, "Invalid URL");
            return null;
        }

        HttpURLConnection connection = null;
        try {
            Log.d(TAG, "HTTP " + method + " to: " + urlString);
            
            URL url = new URL(urlString);
            
            // Check if we need to use a proxy
            String mmsProxy = getMmsProxy(context);
            int mmsProxyPort = getMmsProxyPort(context);
            
            if (mmsProxy != null && !mmsProxy.isEmpty() && mmsProxyPort > 0) {
                Log.d(TAG, "Using MMS proxy: " + mmsProxy + ":" + mmsProxyPort);
                java.net.Proxy proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, 
                    new java.net.InetSocketAddress(mmsProxy, mmsProxyPort));
                connection = (HttpURLConnection) url.openConnection(proxy);
            } else {
                Log.d(TAG, "Using direct connection (no proxy)");
                connection = (HttpURLConnection) url.openConnection();
            }
            
            // Configure connection
            setupConnection(connection, method, contentType);
            
            // Send data for POST requests
            if (HTTP_POST_METHOD.equals(method) && pdu != null) {
                sendData(connection, pdu);
            }
            
            // Get response
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "HTTP response code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return readResponse(connection);
            } else {
                Log.e(TAG, "HTTP error: " + responseCode + " " + connection.getResponseMessage());
                return null;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "HTTP connection failed", e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Sets up the HTTP connection with appropriate headers and timeouts.
     *
     * @param connection The HTTP connection
     * @param method The HTTP method
     * @param contentType The content type
     * @throws IOException If setup fails
     */
    private static void setupConnection(HttpURLConnection connection, String method, 
                                       String contentType) throws IOException {
        connection.setRequestMethod(method);
        
        if (HTTP_POST_METHOD.equals(method)) {
            connection.setDoOutput(true);
            connection.setConnectTimeout(HTTP_POST_TIMEOUT);
            connection.setReadTimeout(HTTP_POST_TIMEOUT);
        } else {
            connection.setConnectTimeout(HTTP_GET_TIMEOUT);
            connection.setReadTimeout(HTTP_GET_TIMEOUT);
        }
        
        // Set headers
        if (contentType != null) {
            connection.setRequestProperty("Content-Type", contentType);
        }
        connection.setRequestProperty("Accept", CONTENT_TYPE_MMS);
        connection.setRequestProperty("User-Agent", "Android MMS/1.0");
        connection.setRequestProperty("Accept-Language", "en-US");
        
        // Disable caching
        connection.setUseCaches(false);
    }

    /**
     * Sends data to the server.
     *
     * @param connection The HTTP connection
     * @param data The data to send
     * @throws IOException If sending fails
     */
    private static void sendData(HttpURLConnection connection, byte[] data) throws IOException {
        Log.d(TAG, "Sending " + data.length + " bytes");
        connection.getOutputStream().write(data);
        connection.getOutputStream().flush();
    }

    /**
     * Reads the response from the server.
     *
     * @param connection The HTTP connection
     * @return The response data
     * @throws IOException If reading fails
     */
    private static byte[] readResponse(HttpURLConnection connection) throws IOException {
        InputStream inputStream = connection.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        
        byte[] response = baos.toByteArray();
        Log.d(TAG, "Received " + response.length + " bytes");
        
        return response;
    }

    /**
     * Gets the MMSC URL for the current carrier.
     *
     * @param context The application context
     * @return The MMSC URL, or null if not available
     */
    public static String getMmscUrl(Context context) {
        try {
            // Try to get MMSC URL from carrier configuration
            android.telephony.CarrierConfigManager configManager = 
                (android.telephony.CarrierConfigManager) context.getSystemService(Context.CARRIER_CONFIG_SERVICE);
            
            if (configManager != null) {
                android.os.PersistableBundle config = configManager.getConfig();
                if (config != null) {
                    String mmscUrl = config.getString("mms_url_string");
                    if (mmscUrl != null && !mmscUrl.isEmpty()) {
                        Log.d(TAG, "Found MMSC URL from carrier config: " + mmscUrl);
                        return mmscUrl;
                    }
                }
            }
            
            // Fallback: Try to read from APN settings
            String mmscUrl = getApnMmscUrl(context);
            if (mmscUrl != null) {
                Log.d(TAG, "Found MMSC URL from APN settings: " + mmscUrl);
                return mmscUrl;
            }
            
            // Last resort: Try some common carrier MMSC URLs based on operator
            mmscUrl = getCarrierMmscUrl(context);
            if (mmscUrl != null) {
                Log.d(TAG, "Using carrier-specific MMSC URL: " + mmscUrl);
                return mmscUrl;
            }
            
            Log.e(TAG, "No MMSC URL found for current carrier");
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting MMSC URL", e);
            return null;
        }
    }

    /**
     * Gets the MMS proxy for the current carrier.
     *
     * @param context The application context
     * @return The MMS proxy, or null if not available
     */
    public static String getMmsProxy(Context context) {
        try {
            // Try to get MMS proxy from carrier configuration
            android.telephony.CarrierConfigManager configManager = 
                (android.telephony.CarrierConfigManager) context.getSystemService(Context.CARRIER_CONFIG_SERVICE);
            
            if (configManager != null) {
                android.os.PersistableBundle config = configManager.getConfig();
                if (config != null) {
                    String mmsProxy = config.getString("mms_http_proxy_string");
                    if (mmsProxy != null && !mmsProxy.isEmpty()) {
                        return mmsProxy;
                    }
                }
            }
            
            // Fallback: Try to read from APN settings
            return getApnMmsProxy(context);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting MMS proxy", e);
            return null;
        }
    }

    /**
     * Gets the MMS proxy port for the current carrier.
     *
     * @param context The application context
     * @return The MMS proxy port, or -1 if not available
     */
    public static int getMmsProxyPort(Context context) {
        try {
            // Try to get MMS proxy port from carrier configuration
            android.telephony.CarrierConfigManager configManager = 
                (android.telephony.CarrierConfigManager) context.getSystemService(Context.CARRIER_CONFIG_SERVICE);
            
            if (configManager != null) {
                android.os.PersistableBundle config = configManager.getConfig();
                if (config != null) {
                    int mmsPort = config.getInt("mms_http_proxy_port_int");
                    if (mmsPort > 0) {
                        return mmsPort;
                    }
                }
            }
            
            // Fallback: Try to read from APN settings
            return getApnMmsProxyPort(context);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting MMS proxy port", e);
            return -1;
        }
    }

    /**
     * Reads MMSC URL from APN settings.
     */
    private static String getApnMmscUrl(Context context) {
        String[] projection = {"mmsc"};
        try (android.database.Cursor cursor = context.getContentResolver().query(
                android.net.Uri.parse("content://telephony/carriers/preferapn"), 
                projection, null, null, null)) {
            
            if (cursor != null && cursor.moveToFirst()) {
                String mmscUrl = cursor.getString(0);
                if (mmscUrl != null && !mmscUrl.isEmpty()) {
                    return mmscUrl;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error reading APN MMSC URL", e);
        }
        return null;
    }

    /**
     * Reads MMS proxy from APN settings.
     */
    private static String getApnMmsProxy(Context context) {
        String[] projection = {"mmsproxy"};
        try (android.database.Cursor cursor = context.getContentResolver().query(
                android.net.Uri.parse("content://telephony/carriers/preferapn"), 
                projection, null, null, null)) {
            
            if (cursor != null && cursor.moveToFirst()) {
                String mmsProxy = cursor.getString(0);
                if (mmsProxy != null && !mmsProxy.isEmpty()) {
                    return mmsProxy;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error reading APN MMS proxy", e);
        }
        return null;
    }

    /**
     * Reads MMS proxy port from APN settings.
     */
    private static int getApnMmsProxyPort(Context context) {
        String[] projection = {"mmsport"};
        try (android.database.Cursor cursor = context.getContentResolver().query(
                android.net.Uri.parse("content://telephony/carriers/preferapn"), 
                projection, null, null, null)) {
            
            if (cursor != null && cursor.moveToFirst()) {
                String portStr = cursor.getString(0);
                if (portStr != null && !portStr.isEmpty()) {
                    return Integer.parseInt(portStr);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error reading APN MMS proxy port", e);
        }
        return -1;
    }

    /**
     * Validates that MMS configuration is available and correct.
     * @param context The application context
     * @return True if MMS can be configured, false otherwise
     */
    public static boolean validateMmsConfiguration(Context context) {
        try {
            // Check MMSC URL
            String mmscUrl = getMmscUrl(context);
            if (mmscUrl == null || mmscUrl.isEmpty()) {
                Log.e(TAG, "MMS validation failed: No MMSC URL available");
                return false;
            }
            
            // Validate URL format
            try {
                new java.net.URL(mmscUrl);
            } catch (java.net.MalformedURLException e) {
                Log.e(TAG, "MMS validation failed: Invalid MMSC URL format: " + mmscUrl);
                return false;
            }
            
            // Check network connectivity
            android.net.ConnectivityManager connectivityManager = 
                (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo == null || !networkInfo.isConnected()) {
                    Log.w(TAG, "MMS validation warning: No active network connection");
                    // Don't return false here as network might come back
                }
            }
            
            Log.d(TAG, "MMS configuration validated successfully. MMSC URL: " + mmscUrl);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error validating MMS configuration", e);
            return false;
        }
    }

    /**
     * Gets carrier-specific MMSC URL based on operator.
     */
    private static String getCarrierMmscUrl(Context context) {
        try {
            android.telephony.TelephonyManager telephonyManager = 
                (android.telephony.TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            
            if (telephonyManager != null) {
                String operatorName = telephonyManager.getNetworkOperatorName();
                String operatorMcc = telephonyManager.getNetworkOperator();
                
                Log.d(TAG, "Operator: " + operatorName + " MCC/MNC: " + operatorMcc);
                
                // Common US carriers
                if (operatorMcc != null) {
                    if (operatorMcc.startsWith("310")) { // US carriers
                        if (operatorName != null) {
                            String lowerName = operatorName.toLowerCase();
                            if (lowerName.contains("verizon")) {
                                return "http://mms.vtext.com/servlets/mms";
                            } else if (lowerName.contains("t-mobile") || lowerName.contains("tmobile")) {
                                return "http://mms.msg.eng.t-mobile.com/mms/wapenc";
                            } else if (lowerName.contains("at&t") || lowerName.contains("att")) {
                                return "http://mmsc.mobile.att.net";
                            } else if (lowerName.contains("sprint")) {
                                return "http://mms.sprintpcs.com";
                            }
                        }
                    }
                    // Add other countries/carriers as needed
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error determining carrier MMSC URL", e);
        }
        return null;
    }
}