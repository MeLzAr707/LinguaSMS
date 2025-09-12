package com.translator.messagingapp.mms.pdu;

/**
 * Represents an MMS Send Confirmation PDU.
 */
public class SendConf extends GenericPdu {
    private byte[] mTransactionId;
    private byte[] mMessageId;
    private int mResponseStatus;
    private EncodedStringValue mResponseText;

    /**
     * Creates a new Send Confirmation PDU.
     */
    public SendConf() {
        // Default constructor
    }

    @Override
    public int getMessageType() {
        return PduHeaders.MESSAGE_TYPE_SEND_CONF;
    }

    /**
     * Gets the transaction ID.
     *
     * @return The transaction ID
     */
    public byte[] getTransactionId() {
        return mTransactionId;
    }

    /**
     * Sets the transaction ID.
     *
     * @param transactionId The transaction ID
     */
    public void setTransactionId(byte[] transactionId) {
        mTransactionId = transactionId;
    }

    /**
     * Gets the message ID.
     *
     * @return The message ID
     */
    public byte[] getMessageId() {
        return mMessageId;
    }

    /**
     * Sets the message ID.
     *
     * @param messageId The message ID
     */
    public void setMessageId(byte[] messageId) {
        mMessageId = messageId;
    }

    /**
     * Gets the response status.
     *
     * @return The response status
     */
    public int getResponseStatus() {
        return mResponseStatus;
    }

    /**
     * Sets the response status.
     *
     * @param responseStatus The response status
     */
    public void setResponseStatus(int responseStatus) {
        mResponseStatus = responseStatus;
    }

    /**
     * Gets the response text.
     *
     * @return The response text
     */
    public EncodedStringValue getResponseText() {
        return mResponseText;
    }

    /**
     * Sets the response text.
     *
     * @param responseText The response text
     */
    public void setResponseText(EncodedStringValue responseText) {
        mResponseText = responseText;
    }

    /**
     * Checks if the send was successful.
     *
     * @return True if successful
     */
    public boolean isSuccessful() {
        return mResponseStatus == PduHeaders.RESPONSE_STATUS_OK;
    }
}