package com.translator.messagingapp.mms.http;

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
            connection = (HttpURLConnection) url.openConnection();
            
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
        // This would use ApnSettings to get the actual MMSC URL
        // For now, return a placeholder
        return "http://mmsc.example.com/mms";
    }

    /**
     * Gets the MMS proxy for the current carrier.
     *
     * @param context The application context
     * @return The MMS proxy, or null if not available
     */
    public static String getMmsProxy(Context context) {
        // This would use ApnSettings to get the actual MMS proxy
        // For now, return null (no proxy)
        return null;
    }

    /**
     * Gets the MMS proxy port for the current carrier.
     *
     * @param context The application context
     * @return The MMS proxy port, or -1 if not available
     */
    public static int getMmsProxyPort(Context context) {
        // This would use ApnSettings to get the actual MMS proxy port
        // For now, return -1 (no proxy)
        return -1;
    }
}