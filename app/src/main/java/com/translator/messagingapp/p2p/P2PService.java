package com.translator.messagingapp.p2p;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Service class that manages P2P connections and handles message routing.
 */
public class P2PService implements P2PConnectionManager.P2PConnectionListener {
    private static final String TAG = "P2PService";
    
    public static final String ACTION_P2P_CONNECTION_ESTABLISHED = "com.translator.messagingapp.P2P_CONNECTION_ESTABLISHED";
    public static final String ACTION_P2P_CONNECTION_FAILED = "com.translator.messagingapp.P2P_CONNECTION_FAILED";
    public static final String ACTION_P2P_MESSAGE_RECEIVED = "com.translator.messagingapp.P2P_MESSAGE_RECEIVED";
    public static final String ACTION_P2P_CONNECTION_CLOSED = "com.translator.messagingapp.P2P_CONNECTION_CLOSED";
    
    public static final String EXTRA_CONNECTION_ID = "connection_id";
    public static final String EXTRA_PHONE_NUMBER = "phone_number";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_ERROR = "error";

    private Context context;
    private Map<String, P2PConnectionManager> activeConnections;
    private Map<String, String> phoneNumberToConnectionId;

    public P2PService(Context context) {
        this.context = context;
        this.activeConnections = new ConcurrentHashMap<>();
        this.phoneNumberToConnectionId = new ConcurrentHashMap<>();
    }

    /**
     * Processes a P2P trigger SMS and attempts to establish connection.
     */
    public void handleP2PTrigger(String senderPhoneNumber, String encryptedPayload) {
        Log.d(TAG, "Processing P2P trigger from: " + senderPhoneNumber);
        
        try {
            // Decrypt the connection data
            String decryptedData = P2PEncryptionUtils.decryptConnectionData(context, encryptedPayload);
            if (decryptedData == null) {
                Log.e(TAG, "Failed to decrypt P2P connection data");
                broadcastConnectionFailed(senderPhoneNumber, "Failed to decrypt connection data");
                return;
            }

            // Parse the connection data
            P2PConnectionData connectionData = P2PConnectionData.fromJson(decryptedData);
            if (!connectionData.isValid()) {
                Log.e(TAG, "Invalid P2P connection data");
                broadcastConnectionFailed(senderPhoneNumber, "Invalid connection data");
                return;
            }

            if (!connectionData.isFresh()) {
                Log.e(TAG, "Stale P2P connection data");
                broadcastConnectionFailed(senderPhoneNumber, "Connection request expired");
                return;
            }

            // Check if we already have a connection with this phone number
            if (phoneNumberToConnectionId.containsKey(senderPhoneNumber)) {
                String existingConnectionId = phoneNumberToConnectionId.get(senderPhoneNumber);
                Log.w(TAG, "Connection already exists with " + senderPhoneNumber + ": " + existingConnectionId);
                // Close existing connection before creating new one
                closeConnection(senderPhoneNumber);
            }

            // Create new P2P connection
            P2PConnectionManager connectionManager = new P2PConnectionManager(context, this);
            activeConnections.put(connectionData.getConnectionId(), connectionManager);
            phoneNumberToConnectionId.put(senderPhoneNumber, connectionData.getConnectionId());

            // Initiate the connection
            connectionManager.initiateConnection(connectionData);
            
            Log.d(TAG, "P2P connection initiated for " + senderPhoneNumber);

        } catch (Exception e) {
            Log.e(TAG, "Error handling P2P trigger", e);
            broadcastConnectionFailed(senderPhoneNumber, "Connection setup failed: " + e.getMessage());
        }
    }

    /**
     * Creates a P2P connection offer and returns the encrypted SMS trigger.
     */
    public String createP2PConnectionOffer(String targetPhoneNumber) {
        Log.d(TAG, "Creating P2P connection offer for: " + targetPhoneNumber);
        
        try {
            // Check if connection already exists
            if (phoneNumberToConnectionId.containsKey(targetPhoneNumber)) {
                Log.w(TAG, "Connection already exists with " + targetPhoneNumber);
                closeConnection(targetPhoneNumber);
            }

            // Create connection manager
            P2PConnectionManager connectionManager = new P2PConnectionManager(context, this);
            P2PConnectionData connectionData = connectionManager.createConnectionOffer(targetPhoneNumber);
            
            if (connectionData == null || !connectionData.isValid()) {
                Log.e(TAG, "Failed to create valid connection data");
                return null;
            }

            // Store the connection
            activeConnections.put(connectionData.getConnectionId(), connectionManager);
            phoneNumberToConnectionId.put(targetPhoneNumber, connectionData.getConnectionId());

            // Encrypt the connection data
            String encryptedPayload = P2PEncryptionUtils.encryptConnectionData(context, connectionData.toJson());
            if (encryptedPayload == null) {
                Log.e(TAG, "Failed to encrypt connection data");
                closeConnection(targetPhoneNumber);
                return null;
            }

            // Create the SMS trigger message
            String smsTrigger = "P2P_CONNECT#USER:" + encryptedPayload;
            Log.d(TAG, "P2P connection offer created, SMS trigger length: " + smsTrigger.length());
            
            return smsTrigger;

        } catch (Exception e) {
            Log.e(TAG, "Error creating P2P connection offer", e);
            return null;
        }
    }

    /**
     * Sends a message over an established P2P connection.
     */
    public boolean sendP2PMessage(String phoneNumber, String message) {
        String connectionId = phoneNumberToConnectionId.get(phoneNumber);
        if (connectionId == null) {
            Log.w(TAG, "No P2P connection found for " + phoneNumber);
            return false;
        }

        P2PConnectionManager connectionManager = activeConnections.get(connectionId);
        if (connectionManager == null) {
            Log.w(TAG, "Connection manager not found for " + connectionId);
            phoneNumberToConnectionId.remove(phoneNumber);
            return false;
        }

        boolean sent = connectionManager.sendMessage(message);
        Log.d(TAG, "P2P message sent to " + phoneNumber + ": " + sent);
        return sent;
    }

    /**
     * Checks if there's an active P2P connection with the given phone number.
     */
    public boolean hasActiveConnection(String phoneNumber) {
        String connectionId = phoneNumberToConnectionId.get(phoneNumber);
        return connectionId != null && activeConnections.containsKey(connectionId);
    }

    /**
     * Closes the P2P connection with the specified phone number.
     */
    public void closeConnection(String phoneNumber) {
        String connectionId = phoneNumberToConnectionId.remove(phoneNumber);
        if (connectionId != null) {
            P2PConnectionManager connectionManager = activeConnections.remove(connectionId);
            if (connectionManager != null) {
                connectionManager.closeConnection();
                Log.d(TAG, "P2P connection closed for " + phoneNumber);
            }
        }
    }

    /**
     * Closes all active P2P connections.
     */
    public void closeAllConnections() {
        Log.d(TAG, "Closing all P2P connections");
        for (P2PConnectionManager connectionManager : activeConnections.values()) {
            connectionManager.closeConnection();
        }
        activeConnections.clear();
        phoneNumberToConnectionId.clear();
    }

    /**
     * Gets the phone number associated with a connection ID.
     */
    private String getPhoneNumberByConnectionId(String connectionId) {
        for (Map.Entry<String, String> entry : phoneNumberToConnectionId.entrySet()) {
            if (connectionId.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    // P2PConnectionManager.P2PConnectionListener implementation

    @Override
    public void onConnectionEstablished(String connectionId) {
        String phoneNumber = getPhoneNumberByConnectionId(connectionId);
        Log.d(TAG, "P2P connection established: " + connectionId + " for " + phoneNumber);
        
        Intent intent = new Intent(ACTION_P2P_CONNECTION_ESTABLISHED);
        intent.putExtra(EXTRA_CONNECTION_ID, connectionId);
        intent.putExtra(EXTRA_PHONE_NUMBER, phoneNumber);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public void onConnectionFailed(String error) {
        Log.e(TAG, "P2P connection failed: " + error);
        broadcastConnectionFailed("unknown", error);
    }

    @Override
    public void onMessageReceived(String message) {
        // Find the connection that received this message
        // For simplicity, we'll broadcast to all listeners
        // In a real implementation, you'd track which connection received the message
        Log.d(TAG, "P2P message received: " + message);
        
        Intent intent = new Intent(ACTION_P2P_MESSAGE_RECEIVED);
        intent.putExtra(EXTRA_MESSAGE, message);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public void onConnectionClosed() {
        Log.d(TAG, "P2P connection closed");
        
        Intent intent = new Intent(ACTION_P2P_CONNECTION_CLOSED);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Helper method to broadcast connection failure.
     */
    private void broadcastConnectionFailed(String phoneNumber, String error) {
        Intent intent = new Intent(ACTION_P2P_CONNECTION_FAILED);
        intent.putExtra(EXTRA_PHONE_NUMBER, phoneNumber);
        intent.putExtra(EXTRA_ERROR, error);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}