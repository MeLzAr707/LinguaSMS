# P2P Connection Implementation Documentation

## Overview

This document describes the implementation of encrypted SMS triggers for establishing peer-to-peer (P2P) connections between LinguaSMS app instances. This feature allows users to communicate directly without going through SMS/MMS infrastructure after the initial connection is established.

## SMS Trigger Format

The P2P connection trigger uses a specific SMS format:

```
P2P_CONNECT#USER:{encrypted_payload}
```

### Format Components

- **P2P_CONNECT#**: Fixed prefix that identifies this as a P2P connection trigger
- **USER:**: User identifier prefix (for potential future extension to support different connection types)
- **{encrypted_payload}**: Base64-encoded encrypted connection data

### Example Trigger SMS

```
P2P_CONNECT#USER:dGVzdGVuY3J5cHRlZGNvbm5lY3Rpb25kYXRhd2l0aGljZXNlcnZlcnM=
```

## Connection Data Structure

The encrypted payload contains a JSON object with the following structure:

```json
{
  "connectionId": "unique-connection-identifier",
  "senderPhoneNumber": "+1234567890",
  "signalServerUrl": "stun:stun.l.google.com:19302",
  "iceServers": "[{\"url\":\"stun:stun.l.google.com:19302\"}]",
  "timestamp": 1640995200000,
  "deviceId": "unique-device-identifier"
}
```

### Field Descriptions

- **connectionId**: UUID identifying this specific connection attempt
- **senderPhoneNumber**: Phone number of the device initiating the connection
- **signalServerUrl**: Primary STUN/TURN server for NAT traversal
- **iceServers**: JSON array of ICE servers for WebRTC connection establishment
- **timestamp**: Unix timestamp (milliseconds) when the connection offer was created
- **deviceId**: Unique identifier for the sender's device

## Security Features

### Encryption

- Uses **AES-256-GCM** encryption with Android Keystore
- Each app instance generates its own encryption key stored securely in Android Keystore
- Includes authentication tag to prevent tampering
- Random IV (Initialization Vector) for each encryption operation

### Payload Validation

- Connection data expires after 5 minutes to prevent replay attacks
- Validates JSON structure and required fields
- Verifies Base64 encoding integrity
- Checks trigger format compliance

### Key Management

- Encryption keys are generated and stored in Android Keystore
- Keys cannot be extracted from the device
- Each device has its own unique encryption key
- Key alias: `P2P_CONNECTION_KEY`

## Connection Establishment Flow

### Initiating Connection (Sender)

1. User initiates P2P connection to a contact
2. App creates `P2PConnectionData` with connection parameters
3. Connection data is encrypted using `P2PEncryptionUtils`
4. Encrypted payload is embedded in P2P trigger SMS format
5. Trigger SMS is sent to target phone number
6. WebRTC peer connection is prepared for incoming response

### Receiving Connection (Recipient)

1. SMS is received and processed by `SmsReceiver`
2. `MessageService.handleIncomingSms()` detects P2P trigger pattern
3. Encrypted payload is extracted and decrypted
4. Connection data is validated (format, freshness, completeness)
5. `P2PService.handleP2PTrigger()` initiates WebRTC connection
6. If successful, P2P channel is established for messaging

### Connection States

- **INITIATING**: Connection offer created and sent
- **CONNECTING**: WebRTC signaling in progress
- **CONNECTED**: P2P data channel established and ready
- **FAILED**: Connection attempt failed
- **CLOSED**: Connection terminated

## WebRTC Integration

### Peer Connection Configuration

- Uses public STUN servers for NAT traversal
- Configures data channels for messaging (no audio/video)
- Ordered data channel delivery for message integrity
- Automatic ICE candidate gathering

### Signaling

Current implementation uses a simplified signaling approach:
- Connection parameters are sent via encrypted SMS trigger
- Full WebRTC signaling could be extended with a signaling server
- For MVP, basic connection establishment is simulated

### Data Channel

- Channel name: `"messages"`
- Ordered delivery: `true`
- Protocol: Text message exchange
- Binary support: Available for future file transfer

## Message Routing

### SMS vs P2P Decision

When sending a message, the app checks:
1. Is there an active P2P connection with the recipient?
2. If yes, send via P2P data channel
3. If no, send via traditional SMS/MMS

### P2P Message Format

Messages sent over P2P connections:
- Plain text content (already established secure channel)
- No additional encryption (WebRTC provides transport security)
- Future: Support for rich media, file attachments

## Error Handling

### Connection Failures

- **Encryption failures**: Log error, don't send trigger SMS
- **Network failures**: Fallback to SMS messaging
- **Timeout**: Connection attempts expire after 30 seconds
- **Invalid triggers**: Silently ignore malformed trigger SMS

### Edge Cases

- **Duplicate connections**: Close existing connection before creating new one
- **Stale triggers**: Reject connection data older than 5 minutes
- **Missing permissions**: Graceful degradation to SMS-only mode
- **App not installed**: Trigger SMS appears as regular message to recipient

## API Usage

### Sending P2P Connection Offer

```java
MessageService messageService = app.getMessageService();
boolean success = messageService.sendP2PConnectionOffer("+1234567890");
```

### Sending Message Over P2P

```java
boolean sent = messageService.sendP2PMessage("+1234567890", "Hello via P2P!");
```

### Checking Connection Status

```java
boolean hasConnection = messageService.hasActiveP2PConnection("+1234567890");
```

### Closing Connection

```java
messageService.closeP2PConnection("+1234567890");
```

## Broadcasting Events

The P2P system broadcasts the following events via `LocalBroadcastManager`:

- **P2P_CONNECTION_ESTABLISHED**: Connection successfully established
- **P2P_CONNECTION_FAILED**: Connection attempt failed
- **P2P_MESSAGE_RECEIVED**: Message received over P2P channel
- **P2P_CONNECTION_CLOSED**: P2P connection was closed

## Performance Considerations

### Battery Usage

- WebRTC connections are closed when not in active use
- No persistent connections maintained
- Efficient wake lock usage during connection establishment

### Network Usage

- Minimal data usage for connection establishment
- Direct peer-to-peer communication after connection
- No server-side infrastructure required (except STUN servers)

### SMS Limitations

- P2P trigger SMS limited by standard SMS length (160 characters)
- Base64 encoding reduces effective payload size
- Large connection data may require SMS concatenation

## Future Enhancements

### Planned Features

1. **File Transfer**: Support for sending files over P2P connections
2. **Voice/Video**: Extend WebRTC to support audio/video calls
3. **Group P2P**: Multi-party P2P connections
4. **Enhanced Signaling**: Dedicated signaling server for improved reliability

### Security Improvements

1. **Perfect Forward Secrecy**: Rotate connection keys periodically
2. **Certificate Pinning**: Validate STUN/TURN server certificates
3. **Rate Limiting**: Prevent P2P connection spam
4. **User Consent**: Require explicit approval for P2P connections

## Testing

### Unit Tests

- `P2PEncryptionUtilsTest`: Validates trigger detection and encryption
- `P2PConnectionDataTest`: Tests data model and JSON serialization

### Integration Testing

- End-to-end P2P connection establishment
- Message delivery over P2P channels
- Fallback to SMS when P2P fails

### Manual Testing

1. Install app on two devices
2. Send P2P connection offer from device A to device B
3. Verify connection establishment
4. Send messages over P2P channel
5. Verify graceful fallback to SMS

## Troubleshooting

### Common Issues

- **Connection fails**: Check network connectivity and NAT configuration
- **Encryption errors**: Verify Android Keystore availability
- **Invalid triggers**: Check SMS format and Base64 encoding
- **Timeout issues**: Verify STUN server accessibility

### Debug Logging

Enable verbose logging with tag filter: `P2P*` to see detailed connection flow.

### Network Debugging

Use `adb logcat | grep WebRTC` to see WebRTC-specific logs during connection establishment.