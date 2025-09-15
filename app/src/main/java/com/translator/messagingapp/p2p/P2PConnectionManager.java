package com.translator.messagingapp.p2p;

import android.content.Context;
import android.util.Log;
import org.webrtc.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages WebRTC peer-to-peer connections for direct app-to-app communication.
 */
public class P2PConnectionManager {
    private static final String TAG = "P2PConnectionManager";
    
    private Context context;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private DataChannel dataChannel;
    private P2PConnectionListener listener;
    private boolean isInitiator;
    private String connectionId;

    public interface P2PConnectionListener {
        void onConnectionEstablished(String connectionId);
        void onConnectionFailed(String error);
        void onMessageReceived(String message);
        void onConnectionClosed();
    }

    public P2PConnectionManager(Context context, P2PConnectionListener listener) {
        this.context = context;
        this.listener = listener;
        this.connectionId = UUID.randomUUID().toString();
        initializeWebRTC();
    }

    /**
     * Initializes WebRTC components.
     */
    private void initializeWebRTC() {
        try {
            // Initialize PeerConnectionFactory
            PeerConnectionFactory.InitializationOptions initOptions =
                    PeerConnectionFactory.InitializationOptions.builder(context)
                            .setEnableInternalTracer(false)
                            .createInitializationOptions();
            PeerConnectionFactory.initialize(initOptions);

            PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
            peerConnectionFactory = PeerConnectionFactory.builder()
                    .setOptions(options)
                    .createPeerConnectionFactory();

            Log.d(TAG, "WebRTC initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize WebRTC", e);
            if (listener != null) {
                listener.onConnectionFailed("WebRTC initialization failed: " + e.getMessage());
            }
        }
    }

    /**
     * Initiates a P2P connection using the provided connection data.
     */
    public void initiateConnection(P2PConnectionData connectionData) {
        if (!connectionData.isValid() || !connectionData.isFresh()) {
            Log.e(TAG, "Invalid or stale connection data");
            if (listener != null) {
                listener.onConnectionFailed("Invalid connection data");
            }
            return;
        }

        this.connectionId = connectionData.getConnectionId();
        this.isInitiator = false; // Recipient of the connection request
        createPeerConnection(connectionData);
    }

    /**
     * Creates a P2P connection offer to send to another device.
     */
    public P2PConnectionData createConnectionOffer(String targetPhoneNumber) {
        this.isInitiator = true;
        
        // Create connection data with basic WebRTC configuration
        P2PConnectionData connectionData = new P2PConnectionData();
        connectionData.setConnectionId(this.connectionId);
        connectionData.setSenderPhoneNumber(getCurrentPhoneNumber());
        connectionData.setSignalServerUrl("stun:stun.l.google.com:19302"); // Public STUN server
        connectionData.setIceServers(getDefaultIceServers());
        connectionData.setDeviceId(getDeviceId());
        
        // Start creating the peer connection
        createPeerConnection(connectionData);
        
        return connectionData;
    }

    /**
     * Creates the WebRTC peer connection.
     */
    private void createPeerConnection(P2PConnectionData connectionData) {
        try {
            List<PeerConnection.IceServer> iceServers = parseIceServers(connectionData.getIceServers());
            
            PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
            rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
            rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;

            peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnectionObserver());
            
            if (peerConnection == null) {
                throw new RuntimeException("Failed to create PeerConnection");
            }

            // Create data channel for messaging
            DataChannel.Init init = new DataChannel.Init();
            init.ordered = true;
            dataChannel = peerConnection.createDataChannel("messages", init);
            dataChannel.registerObserver(new DataChannelObserver());

            Log.d(TAG, "PeerConnection created successfully");

            if (isInitiator) {
                createOffer();
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to create peer connection", e);
            if (listener != null) {
                listener.onConnectionFailed("Failed to create connection: " + e.getMessage());
            }
        }
    }

    /**
     * Creates an offer for connection initiation.
     */
    private void createOffer() {
        if (peerConnection != null) {
            MediaConstraints constraints = new MediaConstraints();
            peerConnection.createOffer(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    Log.d(TAG, "Offer created successfully");
                    peerConnection.setLocalDescription(new SdpObserver() {
                        @Override
                        public void onSetSuccess() {
                            Log.d(TAG, "Local description set successfully");
                            // In a real implementation, this offer would be sent via signaling server
                            // For now, we simulate successful connection
                            simulateConnectionEstablished();
                        }

                        @Override
                        public void onSetFailure(String s) {
                            Log.e(TAG, "Failed to set local description: " + s);
                            if (listener != null) {
                                listener.onConnectionFailed("Failed to set local description");
                            }
                        }

                        @Override
                        public void onCreateSuccess(SessionDescription sessionDescription) {}

                        @Override
                        public void onCreateFailure(String s) {}
                    }, sessionDescription);
                }

                @Override
                public void onCreateFailure(String s) {
                    Log.e(TAG, "Failed to create offer: " + s);
                    if (listener != null) {
                        listener.onConnectionFailed("Failed to create offer");
                    }
                }

                @Override
                public void onSetSuccess() {}

                @Override
                public void onSetFailure(String s) {}
            }, constraints);
        }
    }

    /**
     * Sends a message over the established P2P connection.
     */
    public boolean sendMessage(String message) {
        if (dataChannel != null && dataChannel.state() == DataChannel.State.OPEN) {
            try {
                DataChannel.Buffer buffer = new DataChannel.Buffer(
                        java.nio.ByteBuffer.wrap(message.getBytes()), false);
                boolean result = dataChannel.send(buffer);
                Log.d(TAG, "Message sent: " + result);
                return result;
            } catch (Exception e) {
                Log.e(TAG, "Failed to send message", e);
                return false;
            }
        } else {
            Log.w(TAG, "DataChannel not open, cannot send message");
            return false;
        }
    }

    /**
     * Closes the P2P connection.
     */
    public void closeConnection() {
        try {
            if (dataChannel != null) {
                dataChannel.close();
                dataChannel = null;
            }
            
            if (peerConnection != null) {
                peerConnection.close();
                peerConnection = null;
            }
            
            Log.d(TAG, "P2P connection closed");
            if (listener != null) {
                listener.onConnectionClosed();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing connection", e);
        }
    }

    /**
     * Helper method to parse ICE servers from JSON string.
     */
    private List<PeerConnection.IceServer> parseIceServers(String iceServersJson) {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        
        // Add default STUN server
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        
        if (iceServersJson != null && !iceServersJson.trim().isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray(iceServersJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject serverObj = jsonArray.getJSONObject(i);
                    String url = serverObj.getString("url");
                    iceServers.add(PeerConnection.IceServer.builder(url).createIceServer());
                }
            } catch (JSONException e) {
                Log.w(TAG, "Failed to parse ICE servers, using defaults", e);
            }
        }
        
        return iceServers;
    }

    /**
     * Gets default ICE servers configuration as JSON string.
     */
    private String getDefaultIceServers() {
        try {
            JSONArray iceServers = new JSONArray();
            JSONObject stunServer = new JSONObject();
            stunServer.put("url", "stun:stun.l.google.com:19302");
            iceServers.put(stunServer);
            return iceServers.toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create default ICE servers", e);
            return "[]";
        }
    }

    /**
     * Gets the current device's phone number.
     * In a real implementation, this would retrieve the actual phone number.
     */
    private String getCurrentPhoneNumber() {
        // Placeholder - in real implementation, get from TelephonyManager or user settings
        return "unknown";
    }

    /**
     * Gets a unique device identifier.
     */
    private String getDeviceId() {
        return android.provider.Settings.Secure.getString(context.getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);
    }

    /**
     * Simulates successful connection establishment for demonstration.
     * In a real implementation, this would happen after signaling exchange.
     */
    private void simulateConnectionEstablished() {
        // Simulate successful connection after a brief delay
        new android.os.Handler().postDelayed(() -> {
            if (listener != null) {
                listener.onConnectionEstablished(connectionId);
            }
        }, 1000);
    }

    /**
     * PeerConnection observer to handle connection events.
     */
    private class PeerConnectionObserver implements PeerConnection.Observer {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.d(TAG, "Signaling state changed: " + signalingState);
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.d(TAG, "ICE connection state changed: " + iceConnectionState);
            if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
                if (listener != null) {
                    listener.onConnectionEstablished(connectionId);
                }
            } else if (iceConnectionState == PeerConnection.IceConnectionState.FAILED ||
                       iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
                if (listener != null) {
                    listener.onConnectionFailed("ICE connection failed or disconnected");
                }
            }
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {}

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            Log.d(TAG, "ICE gathering state changed: " + iceGatheringState);
        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            Log.d(TAG, "ICE candidate generated");
            // In a real implementation, this would be sent to the remote peer via signaling
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {}

        @Override
        public void onAddStream(MediaStream mediaStream) {}

        @Override
        public void onRemoveStream(MediaStream mediaStream) {}

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            Log.d(TAG, "Data channel received");
            dataChannel.registerObserver(new DataChannelObserver());
        }

        @Override
        public void onRenegotiationNeeded() {}

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {}
    }

    /**
     * DataChannel observer to handle message events.
     */
    private class DataChannelObserver implements DataChannel.Observer {
        @Override
        public void onBufferedAmountChange(long l) {}

        @Override
        public void onStateChange() {
            Log.d(TAG, "DataChannel state changed: " + (dataChannel != null ? dataChannel.state() : "null"));
        }

        @Override
        public void onMessage(DataChannel.Buffer buffer) {
            if (buffer.data != null) {
                byte[] bytes = new byte[buffer.data.remaining()];
                buffer.data.get(bytes);
                String message = new String(bytes);
                Log.d(TAG, "Message received: " + message);
                if (listener != null) {
                    listener.onMessageReceived(message);
                }
            }
        }
    }
}