package com.translator.messagingapp.mms.pdu;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Composer for creating MMS PDU binary data from structured objects.
 */
public class PduComposer {
    private static final String TAG = "PduComposer";
    
    private final Context mContext;
    private final GenericPdu mPdu;

    /**
     * Creates a new PDU composer.
     *
     * @param context The application context
     * @param pdu The PDU to compose
     */
    public PduComposer(Context context, GenericPdu pdu) {
        mContext = context;
        mPdu = pdu;
    }

    /**
     * Creates the binary PDU data.
     *
     * @return The binary PDU data, or null if failed
     */
    public byte[] make() {
        if (mPdu == null) {
            Log.e(TAG, "No PDU to compose");
            return null;
        }
        
        try {
            Log.d(TAG, "Composing PDU: " + mPdu.getClass().getSimpleName());
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // Compose headers
            byte[] headers = composeHeaders();
            if (headers == null) {
                Log.e(TAG, "Failed to compose headers");
                return null;
            }
            baos.write(headers);
            
            // Compose body if applicable
            if (mPdu instanceof SendReq) {
                SendReq sendReq = (SendReq) mPdu;
                if (sendReq.getBody() != null) {
                    byte[] body = composeBody(sendReq.getBody());
                    if (body != null) {
                        baos.write(body);
                    }
                }
            } else if (mPdu instanceof RetrieveConf) {
                RetrieveConf retrieveConf = (RetrieveConf) mPdu;
                if (retrieveConf.getBody() != null) {
                    byte[] body = composeBody(retrieveConf.getBody());
                    if (body != null) {
                        baos.write(body);
                    }
                }
            }
            
            byte[] result = baos.toByteArray();
            Log.d(TAG, "PDU composed successfully (" + result.length + " bytes)");
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Error composing PDU", e);
            return null;
        }
    }

    /**
     * Composes the PDU headers.
     *
     * @return The header data, or null if failed
     */
    private byte[] composeHeaders() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // Write message type
            writeOctet(baos, PduHeaders.MESSAGE_TYPE);
            writeOctet(baos, mPdu.getMessageType());
            
            // Write MMS version
            writeOctet(baos, PduHeaders.MMS_VERSION);
            writeOctet(baos, PduHeaders.CURRENT_MMS_VERSION);
            
            // Write PDU-specific headers
            if (mPdu instanceof SendReq) {
                composeSendReqHeaders(baos, (SendReq) mPdu);
            } else if (mPdu instanceof SendConf) {
                composeSendConfHeaders(baos, (SendConf) mPdu);
            } else if (mPdu instanceof NotificationInd) {
                composeNotificationIndHeaders(baos, (NotificationInd) mPdu);
            } else if (mPdu instanceof RetrieveConf) {
                composeRetrieveConfHeaders(baos, (RetrieveConf) mPdu);
            }
            
            return baos.toByteArray();
            
        } catch (Exception e) {
            Log.e(TAG, "Error composing headers", e);
            return null;
        }
    }

    /**
     * Composes headers for a SendReq PDU.
     */
    private void composeSendReqHeaders(ByteArrayOutputStream baos, SendReq sendReq) throws IOException {
        // Transaction ID
        if (sendReq.getTransactionId() != null) {
            writeOctet(baos, PduHeaders.TRANSACTION_ID);
            writeTextString(baos, sendReq.getTransactionId());
        }
        
        // From address
        if (sendReq.getFrom() != null) {
            writeOctet(baos, PduHeaders.FROM);
            writeEncodedStringValue(baos, sendReq.getFrom());
        }
        
        // Date
        writeOctet(baos, PduHeaders.DATE);
        writeLongInteger(baos, sendReq.getDate());
        
        // Priority
        writeOctet(baos, PduHeaders.PRIORITY);
        writeOctet(baos, sendReq.getPriority());
        
        // Delivery report
        writeOctet(baos, PduHeaders.DELIVERY_REPORT);
        writeOctet(baos, sendReq.getDeliveryReport());
        
        // Read report
        writeOctet(baos, PduHeaders.READ_REPORT);
        writeOctet(baos, sendReq.getReadReport());
        
        // Expiry
        if (sendReq.getExpiry() > 0) {
            writeOctet(baos, PduHeaders.EXPIRY);
            writeOctet(baos, sendReq.getExpiry());
        }
        
        // Subject
        if (sendReq.getSubject() != null) {
            writeOctet(baos, PduHeaders.SUBJECT);
            writeEncodedStringValue(baos, sendReq.getSubject());
        }
    }

    /**
     * Composes headers for a SendConf PDU.
     */
    private void composeSendConfHeaders(ByteArrayOutputStream baos, SendConf sendConf) throws IOException {
        // Transaction ID
        if (sendConf.getTransactionId() != null) {
            writeOctet(baos, PduHeaders.TRANSACTION_ID);
            writeTextString(baos, sendConf.getTransactionId());
        }
        
        // Message ID
        if (sendConf.getMessageId() != null) {
            writeOctet(baos, PduHeaders.MESSAGE_ID);
            writeTextString(baos, sendConf.getMessageId());
        }
        
        // Response status
        writeOctet(baos, PduHeaders.RESPONSE_STATUS);
        writeOctet(baos, sendConf.getResponseStatus());
    }

    /**
     * Composes headers for a NotificationInd PDU.
     */
    private void composeNotificationIndHeaders(ByteArrayOutputStream baos, NotificationInd notificationInd) throws IOException {
        // Transaction ID
        if (notificationInd.getTransactionId() != null) {
            writeOctet(baos, PduHeaders.TRANSACTION_ID);
            writeTextString(baos, notificationInd.getTransactionId());
        }
        
        // Content location
        if (notificationInd.getContentLocation() != null) {
            writeOctet(baos, PduHeaders.CONTENT_LOCATION);
            writeTextString(baos, notificationInd.getContentLocation());
        }
        
        // Message size
        if (notificationInd.getMessageSize() > 0) {
            writeOctet(baos, PduHeaders.MESSAGE_SIZE);
            writeLongInteger(baos, notificationInd.getMessageSize());
        }
        
        // Expiry
        if (notificationInd.getExpiry() > 0) {
            writeOctet(baos, PduHeaders.EXPIRY);
            writeLongInteger(baos, notificationInd.getExpiry());
        }
    }

    /**
     * Composes headers for a RetrieveConf PDU.
     */
    private void composeRetrieveConfHeaders(ByteArrayOutputStream baos, RetrieveConf retrieveConf) throws IOException {
        // Transaction ID
        if (retrieveConf.getTransactionId() != null) {
            writeOctet(baos, PduHeaders.TRANSACTION_ID);
            writeTextString(baos, retrieveConf.getTransactionId());
        }
        
        // Message ID
        if (retrieveConf.getMessageId() != null) {
            writeOctet(baos, PduHeaders.MESSAGE_ID);
            writeTextString(baos, retrieveConf.getMessageId());
        }
        
        // Date
        writeOctet(baos, PduHeaders.DATE);
        writeLongInteger(baos, retrieveConf.getDate());
        
        // From address
        if (retrieveConf.getFrom() != null) {
            writeOctet(baos, PduHeaders.FROM);
            writeEncodedStringValue(baos, retrieveConf.getFrom());
        }
        
        // Subject
        if (retrieveConf.getSubject() != null) {
            writeOctet(baos, PduHeaders.SUBJECT);
            writeEncodedStringValue(baos, retrieveConf.getSubject());
        }
        
        // Priority
        writeOctet(baos, PduHeaders.PRIORITY);
        writeOctet(baos, retrieveConf.getPriority());
        
        // Content type
        if (retrieveConf.getContentType() != null) {
            writeOctet(baos, PduHeaders.CONTENT_TYPE);
            writeTextString(baos, retrieveConf.getContentType());
        }
    }

    /**
     * Composes the PDU body.
     */
    private byte[] composeBody(PduBody body) {
        try {
            if (body.isEmpty()) {
                return new byte[0];
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // Write number of parts
            writeUintvar(baos, body.getPartsNum());
            
            // Write each part
            for (int i = 0; i < body.getPartsNum(); i++) {
                PduPart part = body.getPart(i);
                if (part != null) {
                    byte[] partData = composePart(part);
                    if (partData != null) {
                        baos.write(partData);
                    }
                }
            }
            
            return baos.toByteArray();
            
        } catch (Exception e) {
            Log.e(TAG, "Error composing body", e);
            return null;
        }
    }

    /**
     * Composes a single part.
     */
    private byte[] composePart(PduPart part) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // Write content type
            if (part.getContentType() != null) {
                writeTextString(baos, part.getContentType());
            }
            
            // Write content data
            if (part.getData() != null) {
                baos.write(part.getData());
            }
            
            return baos.toByteArray();
            
        } catch (Exception e) {
            Log.e(TAG, "Error composing part", e);
            return null;
        }
    }

    /**
     * Writes a single octet.
     */
    private void writeOctet(ByteArrayOutputStream baos, int value) throws IOException {
        baos.write(value & 0xFF);
    }

    /**
     * Writes a text string.
     */
    private void writeTextString(ByteArrayOutputStream baos, byte[] text) throws IOException {
        if (text != null) {
            baos.write(text);
            baos.write(0); // Null terminator
        }
    }

    /**
     * Writes an encoded string value.
     */
    private void writeEncodedStringValue(ByteArrayOutputStream baos, EncodedStringValue value) throws IOException {
        if (value != null) {
            // Write charset
            writeUintvar(baos, value.getCharacterSet());
            // Write text
            writeTextString(baos, value.getTextString());
        }
    }

    /**
     * Writes a long integer.
     */
    private void writeLongInteger(ByteArrayOutputStream baos, long value) throws IOException {
        // For simplicity, write as 4 bytes
        baos.write((int) (value >> 24) & 0xFF);
        baos.write((int) (value >> 16) & 0xFF);
        baos.write((int) (value >> 8) & 0xFF);
        baos.write((int) value & 0xFF);
    }

    /**
     * Writes a variable-length unsigned integer.
     */
    private void writeUintvar(ByteArrayOutputStream baos, long value) throws IOException {
        // Simple implementation: write as single byte if < 128, otherwise use multiple bytes
        if (value < 128) {
            baos.write((int) value);
        } else {
            // Multi-byte encoding (simplified)
            baos.write(0x80 | ((int) (value >> 8) & 0x7F));
            baos.write((int) value & 0xFF);
        }
    }
}