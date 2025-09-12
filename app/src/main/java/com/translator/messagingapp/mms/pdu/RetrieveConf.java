package com.translator.messagingapp.mms.pdu;

/**
 * Represents an MMS Retrieve Confirmation PDU.
 * Used when downloading MMS content from the MMSC.
 */
public class RetrieveConf extends GenericPdu {
    private byte[] mTransactionId;
    private byte[] mMessageId;
    private byte[] mContentType;
    private long mDate;
    private EncodedStringValue mFrom;
    private EncodedStringValue mSubject;
    private int mPriority = PduHeaders.PRIORITY_NORMAL;
    private int mDeliveryReport = PduHeaders.VALUE_NO;
    private int mReadReport = PduHeaders.VALUE_NO;
    private PduBody mBody;

    /**
     * Creates a new Retrieve Confirmation PDU.
     */
    public RetrieveConf() {
        mDate = System.currentTimeMillis() / 1000L;
    }

    @Override
    public int getMessageType() {
        return PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF;
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
     * Gets the content type.
     *
     * @return The content type
     */
    public byte[] getContentType() {
        return mContentType;
    }

    /**
     * Sets the content type.
     *
     * @param contentType The content type
     */
    public void setContentType(byte[] contentType) {
        mContentType = contentType;
    }

    /**
     * Gets the date.
     *
     * @return The date in seconds since epoch
     */
    public long getDate() {
        return mDate;
    }

    /**
     * Sets the date.
     *
     * @param date The date in seconds since epoch
     */
    public void setDate(long date) {
        mDate = date;
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
     * Gets the priority.
     *
     * @return The priority
     */
    public int getPriority() {
        return mPriority;
    }

    /**
     * Sets the priority.
     *
     * @param priority The priority
     */
    public void setPriority(int priority) {
        mPriority = priority;
    }

    /**
     * Gets the delivery report setting.
     *
     * @return The delivery report setting
     */
    public int getDeliveryReport() {
        return mDeliveryReport;
    }

    /**
     * Sets the delivery report setting.
     *
     * @param deliveryReport The delivery report setting
     */
    public void setDeliveryReport(int deliveryReport) {
        mDeliveryReport = deliveryReport;
    }

    /**
     * Gets the read report setting.
     *
     * @return The read report setting
     */
    public int getReadReport() {
        return mReadReport;
    }

    /**
     * Sets the read report setting.
     *
     * @param readReport The read report setting
     */
    public void setReadReport(int readReport) {
        mReadReport = readReport;
    }

    /**
     * Gets the message body.
     *
     * @return The message body
     */
    public PduBody getBody() {
        return mBody;
    }

    /**
     * Sets the message body.
     *
     * @param body The message body
     */
    public void setBody(PduBody body) {
        mBody = body;
    }
}