package com.translator.messagingapp.p2p;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Model class representing P2P connection data that gets encrypted and sent via SMS.
 */
public class P2PConnectionData {
    private String connectionId;
    private String senderPhoneNumber;
    private String signalServerUrl;
    private String iceServers;
    private long timestamp;
    private String deviceId;

    public P2PConnectionData() {
        this.timestamp = System.currentTimeMillis();
    }

    public P2PConnectionData(String connectionId, String senderPhoneNumber, String signalServerUrl, String iceServers, String deviceId) {
        this.connectionId = connectionId;
        this.senderPhoneNumber = senderPhoneNumber;
        this.signalServerUrl = signalServerUrl;
        this.iceServers = iceServers;
        this.deviceId = deviceId;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Converts this object to JSON string for encryption.
     */
    public String toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("connectionId", connectionId);
        json.put("senderPhoneNumber", senderPhoneNumber);
        json.put("signalServerUrl", signalServerUrl);
        json.put("iceServers", iceServers);
        json.put("timestamp", timestamp);
        json.put("deviceId", deviceId);
        return json.toString();
    }

    /**
     * Creates an instance from JSON string after decryption.
     */
    public static P2PConnectionData fromJson(String json) throws JSONException {
        JSONObject jsonObj = new JSONObject(json);
        P2PConnectionData data = new P2PConnectionData();
        data.connectionId = jsonObj.optString("connectionId");
        data.senderPhoneNumber = jsonObj.optString("senderPhoneNumber");
        data.signalServerUrl = jsonObj.optString("signalServerUrl");
        data.iceServers = jsonObj.optString("iceServers");
        data.timestamp = jsonObj.optLong("timestamp", System.currentTimeMillis());
        data.deviceId = jsonObj.optString("deviceId");
        return data;
    }

    /**
     * Validates if the connection data is complete and valid.
     */
    public boolean isValid() {
        return connectionId != null && !connectionId.trim().isEmpty() &&
               senderPhoneNumber != null && !senderPhoneNumber.trim().isEmpty() &&
               signalServerUrl != null && !signalServerUrl.trim().isEmpty() &&
               deviceId != null && !deviceId.trim().isEmpty() &&
               timestamp > 0;
    }

    /**
     * Checks if the connection data is still fresh (within 5 minutes).
     */
    public boolean isFresh() {
        long currentTime = System.currentTimeMillis();
        long maxAge = 5 * 60 * 1000; // 5 minutes in milliseconds
        return (currentTime - timestamp) <= maxAge;
    }

    // Getters and setters
    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getSenderPhoneNumber() {
        return senderPhoneNumber;
    }

    public void setSenderPhoneNumber(String senderPhoneNumber) {
        this.senderPhoneNumber = senderPhoneNumber;
    }

    public String getSignalServerUrl() {
        return signalServerUrl;
    }

    public void setSignalServerUrl(String signalServerUrl) {
        this.signalServerUrl = signalServerUrl;
    }

    public String getIceServers() {
        return iceServers;
    }

    public void setIceServers(String iceServers) {
        this.iceServers = iceServers;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public String toString() {
        return "P2PConnectionData{" +
                "connectionId='" + connectionId + '\'' +
                ", senderPhoneNumber='" + senderPhoneNumber + '\'' +
                ", signalServerUrl='" + signalServerUrl + '\'' +
                ", timestamp=" + timestamp +
                ", deviceId='" + deviceId + '\'' +
                '}';
    }
}