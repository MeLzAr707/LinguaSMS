package com.translator.messagingapp.mms;

import com.translator.messagingapp.mms.*;

import com.translator.messagingapp.message.*;

import android.net.Uri;
import android.provider.Telephony;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an MMS message with support for attachments.
 * Extends the base Message class to add MMS-specific functionality.
 */
public class MmsMessage extends Message {

    // List of attachment URIs
    private List<Attachment> attachments;

    // MMS subject (optional)
    private String subject;

    // Content location for MMS retrieval
    private String contentLocation;

    // Transaction ID
    private String transactionId;

    /**
     * Creates a new empty MMS message for testing.
     */
    public MmsMessage() {
        super();
        this.attachments = new ArrayList<>();
        setMessageType(MESSAGE_TYPE_MMS);
    }

    /**
     * Creates a new MMS message.
     *
     * @param id The message ID
     * @param body The message body text (can be empty for MMS with only attachments)
     * @param date The timestamp of the message
     * @param type The message type (inbox, sent, etc.)
     */
    public MmsMessage(String id, String body, long date, int type) {
        super(id, body, date, type);
        this.attachments = new ArrayList<>();
        setMessageType(MESSAGE_TYPE_MMS);
    }

    /**
     * Creates a new MMS message with a subject.
     *
     * @param id The message ID
     * @param body The message body text
     * @param subject The message subject
     * @param date The timestamp of the message
     * @param type The message type (inbox, sent, etc.)
     */
    public MmsMessage(String id, String body, String subject, long date, int type) {
        super(id, body, date, type);
        this.subject = subject;
        this.attachments = new ArrayList<>();
        setMessageType(MESSAGE_TYPE_MMS);
    }

    /**
     * Adds an attachment to this MMS message.
     *
     * @param attachment The attachment to add
     */
    public void addAttachment(Attachment attachment) {
        if (attachment != null) {
            attachments.add(attachment);
        }
    }

    /**
     * Gets all attachments for this MMS message.
     *
     * @return The list of attachments
     */
    public List<Attachment> getAttachmentObjects() {
        return attachments;
    }

    /**
     * Sets the attachments for this MMS message using Attachment objects.
     *
     * @param attachments The list of Attachment objects
     */
    public void setAttachmentObjects(List<Attachment> attachments) {
        this.attachments = attachments != null ? attachments : new ArrayList<>();
    }

    /**
     * Sets the attachments for this MMS message using URI objects.
     * This overrides the method in the parent class.
     *
     * @param attachments The list of attachment URIs
     */
    @Override
    public void setAttachments(List<Uri> attachments) {
        this.attachments = new ArrayList<>();
        if (attachments != null) {
            for (Uri uri : attachments) {
                if (uri != null) {
                    // Create Attachment objects from URIs with minimal information
                    Attachment attachment = new Attachment(uri, null, null, 0);
                    this.attachments.add(attachment);
                }
            }
        }
    }

    /**
     * Gets all attachment URIs for this MMS message.
     * This overrides the method in the parent class.
     *
     * @return The list of attachment URIs
     */
    @Override
    public List<Uri> getAttachments() {
        List<Uri> uriList = new ArrayList<>();
        if (attachments != null) {
            for (Attachment attachment : attachments) {
                if (attachment != null && attachment.getUri() != null) {
                    uriList.add(attachment.getUri());
                }
            }
        }
        return uriList;
    }

    /**
     * Checks if this MMS message has any attachments.
     *
     * @return True if the message has attachments, false otherwise
     */
    @Override
    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }

    /**
     * Gets the subject of this MMS message.
     *
     * @return The subject, or null if not set
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Sets the subject of this MMS message.
     *
     * @param subject The subject
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * Gets the content location for MMS retrieval.
     *
     * @return The content location URL
     */
    public String getContentLocation() {
        return contentLocation;
    }

    /**
     * Sets the content location for MMS retrieval.
     *
     * @param contentLocation The content location URL
     */
    public void setContentLocation(String contentLocation) {
        this.contentLocation = contentLocation;
    }

    /**
     * Gets the transaction ID for this MMS message.
     *
     * @return The transaction ID
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Sets the transaction ID for this MMS message.
     *
     * @param transactionId The transaction ID
     */
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Checks if this is an MMS message.
     * Always returns true for MmsMessage instances.
     *
     * @return Always true
     */
    @Override
    public boolean isMms() {
        return true;
    }

    /**
     * Sets the sender of this MMS message.
     *
     * @param sender The sender address
     */
    public void setSender(String sender) {
        setAddress(sender);
    }

    /**
     * Sets the timestamp of this MMS message.
     *
     * @param timestamp The timestamp in milliseconds
     */
    public void setTimestamp(long timestamp) {
        setDate(timestamp);
    }

    /**
     * Sets whether this MMS message has attachments.
     *
     * @param hasAttachments True if the message has attachments
     */
    public void setHasAttachments(boolean hasAttachments) {
        // This is a convenience method that doesn't store a separate flag
        // The hasAttachments() method determines this based on actual attachments list
        if (!hasAttachments && attachments != null) {
            attachments.clear();
        }
    }

    /**
     * Gets the sender address of this MMS message.
     * 
     * @return The sender address/phone number
     */
    public String getSender() {
        return getAddress();
    }
    
    /**
     * Gets the thread ID for this MMS message.
     * 
     * @return The thread ID as a string
     */
    public String getThreadId() {
        return String.valueOf(getThread());
    }
    
    /**
     * Gets the URI of the first image attachment, if any.
     * This is used for notification previews.
     * 
     * @return The URI of the first image attachment, or null if none found
     */
    public Uri getFirstImageAttachmentUri() {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }
        
        for (Attachment attachment : attachments) {
            if (attachment != null && attachment.isImage() && attachment.getUri() != null) {
                return attachment.getUri();
            }
        }
        
        return null;
    }

    /**
     * Represents an attachment in an MMS message.
     */
    public static class Attachment {
        private Uri uri;
        private String contentType;
        private String fileName;
        private long size;

        /**
         * Creates a new empty attachment.
         */
        public Attachment() {
            // Default constructor for test cases
        }

        /**
         * Creates a new attachment.
         *
         * @param uri The URI of the attachment content
         * @param contentType The MIME type of the attachment
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
         * Gets the URI of the attachment content.
         *
         * @return The URI
         */
        public Uri getUri() {
            return uri;
        }

        /**
         * Gets the MIME type of the attachment.
         *
         * @return The MIME type
         */
        public String getContentType() {
            return contentType;
        }

        /**
         * Gets the file name of the attachment.
         *
         * @return The file name
         */
        public String getFileName() {
            return fileName;
        }

        /**
         * Gets the size of the attachment in bytes.
         *
         * @return The size in bytes
         */
        public long getSize() {
            return size;
        }

        /**
         * Checks if this attachment is an image.
         *
         * @return True if the attachment is an image, false otherwise
         */
        public boolean isImage() {
            return contentType != null && contentType.startsWith("image/");
        }

        /**
         * Checks if this attachment is a video.
         *
         * @return True if the attachment is a video, false otherwise
         */
        public boolean isVideo() {
            return contentType != null && contentType.startsWith("video/");
        }

        /**
         * Checks if this attachment is audio.
         *
         * @return True if the attachment is audio, false otherwise
         */
        public boolean isAudio() {
            return contentType != null && contentType.startsWith("audio/");
        }
        /**
         * Checks if this attachment is a GIF image.
         *
         * @return True if this attachment is a GIF image, false otherwise
         */
        public boolean isGif() { return contentType != null && contentType.equals("image/gif"); }
        /**
         * Gets the part ID of this attachment.
         *
         * @return The part ID
         */
        public String getPartId() {
            // Using the URI as a unique identifier for the part
            return uri != null ? uri.getLastPathSegment() : null;
        }

        /**
         * Sets the URI of the attachment content.
         *
         * @param uri The URI
         */
        public void setUri(Uri uri) {
            this.uri = uri;
        }

        /**
         * Sets the MIME type of the attachment.
         *
         * @param contentType The MIME type
         */
        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        /**
         * Sets the file name of the attachment.
         *
         * @param fileName The file name
         */
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        /**
         * Sets the size of the attachment in bytes.
         *
         * @param size The size in bytes
         */
        public void setSize(long size) {
            this.size = size;
        }
    }
}
