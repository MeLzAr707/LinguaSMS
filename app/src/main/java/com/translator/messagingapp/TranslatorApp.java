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

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize user preferences
        userPreferences = new UserPreferences(this);

        // Initialize translation cache
        translationCache = new TranslationCache(getApplicationContext());

        // Initialize translation service
        translationService = new GoogleTranslationService(userPreferences.getApiKey());

        // Initialize translation manager with the cache
        translationManager = new TranslationManager(
                getApplicationContext(),
                translationService,
                userPreferences);

        // Initialize message service
        messageService = new MessageService(this, translationManager);

        // Initialize default SMS app manager
        defaultSmsAppManager = new DefaultSmsAppManager(this);

        // Schedule periodic cache maintenance
        schedulePeriodicCacheMaintenance();
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
        return userPreferences;
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