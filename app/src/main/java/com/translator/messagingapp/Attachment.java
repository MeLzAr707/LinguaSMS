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
     * Gets the part ID of this attachment.
     *
     * @return The part ID
     */
    public String getPartId() {
        // Using the URI as a unique identifier for the part
        return uri != null ? uri.getLastPathSegment() : null;
    }
}

