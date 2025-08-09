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
     * IMPROVED: Uses phone number variants to handle different formats.
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
        
        Log.d(TAG, "Looking up contacts for " + phoneNumbers.size() + " phone numbers");
        
        // For each phone number, try different variants
        for (String originalNumber : phoneNumbers) {
            if (TextUtils.isEmpty(originalNumber)) {
                continue;
            }
            
            // Skip if we already found this number
            if (results.containsKey(originalNumber)) {
                continue;
            }
            
            String contactName = lookupSingleContact(resolver, originalNumber);
            if (!TextUtils.isEmpty(contactName)) {
                results.put(originalNumber, contactName);
                Log.d(TAG, "Found contact: " + contactName + " for number: " + originalNumber);
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
     * Looks up a single contact trying multiple phone number variants.
     *
     * @param resolver ContentResolver
     * @param phoneNumber The phone number to look up
     * @return The contact name, or null if not found
     */
    private static String lookupSingleContact(ContentResolver resolver, String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return null;
        }
        
        // Get different variants of the phone number
        String[] phoneVariants = PhoneUtils.getPhoneNumberVariants(phoneNumber);
        
        for (String variant : phoneVariants) {
            if (TextUtils.isEmpty(variant)) {
                continue;
            }
            
            Cursor cursor = null;
            try {
                // Use PhoneLookup for each variant
                Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(variant));
                cursor = resolver.query(
                        uri,
                        new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                        null,
                        null,
                        null);
                
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        String name = cursor.getString(nameIndex);
                        if (!TextUtils.isEmpty(name)) {
                            Log.d(TAG, "Found contact name: " + name + " for variant: " + variant + " (original: " + phoneNumber + ")");
                            return name;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error looking up contact for variant: " + variant, e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        
        return null;
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