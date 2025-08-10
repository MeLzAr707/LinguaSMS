package com.translator.messagingapp;

import android.app.Application;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Optimized application class with background prefetching capabilities.
 */
public class OptimizedTranslatorApp extends Application {
    private static final String TAG = "OptimizedTranslatorApp";
    
    private ExecutorService prefetchExecutor;
    private MessageService messageService;
    private OptimizedMessageService optimizedMessageService;
    private TranslationManager translationManager;
    private GoogleTranslationService translationService;
    private UserPreferences userPreferences;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize user preferences
        userPreferences = new UserPreferences(this);
        
        // Initialize translation service
        translationService = new GoogleTranslationService(userPreferences.getApiKey());
        
        // Initialize translation manager with proper parameters
        translationManager = new TranslationManager(this, translationService, userPreferences);
        messageService = new MessageService(this, translationManager);
        optimizedMessageService = new OptimizedMessageService(this, translationManager);
        
        // Initialize prefetch executor
        prefetchExecutor = Executors.newSingleThreadExecutor();
        
        // Prefetch conversations and frequently accessed data
        prefetchData();
    }
    
    /**
     * Prefetches data in the background to improve app responsiveness.
     */
    private void prefetchData() {
        prefetchExecutor.execute(() -> {
            try {
                // Prefetch conversations
                prefetchConversations();
                
                // Prefetch recent messages
                prefetchRecentMessages();
                
                // Prefetch contact information
                prefetchContacts();
            } catch (Exception e) {
                Log.e(TAG, "Error during data prefetching", e);
            }
        });
    }
    
    /**
     * Prefetches conversations in the background.
     */
    private void prefetchConversations() {
        try {
            Log.d(TAG, "Prefetching conversations");
            messageService.loadConversations();
            Log.d(TAG, "Conversations prefetched");
        } catch (Exception e) {
            Log.e(TAG, "Error prefetching conversations", e);
        }
    }
    
    /**
     * Prefetches recent messages from the most recent conversations.
     */
    private void prefetchRecentMessages() {
        try {
            Log.d(TAG, "Prefetching recent messages");
            
            // Get recent conversations
            List<Conversation> conversations = messageService.loadConversations();
            
            // Prefetch messages from the 3 most recent conversations
            int count = 0;
            for (Conversation conversation : conversations) {
                if (count >= 3) break;
                
                String threadId = conversation.getThreadId();
                if (threadId != null && !threadId.isEmpty()) {
                    optimizedMessageService.getMessagesByThreadIdPaginated(
                        threadId, 
                        0, 
                        20, // Just prefetch the 20 most recent messages
                        messages -> Log.d(TAG, "Prefetched " + messages.size() + " messages for thread " + threadId)
                    );
                    count++;
                }
            }
            
            Log.d(TAG, "Recent messages prefetched");
        } catch (Exception e) {
            Log.e(TAG, "Error prefetching recent messages", e);
        }
    }
    
    /**
     * Prefetches contact information for recent conversations.
     */
    private void prefetchContacts() {
        try {
            Log.d(TAG, "Prefetching contacts");
            
            // Get recent conversations
            List<Conversation> conversations = messageService.loadConversations();
            
            // Extract phone numbers
            List<String> phoneNumbers = new ArrayList<>();
            for (Conversation conversation : conversations) {
                String address = conversation.getAddress();
                if (address != null && !address.isEmpty()) {
                    phoneNumbers.add(address);
                }
            }
            
            // Batch lookup contacts
            if (!phoneNumbers.isEmpty()) {
                OptimizedContactUtils.getContactNamesForNumbers(this, phoneNumbers);
                Log.d(TAG, "Prefetched contacts for " + phoneNumbers.size() + " phone numbers");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error prefetching contacts", e);
        }
    }
    
    @Override
    public void onTerminate() {
        // Shutdown executor
        if (prefetchExecutor != null && !prefetchExecutor.isShutdown()) {
            prefetchExecutor.shutdown();
        }
        
        super.onTerminate();
    }
    
    /**
     * Gets the translation manager.
     *
     * @return The translation manager
     */
    public TranslationManager getTranslationManager() {
        return translationManager;
    }
    
    /**
     * Gets the translation service.
     *
     * @return The Google translation service
     */
    public GoogleTranslationService getTranslationService() {
        return translationService;
    }
    
    /**
     * Gets the user preferences.
     *
     * @return The user preferences
     */
    public UserPreferences getUserPreferences() {
        return userPreferences;
    }
    
    /**
     * Gets the message service.
     *
     * @return The message service
     */
    public MessageService getMessageService() {
        return messageService;
    }
    
    /**
     * Gets the optimized message service.
     *
     * @return The optimized message service
     */
    public OptimizedMessageService getOptimizedMessageService() {
        return optimizedMessageService;
    }
}