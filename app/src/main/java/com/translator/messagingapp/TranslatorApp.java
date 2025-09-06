package com.translator.messagingapp;

import android.app.Application;
import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import androidx.annotation.NonNull;

import java.util.concurrent.TimeUnit;

public class TranslatorApp extends Application {
    private static final String TAG = "TranslatorApp";

    private TranslationCache translationCache;
    private TranslationManager translationManager;
    private MessageService messageService;
    private GoogleTranslationService translationService;
    private DefaultSmsAppManager defaultSmsAppManager;
    private UserPreferences userPreferences;
    private MessageWorkManager messageWorkManager;
    private MessageContentObserver messageContentObserver;
    private GenAIMessagingService genAIMessagingService;

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            // Initialize user preferences
            userPreferences = new UserPreferences(this);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error initializing user preferences", e);
            // Create a fallback UserPreferences that won't crash
            userPreferences = new UserPreferences(this);
        }

        try {
            // Initialize translation cache
            translationCache = new TranslationCache(getApplicationContext());
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error initializing translation cache", e);
            // TranslationCache handles this internally now, but extra safety
        }

        try {
            // Initialize translation service
            translationService = new GoogleTranslationService(userPreferences != null ? userPreferences.getApiKey() : "");
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error initializing translation service", e);
            translationService = new GoogleTranslationService("");
        }

        try {
            // Initialize translation manager with the cache
            translationManager = new TranslationManager(
                    getApplicationContext(),
                    translationService,
                    userPreferences);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error initializing translation manager", e);
            // translationManager will remain null, other components should handle this
        }

        try {
            // Initialize message service
            messageService = new MessageService(this, translationManager);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error initializing message service", e);
            // messageService will remain null, activities should handle this
        }

        try {
            // Initialize default SMS app manager
            defaultSmsAppManager = new DefaultSmsAppManager(this);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error initializing default SMS app manager", e);
            // defaultSmsAppManager will remain null, activities should handle this
        }
        


        try {
            // Schedule periodic cache maintenance
            schedulePeriodicCacheMaintenance();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error scheduling cache maintenance", e);
            // Continue without periodic maintenance
        }

        try {
            // Initialize WorkManager for message processing
            messageWorkManager = new MessageWorkManager(this);
            messageWorkManager.initializePeriodicWork();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error initializing MessageWorkManager", e);
        }

        try {
            // Initialize and register ContentObserver for reactive updates
            messageContentObserver = new MessageContentObserver(this);
            messageContentObserver.register();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error initializing MessageContentObserver", e);
        }

        try {
            // Initialize GenAI messaging service
            genAIMessagingService = new GenAIMessagingService(this);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error initializing GenAI messaging service", e);
        }
    }

    public TranslationCache getTranslationCache() {
        if (translationCache == null) {
            try {
                translationCache = new TranslationCache(getApplicationContext());
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error creating fallback TranslationCache", e);
            }
        }
        return translationCache;
    }

    public TranslationManager getTranslationManager() {
        if (translationManager == null) {
            try {
                // Try to create a minimal translation manager if possible
                if (translationService != null && userPreferences != null) {
                    translationManager = new TranslationManager(
                            getApplicationContext(),
                            translationService,
                            userPreferences);
                }
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error creating fallback TranslationManager", e);
            }
        }
        return translationManager;
    }

    public MessageService getMessageService() {
        if (messageService == null) {
            try {
                // Try to create a minimal message service if possible
                messageService = new MessageService(this, getTranslationManager());
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error creating fallback MessageService", e);
            }
        }
        return messageService;
    }

    public GoogleTranslationService getTranslationService() {
        if (translationService == null) {
            try {
                translationService = new GoogleTranslationService(
                    userPreferences != null ? userPreferences.getApiKey() : "");
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error creating fallback GoogleTranslationService", e);
            }
        }
        return translationService;
    }

    public DefaultSmsAppManager getDefaultSmsAppManager() {
        if (defaultSmsAppManager == null) {
            try {
                defaultSmsAppManager = new DefaultSmsAppManager(this);
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error creating fallback DefaultSmsAppManager", e);
            }
        }
        return defaultSmsAppManager;
    }

    public UserPreferences getUserPreferences() {
        // Ensure we always return a valid UserPreferences object
        if (userPreferences == null) {
            try {
                userPreferences = new UserPreferences(this);
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error creating fallback UserPreferences", e);
            }
        }
        return userPreferences;
    }
    
    public MessageWorkManager getMessageWorkManager() {
        if (messageWorkManager == null) {
            try {
                messageWorkManager = new MessageWorkManager(this);
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error creating fallback MessageWorkManager", e);
            }
        }
        return messageWorkManager;
    }

    public MessageContentObserver getMessageContentObserver() {
        if (messageContentObserver == null) {
            try {
                messageContentObserver = new MessageContentObserver(this);
                messageContentObserver.register();
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error creating fallback MessageContentObserver", e);
            }
        }
        return messageContentObserver;
    }
    
    public GenAIMessagingService getGenAIMessagingService() {
        if (genAIMessagingService == null) {
            try {
                genAIMessagingService = new GenAIMessagingService(this);
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error creating fallback GenAIMessagingService", e);
            }
        }
        return genAIMessagingService;
    }
    


    /**
     * Checks if the app has offline translation capability.
     *
     * @return true if offline translation models are available, false otherwise
     */
    public boolean hasOfflineTranslationCapability() {
        if (translationManager != null) {
            // Check Gemini Nano service first (new approach)
            GeminiNanoTranslationService geminiNanoService = translationManager.getGeminiNanoTranslationService();
            if (geminiNanoService != null) {
                // Check if any Gemini Nano models are available
                return geminiNanoService.hasAnyDownloadedModels();
            }
            
            // Fallback to legacy OfflineTranslationService (deprecated)
            OfflineTranslationService offlineService = translationManager.getOfflineTranslationService();
            if (offlineService != null) {
                // Check if any offline models are downloaded
                return offlineService.hasAnyDownloadedModels();
            }
        }
        return false;
    }

    /**
     * Checks if the app has translation capability (offline preferred, online as fallback).
     * This method prioritizes offline ML Kit usage and treats API keys as optional.
     *
     * @return true if offline translation is available or if an API key is available as fallback, false otherwise
     */
    public boolean hasValidApiKey() {
        // Prioritize offline capability - if we have offline models, we don't need API keys
        boolean hasOfflineCapability = hasOfflineTranslationCapability();
        if (hasOfflineCapability) {
            return true;
        }
        
        // Fallback to online capability only if offline is not available
        boolean hasOnlineCapability = translationService != null && translationService.hasApiKey();
        return hasOnlineCapability;
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

    @Override
    public void onTerminate() {
        // Clean up resources
        if (translationCache != null) {
            translationCache.close();
        }
        if (translationManager != null) {
            translationManager.cleanup();
        }
        if (messageContentObserver != null) {
            messageContentObserver.unregister();
        }
        if (messageWorkManager != null) {
            messageWorkManager.cancelPeriodicWork();
        }
        if (genAIMessagingService != null) {
            genAIMessagingService.cleanup();
        }

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
            TranslationCache cache = ((TranslatorApp) getApplicationContext()).getTranslationCache();
            cache.performMaintenance();
            return Result.success();
        }
    }
}