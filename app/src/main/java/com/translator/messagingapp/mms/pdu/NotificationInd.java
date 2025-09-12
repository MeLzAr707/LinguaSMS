package com.translator.messagingapp.mms.pdu;

/**
 * Represents an MMS Notification Indication PDU.
 */
public class NotificationInd extends GenericPdu {
    private byte[] mTransactionId;
    private byte[] mContentLocation;
    private EncodedStringValue mFrom;
    private EncodedStringValue mSubject;
    private long mExpiry;
    private long mMessageSize;

    /**
     * Creates a new Notification Indication PDU.
     */
    public NotificationInd() {
        // Default constructor
    }

    @Override
    public int getMessageType() {
        return PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;
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
     * Gets the content location.
     *
     * @return The content location
     */
    public byte[] getContentLocation() {
        return mContentLocation;
    }

    /**
     * Sets the content location.
     *
     * @param contentLocation The content location
     */
    public void setContentLocation(byte[] contentLocation) {
        mContentLocation = contentLocation;
    }

    /**
     * Gets the from address.
     *
     * @return The from address
     */
    public EncodedStringValue getFrom() {
        return mFrom;
    }

    /**
     * Sets the from address.
     *
     * @param from The from address
     */
    public void setFrom(EncodedStringValue from) {
        mFrom = from;
    }

    /**
     * Gets the subject.
     *
     * @return The subject
     */
    public EncodedStringValue getSubject() {
        return mSubject;
    }

    /**
     * Sets the subject.
     *
     * @param subject The subject
     */
    public void setSubject(EncodedStringValue subject) {
        mSubject = subject;
    }

    /**
     * Gets the expiry time.
     *
     * @return The expiry time
     */
    public long getExpiry() {
        return mExpiry;
    }

    /**
     * Sets the expiry time.
     *
     * @param expiry The expiry time
     */
    public void setExpiry(long expiry) {
        mExpiry = expiry;
    }

    /**
     * Gets the message size.
     *
     * @return The message size
     */
    public long getMessageSize() {
        return mMessageSize;
    }

    /**
     * Sets the message size.
     *
     * @param messageSize The message size
     */
    public void setMessageSize(long messageSize) {
        mMessageSize = messageSize;
    }
}