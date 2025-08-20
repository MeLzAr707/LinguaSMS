package com.translator.messagingapp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Optimized service for loading conversations with pagination and batch queries.
 * Addresses performance bottlenecks in conversation list loading.
 */
public class OptimizedConversationService {
    private static final String TAG = "OptimizedConversationService";
    private final Context context;
    private final OptimizedMessageCache cache;
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();
    
    // Default page size for conversation loading
    public static final int DEFAULT_PAGE_SIZE = 20;
    
    public OptimizedConversationService(Context context) {
        this.context = context;
        this.cache = new OptimizedMessageCache();
    }
    
    /**
     * Callback interface for asynchronous conversation loading.
     */
    public interface ConversationLoadCallback {
        void onConversationsLoaded(List<Conversation> conversations, boolean hasMore);
        void onError(Exception error);
    }
    
    /**
     * Loads conversations with pagination to improve startup performance.
     *
     * @param offset The offset to start loading from
     * @param limit The maximum number of conversations to load
     * @param callback The callback to invoke with results
     */
    public void loadConversationsPaginated(int offset, int limit, ConversationLoadCallback callback) {
        backgroundExecutor.execute(() -> {
            try {
                List<Conversation> conversations = loadConversationsBatch(offset, limit);
                boolean hasMore = conversations.size() >= limit;
                
                // Cache loaded conversations
                for (Conversation conversation : conversations) {
                    cache.cacheConversation(conversation.getThreadId(), conversation);
                }
                
                callback.onConversationsLoaded(conversations, hasMore);
                Log.d(TAG, "Loaded " + conversations.size() + " conversations (offset: " + offset + ", limit: " + limit + ")");
            } catch (Exception e) {
                Log.e(TAG, "Error loading conversations", e);
                callback.onError(e);
            }
        });
    }
    
    /**
     * Loads a batch of conversations using optimized queries.
     */
    private List<Conversation> loadConversationsBatch(int offset, int limit) {
        List<Conversation> conversations = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        
        // Since ContentResolver doesn't support raw SQL queries, use the basic method approach
        // which works with proper ContentResolver.query() calls
        try {
            // Query conversation threads using the standard Android API
            Uri uri = Uri.parse("content://mms-sms/conversations?simple=true");
            String[] projection = {
                    Telephony.Threads._ID,
                    Telephony.Threads.DATE,
                    Telephony.Threads.MESSAGE_COUNT,
                    Telephony.Threads.RECIPIENT_IDS,
                    Telephony.Threads.SNIPPET,
                    Telephony.Threads.READ
            };
            String sortOrder = Telephony.Threads.DATE + " DESC LIMIT " + limit + " OFFSET " + offset;
            
            try (Cursor cursor = contentResolver.query(uri, projection, null, null, sortOrder)) {
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String threadId = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Threads._ID));
                        
                        // Check cache first
                        Conversation cached = cache.getCachedConversation(threadId);
                        if (cached != null) {
                            conversations.add(cached);
                            continue;
                        }
                        
                        // Load conversation details using efficient method
                        Conversation conversation = loadConversationDetailsEfficient(threadId);
                        if (conversation != null) {
                            conversations.add(conversation);
                            cache.cacheConversation(threadId, conversation);
                        }
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in optimized conversation query, falling back to basic method", e);
            // Fallback to basic method if optimized query fails
            conversations = loadConversationsBasic(offset, limit);
        }
        
        return conversations;
    }
    
    /**
     * Builds an optimized SQL query that combines SMS and MMS data with unread counts.
     */
    private String buildOptimizedConversationQuery(int offset, int limit) {
        // This query combines SMS and MMS data and calculates unread counts in a single operation
        return "SELECT DISTINCT " +
               "threads.thread_id, " +
               "threads.address, " +
               "threads.snippet, " +
               "threads.date, " +
               "threads.read, " +
               "threads.unread_count " +
               "FROM (" +
               "  SELECT " +
               "    thread_id, " +
               "    address, " +
               "    body as snippet, " +
               "    date, " +
               "    read, " +
               "    (SELECT COUNT(*) FROM sms s2 WHERE s2.thread_id = sms.thread_id AND s2.read = 0) as unread_count, " +
               "    ROW_NUMBER() OVER (PARTITION BY thread_id ORDER BY date DESC) as rn " +
               "  FROM sms " +
               "  UNION ALL " +
               "  SELECT " +
               "    thread_id, " +
               "    (SELECT addr.address FROM mms_addresses addr WHERE addr.msg_id = mms._id AND addr.type = 137 LIMIT 1) as address, " +
               "    '[MMS]' as snippet, " +
               "    date * 1000 as date, " +
               "    read, " +
               "    (SELECT COUNT(*) FROM mms m2 WHERE m2.thread_id = mms.thread_id AND m2.read = 0) as unread_count, " +
               "    ROW_NUMBER() OVER (PARTITION BY thread_id ORDER BY date DESC) as rn " +
               "  FROM mms " +
               ") as threads " +
               "WHERE threads.rn = 1 " +
               "ORDER BY threads.date DESC " +
               "LIMIT " + limit + " OFFSET " + offset;
    }
    
    /**
     * Fallback method using basic queries if optimized query fails.
     */
    private List<Conversation> loadConversationsBasic(int offset, int limit) {
        List<Conversation> conversations = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        
        // Query conversation threads
        Uri uri = Uri.parse("content://mms-sms/conversations?simple=true");
        String[] projection = {
                Telephony.Threads._ID,
                Telephony.Threads.DATE,
                Telephony.Threads.MESSAGE_COUNT,
                Telephony.Threads.RECIPIENT_IDS,
                Telephony.Threads.SNIPPET,
                Telephony.Threads.READ
        };
        String sortOrder = Telephony.Threads.DATE + " DESC LIMIT " + limit + " OFFSET " + offset;
        
        try (Cursor cursor = contentResolver.query(uri, projection, null, null, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String threadId = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Threads._ID));
                    
                    // Check cache first
                    Conversation cached = cache.getCachedConversation(threadId);
                    if (cached != null) {
                        conversations.add(cached);
                        continue;
                    }
                    
                    // Load conversation details using efficient method
                    Conversation conversation = loadConversationDetailsEfficient(threadId);
                    if (conversation != null) {
                        conversations.add(conversation);
                        cache.cacheConversation(threadId, conversation);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in basic conversation loading", e);
        }
        
        return conversations;
    }
    
    /**
     * Loads conversation details using a more efficient single-query approach.
     */
    private Conversation loadConversationDetailsEfficient(String threadId) {
        ContentResolver contentResolver = context.getContentResolver();
        
        // Use standard ContentResolver queries instead of raw SQL
        // First, get the latest SMS message for this thread
        String address = null;
        String snippet = null;
        long date = 0;
        boolean read = true;
        int unreadCount = 0;
        
        try {
            // Query SMS messages for this thread
            Uri smsUri = Telephony.Sms.CONTENT_URI;
            String[] projection = {
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.READ
            };
            String selection = Telephony.Sms.THREAD_ID + " = ?";
            String[] selectionArgs = {threadId};
            String sortOrder = Telephony.Sms.DATE + " DESC LIMIT 1";
            
            try (Cursor cursor = contentResolver.query(smsUri, projection, selection, selectionArgs, sortOrder)) {
                if (cursor != null && cursor.moveToFirst()) {
                    address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    snippet = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                    read = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1;
                }
            }
            
            // Get unread count for this thread
            String unreadSelection = Telephony.Sms.THREAD_ID + " = ? AND " + Telephony.Sms.READ + " = 0";
            try (Cursor unreadCursor = contentResolver.query(smsUri, new String[]{"COUNT(*)"}, unreadSelection, selectionArgs, null)) {
                if (unreadCursor != null && unreadCursor.moveToFirst()) {
                    unreadCount = unreadCursor.getInt(0);
                }
            }
            
            // If we didn't find SMS data, try MMS
            if (address == null) {
                // Check MMS messages for this thread
                Uri mmsUri = Telephony.Mms.CONTENT_URI;
                String[] mmsProjection = {
                    Telephony.Mms.DATE,
                    Telephony.Mms.READ
                };
                String mmsSelection = Telephony.Mms.THREAD_ID + " = ?";
                String mmsSortOrder = Telephony.Mms.DATE + " DESC LIMIT 1";
                
                try (Cursor mmsCursor = contentResolver.query(mmsUri, mmsProjection, mmsSelection, selectionArgs, mmsSortOrder)) {
                    if (mmsCursor != null && mmsCursor.moveToFirst()) {
                        date = mmsCursor.getLong(mmsCursor.getColumnIndexOrThrow(Telephony.Mms.DATE)) * 1000; // Convert to milliseconds
                        read = mmsCursor.getInt(mmsCursor.getColumnIndexOrThrow(Telephony.Mms.READ)) == 1;
                        snippet = "[MMS]";
                        
                        // For MMS, we need to get the address from a different table
                        // This is a simplified approach - in production you'd query the MMS address table
                        address = "Unknown";
                    }
                }
            }
            
            if (address != null) {
                Conversation conversation = new Conversation();
                conversation.setThreadId(threadId);
                conversation.setAddress(address);
                conversation.setSnippet(snippet != null ? snippet : "");
                conversation.setLastMessage(snippet != null ? snippet : "");
                conversation.setDate(date);
                conversation.setRead(read);
                conversation.setUnreadCount(unreadCount);
                
                // Look up contact name for the address
                String contactName = ContactUtils.getContactName(context, address);
                conversation.setContactName(contactName);
                
                return conversation;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in efficient conversation details loading for thread " + threadId, e);
        }
        
        return null;
    }
    
    /**
     * Clears the conversation cache.
     */
    public void clearCache() {
        cache.clearCache();
    }
    
    /**
     * Gets cache statistics for debugging.
     */
    public String getCacheStats() {
        return cache.getCacheStats();
    }
}