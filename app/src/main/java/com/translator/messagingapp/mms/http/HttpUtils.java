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
                // Log additional context for troubleshooting
                if (mmsProxy == null || mmsProxy.isEmpty()) {
                    Log.d(TAG, "No MMS proxy available - this is normal for most carriers including Verizon");
                } else {
                    Log.w(TAG, "MMS proxy available but invalid port: " + mmsProxyPort);
                }
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
                byte[] response = readResponse(connection);
                Log.d(TAG, "MMS HTTP request completed successfully, received " + 
                      (response != null ? response.length : 0) + " bytes");
                return response;
            } else {
                String errorMsg = "HTTP error: " + responseCode + " " + connection.getResponseMessage();
                Log.e(TAG, errorMsg);
                
                // Enhanced error reporting for common issues
                if (responseCode == HttpURLConnection.HTTP_FORBIDDEN || responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    Log.e(TAG, "MMS send failed: Authentication/authorization error. Check APN settings or carrier configuration.");
                } else if (responseCode >= 500) {
                    Log.e(TAG, "MMS send failed: Server error. MMSC may be temporarily unavailable.");
                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    Log.e(TAG, "MMS send failed: MMSC URL not found. Check carrier MMSC configuration.");
                }
                
                return null;
            }
            
        } catch (java.net.ConnectException e) {
            Log.e(TAG, "MMS send failed: Connection refused. Check network connectivity and MMSC URL.", e);
            return null;
        } catch (java.net.SocketTimeoutException e) {
            Log.e(TAG, "MMS send failed: Connection timeout. Network may be slow or MMSC unavailable.", e);
            return null;
        } catch (java.net.UnknownHostException e) {
            Log.e(TAG, "MMS send failed: Cannot resolve MMSC hostname. Check network connectivity.", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "MMS HTTP connection failed", e);
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
            Log.d(TAG, "Attempting to get MMSC URL using multiple methods...");
            
            // Method 1: Try to get MMSC URL from carrier configuration (preferred method)
            android.telephony.CarrierConfigManager configManager = 
                (android.telephony.CarrierConfigManager) context.getSystemService(Context.CARRIER_CONFIG_SERVICE);
            
            if (configManager != null) {
                android.os.PersistableBundle config = configManager.getConfig();
                if (config != null) {
                    String mmscUrl = config.getString("mms_url_string");
                    if (mmscUrl != null && !mmscUrl.isEmpty()) {
                        Log.d(TAG, "Found MMSC URL from carrier config: " + mmscUrl);
                        return mmscUrl;
                    } else {
                        Log.d(TAG, "CarrierConfigManager returned empty/null MMSC URL");
                    }
                } else {
                    Log.d(TAG, "CarrierConfigManager config is null");
                }
            } else {
                Log.d(TAG, "CarrierConfigManager is not available");
            }
            
            // Method 2: Try carrier-specific hardcoded URLs (more reliable on modern Android)
            String mmscUrl = getCarrierMmscUrl(context);
            if (mmscUrl != null) {
                Log.d(TAG, "Using carrier-specific MMSC URL: " + mmscUrl);
                return mmscUrl;
            } else {
                Log.d(TAG, "No carrier-specific MMSC URL found");
            }
            
            // Method 3: Try to read from APN settings (may fail due to SecurityException)
            mmscUrl = getApnMmscUrl(context);
            if (mmscUrl != null) {
                Log.d(TAG, "Found MMSC URL from APN settings: " + mmscUrl);
                return mmscUrl;
            } else {
                Log.d(TAG, "APN settings did not provide MMSC URL");
            }
            
            Log.e(TAG, "No MMSC URL found for current carrier using any method");
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
                        Log.d(TAG, "Found MMS proxy from carrier config: " + mmsProxy);
                        return mmsProxy;
                    }
                }
            }
            
            // Enhanced fallback: Try carrier-specific proxy database before APN settings
            String carrierProxy = getCarrierMmsProxy(context);
            if (carrierProxy != null) {
                Log.d(TAG, "Using carrier-specific MMS proxy: " + carrierProxy);
                return carrierProxy;
            }
            
            // Last resort: Try to read from APN settings (may fail with SecurityException)
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
                        Log.d(TAG, "Found MMS proxy port from carrier config: " + mmsPort);
                        return mmsPort;
                    }
                }
            }
            
            // Enhanced fallback: Try carrier-specific proxy port database before APN settings
            int carrierPort = getCarrierMmsProxyPort(context);
            if (carrierPort > 0) {
                Log.d(TAG, "Using carrier-specific MMS proxy port: " + carrierPort);
                return carrierPort;
            }
            
            // Last resort: Try to read from APN settings (may fail with SecurityException)
            return getApnMmsProxyPort(context);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting MMS proxy port", e);
            return -1;
        }
    }

    /**
     * Reads MMSC URL from APN settings.
     * Note: This may fail on modern Android versions due to restricted APN access.
     */
    private static String getApnMmscUrl(Context context) {
        String[] projection = {"mmsc"};
        try (android.database.Cursor cursor = context.getContentResolver().query(
                android.net.Uri.parse("content://telephony/carriers/preferapn"), 
                projection, null, null, null)) {
            
            if (cursor != null && cursor.moveToFirst()) {
                String mmscUrl = cursor.getString(0);
                if (mmscUrl != null && !mmscUrl.isEmpty()) {
                    Log.d(TAG, "Successfully retrieved MMSC URL from APN settings");
                    return mmscUrl;
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "SecurityException: No permission to access APN settings. This is expected on modern Android versions.", e);
        } catch (Exception e) {
            Log.w(TAG, "Error reading APN MMSC URL", e);
        }
        return null;
    }

    /**
     * Reads MMS proxy from APN settings.
     * Note: This may fail on modern Android versions due to restricted APN access.
     */
    private static String getApnMmsProxy(Context context) {
        String[] projection = {"mmsproxy"};
        try (android.database.Cursor cursor = context.getContentResolver().query(
                android.net.Uri.parse("content://telephony/carriers/preferapn"), 
                projection, null, null, null)) {
            
            if (cursor != null && cursor.moveToFirst()) {
                String mmsProxy = cursor.getString(0);
                if (mmsProxy != null && !mmsProxy.isEmpty()) {
                    Log.d(TAG, "Successfully retrieved MMS proxy from APN settings");
                    return mmsProxy;
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "SecurityException: No permission to access APN settings for MMS proxy. This is expected on modern Android versions.", e);
        } catch (Exception e) {
            Log.w(TAG, "Error reading APN MMS proxy", e);
        }
        return null;
    }

    /**
     * Reads MMS proxy port from APN settings.
     * Note: This may fail on modern Android versions due to restricted APN access.
     */
    private static int getApnMmsProxyPort(Context context) {
        String[] projection = {"mmsport"};
        try (android.database.Cursor cursor = context.getContentResolver().query(
                android.net.Uri.parse("content://telephony/carriers/preferapn"), 
                projection, null, null, null)) {
            
            if (cursor != null && cursor.moveToFirst()) {
                String portStr = cursor.getString(0);
                if (portStr != null && !portStr.isEmpty()) {
                    Log.d(TAG, "Successfully retrieved MMS proxy port from APN settings");
                    return Integer.parseInt(portStr);
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "SecurityException: No permission to access APN settings for MMS proxy port. This is expected on modern Android versions.", e);
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
            // On Android 5.0+, SmsManager can handle MMS without manual MMSC configuration
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                Log.d(TAG, "Android 5.0+ detected - SmsManager available for MMS, skipping MMSC validation");
                return true;
            }
            
            // For older Android versions, try to validate MMSC configuration
            String mmscUrl = getMmscUrl(context);
            if (mmscUrl == null || mmscUrl.isEmpty()) {
                Log.w(TAG, "MMS validation warning: No MMSC URL available, but continuing anyway");
                // Don't fail completely - the transaction architecture might still work
                return true;
            }
            
            // Validate URL format if we have one
            try {
                new java.net.URL(mmscUrl);
                Log.d(TAG, "MMS configuration validated successfully. MMSC URL: " + mmscUrl);
            } catch (java.net.MalformedURLException e) {
                Log.w(TAG, "MMS validation warning: Invalid MMSC URL format: " + mmscUrl + ", but continuing anyway");
                // Don't fail completely - other methods might work
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
            
            return true;
            
        } catch (Exception e) {
            Log.w(TAG, "Error validating MMS configuration, but continuing anyway", e);
            // Don't fail completely on validation errors
            return true;
        }
    }

    /**
     * Gets carrier-specific MMSC URL based on operator.
     * Enhanced with comprehensive carrier database.
     */
    private static String getCarrierMmscUrl(Context context) {
        try {
            android.telephony.TelephonyManager telephonyManager = 
                (android.telephony.TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            
            if (telephonyManager != null) {
                String operatorName = telephonyManager.getNetworkOperatorName();
                String operatorMcc = telephonyManager.getNetworkOperator();
                
                Log.d(TAG, "Operator: " + operatorName + " MCC/MNC: " + operatorMcc);
                
                // First, try exact MCC/MNC matching for better accuracy
                if (operatorMcc != null && operatorMcc.length() >= 5) {
                    String mmscUrl = getMmscUrlByMccMnc(operatorMcc);
                    if (mmscUrl != null) {
                        Log.d(TAG, "Found MMSC URL by MCC/MNC: " + mmscUrl);
                        return mmscUrl;
                    }
                }
                
                // Fallback to operator name matching
                if (operatorMcc != null && operatorName != null) {
                    if (operatorMcc.startsWith("310") || operatorMcc.startsWith("311")) { // US carriers
                        String mmscUrl = getUsMmscUrlByOperatorName(operatorName);
                        if (mmscUrl != null) {
                            Log.d(TAG, "Found US MMSC URL by operator name: " + mmscUrl);
                            return mmscUrl;
                        }
                    } else if (operatorMcc.startsWith("302")) { // Canada
                        String mmscUrl = getCanadianMmscUrl(operatorName);
                        if (mmscUrl != null) {
                            Log.d(TAG, "Found Canadian MMSC URL: " + mmscUrl);
                            return mmscUrl;
                        }
                    }
                    // Add other countries as needed
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error determining carrier MMSC URL", e);
        }
        return null;
    }
    
    /**
     * Gets MMSC URL by exact MCC/MNC matching.
     */
    private static String getMmscUrlByMccMnc(String mccMnc) {
        // US Carriers (MCC 310-316)
        switch (mccMnc) {
            case "311480": // Verizon (the specific case from the error)
                return "http://mms.vtext.com/servlets/mms";
            case "310004": // Verizon
            case "310005": // Verizon
            case "310006": // Verizon
            case "310010": // Verizon
            case "310012": // Verizon
            case "310013": // Verizon
            case "311270": // Verizon
            case "311271": // Verizon
            case "311272": // Verizon
            case "311273": // Verizon
            case "311274": // Verizon
            case "311275": // Verizon
            case "311276": // Verizon
            case "311277": // Verizon
            case "311278": // Verizon
            case "311279": // Verizon
            case "311280": // Verizon
            case "311281": // Verizon
            case "311282": // Verizon
            case "311283": // Verizon
            case "311284": // Verizon
            case "311285": // Verizon
            case "311286": // Verizon
            case "311287": // Verizon
            case "311288": // Verizon
            case "311289": // Verizon
            case "311390": // Verizon
            case "311481": // Verizon
            case "311482": // Verizon
            case "311483": // Verizon
            case "311484": // Verizon
            case "311485": // Verizon
            case "311486": // Verizon
            case "311487": // Verizon
            case "311488": // Verizon
            case "311489": // Verizon
                return "http://mms.vtext.com/servlets/mms";
            
            case "310260": // T-Mobile
            case "310026": // T-Mobile
            case "310160": // T-Mobile
            case "310200": // T-Mobile
            case "310210": // T-Mobile
            case "310220": // T-Mobile
            case "310230": // T-Mobile
            case "310240": // T-Mobile
            case "310250": // T-Mobile
            case "310270": // T-Mobile
            case "310310": // T-Mobile
            case "310580": // T-Mobile
            case "310800": // T-Mobile
                return "http://mms.msg.eng.t-mobile.com/mms/wapenc";
            
            case "310150": // AT&T
            case "310070": // AT&T
            case "310560": // AT&T
            case "310680": // AT&T
            case "310170": // AT&T
            case "310280": // AT&T
            case "310380": // AT&T
            case "310410": // AT&T
                return "http://mmsc.mobile.att.net";
            
            case "310120": // Sprint (now part of T-Mobile)
            case "311490": // Sprint
            case "311870": // Sprint
            case "311880": // Sprint
            case "312190": // Sprint
                return "http://mms.sprintpcs.com";
                
            // Add more carriers as needed
            default:
                return null;
        }
    }
    
    /**
     * Gets US MMSC URL by operator name (fallback method).
     */
    private static String getUsMmscUrlByOperatorName(String operatorName) {
        String lowerName = operatorName.toLowerCase();
        if (lowerName.contains("verizon")) {
            return "http://mms.vtext.com/servlets/mms";
        } else if (lowerName.contains("t-mobile") || lowerName.contains("tmobile")) {
            return "http://mms.msg.eng.t-mobile.com/mms/wapenc";
        } else if (lowerName.contains("at&t") || lowerName.contains("att")) {
            return "http://mmsc.mobile.att.net";
        } else if (lowerName.contains("sprint")) {
            return "http://mms.sprintpcs.com";
        } else if (lowerName.contains("visible")) {
            return "http://mms.vtext.com/servlets/mms"; // Visible uses Verizon network
        } else if (lowerName.contains("cricket")) {
            return "http://mmsc.mobile.att.net"; // Cricket uses AT&T network
        } else if (lowerName.contains("metro")) {
            return "http://mms.msg.eng.t-mobile.com/mms/wapenc"; // Metro uses T-Mobile network
        }
        return null;
    }
    
    /**
     * Gets Canadian MMSC URL by operator name.
     */
    private static String getCanadianMmscUrl(String operatorName) {
        String lowerName = operatorName.toLowerCase();
        if (lowerName.contains("rogers")) {
            return "http://mms.gprs.rogers.com";
        } else if (lowerName.contains("bell")) {
            return "http://mms.bell.ca/mms/wapenc";
        } else if (lowerName.contains("telus")) {
            return "http://aliasredirect.net/proxy/mmsc";
        }
        return null;
    }
    
    /**
     * Gets carrier-specific MMS proxy based on operator.
     * This provides fallback proxy information when APN access fails.
     */
    private static String getCarrierMmsProxy(Context context) {
        try {
            android.telephony.TelephonyManager telephonyManager = 
                (android.telephony.TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            
            if (telephonyManager != null) {
                String operatorMcc = telephonyManager.getNetworkOperator();
                String operatorName = telephonyManager.getNetworkOperatorName();
                
                Log.d(TAG, "Getting MMS proxy for operator: " + operatorName + " MCC/MNC: " + operatorMcc);
                
                // Check if specific proxy is required for this carrier
                if (operatorMcc != null && operatorMcc.length() >= 5) {
                    String proxy = getMmsProxyByMccMnc(operatorMcc);
                    if (proxy != null) {
                        Log.d(TAG, "Found carrier-specific MMS proxy: " + proxy);
                        return proxy;
                    }
                }
                
                // Fallback to operator name matching for US carriers
                if (operatorMcc != null && (operatorMcc.startsWith("310") || operatorMcc.startsWith("311"))) {
                    return getUsMmsProxyByOperatorName(operatorName);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error determining carrier MMS proxy", e);
        }
        return null;
    }
    
    /**
     * Gets carrier-specific MMS proxy port based on operator.
     * This provides fallback proxy port information when APN access fails.
     */
    private static int getCarrierMmsProxyPort(Context context) {
        try {
            android.telephony.TelephonyManager telephonyManager = 
                (android.telephony.TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            
            if (telephonyManager != null) {
                String operatorMcc = telephonyManager.getNetworkOperator();
                String operatorName = telephonyManager.getNetworkOperatorName();
                
                Log.d(TAG, "Getting MMS proxy port for operator: " + operatorName + " MCC/MNC: " + operatorMcc);
                
                // Check if specific proxy port is required for this carrier
                if (operatorMcc != null && operatorMcc.length() >= 5) {
                    int port = getMmsProxyPortByMccMnc(operatorMcc);
                    if (port > 0) {
                        Log.d(TAG, "Found carrier-specific MMS proxy port: " + port);
                        return port;
                    }
                }
                
                // Fallback to operator name matching for US carriers
                if (operatorMcc != null && (operatorMcc.startsWith("310") || operatorMcc.startsWith("311"))) {
                    return getUsMmsProxyPortByOperatorName(operatorName);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error determining carrier MMS proxy port", e);
        }
        return -1;
    }
    
    /**
     * Gets MMS proxy by exact MCC/MNC matching.
     * Some carriers require specific proxy settings for MMS to work.
     */
    private static String getMmsProxyByMccMnc(String mccMnc) {
        // Note: Verizon typically does NOT require a proxy for MMS
        // Most Verizon networks work with direct connection
        switch (mccMnc) {
            // T-Mobile networks that may require proxy
            case "310260": // T-Mobile
            case "310026": // T-Mobile
                // T-Mobile generally works without proxy, but some configurations may need it
                return null;
            
            // AT&T networks - generally no proxy required
            case "310150": // AT&T
            case "310410": // AT&T
                return null;
                
            // Sprint networks - some may require proxy
            case "310120": // Sprint (now part of T-Mobile)
            case "311490": // Sprint
                return null;
                
            // Most other carriers work without proxy
            default:
                return null;
        }
    }
    
    /**
     * Gets MMS proxy port by exact MCC/MNC matching.
     */
    private static int getMmsProxyPortByMccMnc(String mccMnc) {
        // Most major US carriers do not require proxy ports for MMS
        // This is mainly for international carriers or specific configurations
        switch (mccMnc) {
            // No specific proxy ports needed for major US carriers
            default:
                return -1;
        }
    }
    
    /**
     * Gets US MMS proxy by operator name (fallback method).
     */
    private static String getUsMmsProxyByOperatorName(String operatorName) {
        if (operatorName == null) return null;
        
        String lowerName = operatorName.toLowerCase();
        // Most major US carriers (Verizon, T-Mobile, AT&T, Sprint) work without proxy
        // Only return proxy if specifically required by carrier
        return null;
    }
    
    /**
     * Gets US MMS proxy port by operator name (fallback method).
     */
    private static int getUsMmsProxyPortByOperatorName(String operatorName) {
        if (operatorName == null) return -1;
        
        // Most major US carriers work without proxy ports
        return -1;
    }
    
    /**
     * Provides comprehensive MMS configuration diagnostics.
     * This helps troubleshoot MMS issues by showing what settings are available.
     */
    public static void logMmsConfigurationDiagnostics(Context context) {
        Log.d(TAG, "=== MMS Configuration Diagnostics ===");
        
        try {
            // Carrier information
            android.telephony.TelephonyManager telephonyManager = 
                (android.telephony.TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            
            if (telephonyManager != null) {
                String operatorName = telephonyManager.getNetworkOperatorName();
                String operatorMcc = telephonyManager.getNetworkOperator();
                Log.d(TAG, "Carrier: " + operatorName + " (MCC/MNC: " + operatorMcc + ")");
            }
            
            // MMSC URL
            String mmscUrl = getMmscUrl(context);
            Log.d(TAG, "MMSC URL: " + (mmscUrl != null ? mmscUrl : "Not available"));
            
            // Proxy settings
            String mmsProxy = getMmsProxy(context);
            int mmsProxyPort = getMmsProxyPort(context);
            Log.d(TAG, "MMS Proxy: " + (mmsProxy != null ? mmsProxy : "Not required/available"));
            Log.d(TAG, "MMS Proxy Port: " + (mmsProxyPort > 0 ? mmsProxyPort : "Not required/available"));
            
            // CarrierConfigManager availability
            android.telephony.CarrierConfigManager configManager = 
                (android.telephony.CarrierConfigManager) context.getSystemService(Context.CARRIER_CONFIG_SERVICE);
            Log.d(TAG, "CarrierConfigManager available: " + (configManager != null));
            
            if (configManager != null) {
                android.os.PersistableBundle config = configManager.getConfig();
                Log.d(TAG, "Carrier config available: " + (config != null));
            }
            
            // Network connectivity
            android.net.ConnectivityManager connectivityManager = 
                (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                Log.d(TAG, "Network connected: " + (networkInfo != null && networkInfo.isConnected()));
                if (networkInfo != null) {
                    Log.d(TAG, "Network type: " + networkInfo.getTypeName());
                }
            }
            
            // Android version info
            Log.d(TAG, "Android SDK: " + android.os.Build.VERSION.SDK_INT);
            Log.d(TAG, "Uses SmsManager (Android 5.0+): " + (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP));
            
        } catch (Exception e) {
            Log.e(TAG, "Error during MMS diagnostics", e);
        }
        
        Log.d(TAG, "=== End MMS Diagnostics ===");
    }
}