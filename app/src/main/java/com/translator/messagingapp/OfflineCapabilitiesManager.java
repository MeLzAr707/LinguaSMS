package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;

/**
 * Utility class for monitoring and managing enhanced offline capabilities.
 * Provides convenient access to queue status, translation metrics, and system health.
 */
public class OfflineCapabilitiesManager {
    private static final String TAG = "OfflineCapabilitiesManager";
    
    private final Context context;
    private final TranslatorApp app;
    
    public OfflineCapabilitiesManager(Context context) {
        this.context = context.getApplicationContext();
        this.app = (TranslatorApp) this.context;
    }
    
    /**
     * Gets comprehensive status of offline capabilities.
     */
    public OfflineCapabilitiesStatus getStatus() {
        OfflineCapabilitiesStatus status = new OfflineCapabilitiesStatus();
        
        // Check translation capabilities
        try {
            TranslationManager translationManager = app.getTranslationManager();
            if (translationManager != null) {
                OfflineTranslationService offlineService = translationManager.getOfflineTranslationService();
                if (offlineService != null) {
                    status.hasOfflineTranslation = offlineService.hasAnyDownloadedModels();
                    status.downloadedModelCount = offlineService.getDownloadedModels().size();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking translation status", e);
        }
        
        // Check message queue status
        try {
            OfflineMessageQueue messageQueue = app.getOfflineMessageQueue();
            if (messageQueue != null) {
                OfflineMessageQueue.QueueStatus queueStatus = messageQueue.getQueueStatus();
                status.queueStatus = queueStatus;
                status.hasQueuedMessages = queueStatus.hasMessages();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking queue status", e);
        }
        
        return status;
    }
    
    /**
     * Processes any pending messages in the queue.
     */
    public void processQueue() {
        try {
            OfflineMessageQueue messageQueue = app.getOfflineMessageQueue();
            if (messageQueue != null) {
                messageQueue.processQueuedMessages();
                Log.d(TAG, "Manual queue processing triggered");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing queue manually", e);
        }
    }
    
    /**
     * Starts network monitoring for automatic queue processing.
     */
    public void startNetworkMonitoring() {
        try {
            OfflineMessageQueue messageQueue = app.getOfflineMessageQueue();
            if (messageQueue != null) {
                messageQueue.startNetworkMonitoring();
                Log.d(TAG, "Network monitoring started");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting network monitoring", e);
        }
    }
    
    /**
     * Stops network monitoring.
     */
    public void stopNetworkMonitoring() {
        try {
            OfflineMessageQueue messageQueue = app.getOfflineMessageQueue();
            if (messageQueue != null) {
                messageQueue.stopNetworkMonitoring();
                Log.d(TAG, "Network monitoring stopped");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping network monitoring", e);
        }
    }
    
    /**
     * Clears failed messages from the queue.
     */
    public int clearFailedMessages() {
        try {
            OfflineMessageQueue messageQueue = app.getOfflineMessageQueue();
            if (messageQueue != null) {
                return messageQueue.clearFailedMessages();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing failed messages", e);
        }
        return 0;
    }
    
    /**
     * Gets translation performance metrics.
     */
    public TranslationMetrics getTranslationMetrics() {
        TranslationMetrics metrics = new TranslationMetrics();
        
        try {
            TranslationManager translationManager = app.getTranslationManager();
            if (translationManager != null) {
                // Get cache hit rate and other metrics
                TranslationCache cache = translationManager.getTranslationCache();
                if (cache != null) {
                    // These would need to be added to TranslationCache
                    // For now, we provide placeholder values
                    metrics.cacheHitRate = 0.75f; // Example value
                    metrics.totalTranslations = 100; // Example value
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting translation metrics", e);
        }
        
        return metrics;
    }
    
    /**
     * Checks if the system is ready for offline operation.
     */
    public boolean isOfflineReady() {
        OfflineCapabilitiesStatus status = getStatus();
        return status.hasOfflineTranslation && !status.hasQueuedMessages;
    }
    
    /**
     * Gets a human-readable status summary.
     */
    public String getStatusSummary() {
        OfflineCapabilitiesStatus status = getStatus();
        StringBuilder summary = new StringBuilder();
        
        summary.append("Offline Translation: ");
        if (status.hasOfflineTranslation) {
            summary.append("✓ Ready (").append(status.downloadedModelCount).append(" models)");
        } else {
            summary.append("✗ No models downloaded");
        }
        
        summary.append("\nMessage Queue: ");
        if (status.queueStatus != null) {
            if (status.queueStatus.totalCount == 0) {
                summary.append("✓ Empty");
            } else {
                summary.append(status.queueStatus.totalCount).append(" messages")
                       .append(" (").append(status.queueStatus.pendingCount).append(" pending, ")
                       .append(status.queueStatus.failedCount).append(" failed)");
            }
        } else {
            summary.append("✗ Not available");
        }
        
        return summary.toString();
    }
    
    /**
     * Status information for offline capabilities.
     */
    public static class OfflineCapabilitiesStatus {
        public boolean hasOfflineTranslation = false;
        public int downloadedModelCount = 0;
        public boolean hasQueuedMessages = false;
        public OfflineMessageQueue.QueueStatus queueStatus = null;
        
        public boolean isFullyOperational() {
            return hasOfflineTranslation && !hasQueuedMessages;
        }
    }
    
    /**
     * Translation performance metrics.
     */
    public static class TranslationMetrics {
        public float cacheHitRate = 0.0f;
        public int totalTranslations = 0;
        public int offlineTranslations = 0;
        public int onlineTranslations = 0;
        public float averageTranslationTime = 0.0f;
        
        public float getOfflinePercentage() {
            if (totalTranslations == 0) return 0.0f;
            return (float) offlineTranslations / totalTranslations;
        }
    }
    
    /**
     * Interface for status change callbacks.
     */
    public interface StatusChangeCallback {
        void onQueueStatusChanged(OfflineMessageQueue.QueueStatus status);
        void onTranslationStatusChanged(boolean hasOfflineModels, int modelCount);
        void onNetworkStatusChanged(boolean isAvailable);
    }
}