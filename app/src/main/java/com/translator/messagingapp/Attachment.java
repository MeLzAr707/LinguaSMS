package com.translator.messagingapp;

import android.net.Uri;

/**
 * Represents an attachment in an MMS message.
 */
public class Attachment {
    private Uri uri;
    private String contentType;
    private String fileName;
    private long size;
    private String data;     // For text data
    private String text;     // For text content
    private String partId;   // For MMS part ID

    /**
     * Creates a new attachment.
     *
     * @param uri The URI of the attachment
     * @param contentType The content type (MIME type) of the attachment
     * @param fileName The file name of the attachment
     * @param size The size of the attachment in bytes
     */
    public Attachment(Uri uri, String contentType, String fileName, long size) {
        this.uri = uri;
        this.contentType = contentType;
        this.fileName = fileName;
        this.size = size;
    }

    /**
     * Gets the URI of this attachment.
     *
     * @return The URI
     */
    public Uri getUri() {
        return uri;
    }

    /**
     * Gets the content type (MIME type) of this attachment.
     *
     * @return The content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Gets the file name of this attachment.
     *
     * @return The file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Gets the size of this attachment in bytes.
     *
     * @return The size in bytes
     */
    public long getSize() {
        return size;
    }

    /**
     * Checks if this attachment is an image.
     *
     * @return True if this attachment is an image, false otherwise
     */
    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * Checks if this attachment is a video.
     *
     * @return True if this attachment is a video, false otherwise
     */
    public boolean isVideo() {
        return contentType != null && contentType.startsWith("video/");
    }

    /**
     * Checks if this attachment is audio.
     *
     * @return True if this attachment is audio, false otherwise
     */
    public boolean isAudio() {
        return contentType != null && contentType.startsWith("audio/");
    }

    /**
     * Checks if this attachment is text.
     *
     * @return True if this attachment is text, false otherwise
     */
    public boolean isText() {
        return contentType != null && contentType.startsWith("text/");
    }

    /**
     * Sets the content type of this attachment.
     *
     * @param contentType The content type to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Sets the file name of this attachment.
     *
     * @param fileName The file name to set  
     */
    public void setName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Sets the data content of this attachment.
     *
     * @param data The data content to set
     */
    public void setData(String data) {
        this.data = data;
    }

    /**
     * Gets the data content of this attachment.
     *
     * @return The data content
     */
    public String getData() {
        return data;
    }

    /**
     * Sets the text content of this attachment.
     *
     * @param text The text content to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Gets the text content of this attachment.
     *
     * @return The text content
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the part ID of this attachment.
     *
     * @param partId The part ID to set
     */
    public void setPartId(String partId) {
        this.partId = partId;
    }

    /**
     * Gets the part ID of this attachment.
     *
     * @return The part ID
     */
    public String getPartId() {
        return partId;
    }
}

