package com.translator.messagingapp.mms;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;

import com.translator.messagingapp.mms.pdu.PduHeaders;

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
    
    // Address type constants (from MMS specifications)
    private static final int TYPE_TO = 151;   // Recipient address

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
     * IMPORTANT: The callback parameter is kept for API compatibility, but due to Android API
     * changes in API 29+, actual results are delivered via PendingIntent to a BroadcastReceiver.
     * The callback will only be used for immediate error conditions (e.g., failed to create draft).
     * For send success/failure, implement a BroadcastReceiver for "com.translator.messagingapp.MMS_SENT".
     *
     * @param recipientNumber The phone number of the recipient.
     * @param subject The subject of the MMS.
     * @param imageUri The Uri of the local content (e.g., an image) to be sent.
     * @param callback Optional callback for handling immediate errors (not send results).
     */
    public void sendMms(String recipientNumber, String subject, Uri imageUri, SendMultimediaMessageCallback callback) {
        try {
            // 1. Check Android version compatibility
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                Log.e(TAG, "This implementation requires Android 10 (API 29) or higher");
                if (callback != null) {
                    callback.onSendMmsError(null, -2);
                }
                return;
            }

            // 2. Get the default SMS subscription ID and validate
            int subscriptionId = SmsManager.getDefaultSmsSubscriptionId();
            if (subscriptionId == -1) {
                Log.e(TAG, "No default SMS subscription available");
                if (callback != null) {
                    callback.onSendMmsError(null, -3);
                }
                return;
            }
            
            // 3. Get the SmsManager for this subscription
            SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId);
            if (smsManager == null) {
                Log.e(TAG, "Unable to get SmsManager for subscription " + subscriptionId);
                if (callback != null) {
                    callback.onSendMmsError(null, -4);
                }
                return;
            }
            
            // 4. Create the MMS draft in the provider with proper formatting
            Uri contentUri = createMmsDraft(recipientNumber, subject, imageUri);
            if (contentUri == null) {
                Log.e(TAG, "Failed to create MMS draft URI.");
                if (callback != null) {
                    callback.onSendMmsError(null, -1);
                }
                return;
            }

            // 5. Create PendingIntent for send result with proper flags
            Intent sentIntent = new Intent("com.translator.messagingapp.MMS_SENT");
            sentIntent.setPackage(context.getPackageName()); // Ensure intent stays in our app
            sentIntent.putExtra("message_uri", contentUri.toString());
            sentIntent.putExtra("recipient", recipientNumber);
            sentIntent.putExtra("subscription_id", subscriptionId);
            
            PendingIntent sentPendingIntent = PendingIntent.getBroadcast(
                context,
                (int) System.currentTimeMillis(), // Unique request code
                sentIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE :
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
            
            // 6. Call the platform's sendMultimediaMessage to initiate sending
            Log.d(TAG, "Sending MMS with subscription " + subscriptionId + ", content URI: " + contentUri);
            
            smsManager.sendMultimediaMessage(
                context,
                contentUri,         // The URI of the MMS PDU in the Telephony.Mms table
                null,              // MMSC URL (null uses carrier settings)
                null,              // Configuration overrides (null uses defaults)
                sentPendingIntent  // PendingIntent for send result
            );
            
            Log.d(TAG, "MMS send request submitted successfully. Content URI: " + contentUri);
            
            // Update message status to sending
            updateMessageStatus(contentUri, Telephony.Mms.MESSAGE_BOX_OUTBOX);
            
            // Call success callback for immediate feedback (actual send result comes via broadcast)
            if (callback != null) {
                callback.onSendMmsComplete(contentUri);
            }
            
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException sending MMS - check permissions", e);
            if (callback != null) {
                callback.onSendMmsError(null, -5);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error sending MMS", e);
            if (callback != null) {
                callback.onSendMmsError(null, -6);
            }
        }
    }

    /**
     * Creates a proper MMS draft in the Telephony provider with the image attachment.
     * This is a complete implementation for Android 10+.
     */
    private Uri createMmsDraft(String recipient, String subject, Uri imageUri) {
        ContentResolver contentResolver = context.getContentResolver();
        
        try {
            // 1. Create the MMS message entry in the provider with proper headers
            ContentValues values = new ContentValues();
            values.put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_DRAFTS);
            values.put(Telephony.Mms.DATE, System.currentTimeMillis() / 1000);
            values.put(Telephony.Mms.MESSAGE_TYPE, PduHeaders.MESSAGE_TYPE_SEND_REQ);
            values.put(Telephony.Mms.MMS_VERSION, PduHeaders.CURRENT_MMS_VERSION);
            values.put(Telephony.Mms.READ, 1);
            values.put(Telephony.Mms.SEEN, 1);
            
            // Set subject if provided
            if (subject != null && !subject.trim().isEmpty()) {
                values.put(Telephony.Mms.SUBJECT, subject.trim());
                values.put(Telephony.Mms.SUBJECT_CHARSET, 106); // UTF-8
            }
            
            // Set proper MMS headers for Android 10+ compatibility
            values.put(Telephony.Mms.PRIORITY, PduHeaders.PRIORITY_NORMAL);
            values.put(Telephony.Mms.DELIVERY_REPORT, PduHeaders.VALUE_NO);
            values.put(Telephony.Mms.READ_REPORT, PduHeaders.VALUE_NO);
            values.put(Telephony.Mms.CONTENT_CLASS, PduHeaders.CONTENT_CLASS_IMAGE);
            values.put(Telephony.Mms.EXPIRY, 604800L); // 1 week in seconds
            
            // Essential for Android 10+ - set content type
            values.put(Telephony.Mms.CONTENT_TYPE, "application/vnd.wap.multipart.related");
            
            // Insert the MMS message and get its URI
            Uri messageUri = contentResolver.insert(Telephony.Mms.CONTENT_URI, values);
            if (messageUri == null) {
                Log.e(TAG, "Failed to insert MMS message into provider");
                return null;
            }
            
            // Extract the message ID from the URI
            String messageId = messageUri.getLastPathSegment();
            Log.d(TAG, "Created MMS message with ID: " + messageId);
            
            // 2. Add the recipient address part - this is critical for Android 10+
            if (!addAddressPart(contentResolver, messageId, recipient, TYPE_TO)) {
                Log.e(TAG, "Failed to add recipient address part");
                // Clean up the message we created
                contentResolver.delete(messageUri, null, null);
                return null;
            }
            
            // 3. Add the image as a multimedia part
            if (!addImagePart(contentResolver, messageId, imageUri)) {
                Log.e(TAG, "Failed to add image part");
                // Clean up the message we created
                contentResolver.delete(messageUri, null, null);
                return null;
            }
            
            // 4. Add a text part if we have a meaningful subject (not just the image filename)
            if (subject != null && !subject.trim().isEmpty() && !isImageFileName(subject)) {
                addTextPart(contentResolver, messageId, subject.trim());
            }
            
            Log.d(TAG, "Successfully created MMS draft with ID: " + messageId + " for recipient: " + recipient);
            return messageUri;
            
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException creating MMS draft - check permissions", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error creating MMS draft: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Adds an address part (recipient) to the MMS message.
     * @return true if successful, false otherwise
     */
    private boolean addAddressPart(ContentResolver contentResolver, String messageId, String address, int type) {
        try {
            ContentValues addrValues = new ContentValues();
            addrValues.put(Telephony.Mms.Addr.ADDRESS, address);
            addrValues.put(Telephony.Mms.Addr.TYPE, type);
            addrValues.put(Telephony.Mms.Addr.CHARSET, 106); // UTF-8
            addrValues.put(Telephony.Mms.Addr.MSG_ID, messageId);
            
            Uri addrUri = Uri.parse("content://mms/" + messageId + "/addr");
            Uri insertedUri = contentResolver.insert(addrUri, addrValues);
            
            if (insertedUri != null) {
                Log.d(TAG, "Successfully added address part: " + address + " with type " + type);
                return true;
            } else {
                Log.e(TAG, "Failed to insert address part for " + address);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception adding address part for " + address, e);
            return false;
        }
    }
    
    /**
     * Adds an image part to the MMS message.
     * @return true if successful, false otherwise
     */
    private boolean addImagePart(ContentResolver contentResolver, String messageId, Uri imageUri) {
        try {
            // Get the content type from the URI
            String contentType = contentResolver.getType(imageUri);
            if (contentType == null) {
                // Try to determine from file extension or default to JPEG
                String uriString = imageUri.toString();
                if (uriString.contains(".png")) {
                    contentType = "image/png";
                } else if (uriString.contains(".gif")) {
                    contentType = "image/gif";
                } else {
                    contentType = "image/jpeg"; // Default
                }
            }
            
            // Ensure we support this image type
            if (!contentType.startsWith("image/")) {
                Log.e(TAG, "Unsupported content type for MMS image: " + contentType);
                return false;
            }
            
            Log.d(TAG, "Adding image part with content type: " + contentType);
            
            // Read the image data
            byte[] imageData = readBytes(contentResolver, imageUri);
            if (imageData == null || imageData.length == 0) {
                Log.e(TAG, "Failed to read image data or image is empty");
                return false;
            }
            
            // Check image size - Android typically limits MMS to 1MB total
            if (imageData.length > 1024 * 1024) { // 1MB
                Log.w(TAG, "Image size (" + imageData.length + " bytes) may be too large for MMS");
                // Could implement compression here if needed
            }
            
            // Create the image part with proper Android 10+ formatting
            ContentValues partValues = new ContentValues();
            partValues.put(Telephony.Mms.Part.MSG_ID, messageId);
            partValues.put(Telephony.Mms.Part.CONTENT_TYPE, contentType);
            partValues.put(Telephony.Mms.Part.NAME, "image");
            partValues.put(Telephony.Mms.Part.FILENAME, "image");
            
            // Generate unique content ID for this part
            String contentId = "<" + System.currentTimeMillis() + "@" + MESSAGE_ID + ">";
            partValues.put(Telephony.Mms.Part.CONTENT_ID, contentId);
            partValues.put(Telephony.Mms.Part.CONTENT_LOCATION, "image");
            partValues.put(Telephony.Mms.Part.CONTENT_DISPOSITION, "attachment");
            partValues.put(Telephony.Mms.Part.CHARSET, 106); // UTF-8
            
            // Insert the part and get its URI
            Uri partUri = Uri.parse("content://mms/" + messageId + "/part");
            Uri insertedPartUri = contentResolver.insert(partUri, partValues);
            
            if (insertedPartUri != null) {
                // Write the image data to the part
                try (java.io.OutputStream os = contentResolver.openOutputStream(insertedPartUri)) {
                    if (os != null) {
                        os.write(imageData);
                        os.flush();
                        Log.d(TAG, "Successfully wrote " + imageData.length + " bytes to image part");
                        return true;
                    } else {
                        Log.e(TAG, "Could not open output stream for image part");
                        return false;
                    }
                }
            } else {
                Log.e(TAG, "Failed to insert image part");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception adding image part", e);
            return false;
        }
    }
    
    /**
     * Adds a text part to the MMS message.
     * @return true if successful, false otherwise
     */
    private boolean addTextPart(ContentResolver contentResolver, String messageId, String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                Log.d(TAG, "No text to add to MMS");
                return true; // Not an error
            }
            
            byte[] textData = text.trim().getBytes("UTF-8");
            
            // Create the text part
            ContentValues partValues = new ContentValues();
            partValues.put(Telephony.Mms.Part.MSG_ID, messageId);
            partValues.put(Telephony.Mms.Part.CONTENT_TYPE, "text/plain");
            partValues.put(Telephony.Mms.Part.NAME, "text");
            partValues.put(Telephony.Mms.Part.FILENAME, "text");
            
            // Generate unique content ID for this part
            String contentId = "<text_" + System.currentTimeMillis() + "@" + MESSAGE_ID + ">";
            partValues.put(Telephony.Mms.Part.CONTENT_ID, contentId);
            partValues.put(Telephony.Mms.Part.CONTENT_LOCATION, "text");
            partValues.put(Telephony.Mms.Part.CHARSET, 106); // UTF-8
            
            // Insert the part and get its URI
            Uri partUri = Uri.parse("content://mms/" + messageId + "/part");
            Uri insertedPartUri = contentResolver.insert(partUri, partValues);
            
            if (insertedPartUri != null) {
                // Write the text data to the part
                try (java.io.OutputStream os = contentResolver.openOutputStream(insertedPartUri)) {
                    if (os != null) {
                        os.write(textData);
                        os.flush();
                        Log.d(TAG, "Successfully added text part: " + text.substring(0, Math.min(50, text.length())));
                        return true;
                    } else {
                        Log.e(TAG, "Could not open output stream for text part");
                        return false;
                    }
                }
            } else {
                Log.e(TAG, "Failed to insert text part");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception adding text part", e);
            return false;
        }
    }
    
    /**
     * Helper method to check if a string looks like an image filename.
     */
    private boolean isImageFileName(String text) {
        if (text == null) return false;
        String lowerText = text.toLowerCase();
        return lowerText.endsWith(".jpg") || lowerText.endsWith(".jpeg") ||
               lowerText.endsWith(".png") || lowerText.endsWith(".gif") ||
               lowerText.endsWith(".bmp") || lowerText.endsWith(".webp");
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
     * This is important for proper MMS lifecycle management on Android 10+.
     */
    private void updateMessageStatus(Uri messageUri, int messageBox) {
        try {
            ContentValues values = new ContentValues();
            values.put(Telephony.Mms.MESSAGE_BOX, messageBox);
            
            // Add timestamp for the status change
            if (messageBox == Telephony.Mms.MESSAGE_BOX_OUTBOX) {
                values.put(Telephony.Mms.DATE, System.currentTimeMillis() / 1000);
            }
            
            ContentResolver contentResolver = context.getContentResolver();
            int rowsUpdated = contentResolver.update(messageUri, values, null, null);
            
            if (rowsUpdated > 0) {
                Log.d(TAG, "Updated message status to box: " + messageBox + " for URI: " + messageUri);
            } else {
                Log.w(TAG, "No rows updated when changing message box to: " + messageBox);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException updating message status - check permissions", e);
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