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
        
        // Use optimized query to get conversation threads with all needed data in one go
        String unionQuery = buildOptimizedConversationQuery(offset, limit);
        
        try (Cursor cursor = contentResolver.rawQuery(unionQuery, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                Map<String, Conversation> conversationMap = new HashMap<>();
                
                do {
                    String threadId = cursor.getString(0); // thread_id
                    String address = cursor.getString(1);  // address
                    String snippet = cursor.getString(2);  // snippet/body
                    long date = cursor.getLong(3);         // date
                    int isRead = cursor.getInt(4);         // read status
                    int unreadCount = cursor.getInt(5);    // unread count
                    
                    // Check if we already have this conversation
                    Conversation conversation = conversationMap.get(threadId);
                    if (conversation == null) {
                        conversation = new Conversation();
                        conversation.setThreadId(threadId);
                        conversation.setAddress(address);
                        conversation.setSnippet(snippet != null ? snippet : "");
                        conversation.setLastMessage(snippet != null ? snippet : "");
                        conversation.setDate(date);
                        conversation.setRead(isRead == 1);
                        conversation.setUnreadCount(unreadCount);
                        
                        conversationMap.put(threadId, conversation);
                    }
                    
                } while (cursor.moveToNext());
                
                conversations.addAll(conversationMap.values());
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
        
        // Single query to get latest message and unread count for SMS
        String query = "SELECT address, body, date, read, " +
                      "(SELECT COUNT(*) FROM sms WHERE thread_id = ? AND read = 0) as unread_count " +
                      "FROM sms WHERE thread_id = ? ORDER BY date DESC LIMIT 1";
        
        try (Cursor cursor = contentResolver.rawQuery(query, new String[]{threadId, threadId})) {
            if (cursor != null && cursor.moveToFirst()) {
                String address = cursor.getString(0);
                String snippet = cursor.getString(1);
                long date = cursor.getLong(2);
                boolean read = cursor.getInt(3) == 1;
                int unreadCount = cursor.getInt(4);
                
                Conversation conversation = new Conversation();
                conversation.setThreadId(threadId);
                conversation.setAddress(address);
                conversation.setSnippet(snippet != null ? snippet : "");
                conversation.setLastMessage(snippet != null ? snippet : "");
                conversation.setDate(date);
                conversation.setRead(read);
                conversation.setUnreadCount(unreadCount);
                
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