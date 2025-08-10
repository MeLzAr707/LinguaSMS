package com.translator.messagingapp;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Optimized application class with background prefetching capabilities.
 */
public class OptimizedTranslatorApp extends Application {
    private static final String TAG = "OptimizedTranslatorApp";
    
    private ExecutorService prefetchExecutor;
    private MessageService messageService;
    private OptimizedMessageService optimizedMessageService;
    private TranslationManager translationManager;
    private TranslationCache translationCache;
    private UserPreferences userPreferences;
    private DefaultSmsAppManager defaultSmsAppManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize user preferences
        userPreferences = new UserPreferences(this);
        
        // Initialize translation cache
        translationCache = new TranslationCache(getApplicationContext());
        
        // Initialize default SMS app manager
        defaultSmsAppManager = new DefaultSmsAppManager(this);
        
        // Initialize services
        translationManager = new TranslationManager(this);
        messageService = new MessageService(this, translationManager);
        optimizedMessageService = new OptimizedMessageService(this, translationManager);
        
        // Initialize prefetch executor
        prefetchExecutor = Executors.newSingleThreadExecutor();
        
        // Prefetch conversations and frequently accessed data
        prefetchData();
        
        // Schedule periodic cache maintenance
        schedulePeriodicCacheMaintenance();
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
        // Clean up resources
        if (translationCache != null) {
            translationCache.close();
        }
        if (translationManager != null) {
            translationManager.cleanup();
        }
        
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
     * Gets the translation cache.
     *
     * @return The translation cache
     */
    public TranslationCache getTranslationCache() {
        return translationCache;
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
     * Gets the default SMS app manager.
     *
     * @return The default SMS app manager
     */
    public DefaultSmsAppManager getDefaultSmsAppManager() {
        return defaultSmsAppManager;
    }
    
    private void schedulePeriodicCacheMaintenance() {
        WorkManager workManager = WorkManager.getInstance(this);

        // Define a periodic work request to run once a day
        PeriodicWorkRequest maintenanceWork =
                new PeriodicWorkRequest.Builder(CacheMaintenanceWorker.class, 1, TimeUnit.DAYS)
                        .setConstraints(new Constraints.Builder()
                                .setRequiresCharging(true)
                                .setRequiresBatteryNotLow(true)
                                .build())
                        .build();

        // Enqueue the work
        workManager.enqueueUniquePeriodicWork(
                "translation_cache_maintenance",
                ExistingPeriodicWorkPolicy.REPLACE,
                maintenanceWork);
    }
    
    /**
     * Worker class for cache maintenance.
     */
    public static class CacheMaintenanceWorker extends Worker {
        public CacheMaintenanceWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
        }

        @NonNull
        @Override
        public Result doWork() {
            TranslationCache cache = ((OptimizedTranslatorApp) getApplicationContext()).getTranslationCache();
            cache.performMaintenance();
            return Result.success();
        }
    }
}