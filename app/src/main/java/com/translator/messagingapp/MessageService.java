package com.translator.messagingapp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import java.io.OutputStream;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service class that handles all SMS and MMS related operations.
 * Consolidates functionality from SmsSender, SmsReceiver, and MessageLoader.
 */
public class MessageService {
    private static final String TAG = "MessageService";
    private static final int MAX_SMS_LENGTH = 160;
    private static final int MAX_MMS_SIZE = 1024 * 1024; // 1MB max size

    // Define missing MMS constants
    private static final int MESSAGE_TYPE_SEND_REQ = 128;
    private static final int TYPE_TO = 151;

    private final Context context;
    private final ExecutorService executorService;
    private final TranslationManager translationManager;
    private final TranslationCache translationCache;

    /**
     * Creates a new MessageService.
     *
     * @param context The application context
     */
    public MessageService(Context context, TranslationManager translationManager, TranslationCache translationCache) {
        this.context = context;
        this.executorService = Executors.newFixedThreadPool(2);
        this.translationManager = translationManager;
        this.translationCache = translationCache;
    }

    /**
     * Alternative constructor for backward compatibility
     */
    public MessageService(Context context, TranslationManager translationManager) {
        this(context, translationManager, new TranslationCache(context));
    }

    /**
     * Loads all conversations from the device.
     *
     * @return A list of conversations
     */
    public List<Conversation> loadConversations() {
        List<Conversation> conversations = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();

        // Query the content provider for conversations
        Uri uri = Uri.parse("content://mms-sms/conversations?simple=true");
        String[] projection = new String[] {
                Telephony.Threads._ID,
                Telephony.Threads.DATE,
                Telephony.Threads.MESSAGE_COUNT,
                Telephony.Threads.RECIPIENT_IDS,
                Telephony.Threads.SNIPPET,
                Telephony.Threads.READ,
                Telephony.Threads.TYPE
        };
        String sortOrder = Telephony.Threads.DATE + " DESC";

        try (Cursor cursor = contentResolver.query(uri, projection, null, null, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String threadId = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Threads._ID));
                    Conversation conversation = loadConversationDetails(threadId);
                    if (conversation != null) {
                        conversations.add(conversation);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading conversations", e);
        }

        return conversations;
    }

    /**
     * Loads details for a specific conversation.
     *
     * @param threadId The thread ID of the conversation
     * @return The conversation details
     */
    private Conversation loadConversationDetails(String threadId) {
        ContentResolver contentResolver = context.getContentResolver();

        // Query the content provider for the latest message in this thread
        Uri uri = Uri.parse("content://sms/conversations/" + threadId);
        String sortOrder = "date DESC";

        try (Cursor cursor = contentResolver.query(uri, null, null, null, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                // Get the address (phone number)
                String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));

                // Get the latest message details
                String snippet = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                long date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                boolean read = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1;

                // Create the conversation object
                Conversation conversation = new Conversation();
                conversation.setThreadId(threadId);
                conversation.setAddress(address);
                conversation.setSnippet(snippet);
                conversation.setDate(date);
                conversation.setRead(read);

                // Count unread messages
                int unreadCount = countUnreadMessages(threadId);
                conversation.setUnreadCount(unreadCount);

                return conversation;
            } else {
                // If no SMS messages, try MMS
                return loadMmsConversationDetails(threadId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading conversation details for thread " + threadId, e);
        }

        return null;
    }

    /**
     * Loads MMS conversation details.
     *
     * @param threadId The thread ID of the conversation
     * @return The conversation details
     */
    private Conversation loadMmsConversationDetails(String threadId) {
        ContentResolver contentResolver = context.getContentResolver();

        // Query the content provider for the latest MMS in this thread
        Uri uri = Uri.parse("content://mms/conversations/" + threadId);
        String sortOrder = "date DESC";

        try (Cursor cursor = contentResolver.query(uri, null, null, null, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                // Get the MMS ID
                String id = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Mms._ID));

                // Get the address (phone number)
                String address = getMmsAddress(contentResolver, id, Telephony.Mms.MESSAGE_BOX_INBOX);

                // Get the latest message details
                String snippet = getMmsText(contentResolver, id);
                long date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Mms.DATE)) * 1000; // Convert to milliseconds
                boolean read = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Mms.READ)) == 1;

                // Create the conversation object
                Conversation conversation = new Conversation();
                conversation.setThreadId(threadId);
                conversation.setAddress(address);
                conversation.setSnippet(snippet != null ? snippet : "[MMS]");
                conversation.setDate(date);
                conversation.setRead(read);

                // Count unread messages
                int unreadCount = countUnreadMessages(threadId);
                conversation.setUnreadCount(unreadCount);

                return conversation;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading MMS conversation details for thread " + threadId, e);
        }

        return null;
    }

    /**
     * Gets the address (phone number) from an MMS message.
     *
     * @param contentResolver The content resolver
     * @param messageId The MMS message ID
     * @param messageBox The message box type
     * @return The address
     */
    public String getMmsAddress(ContentResolver contentResolver, String messageId, int messageBox) {
        String address = null;

        // Query the addr table to get the address
        Uri uri = Uri.parse("content://mms/" + messageId + "/addr");
        String selection = "type=" + TYPE_TO;

        try (Cursor cursor = contentResolver.query(uri, null, selection, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting MMS address for message " + messageId, e);
        }

        return address;
    }

    /**
     * Gets the text content from an MMS message.
     *
     * @param contentResolver The content resolver
     * @param messageId The MMS message ID
     * @return The text content
     */
    public String getMmsText(ContentResolver contentResolver, String messageId) {
        String text = null;

        // Query the part table to get the text parts
        Uri uri = Uri.parse("content://mms/" + messageId + "/part");

        try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String contentType = cursor.getString(cursor.getColumnIndexOrThrow("ct"));
                    if (contentType != null && contentType.startsWith("text/plain")) {
                        String data = cursor.getString(cursor.getColumnIndexOrThrow("_data"));
                        if (data != null) {
                            // Text is stored in a file
                            text = getMmsTextFromFile(contentResolver, data);
                        } else {
                            // Text is stored directly in the table
                            text = cursor.getString(cursor.getColumnIndexOrThrow("text"));
                        }
                        break;
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting MMS text for message " + messageId, e);
        }

        return text;
    }

    /**
     * Gets text content from an MMS file.
     *
     * @param contentResolver The content resolver
     * @param dataPath The path to the file
     * @return The text content
     */
    private String getMmsTextFromFile(ContentResolver contentResolver, String dataPath) {
        Uri uri = Uri.parse("content://mms/part/" + dataPath);
        String text = null;

        try (InputStream is = contentResolver.openInputStream(uri)) {
            if (is != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                text = new String(baos.toByteArray());
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading MMS text from file", e);
        }

        return text;
    }

    /**
     * Counts the number of unread messages in a thread.
     *
     * @param threadId The thread ID
     * @return The number of unread messages
     */
    private int countUnreadMessages(String threadId) {
        int unreadCount = 0;
        ContentResolver contentResolver = context.getContentResolver();

        // Count unread SMS messages
        Uri smsUri = Uri.parse("content://sms/inbox");
        String smsSelection = "thread_id = ? AND read = 0";
        String[] smsSelectionArgs = new String[] { threadId };

        try (Cursor cursor = contentResolver.query(smsUri, null, smsSelection, smsSelectionArgs, null)) {
            if (cursor != null) {
                unreadCount += cursor.getCount();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error counting unread SMS messages", e);
        }

        // Count unread MMS messages
        Uri mmsUri = Uri.parse("content://mms/inbox");
        String mmsSelection = "thread_id = ? AND read = 0";
        String[] mmsSelectionArgs = new String[] { threadId };

        try (Cursor cursor = contentResolver.query(mmsUri, null, mmsSelection, mmsSelectionArgs, null)) {
            if (cursor != null) {
                unreadCount += cursor.getCount();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error counting unread MMS messages", e);
        }

        return unreadCount;
    }

    /**
     * Loads messages for a specific thread.
     *
     * @param threadId The thread ID
     * @return A list of messages
     */
    public List<Message> loadMessages(String threadId) {
        Log.d(TAG, "loadMessages called with threadId: " + threadId);
        
        if (threadId == null || threadId.trim().isEmpty()) {
            Log.e(TAG, "threadId is null or empty, cannot load messages");
            return new ArrayList<>();
        }
        
        // First, let's validate that the SMS content provider is accessible
        if (!isSmsContentProviderAccessible()) {
            Log.e(TAG, "SMS content provider is not accessible - check permissions");
            return new ArrayList<>();
        }
        
        List<Message> messages = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();

        // Load SMS messages
        loadSmsMessages(contentResolver, threadId, messages);
        Log.d(TAG, "After loading SMS messages, total count: " + messages.size());

        // Load MMS messages
        loadMmsMessages(contentResolver, threadId, messages);
        Log.d(TAG, "After loading MMS messages, total count: " + messages.size());

        // Sort by date (newest first)
        Collections.sort(messages, (m1, m2) -> Long.compare(m2.getDate(), m1.getDate()));

        Log.d(TAG, "Returning " + messages.size() + " messages for threadId: " + threadId);
        return messages;
    }

    /**
     * Check if the SMS content provider is accessible
     * @return true if accessible, false otherwise
     */
    private boolean isSmsContentProviderAccessible() {
        try {
            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = Uri.parse("content://sms");
            
            // Try a simple query to check accessibility
            try (Cursor cursor = contentResolver.query(uri, new String[]{"_id"}, null, null, "_id LIMIT 1")) {
                boolean accessible = cursor != null;
                Log.d(TAG, "SMS content provider accessible: " + accessible);
                return accessible;
            }
        } catch (Exception e) {
            Log.e(TAG, "SMS content provider not accessible", e);
            return false;
        }
    }

    /**
     * Loads SMS messages for a thread.
     *
     * @param contentResolver The content resolver
     * @param threadId The thread ID
     * @param messages The list to add messages to
     */
    private void loadSmsMessages(ContentResolver contentResolver, String threadId, List<Message> messages) {
        Log.d(TAG, "loadSmsMessages called for threadId: " + threadId);
        
        // First, let's check if there are any SMS messages at all
        int totalSmsCount = getTotalSmsCount(contentResolver);
        Log.d(TAG, "Total SMS messages in database: " + totalSmsCount);
        
        Uri uri = Uri.parse("content://sms");
        String selection = "thread_id = ?";
        String[] selectionArgs = new String[] { threadId };
        String sortOrder = "date DESC";

        Log.d(TAG, "Querying SMS with URI: " + uri + ", selection: " + selection + ", args: [" + threadId + "]");

        try (Cursor cursor = contentResolver.query(uri, null, selection, selectionArgs, sortOrder)) {
            if (cursor == null) {
                Log.e(TAG, "SMS cursor is null for threadId: " + threadId);
                return;
            }
            
            Log.d(TAG, "SMS cursor count: " + cursor.getCount() + " for threadId: " + threadId);
            
            if (cursor.moveToFirst()) {
                int messageCount = 0;
                do {
                    try {
                        String id = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms._ID));
                        String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                        long date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                        int type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE));
                        String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                        boolean read = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1;

                        Message message = new Message();
                        message.setId(Long.parseLong(id));
                        message.setBody(body);
                        message.setDate(date);
                        message.setType(type);
                        message.setAddress(address);
                        message.setRead(read);
                        message.setThreadId(Long.parseLong(threadId));
                        message.setMessageType(Message.MESSAGE_TYPE_SMS);

                        messages.add(message);
                        messageCount++;
                        
                        if (messageCount <= 3) { // Log details for first few messages
                            Log.d(TAG, "Added SMS message " + messageCount + ": id=" + id + ", body=" + 
                                    (body != null ? body.substring(0, Math.min(body.length(), 50)) + "..." : "null") +
                                    ", date=" + date + ", type=" + type);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing SMS message in cursor", e);
                    }
                } while (cursor.moveToNext());
                
                Log.d(TAG, "Successfully added " + messageCount + " SMS messages for threadId: " + threadId);
            } else {
                Log.w(TAG, "No SMS messages found for threadId: " + threadId);
                
                // Let's also check if this thread exists at all
                checkThreadExists(contentResolver, threadId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading SMS messages for thread " + threadId, e);
        }
    }

    /**
     * Get the total count of SMS messages in the database
     * @param contentResolver The content resolver
     * @return The total count
     */
    private int getTotalSmsCount(ContentResolver contentResolver) {
        try (Cursor cursor = contentResolver.query(Uri.parse("content://sms"), 
                new String[]{"COUNT(*)"}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting total SMS count", e);
        }
        return 0;
    }

    /**
     * Check if a thread exists in the conversations table
     * @param contentResolver The content resolver
     * @param threadId The thread ID to check
     */
    private void checkThreadExists(ContentResolver contentResolver, String threadId) {
        try {
            Uri uri = Uri.parse("content://mms-sms/conversations");
            String selection = "_id = ?";
            String[] selectionArgs = new String[] { threadId };
            
            try (Cursor cursor = contentResolver.query(uri, null, selection, selectionArgs, null)) {
                if (cursor != null) {
                    boolean threadExists = cursor.getCount() > 0;
                    Log.d(TAG, "Thread " + threadId + " exists in conversations table: " + threadExists);
                    if (cursor.moveToFirst()) {
                        int messageCount = cursor.getInt(cursor.getColumnIndexOrThrow("message_count"));
                        Log.d(TAG, "Thread " + threadId + " has message_count: " + messageCount);
                    }
                } else {
                    Log.w(TAG, "Could not query conversations table for thread " + threadId);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking thread existence for " + threadId, e);
        }
    }

    /**
     * Loads MMS messages for a thread.
     *
     * @param contentResolver The content resolver
     * @param threadId The thread ID
     * @param messages The list to add messages to
     */
    private void loadMmsMessages(ContentResolver contentResolver, String threadId, List<Message> messages) {
        Uri uri = Uri.parse("content://mms");
        String selection = "thread_id = ?";
        String[] selectionArgs = new String[] { threadId };
        String sortOrder = "date DESC";

        try (Cursor cursor = contentResolver.query(uri, null, selection, selectionArgs, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Mms._ID));
                    long date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Mms.DATE)) * 1000; // Convert to milliseconds
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX));
                    boolean read = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Mms.READ)) == 1;

                    // Get the address and text content
                    String address = getMmsAddress(contentResolver, id, type);
                    String body = getMmsText(contentResolver, id);

                    // Create the message
                    MmsMessage message = new MmsMessage(id, body, date, type);
                    message.setAddress(address);
                    message.setRead(read);
                    message.setThreadId(Long.parseLong(threadId));

                    messages.add(message);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading MMS messages for thread " + threadId, e);
        }
    }

    /**
     * Sends an SMS message.
     *
     * @param address The recipient address
     * @param body The message body
     * @return True if the message was sent successfully
     */
    /**
     * Sends an SMS message.
     *
     * @param address The recipient address
     * @param body The message body
     * @return True if the message was sent successfully
     */
    public boolean sendSmsMessage(String address, String body) {
        return sendSmsMessage(address, body, null, null);
    }

    /**
     * Sends an SMS message with additional parameters.
     *
     * @param address The recipient address
     * @param body The message body
     * @param threadId The thread ID (can be null)
     * @param callback Callback to be executed after sending (can be null)
     * @return True if the message was sent successfully
     */
    public boolean sendSmsMessage(String address, String body, String threadId, Runnable callback) {
        try {
            SmsManager smsManager = SmsManager.getDefault();

            if (body.length() > MAX_SMS_LENGTH) {
                // Split the message into parts
                ArrayList<String> parts = smsManager.divideMessage(body);
                smsManager.sendMultipartTextMessage(address, null, parts, null, null);
            } else {
                // Send a single message
                smsManager.sendTextMessage(address, null, body, null, null);
            }

            // Execute callback if provided
            if (callback != null) {
                callback.run();
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS message", e);
            return false;
        }
    }

    /**
     * Sends an MMS message.
     *
     * @param address The recipient address
     * @param subject The message subject
     * @param body The message body
     * @param attachments The attachments
     * @return True if the message was sent successfully
     */
    public boolean sendMmsMessage(String address, String subject, String body, List<Uri> attachments) {
        try {
            // Create a new MMS message
            ContentValues values = new ContentValues();
            values.put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_OUTBOX);
            values.put(Telephony.Mms.MESSAGE_TYPE, MESSAGE_TYPE_SEND_REQ);
            values.put(Telephony.Mms.SUBJECT, subject);
            values.put(Telephony.Mms.CONTENT_TYPE, "application/vnd.wap.multipart.related");
            values.put(Telephony.Mms.DATE, System.currentTimeMillis() / 1000);
            values.put(Telephony.Mms.READ, 1);
            values.put(Telephony.Mms.SEEN, 1);

            // Insert the message
            Uri messageUri = context.getContentResolver().insert(Uri.parse("content://mms"), values);
            if (messageUri == null) {
                Log.e(TAG, "Failed to insert MMS message");
                return false;
            }

            // Get the message ID
            String messageId = messageUri.getLastPathSegment();

            // Add the recipient
            ContentValues addrValues = new ContentValues();
            addrValues.put(Telephony.Mms.Addr.ADDRESS, address);
            addrValues.put(Telephony.Mms.Addr.TYPE, TYPE_TO);
            addrValues.put(Telephony.Mms.Addr.CHARSET, "106");
            Uri addrUri = Uri.parse("content://mms/" + messageId + "/addr");
            context.getContentResolver().insert(addrUri, addrValues);

            // Add the text part
            if (!TextUtils.isEmpty(body)) {
                ContentValues textValues = new ContentValues();
                textValues.put(Telephony.Mms.Part.MSG_ID, messageId);
                textValues.put(Telephony.Mms.Part.CONTENT_TYPE, "text/plain");
                textValues.put(Telephony.Mms.Part.CHARSET, "106");
                textValues.put(Telephony.Mms.Part.CONTENT_DISPOSITION, "inline");
                textValues.put(Telephony.Mms.Part.FILENAME, "text.txt");
                textValues.put(Telephony.Mms.Part.NAME, "text.txt");
                textValues.put(Telephony.Mms.Part.TEXT, body);

                Uri partUri = Uri.parse("content://mms/" + messageId + "/part");
                context.getContentResolver().insert(partUri, textValues);
            }

            // Add attachments
            if (attachments != null && !attachments.isEmpty()) {
                for (Uri attachmentUri : attachments) {
                    try {
                        // Get the attachment details
                        String mimeType = context.getContentResolver().getType(attachmentUri);
                        if (mimeType == null) {
                            mimeType = "application/octet-stream";
                        }

                        // Get the file name
                        String fileName = "attachment";
                        String[] projection = {android.provider.MediaStore.MediaColumns.DISPLAY_NAME};
                        try (Cursor cursor = context.getContentResolver().query(attachmentUri, projection, null, null, null)) {
                            if (cursor != null && cursor.moveToFirst()) {
                                fileName = cursor.getString(0);
                            }
                        }

                        // Create the part
                        ContentValues partValues = new ContentValues();
                        partValues.put(Telephony.Mms.Part.MSG_ID, messageId);
                        partValues.put(Telephony.Mms.Part.CONTENT_TYPE, mimeType);
                        partValues.put(Telephony.Mms.Part.FILENAME, fileName);
                        partValues.put(Telephony.Mms.Part.NAME, fileName);
                        partValues.put(Telephony.Mms.Part.CONTENT_DISPOSITION, "attachment");

                        // Insert the part
                        Uri partUri = Uri.parse("content://mms/" + messageId + "/part");
                        Uri newPartUri = context.getContentResolver().insert(partUri, partValues);

                        // Copy the attachment data
                        if (newPartUri != null) {
                            try (InputStream is = context.getContentResolver().openInputStream(attachmentUri);
                                 OutputStream os = context.getContentResolver().openOutputStream(newPartUri)) {
                                if (is != null && os != null) {
                                    byte[] buffer = new byte[1024];
                                    int len;
                                    while ((len = is.read(buffer)) != -1) {
                                        os.write(buffer, 0, len);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error adding attachment to MMS", e);
                    }
                }
            }

            // Move the message to the outbox
            ContentValues outboxValues = new ContentValues();
            outboxValues.put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_OUTBOX);
            context.getContentResolver().update(messageUri, outboxValues, null, null);

            // Request to send the message
            Intent intent = new Intent("android.provider.Telephony.MMS_SENT");
            context.sendBroadcast(intent);

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sending MMS message", e);
            return false;
        }
    }

    /**
     * Adds a test message for debugging purposes.
     *
     * @return True if the message was added successfully
     */
    public boolean addTestMessage() {
        try {
            ContentValues values = new ContentValues();
            values.put(Telephony.Sms.ADDRESS, "123456789");
            values.put(Telephony.Sms.BODY, "This is a test message");
            values.put(Telephony.Sms.DATE, System.currentTimeMillis());
            values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX);
            values.put(Telephony.Sms.READ, 0);

            Uri uri = context.getContentResolver().insert(Uri.parse("content://sms/inbox"), values);
            return uri != null;
        } catch (Exception e) {
            Log.e(TAG, "Error adding test message", e);
            return false;
        }
    }

    /**
     * Deletes a conversation.
     *
     * @param threadId The thread ID of the conversation to delete
     * @return True if the conversation was deleted successfully
     */
    public boolean deleteConversation(String threadId) {
        try {
            // Delete SMS messages
            Uri smsUri = Uri.parse("content://sms");
            String smsSelection = "thread_id = ?";
            String[] smsSelectionArgs = new String[] { threadId };
            int smsDeleted = context.getContentResolver().delete(smsUri, smsSelection, smsSelectionArgs);

            // Delete MMS messages
            Uri mmsUri = Uri.parse("content://mms");
            String mmsSelection = "thread_id = ?";
            String[] mmsSelectionArgs = new String[] { threadId };
            int mmsDeleted = context.getContentResolver().delete(mmsUri, mmsSelection, mmsSelectionArgs);

            // Delete the thread
            Uri threadUri = Uri.parse("content://mms-sms/conversations/" + threadId);
            int threadDeleted = context.getContentResolver().delete(threadUri, null, null);

            Log.d(TAG, "Deleted conversation " + threadId + ": " + smsDeleted + " SMS, " + mmsDeleted + " MMS, thread: " + threadDeleted);

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting conversation " + threadId, e);
            return false;
        }
    }

    /**
     * Marks a thread as read.
     *
     * @param threadId The thread ID to mark as read
     * @return True if the thread was marked as read successfully
     */
    public boolean markThreadAsRead(String threadId) {
        try {
            // Mark SMS messages as read
            ContentValues smsValues = new ContentValues();
            smsValues.put(Telephony.Sms.READ, 1);

            Uri smsUri = Uri.parse("content://sms");
            String smsSelection = "thread_id = ? AND read = 0";
            String[] smsSelectionArgs = new String[] { threadId };
            int smsUpdated = context.getContentResolver().update(smsUri, smsValues, smsSelection, smsSelectionArgs);

            // Mark MMS messages as read
            ContentValues mmsValues = new ContentValues();
            mmsValues.put(Telephony.Mms.READ, 1);

            Uri mmsUri = Uri.parse("content://mms");
            String mmsSelection = "thread_id = ? AND read = 0";
            String[] mmsSelectionArgs = new String[] { threadId };
            int mmsUpdated = context.getContentResolver().update(mmsUri, mmsValues, mmsSelection, mmsSelectionArgs);

            Log.d(TAG, "Marked thread " + threadId + " as read: " + smsUpdated + " SMS, " + mmsUpdated + " MMS");

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error marking thread " + threadId + " as read", e);
            return false;
        }
    }

    /**
     * Searches for messages containing the specified query text.
     *
     * @param query The search query
     * @return A list of messages matching the query
     */
    public List<Message> searchMessages(String query) {
        List<Message> results = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            Log.e(TAG, "Cannot search with empty query");
            return results;
        }

        // Normalize the query for case-insensitive search
        String normalizedQuery = query.toLowerCase().trim();

        ContentResolver contentResolver = context.getContentResolver();

        try {
            // Search SMS messages
            searchSmsMessages(contentResolver, normalizedQuery, results);

            // Search MMS messages
            searchMmsMessages(contentResolver, normalizedQuery, results);

            // Sort results by date (newest first)
            if (!results.isEmpty()) {
                results.sort((m1, m2) -> Long.compare(m2.getDate(), m1.getDate()));
            }

            Log.d(TAG, "Found " + results.size() + " messages matching query: " + query);

        } catch (Exception e) {
            Log.e(TAG, "Error searching messages", e);
        }

        return results;
    }

    /**
     * Gets messages by address (phone number).
     *
     * @param address The address to filter by
     * @return A list of messages for the specified address
     */
    public List<Message> getMessagesByAddress(String address) {
        if (address == null || address.isEmpty()) {
            return new ArrayList<>();
        }

        List<Message> messages = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();

        // Get SMS messages for this address
        Uri uri = Uri.parse("content://sms");
        String selection = "address = ?";
        String[] selectionArgs = new String[] { address };

        try (Cursor cursor = contentResolver.query(uri, null, selection, selectionArgs, "date DESC")) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms._ID));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    long date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE));
                    boolean read = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1;
                    String threadId = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID));

                    Message message = new Message();
                    message.setId(Long.parseLong(id));
                    message.setBody(body);
                    message.setDate(date);
                    message.setType(type);
                    message.setRead(read);
                    message.setAddress(address);
                    message.setThreadId(Long.parseLong(threadId));
                    message.setMessageType(Message.MESSAGE_TYPE_SMS);

                    messages.add(message);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting SMS messages by address", e);
        }

        // Get MMS messages for this address (simplified implementation)
        try {
            String threadId = getThreadIdForAddress(address);
            if (threadId != null) {
                uri = Uri.parse("content://mms");
                selection = "thread_id = ?";
                selectionArgs = new String[] { threadId };

                try (Cursor cursor = contentResolver.query(uri, null, selection, selectionArgs, "date DESC")) {
                    if (cursor != null && cursor.moveToFirst()) {
                        do {
                            String id = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Mms._ID));
                            long date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Mms.DATE)) * 1000; // Convert to milliseconds
                            int type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX));
                            boolean read = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Mms.READ)) == 1;

                            // Get MMS body
                            String body = getMmsText(contentResolver, id);

                            MmsMessage message = new MmsMessage(id, body, date, type);
                            message.setRead(read);
                            message.setAddress(address);
                            message.setThreadId(Long.parseLong(threadId));

                            messages.add(message);
                        } while (cursor.moveToNext());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting MMS messages by address", e);
        }

        // Sort by date
        Collections.sort(messages, (m1, m2) -> Long.compare(m2.getDate(), m1.getDate()));

        return messages;
    }

    /**
     * Searches for SMS messages containing the specified query text.
     *
     * @param contentResolver The content resolver
     * @param query The search query (normalized)
     * @param results The list to add results to
     */
    private void searchSmsMessages(ContentResolver contentResolver, String query, List<Message> results) {
        Cursor cursor = null;
        try {
            // Query the SMS content provider
            Uri uri = Uri.parse("content://sms");
            String selection = "body LIKE ?";
            String[] selectionArgs = {"%" + query + "%"};
            String sortOrder = "date DESC";

            cursor = contentResolver.query(
                    uri,
                    null,
                    selection,
                    selectionArgs,
                    sortOrder
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int idIndex = cursor.getColumnIndex(Telephony.Sms._ID);
                    int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
                    int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);
                    int typeIndex = cursor.getColumnIndex(Telephony.Sms.TYPE);
                    int addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                    int readIndex = cursor.getColumnIndex(Telephony.Sms.READ);
                    int threadIdIndex = cursor.getColumnIndex(Telephony.Sms.THREAD_ID);

                    // Skip if required columns are missing
                    if (idIndex < 0 || bodyIndex < 0 || dateIndex < 0 || typeIndex < 0) {
                        Log.w(TAG, "Required column missing in SMS cursor");
                        continue;
                    }

                    // Extract message data
                    String id = cursor.getString(idIndex);
                    String body = cursor.getString(bodyIndex);
                    long date = cursor.getLong(dateIndex);
                    int type = cursor.getInt(typeIndex);
                    String address = addressIndex >= 0 ? cursor.getString(addressIndex) : "";
                    boolean read = readIndex >= 0 && cursor.getInt(readIndex) > 0;
                    String threadId = threadIdIndex >= 0 ? cursor.getString(threadIdIndex) : "";

                    // Create message object
                    Message message = new Message();
                    message.setId(Long.parseLong(id));
                    message.setBody(body);
                    message.setDate(date);
                    message.setType(type);
                    message.setAddress(address);
                    message.setRead(read);
                    message.setThreadId(Long.parseLong(threadId));
                    message.setMessageType(Message.MESSAGE_TYPE_SMS);

                    // Add search metadata
                    message.setSearchQuery(query);

                    // Add to results
                    results.add(message);

                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Searches for MMS messages containing the specified query text.
     *
     * @param contentResolver The content resolver
     * @param query The search query (normalized)
     * @param results The list to add results to
     */
    private void searchMmsMessages(ContentResolver contentResolver, String query, List<Message> results) {
        Cursor cursor = null;
        try {
            // Query the MMS content provider
            Uri uri = Uri.parse("content://mms");
            String sortOrder = "date DESC";

            cursor = contentResolver.query(
                    uri,
                    null,
                    null,
                    null,
                    sortOrder
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int idIndex = cursor.getColumnIndex(Telephony.Mms._ID);
                    int dateIndex = cursor.getColumnIndex(Telephony.Mms.DATE);
                    int typeIndex = cursor.getColumnIndex(Telephony.Mms.MESSAGE_BOX);
                    int readIndex = cursor.getColumnIndex(Telephony.Mms.READ);
                    int threadIdIndex = cursor.getColumnIndex(Telephony.Mms.THREAD_ID);

                    // Skip if required columns are missing
                    if (idIndex < 0 || dateIndex < 0 || typeIndex < 0) {
                        Log.w(TAG, "Required column missing in MMS cursor");
                        continue;
                    }

                    // Extract message data
                    String id = cursor.getString(idIndex);
                    long date = cursor.getLong(dateIndex) * 1000; // MMS date is in seconds, convert to milliseconds
                    int type = cursor.getInt(typeIndex);
                    boolean read = readIndex >= 0 && cursor.getInt(readIndex) > 0;
                    String threadId = threadIdIndex >= 0 ? cursor.getString(threadIdIndex) : "";

                    // Get MMS body and address
                    String body = getMmsText(contentResolver, id);
                    String address = getMmsAddress(contentResolver, id, type);

                    // Skip if body doesn't contain search query
                    if (body == null || !body.toLowerCase().contains(query)) {
                        continue;
                    }

                    // Create message object
                    MmsMessage message = new MmsMessage(id, body, date, type);
                    message.setRead(read);
                    message.setAddress(address);
                    message.setThreadId(Long.parseLong(threadId));
                    message.setMessageType(Message.MESSAGE_TYPE_MMS);

                    // Add search metadata
                    message.setSearchQuery(query);

                    // Add to results
                    results.add(message);

                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Gets the thread ID for a specific address.
     *
     * @param address The address to look up
     * @return The thread ID, or null if not found
     */
    public String getThreadIdForAddress(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }

        Uri uri = Uri.parse("content://sms/inbox");
        String[] projection = new String[] { "thread_id" };
        String selection = "address = ?";
        String[] selectionArgs = new String[] { address };

        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting thread ID for address: " + address, e);
        }

        return null;
    }

    /**
     * Handles an incoming SMS message.
     *
     * @param intent The intent containing the SMS message
     */
    public void handleIncomingSms(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            String format = bundle.getString("format");

            if (pdus != null) {
                for (Object pdu : pdus) {
                    SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
                    String address = smsMessage.getOriginatingAddress();
                    String body = smsMessage.getMessageBody();
                    long timestamp = smsMessage.getTimestampMillis();

                    // Process the message
                    Log.d(TAG, "Received SMS from " + address + ": " + body);

                    // Notify listeners
                    // TODO: Implement notification mechanism
                }
            }
        }
    }

    /**
     * Deletes a specific message.
     *
     * @param id The message ID
     * @param messageType The message type (SMS or MMS)
     * @return True if the message was deleted successfully
     */
    public boolean deleteMessage(String id, int messageType) {
        try {
            Uri uri;
            if (messageType == Message.MESSAGE_TYPE_SMS) {
                uri = Uri.parse("content://sms/" + id);
            } else if (messageType == Message.MESSAGE_TYPE_MMS) {
                uri = Uri.parse("content://mms/" + id);
            } else {
                Log.e(TAG, "Unknown message type: " + messageType);
                return false;
            }

            int deleted = context.getContentResolver().delete(uri, null, null);
            return deleted > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting message " + id, e);
            return false;
        }
    }

    /**
     * Cleans up resources used by this service.
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
