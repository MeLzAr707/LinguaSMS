package com.translator.messagingapp;

import java.util.Date;

/**
 * Model class representing an SMS message.
 * Contains information about the message content, sender/recipient, and translation data.
 */
public class SmsMessage {
    private String id;
    private String address;
    private String originalText;
    private String translatedText;
    private String originalLanguage;
    private String translatedLanguage;
    private Date timestamp;
    private boolean isIncoming;
    private boolean isRead;

    /**
     * Constructor for creating a new SMS message.
     *
     * @param address The phone number or address of the sender/recipient
     * @param text The text content of the message
     */
    public SmsMessage(String address, String text) {
        this.address = address;
        this.originalText = text;
        this.timestamp = new Date();
        this.isIncoming = false;
        this.isRead = true;
    }

    /**
     * Constructor for creating a new SMS message with a specific timestamp.
     *
     * @param address The phone number or address of the sender/recipient
     * @param text The text content of the message
     * @param timestamp The timestamp of the message
     */
    public SmsMessage(String address, String text, Date timestamp) {
        this.address = address;
        this.originalText = text;
        this.timestamp = timestamp;
        this.isIncoming = false;
        this.isRead = true;
    }

    /**
     * Gets the ID of the message.
     *
     * @return The message ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID of the message.
     *
     * @param id The message ID to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the address (phone number) of the sender/recipient.
     *
     * @return The address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Sets the address (phone number) of the sender/recipient.
     *
     * @param address The address to set
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Gets the original text content of the message.
     *
     * @return The original text
     */
    public String getOriginalText() {
        return originalText;
    }

    /**
     * Sets the original text content of the message.
     *
     * @param originalText The original text to set
     */
    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    /**
     * Gets the translated text content of the message.
     *
     * @return The translated text, or null if not translated
     */
    public String getTranslatedText() {
        return translatedText;
    }

    /**
     * Sets the translated text content of the message.
     *
     * @param translatedText The translated text to set
     */
    public void setTranslatedText(String translatedText) {
        this.translatedText = translatedText;
    }

    /**
     * Gets the language code of the original text.
     *
     * @return The original language code
     */
    public String getOriginalLanguage() {
        return originalLanguage;
    }

    /**
     * Sets the language code of the original text.
     *
     * @param originalLanguage The original language code to set
     */
    public void setOriginalLanguage(String originalLanguage) {
        this.originalLanguage = originalLanguage;
    }

/**
 * Gets the language code of the translated text.
 *
 * @return The translated language code

 */
public String getTranslatedLanguage() {
    return translatedLanguage;
}

    /**
     * Sets the language code of the translated text.
     *
     * @param translatedLanguage The translated language code to set
     */
    public void setTranslatedLanguage(String translatedLanguage) {
        this.translatedLanguage = translatedLanguage;
    }

    /**
     * Gets the timestamp of the message.
     *
     * @return The timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp of the message.
     *
     * @param timestamp The timestamp to set
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Checks if the message is incoming (received) or outgoing (sent).
     *
     * @return true if the message is incoming, false if outgoing
     */
    public boolean isIncoming() {
        return isIncoming;
    }

    /**
     * Sets whether the message is incoming or outgoing.
     *
     * @param incoming true if the message is incoming, false if outgoing
     */
    public void setIncoming(boolean incoming) {
        isIncoming = incoming;
    }

    /**
     * Checks if the message has been read.
     *
     * @return true if the message has been read, false otherwise
     */
    public boolean isRead() {
        return isRead;
    }

    /**
     * Sets whether the message has been read.
     *
     * @param read true if the message has been read, false otherwise
     */
    public void setRead(boolean read) {
        isRead = read;
    }

    /**
     * Checks if the message has been translated.
     *
     * @return true if the message has been translated, false otherwise
     */
    public boolean isTranslated() {
        return translatedText != null && !translatedText.isEmpty();
    }

    /**
     * Gets the display text for the message, which is either the translated text if available,
     * or the original text otherwise.
     *
     * @return The display text
     */
    public String getDisplayText() {
        return isTranslated() ? translatedText : originalText;
    }

    /**
     * Returns a string representation of the message.
     *
     * @return A string representation
     */
    @Override
    public String toString() {
        String truncatedOriginal = originalText != null ?
                originalText.substring(0, Math.min(20, originalText.length())) + "..." : "null";
        String truncatedTranslated = translatedText != null ?
                translatedText.substring(0, Math.min(20, translatedText.length())) + "..." : "null";

        return "SmsMessage{" +
                "id='" + id + '\'' +
                ", address='" + address + '\'' +
                ", originalText='" + truncatedOriginal + '\'' +
                ", translatedText='" + truncatedTranslated + '\'' +
                ", originalLanguage='" + originalLanguage + '\'' +
                ", translatedLanguage='" + translatedLanguage + '\'' +
                ", timestamp=" + timestamp +
                ", isIncoming=" + isIncoming +
                ", isRead=" + isRead +
                '}';
    }

    /**
     * Checks if this message is equal to another object.
     *
     * @param o The object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SmsMessage message = (SmsMessage) o;

        if (id != null) {
            return id.equals(message.id);
        } else {
            // If ID is not available, compare other fields
            if (timestamp == null || message.timestamp == null) return false;
            if (!timestamp.equals(message.timestamp)) return false;
            if (!address.equals(message.address)) return false;
            return originalText.equals(message.originalText);
        }
    }

    /**
     * Generates a hash code for this message.
     *
     * @return The hash code
     */
    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        } else {
            int result = address.hashCode();
            result = 31 * result + originalText.hashCode();
            result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
            return result;
        }
    }
}
