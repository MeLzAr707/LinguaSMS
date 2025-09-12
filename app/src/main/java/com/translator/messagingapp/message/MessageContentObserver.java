package com.translator.messagingapp.message;

import com.translator.messagingapp.system.*;

import com.translator.messagingapp.message.*;

import com.translator.messagingapp.conversation.*;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * ContentObserver for monitoring SMS and MMS content changes.
 * Provides reactive updates when messages are added, modified, or deleted.
 */
public class MessageContentObserver extends ContentObserver {
    private static final String TAG = "MessageContentObserver";

    /**
     * Interface for listening to message content changes.
     */
    public interface OnMessageChangeListener {
        /**
         * Called when SMS messages change.
         */
        void onSmsChanged(Uri uri);
        
        /**
         * Called when MMS messages change.
         */
        void onMmsChanged(Uri uri);
        
        /**
         * Called when conversation threads change.
         */
        void onConversationChanged(Uri uri);
        
        /**
         * Called when any message-related content changes.
         */
        void onMessageContentChanged(Uri uri);
    }

    private final Context context;
    private final List<OnMessageChangeListener> listeners;
    private boolean isRegistered = false;

    /**
     * Creates a new MessageContentObserver.
     *
     * @param context The application context
     */
    public MessageContentObserver(Context context) {
        super(new Handler(Looper.getMainLooper()));
        this.context = context.getApplicationContext();
        this.listeners = new ArrayList<>();
    }

    /**
     * Creates a MessageContentObserver with a custom handler.
     *
     * @param context The application context
     * @param handler The handler to use for notifications
     */
    public MessageContentObserver(Context context, Handler handler) {
        super(handler);
        this.context = context.getApplicationContext();
        this.listeners = new ArrayList<>();
    }

    /**
     * Registers the content observer to monitor SMS and MMS changes.
     */
    public void register() {
        if (isRegistered) {
            Log.w(TAG, "ContentObserver is already registered");
            return;
        }

        try {
            // Register for combined SMS/MMS changes ONLY - this covers all message events
            // and prevents redundant notifications from multiple URIs
            context.getContentResolver().registerContentObserver(
                Uri.parse("content://mms-sms/"), true, this);
            
            isRegistered = true;
            Log.d(TAG, "MessageContentObserver registered successfully for combined SMS/MMS URI");
        } catch (Exception e) {
            Log.e(TAG, "Error registering MessageContentObserver", e);
        }
    }

    /**
     * Unregisters the content observer.
     */
    public void unregister() {
        if (!isRegistered) {
            Log.w(TAG, "ContentObserver is not registered");
            return;
        }

        try {
            context.getContentResolver().unregisterContentObserver(this);
            isRegistered = false;
            Log.d(TAG, "MessageContentObserver unregistered successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering MessageContentObserver", e);
        }
    }

    /**
     * Adds a listener for message content changes.
     *
     * @param listener The listener to add
     */
    public void addListener(OnMessageChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            Log.d(TAG, "Added message change listener. Total listeners: " + listeners.size());
        }
    }

    /**
     * Removes a listener for message content changes.
     *
     * @param listener The listener to remove
     */
    public void removeListener(OnMessageChangeListener listener) {
        if (listeners.remove(listener)) {
            Log.d(TAG, "Removed message change listener. Total listeners: " + listeners.size());
        }
    }

    /**
     * Removes all listeners.
     */
    public void clearListeners() {
        listeners.clear();
        Log.d(TAG, "Cleared all message change listeners");
    }

    @Override
    public void onChange(boolean selfChange) {
        onChange(selfChange, null);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        Log.d(TAG, "Content changed - URI: " + uri + ", selfChange: " + selfChange);

        if (uri == null) {
            // Generic change notification
            notifyAllListeners(null);
            return;
        }

        try {
            String uriString = uri.toString();
            
            if (isSmsUri(uriString)) {
                notifySmsChanged(uri);
            } else if (isMmsUri(uriString)) {
                notifyMmsChanged(uri);
            } else if (isConversationUri(uriString)) {
                notifyConversationChanged(uri);
            } else {
                // Generic message content change
                notifyMessageContentChanged(uri);
            }
            
            // Always notify all listeners of general content change
            notifyAllListeners(uri);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling content change", e);
        }
    }

    /**
     * Checks if the URI is related to SMS content.
     */
    private boolean isSmsUri(String uriString) {
        return uriString.contains("content://sms") || 
               uriString.contains(Telephony.Sms.CONTENT_URI.toString());
    }

    /**
     * Checks if the URI is related to MMS content.
     */
    private boolean isMmsUri(String uriString) {
        return uriString.contains("content://mms") || 
               uriString.contains(Telephony.Mms.CONTENT_URI.toString());
    }

    /**
     * Checks if the URI is related to conversation threads.
     */
    private boolean isConversationUri(String uriString) {
        return uriString.contains("content://mms-sms/conversations") ||
               uriString.contains(Telephony.Threads.CONTENT_URI.toString());
    }

    /**
     * Notifies listeners of SMS changes.
     */
    private void notifySmsChanged(Uri uri) {
        for (OnMessageChangeListener listener : listeners) {
            try {
                listener.onSmsChanged(uri);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying SMS change listener", e);
            }
        }
    }

    /**
     * Notifies listeners of MMS changes.
     */
    private void notifyMmsChanged(Uri uri) {
        for (OnMessageChangeListener listener : listeners) {
            try {
                listener.onMmsChanged(uri);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying MMS change listener", e);
            }
        }
    }

    /**
     * Notifies listeners of conversation changes.
     */
    private void notifyConversationChanged(Uri uri) {
        for (OnMessageChangeListener listener : listeners) {
            try {
                listener.onConversationChanged(uri);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying conversation change listener", e);
            }
        }
    }

    /**
     * Notifies listeners of general message content changes.
     */
    private void notifyMessageContentChanged(Uri uri) {
        for (OnMessageChangeListener listener : listeners) {
            try {
                listener.onMessageContentChanged(uri);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying message content change listener", e);
            }
        }
    }

    /**
     * Notifies all listeners of a content change.
     */
    private void notifyAllListeners(Uri uri) {
        // Schedule message sync work when content changes
        try {
            TranslatorApp app = (TranslatorApp) context;
            MessageWorkManager workManager = new MessageWorkManager(context);
            workManager.scheduleSyncMessages();
            Log.d(TAG, "Scheduled message sync due to content change");
        } catch (Exception e) {
            Log.w(TAG, "Could not schedule message sync", e);
        }

        // Clear message cache to ensure fresh data is loaded
        try {
            // Extract thread ID from URI if possible
            String threadId = extractThreadIdFromUri(uri);
            if (threadId != null) {
                // Only clear cache for the affected thread
                MessageCache.clearCacheForThread(threadId);
                Log.d(TAG, "Cleared message cache for thread " + threadId + " due to content change");
            } else {
                // Only clear all cache if we can't determine which thread changed
                MessageCache.clearCache();
                Log.d(TAG, "Cleared all message cache due to content change");
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not clear message cache", e);
        }
    }

    // Add helper method to extract thread ID from URI
    private String extractThreadIdFromUri(Uri uri) {
        if (uri == null) return null;
        
        try {
            String uriString = uri.toString();
            
            // Try to extract thread_id from URI patterns
            if (uriString.contains("thread_id=")) {
                String[] parts = uriString.split("thread_id=");
                if (parts.length > 1) {
                    String threadIdPart = parts[1];
                    int endIndex = threadIdPart.indexOf('&');
                    if (endIndex > 0) {
                        return threadIdPart.substring(0, endIndex);
                    } else {
                        return threadIdPart;
                    }
                }
            }
            
            // Try to extract from path segments
            List<String> segments = uri.getPathSegments();
            if (segments.size() >= 2) {
                if ("conversations".equals(segments.get(0))) {
                    return segments.get(1);
                } else if ("threads".equals(segments.get(0))) {
                    return segments.get(1);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting thread ID from URI", e);
        }
        
        return null;
    }

    /**
     * Returns whether the observer is currently registered.
     */
    public boolean isRegistered() {
        return isRegistered;
    }

    /**
     * Returns the number of active listeners.
     */
    public int getListenerCount() {
        return listeners.size();
    }
}