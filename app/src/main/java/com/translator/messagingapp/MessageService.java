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
     * @param translationManager The translation manager
     * @param translationCache The translation cache
     */
    public MessageService(Context context, TranslationManager translationManager, TranslationCache translationCache) {
        this.context = context;
        this.executorService = Executors.newCachedThreadPool();
        this.translationManager = translationManager;
        this.translationCache = translationCache;
    }

    /**
     * Loads all conversations.
     *
     * @return A list of conversations
     */
    public List<Conversation> loadConversations() {
        List<Conversation> conversations = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();

        // Query the SMS content provider for conversations
        Uri uri = Uri.parse("content://sms/conversations");
        Cursor cursor = null;

        try {
            cursor = contentResolver.query(uri, null, null, null, "date DESC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    // Get thread_id
                    int threadIdIndex = cursor.getColumnIndex("thread_id");
                    if (threadIdIndex < 0) {
                        Log.e(TAG, "thread_id column not found");
                        continue;
                    }

                    String threadId = cursor.getString(threadIdIndex);
                    if (threadId == null) {
                        Log.e(TAG, "thread_id is null");
                        continue;
                    }

                    // Load conversation details
                    Conversation conversation = loadConversationDetails(threadId);
                    if (conversation != null) {
                        conversations.add(conversation);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading conversations", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return conversations;
    }

    /**
     * Loads the details of a conversation.
     *
     * @param threadId The thread ID
     * @return The conversation
     */
    private Conversation loadConversationDetails(String threadId) {
        ContentResolver contentResolver = context.getContentResolver();
        Conversation conversation = new Conversation();
        conversation.setThreadId(threadId);

        // Query the SMS content provider for the latest message in this thread
        Uri uri = Uri.parse("content://sms");
        String selection = "thread_id = ?";
        String[] selectionArgs = { threadId };
        String sortOrder = "date DESC";
        Cursor cursor = null;

        try {
            cursor = contentResolver.query(uri, null, selection, selectionArgs, sortOrder);

            if (cursor != null && cursor.moveToFirst()) {
                // Get message details
                int addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
                int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);
                int typeIndex = cursor.getColumnIndex(Telephony.Sms.TYPE);
                int readIndex = cursor.getColumnIndex(Telephony.Sms.READ);

                // Skip if we don't have required columns
                if (addressIndex < 0 || bodyIndex < 0 || dateIndex < 0) {
                    Log.e(TAG, "Required columns not found");
                    return null;
                }

                // Get values
                String address = cursor.getString(addressIndex);
                String snippet = cursor.getString(bodyIndex);
                long date = cursor.getLong(dateIndex);
                int type = typeIndex >= 0 ? cursor.getInt(typeIndex) : Telephony.Sms.MESSAGE_TYPE_INBOX;
                boolean read = readIndex >= 0 && cursor.getInt(readIndex) > 0;

                // Set conversation details
                conversation.setAddress(address);
                conversation.setSnippet(snippet);
                conversation.setDate(date);
                conversation.setType(type);
                conversation.setRead(read);

                // Get contact name
                String contactName = ContactUtils.getContactName(context, address);
                conversation.setContactName(contactName);

                // Count unread messages
                int unreadCount = countUnreadMessages(threadId);
                conversation.setUnreadCount(unreadCount);

                return conversation;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading conversation details", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // Try MMS if SMS failed or returned no results
        return loadMmsConversationDetails(threadId);
    }

    /**
     * Loads the details of an MMS conversation.
     *
     * @param threadId The thread ID
     * @return The conversation
     */
    private Conversation loadMmsConversationDetails(String threadId) {
        ContentResolver contentResolver = context.getContentResolver();
        Conversation conversation = new Conversation();
        conversation.setThreadId(threadId);

        // Query the MMS content provider for the latest message in this thread
        Uri uri = Uri.parse("content://mms");
        String selection = "thread_id = ?";
        String[] selectionArgs = { threadId };
        String sortOrder = "date DESC";
        Cursor cursor = null;

        try {
            cursor = contentResolver.query(uri, null, selection, selectionArgs, sortOrder);

            if (cursor != null && cursor.moveToFirst()) {
                // Get message details
                int idIndex = cursor.getColumnIndex(Telephony.Mms._ID);
                int dateIndex = cursor.getColumnIndex(Telephony.Mms.DATE);
                int readIndex = cursor.getColumnIndex(Telephony.Mms.READ);

                // Skip if we don't have required columns
                if (idIndex < 0 || dateIndex < 0) {
                    Log.e(TAG, "Required MMS columns not found");
                    return null;
                }

                // Get values
                String id = cursor.getString(idIndex);
                long date = cursor.getLong(dateIndex) * 1000; // MMS date is in seconds, convert to milliseconds
                boolean read = readIndex >= 0 && cursor.getInt(readIndex) > 0;

                // Get address from MMS addr table
                String address = getMmsAddress(contentResolver, id, Telephony.Mms.MESSAGE_BOX_INBOX);
                if (address == null) {
                    Log.e(TAG, "Could not get MMS address");
                    return null;
                }

                // Get MMS text
                String snippet = getMmsText(contentResolver, id);
                if (snippet == null) {
                    snippet = "[MMS]"; // Fallback if no text found
                }

                // Set conversation details
                conversation.setAddress(address);
                conversation.setSnippet(snippet);
                conversation.setDate(date);
                conversation.setType(Telephony.Mms.MESSAGE_BOX_INBOX); // Default to inbox
                conversation.setRead(read);

                // Get contact name
                String contactName = ContactUtils.getContactName(context, address);
                conversation.setContactName(contactName);

                // Count unread messages
                int unreadCount = countUnreadMessages(threadId);
                conversation.setUnreadCount(unreadCount);

                return conversation;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading MMS conversation details", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    /**
     * Gets the address from an MMS message.
     *
     * @param contentResolver The content resolver
     * @param messageId The message ID
     * @param messageBox The message box (inbox, outbox, etc.)
     * @return The address
     */
    public String getMmsAddress(ContentResolver contentResolver, String messageId, int messageBox) {
        String address = null;
        Cursor cursor = null;

        try {
            Uri uri = Uri.parse("content://mms/" + messageId + "/addr");
            String selection;

            if (messageBox == Telephony.Mms.MESSAGE_BOX_INBOX) {
                // For received messages, get the sender's address
                selection = "type=" + TYPE_TO;
            } else {
                // For sent messages, get the recipient's address
                selection = "type=" + TYPE_TO;
            }

            cursor = contentResolver.query(uri, null, selection, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int addressIndex = cursor.getColumnIndex("address");
                if (addressIndex >= 0) {
                    address = cursor.getString(addressIndex);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting MMS address", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return address;
    }

    /**
     * Gets the text content from an MMS message.
     *
     * @param contentResolver The content resolver
     * @param messageId The message ID
     * @return The text content
     */
    public String getMmsText(ContentResolver contentResolver, String messageId) {
        String text = null;
        Cursor cursor = null;

        try {
            Uri uri = Uri.parse("content://mms/part");
            String selection = "mid=? AND ct=?";
            String[] selectionArgs = { messageId, "text/plain" };
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null);

            if (cursor != null && cursor.moveToFirst()) {
                int textIndex = cursor.getColumnIndex("text");
                int dataIndex = cursor.getColumnIndex("_data");

                if (textIndex >= 0) {
                    // Text is stored directly in the table
                    text = cursor.getString(textIndex);
                } else if (dataIndex >= 0) {
                    // Text is stored in a file
                    String dataPath = cursor.getString(dataIndex);
                    if (dataPath != null) {
                        text = getMmsTextFromFile(contentResolver, dataPath);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting MMS text", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return text;
    }

    /**
     * Gets the text content from an MMS file.
     *
     * @param contentResolver The content resolver
     * @param dataPath The data path
     * @return The text content
     */
    private String getMmsTextFromFile(ContentResolver contentResolver, String dataPath) {
        InputStream inputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = null;

        try {
            inputStream = contentResolver.openInputStream(Uri.parse("content://mms/part/" + dataPath));
            if (inputStream == null) {
                return null;
            }

            byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            return new String(byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            Log.e(TAG, "Error reading MMS text from file", e);
            return null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (byteArrayOutputStream != null) {
                    byteArrayOutputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams", e);
            }
        }
    }

    /**
     * Counts the number of unread messages in a thread.
     *
     * @param threadId The thread ID
     * @return The number of unread messages
     */
    private int countUnreadMessages(String threadId) {
        ContentResolver contentResolver = context.getContentResolver();
        int unreadCount = 0;
        Cursor cursor = null;

        try {
            // Count unread SMS messages
            Uri uri = Uri.parse("content://sms");
            String selection = "thread_id = ? AND read = 0";
            String[] selectionArgs = { threadId };
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null);
            if (cursor != null) {
                unreadCount += cursor.getCount();
                cursor.close();
                cursor = null;
            }

            // Count unread MMS messages
            uri = Uri.parse("content://mms");
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null);
            if (cursor != null) {
                unreadCount += cursor.getCount();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error counting unread messages", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return unreadCount;
    }

    /**
     * Loads messages for a conversation.
     *
     * @param threadId The thread ID
     * @return A list of messages
     */
    public List<Message> loadMessages(String threadId) {
        List<Message> messages = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();

        // Load SMS messages
        loadSmsMessages(contentResolver, threadId, messages);

        // Load MMS messages
        loadMmsMessages(contentResolver, threadId, messages);

        // Sort messages by date
        messages.sort((m1, m2) -> Long.compare(m1.getDate(), m2.getDate()));

        return messages;
    }

    /**
     * Loads SMS messages for a conversation.
     *
     * @param contentResolver The content resolver
     * @param threadId The thread ID
     * @param messages The list to add messages to
     */
    private void loadSmsMessages(ContentResolver contentResolver, String threadId, List<Message> messages) {
        Cursor cursor = null;

        try {
            Uri uri = Uri.parse("content://sms");
            String selection = "thread_id = ?";
            String[] selectionArgs = { threadId };
            String sortOrder = "date ASC";
            cursor = contentResolver.query(uri, null, selection, selectionArgs, sortOrder);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    // Get message details
                    int idIndex = cursor.getColumnIndex(Telephony.Sms._ID);
                    int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
                    int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);
                    int typeIndex = cursor.getColumnIndex(Telephony.Sms.TYPE);
                    int addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                    int readIndex = cursor.getColumnIndex(Telephony.Sms.READ);

                    // Skip if we don't have required columns
                    if (idIndex < 0 || bodyIndex < 0 || dateIndex < 0 || typeIndex < 0) {
                        Log.e(TAG, "Required SMS columns not found");
                        continue;
                    }

                    // Get values
                    String id = cursor.getString(idIndex);
                    String body = cursor.getString(bodyIndex);
                    long date = cursor.getLong(dateIndex);
                    int type = cursor.getInt(typeIndex);
                    String address = addressIndex >= 0 ? cursor.getString(addressIndex) : "";
                    boolean read = readIndex >= 0 && cursor.getInt(readIndex) > 0;

                    // Create message
                    Message message = new Message();
                    message.setId(Long.parseLong(id));
                    message.setBody(body);
                    message.setDate(date);
                    message.setType(type);
                    message.setAddress(address);
                    message.setRead(read);
                    message.setThreadId(Long.parseLong(threadId));
                    message.setMessageType(Message.TYPE_SMS);

                    // Check if this message has a translation
                    if (translationCache != null) {
                        message.restoreTranslationState(translationCache);
                    }

                    // Add to list
                    messages.add(message);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading SMS messages", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Loads MMS messages for a conversation.
     *
     * @param contentResolver The content resolver
     * @param threadId The thread ID
     * @param messages The list to add messages to
     */
    private void loadMmsMessages(ContentResolver contentResolver, String threadId, List<Message> messages) {
        Cursor cursor = null;

        try {
            Uri uri = Uri.parse("content://mms");
            String selection = "thread_id = ?";
            String[] selectionArgs = { threadId };
            String sortOrder = "date ASC";
            cursor = contentResolver.query(uri, null, selection, selectionArgs, sortOrder);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    // Get message details
                    int idIndex = cursor.getColumnIndex(Telephony.Mms._ID);
                    int dateIndex = cursor.getColumnIndex(Telephony.Mms.DATE);
                    int typeIndex = cursor.getColumnIndex(Telephony.Mms.MESSAGE_BOX);
                    int readIndex = cursor.getColumnIndex(Telephony.Mms.READ);

                    // Skip if we don't have required columns
                    if (idIndex < 0 || dateIndex < 0 || typeIndex < 0) {
                        Log.e(TAG, "Required MMS columns not found");
                        continue;
                    }

                    // Get values
                    String id = cursor.getString(idIndex);
                    long date = cursor.getLong(dateIndex) * 1000; // MMS date is in seconds, convert to milliseconds
                    int type = cursor.getInt(typeIndex);
                    boolean read = readIndex >= 0 && cursor.getInt(readIndex) > 0;

                    // Get address
                    String address = getMmsAddress(contentResolver, id, type);
                    if (address == null) {
                        Log.e(TAG, "Could not get MMS address");
                        continue;
                    }

                    // Get text content
                    String body = getMmsText(contentResolver, id);

                    // Create MMS message
                    MmsMessage message = new MmsMessage();
                    message.setId(Long.parseLong(id));
                    message.setBody(body);
                    message.setDate(date);
                    message.setType(type);
                    message.setAddress(address);
                    message.setRead(read);
                    message.setThreadId(Long.parseLong(threadId));
                    message.setMessageType(Message.TYPE_MMS);

                    // Load attachments
                    loadMmsAttachments(contentResolver, id, message);

                    // Check if this message has a translation
                    if (translationCache != null) {
                        message.restoreTranslationState(translationCache);
                    }

                    // Add to list
                    messages.add(message);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading MMS messages", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Loads attachments for an MMS message.
     *
     * @param contentResolver The content resolver
     * @param messageId The message ID
     * @param message The MMS message
     */
    private void loadMmsAttachments(ContentResolver contentResolver, String messageId, MmsMessage message) {
        Cursor cursor = null;

        try {
            Uri uri = Uri.parse("content://mms/part");
            String selection = "mid=?";
            String[] selectionArgs = { messageId };
            cursor = contentResolver.query(uri, null, selection, selectionArgs, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int contentTypeIndex = cursor.getColumnIndex("ct");
                    int nameIndex = cursor.getColumnIndex("name");
                    int dataIndex = cursor.getColumnIndex("_data");
                    int textIndex = cursor.getColumnIndex("text");
                    int idIndex = cursor.getColumnIndex("_id");

                    if (contentTypeIndex < 0 || idIndex < 0) {
                        continue;
                    }

                    String contentType = cursor.getString(contentTypeIndex);
                    String name = nameIndex >= 0 ? cursor.getString(nameIndex) : null;
                    String data = dataIndex >= 0 ? cursor.getString(dataIndex) : null;
                    String text = textIndex >= 0 ? cursor.getString(textIndex) : null;
                    String partId = cursor.getString(idIndex);

                    // Skip text/plain parts as they are handled separately
                    if ("text/plain".equals(contentType)) {
                        continue;
                    }

                    // Create attachment
                    // Create a Uri for the attachment
                    Uri attachmentUri = Uri.parse("content://mms/part/" + partId);
                    
                    // Create the attachment with the available information
                    MmsMessage.Attachment attachment = new MmsMessage.Attachment(
                            attachmentUri,
                            contentType,
                            name != null ? name : "attachment",
                            data != null ? data.length() : 0
                    );

                    // Add to message
                    message.addAttachment(attachment);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading MMS attachments", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Sends an SMS message.
     *
     * @param address The recipient address
     * @param body The message body
     * @return True if the message was sent successfully
     */
    public boolean sendSmsMessage(String address, String body) {
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

            // Add the message to the sent folder
            ContentValues values = new ContentValues();
            values.put(Telephony.Sms.ADDRESS, address);
            values.put(Telephony.Sms.BODY, body);
            values.put(Telephony.Sms.DATE, System.currentTimeMillis());
            values.put(Telephony.Sms.READ, 1);
            values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT);

            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = contentResolver.insert(Telephony.Sms.CONTENT_URI, values);

            return uri != null;
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
        // This is a simplified implementation
        // In a real app, you would need to use the MMS API to send the message
        // For now, we'll just add a placeholder message to the sent folder
        try {
            ContentValues values = new ContentValues();
            values.put(Telephony.Mms.SUBJECT, subject);
            values.put(Telephony.Mms.DATE, System.currentTimeMillis() / 1000); // MMS date is in seconds
            values.put(Telephony.Mms.READ, 1);
            values.put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_SENT);
            values.put(Telephony.Mms.CONTENT_TYPE, "application/vnd.wap.multipart.related");
            values.put(Telephony.Mms.MESSAGE_TYPE, MESSAGE_TYPE_SEND_REQ);

            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = contentResolver.insert(Telephony.Mms.CONTENT_URI, values);

            if (uri != null) {
                String messageId = uri.getLastPathSegment();

                // Add address part
                ContentValues addrValues = new ContentValues();
                addrValues.put("address", PhoneNumberUtils.stripSeparators(address));
                addrValues.put("charset", "106");
                addrValues.put("type", TYPE_TO);
                addrValues.put("msg_id", messageId);
                contentResolver.insert(Uri.parse("content://mms/" + messageId + "/addr"), addrValues);

                // Add text part if body is not empty
                if (!TextUtils.isEmpty(body)) {
                    ContentValues textValues = new ContentValues();
                    textValues.put("mid", messageId);
                    textValues.put("ct", "text/plain");
                    textValues.put("text", body);
                    contentResolver.insert(Uri.parse("content://mms/part"), textValues);
                }

                // Add attachment parts
                if (attachments != null) {
                    for (Uri attachmentUri : attachments) {
                        try {
                            // Get content type
                            String contentType = contentResolver.getType(attachmentUri);
                            if (contentType == null) {
                                // Try to guess content type from file extension
                                String path = attachmentUri.getPath();
                                if (path != null) {
                                    String extension = MimeTypeMap.getFileExtensionFromUrl(path);
                                    if (extension != null) {
                                        contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                                    }
                                }
                                if (contentType == null) {
                                    contentType = "application/octet-stream";
                                }
                            }

                            // Create part
                            ContentValues partValues = new ContentValues();
                            partValues.put("mid", messageId);
                            partValues.put("ct", contentType);
                            partValues.put("name", "attachment");

                            // Insert part
                            Uri partUri = contentResolver.insert(Uri.parse("content://mms/part"), partValues);
                            if (partUri != null) {
                                // Copy data
                                InputStream inputStream = null;
                                OutputStream outputStream = null;
                                try {
                                    inputStream = contentResolver.openInputStream(attachmentUri);
                                    outputStream = contentResolver.openOutputStream(partUri);
                                    if (inputStream != null && outputStream != null) {
                                        byte[] buffer = new byte[1024];
                                        int length;
                                        while ((length = inputStream.read(buffer)) > 0) {
                                            outputStream.write(buffer, 0, length);
                                        }
                                    }
                                } finally {
                                    if (inputStream != null) {
                                        inputStream.close();
                                    }
                                    if (outputStream != null) {
                                        outputStream.close();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error adding attachment", e);
                        }
                    }
                }

                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending MMS message", e);
        }

        return false;
    }

    /**
     * Adds a test message.
     *
     * @return True if the message was added successfully
     */
    public boolean addTestMessage() {
        try {
            ContentValues values = new ContentValues();
            values.put(Telephony.Sms.ADDRESS, "1234567890");
            values.put(Telephony.Sms.BODY, "This is a test message created at " + new java.util.Date());
            values.put(Telephony.Sms.DATE, System.currentTimeMillis());
            values.put(Telephony.Sms.READ, 0);
            values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX);

            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = contentResolver.insert(Telephony.Sms.CONTENT_URI, values);

            return uri != null;
        } catch (Exception e) {
            Log.e(TAG, "Error adding test message", e);
            return false;
        }
    }

    /**
     * Deletes a conversation.
     *
     * @param threadId The thread ID
     * @return True if the conversation was deleted successfully
     */
    public boolean deleteConversation(String threadId) {
        ContentResolver contentResolver = context.getContentResolver();
        try {
            // Delete SMS messages
            Uri smsUri = Uri.parse("content://sms");
            String smsSelection = "thread_id = ?";
            String[] smsSelectionArgs = { threadId };
            int smsDeleted = contentResolver.delete(smsUri, smsSelection, smsSelectionArgs);

            // Delete MMS messages
            Uri mmsUri = Uri.parse("content://mms");
            String mmsSelection = "thread_id = ?";
            String[] mmsSelectionArgs = { threadId };
            int mmsDeleted = contentResolver.delete(mmsUri, mmsSelection, mmsSelectionArgs);

            return smsDeleted > 0 || mmsDeleted > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting conversation", e);
            return false;
        }
    }

    /**
     * Marks a conversation as read.
     *
     * @param threadId The thread ID
     * @return True if the conversation was marked as read successfully
     */
    public boolean markThreadAsRead(String threadId) {
        ContentResolver contentResolver = context.getContentResolver();
        try {
            ContentValues values = new ContentValues();
            values.put(Telephony.Sms.READ, 1);

            // Mark SMS messages as read
            Uri smsUri = Uri.parse("content://sms");
            String smsSelection = Telephony.Sms.THREAD_ID + " = ?";
            String[] smsSelectionArgs = { threadId };
            int smsUpdated = contentResolver.update(smsUri, values, smsSelection, smsSelectionArgs);

            // Mark MMS messages as read
            Uri mmsUri = Uri.parse("content://mms");
            String mmsSelection = Telephony.Mms.THREAD_ID + " = ?";
            String[] mmsSelectionArgs = { threadId };
            int mmsUpdated = contentResolver.update(mmsUri, values, mmsSelection, mmsSelectionArgs);

            return smsUpdated > 0 || mmsUpdated > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error marking thread as read", e);
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
                    // Get message data
                    int idIndex = cursor.getColumnIndex(Telephony.Sms._ID);
                    int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
                    int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);
                    int typeIndex = cursor.getColumnIndex(Telephony.Sms.TYPE);
                    int addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                    int readIndex = cursor.getColumnIndex(Telephony.Sms.READ);
                    int threadIdIndex = cursor.getColumnIndex(Telephony.Sms.THREAD_ID);
                    
                    // Skip if we don't have required columns
                    if (idIndex < 0 || bodyIndex < 0 || dateIndex < 0 || typeIndex < 0) {
                        Log.w(TAG, "Skipping SMS message due to missing required columns");
                        continue;
                    }
                    
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
                    message.setMessageType(Message.TYPE_SMS);
                    
                    // Set the search query for highlighting
                    message.setSearchQuery(query);
                    
                    // Add to results
                    results.add(message);
                    
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error searching SMS messages", e);
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
            // First, get all MMS messages
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
                    // Get message data
                    int idIndex = cursor.getColumnIndex(Telephony.Mms._ID);
                    int dateIndex = cursor.getColumnIndex(Telephony.Mms.DATE);
                    int typeIndex = cursor.getColumnIndex(Telephony.Mms.MESSAGE_BOX);
                    int readIndex = cursor.getColumnIndex(Telephony.Mms.READ);
                    int threadIdIndex = cursor.getColumnIndex(Telephony.Mms.THREAD_ID);
                    
                    // Skip if we don't have required columns
                    if (idIndex < 0 || dateIndex < 0 || typeIndex < 0) {
                        Log.w(TAG, "Skipping MMS message due to missing required columns");
                        continue;
                    }
                    
                    String id = cursor.getString(idIndex);
                    long date = cursor.getLong(dateIndex) * 1000; // MMS date is in seconds, convert to milliseconds
                    int type = cursor.getInt(typeIndex);
                    boolean read = readIndex >= 0 && cursor.getInt(readIndex) > 0;
                    String threadId = threadIdIndex >= 0 ? cursor.getString(threadIdIndex) : "";
                    
                    // Get MMS text content
                    String body = getMmsText(contentResolver, id);
                    
                    // Skip if no text content or doesn't match query
                    if (body == null || !body.toLowerCase().contains(query)) {
                        continue;
                    }
                    
                    // Get address (from or to)
                    String address = getMmsAddress(contentResolver, id, type);
                    
                    // Create message object
                    Message message = new MmsMessage();
                    message.setId(Long.parseLong(id));
                    message.setBody(body);
                    message.setDate(date);
                    message.setType(type);
                    message.setAddress(address);
                    message.setRead(read);
                    message.setThreadId(Long.parseLong(threadId));
                    message.setMessageType(Message.TYPE_MMS);
                    
                    // Set the search query for highlighting
                    message.setSearchQuery(query);
                    
                    // Add to results
                    results.add(message);
                    
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error searching MMS messages", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Cleans up resources.
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
}