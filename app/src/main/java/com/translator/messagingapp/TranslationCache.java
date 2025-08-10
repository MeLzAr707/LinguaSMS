package com.translator.messagingapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for storing translated text using SQLite database with memory cache layer.
 */
public class TranslationCache {
    private static final String TAG = "TranslationCache";

    // In-memory cache for fastest access to recent translations
    private final ConcurrentHashMap<String, String> memoryCache;
    private static final int MEMORY_CACHE_SIZE = 100; // Keep most recent 100 translations in memory

    // Database helper and constants
    private final TranslationDbHelper dbHelper;
    private static final int MAX_CACHE_SIZE = 10000; // Maximum entries in the database
    private static final long CACHE_EXPIRY_MS = 30 * 24 * 60 * 60 * 1000L; // 30 days

    // Statistics
    private int cacheHits = 0;
    private int cacheMisses = 0;

    /**
     * Database helper class for the translation cache.
     */
    private static class TranslationDbHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "translations.db";
        private static final int DATABASE_VERSION = 1;

        // Table and column names
        static final String TABLE_TRANSLATIONS = "translations";
        static final String COLUMN_CACHE_KEY = "cache_key";
        static final String COLUMN_TRANSLATION = "translation";
        static final String COLUMN_TIMESTAMP = "timestamp";

        // SQL statements
        private static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_TRANSLATIONS + " (" +
                        COLUMN_CACHE_KEY + " TEXT PRIMARY KEY, " +
                        COLUMN_TRANSLATION + " TEXT NOT NULL, " +
                        COLUMN_TIMESTAMP + " INTEGER NOT NULL)";

        private static final String SQL_CREATE_INDEX =
                "CREATE INDEX idx_timestamp ON " + TABLE_TRANSLATIONS + "(" + COLUMN_TIMESTAMP + ")";

        TranslationDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_TABLE);
            db.execSQL(SQL_CREATE_INDEX);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // For future schema upgrades
            if (oldVersion < newVersion) {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSLATIONS);
                onCreate(db);
            }
        }
    }

    /**
     * Creates a new TranslationCache.
     *
     * @param context The application context
     */
    public TranslationCache(Context context) {
        this.memoryCache = new ConcurrentHashMap<>(MEMORY_CACHE_SIZE);
        this.dbHelper = new TranslationDbHelper(context.getApplicationContext());

        // Perform maintenance on startup (in background)
        new Thread(this::performMaintenance).start();
    }

    /**
     * Gets a translation from the cache.
     *
     * @param key The cache key (typically originalText_targetLanguage)
     * @return The translated text, or null if not found
     */
    public String get(String key) {
        if (key == null) {
            return null;
        }

        // Check memory cache first (fastest)
        String translation = memoryCache.get(key);

        if (translation != null) {
            // Cache hit
            cacheHits++;
            return translation;
        }

        // Check database
        SQLiteDatabase db;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();

            // Query the database
            cursor = db.query(
                    TranslationDbHelper.TABLE_TRANSLATIONS,
                    new String[]{TranslationDbHelper.COLUMN_TRANSLATION},
                    TranslationDbHelper.COLUMN_CACHE_KEY + " = ?",
                    new String[]{key},
                    null, null, null);

            if (cursor.moveToFirst()) {
                translation = cursor.getString(0);

                // Update the timestamp in background to mark as recently used
                updateTimestamp(key);

                // Add to memory cache
                addToMemoryCache(key, translation);

                cacheHits++;
                return translation;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving translation from database", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // Cache miss
        cacheMisses++;
        return null;
    }

    /**
     * Puts a translation in the cache.
     *
     * @param key The cache key (typically originalText_targetLanguage)
     * @param translation The translated text
     */
    public void put(String key, String translation) {
        if (key == null || translation == null) {
            return;
        }

        // Add to memory cache
        addToMemoryCache(key, translation);

        // Add to database
        SQLiteDatabase db;

        try {
            db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(TranslationDbHelper.COLUMN_CACHE_KEY, key);
            values.put(TranslationDbHelper.COLUMN_TRANSLATION, translation);
            values.put(TranslationDbHelper.COLUMN_TIMESTAMP, System.currentTimeMillis());

            // Insert or replace if exists
            db.insertWithOnConflict(
                    TranslationDbHelper.TABLE_TRANSLATIONS,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE);

        } catch (Exception e) {
            Log.e(TAG, "Error storing translation in database", e);
        }
    }

    /**
     * Deletes an entry from the cache.
     *
     * @param key The cache key to delete
     */
    public void delete(String key) {
        if (key == null) {
            return;
        }

        // Remove from memory cache
        memoryCache.remove(key);

        // Remove from database
        SQLiteDatabase db;
        try {
            db = dbHelper.getWritableDatabase();
            db.delete(
                    TranslationDbHelper.TABLE_TRANSLATIONS,
                    TranslationDbHelper.COLUMN_CACHE_KEY + " = ?",
                    new String[]{key});
        } catch (Exception e) {
            Log.e(TAG, "Error deleting from translation cache", e);
        }
    }

    /**
     * Saves message translation state.
     *
     * @param messageId The message ID
     * @param translationState JSON string containing translation state
     */
    public void saveMessageTranslationState(long messageId, String translationState) {
        String key = "msg_" + messageId + "_translation_state";
        put(key, translationState);
    }

    /**
     * Retrieves message translation state.
     *
     * @param messageId The message ID
     * @return JSON string containing translation state, or null if not found
     */
    public String getMessageTranslationState(long messageId) {
        String key = "msg_" + messageId + "_translation_state";
        return get(key);
    }

    /**
     * Clears translation state for a specific message.
     *
     * @param messageId The message ID
     */
    public void clearMessageTranslationState(long messageId) {
        String key = "msg_" + messageId + "_translation_state";
        delete(key);
    }

    /**
     * Clears the cache.
     */
    public void clear() {
        // Clear memory cache
        memoryCache.clear();

        // Clear database
        SQLiteDatabase db;
        try {
            db = dbHelper.getWritableDatabase();
            db.delete(TranslationDbHelper.TABLE_TRANSLATIONS, null, null);
        } catch (Exception e) {
            Log.e(TAG, "Error clearing translation cache", e);
        }

        // Reset statistics
        cacheHits = 0;
        cacheMisses = 0;
        Log.d(TAG, "Cache cleared");
    }

    /**
     * Gets cache statistics.
     *
     * @return A string containing cache statistics
     */
    public String getStatistics() {
        int total = cacheHits + cacheMisses;
        float hitRate = total > 0 ? (float) cacheHits / total * 100 : 0;
        int dbSize = getDatabaseSize();

        // Use Locale.US for consistent formatting
        return String.format(Locale.US,
                "Memory cache size: %d entries\n" +
                        "Database cache size: %d entries\n" +
                        "Hits: %d\nMisses: %d\nHit rate: %.1f%%",
                memoryCache.size(), dbSize, cacheHits, cacheMisses, hitRate);
    }

    /**
     * Gets the number of entries in the database.
     */
    private int getDatabaseSize() {
        SQLiteDatabase db;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TranslationDbHelper.TABLE_TRANSLATIONS, null);

            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting database size", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return 0;
    }

    /**
     * Updates the timestamp for a cache entry to mark it as recently used.
     */
    private void updateTimestamp(final String key) {
        new Thread(() -> {
            SQLiteDatabase db;
            try {
                db = dbHelper.getWritableDatabase();

                ContentValues values = new ContentValues();
                values.put(TranslationDbHelper.COLUMN_TIMESTAMP, System.currentTimeMillis());

                db.update(
                        TranslationDbHelper.TABLE_TRANSLATIONS,
                        values,
                        TranslationDbHelper.COLUMN_CACHE_KEY + " = ?",
                        new String[]{key});

            } catch (Exception e) {
                Log.e(TAG, "Error updating timestamp", e);
            }
        }).start();
    }

    /**
     * Adds an entry to the memory cache, managing its size.
     */
    private synchronized void addToMemoryCache(String key, String translation) {
        // If memory cache is full, remove the first entry (approximate LRU)
        if (memoryCache.size() >= MEMORY_CACHE_SIZE) {
            String firstKey = memoryCache.keySet().iterator().next();
            memoryCache.remove(firstKey);
        }

        // Add the new entry
        memoryCache.put(key, translation);
    }

    /**
     * Performs maintenance on the cache:
     * 1. Removes expired entries
     * 2. Trims the cache if it exceeds the maximum size
     */
    public void performMaintenance() {
        SQLiteDatabase db = null;

        try {
            db = dbHelper.getWritableDatabase();

            // Begin transaction for better performance
            db.beginTransaction();

            // 1. Remove expired entries
            long expiryThreshold = System.currentTimeMillis() - CACHE_EXPIRY_MS;
            int deletedExpired = db.delete(
                    TranslationDbHelper.TABLE_TRANSLATIONS,
                    TranslationDbHelper.COLUMN_TIMESTAMP + " < ?",
                    new String[]{String.valueOf(expiryThreshold)});

            // 2. Check if we still need to trim the cache
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TranslationDbHelper.TABLE_TRANSLATIONS, null);
            int count = 0;
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
                cursor.close();
            } else {
                cursor.close();
            }

            // If still too many entries, delete oldest ones
            if (count > MAX_CACHE_SIZE) {
                int toDelete = count - MAX_CACHE_SIZE;

                // Find the oldest entries
                cursor = db.query(
                        TranslationDbHelper.TABLE_TRANSLATIONS,
                        new String[]{TranslationDbHelper.COLUMN_CACHE_KEY},
                        null, null, null, null,
                        TranslationDbHelper.COLUMN_TIMESTAMP + " ASC",
                        String.valueOf(toDelete));

                while (cursor.moveToNext()) {
                    String key = cursor.getString(0);
                    db.delete(
                            TranslationDbHelper.TABLE_TRANSLATIONS,
                            TranslationDbHelper.COLUMN_CACHE_KEY + " = ?",
                            new String[]{key});
                }
                cursor.close();
            }

            // Commit the transaction
            db.setTransactionSuccessful();

            // Use Locale.US for consistent formatting in log messages
            Log.d(TAG, String.format(Locale.US,
                    "Cache maintenance completed. Removed %d expired entries.",
                    deletedExpired));

        } catch (Exception e) {
            Log.e(TAG, "Error during cache maintenance", e);
        } finally {
            if (db != null && db.inTransaction()) {
                db.endTransaction();
            }
        }
    }

    /**
     * Closes the database helper.
     * Should be called when the app is being destroyed.
     */
    public void close() {
        dbHelper.close();
    }
}



