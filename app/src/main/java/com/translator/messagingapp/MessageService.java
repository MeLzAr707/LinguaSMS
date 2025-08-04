package com.translator.messagingapp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service for handling messages.
 * This is a complete implementation with all required methods.
 */
public class MessageService {
    private static final String TAG = "MessageService";
    private Context context;
    private TranslationManager translationManager;
    private TranslationCache translationCache;
    private MessageCache messageCache;

    /**
     * Creates a new MessageService.
     *
     * @param context The application context
     * @param translationManager The translation manager
     */
    public MessageService(Context context, TranslationManager translationManager) {
        this.context = context;
        this.translationManager = translationManager;
        this.translationCache = ((TranslatorApp) context.getApplicationContext()).getTranslationCache();
        this.messageCache = new MessageCache();
    }

    /**
     * Gets messages by thread ID.
     *
     * @param threadId The thread ID
     * @return The list of messages
     */
    public List<Message> getMessagesByThreadId(String threadId) {
        Log.d(TAG, "Getting messages for thread ID: " + threadId);

        if (TextUtils.isEmpty(threadId)) {
            Log.e(TAG, "Thread ID is empty");
            return new ArrayList<>();
        }

        // Check cache first
        List<Message> cachedMessages = messageCache.getMessages(threadId);
        if (cachedMessages != null && !cachedMessages.isEmpty()) {
            Log.d(TAG, "Returning " + cachedMessages.size() + " cached messages for thread ID: " + threadId);
            return cachedMessages;
        }

        List<Message> messages = new ArrayList<>();

        try {
            // Query SMS messages
            List<Message> smsMessages = getSmsMessagesByThreadId(threadId);
            if (smsMessages != null) {
                messages.addAll(smsMessages);
                Log.d(TAG, "Found " + smsMessages.size() + " SMS messages for thread " + threadId);
            }

            // Query MMS messages
            List<Message> mmsMessages = getMmsMessagesByThreadId(threadId);
            if (mmsMessages != null) {
                messages.addAll(mmsMessages);
                Log.d(TAG, "Found " + mmsMessages.size() + " MMS messages for thread " + threadId);
            }

            // Sort messages by date
            messages.sort((m1, m2) -> Long.compare(m1.getDate(), m2.getDate()));

            // Cache messages
            messageCache.cacheMessages(threadId, messages);

            Log.d(TAG, "Returning " + messages.size() + " total messages for thread ID: " + threadId);
        } catch (Exception e) {
            Log.e(TAG, "Error getting messages for thread ID: " + threadId, e);
        }

        return messages;
    }

    /**
     * Gets messages by address.
     *
     * @param address The address
     * @return The list of messages
     */
    public List<Message> getMessagesByAddress(String address) {
        Log.d(TAG, "Getting messages for address: " + address);

        if (TextUtils.isEmpty(address)) {
            Log.e(TAG, "Address is empty");
            return new ArrayList<>();
        }

        try {
            // Find thread ID for this address
            String threadId = getThreadIdForAddress(address);

            if (!TextUtils.isEmpty(threadId)) {
                // If we found a thread ID, use it to get messages
                Log.d(TAG, "Found thread ID " + threadId + " for address " + address);
                return getMessagesByThreadId(threadId);
            } else {
                // If no thread ID found, try direct query by address
                Log.d(TAG, "No thread ID found for address " + address + ", trying direct query");
                return getMessagesByAddressDirect(address);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting messages for address: " + address, e);
            return new ArrayList<>();
        }
    }

    /**
     * Gets messages by address using direct query (fallback method).
     *
     * @param address The address
     * @return The list of messages
     */
    private List<Message> getMessagesByAddressDirect(String address) {
        List<Message> messages = new ArrayList<>();
        
        try {
            // Query SMS messages directly by address
            List<Message> smsMessages = getSmsMessagesByAddress(address);
            if (smsMessages != null) {
                messages.addAll(smsMessages);
                Log.d(TAG, "Found " + smsMessages.size() + " SMS messages for address " + address);
            }

            // Query MMS messages directly by address
            List<Message> mmsMessages = getMmsMessagesByAddress(address);
            if (mmsMessages != null) {
                messages.addAll(mmsMessages);
                Log.d(TAG, "Found " + mmsMessages.size() + " MMS messages for address " + address);
            }

            // Sort messages by date
            messages.sort((m1, m2) -> Long.compare(m1.getDate(), m2.getDate()));

            Log.d(TAG, "Returning " + messages.size() + " total messages for address: " + address);
        } catch (Exception e) {
            Log.e(TAG, "Error in direct query for address: " + address, e);
        }

        return messages;
    }

    /**
     * Gets SMS messages by address directly.
     *
     * @param address The address
     * @return The list of SMS messages
     */
    private List<Message> getSmsMessagesByAddress(String address) {
        List<Message> messages = new ArrayList<>();
        Cursor cursor = null;

        try {
            // Query SMS messages by address
            cursor = context.getContentResolver().query(
                    Telephony.Sms.CONTENT_URI,
                    null,
                    Telephony.Sms.ADDRESS + " = ?",
                    new String[]{address},
                    Telephony.Sms.DATE + " ASC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    SmsMessage message = new SmsMessage();

                    // Get message data
                    String id = cursor.getString(cursor.getColumnIndex(Telephony.Sms._ID));
                    String body = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
                    String messageAddress = cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS));
                    long date = cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE));
                    int type = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.TYPE));
                    boolean read = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.READ)) == 1;
                    String threadId = cursor.getString(cursor.getColumnIndex(Telephony.Sms.THREAD_ID));

                    // Set message data
                    message.setId(id);
                    message.setBody(body);
                    message.setAddress(messageAddress);
                    message.setDate(date);
                    message.setType(type);
                    message.setRead(read);
                    message.setThreadId(threadId);
                    message.setMessageType(Message.MESSAGE_TYPE_SMS);

                    // Add message to list
                    messages.add(message);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting SMS messages for address: " + address, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return messages;
    }

    /**
     * Gets MMS messages by address directly.
     *
     * @param address The address
     * @return The list of MMS messages
     */
    private List<Message> getMmsMessagesByAddress(String address) {
        List<Message> messages = new ArrayList<>();
        Cursor cursor = null;

        try {
            // This is more complex for MMS as we need to join with the address table
            // For now, we'll do a simplified approach
            cursor = context.getContentResolver().query(
                    Telephony.Mms.CONTENT_URI,
                    null,
                    null,
                    null,
                    Telephony.Mms.DATE + " ASC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String id = cursor.getString(cursor.getColumnIndex(Telephony.Mms._ID));
                    
                    // Check if this MMS is to/from the specified address
                    String mmsAddress = getMmsAddress(context.getContentResolver(), id);
                    if (address.equals(mmsAddress)) {
                        MmsMessage message = new MmsMessage();

                        // Get message data
                        long date = cursor.getLong(cursor.getColumnIndex(Telephony.Mms.DATE)) * 1000;
                        int type = cursor.getInt(cursor.getColumnIndex(Telephony.Mms.MESSAGE_BOX));
                        boolean read = cursor.getInt(cursor.getColumnIndex(Telephony.Mms.READ)) == 1;
                        String threadId = cursor.getString(cursor.getColumnIndex(Telephony.Mms.THREAD_ID));

                        // Convert MMS message box to SMS type
                        int smsType;
                        switch (type) {
                            case Telephony.Mms.MESSAGE_BOX_INBOX:
                                smsType = Message.TYPE_INBOX;
                                break;
                            case Telephony.Mms.MESSAGE_BOX_SENT:
                                smsType = Message.TYPE_SENT;
                                break;
                            case Telephony.Mms.MESSAGE_BOX_DRAFTS:
                                smsType = Message.TYPE_DRAFT;
                                break;
                            case Telephony.Mms.MESSAGE_BOX_OUTBOX:
                                smsType = Message.TYPE_OUTBOX;
                                break;
                            default:
                                smsType = Message.TYPE_ALL;
                                break;
                        }

                        // Get MMS text
                        String body = getMmsText(context.getContentResolver(), id);

                        // Set message data
                        message.setId(id);
                        message.setBody(body);
                        message.setDate(date);
                        message.setType(smsType);
                        message.setRead(read);
                        message.setAddress(mmsAddress);
                        message.setThreadId(threadId);
                        message.setMessageType(Message.MESSAGE_TYPE_MMS);

                        // Add attachments
                        List<MmsMessage.Attachment> attachments = getMmsAttachments(context.getContentResolver(), id);
                        message.setAttachmentObjects(attachments);

                        // Add message to list
                        messages.add(message);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting MMS messages for address: " + address, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return messages;
    }

    /**
     * Gets the thread ID for an address.
     *
     * @param address The address
     * @return The thread ID, or null if not found
     */
    private String getThreadIdForAddress(String address) {
        if (TextUtils.isEmpty(address)) {
            return null;
        }

        String threadId = null;
        Cursor cursor = null;

        try {
            // Query the canonical addresses table
            Uri uri = Telephony.MmsSms.CONTENT_URI.buildUpon()
                    .appendPath("canonical-addresses")
                    .build();
            cursor = context.getContentResolver().query(
                    uri,
                    new String[]{"_id"},
                    "address = ?",
                    new String[]{address},
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                String recipientId = cursor.getString(0);
                cursor.close();

                // Query the threads table
                uri = Telephony.MmsSms.CONTENT_CONVERSATIONS_URI.buildUpon()
                        .appendQueryParameter("simple", "true")
                        .build();
                cursor = context.getContentResolver().query(
                        uri,
                        new String[]{"_id"},
                        "recipient_ids = ?",
                        new String[]{recipientId},
                        null);

                if (cursor != null && cursor.moveToFirst()) {
                    threadId = cursor.getString(0);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting thread ID for address: " + address, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return threadId;
    }

    /**
     * Gets SMS messages by thread ID.
     *
     * @param threadId The thread ID
     * @return The list of SMS messages
     */
    private List<Message> getSmsMessagesByThreadId(String threadId) {
        List<Message> messages = new ArrayList<>();
        Cursor cursor = null;

        try {
            // Query SMS messages
            cursor = context.getContentResolver().query(
                    Telephony.Sms.CONTENT_URI,
                    null,
                    Telephony.Sms.THREAD_ID + " = ?",
                    new String[]{threadId},
                    Telephony.Sms.DATE + " ASC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    SmsMessage message = new SmsMessage();

                    // Get message data
                    String id = cursor.getString(cursor.getColumnIndex(Telephony.Sms._ID));
                    String body = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
                    long date = cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE));
                    int type = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.TYPE));
                    boolean read = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.READ)) == 1;
                    String address = cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS));

                    // Set message data
                    message.setId(id);
                    message.setBody(body);
                    message.setDate(date);
                    message.setType(type);
                    message.setRead(read);
                    message.setAddress(address);
                    message.setThreadId(threadId);
                    message.setMessageType(Message.MESSAGE_TYPE_SMS);

                    // Add message to list
                    messages.add(message);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting SMS messages for thread ID: " + threadId, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return messages;
    }

    /**
     * Gets MMS messages by thread ID.
     *
     * @param threadId The thread ID
     * @return The list of MMS messages
     */
    private List<Message> getMmsMessagesByThreadId(String threadId) {
        List<Message> messages = new ArrayList<>();
        Cursor cursor = null;

        try {
            // Query MMS messages
            cursor = context.getContentResolver().query(
                    Telephony.Mms.CONTENT_URI,
                    null,
                    Telephony.Mms.THREAD_ID + " = ?",
                    new String[]{threadId},
                    Telephony.Mms.DATE + " ASC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    MmsMessage message = new MmsMessage();

                    // Get message data
                    String id = cursor.getString(cursor.getColumnIndex(Telephony.Mms._ID));
                    long date = cursor.getLong(cursor.getColumnIndex(Telephony.Mms.DATE)) * 1000; // MMS date is in seconds
                    int type = cursor.getInt(cursor.getColumnIndex(Telephony.Mms.MESSAGE_BOX));
                    boolean read = cursor.getInt(cursor.getColumnIndex(Telephony.Mms.READ)) == 1;

                    // Convert MMS message box to SMS type
                    int smsType;
                    switch (type) {
                        case Telephony.Mms.MESSAGE_BOX_INBOX:
                            smsType = Message.TYPE_INBOX;
                            break;
                        case Telephony.Mms.MESSAGE_BOX_SENT:
                            smsType = Message.TYPE_SENT;
                            break;
                        case Telephony.Mms.MESSAGE_BOX_DRAFTS:
                            smsType = Message.TYPE_DRAFT;
                            break;
                        case Telephony.Mms.MESSAGE_BOX_OUTBOX:
                            smsType = Message.TYPE_OUTBOX;
                            break;
                        default:
                            smsType = Message.TYPE_ALL;
                            break;
                    }

                    // Get MMS address
                    String address = getMmsAddress(context.getContentResolver(), id);

                    // Get MMS text
                    String body = getMmsText(context.getContentResolver(), id);

                    // Set message data
                    message.setId(id);
                    message.setBody(body);
                    message.setDate(date);
                    message.setType(smsType);
                    message.setRead(read);
                    message.setAddress(address);
                    message.setThreadId(threadId);
                    message.setMessageType(Message.MESSAGE_TYPE_MMS);

                    // Add attachments
                    List<MmsMessage.Attachment> attachments = getMmsAttachments(context.getContentResolver(), id);
                    message.setAttachmentObjects(attachments);

                    // Add message to list
                    messages.add(message);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting MMS messages for thread ID: " + threadId, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return messages;
    }

    /**
     * Gets the MMS address.
     *
     * @param contentResolver The content resolver
     * @param id The message ID
     * @return The address
     */
    public String getMmsAddress(ContentResolver contentResolver, String id) {
        return getMmsAddress(contentResolver, id, 0);
    }

    /**
     * Gets the MMS address with type.
     *
     * @param contentResolver The content resolver
     * @param id The message ID
     * @param type The address type
     * @return The address
     */
    public String getMmsAddress(ContentResolver contentResolver, String id, int type) {
        String address = "";
        Cursor cursor = null;

        try {
            // Query MMS address
            cursor = contentResolver.query(
                    Uri.withAppendedPath(Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, id), "addr"),
                    new String[]{"address"},
                    "type = ?",
                    new String[]{String.valueOf(type)},
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                address = cursor.getString(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting MMS address for ID: " + id, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return address;
    }

    /**
     * Gets the text of an MMS message.
     *
     * @param contentResolver The content resolver
     * @param id The message ID
     * @return The message text
     */
    public String getMmsText(ContentResolver contentResolver, String id) {
        String text = "";
        Cursor cursor = null;

        try {
            // Query MMS parts
            cursor = contentResolver.query(
                    Telephony.Mms.Part.CONTENT_URI,
                    new String[]{"_id", "text", "ct"},
                    "mid = ?",
                    new String[]{id},
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String partId = cursor.getString(0);
                    String partText = cursor.getString(1);
                    String contentType = cursor.getString(2);

                    if (contentType != null && contentType.equals("text/plain")) {
                        if (partText != null) {
                            text = partText;
                            break;
                        } else {
                            // If text is null, try to get it from the part
                            text = getMmsPartText(contentResolver, partId);
                            if (!TextUtils.isEmpty(text)) {
                                break;
                            }
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting MMS text for ID: " + id, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return text;
    }

    /**
     * Gets the text of an MMS part.
     *
     * @param contentResolver The content resolver
     * @param partId The part ID
     * @return The part text
     */
    private String getMmsPartText(ContentResolver contentResolver, String partId) {
        String text = "";

        try {
            // Get part data
            Uri partUri = Uri.withAppendedPath(Telephony.Mms.Part.CONTENT_URI, partId);
            byte[] data = new byte[0];

            try (java.io.InputStream is = contentResolver.openInputStream(partUri)) {
                if (is != null) {
                    data = new byte[is.available()];
                    is.read(data);
                }
            }

            if (data.length > 0) {
                text = new String(data);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting MMS part text for ID: " + partId, e);
        }

        return text;
    }

    /**
     * Gets the attachments of an MMS message.
     *
     * @param contentResolver The content resolver
     * @param id The message ID
     * @return The list of attachments
     */
    private List<MmsMessage.Attachment> getMmsAttachments(ContentResolver contentResolver, String id) {
        List<MmsMessage.Attachment> attachments = new ArrayList<>();
        Cursor cursor = null;

        try {
            // Query MMS parts
            cursor = contentResolver.query(
                    Telephony.Mms.Part.CONTENT_URI,
                    new String[]{"_id", "ct", "_data", "text"},
                    "mid = ?",
                    new String[]{id},
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String partId = cursor.getString(0);
                    String contentType = cursor.getString(1);
                    String data = cursor.getString(2);
                    String text = cursor.getString(3);

                    if (contentType != null && !contentType.equals("text/plain") && !contentType.equals("application/smil")) {
                        // Create attachment
                        MmsMessage.Attachment attachment = new MmsMessage.Attachment();
                        attachment.setContentType(contentType);
                        attachment.setPartId(partId);

                        // Set URI
                        Uri uri = Uri.withAppendedPath(Telephony.Mms.Part.CONTENT_URI, partId);
                        attachment.setUri(uri);

                        // Add attachment to list
                        attachments.add(attachment);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting MMS attachments for ID: " + id, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return attachments;
    }

    /**
     * Sends an SMS message.
     *
     * @param address The recipient address
     * @param body The message body
     * @return True if the message was sent successfully, false otherwise
     */
    public boolean sendSmsMessage(String address, String body) {
        return sendSmsMessage(address, body, null, null);
    }

    /**
     * Sends an SMS message with additional parameters.
     *
     * @param address The recipient address
     * @param body The message body
     * @param threadId The thread ID (optional)
     * @param callback Callback to be called after sending (optional)
     * @return True if the message was sent successfully, false otherwise
     */
    public boolean sendSmsMessage(String address, String body, String threadId, MessageCallback callback) {
        if (TextUtils.isEmpty(address) || TextUtils.isEmpty(body)) {
            Log.e(TAG, "Cannot send SMS: address or body is empty");
            if (callback != null) {
                callback.onMessageFailed("Address or body is empty");
            }
            return false;
        }

        try {
            // Get SMS manager
            SmsManager smsManager = SmsManager.getDefault();

            // Check if message needs to be split
            if (body.length() > 160) {
                // Split message
                ArrayList<String> parts = smsManager.divideMessage(body);

                // Send message parts
                smsManager.sendMultipartTextMessage(address, null, parts, null, null);
            } else {
                // Send single message
                smsManager.sendTextMessage(address, null, body, null, null);
            }

            // Create message object
            SmsMessage message = new SmsMessage();
            message.setBody(body);
            message.setAddress(address);
            message.setDate(System.currentTimeMillis());
            message.setType(Message.TYPE_SENT);
            message.setRead(true);

            if (!TextUtils.isEmpty(threadId)) {
                message.setThreadId(threadId);
            }

            // Insert message into database
            ContentValues values = new ContentValues();
            values.put(Telephony.Sms.ADDRESS, address);
            values.put(Telephony.Sms.BODY, body);
            values.put(Telephony.Sms.DATE, System.currentTimeMillis());
            values.put(Telephony.Sms.READ, 1);
            values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT);

            if (!TextUtils.isEmpty(threadId)) {
                values.put(Telephony.Sms.THREAD_ID, threadId);
            }

            Uri uri = context.getContentResolver().insert(Telephony.Sms.CONTENT_URI, values);

            if (uri != null) {
                // Get message ID
                String id = uri.getLastPathSegment();
                message.setId(id);

                // Get thread ID if not provided
                if (TextUtils.isEmpty(threadId)) {
                    Cursor cursor = null;
                    try {
                        cursor = context.getContentResolver().query(
                                uri,
                                new String[]{Telephony.Sms.THREAD_ID},
                                null,
                                null,
                                null);

                        if (cursor != null && cursor.moveToFirst()) {
                            threadId = cursor.getString(0);
                            message.setThreadId(threadId);
                        }
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }

                // Clear cache for this thread
                if (!TextUtils.isEmpty(threadId)) {
                    messageCache.clearCache(threadId);
                }

                // Call callback
                if (callback != null) {
                    callback.onMessageSent(message);
                }

                return true;
            } else {
                Log.e(TAG, "Failed to insert SMS into database");
                if (callback != null) {
                    callback.onMessageFailed("Failed to insert SMS into database");
                }
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS: " + e.getMessage(), e);
            if (callback != null) {
                callback.onMessageFailed(e.getMessage());
            }
            return false;
        }
    }

    /**
     * Deletes a message.
     *
     * @param id The message ID
     * @return True if the message was deleted successfully, false otherwise
     */
    public boolean deleteMessage(String id) {
        return deleteMessage(id, Message.MESSAGE_TYPE_SMS);
    }

    /**
     * Deletes a message.
     *
     * @param id The message ID
     * @param messageType The message type
     * @return True if the message was deleted successfully, false otherwise
     */
    public boolean deleteMessage(String id, int messageType) {
        if (TextUtils.isEmpty(id)) {
            Log.e(TAG, "Cannot delete message: ID is empty");
            return false;
        }

        try {
            Uri uri;
            if (messageType == Message.MESSAGE_TYPE_MMS) {
                uri = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, id);
            } else {
                uri = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, id);
            }

            int deleted = context.getContentResolver().delete(uri, null, null);

            if (deleted > 0) {
                // Clear all caches since we don't know which thread this message belongs to
                messageCache.clearAllCaches();
                return true;
            } else {
                Log.e(TAG, "Failed to delete message: " + id);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting message: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Deletes a conversation.
     *
     * @param threadId The thread ID
     * @return True if successful, false otherwise
     */
    public boolean deleteConversation(String threadId) {
        if (TextUtils.isEmpty(threadId)) {
            Log.e(TAG, "Cannot delete conversation: thread ID is empty");
            return false;
        }

        try {
            Uri uri = Uri.withAppendedPath(Telephony.Sms.Conversations.CONTENT_URI, threadId);
            int deleted = context.getContentResolver().delete(uri, null, null);

            // Also delete MMS messages
            Uri mmsUri = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, "conversations/" + threadId);
            int mmsDeleted = context.getContentResolver().delete(mmsUri, null, null);

            // Clear cache for this thread
            messageCache.clearCache(threadId);

            return deleted > 0 || mmsDeleted > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting conversation: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Deletes a conversation by address.
     *
     * @param address The address
     * @return True if successful, false otherwise
     */
    public boolean deleteConversationByAddress(String address) {
        if (TextUtils.isEmpty(address)) {
            Log.e(TAG, "Cannot delete conversation: address is empty");
            return false;
        }

        // Find thread ID for this address
        String threadId = getThreadIdForAddress(address);

        if (!TextUtils.isEmpty(threadId)) {
            // If we found a thread ID, use it to delete the conversation
            return deleteConversation(threadId);
        } else {
            Log.e(TAG, "No thread ID found for address: " + address);
            return false;
        }
    }

    /**
     * Handles an incoming SMS.
     *
     * @param intent The intent containing the SMS data
     */
    public void handleIncomingSms(Intent intent) {
        // Implementation
    }

    /**
     * Loads messages for a conversation.
     *
     * @param threadId The thread ID
     * @return The list of messages
     */
    public List<Message> loadMessages(String threadId) {
        return getMessagesByThreadId(threadId);
    }

    /**
     * Marks a thread as read.
     *
     * @param threadId The thread ID
     * @return True if successful, false otherwise
     */
    public boolean markThreadAsRead(String threadId) {
        if (TextUtils.isEmpty(threadId)) {
            Log.e(TAG, "Cannot mark thread as read: thread ID is empty");
            return false;
        }

        try {
            // Mark SMS messages as read
            ContentValues values = new ContentValues();
            values.put(Telephony.Sms.READ, 1);

            int updated = context.getContentResolver().update(
                    Telephony.Sms.CONTENT_URI,
                    values,
                    Telephony.Sms.THREAD_ID + " = ?",
                    new String[]{threadId});

            // Mark MMS messages as read
            int mmsUpdated = context.getContentResolver().update(
                    Telephony.Mms.CONTENT_URI,
                    values,
                    Telephony.Mms.THREAD_ID + " = ?",
                    new String[]{threadId});

            // Clear cache for this thread
            messageCache.clearCache(threadId);

            return updated > 0 || mmsUpdated > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error marking thread as read: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Loads all conversations.
     *
     * @return The list of conversations
     */
    public List<Conversation> loadConversations() {
        List<Conversation> conversations = new ArrayList<>();
        Cursor cursor = null;

        try {
            // Query conversations
            Uri uri = Telephony.MmsSms.CONTENT_CONVERSATIONS_URI.buildUpon()
                    .appendQueryParameter("simple", "true")
                    .build();
            cursor = context.getContentResolver().query(
                    uri,
                    null,
                    null,
                    null,
                    Telephony.Sms.DEFAULT_SORT_ORDER);

            if (cursor != null && cursor.moveToFirst()) {
                // First pass: collect all conversation data without contact names
                List<String> addresses = new ArrayList<>();
                
                do {
                    Conversation conversation = new Conversation();

                    // Get conversation data
                    String threadId = cursor.getString(cursor.getColumnIndex("_id"));
                    String snippet = cursor.getString(cursor.getColumnIndex("snippet"));
                    long date = cursor.getLong(cursor.getColumnIndex("date"));
                    int messageCount = cursor.getInt(cursor.getColumnIndex("message_count"));
                    int read = cursor.getInt(cursor.getColumnIndex("read"));

                    // Set conversation data
                    conversation.setThreadId(threadId);
                    conversation.setSnippet(snippet);
                    conversation.setDate(new Date(date));
                    conversation.setMessageCount(messageCount);
                    conversation.setRead(read == 1);

                    // Get recipient address
                    String address = getAddressForThreadId(threadId);
                    conversation.setAddress(address);
                    
                    // Collect addresses for batch contact lookup
                    if (!TextUtils.isEmpty(address)) {
                        addresses.add(address);
                    }

                    // Add conversation to list
                    conversations.add(conversation);
                } while (cursor.moveToNext());
                
                // Second pass: batch lookup contact names
                Log.d(TAG, "Batch looking up contact names for " + addresses.size() + " addresses");
                Map<String, String> contactNames = ContactUtils.getContactNamesForNumbers(context, addresses);
                
                // Third pass: assign contact names with fallbacks
                for (Conversation conversation : conversations) {
                    String address = conversation.getAddress();
                    String contactName = contactNames.get(address);
                    
                    if (!TextUtils.isEmpty(contactName)) {
                        conversation.setContactName(contactName);
                        Log.d(TAG, "Found contact name '" + contactName + "' for address " + address);
                    } else if (!TextUtils.isEmpty(address)) {
                        // Fallback to address/phone number
                        conversation.setContactName(address);
                        Log.d(TAG, "Using address as contact name for " + address);
                    } else {
                        // Last resort fallback
                        conversation.setContactName("Unknown Contact");
                        Log.w(TAG, "No address or contact name available for thread " + conversation.getThreadId());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading conversations: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Log.d(TAG, "Loaded " + conversations.size() + " conversations");
        return conversations;
    }

    /**
     * Gets the address for a thread ID.
     *
     * @param threadId The thread ID
     * @return The address
     */
    private String getAddressForThreadId(String threadId) {
        String address = "";
        Cursor cursor = null;

        try {
            // Query the canonical addresses table
            Uri uri = Telephony.MmsSms.CONTENT_URI.buildUpon()
                    .appendPath("canonical-addresses")
                    .build();

            // First get the recipient ID for this thread
            String recipientId = null;
            Cursor threadCursor = null;

            try {
                threadCursor = context.getContentResolver().query(
                        Telephony.MmsSms.CONTENT_CONVERSATIONS_URI.buildUpon()
                                .appendQueryParameter("simple", "true")
                                .build(),
                        new String[]{"recipient_ids"},
                        "_id = ?",
                        new String[]{threadId},
                        null);

                if (threadCursor != null && threadCursor.moveToFirst()) {
                    recipientId = threadCursor.getString(0);
                }
            } finally {
                if (threadCursor != null) {
                    threadCursor.close();
                }
            }

            if (recipientId != null) {
                // Now get the address for this recipient ID
                cursor = context.getContentResolver().query(
                        uri,
                        new String[]{"address"},
                        "_id = ?",
                        new String[]{recipientId},
                        null);

                if (cursor != null && cursor.moveToFirst()) {
                    address = cursor.getString(0);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting address for thread ID: " + threadId, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return address;
    }

    /**
     * Searches messages.
     *
     * @param query The search query
     * @return The list of matching messages
     */
    public List<Message> searchMessages(String query) {
        List<Message> messages = new ArrayList<>();

        if (TextUtils.isEmpty(query)) {
            return messages;
        }

        // Search SMS messages
        messages.addAll(searchSmsMessages(query));

        // Search MMS messages
        messages.addAll(searchMmsMessages(query));

        // Sort messages by date
        messages.sort((m1, m2) -> Long.compare(m2.getDate(), m1.getDate()));

        return messages;
    }

    /**
     * Searches SMS messages.
     *
     * @param query The search query
     * @return The list of matching SMS messages
     */
    private List<Message> searchSmsMessages(String query) {
        List<Message> messages = new ArrayList<>();
        Cursor cursor = null;

        try {
            // Query SMS messages
            cursor = context.getContentResolver().query(
                    Telephony.Sms.CONTENT_URI,
                    null,
                    Telephony.Sms.BODY + " LIKE ?",
                    new String[]{"%" + query + "%"},
                    Telephony.Sms.DEFAULT_SORT_ORDER);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    SmsMessage message = new SmsMessage();

                    // Get message data
                    String id = cursor.getString(cursor.getColumnIndex(Telephony.Sms._ID));
                    String body = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
                    long date = cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE));
                    int type = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.TYPE));
                    boolean read = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.READ)) == 1;
                    String address = cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS));
                    String threadId = cursor.getString(cursor.getColumnIndex(Telephony.Sms.THREAD_ID));

                    // Set message data
                    message.setId(id);
                    message.setBody(body);
                    message.setDate(date);
                    message.setType(type);
                    message.setRead(read);
                    message.setAddress(address);
                    message.setThreadId(threadId);
                    message.setMessageType(Message.MESSAGE_TYPE_SMS);
                    message.setSearchQuery(query);

                    // Get contact name
                    String contactName = ContactUtils.getContactName(context, address);
                    message.setContactName(contactName);

                    // Add message to list
                    messages.add(message);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error searching SMS messages: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return messages;
    }

    /**
     * Adds a test message to the database.
     *
     * @return True if successful, false otherwise
     */
    public boolean addTestMessage() {
        // Create a test message
        String address = "+15555555555";
        String body = "This is a test message from the app.";

        // Send the message
        return sendSmsMessage(address, body);
    }

    /**
     * Searches MMS messages.
     *
     * @param query The search query
     * @return The list of matching MMS messages
     */
    private List<Message> searchMmsMessages(String query) {
        List<Message> messages = new ArrayList<>();
        Cursor cursor = null;

        try {
            // Query MMS parts
            cursor = context.getContentResolver().query(
                    Telephony.Mms.Part.CONTENT_URI,
                    new String[]{"mid"},
                    "text LIKE ?",
                    new String[]{"%" + query + "%"},
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String messageId = cursor.getString(0);

                    // Get MMS message
                    Cursor mmsCursor = null;
                    try {
                        mmsCursor = context.getContentResolver().query(
                                Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, messageId),
                                null,
                                null,
                                null,
                                null);

                        if (mmsCursor != null && mmsCursor.moveToFirst()) {
                            MmsMessage message = new MmsMessage();

                            // Get message data
                            String id = mmsCursor.getString(mmsCursor.getColumnIndex(Telephony.Mms._ID));
                            long date = mmsCursor.getLong(mmsCursor.getColumnIndex(Telephony.Mms.DATE)) * 1000; // MMS date is in seconds
                            int type = mmsCursor.getInt(mmsCursor.getColumnIndex(Telephony.Mms.MESSAGE_BOX));
                            boolean read = mmsCursor.getInt(mmsCursor.getColumnIndex(Telephony.Mms.READ)) == 1;
                            String threadId = mmsCursor.getString(mmsCursor.getColumnIndex(Telephony.Mms.THREAD_ID));

                            // Convert MMS message box to SMS type
                            int smsType;
                            switch (type) {
                                case Telephony.Mms.MESSAGE_BOX_INBOX:
                                    smsType = Message.TYPE_INBOX;
                                    break;
                                case Telephony.Mms.MESSAGE_BOX_SENT:
                                    smsType = Message.TYPE_SENT;
                                    break;
                                case Telephony.Mms.MESSAGE_BOX_DRAFTS:
                                    smsType = Message.TYPE_DRAFT;
                                    break;
                                case Telephony.Mms.MESSAGE_BOX_OUTBOX:
                                    smsType = Message.TYPE_OUTBOX;
                                    break;
                                default:
                                    smsType = Message.TYPE_ALL;
                                    break;
                            }

                            // Get MMS address
                            String address = getMmsAddress(context.getContentResolver(), id);

                            // Get MMS text
                            String body = getMmsText(context.getContentResolver(), id);

                            // Set message data
                            message.setId(id);
                            message.setBody(body);
                            message.setDate(date);
                            message.setType(smsType);
                            message.setRead(read);
                            message.setAddress(address);
                            message.setThreadId(threadId);
                            message.setMessageType(Message.MESSAGE_TYPE_MMS);
                            message.setSearchQuery(query);

                            // Get contact name
                            String contactName = ContactUtils.getContactName(context, address);
                            message.setContactName(contactName);

                            // Add attachments
                            List<MmsMessage.Attachment> attachments = getMmsAttachments(context.getContentResolver(), id);
                            message.setAttachmentObjects(attachments);

                            // Add message to list
                            messages.add(message);
                        }
                    } finally {
                        if (mmsCursor != null) {
                            mmsCursor.close();
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error searching MMS messages: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return messages;
    }

    /**
     * Callback interface for message operations.
     */
    public interface MessageCallback {
        void onMessageSent(Message message);
        void onMessageFailed(String error);

        /**
         * Static factory method to create a simple success callback.
         *
         * @param runnable The runnable to execute on success
         * @return A MessageCallback that runs the runnable on success and does nothing on failure
         */
        static MessageCallback onSuccess(Runnable runnable) {
            return new MessageCallback() {
                @Override
                public void onMessageSent(Message message) {
                    runnable.run();
                }

                @Override
                public void onMessageFailed(String error) {
                    // Do nothing
                }
            };
        }
    }
}