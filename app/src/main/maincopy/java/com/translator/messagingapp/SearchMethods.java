package com.translator.messagingapp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for searching messages.
 */
public class SearchMethods {
    private static final String TAG = "SearchMethods";

    /**
     * Searches for messages containing the specified query.
     *
     * @param context The context
     * @param query The search query
     * @return A list of messages containing the query
     */
    public static List<Message> searchMessages(Context context, String query) {
        List<Message> results = new ArrayList<>();
        
        // Search SMS messages
        List<Message> smsResults = searchSmsMessages(context, query);
        if (smsResults != null) {
            results.addAll(smsResults);
        }
        
        // Search MMS messages
        List<Message> mmsResults = searchMmsMessages(context, query);
        if (mmsResults != null) {
            results.addAll(mmsResults);
        }
        
        return results;
    }
    
    /**
     * Searches for SMS messages containing the specified query.
     *
     * @param context The context
     * @param query The search query
     * @return A list of SMS messages containing the query
     */
    private static List<Message> searchSmsMessages(Context context, String query) {
        List<Message> results = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        
        // Define the columns to retrieve
        String[] projection = new String[] {
            Telephony.Sms._ID,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.THREAD_ID
        };
        
        // Define the selection criteria
        String selection = Telephony.Sms.BODY + " LIKE ?";
        String[] selectionArgs = new String[] { "%" + query + "%" };
        
        // Define the sort order
        String sortOrder = Telephony.Sms.DATE + " DESC";
        
        try {
            // Query the SMS content provider
            Cursor cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            );
            
            if (cursor != null) {
                // Get column indices
                int idIndex = cursor.getColumnIndex(Telephony.Sms._ID);
                int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
                int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);
                int typeIndex = cursor.getColumnIndex(Telephony.Sms.TYPE);
                int readIndex = cursor.getColumnIndex(Telephony.Sms.READ);
                int addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                int threadIdIndex = cursor.getColumnIndex(Telephony.Sms.THREAD_ID);
                
                // Process results
                if (cursor.moveToFirst()) {
                    do {
                        // Extract values
                        long id = cursor.getLong(idIndex);
                        String body = cursor.getString(bodyIndex);
                        long date = cursor.getLong(dateIndex);
                        int type = cursor.getInt(typeIndex);
                        
                        // Use safe defaults for optional columns
                        boolean read = readIndex >= 0 && cursor.getInt(readIndex) > 0;
                        String address = addressIndex >= 0 ? cursor.getString(addressIndex) : "";
                        long threadId = threadIdIndex >= 0 ? cursor.getLong(threadIdIndex) : 0;
                        
                        // Create message object
                        Message message = new Message();
                        message.setId(id);
                        message.setBody(body);
                        message.setDate(date);
                        message.setType(type);
                        message.setRead(read);
                        message.setAddress(address);
                        message.setThreadId(threadId);
                        message.setMessageType(Message.TYPE_SMS);
                        
                        // Set the search query for highlighting
                        message.setSearchQuery(query);
                        
                        // Add to results
                        results.add(message);
                        
                    } while (cursor.moveToNext());
                }
                
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error searching SMS messages", e);
        }
        
        return results;
    }
    
    /**
     * Searches for MMS messages containing the specified query.
     *
     * @param context The context
     * @param query The search query
     * @return A list of MMS messages containing the query
     */
    private static List<Message> searchMmsMessages(Context context, String query) {
        List<Message> results = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        
        // Define the columns to retrieve
        String[] projection = new String[] {
            Telephony.Mms._ID,
            Telephony.Mms.DATE,
            Telephony.Mms.MESSAGE_BOX,
            Telephony.Mms.READ,
            Telephony.Mms.THREAD_ID
        };
        
        // We can't directly search MMS content, so we'll get all MMS messages
        // and then filter them in code after retrieving their text content
        
        // Define the sort order
        String sortOrder = Telephony.Mms.DATE + " DESC";
        
        try {
            // Query the MMS content provider
            Cursor cursor = contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            );
            
            if (cursor != null) {
                // Get column indices
                int idIndex = cursor.getColumnIndex(Telephony.Mms._ID);
                int dateIndex = cursor.getColumnIndex(Telephony.Mms.DATE);
                int typeIndex = cursor.getColumnIndex(Telephony.Mms.MESSAGE_BOX);
                int readIndex = cursor.getColumnIndex(Telephony.Mms.READ);
                int threadIdIndex = cursor.getColumnIndex(Telephony.Mms.THREAD_ID);
                
                // Process results
                if (cursor.moveToFirst()) {
                    do {
                        // Extract values
                        long id = cursor.getLong(idIndex);
                        long date = cursor.getLong(dateIndex) * 1000; // MMS date is in seconds, convert to milliseconds
                        int type = cursor.getInt(typeIndex);
                        boolean read = readIndex >= 0 && cursor.getInt(readIndex) > 0;
                        long threadId = threadIdIndex >= 0 ? cursor.getLong(threadIdIndex) : 0;
                        
                        // Get MMS text content
                        MessageService messageService = ((TranslatorApp) context.getApplicationContext()).getMessageService();
                        String body = messageService.getMmsText(contentResolver, String.valueOf(id));
                        
                        // Get MMS address
                        String address = messageService.getMmsAddress(contentResolver, String.valueOf(id));
                        
                        // Only add messages that contain the query
                        if (body != null && body.toLowerCase().contains(query.toLowerCase())) {
                            // Create message object
                            Message message = new Message();
                            message.setId(id);
                            message.setBody(body);
                            message.setDate(date);
                            message.setType(type);
                            message.setAddress(address);
                            message.setRead(read);
                            message.setThreadId(threadId);
                            message.setMessageType(Message.TYPE_MMS);
                            
                            // Set the search query for highlighting
                            message.setSearchQuery(query);
                            
                            // Add to results
                            results.add(message);
                        }
                        
                    } while (cursor.moveToNext());
                }
                
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error searching MMS messages", e);
        }
        
        return results;
    }
}