package com.translator.messagingapp;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Methods for searching messages.
 */
public class SearchMethods {
    private static final String TAG = "SearchMethods";

    /**
     * Searches for messages containing the specified query text.
     *
     * @param contentResolver The content resolver
     * @param messageService The message service
     * @param query The search query
     * @return A list of messages matching the query
     */
    public static List<Message> searchMessages(ContentResolver contentResolver, MessageService messageService, String query) {
        List<Message> results = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        // Normalize the query for case-insensitive search
        String normalizedQuery = query.toLowerCase().trim();

        try {
            // Search SMS messages
            searchSmsMessages(contentResolver, messageService, normalizedQuery, results);

            // Search MMS messages
            searchMmsMessages(contentResolver, messageService, normalizedQuery, results);

            // Sort results by date (newest first)
            if (!results.isEmpty()) {
                results.sort((m1, m2) -> Long.compare(m2.getDate(), m1.getDate()));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error searching messages", e);
        }

        return results;
    }

    /**
     * Searches for SMS messages containing the specified query text.
     *
     * @param contentResolver The content resolver
     * @param messageService The message service
     * @param query The search query (normalized)
     * @param results The list to add results to
     */
    private static void searchSmsMessages(ContentResolver contentResolver, MessageService messageService, String query, List<Message> results) {
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
     * @param messageService The message service
     * @param query The search query (normalized)
     * @param results The list to add results to
     */
    private static void searchMmsMessages(ContentResolver contentResolver, MessageService messageService, String query, List<Message> results) {
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
                    String body = messageService.getMmsText(contentResolver, id);
                    String address = messageService.getMmsAddress(contentResolver, id, type);

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
}