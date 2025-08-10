package com.translator.messagingapp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class OptimizedMessageService {
    private static final String TAG = "OptimizedMessageService";
    private final Context context;
    private final TranslationManager translationManager;
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();

    public OptimizedMessageService(Context context, TranslationManager translationManager) {
        this.context = context;
        this.translationManager = translationManager;
    }

    public interface Callback<T> {
        void onResult(T result);
    }

    public void getMessagesByThreadIdPaginated(String threadId, int offset, int limit, Callback<List<Message>> callback) {
        backgroundExecutor.execute(() -> {
            List<Message> messages = new ArrayList<>();
            ContentResolver contentResolver = context.getContentResolver();
            Cursor cursor = null;
            try {
                Uri uri = Telephony.Sms.CONTENT_URI;
                String[] projection = null;
                String selection = Telephony.Sms.THREAD_ID + " = ?";
                String[] selectionArgs = {threadId};
                String sortOrder = Telephony.Sms.DATE + " ASC LIMIT " + limit + " OFFSET " + offset;

                cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder);

                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        Message message = new Message();
                        message.setId(cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms._ID)));
                        message.setThreadId(threadId);
                        message.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)));
                        message.setBody(cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)));
                        message.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)));
                        message.setType(cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)));
                        message.setRead(cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1);
                        messages.add(message);
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading messages", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            callback.onResult(messages);
        });
    }
}
