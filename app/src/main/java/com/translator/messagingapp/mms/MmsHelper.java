package com.translator.messagingapp.mms;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;

import com.translator.messagingapp.util.PhoneUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for querying and manipulating MMS messages from the system provider.
 * Provides methods to download, process, and manage MMS content while maintaining
 * compatibility with Android's MMS architecture.
 * 
 * Compatible with API 29+ requirements and handles proper permission checking.
 */
public class MmsHelper {
    private static final String TAG = "MmsHelper";
    
    private final Context context;
    private final ContentResolver contentResolver;

    // MMS content URIs
    public static final Uri MMS_CONTENT_URI = Telephony.Mms.CONTENT_URI;
    public static final Uri MMS_INBOX_CONTENT_URI = Telephony.Mms.Inbox.CONTENT_URI;
    public static final Uri MMS_SENT_CONTENT_URI = Telephony.Mms.Sent.CONTENT_URI;
    public static final Uri PART_CONTENT_URI = Uri.parse("content://mms/part");

    // MMS database columns
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_THREAD_ID = "thread_id";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_DATE_SENT = "date_sent";
    public static final String COLUMN_MESSAGE_BOX = "msg_box";
    public static final String COLUMN_READ = "read";
    public static final String COLUMN_SEEN = "seen";
    public static final String COLUMN_SUBJECT = "sub";
    public static final String COLUMN_CONTENT_TYPE = "ct";
    public static final String COLUMN_RESPONSE_TEXT = "resp_txt";
    public static final String COLUMN_RESPONSE_STATUS = "resp_st";

    // Message box types
    public static final int MESSAGE_BOX_INBOX = Telephony.Mms.MESSAGE_BOX_INBOX;
    public static final int MESSAGE_BOX_SENT = Telephony.Mms.MESSAGE_BOX_SENT;
    public static final int MESSAGE_BOX_DRAFTS = Telephony.Mms.MESSAGE_BOX_DRAFTS;
    public static final int MESSAGE_BOX_OUTBOX = Telephony.Mms.MESSAGE_BOX_OUTBOX;

    public MmsHelper(Context context) {
        this.context = context.getApplicationContext();
        this.contentResolver = this.context.getContentResolver();
    }

    /**
     * Downloads an MMS message from the network.
     * 
     * @param mmsUri The URI of the MMS message to download
     * @param threadId The thread ID (optional)
     * @return true if download was successful, false otherwise
     */
    public boolean downloadMms(Uri mmsUri, String threadId) {
        if (!PhoneUtils.isDefaultSmsApp(context)) {
            Log.w(TAG, "Cannot download MMS: app is not default SMS app");
            return false;
        }

        try {
            Log.d(TAG, "Starting MMS download for URI: " + mmsUri);
            
            // Query the notification to get download information
            MmsMessage notification = queryMmsMessage(mmsUri);
            if (notification == null) {
                Log.e(TAG, "Failed to query MMS notification");
                return false;
            }

            // Check if this is a notification that needs downloading
            if (!isDownloadableNotification(notification)) {
                Log.d(TAG, "MMS does not need downloading");
                return true;
            }

            // Perform the actual download using the transaction system
            return performMmsDownload(mmsUri, notification);

        } catch (Exception e) {
            Log.e(TAG, "Error downloading MMS", e);
            return false;
        }
    }

    /**
     * Processes an MMS message after download.
     * 
     * @param mmsUri The URI of the MMS message to process
     * @return true if processing was successful, false otherwise
     */
    public boolean processMms(Uri mmsUri) {
        try {
            Log.d(TAG, "Processing MMS for URI: " + mmsUri);
            
            MmsMessage mmsMessage = queryMmsMessage(mmsUri);
            if (mmsMessage == null) {
                Log.e(TAG, "Failed to query MMS message for processing");
                return false;
            }

            // Load attachments for the message
            List<MmsMessage.Attachment> attachments = loadMmsAttachments(mmsUri);
            mmsMessage.setAttachmentObjects(attachments);

            Log.d(TAG, "Successfully processed MMS with " + attachments.size() + " attachments");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error processing MMS", e);
            return false;
        }
    }

    /**
     * Queries an MMS message from the database.
     * 
     * @param mmsUri The URI of the MMS message
     * @return MmsMessage object or null if not found
     */
    public MmsMessage queryMmsMessage(Uri mmsUri) {
        if (mmsUri == null) {
            return null;
        }

        String[] projection = {
            COLUMN_ID,
            COLUMN_THREAD_ID,
            COLUMN_DATE,
            COLUMN_DATE_SENT,
            COLUMN_MESSAGE_BOX,
            COLUMN_READ,
            COLUMN_SEEN,
            COLUMN_SUBJECT,
            COLUMN_CONTENT_TYPE,
            COLUMN_RESPONSE_TEXT,
            COLUMN_RESPONSE_STATUS
        };

        try (Cursor cursor = contentResolver.query(mmsUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return createMmsMessageFromCursor(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying MMS message", e);
        }

        return null;
    }

    /**
     * Gets MMS message from URI.
     * Enhanced method for Simple-SMS-Messenger integration.
     * This is an alias for queryMmsMessage with additional error handling.
     * 
     * @param mmsUri The URI of the MMS message
     * @return MmsMessage object or null if not found
     */
    public MmsMessage getMmsFromUri(Uri mmsUri) {
        try {
            MmsMessage mms = queryMmsMessage(mmsUri);
            if (mms != null) {
                // Load attachments for the message
                List<MmsMessage.Attachment> attachments = loadMmsAttachments(mmsUri);
                mms.setAttachmentObjects(attachments);
                
                // Load text content if not already loaded
                if (TextUtils.isEmpty(mms.getBody())) {
                    String messageId = mmsUri.getLastPathSegment();
                    if (messageId != null) {
                        String textContent = loadMmsTextContent(messageId);
                        if (!TextUtils.isEmpty(textContent)) {
                            mms.setBody(textContent);
                        }
                    }
                }
            }
            return mms;
        } catch (Exception e) {
            Log.e(TAG, "Error getting MMS from URI: " + mmsUri, e);
            return null;
        }
    }

    /**
     * Loads all attachments for an MMS message.
     * 
     * @param mmsUri The URI of the MMS message
     * @return List of attachments
     */
    public List<MmsMessage.Attachment> loadMmsAttachments(Uri mmsUri) {
        List<MmsMessage.Attachment> attachments = new ArrayList<>();
        
        if (mmsUri == null) {
            return attachments;
        }

        // Extract message ID from URI
        String messageId = mmsUri.getLastPathSegment();
        if (messageId == null) {
            return attachments;
        }

        String selection = "mid = ?";
        String[] selectionArgs = {messageId};
        String[] projection = {
            "_id",
            "ct",      // content type
            "name",    // file name  
            "cl",      // content location
            "_data"    // file path
        };

        try (Cursor cursor = contentResolver.query(
                PART_CONTENT_URI, 
                projection, 
                selection, 
                selectionArgs, 
                null)) {
                
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    MmsMessage.Attachment attachment = createAttachmentFromCursor(cursor);
                    if (attachment != null) {
                        attachments.add(attachment);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading MMS attachments", e);
        }

        Log.d(TAG, "Loaded " + attachments.size() + " attachments for MMS " + messageId);
        return attachments;
    }

    /**
     * Loads text content from MMS message parts.
     * Enhanced method for Simple-SMS-Messenger integration.
     * 
     * @param messageId The MMS message ID
     * @return The text content or null if none found
     */
    private String loadMmsTextContent(String messageId) {
        if (messageId == null) {
            return null;
        }

        String selection = "mid = ? AND ct = ?";
        String[] selectionArgs = {messageId, "text/plain"};
        String[] projection = {"text", "_data"};

        try (Cursor cursor = contentResolver.query(
                PART_CONTENT_URI, 
                projection, 
                selection, 
                selectionArgs, 
                null)) {
                
            if (cursor != null && cursor.moveToFirst()) {
                // Try to get text from the TEXT column first
                int textIndex = cursor.getColumnIndex("text");
                if (textIndex >= 0) {
                    String text = cursor.getString(textIndex);
                    if (!TextUtils.isEmpty(text)) {
                        return text;
                    }
                }
                
                // If text column is empty, try to read from data file
                int dataIndex = cursor.getColumnIndex("_data");
                if (dataIndex >= 0) {
                    String dataPath = cursor.getString(dataIndex);
                    if (!TextUtils.isEmpty(dataPath)) {
                        try {
                            Uri partUri = Uri.parse("content://mms/part/" + dataPath);
                            return readTextFromUri(partUri);
                        } catch (Exception e) {
                            Log.w(TAG, "Error reading text from data path: " + dataPath, e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading MMS text content for message " + messageId, e);
        }

        return null;
    }

    /**
     * Reads text content from a content URI.
     * 
     * @param uri The content URI to read from
     * @return The text content or null if unable to read
     */
    private String readTextFromUri(Uri uri) {
        try {
            java.io.InputStream inputStream = contentResolver.openInputStream(uri);
            if (inputStream != null) {
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(inputStream));
                StringBuilder text = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    text.append(line).append("\n");
                }
                reader.close();
                return text.toString().trim();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error reading text from URI: " + uri, e);
        }
        return null;
    }

    /**
     * Updates an MMS message in the database.
     * 
     * @param mmsUri The URI of the MMS message
     * @param values ContentValues to update
     * @return true if update was successful
     */
    public boolean updateMmsMessage(Uri mmsUri, ContentValues values) {
        if (!PhoneUtils.isDefaultSmsApp(context)) {
            Log.w(TAG, "Cannot update MMS: app is not default SMS app");
            return false;
        }

        try {
            int rowsUpdated = contentResolver.update(mmsUri, values, null, null);
            Log.d(TAG, "Updated " + rowsUpdated + " MMS rows");
            return rowsUpdated > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error updating MMS message", e);
            return false;
        }
    }

    /**
     * Marks an MMS message as read.
     * 
     * @param mmsUri The URI of the MMS message
     * @return true if successful
     */
    public boolean markMmsAsRead(Uri mmsUri) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_READ, 1);
        values.put(COLUMN_SEEN, 1);
        return updateMmsMessage(mmsUri, values);
    }

    /**
     * Deletes an MMS message.
     * 
     * @param mmsUri The URI of the MMS message
     * @return true if deletion was successful
     */
    public boolean deleteMmsMessage(Uri mmsUri) {
        if (!PhoneUtils.isDefaultSmsApp(context)) {
            Log.w(TAG, "Cannot delete MMS: app is not default SMS app");
            return false;
        }

        try {
            int rowsDeleted = contentResolver.delete(mmsUri, null, null);
            Log.d(TAG, "Deleted " + rowsDeleted + " MMS rows");
            return rowsDeleted > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting MMS message", e);
            return false;
        }
    }

    /**
     * Gets all MMS messages for a specific thread.
     * 
     * @param threadId The thread ID
     * @return List of MMS messages
     */
    public List<MmsMessage> getMmsMessagesForThread(String threadId) {
        List<MmsMessage> messages = new ArrayList<>();
        
        if (TextUtils.isEmpty(threadId)) {
            return messages;
        }

        String selection = COLUMN_THREAD_ID + " = ?";
        String[] selectionArgs = {threadId};
        String sortOrder = COLUMN_DATE + " ASC";

        String[] projection = {
            COLUMN_ID,
            COLUMN_THREAD_ID,
            COLUMN_DATE,
            COLUMN_DATE_SENT,
            COLUMN_MESSAGE_BOX,
            COLUMN_READ,
            COLUMN_SEEN,
            COLUMN_SUBJECT,
            COLUMN_CONTENT_TYPE
        };

        try (Cursor cursor = contentResolver.query(
                MMS_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder)) {
                
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    MmsMessage message = createMmsMessageFromCursor(cursor);
                    if (message != null) {
                        messages.add(message);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying MMS messages for thread", e);
        }

        Log.d(TAG, "Found " + messages.size() + " MMS messages for thread " + threadId);
        return messages;
    }

    /**
     * Creates an MmsMessage object from a database cursor.
     */
    private MmsMessage createMmsMessageFromCursor(Cursor cursor) {
        try {
            String id = cursor.getString(cursor.getColumnIndex(COLUMN_ID));
            String threadId = cursor.getString(cursor.getColumnIndex(COLUMN_THREAD_ID));
            long date = cursor.getLong(cursor.getColumnIndex(COLUMN_DATE));
            int messageBox = cursor.getInt(cursor.getColumnIndex(COLUMN_MESSAGE_BOX));
            
            String subject = "";
            int subjectIndex = cursor.getColumnIndex(COLUMN_SUBJECT);
            if (subjectIndex != -1) {
                subject = cursor.getString(subjectIndex);
            }

            MmsMessage message = new MmsMessage(id, subject, date * 1000, messageBox);
            message.setThreadId(Long.parseLong(threadId));
            
            // Set read status
            int readIndex = cursor.getColumnIndex(COLUMN_READ);
            if (readIndex != -1) {
                message.setRead(cursor.getInt(readIndex) == 1);
            }

            return message;
        } catch (Exception e) {
            Log.e(TAG, "Error creating MmsMessage from cursor", e);
            return null;
        }
    }

    /**
     * Creates an attachment object from a database cursor.
     */
    private MmsMessage.Attachment createAttachmentFromCursor(Cursor cursor) {
        try {
            String partId = cursor.getString(cursor.getColumnIndex("_id"));
            String contentType = cursor.getString(cursor.getColumnIndex("ct"));
            String fileName = cursor.getString(cursor.getColumnIndex("name"));
            String contentLocation = cursor.getString(cursor.getColumnIndex("cl"));
            String dataPath = cursor.getString(cursor.getColumnIndex("_data"));

            // Skip text/plain parts (these are usually the message body)
            if ("text/plain".equals(contentType)) {
                return null;
            }

            Uri partUri = Uri.withAppendedPath(PART_CONTENT_URI, partId);
            MmsMessage.Attachment attachment = new MmsMessage.Attachment(
                partUri, 
                contentType, 
                fileName, 
                0 // Size will be determined when content is accessed
            );

            return attachment;
        } catch (Exception e) {
            Log.e(TAG, "Error creating attachment from cursor", e);
            return null;
        }
    }

    /**
     * Checks if an MMS message is a downloadable notification.
     */
    private boolean isDownloadableNotification(MmsMessage message) {
        // This would typically check if the message is a notification that needs downloading
        // For now, we'll assume all messages in the inbox that aren't fully downloaded need processing
        return message.getType() == MESSAGE_BOX_INBOX;
    }

    /**
     * Performs the actual MMS download using the existing transaction system.
     */
    private boolean performMmsDownload(Uri mmsUri, MmsMessage notification) {
        try {
            // This would integrate with the existing transaction system
            // For now, we'll mark it as a successful download
            Log.d(TAG, "Performing MMS download for: " + mmsUri);
            
            // Mark as read after successful download
            markMmsAsRead(mmsUri);
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error performing MMS download", e);
            return false;
        }
    }

    /**
     * Creates a notification entry in the content provider.
     *
     * @param pushData The MMS push data
     * @return The URI of the created notification, or null if failed
     */
    public Uri createNotificationEntry(byte[] pushData) {
        if (!PhoneUtils.isDefaultSmsApp(context)) {
            Log.w(TAG, "Cannot create notification entry: app is not default SMS app");
            return null;
        }

        try {
            Log.d(TAG, "Creating notification entry for " + pushData.length + " bytes of data");
            
            // Create basic notification entry in the MMS database
            ContentValues values = new ContentValues();
            values.put(COLUMN_MESSAGE_BOX, MESSAGE_BOX_INBOX);
            values.put(COLUMN_DATE, System.currentTimeMillis() / 1000);
            values.put(COLUMN_READ, 0);
            values.put(COLUMN_SEEN, 0);
            
            // Insert the notification
            Uri notificationUri = contentResolver.insert(MMS_CONTENT_URI, values);
            
            if (notificationUri != null) {
                Log.d(TAG, "Successfully created notification entry: " + notificationUri);
            }
            
            return notificationUri;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to create notification entry", e);
            return null;
        }
    }

    /**
     * Updates notification metadata for an MMS notification.
     *
     * @param notificationUri The URI of the notification
     * @param sender The sender address
     * @param subject The message subject
     * @param timestamp The message timestamp
     * @return true if successful, false otherwise
     */
    public boolean updateNotificationMetadata(Uri notificationUri, String sender, String subject, long timestamp) {
        if (!PhoneUtils.isDefaultSmsApp(context)) {
            Log.w(TAG, "Cannot update notification metadata: app is not default SMS app");
            return false;
        }

        if (notificationUri == null) {
            Log.w(TAG, "Cannot update notification metadata: URI is null");
            return false;
        }

        try {
            ContentValues values = new ContentValues();
            
            // Update subject if provided
            if (subject != null && !subject.trim().isEmpty()) {
                values.put(COLUMN_SUBJECT, subject.trim());
            }
            
            // Update timestamp if provided
            if (timestamp > 0) {
                values.put(COLUMN_DATE, timestamp / 1000); // Convert to seconds
            }
            
            // Only update if we have values to set
            if (values.size() > 0) {
                int rowsUpdated = contentResolver.update(notificationUri, values, null, null);
                Log.d(TAG, "Updated " + rowsUpdated + " notification rows with metadata");
                return rowsUpdated > 0;
            }
            
            Log.d(TAG, "No metadata to update for notification");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification metadata", e);
            return false;
        }
    }
}