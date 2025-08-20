package com.translator.messagingapp;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/**
 * Example of how to integrate the new WorkManager and ContentObserver functionality
 * into existing activities for reactive UI updates and background processing.
 */
public class ActivityIntegrationExample extends BaseActivity 
        implements MessageContentObserver.OnMessageChangeListener {
    
    private static final String TAG = "ActivityIntegrationExample";
    
    private MessageWorkManager workManager;
    private MessageContentObserver contentObserver;
    private boolean isContentObserverRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Get components from TranslatorApp
        TranslatorApp app = (TranslatorApp) getApplicationContext();
        workManager = app.getMessageWorkManager();
        contentObserver = app.getMessageContentObserver();
        
        // Set up reactive updates
        setupReactiveUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Register for content changes when activity becomes visible
        if (contentObserver != null && !isContentObserverRegistered) {
            contentObserver.addListener(this);
            isContentObserverRegistered = true;
            Log.d(TAG, "Registered for content changes");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        // Unregister from content changes when activity is not visible
        if (contentObserver != null && isContentObserverRegistered) {
            contentObserver.removeListener(this);
            isContentObserverRegistered = false;
            Log.d(TAG, "Unregistered from content changes");
        }
    }

    /**
     * Example: Send a message using background processing
     */
    private void sendMessageWithBackgroundProcessing(String recipient, String message) {
        if (workManager != null) {
            // Schedule the message to be sent in background
            // This ensures delivery even if activity is destroyed
            workManager.scheduleSendSms(recipient, message, getCurrentThreadId());
            
            Log.d(TAG, "Scheduled SMS for background sending");
            
            // UI will be updated automatically when message is sent
            // via ContentObserver notifications
        } else {
            Log.e(TAG, "WorkManager not available, falling back to direct sending");
            // Fallback to direct sending if WorkManager is not available
            fallbackSendMessage(recipient, message);
        }
    }

    /**
     * Example: Translate a message in the background
     */
    private void translateMessageInBackground(String messageId, String messageBody) {
        if (workManager != null) {
            // Get user's preferred target language
            String targetLanguage = getUserPreferences().getTargetLanguage();
            
            // Schedule translation work
            workManager.scheduleTranslateMessage(
                messageId, messageBody, "auto", targetLanguage);
            
            Log.d(TAG, "Scheduled message translation");
            
            // Translation result can be handled when work completes
            // and UI updated via ContentObserver
        }
    }

    /**
     * Example: Trigger manual refresh
     */
    private void refreshMessages() {
        if (workManager != null) {
            // Schedule immediate sync to refresh all messages
            workManager.scheduleSyncMessages();
            
            Log.d(TAG, "Triggered manual message sync");
            
            // UI will be updated when sync completes via ContentObserver
        }
    }

    // ContentObserver.OnMessageChangeListener implementation

    @Override
    public void onSmsChanged(Uri uri) {
        Log.d(TAG, "SMS content changed, refreshing SMS UI");
        
        // Update SMS-specific UI elements
        runOnUiThread(() -> {
            refreshSmsMessages();
            updateUnreadCount();
        });
    }

    @Override
    public void onMmsChanged(Uri uri) {
        Log.d(TAG, "MMS content changed, refreshing MMS UI");
        
        // Update MMS-specific UI elements
        runOnUiThread(() -> {
            refreshMmsMessages();
            updateUnreadCount();
        });
    }

    @Override
    public void onConversationChanged(Uri uri) {
        Log.d(TAG, "Conversation content changed, refreshing conversation list");
        
        // Update conversation list
        runOnUiThread(() -> {
            refreshConversationList();
            updateLastMessageInfo();
        });
    }

    @Override
    public void onMessageContentChanged(Uri uri) {
        Log.d(TAG, "General message content changed");
        
        // Handle general content changes
        runOnUiThread(() -> {
            refreshAllMessageContent();
        });
    }

    /**
     * Set up reactive updates for the activity
     */
    private void setupReactiveUpdates() {
        // ContentObserver is already registered at app level
        // We just need to add our listener when activity is visible
        
        Log.d(TAG, "Reactive updates configured");
    }

    // Example UI update methods (implement based on your activity's needs)

    private void refreshSmsMessages() {
        // Clear message cache and reload SMS messages
        MessageCache.clearCache();
        loadMessages(); // Your existing message loading method
    }

    private void refreshMmsMessages() {
        // Refresh MMS-specific content
        loadMmsContent(); // Your existing MMS loading method
    }

    private void refreshConversationList() {
        // Refresh conversation list
        loadConversations(); // Your existing conversation loading method
    }

    private void refreshAllMessageContent() {
        // Refresh all message-related content
        refreshSmsMessages();
        refreshMmsMessages();
        refreshConversationList();
    }

    private void updateUnreadCount() {
        // Update unread message count in UI
        // This could update badges, counters, etc.
    }

    private void updateLastMessageInfo() {
        // Update last message information in conversation list
        // This could update snippets, timestamps, etc.
    }

    // Helper methods (implement based on your activity's needs)

    private String getCurrentThreadId() {
        // Return current thread ID if in conversation view
        return "example_thread_id";
    }

    private UserPreferences getUserPreferences() {
        TranslatorApp app = (TranslatorApp) getApplicationContext();
        return app.getUserPreferences();
    }

    private void fallbackSendMessage(String recipient, String message) {
        // Fallback message sending without WorkManager
        TranslatorApp app = (TranslatorApp) getApplicationContext();
        MessageService messageService = app.getMessageService();
        if (messageService != null) {
            messageService.sendSmsMessage(recipient, message);
        }
    }

    private void loadMessages() {
        // Your existing message loading implementation
    }

    private void loadMmsContent() {
        // Your existing MMS loading implementation
    }

    private void loadConversations() {
        // Your existing conversation loading implementation
    }
}