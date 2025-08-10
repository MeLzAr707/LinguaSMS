package com.translator.messagingapp;

import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Content provider for SMS functionality when the app is set as the default SMS app.
 * This provider delegates most operations to the system's SMS provider but can
 * add custom functionality like translation metadata.
 *
 * Updated to work with MessageService for enhanced functionality.
 */
public class SmsProvider extends ContentProvider {
    private static final String TAG = "SmsProvider";

    // Authority for this provider
    private static final String AUTHORITY = "com.translator.messagingapp.provider";

    // URI matcher codes
    private static final int SMS = 1;
    private static final int SMS_ID = 2;
    private static final int CONVERSATIONS = 3;
    private static final int TRANSLATIONS = 4;
    private static final int TRANSLATION_ID = 5;

    // URI matcher
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Service references
    private MessageService messageService;
    private TranslationManager translationManager;

    // Initialize URI matcher
    static {
        uriMatcher.addURI(AUTHORITY, "sms", SMS);
        uriMatcher.addURI(AUTHORITY, "sms/#", SMS_ID);
        uriMatcher.addURI(AUTHORITY, "conversations", CONVERSATIONS);
        uriMatcher.addURI(AUTHORITY, "translations", TRANSLATIONS);
        uriMatcher.addURI(AUTHORITY, "translations/#", TRANSLATION_ID);
    }

    @Override
    public boolean onCreate() {
        Log.d(TAG, "SmsProvider created");

        // Get service instances from the application context
        Application app = getContext().getApplicationContext();
        
        if (app instanceof TranslatorApp) {
            TranslatorApp translatorApp = (TranslatorApp) app;
            messageService = translatorApp.getMessageService();
            translationManager = translatorApp.getTranslationManager();
        } else if (app instanceof OptimizedTranslatorApp) {
            OptimizedTranslatorApp optimizedApp = (OptimizedTranslatorApp) app;
            messageService = optimizedApp.getMessageService();
            translationManager = optimizedApp.getTranslationManager();
        } else {
            Log.e(TAG, "Unknown application type: " + app.getClass().getName());
            return false;
        }

        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Log.d(TAG, "Query: " + uri);

        try {
            int match = uriMatcher.match(uri);
            switch (match) {
                case SMS:
                case SMS_ID:
                    // Delegate to system SMS provider
                    return delegateQueryToSystemProvider(uri, projection, selection, selectionArgs, sortOrder);

                case CONVERSATIONS:
                    // Query conversations using MessageService
                    return queryConversations(projection, selection, selectionArgs, sortOrder);

                case TRANSLATIONS:
                case TRANSLATION_ID:
                    // Query translations
                    return queryTranslations(uri, projection, selection, selectionArgs, sortOrder);

                default:
                    throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in query", e);
            return null;
        }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        Log.d(TAG, "GetType: " + uri);

        int match = uriMatcher.match(uri);
        switch (match) {
            case SMS:
                return "vnd.android.cursor.dir/sms";
            case SMS_ID:
                return "vnd.android.cursor.item/sms";
            case CONVERSATIONS:
                return "vnd.android.cursor.dir/conversations";
            case TRANSLATIONS:
                return "vnd.android.cursor.dir/translations";
            case TRANSLATION_ID:
                return "vnd.android.cursor.item/translation";
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        Log.d(TAG, "Insert: " + uri);

        try {
            int match = uriMatcher.match(uri);
            switch (match) {
                case SMS:
                    // For SMS inserts, we can use MessageService to handle additional logic
                    if (values != null && values.containsKey(Telephony.Sms.ADDRESS) &&
                            values.containsKey(Telephony.Sms.BODY)) {

                        String address = values.getAsString(Telephony.Sms.ADDRESS);
                        String body = values.getAsString(Telephony.Sms.BODY);

                        // Check if this is a sent message
                        Integer type = values.getAsInteger(Telephony.Sms.TYPE);
                        if (type != null && type == Telephony.Sms.MESSAGE_TYPE_SENT) {
                            // For sent messages, we can use MessageService
                            String threadId = values.getAsString(Telephony.Sms.THREAD_ID);

                            // This is a non-blocking call, so we still need to delegate to system provider
                            messageService.sendSmsMessage(address, body, threadId, null);
                        }
                    }

                    // Delegate to system SMS provider for the actual insert
                    return delegateInsertToSystemProvider(uri, values);

                case TRANSLATIONS:
                    // Insert translation
                    return insertTranslation(uri, values);

                default:
                    throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in insert", e);
            return null;
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        Log.d(TAG, "Delete: " + uri);

        try {
            int match = uriMatcher.match(uri);
            switch (match) {
                case SMS:
                    // For bulk SMS deletes, we might want to clear caches
                    int result = delegateDeleteToSystemProvider(uri, selection, selectionArgs);
                    if (result > 0) {
                        // Clear message cache since we don't know which threads were affected
                        MessageCache.clearCache();
                    }
                    return result;

                case SMS_ID:
                    // For single SMS delete, we can be more specific
                    String id = uri.getLastPathSegment();
                    if (id != null) {
                        // Use MessageService to delete the message
                        boolean success = messageService.deleteMessage(id, Message.MESSAGE_TYPE_SMS);
                        return success ? 1 : 0;
                    }
                    return delegateDeleteToSystemProvider(uri, selection, selectionArgs);

                case TRANSLATIONS:
                case TRANSLATION_ID:
                    // Delete translation
                    return deleteTranslation(uri, selection, selectionArgs);

                default:
                    throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in delete", e);
            return 0;
        }
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        Log.d(TAG, "Update: " + uri);

        try {
            int match = uriMatcher.match(uri);
            switch (match) {
                case SMS:
                case SMS_ID:
                    // Delegate to system SMS provider
                    return delegateUpdateToSystemProvider(uri, values, selection, selectionArgs);

                case TRANSLATIONS:
                case TRANSLATION_ID:
                    // Update translation
                    return updateTranslation(uri, values, selection, selectionArgs);

                default:
                    throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in update", e);
            return 0;
        }
    }

    /**
     * Delegates a query operation to the system SMS provider.
     */
    private Cursor delegateQueryToSystemProvider(Uri uri, String[] projection, String selection,
                                                 String[] selectionArgs, String sortOrder) {
        // Convert our URI to the system URI
        Uri systemUri = convertToSystemUri(uri);

        // Query the system provider
        return getContext().getContentResolver().query(
                systemUri, projection, selection, selectionArgs, sortOrder);
    }

    /**
     * Delegates an insert operation to the system SMS provider.
     */
    private Uri delegateInsertToSystemProvider(Uri uri, ContentValues values) {
        // Convert our URI to the system URI
        Uri systemUri = convertToSystemUri(uri);

        // Insert into the system provider
        return getContext().getContentResolver().insert(systemUri, values);
    }

    /**
     * Delegates a delete operation to the system SMS provider.
     */
    private int delegateDeleteToSystemProvider(Uri uri, String selection, String[] selectionArgs) {
        // Convert our URI to the system URI
        Uri systemUri = convertToSystemUri(uri);

        // Delete from the system provider
        return getContext().getContentResolver().delete(systemUri, selection, selectionArgs);
    }

    /**
     * Delegates an update operation to the system SMS provider.
     */
    private int delegateUpdateToSystemProvider(Uri uri, ContentValues values, String selection,
                                               String[] selectionArgs) {
        // Convert our URI to the system URI
        Uri systemUri = convertToSystemUri(uri);

        // Update in the system provider
        return getContext().getContentResolver().update(systemUri, values, selection, selectionArgs);
    }

    /**
     * Converts our provider URI to the system provider URI.
     */
    private Uri convertToSystemUri(Uri uri) {
        int match = uriMatcher.match(uri);

        switch (match) {
            case SMS:
                return Telephony.Sms.CONTENT_URI;

            case SMS_ID:
                // Extract ID from our URI
                String id = uri.getLastPathSegment();
                return Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, id);

            default:
                return uri;
        }
    }

    /**
     * Queries conversations using MessageService.
     */
    private Cursor queryConversations(String[] projection, String selection,
                                      String[] selectionArgs, String sortOrder) {
        // Use MessageService to load conversations
        List<Conversation> conversations = messageService.loadConversations();

        // Convert to cursor
        MatrixCursor cursor = new MatrixCursor(new String[] {
                "thread_id", "address", "contact_name", "snippet", "date", "read", "message_count"
        });

        for (Conversation conversation : conversations) {
            cursor.addRow(new Object[] {
                    conversation.getThreadId(),
                    conversation.getAddress(),
                    conversation.getContactName(),
                    conversation.getSnippet(),
                    conversation.getDate().getTime(),
                    conversation.isRead() ? 1 : 0,
                    conversation.getMessageCount()
            });
        }

        return cursor;
    }

    /**
     * Queries translations.
     * This would be implemented to provide access to stored translations.
     */
    private Cursor queryTranslations(Uri uri, String[] projection, String selection,
                                     String[] selectionArgs, String sortOrder) {
        // This is a placeholder for a real implementation
        // In a full implementation, we would query a database of stored translations

        // For now, return an empty cursor with the expected columns
        MatrixCursor cursor = new MatrixCursor(new String[] {
                "_id", "message_id", "original_text", "translated_text",
                "original_language", "translated_language", "timestamp"
        });

        return cursor;
    }

    /**
     * Inserts a translation.
     * This would be implemented to store translations for messages.
     */
    private Uri insertTranslation(Uri uri, ContentValues values) {
        // This is a placeholder for a real implementation
        // In a full implementation, we would insert into a database of stored translations

        Log.d(TAG, "Translation insert requested (not implemented): " + values);

        // Return a dummy URI
        return Uri.withAppendedPath(uri, "1");
    }

    /**
     * Updates a translation.
     * This would be implemented to update stored translations.
     */
    private int updateTranslation(Uri uri, ContentValues values, String selection,
                                  String[] selectionArgs) {
        // This is a placeholder for a real implementation
        // In a full implementation, we would update a database of stored translations

        Log.d(TAG, "Translation update requested (not implemented): " + values);

        // Return 0 rows affected
        return 0;
    }

    /**
     * Deletes a translation.
     * This would be implemented to delete stored translations.
     */
    private int deleteTranslation(Uri uri, String selection, String[] selectionArgs) {
        // This is a placeholder for a real implementation
        // In a full implementation, we would delete from a database of stored translations

        Log.d(TAG, "Translation delete requested (not implemented)");

        // Return 0 rows affected
        return 0;
    }
}



