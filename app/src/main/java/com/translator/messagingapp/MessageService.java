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
    private final RcsService rcsService;

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
        this.rcsService = new RcsService(context);
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

        // Try the primary method first
        try {
            conversations = loadConversationsFromProvider(contentResolver);
            if (!conversations.isEmpty()) {
                return conversations;
            }
        } catch (Exception e) {
            Log.e(TAG, "Primary conversation loading failed, trying fallback", e);
        }

        // Fallback method: Load from SMS directly 
        try {
            conversations = loadConversationsFromSms(contentResolver);
            if (!conversations.isEmpty()) {
                return conversations;
            }
        } catch (Exception e) {
            Log.e(TAG, "SMS conversation loading failed", e);
        }

        // Final fallback: Load from MMS
        try {
            conversations = loadConversationsFromMms(contentResolver);
        } catch (Exception e) {
            Log.e(TAG, "MMS conversation loading failed", e);
        }

        // Ensure conversations are sorted by date (newest first)
        Collections.sort(conversations, (c1, c2) -> {
            if (c1.getDate() == null && c2.getDate() == null) return 0;
            if (c1.getDate() == null) return 1;
            if (c2.getDate() == null) return -1;
            return Long.compare(c2.getDate().getTime(), c1.getDate().getTime()); // Newest first
        });

        return conversations;
    }

    /**
     * Loads conversations using the standard conversations provider.
     */
    private List<Conversation> loadConversationsFromProvider(ContentResolver contentResolver) {
        List<Conversation> conversations = new ArrayList<>();
        
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
        }

        return conversations;
    }

    /**
     * Loads conversations by examining SMS messages directly.
     */
    private List<Conversation> loadConversationsFromSms(ContentResolver contentResolver) {
        List<Conversation> conversations = new ArrayList<>();
        
        // Query SMS messages to find distinct thread IDs
        Uri uri = Uri.parse("content://sms");
        String sortOrder = "date DESC";

        try (Cursor cursor = contentResolver.query(uri, null, null, null, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                List<String> seenThreadIds = new ArrayList<>();
                do {
                    int threadIdIndex = cursor.getColumnIndex("thread_id");
                    if (threadIdIndex >= 0) {
                        String threadId = cursor.getString(threadIdIndex);
                        if (threadId != null && !seenThreadIds.contains(threadId)) {
                            seenThreadIds.add(threadId);
                            Conversation conversation = loadConversationDetails(threadId);
                            if (conversation != null) {
                                conversations.add(conversation);
                            }
                        }
                    }
                } while (cursor.moveToNext());
            }
        }

        return conversations;
    }

    /**
     * Loads conversations by examining MMS messages directly.
     */
    private List<Conversation> loadConversationsFromMms(ContentResolver contentResolver) {
        List<Conversation> conversations = new ArrayList<>();
        
        // Query MMS messages to find distinct thread IDs
        Uri uri = Uri.parse("content://mms");
        String sortOrder = "date DESC";

        try (Cursor cursor = contentResolver.query(uri, null, null, null, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                List<String> seenThreadIds = new ArrayList<>();
                do {
                    int threadIdIndex = cursor.getColumnIndex("thread_id");
                    if (threadIdIndex >= 0) {
                        String threadId = cursor.getString(threadIdIndex);
                        if (threadId != null && !seenThreadIds.contains(threadId)) {
                            seenThreadIds.add(threadId);
                            Conversation conversation = loadMmsConversationDetails(threadId);
                            if (conversation != null) {
                                conversations.add(conversation);
                            }
                        }
                    }
                } while (cursor.moveToNext());
            }
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

        // First try to get SMS messages for this thread
        Uri smsUri = Uri.parse("content://sms");
        String smsSelection = "thread_id = ?";
        String[] smsSelectionArgs = new String[] { threadId };
        String sortOrder = "date DESC LIMIT 1";

        try (Cursor cursor = contentResolver.query(smsUri, null, smsSelection, smsSelectionArgs, sortOrder)) {
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
                conversation.setLastMessage(snippet); // Also set lastMessage for consistency
                conversation.setDate(date);
                conversation.setRead(read);

                // Resolve contact name
                String contactName = ContactUtils.getContactName(context, address);
                conversation.setContactName(contactName);

                // Count unread messages
                int unreadCount = countUnreadMessages(threadId);
                conversation.setUnreadCount(unreadCount);

                return conversation;
            } else {
                // If no SMS messages, try MMS
                return loadMmsConversationDetails(threadId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading SMS conversation details for thread " + threadId, e);
            // Try MMS as fallback
            return loadMmsConversationDetails(threadId);
        }
    }

    /**
     * Loads MMS conversation details.
     *
     * @param threadId The thread ID of the conversation
     * @return The conversation details
     */
    private Conversation loadMmsConversationDetails(String threadId) {
        ContentResolver contentResolver = context.getContentResolver();

        // Query the MMS content provider for the latest message in this thread
        Uri mmsUri = Uri.parse("content://mms");
        String mmsSelection = "thread_id = ?";
        String[] mmsSelectionArgs = new String[] { threadId };
        String sortOrder = "date DESC LIMIT 1";

        try (Cursor cursor = contentResolver.query(mmsUri, null, mmsSelection, mmsSelectionArgs, sortOrder)) {
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
                String mmsSnippet = snippet != null ? snippet : "[MMS]";
                conversation.setSnippet(mmsSnippet);
                conversation.setLastMessage(mmsSnippet); // Also set lastMessage for consistency
                conversation.setDate(date);
                conversation.setRead(read);

                // Resolve contact name
                String contactName = ContactUtils.getContactName(context, address);
                conversation.setContactName(contactName);

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
        if (messageId == null || messageId.isEmpty()) {
            return null;
        }

        String address = null;

        // Query the addr table to get the address
        Uri uri = Uri.parse("content://mms/" + messageId + "/addr");
        String selection = "type=" + TYPE_TO;

        try (Cursor cursor = contentResolver.query(uri, null, selection, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int addressIndex = cursor.getColumnIndex("address");
                if (addressIndex >= 0) {
                    address = cursor.getString(addressIndex);
                }
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
        if (messageId == null || messageId.isEmpty()) {
            return null;
        }

        String text = null;

        // Query the part table to get the text parts
        Uri uri = Uri.parse("content://mms/" + messageId + "/part");

        try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int contentTypeIndex = cursor.getColumnIndex("ct");
                    int dataIndex = cursor.getColumnIndex("_data");
                    int textIndex = cursor.getColumnIndex("text");
                    
                    String contentType = null;
                    if (contentTypeIndex >= 0) {
                        contentType = cursor.getString(contentTypeIndex);
                    }
                    
                    // Look for various text content types - be more inclusive
                    boolean isTextPart = false;
                    if (contentType != null) {
                        String lowerContentType = contentType.toLowerCase();
                        isTextPart = lowerContentType.startsWith("text/plain") || 
                                    lowerContentType.startsWith("text/") ||
                                    lowerContentType.equals("text") ||
                                    lowerContentType.contains("text");
                    } else {
                        // If content type is null, still check for text data
                        isTextPart = true;
                    }
                    
                    if (isTextPart) {
                        // Try to get text from _data column first (file-based storage)
                        if (dataIndex >= 0) {
                            String data = cursor.getString(dataIndex);
                            if (data != null && !data.trim().isEmpty()) {
                                // Text is stored in a file
                                text = getMmsTextFromFile(contentResolver, data);
                                if (text != null && !text.trim().isEmpty()) {
                                    Log.d(TAG, "Found MMS text from file for message " + messageId + ": " + 
                                          text.substring(0, Math.min(50, text.length())));
                                    break;
                                }
                            }
                        }
                        
                        // Try to get text directly from text column
                        if (textIndex >= 0) {
                            String directText = cursor.getString(textIndex);
                            if (directText != null && !directText.trim().isEmpty()) {
                                text = directText;
                                Log.d(TAG, "Found MMS text directly for message " + messageId + ": " + 
                                      text.substring(0, Math.min(50, text.length())));
                                break;
                            }
                        }
                    }
                } while (cursor.moveToNext());
            } else {
                Log.d(TAG, "No MMS parts found for message " + messageId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting MMS text for message " + messageId, e);
        }

        if (text == null || text.trim().isEmpty()) {
            Log.d(TAG, "No text content found for MMS message " + messageId);
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
        if (dataPath == null || dataPath.trim().isEmpty()) {
            Log.d(TAG, "Data path is null or empty for MMS text file");
            return null;
        }
        
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
                
                // Convert bytes to string and handle potential encoding issues
                byte[] textBytes = baos.toByteArray();
                if (textBytes.length > 0) {
                    text = new String(textBytes).trim();
                    Log.d(TAG, "Successfully read " + textBytes.length + " bytes from MMS text file");
                } else {
                    Log.d(TAG, "MMS text file is empty");
                }
            } else {
                Log.d(TAG, "Could not open input stream for MMS text file: " + uri);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading MMS text from file: " + uri, e);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception reading MMS text from file: " + uri, e);
        }

        return text;
    }

    /**
     * Loads attachments for an MMS message.
     *
     * @param contentResolver The content resolver
     * @param messageId The MMS message ID
     * @param mmsMessage The MMS message to add attachments to
     */
    private void loadMmsAttachments(ContentResolver contentResolver, String messageId, MmsMessage mmsMessage) {
        if (messageId == null || messageId.isEmpty() || mmsMessage == null) {
            return;
        }

        // Query the part table to get all parts for this MMS message
        Uri uri = Uri.parse("content://mms/" + messageId + "/part");

        try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int contentTypeIndex = cursor.getColumnIndex("ct");
                    int dataIdIndex = cursor.getColumnIndex("_id");
                    int nameIndex = cursor.getColumnIndex("name");
                    int sizeIndex = cursor.getColumnIndex("_size");

                    String contentType = null;
                    String partId = null;
                    
                    if (contentTypeIndex >= 0) {
                        contentType = cursor.getString(contentTypeIndex);
                    }
                    
                    if (dataIdIndex >= 0) {
                        partId = cursor.getString(dataIdIndex);
                    }

                    // Skip if we don't have the essential data
                    if (partId == null || partId.trim().isEmpty()) {
                        continue;
                    }

                    // Skip text parts and SMIL as they're handled by getMmsText or are metadata
                    boolean isAttachment = true;
                    if (contentType != null) {
                        String lowerContentType = contentType.toLowerCase();
                        if (lowerContentType.startsWith("text/plain") || 
                            lowerContentType.equals("application/smil") ||
                            lowerContentType.startsWith("text/") ||
                            lowerContentType.equals("text")) {
                            isAttachment = false;
                        }
                    }

                    if (isAttachment) {
                        // This is an attachment (image, video, audio, etc.)
                        Uri attachmentUri = Uri.parse("content://mms/part/" + partId);
                        
                        String fileName = null;
                        if (nameIndex >= 0) {
                            fileName = cursor.getString(nameIndex);
                        }
                        
                        long size = 0;
                        if (sizeIndex >= 0) {
                            size = cursor.getLong(sizeIndex);
                        }

                        // Create attachment object
                        MmsMessage.Attachment attachment = new MmsMessage.Attachment(
                            attachmentUri, 
                            contentType, 
                            fileName, 
                            size
                        );

                        mmsMessage.addAttachment(attachment);
                        Log.d(TAG, "Loaded attachment: " + contentType + " for MMS " + messageId + 
                              " (URI: " + attachmentUri + ")");
                    } else {
                        Log.d(TAG, "Skipped text/metadata part: " + contentType + " for MMS " + messageId);
                    }
                } while (cursor.moveToNext());
                
                Log.d(TAG, "Loaded " + mmsMessage.getAttachmentObjects().size() + " attachments for MMS " + messageId);
            } else {
                Log.d(TAG, "No MMS parts found for message " + messageId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading MMS attachments for message " + messageId, e);
        }
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
        if (threadId == null || threadId.isEmpty()) {
            Log.e(TAG, "Cannot load messages: threadId is null or empty");
            return new ArrayList<>();
        }

        List<Message> messages = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();

        try {
            // Load SMS messages
            loadSmsMessages(contentResolver, threadId, messages);

            // Load MMS messages
            loadMmsMessages(contentResolver, threadId, messages);

            // Load RCS messages if available
            loadRcsMessages(threadId, messages);

            // Sort by date (oldest first for proper chronological order)
            Collections.sort(messages, (m1, m2) -> Long.compare(m1.getDate(), m2.getDate()));

            Log.d(TAG, "Loaded " + messages.size() + " messages for thread " + threadId);
        } catch (Exception e) {
            Log.e(TAG, "Error loading messages for thread " + threadId, e);
        }

        return messages;
    }

    /**
     * Loads messages for a specific thread with pagination support.
     *
     * @param threadId The thread ID
     * @param offset The number of messages to skip
     * @param limit The maximum number of messages to load
     * @return A list of messages
     */
    public List<Message> loadMessagesPaginated(String threadId, int offset, int limit) {
        if (threadId == null || threadId.isEmpty()) {
            Log.e(TAG, "Cannot load messages: threadId is null or empty");
            return new ArrayList<>();
        }

        List<Message> messages = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();

        try {
            // Load SMS messages with pagination
            loadSmsMessagesPaginated(contentResolver, threadId, messages, offset, limit);

            // Load MMS messages with pagination
            loadMmsMessagesPaginated(contentResolver, threadId, messages, offset, limit);

            // Load RCS messages with pagination if available
            loadRcsMessagesPaginated(threadId, messages, offset, limit);

            // Sort by date (newest first for pagination)
            Collections.sort(messages, (m1, m2) -> Long.compare(m2.getDate(), m1.getDate()));

            // Apply limit after sorting
            if (messages.size() > limit) {
                messages = new ArrayList<>(messages.subList(0, limit));
            }

            Log.d(TAG, "Loaded " + messages.size() + " paginated messages for thread " + threadId + 
                  " (offset: " + offset + ", limit: " + limit + ")");
        } catch (Exception e) {
            Log.e(TAG, "Error loading paginated messages for thread " + threadId, e);
        }

        return messages;
    }

    /**
     * Loads SMS messages for a thread.
     *
     * @param contentResolver The content resolver
     * @param threadId The thread ID
     * @param messages The list to add messages to
     */
    private void loadSmsMessages(ContentResolver contentResolver, String threadId, List<Message> messages) {
        Uri uri = Uri.parse("content://sms");
        String selection = "thread_id = ?";
        String[] selectionArgs = new String[] { threadId };
        String sortOrder = "date ASC";

        try (Cursor cursor = contentResolver.query(uri, null, selection, selectionArgs, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                Log.d(TAG, "Found " + cursor.getCount() + " SMS messages for thread " + threadId);
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
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing SMS message in thread " + threadId, e);
                        // Continue processing other messages
                    }
                } while (cursor.moveToNext());
            } else {
                Log.d(TAG, "No SMS messages found for thread " + threadId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading SMS messages for thread " + threadId, e);
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
        String sortOrder = "date ASC";

        try (Cursor cursor = contentResolver.query(uri, null, selection, selectionArgs, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                Log.d(TAG, "Found " + cursor.getCount() + " MMS messages for thread " + threadId);
                do {
                    try {
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

                        // Load attachments for this MMS message
                        loadMmsAttachments(contentResolver, id, message);

                        messages.add(message);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing MMS message in thread " + threadId, e);
                        // Continue processing other messages
                    }
                } while (cursor.moveToNext());
            } else {
                Log.d(TAG, "No MMS messages found for thread " + threadId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading MMS messages for thread " + threadId, e);
        }
    }

    /**
     * Loads SMS messages for a thread with pagination.
     *
     * @param contentResolver The content resolver
     * @param threadId The thread ID
     * @param messages The list to add messages to
     * @param offset The number of messages to skip
     * @param limit The maximum number of messages to load
     */
    private void loadSmsMessagesPaginated(ContentResolver contentResolver, String threadId, List<Message> messages, int offset, int limit) {
        Uri uri = Uri.parse("content://sms");
        String selection = "thread_id = ?";
        String[] selectionArgs = new String[] { threadId };
        String sortOrder = "date DESC LIMIT " + limit + " OFFSET " + offset;

        try (Cursor cursor = contentResolver.query(uri, null, selection, selectionArgs, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                Log.d(TAG, "Found " + cursor.getCount() + " SMS messages for thread " + threadId + " (paginated)");
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
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing SMS message in thread " + threadId, e);
                        // Continue processing other messages
                    }
                } while (cursor.moveToNext());
            } else {
                Log.d(TAG, "No SMS messages found for thread " + threadId + " (paginated)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading paginated SMS messages for thread " + threadId, e);
        }
    }

    /**
     * Loads MMS messages for a thread with pagination.
     *
     * @param contentResolver The content resolver
     * @param threadId The thread ID
     * @param messages The list to add messages to
     * @param offset The number of messages to skip
     * @param limit The maximum number of messages to load
     */
    private void loadMmsMessagesPaginated(ContentResolver contentResolver, String threadId, List<Message> messages, int offset, int limit) {
        Uri uri = Uri.parse("content://mms");
        String selection = "thread_id = ?";
        String[] selectionArgs = new String[] { threadId };
        String sortOrder = "date DESC LIMIT " + limit + " OFFSET " + offset;

        try (Cursor cursor = contentResolver.query(uri, null, selection, selectionArgs, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                Log.d(TAG, "Found " + cursor.getCount() + " MMS messages for thread " + threadId + " (paginated)");
                do {
                    try {
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

                        // Load attachments for this MMS message
                        loadMmsAttachments(contentResolver, id, message);

                        messages.add(message);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing MMS message in thread " + threadId, e);
                        // Continue processing other messages
                    }
                } while (cursor.moveToNext());
            } else {
                Log.d(TAG, "No MMS messages found for thread " + threadId + " (paginated)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading paginated MMS messages for thread " + threadId, e);
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
            
            // Broadcast message sent to refresh UI
            broadcastMessageSent();

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

            // Broadcast message sent to refresh UI
            broadcastMessageSent();

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

                            // Load attachments for this MMS message
                            loadMmsAttachments(contentResolver, id, message);

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

                    // Load attachments for this MMS message
                    loadMmsAttachments(contentResolver, id, message);

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
                StringBuilder fullMessageBody = new StringBuilder();
                String senderAddress = null;
                long messageTimestamp = 0;

                // Process all PDUs to handle multi-part messages
                for (Object pdu : pdus) {
                    SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
                    if (senderAddress == null) {
                        senderAddress = smsMessage.getOriginatingAddress();
                        messageTimestamp = smsMessage.getTimestampMillis();
                    }
                    fullMessageBody.append(smsMessage.getMessageBody());
                }

                if (senderAddress != null && fullMessageBody.length() > 0) {
                    // Store the message in the SMS database
                    storeSmsMessage(senderAddress, fullMessageBody.toString(), messageTimestamp);
                    
                    // Process the message
                    Log.d(TAG, "Received SMS from " + senderAddress + ": " + fullMessageBody.toString());

                    // Show notification
                    showSmsNotification(senderAddress, fullMessageBody.toString());
                    
                    // Broadcast message received to refresh UI
                    broadcastMessageReceived();
                }
            }
        }
    }

    /**
     * Stores an incoming SMS message in the device's SMS database.
     *
     * @param address The sender's address
     * @param body The message body
     * @param timestamp The message timestamp
     */
    private void storeSmsMessage(String address, String body, long timestamp) {
        try {
            ContentValues values = new ContentValues();
            values.put(Telephony.Sms.ADDRESS, address);
            values.put(Telephony.Sms.BODY, body);
            values.put(Telephony.Sms.DATE, timestamp);
            values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX);
            values.put(Telephony.Sms.READ, 0); // Mark as unread
            values.put(Telephony.Sms.SEEN, 0); // Mark as unseen

            Uri uri = context.getContentResolver().insert(Telephony.Sms.CONTENT_URI, values);
            if (uri != null) {
                Log.d(TAG, "Successfully stored SMS message from " + address);
            } else {
                Log.e(TAG, "Failed to store SMS message from " + address);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error storing SMS message from " + address, e);
        }
    }

    /**
     * Shows a notification for an incoming SMS message.
     *
     * @param address The sender's address
     * @param body The message body
     */
    private void showSmsNotification(String address, String body) {
        try {
            // Get thread ID for the notification
            String threadId = getThreadIdForAddress(address);
            
            // Get contact name or use address as fallback
            String displayName = getContactNameForAddress(address);
            if (displayName == null || displayName.isEmpty()) {
                displayName = address;
            }
            
            // Create and show notification
            NotificationHelper notificationHelper = new NotificationHelper(context);
            notificationHelper.showSmsReceivedNotification(displayName, body, threadId);
            
            Log.d(TAG, "Showed notification for SMS from " + address);
        } catch (Exception e) {
            Log.e(TAG, "Error showing SMS notification", e);
        }
    }

    /**
     * Gets the contact name for a given address.
     *
     * @param address The phone number or address
     * @return The contact name, or null if not found
     */
    private String getContactNameForAddress(String address) {
        // This is a simplified implementation - you could enhance it with proper contact lookup
        return null;
    }

    /**
     * Handles an incoming MMS message.
     *
     * @param intent The intent containing the MMS message
     */
    public void handleIncomingMms(Intent intent) {
        try {
            Log.d(TAG, "Processing incoming MMS");
            
            // For now, show a simple MMS notification
            // This could be enhanced to parse MMS content in the future
            NotificationHelper notificationHelper = new NotificationHelper(context);
            notificationHelper.showMmsReceivedNotification("New MMS", "You have received a new MMS message");
            
            // Broadcast message received to refresh UI
            broadcastMessageReceived();
            
            Log.d(TAG, "Showed MMS notification");
        } catch (Exception e) {
            Log.e(TAG, "Error handling incoming MMS", e);
        }
    }

    /**
     * Broadcasts that a new message has been received to refresh the UI.
     */
    private void broadcastMessageReceived() {
        try {
            Intent broadcastIntent = new Intent("com.translator.messagingapp.MESSAGE_RECEIVED");
            context.sendBroadcast(broadcastIntent);
            Log.d(TAG, "Broadcasted message received event");
        } catch (Exception e) {
            Log.e(TAG, "Error broadcasting message received event", e);
        }
    }

    /**
     * Broadcasts that a new message has been sent to refresh the UI.
     * Includes a small delay to ensure the message is stored in the SMS database.
     */
    private void broadcastMessageSent() {
        // Use executor to add a small delay before broadcasting
        // This ensures the SMS is actually stored in the database before UI refresh
        executorService.execute(() -> {
            try {
                // Small delay to allow SMS system to store the message
                Thread.sleep(500);
                
                Intent broadcastIntent = new Intent("com.translator.messagingapp.MESSAGE_SENT");
                context.sendBroadcast(broadcastIntent);
                Log.d(TAG, "Broadcasted message sent event (with delay)");
            } catch (Exception e) {
                Log.e(TAG, "Error broadcasting message sent event", e);
                
                // Fallback: broadcast immediately if there's an error
                try {
                    Intent broadcastIntent = new Intent("com.translator.messagingapp.MESSAGE_SENT");
                    context.sendBroadcast(broadcastIntent);
                    Log.d(TAG, "Broadcasted message sent event (fallback)");
                } catch (Exception fallbackError) {
                    Log.e(TAG, "Error in fallback broadcast", fallbackError);
                }
            }
        });
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

    /**
     * Loads RCS messages for a thread.
     *
     * @param threadId The thread ID
     * @param messages The list to add messages to
     */
    private void loadRcsMessages(String threadId, List<Message> messages) {
        try {
            if (rcsService != null) {
                Log.d(TAG, "Loading RCS messages for thread " + threadId);
                List<RcsMessage> rcsMessages = rcsService.loadRcsMessages(threadId);
                if (rcsMessages != null && !rcsMessages.isEmpty()) {
                    Log.d(TAG, "Found " + rcsMessages.size() + " RCS messages for thread " + threadId);
                    messages.addAll(rcsMessages);
                } else {
                    Log.d(TAG, "No RCS messages found for thread " + threadId);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading RCS messages for thread " + threadId, e);
        }
    }

    /**
     * Loads RCS messages for a thread with pagination.
     *
     * @param threadId The thread ID
     * @param messages The list to add messages to
     * @param offset The number of messages to skip
     * @param limit The maximum number of messages to load
     */
    private void loadRcsMessagesPaginated(String threadId, List<Message> messages, int offset, int limit) {
        try {
            if (rcsService != null) {
                Log.d(TAG, "Loading paginated RCS messages for thread " + threadId + 
                      " (offset: " + offset + ", limit: " + limit + ")");
                List<RcsMessage> rcsMessages = rcsService.loadRcsMessages(threadId);
                if (rcsMessages != null && !rcsMessages.isEmpty()) {
                    // Apply pagination to RCS messages
                    Collections.sort(rcsMessages, (m1, m2) -> Long.compare(m2.getDate(), m1.getDate()));
                    
                    int startIndex = Math.min(offset, rcsMessages.size());
                    int endIndex = Math.min(offset + limit, rcsMessages.size());
                    
                    if (startIndex < endIndex) {
                        List<RcsMessage> paginatedRcsMessages = rcsMessages.subList(startIndex, endIndex);
                        Log.d(TAG, "Found " + paginatedRcsMessages.size() + " paginated RCS messages for thread " + threadId);
                        messages.addAll(paginatedRcsMessages);
                    } else {
                        Log.d(TAG, "No paginated RCS messages found for thread " + threadId + " at offset " + offset);
                    }
                } else {
                    Log.d(TAG, "No RCS messages found for thread " + threadId + " (paginated)");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading paginated RCS messages for thread " + threadId, e);
        }
    }
}
