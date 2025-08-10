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
    private RcsService rcsService;

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
            // Initialize RCS service
            rcsService = new RcsService(this);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error initializing RCS service", e);
            // rcsService will remain null
        }

        try {
            // Schedule periodic cache maintenance
            schedulePeriodicCacheMaintenance();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error scheduling cache maintenance", e);
            // Continue without periodic maintenance
        }
    }

    public TranslationCache getTranslationCache() {
        return translationCache;
    }

    public TranslationManager getTranslationManager() {
        return translationManager;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public GoogleTranslationService getTranslationService() {
        return translationService;
    }

    public DefaultSmsAppManager getDefaultSmsAppManager() {
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
    
    public RcsService getRcsService() {
        return rcsService;
    }

    /**
     * Checks if the app has a valid API key.
     *
     * @return true if a valid API key is available, false otherwise
     */
    public boolean hasValidApiKey() {
        return translationService != null && translationService.hasApiKey();
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
        if (rcsService != null) {
            rcsService.cleanup();
        }
        super.onTerminate();
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