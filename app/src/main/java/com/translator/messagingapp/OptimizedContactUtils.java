package com.translator.messagingapp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optimized utility class for contact operations with batch processing capabilities.
 */
public class OptimizedContactUtils {
    private static final String TAG = "OptimizedContactUtils";
    
    // Cache for contact names to reduce repeated lookups
    private static final Map<String, String> contactNameCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 500;
    
    /**
     * Gets contact names for multiple phone numbers in a single batch query.
     *
     * @param context The context
     * @param phoneNumbers List of phone numbers to look up
     * @return Map of phone number to contact name
     */
    public static Map<String, String> getContactNamesForNumbers(Context context, List<String> phoneNumbers) {
        Map<String, String> contactNames = new HashMap<>();
        
        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            return contactNames;
        }
        
        Log.d(TAG, "Batch looking up " + phoneNumbers.size() + " contacts");
        
        // Check cache first
        List<String> numbersToLookup = new ArrayList<>();
        for (String number : phoneNumbers) {
            if (contactNameCache.containsKey(number)) {
                contactNames.put(number, contactNameCache.get(number));
            } else {
                numbersToLookup.add(number);
            }
        }
        
        if (numbersToLookup.isEmpty()) {
            Log.d(TAG, "All contacts found in cache");
            return contactNames;
        }
        
        Log.d(TAG, "Looking up " + numbersToLookup.size() + " contacts from database");
        
        // Build selection for batch query
        ContentResolver resolver = context.getContentResolver();
        
        // Process in smaller batches to avoid query size limitations
        int batchSize = 20;
        for (int i = 0; i < numbersToLookup.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, numbersToLookup.size());
            List<String> batch = numbersToLookup.subList(i, endIndex);
            
            Map<String, String> batchResults = lookupContactBatch(resolver, batch);
            contactNames.putAll(batchResults);
            
            // Add to cache
            for (Map.Entry<String, String> entry : batchResults.entrySet()) {
                // Manage cache size
                if (contactNameCache.size() >= MAX_CACHE_SIZE) {
                    // Simple strategy: clear half the cache when it gets full
                    List<String> keysToRemove = new ArrayList<>(contactNameCache.keySet());
                    for (int j = 0; j < keysToRemove.size() / 2; j++) {
                        contactNameCache.remove(keysToRemove.get(j));
                    }
                }
                
                contactNameCache.put(entry.getKey(), entry.getValue());
            }
        }
        
        return contactNames;
    }
    
    /**
     * Looks up a batch of contacts.
     *
     * @param resolver ContentResolver
     * @param phoneNumbers List of phone numbers to look up
     * @return Map of phone number to contact name
     */
    private static Map<String, String> lookupContactBatch(ContentResolver resolver, List<String> phoneNumbers) {
        Map<String, String> results = new HashMap<>();
        
        if (phoneNumbers.isEmpty()) {
            return results;
        }
        
        // Build selection string for batch query
        StringBuilder selection = new StringBuilder();
        String[] selectionArgs = new String[phoneNumbers.size()];
        
        for (int i = 0; i < phoneNumbers.size(); i++) {
            if (i > 0) selection.append(" OR ");
            selection.append(ContactsContract.PhoneLookup.NUMBER).append(" LIKE ?");
            selectionArgs[i] = phoneNumbers.get(i);
        }
        
        // Execute batch query
        Cursor cursor = null;
        try {
            cursor = resolver.query(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    new String[]{
                            ContactsContract.PhoneLookup.NUMBER,
                            ContactsContract.PhoneLookup.DISPLAY_NAME
                    },
                    selection.toString(),
                    selectionArgs,
                    null
            );
            
            if (cursor != null) {
                int numberIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.NUMBER);
                int nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                
                while (cursor.moveToNext()) {
                    String number = cursor.getString(numberIndex);
                    String name = cursor.getString(nameIndex);
                    
                    if (!TextUtils.isEmpty(name)) {
                        results.put(number, name);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in batch contact lookup", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        // For any numbers not found, use the number as the name
        for (String number : phoneNumbers) {
            if (!results.containsKey(number)) {
                results.put(number, number);
            }
        }
        
        return results;
    }
    
    /**
     * Gets a contact name for a single phone number.
     * Uses the cache if available.
     *
     * @param context The context
     * @param phoneNumber The phone number
     * @return The contact name, or the phone number if not found
     */
    public static String getContactName(Context context, String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return "Unknown";
        }
        
        // Check cache first
        if (contactNameCache.containsKey(phoneNumber)) {
            return contactNameCache.get(phoneNumber);
        }
        
        // Look up single contact
        List<String> numbers = new ArrayList<>();
        numbers.add(phoneNumber);
        Map<String, String> result = getContactNamesForNumbers(context, numbers);
        
        return result.getOrDefault(phoneNumber, phoneNumber);
    }
    
    /**
     * Clears the contact name cache.
     */
    public static void clearCache() {
        contactNameCache.clear();
        Log.d(TAG, "Contact cache cleared");
    }
}