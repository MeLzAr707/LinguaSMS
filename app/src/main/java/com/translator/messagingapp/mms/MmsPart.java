package com.translator.messagingapp.mms;

import android.content.ContentValues;
import android.provider.Telephony;

/**
 * Represents an MMS part/attachment with enhanced serialization support.
 * Adapted from Simple-SMS-Messenger for better MMS content handling.
 */
public class MmsPart {
    
    private String contentDisposition;
    private String charset;
    private String contentId;
    private String contentLocation;
    private String contentType;
    private String ctStart;
    private String ctType;
    private String filename;
    private String name;
    private int sequenceOrder;
    private String text;
    private String data;

    /**
     * Default constructor for MmsPart.
     */
    public MmsPart() {
        // Default constructor
    }

    /**
     * Creates an MmsPart with required fields.
     * 
     * @param contentType The MIME content type
     * @param sequenceOrder The sequence order in the MMS
     */
    public MmsPart(String contentType, int sequenceOrder) {
        this.contentType = contentType;
        this.sequenceOrder = sequenceOrder;
    }

    /**
     * Converts this MmsPart to ContentValues for database operations.
     * 
     * @return ContentValues representation
     */
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        
        if (contentDisposition != null) {
            values.put(Telephony.Mms.Part.CONTENT_DISPOSITION, contentDisposition);
        }
        if (charset != null) {
            values.put(Telephony.Mms.Part.CHARSET, charset);
        }
        if (contentId != null) {
            values.put(Telephony.Mms.Part.CONTENT_ID, contentId);
        }
        if (contentLocation != null) {
            values.put(Telephony.Mms.Part.CONTENT_LOCATION, contentLocation);
        }
        if (contentType != null) {
            values.put(Telephony.Mms.Part.CONTENT_TYPE, contentType);
        }
        if (ctStart != null) {
            values.put(Telephony.Mms.Part.CT_START, ctStart);
        }
        if (ctType != null) {
            values.put(Telephony.Mms.Part.CT_TYPE, ctType);
        }
        if (filename != null) {
            values.put(Telephony.Mms.Part.FILENAME, filename);
        }
        if (name != null) {
            values.put(Telephony.Mms.Part.NAME, name);
        }
        values.put(Telephony.Mms.Part.SEQ, sequenceOrder);
        if (text != null) {
            values.put(Telephony.Mms.Part.TEXT, text);
        }
        
        return values;
    }

    /**
     * Checks if this part contains non-text content.
     * 
     * @return True if this is a non-text part (attachment)
     */
    public boolean isNonText() {
        if (text != null) {
            return false;
        }
        
        if (contentType == null) {
            return false;
        }
        
        String lowerContentType = contentType.toLowerCase();
        return !(lowerContentType.startsWith("text") || 
                lowerContentType.equals("application/smil"));
    }

    /**
     * Checks if this part is an image.
     * 
     * @return True if this part is an image
     */
    public boolean isImage() {
        return contentType != null && contentType.toLowerCase().startsWith("image/");
    }

    /**
     * Checks if this part is a video.
     * 
     * @return True if this part is a video
     */
    public boolean isVideo() {
        return contentType != null && contentType.toLowerCase().startsWith("video/");
    }

    /**
     * Checks if this part is audio.
     * 
     * @return True if this part is audio
     */
    public boolean isAudio() {
        return contentType != null && contentType.toLowerCase().startsWith("audio/");
    }

    // Getters and setters

    public String getContentDisposition() {
        return contentDisposition;
    }

    public void setContentDisposition(String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public String getContentLocation() {
        return contentLocation;
    }

    public void setContentLocation(String contentLocation) {
        this.contentLocation = contentLocation;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getCtStart() {
        return ctStart;
    }

    public void setCtStart(String ctStart) {
        this.ctStart = ctStart;
    }

    public String getCtType() {
        return ctType;
    }

    public void setCtType(String ctType) {
        this.ctType = ctType;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSequenceOrder() {
        return sequenceOrder;
    }

    public void setSequenceOrder(int sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "MmsPart{" +
                "contentType='" + contentType + '\'' +
                ", filename='" + filename + '\'' +
                ", sequenceOrder=" + sequenceOrder +
                ", hasText=" + (text != null) +
                ", hasData=" + (data != null) +
                '}';
    }
}