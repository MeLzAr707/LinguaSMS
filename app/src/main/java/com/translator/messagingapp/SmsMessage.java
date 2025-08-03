package com.translator.messagingapp;

/**
 * Model class representing an SMS message.
 * Contains information about the message content, sender/recipient, and translation data.
 */
public class SmsMessage extends Message {

    /**
     * Default constructor.
     */
    public SmsMessage() {
        setMessageType(MESSAGE_TYPE_SMS);
    }

    /**
     * Constructor for creating a new SMS message.
     *
     * @param address The phone number or address of the sender/recipient
     * @param body The text content of the message
     */
    public SmsMessage(String address, String body) {
        setAddress(address);
        setBody(body);
        setDate(System.currentTimeMillis());
        setRead(true);
        setMessageType(MESSAGE_TYPE_SMS);
    }

    /**
     * Constructor for creating a new SMS message with a specific timestamp.
     *
     * @param address The phone number or address of the sender/recipient
     * @param body The text content of the message
     * @param timestamp The timestamp of the message
     */
    public SmsMessage(String address, String body, long timestamp) {
        setAddress(address);
        setBody(body);
        setDate(timestamp);
        setRead(true);
        setMessageType(MESSAGE_TYPE_SMS);
    }

    /**
     * Gets the original text content of the message.
     *
     * @return The original text
     */
    public String getOriginalText() {
        return getBody();
    }

    /**
     * Sets the original text content of the message.
     *
     * @param originalText The original text to set
     */
    public void setOriginalText(String originalText) {
        setBody(originalText);
    }
}