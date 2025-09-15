package com.translator.messagingapp.p2p;

import org.junit.Test;
import org.junit.Before;
import org.json.JSONException;
import static org.junit.Assert.*;

/**
 * Unit tests for P2PConnectionData model.
 */
public class P2PConnectionDataTest {

    private P2PConnectionData connectionData;

    @Before
    public void setUp() {
        connectionData = new P2PConnectionData();
        connectionData.setConnectionId("test-connection-123");
        connectionData.setSenderPhoneNumber("+1234567890");
        connectionData.setSignalServerUrl("stun:stun.l.google.com:19302");
        connectionData.setIceServers("[{\"url\":\"stun:stun.l.google.com:19302\"}]");
        connectionData.setDeviceId("test-device-id");
    }

    @Test
    public void testValidConnectionData() {
        assertTrue("Valid connection data should pass validation", connectionData.isValid());
    }

    @Test
    public void testInvalidConnectionDataMissingFields() {
        P2PConnectionData invalidData = new P2PConnectionData();
        assertFalse("Empty connection data should be invalid", invalidData.isValid());

        invalidData.setConnectionId("test-id");
        assertFalse("Connection data with only ID should be invalid", invalidData.isValid());

        invalidData.setSenderPhoneNumber("+1234567890");
        assertFalse("Connection data missing other fields should be invalid", invalidData.isValid());
    }

    @Test
    public void testConnectionDataWithEmptyStrings() {
        connectionData.setConnectionId("");
        assertFalse("Connection data with empty ID should be invalid", connectionData.isValid());

        connectionData.setConnectionId("test-id");
        connectionData.setSenderPhoneNumber("   ");
        assertFalse("Connection data with whitespace-only phone should be invalid", connectionData.isValid());
    }

    @Test
    public void testFreshnessValidation() throws InterruptedException {
        // Newly created data should be fresh
        P2PConnectionData freshData = new P2PConnectionData("id", "phone", "url", "ice", "device");
        assertTrue("Newly created connection data should be fresh", freshData.isFresh());

        // Test with old timestamp
        P2PConnectionData staleData = new P2PConnectionData();
        staleData.setTimestamp(System.currentTimeMillis() - (10 * 60 * 1000)); // 10 minutes ago
        assertFalse("Data older than 5 minutes should be stale", staleData.isFresh());
    }

    @Test
    public void testJsonSerialization() throws JSONException {
        String json = connectionData.toJson();
        assertNotNull("JSON should not be null", json);
        assertTrue("JSON should contain connection ID", json.contains(connectionData.getConnectionId()));
        assertTrue("JSON should contain phone number", json.contains(connectionData.getSenderPhoneNumber()));
    }

    @Test
    public void testJsonDeserialization() throws JSONException {
        String json = connectionData.toJson();
        P2PConnectionData deserializedData = P2PConnectionData.fromJson(json);
        
        assertEquals("Connection ID should match", connectionData.getConnectionId(), deserializedData.getConnectionId());
        assertEquals("Phone number should match", connectionData.getSenderPhoneNumber(), deserializedData.getSenderPhoneNumber());
        assertEquals("Signal server URL should match", connectionData.getSignalServerUrl(), deserializedData.getSignalServerUrl());
        assertEquals("ICE servers should match", connectionData.getIceServers(), deserializedData.getIceServers());
        assertEquals("Device ID should match", connectionData.getDeviceId(), deserializedData.getDeviceId());
    }

    @Test
    public void testJsonRoundTrip() throws JSONException {
        String originalJson = connectionData.toJson();
        P2PConnectionData deserializedData = P2PConnectionData.fromJson(originalJson);
        String secondJson = deserializedData.toJson();
        
        P2PConnectionData secondDeserializedData = P2PConnectionData.fromJson(secondJson);
        assertEquals("Data should survive round trip", connectionData.getConnectionId(), secondDeserializedData.getConnectionId());
    }

    @Test
    public void testInvalidJsonHandling() {
        try {
            P2PConnectionData.fromJson("invalid json");
            fail("Should throw JSONException for invalid JSON");
        } catch (JSONException e) {
            // Expected behavior
        }
    }

    @Test
    public void testEmptyJsonHandling() throws JSONException {
        P2PConnectionData dataFromEmptyJson = P2PConnectionData.fromJson("{}");
        assertFalse("Data from empty JSON should be invalid", dataFromEmptyJson.isValid());
    }

    @Test
    public void testTimestampHandling() throws JSONException {
        long testTimestamp = System.currentTimeMillis() - 1000; // 1 second ago
        connectionData.setTimestamp(testTimestamp);
        
        String json = connectionData.toJson();
        P2PConnectionData deserializedData = P2PConnectionData.fromJson(json);
        
        assertEquals("Timestamp should be preserved", testTimestamp, deserializedData.getTimestamp());
    }

    @Test
    public void testToStringMethod() {
        String toString = connectionData.toString();
        assertNotNull("toString should not be null", toString);
        assertTrue("toString should contain connection ID", toString.contains(connectionData.getConnectionId()));
        assertTrue("toString should contain phone number", toString.contains(connectionData.getSenderPhoneNumber()));
    }

    @Test
    public void testConstructorWithParameters() {
        P2PConnectionData constructedData = new P2PConnectionData(
            "test-id", "+1234567890", "stun:server", "[]", "device-id"
        );
        
        assertTrue("Constructed data should be valid", constructedData.isValid());
        assertTrue("Constructed data should be fresh", constructedData.isFresh());
        assertEquals("Connection ID should match", "test-id", constructedData.getConnectionId());
    }
}