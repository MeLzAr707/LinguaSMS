package com.translator.messagingapp.mms.pdu;

import com.translator.messagingapp.message.*;

/**
 * Defines constants for MMS PDU headers.
 */
public class PduHeaders {
    
    // Message types
    public static final int MESSAGE_TYPE_SEND_REQ = 0x80;
    public static final int MESSAGE_TYPE_SEND_CONF = 0x81;
    public static final int MESSAGE_TYPE_NOTIFICATION_IND = 0x82;
    public static final int MESSAGE_TYPE_NOTIFYRESP_IND = 0x83;
    public static final int MESSAGE_TYPE_RETRIEVE_CONF = 0x84;
    public static final int MESSAGE_TYPE_ACKNOWLEDGE_IND = 0x85;
    public static final int MESSAGE_TYPE_DELIVERY_IND = 0x86;

    // Header field names
    public static final int BCC = 0x81;
    public static final int CC = 0x82;
    public static final int CONTENT_LOCATION = 0x83;
    public static final int CONTENT_TYPE = 0x84;
    public static final int DATE = 0x85;
    public static final int DELIVERY_REPORT = 0x86;
    public static final int DELIVERY_TIME = 0x87;
    public static final int EXPIRY = 0x88;
    public static final int FROM = 0x89;
    public static final int MESSAGE_CLASS = 0x8A;
    public static final int MESSAGE_ID = 0x8B;
    public static final int MESSAGE_TYPE = 0x8C;
    public static final int MMS_VERSION = 0x8D;
    public static final int MESSAGE_SIZE = 0x8E;
    public static final int PRIORITY = 0x8F;
    public static final int READ_REPORT = 0x90;
    public static final int RESPONSE_STATUS = 0x92;
    public static final int RESPONSE_TEXT = 0x93;
    public static final int SENDER_VISIBILITY = 0x94;
    public static final int STATUS = 0x95;
    public static final int SUBJECT = 0x96;
    public static final int TO = 0x97;
    public static final int TRANSACTION_ID = 0x98;

    // Response status values
    public static final int RESPONSE_STATUS_OK = 0x80;
    public static final int RESPONSE_STATUS_ERROR_UNSPECIFIED = 0x81;
    public static final int RESPONSE_STATUS_ERROR_SERVICE_DENIED = 0x82;
    public static final int RESPONSE_STATUS_ERROR_MESSAGE_FORMAT_CORRUPT = 0x83;
    public static final int RESPONSE_STATUS_ERROR_SENDING_ADDRESS_UNRESOLVED = 0x84;
    public static final int RESPONSE_STATUS_ERROR_MESSAGE_NOT_FOUND = 0x85;
    public static final int RESPONSE_STATUS_ERROR_NETWORK_PROBLEM = 0x86;
    public static final int RESPONSE_STATUS_ERROR_CONTENT_NOT_ACCEPTED = 0x87;
    public static final int RESPONSE_STATUS_ERROR_UNSUPPORTED_MESSAGE = 0x88;

    // Priority values
    public static final int PRIORITY_LOW = 0x80;
    public static final int PRIORITY_NORMAL = 0x81;
    public static final int PRIORITY_HIGH = 0x82;

    // Delivery report values
    public static final int VALUE_YES = 0x80;
    public static final int VALUE_NO = 0x81;

    // Message class values
    public static final int MESSAGE_CLASS_PERSONAL = 0x80;
    public static final int MESSAGE_CLASS_ADVERTISEMENT = 0x81;
    public static final int MESSAGE_CLASS_INFORMATIONAL = 0x82;
    public static final int MESSAGE_CLASS_AUTO = 0x83;

    // Content class values
    public static final int CONTENT_CLASS_TEXT = 0x00;
    public static final int CONTENT_CLASS_IMAGE = 0x01;
    public static final int CONTENT_CLASS_AUDIO = 0x02;
    public static final int CONTENT_CLASS_VIDEO = 0x03;

    // Sender visibility values
    public static final int SENDER_VISIBILITY_HIDE = 0x80;
    public static final int SENDER_VISIBILITY_SHOW = 0x81;

    // MMS version
    public static final int CURRENT_MMS_VERSION = 0x12; // Version 1.2

    /**
     * Gets a human-readable name for a header field.
     *
     * @param header The header field constant
     * @return The header name
     */
    public static String getHeaderName(int header) {
        switch (header) {
            case BCC: return "BCC";
            case CC: return "CC";
            case CONTENT_LOCATION: return "Content-Location";
            case CONTENT_TYPE: return "Content-Type";
            case DATE: return "Date";
            case DELIVERY_REPORT: return "Delivery-Report";
            case DELIVERY_TIME: return "Delivery-Time";
            case EXPIRY: return "Expiry";
            case FROM: return "From";
            case MESSAGE_CLASS: return "Message-Class";
            case MESSAGE_ID: return "Message-ID";
            case MESSAGE_TYPE: return "Message-Type";
            case MMS_VERSION: return "MMS-Version";
            case MESSAGE_SIZE: return "Message-Size";
            case PRIORITY: return "Priority";
            case READ_REPORT: return "Read-Report";
            case RESPONSE_STATUS: return "Response-Status";
            case RESPONSE_TEXT: return "Response-Text";
            case SENDER_VISIBILITY: return "Sender-Visibility";
            case STATUS: return "Status";
            case SUBJECT: return "Subject";
            case TO: return "To";
            case TRANSACTION_ID: return "Transaction-ID";
            default: return "Unknown-" + header;
        }
    }

    /**
     * Gets a human-readable name for a message type.
     *
     * @param messageType The message type constant
     * @return The message type name
     */
    public static String getMessageTypeName(int messageType) {
        switch (messageType) {
            case MESSAGE_TYPE_SEND_REQ: return "Send-Request";
            case MESSAGE_TYPE_SEND_CONF: return "Send-Confirmation";
            case MESSAGE_TYPE_NOTIFICATION_IND: return "Notification-Indication";
            case MESSAGE_TYPE_NOTIFYRESP_IND: return "Notify-Response-Indication";
            case MESSAGE_TYPE_RETRIEVE_CONF: return "Retrieve-Confirmation";
            case MESSAGE_TYPE_ACKNOWLEDGE_IND: return "Acknowledge-Indication";
            case MESSAGE_TYPE_DELIVERY_IND: return "Delivery-Indication";
            default: return "Unknown-" + messageType;
        }
    }

    /**
     * Gets a human-readable name for a response status.
     *
     * @param status The response status constant
     * @return The status name
     */
    public static String getResponseStatusName(int status) {
        switch (status) {
            case RESPONSE_STATUS_OK: return "OK";
            case RESPONSE_STATUS_ERROR_UNSPECIFIED: return "Error-Unspecified";
            case RESPONSE_STATUS_ERROR_SERVICE_DENIED: return "Error-Service-Denied";
            case RESPONSE_STATUS_ERROR_MESSAGE_FORMAT_CORRUPT: return "Error-Message-Format-Corrupt";
            case RESPONSE_STATUS_ERROR_SENDING_ADDRESS_UNRESOLVED: return "Error-Sending-Address-Unresolved";
            case RESPONSE_STATUS_ERROR_MESSAGE_NOT_FOUND: return "Error-Message-Not-Found";
            case RESPONSE_STATUS_ERROR_NETWORK_PROBLEM: return "Error-Network-Problem";
            case RESPONSE_STATUS_ERROR_CONTENT_NOT_ACCEPTED: return "Error-Content-Not-Accepted";
            case RESPONSE_STATUS_ERROR_UNSUPPORTED_MESSAGE: return "Error-Unsupported-Message";
            default: return "Unknown-Status-" + status;
        }
    }
}