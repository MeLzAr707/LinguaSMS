package com.translator.messagingapp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Optimized service for handling messages with improved performance.
 */
public class OptimizedMessageService {
    private static final String TAG = "OptimizedMessageService";
    private static final int DEFAULT_PAGE_SIZE = 50;
    
    private final Context context;
    private final TranslationManager translationManager;
    private final OptimizedMessageCache messageCache;
    private final Executor queryExecutor;
    
    /**
     * Creates a new OptimizedMessageService.
     *
     * @param context The application context
     * @param translationManager The translation manager
     */
    public OptimizedMessageService(Context context, TranslationManager translationManager) {
        this.context = context;
        this.translationManager = translationManager;
        this.messageCache = new OptimizedMessageCache();
        this.queryExecutor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Gets paginated messages by thread ID.
     *
     * @param threadId The thread ID
     * @param offset The offset
     * @param limit The limit
     * @param callback Callback to receive the messages
     */
    public void getMessagesByThreadIdPaginated(String threadId, int offset, int limit, MessageCallback callback) {
        if (TextUtils.isEmpty(threadId)) {
            Log.e(TAG, "Thread ID is empty");
            if (callback != null) {
                callback.onMessagesLoaded(new ArrayList<>());
            }
            return;
        }
        
        // Generate a cache key that includes pagination parameters
        final String cacheKey = threadId + "_" + offset + "_" + limit;
        
        // Check cache first
        List<Message> cachedMessages = messageCache.getMessages(cacheKey);
        if (cachedMessages != null) {
            Log.d(TAG, "Returning " + cachedMessages.size() + " cached messages for thread ID: " + threadId);
            if (callback != null) {
                callback.onMessagesLoaded(cachedMessages);
            }
            return;
        }
        
        // Execute query in background
        queryExecutor.execute(() -> {
            try {
                List<Message> messages = new ArrayList<>();
                
                // Query SMS messages with pagination
                List<Message> smsMessages = getSmsMessagesByThreadIdPaginated(threadId, offset, limit);
                if (smsMessages != null) {
                    messages.addAll(smsMessages);
                    Log.d(TAG, "Found " + smsMessages.size() + " SMS messages for thread " + threadId);
                }
                
                // Query MMS messages with pagination
                List<Message> mmsMessages = getMmsMessagesByThreadIdPaginated(threadId, offset, limit);
                if (mmsMessages != null) {
                    messages.addAll(mmsMessages);
                    Log.d(TAG, "Found " + mmsMessages.size() + " MMS messages for thread " + threadId);
                }
                
                // Sort messages by date
                messages.sort((m1, m2) -> Long.compare(m1.getDate(), m2.getDate()));
                
                // Cache messages
                messageCache.cacheMessages(cacheKey, messages);
                
                // Deliver result on the calling thread
                if (callback != null) {
                    callback.onMessagesLoaded(messages);
                }
                
                Log.d(TAG, "Returning " + messages.size() + " total messages for thread ID: " + threadId);
            } catch (Exception e) {
                Log.e(TAG, "Error getting paginated messages for thread ID: " + threadId, e);
                if (callback != null) {
                    callback.onMessagesLoaded(new ArrayList<>());
                }
            }
        });
    }
    
    /**
     * Gets SMS messages by thread ID with pagination.
     *
     * @param threadId The thread ID
     * @param offset The offset
     * @param limit The limit
     * @return The list of SMS messages
     */
    private List<Message> getSmsMessagesByThreadIdPaginated(String threadId, int offset, int limit) {
        List<Message> messages = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            // Query SMS messages with pagination
            cursor = context.getContentResolver().query(
                    Telephony.Sms.CONTENT_URI,
                    null,
                    Telephony.Sms.THREAD_ID + " = ?",
                    new String[]{threadId},
                    Telephony.Sms.DATE + " ASC LIMIT " + limit + " OFFSET " + offset);
            
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
            Log.e(TAG, "Error getting paginated SMS messages for thread ID: " + threadId, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return messages;
    }
    
    /**
     * Gets MMS messages by thread ID with pagination.
     *
     * @param threadId The thread ID
     * @param offset The offset
     * @param limit The limit
     * @return The list of MMS messages
     */
    private List<Message> getMmsMessagesByThreadIdPaginated(String threadId, int offset, int limit) {
        List<Message> messages = new ArrayList<>();
        Cursor cursor = null;
        
        try {
            // Query MMS messages with pagination
            cursor = context.getContentResolver().query(
                    Telephony.Mms.CONTENT_URI,
                    null,
                    Telephony.Mms.THREAD_ID + " = ?",
                    new String[]{threadId},
                    Telephony.Mms.DATE + " ASC LIMIT " + limit + " OFFSET " + offset);
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    MmsMessage message = new MmsMessage();
                    
                    // Get message data
                    String id = cursor.getString(cursor.getColumnIndex(Telephony.Mms._ID));
                    long date = cursor.getLong(cursor.getColumnIndex(Telephony.Mms.DATE)) * 1000; // MMS date is in seconds
                    int type = cursor.getInt(cursor.getColumnIndex(Telephony.Mms.MESSAGE_BOX));
                    boolean read = cursor.getInt(cursor.getColumnIndex(Telephony.Mms.READ)) == 1;
                    
                    // Convert MMS message box to SMS type
                    int smsType = convertMmsTypeToSmsType(type);
                    
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
            Log.e(TAG, "Error getting paginated MMS messages for thread ID: " + threadId, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return messages;
    }
    
    /**
     * Converts MMS message box type to SMS type.
     *
     * @param mmsType The MMS message box type
     * @return The SMS type
     */
    private int convertMmsTypeToSmsType(int mmsType) {
        switch (mmsType) {
            case Telephony.Mms.MESSAGE_BOX_INBOX:
                return Message.TYPE_INBOX;
            case Telephony.Mms.MESSAGE_BOX_SENT:
                return Message.TYPE_SENT;
            case Telephony.Mms.MESSAGE_BOX_DRAFTS:
                return Message.TYPE_DRAFT;
            case Telephony.Mms.MESSAGE_BOX_OUTBOX:
                return Message.TYPE_OUTBOX;
            default:
                return Message.TYPE_ALL;
        }
    }
    
    /**
     * Gets the MMS address.
     *
     * @param contentResolver The content resolver
     * @param id The message ID
     * @return The address
     */
    private String getMmsAddress(ContentResolver contentResolver, String id) {
        String address = "";
        Cursor cursor = null;
        
        try {
            // Query MMS address
            cursor = contentResolver.query(
                    Uri.parse("content://mms/" + id + "/addr"),
                    new String[]{"address"},
                    "type = ?",
                    new String[]{"137"}, // 137 = TO address type
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
    private String getMmsText(ContentResolver contentResolver, String id) {
        String text = "";
        Cursor cursor = null;
        
        try {
            // Query MMS parts
            cursor = contentResolver.query(
                    Uri.parse("content://mms/part"),
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
            Uri partUri = Uri.parse("content://mms/part/" + partId);
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
                    Uri.parse("content://mms/part"),
                    new String[]{"_id", "ct", "_data", "text"},
                    "mid = ?",
                    new String[]{id},
                    null);
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String partId = cursor.getString(0);
                    String contentType = cursor.getString(1);
                    
                    if (contentType != null && !contentType.equals("text/plain") && !contentType.equals("application/smil")) {
                        // Create attachment
                        MmsMessage.Attachment attachment = new MmsMessage.Attachment();
                        attachment.setContentType(contentType);
                        attachment.setPartId(partId);
                        
                        // Set URI
                        Uri uri = Uri.parse("content://mms/part/" + partId);
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
     * Gets the total number of messages in a thread.
     *
     * @param threadId The thread ID
     * @return The total number of messages
     */
    public int getMessageCount(String threadId) {
        if (TextUtils.isEmpty(threadId)) {
            return 0;
        }
        
        int smsCount = 0;
        int mmsCount = 0;
        
        // Count SMS messages
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    Telephony.Sms.CONTENT_URI,
                    new String[]{"COUNT(*) AS count"},
                    Telephony.Sms.THREAD_ID + " = ?",
                    new String[]{threadId},
                    null);
            
            if (cursor != null && cursor.moveToFirst()) {
                smsCount = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error counting SMS messages for thread ID: " + threadId, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        // Count MMS messages
        cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    Telephony.Mms.CONTENT_URI,
                    new String[]{"COUNT(*) AS count"},
                    Telephony.Mms.THREAD_ID + " = ?",
                    new String[]{threadId},
                    null);
            
            if (cursor != null && cursor.moveToFirst()) {
                mmsCount = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error counting MMS messages for thread ID: " + threadId, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return smsCount + mmsCount;
    }
    
    /**
     * Clears the message cache.
     */
    public void clearCache() {
        messageCache.clearAllCaches();
    }
    
    /**
     * Callback interface for message loading.
     */
    public interface MessageCallback {
        /**
         * Called when messages are loaded.
         *
         * @param messages The loaded messages
         */
        void onMessagesLoaded(List<Message> messages);
    }
}