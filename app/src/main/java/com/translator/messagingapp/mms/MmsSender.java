package com.translator.messagingapp.mms;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Android 10+ (API 29) compatible MMS sender with enhanced callback support.
 * This class provides modern MMS sending capabilities with proper error handling
 * and message status callbacks.
 */
public class MmsSender {
    private static final String TAG = "MmsSender";
    private final Context context;
    
    // Unique ID for the message in our local database (not the message ID)
    private final long MESSAGE_ID = System.currentTimeMillis();

    public MmsSender(Context context) {
        this.context = context;
    }

    /**
     * Sends an MMS message using MmsManager.
     * NOTE: This is for Android 10 (API 29) and above.
     *
     * @param recipientNumber The phone number of the recipient.
     * @param subject The subject of the MMS.
     * @param imageUri The Uri of the local content (e.g., an image) to be sent.
     */
    public void sendMms(String recipientNumber, String subject, Uri imageUri) {
        sendMms(recipientNumber, subject, imageUri, null);
    }

    /**
     * Sends an MMS message using MmsManager with callback support.
     * NOTE: This is for Android 10 (API 29) and above.
     *
     * @param recipientNumber The phone number of the recipient.
     * @param subject The subject of the MMS.
     * @param imageUri The Uri of the local content (e.g., an image) to be sent.
     * @param callback Optional callback for handling send results.
     */
    public void sendMms(String recipientNumber, String subject, Uri imageUri, SendMultimediaMessageCallback callback) {
        // 1. Get the default SMS subscription ID
        int subscriptionId = SmsManager.getDefaultSmsSubscriptionId();
        
        // 2. Get the SmsManager for this subscription
        SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId);
        
        // 3. Create the MMS draft in the provider
        Uri contentUri = createMmsDraft(recipientNumber, subject, imageUri);
        if (contentUri == null) {
            Log.e(TAG, "Failed to create MMS draft URI.");
            if (callback != null) {
                callback.onSendMmsError(null, -1);
            }
            return;
        }

        // 4. Call the platform's sendMultimediaMessage to initiate sending
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10+
            smsManager.sendMultimediaMessage(
                context,
                contentUri, // The URI of the MMS PDU in the Telephony.Mms table
                null,       // MMSC URL (null uses carrier settings)
                null,       // HTTP Proxy (null uses carrier settings)
                new SendMultimediaMessageCallback() {
                    @Override
                    public void onSendMmsComplete(Uri uri) {
                        Log.d(TAG, "MMS send successful. Result URI: " + uri);
                        // Update the message status in your local database from 'draft' to 'sent'
                        updateMessageStatus(contentUri, Telephony.Mms.MESSAGE_BOX_SENT);
                        if (callback != null) {
                            callback.onSendMmsComplete(uri);
                        }
                    }
                    
                    @Override
                    public void onSendMmsError(Uri uri, int errorCode) {
                        Log.e(TAG, "MMS send failed. Error code: " + errorCode);
                        // Handle failure, update local message status to 'failed'
                        updateMessageStatus(contentUri, Telephony.Mms.MESSAGE_BOX_FAILED);
                        if (callback != null) {
                            callback.onSendMmsError(uri, errorCode);
                        }
                    }
                }
            );
        } else {
            Log.e(TAG, "This implementation requires Android 10 (API 29) or higher");
            if (callback != null) {
                callback.onSendMmsError(contentUri, -2);
            }
        }
    }

    /**
     * Creates a proper MMS draft in the Telephony provider with the image attachment.
     * This is a complete implementation for Android 10+.
     */
    private Uri createMmsDraft(String recipient, String subject, Uri imageUri) {
        try {
            ContentResolver contentResolver = context.getContentResolver();
            
            // 1. Create the MMS message entry in the provider
            ContentValues values = new ContentValues();
            values.put(Telephony.Mms.SUBJECT, subject);
            values.put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_DRAFTS);
            values.put(Telephony.Mms.READ, 1);
            values.put(Telephony.Mms.SEEN, 1);
            values.put(Telephony.Mms.MESSAGE_TYPE, Telephony.Mms.MESSAGE_TYPE_SEND_REQ);
            values.put(Telephony.Mms.MMS_VERSION, 0x10); // MMS 1.0
            values.put(Telephony.Mms.PRIORITY, 129); // Normal priority
            values.put(Telephony.Mms.READ_REPORT, 0); // No read report
            values.put(Telephony.Mms.CONTENT_CLASS, 0); // Personal content class
            values.put(Telephony.Mms.DELIVERY_REPORT, 0); // No delivery report
            values.put(Telephony.Mms.EXPIRY, 604800); // 1 week
            values.put(Telephony.Mms.DATE, System.currentTimeMillis() / 1000);
            
            // Insert the MMS message and get its URI
            Uri messageUri = contentResolver.insert(Telephony.Mms.CONTENT_URI, values);
            if (messageUri == null) {
                Log.e(TAG, "Failed to insert MMS message into provider");
                return null;
            }
            
            // Extract the message ID from the URI
            String messageId = messageUri.getLastPathSegment();
            Log.d(TAG, "Created MMS message with ID: " + messageId);
            
            // 2. Add the recipient address part
            addAddressPart(contentResolver, messageId, recipient, Telephony.Mms.Addr.TYPE_TO);
            
            // 3. Add the image as a part
            String contentType = contentResolver.getType(imageUri);
            if (contentType == null) {
                contentType = "image/jpeg"; // Default to JPEG if type cannot be determined
            }
            
            // Read the image data
            byte[] imageData = readBytes(contentResolver, imageUri);
            if (imageData == null || imageData.length == 0) {
                Log.e(TAG, "Failed to read image data");
                return null;
            }
            
            // Add the image part
            addMediaPart(contentResolver, messageId, contentType, "image.jpg", imageData);
            
            // 4. Add a text part if needed (optional)
            if (subject != null && !subject.isEmpty()) {
                addMediaPart(contentResolver, messageId, "text/plain", "text.txt", subject.getBytes());
            }
            
            return messageUri;
        } catch (Exception e) {
            Log.e(TAG, "Error creating MMS draft: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Adds an address part (recipient) to the MMS message.
     */
    private void addAddressPart(ContentResolver contentResolver, String messageId, String address, int type) {
        ContentValues addrValues = new ContentValues();
        addrValues.put(Telephony.Mms.Addr.ADDRESS, address);
        addrValues.put(Telephony.Mms.Addr.TYPE, type);
        addrValues.put(Telephony.Mms.Addr.CHARSET, 106); // UTF-8
        addrValues.put(Telephony.Mms.Addr.MSG_ID, messageId);
        
        Uri addrUri = Uri.parse("content://mms/" + messageId + "/addr");
        contentResolver.insert(addrUri, addrValues);
    }
    
    /**
     * Adds a media part (image, text, etc.) to the MMS message.
     */
    private void addMediaPart(ContentResolver contentResolver, String messageId, 
                             String contentType, String fileName, byte[] data) {
        // 1. Create the part entry
        ContentValues partValues = new ContentValues();
        partValues.put(Telephony.Mms.Part.MSG_ID, messageId);
        partValues.put(Telephony.Mms.Part.CONTENT_TYPE, contentType);
        partValues.put(Telephony.Mms.Part.NAME, fileName);
        partValues.put(Telephony.Mms.Part.CONTENT_ID, "<" + System.currentTimeMillis() + ">");
        partValues.put(Telephony.Mms.Part.CONTENT_LOCATION, fileName);
        partValues.put(Telephony.Mms.Part.CHARSET, 106); // UTF-8
        
        // 2. Insert the part and get its URI
        Uri partUri = Uri.parse("content://mms/" + messageId + "/part");
        Uri uri = contentResolver.insert(partUri, partValues);
        
        if (uri != null) {
            // 3. Write the data to the part
            try (java.io.OutputStream os = contentResolver.openOutputStream(uri)) {
                if (os != null) {
                    os.write(data);
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to write data to part: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Reads bytes from a content URI.
     */
    private byte[] readBytes(ContentResolver contentResolver, Uri uri) {
        try (InputStream inputStream = contentResolver.openInputStream(uri);
             ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream()) {
            
            if (inputStream == null) {
                return null;
            }
            
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.getMessage(), e);
            return null;
        } catch (IOException e) {
            Log.e(TAG, "Error reading file: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Updates the message status in the provider.
     */
    private void updateMessageStatus(Uri messageUri, int messageBox) {
        try {
            ContentValues values = new ContentValues();
            values.put(Telephony.Mms.MESSAGE_BOX, messageBox);
            
            ContentResolver contentResolver = context.getContentResolver();
            contentResolver.update(messageUri, values, null, null);
        } catch (Exception e) {
            Log.e(TAG, "Error updating message status: " + e.getMessage(), e);
        }
    }
    
    /**
     * Interface for MMS send callbacks.
     */
    public interface SendMultimediaMessageCallback {
        void onSendMmsComplete(Uri uri);
        void onSendMmsError(Uri uri, int errorCode);
    }
}