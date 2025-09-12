package com.translator.messagingapp.mms.pdu;

import android.net.Uri;

/**
 * Represents a single part of an MMS message.
 */
public class PduPart {
    private byte[] mContentType;
    private byte[] mContentLocation;
    private byte[] mContentId;
    private byte[] mData;
    private Uri mDataUri;
    private String mFilename;
    private String mName;
    private int mCharset;

    /**
     * Creates a new PDU part.
     */
    public PduPart() {
        // Default constructor
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
     * Gets the content type as a string.
     *
     * @return The content type string
     */
    public String getContentTypeString() {
        return mContentType != null ? new String(mContentType) : null;
    }

    /**
     * Sets the content type from a string.
     *
     * @param contentType The content type string
     */
    public void setContentType(String contentType) {
        mContentType = contentType != null ? contentType.getBytes() : null;
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
     * Gets the content ID.
     *
     * @return The content ID
     */
    public byte[] getContentId() {
        return mContentId;
    }

    /**
     * Sets the content ID.
     *
     * @param contentId The content ID
     */
    public void setContentId(byte[] contentId) {
        mContentId = contentId;
    }

    /**
     * Gets the data.
     *
     * @return The data
     */
    public byte[] getData() {
        return mData;
    }

    /**
     * Sets the data.
     *
     * @param data The data
     */
    public void setData(byte[] data) {
        mData = data;
    }

    /**
     * Gets the data URI.
     *
     * @return The data URI
     */
    public Uri getDataUri() {
        return mDataUri;
    }

    /**
     * Sets the data URI.
     *
     * @param dataUri The data URI
     */
    public void setDataUri(Uri dataUri) {
        mDataUri = dataUri;
    }

    /**
     * Gets the filename.
     *
     * @return The filename
     */
    public String getFilename() {
        return mFilename;
    }

    /**
     * Sets the filename.
     *
     * @param filename The filename
     */
    public void setFilename(String filename) {
        mFilename = filename;
    }

    /**
     * Gets the name.
     *
     * @return The name
     */
    public String getName() {
        return mName;
    }

    /**
     * Sets the name.
     *
     * @param name The name
     */
    public void setName(String name) {
        mName = name;
    }

    /**
     * Gets the charset.
     *
     * @return The charset
     */
    public int getCharset() {
        return mCharset;
    }

    /**
     * Sets the charset.
     *
     * @param charset The charset
     */
    public void setCharset(int charset) {
        mCharset = charset;
    }

    /**
     * Checks if this is a text part.
     *
     * @return True if this is a text part
     */
    public boolean isText() {
        String contentType = getContentTypeString();
        return contentType != null && contentType.startsWith("text/");
    }

    /**
     * Checks if this is an image part.
     *
     * @return True if this is an image part
     */
    public boolean isImage() {
        String contentType = getContentTypeString();
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * Checks if this is a video part.
     *
     * @return True if this is a video part
     */
    public boolean isVideo() {
        String contentType = getContentTypeString();
        return contentType != null && contentType.startsWith("video/");
    }

    /**
     * Checks if this is an audio part.
     *
     * @return True if this is an audio part
     */
    public boolean isAudio() {
        String contentType = getContentTypeString();
        return contentType != null && contentType.startsWith("audio/");
    }

    @Override
    public String toString() {
        return "PduPart{" +
                "contentType='" + getContentTypeString() + '\'' +
                ", name='" + mName + '\'' +
                ", filename='" + mFilename + '\'' +
                ", dataUri=" + mDataUri +
                '}';
    }
}