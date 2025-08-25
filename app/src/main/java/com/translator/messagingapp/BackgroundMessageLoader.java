package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Background service for preloading messages to enable smooth scrolling
 * and improved user experience when navigating message history.
 */
public class BackgroundMessageLoader {
    private static final String TAG = "BackgroundMessageLoader";
    private static final int PREFETCH_PAGE_SIZE = 50;
    private static final int MAX_PREFETCH_PAGES = 3;
    private static final long PREFETCH_DELAY_MS = 1000; // 1 second delay before prefetching
    
    private final Context context;
    private final MessageService messageService;
    private final OptimizedMessageCache cache;
    private final ExecutorService prefetchExecutor;
    
    // Track ongoing prefetch operations to avoid duplicates
    private final Set<String> activePrefetches = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean isEnabled = new AtomicBoolean(true);
    
    public BackgroundMessageLoader(Context context, MessageService messageService, OptimizedMessageCache cache) {
        this.context = context;
        this.messageService = messageService;
        this.cache = cache;
        this.prefetchExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "BackgroundMessageLoader");
            t.setPriority(Thread.MIN_PRIORITY); // Low priority to not interfere with UI
            return t;
        });
    }
    
    /**
     * Schedules background prefetching for older messages in a conversation.
     * This is called when a user starts viewing a conversation.
     *
     * @param threadId The thread ID to prefetch messages for
     * @param currentMessageCount Number of messages currently loaded
     */
    public void scheduleMessagePrefetch(String threadId, int currentMessageCount) {
        if (!isEnabled.get() || threadId == null || threadId.isEmpty()) {
            return;
        }
        
        // Check if already prefetching for this thread
        if (activePrefetches.contains(threadId)) {
            Log.d(TAG, "Prefetch already active for thread: " + threadId);
            return;
        }
        
        Log.d(TAG, "Scheduling prefetch for thread: " + threadId + 
              " (current messages: " + currentMessageCount + ")");
        
        activePrefetches.add(threadId);
        
        // Submit prefetch task with delay to avoid interfering with immediate loads
        prefetchExecutor.submit(() -> {
            try {
                Thread.sleep(PREFETCH_DELAY_MS);
                prefetchOlderMessages(threadId, currentMessageCount);
            } catch (InterruptedException e) {
                Log.d(TAG, "Prefetch interrupted for thread: " + threadId);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Log.e(TAG, "Error during prefetch for thread: " + threadId, e);
            } finally {
                activePrefetches.remove(threadId);
            }
        });
    }
    
    /**
     * Prefetches older messages for smooth scrolling experience.
     */
    private void prefetchOlderMessages(String threadId, int currentMessageCount) {
        if (!isEnabled.get()) {
            return;
        }
        
        Log.d(TAG, "Starting prefetch for thread: " + threadId);
        
        for (int page = 1; page <= MAX_PREFETCH_PAGES; page++) {
            if (!isEnabled.get() || !activePrefetches.contains(threadId)) {
                break; // Stop if disabled or cancelled
            }
            
            int offset = currentMessageCount + ((page - 1) * PREFETCH_PAGE_SIZE);
            
            try {
                // Check if this range is already cached
                String pageKey = threadId + "_page_" + page;
                if (cache.getCachedMessages(pageKey) != null) {
                    Log.d(TAG, "Page " + page + " already cached for thread: " + threadId);
                    continue;
                }
                
                // Load messages from database
                List<Message> messages = loadMessagesPaginated(threadId, offset, PREFETCH_PAGE_SIZE);
                
                if (messages == null || messages.isEmpty()) {
                    Log.d(TAG, "No more messages to prefetch for thread: " + threadId);
                    break; // No more messages available
                }
                
                // Cache the prefetched messages with a page-specific key
                cache.cacheMessages(pageKey, messages);
                
                Log.d(TAG, "Prefetched page " + page + " (" + messages.size() + 
                      " messages) for thread: " + threadId);
                
                // Small delay between pages to avoid overwhelming the system
                Thread.sleep(100);
                
            } catch (InterruptedException e) {
                Log.d(TAG, "Prefetch interrupted for thread: " + threadId);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.e(TAG, "Error prefetching page " + page + " for thread: " + threadId, e);
                break; // Stop prefetching on error
            }
        }
        
        Log.d(TAG, "Completed prefetch for thread: " + threadId);
    }
    
    /**
     * Loads messages with pagination support.
     * This method attempts to use existing MessageService pagination if available.
     */
    private List<Message> loadMessagesPaginated(String threadId, int offset, int limit) {
        try {
            // Try to use paginated method if available
            if (messageService != null) {
                // Use reflection to check if paginated method exists
                try {
                    return messageService.getMessagesByThreadIdPaginated(threadId, offset, limit);
                } catch (Exception e) {
                    // Fallback to regular method and manually implement pagination
                    List<Message> allMessages = messageService.getMessagesByThreadId(threadId);
                    if (allMessages != null && allMessages.size() > offset) {
                        int endIndex = Math.min(offset + limit, allMessages.size());
                        return allMessages.subList(offset, endIndex);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading paginated messages", e);
        }
        
        return null;
    }
    
    /**
     * Prefetches recent conversations for faster app startup.
     */
    public void prefetchRecentConversations() {
        if (!isEnabled.get()) {
            return;
        }
        
        prefetchExecutor.submit(() -> {
            try {
                Log.d(TAG, "Prefetching recent conversations");
                
                // Load recent conversations in background
                if (messageService != null) {
                    List<Conversation> conversations = messageService.loadConversations();
                    
                    if (conversations != null && !conversations.isEmpty()) {
                        // Cache the most recent conversations
                        int limit = Math.min(conversations.size(), 10); // Cache top 10 conversations
                        
                        for (int i = 0; i < limit; i++) {
                            Conversation conv = conversations.get(i);
                            if (conv != null && conv.getThreadId() != null) {
                                cache.cacheConversation(conv.getThreadId(), conv);
                            }
                        }
                        
                        Log.d(TAG, "Prefetched " + limit + " recent conversations");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error prefetching recent conversations", e);
            }
        });
    }
    
    /**
     * Cancels any ongoing prefetch operations for a specific thread.
     */
    public void cancelPrefetch(String threadId) {
        if (threadId != null) {
            activePrefetches.remove(threadId);
            Log.d(TAG, "Cancelled prefetch for thread: " + threadId);
        }
    }
    
    /**
     * Enables or disables background prefetching.
     */
    public void setEnabled(boolean enabled) {
        isEnabled.set(enabled);
        if (!enabled) {
            // Clear active prefetches
            activePrefetches.clear();
            Log.d(TAG, "Background prefetching disabled");
        } else {
            Log.d(TAG, "Background prefetching enabled");
        }
    }
    
    /**
     * Checks if background prefetching is enabled.
     */
    public boolean isEnabled() {
        return isEnabled.get();
    }
    
    /**
     * Gets the number of active prefetch operations.
     */
    public int getActivePrefetchCount() {
        return activePrefetches.size();
    }
    
    /**
     * Shuts down the background loader and cleanup resources.
     */
    public void shutdown() {
        setEnabled(false);
        
        if (prefetchExecutor != null && !prefetchExecutor.isShutdown()) {
            prefetchExecutor.shutdown();
            Log.d(TAG, "Background message loader shutdown");
        }
    }
}