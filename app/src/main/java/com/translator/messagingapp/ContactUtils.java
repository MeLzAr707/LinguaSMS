package com.translator.messagingapp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for contact-related operations.
 */
public class ContactUtils {
    private static final String TAG = "ContactUtils";

    /**
     * Gets the contact name for a phone number.
     *
     * @param context     The context
     * @param phoneNumber The phone number
     * @return The contact name, or null if not found
     */
    public static String getContactName(Context context, String phoneNumber) {
        if (context == null || TextUtils.isEmpty(phoneNumber)) {
            return null;
        }

        try {
            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            Cursor cursor = contentResolver.query(
                    uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                    null,
                    null,
                    null);

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                        if (nameIndex >= 0) {
                            return cursor.getString(nameIndex);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact name for " + phoneNumber, e);
        }

        return null;
    }

    /**
     * Gets contact names for multiple phone numbers in a batch operation.
     *
     * @param context      The context
     * @param phoneNumbers List of phone numbers to look up
     * @return Map of phone numbers to contact names
     */
    public static Map<String, String> getContactNamesForNumbers(Context context, List<String> phoneNumbers) {
        if (context == null || phoneNumbers == null || phoneNumbers.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, String> result = new HashMap<>();

        try {
            ContentResolver contentResolver = context.getContentResolver();

            // Process in batches to avoid excessive queries
            for (String phoneNumber : phoneNumbers) {
                if (TextUtils.isEmpty(phoneNumber)) {
                    continue;
                }

                // Skip if we already have this number
                if (result.containsKey(phoneNumber)) {
                    continue;
                }

                // Try to normalize the phone number
                String normalizedNumber = phoneNumber;
                try {
                    normalizedNumber = PhoneNumberUtils.normalizeNumber(phoneNumber);
                } catch (Exception e) {
                    // Ignore normalization errors
                }

                // Query the contact
                Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(normalizedNumber));
                Cursor cursor = contentResolver.query(
                        uri,
                        new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                        null,
                        null,
                        null);

                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            int nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                            if (nameIndex >= 0) {
                                String contactName = cursor.getString(nameIndex);
                                if (!TextUtils.isEmpty(contactName)) {
                                    result.put(phoneNumber, contactName);
                                }
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error batch looking up contact names", e);
        }

        return result;
    }

    /**
     * Gets the first letter of a contact name or phone number.
     *
     * @param contactName The contact name or phone number
     * @return The first letter, or "#" if not available
     */
    public static String getContactInitial(String contactName) {
        if (TextUtils.isEmpty(contactName)) {
            return "#";
        }

        // Clean up the name
        String cleanName = contactName.trim();
        if (cleanName.isEmpty()) {
            return "#";
        }

        // Get the first character
        char firstChar = cleanName.charAt(0);

        // Check if it's a letter
        if (Character.isLetter(firstChar)) {
            return String.valueOf(Character.toUpperCase(firstChar));
        }

        // For phone numbers, use the first digit
        if (Character.isDigit(firstChar)) {
            return String.valueOf(firstChar);
        }

        // For other characters, use #
        return "#";
    }

    /**
     * Gets a color for a contact based on their name or phone number.
     *
     * @param contactNameOrNumber The contact name or phone number
     * @return A color value
     */
    public static int getContactColor(String contactNameOrNumber) {
        if (TextUtils.isEmpty(contactNameOrNumber)) {
            return 0xFF9E9E9E; // Default gray
        }

        // Define a set of material colors
        int[] colors = {
                0xFFE57373, // Red
                0xFFF06292, // Pink
                0xFFBA68C8, // Purple
                0xFF9575CD, // Deep Purple
                0xFF7986CB, // Indigo
                0xFF64B5F6, // Blue
                0xFF4FC3F7, // Light Blue
                0xFF4DD0E1, // Cyan
                0xFF4DB6AC, // Teal
                0xFF81C784, // Green
                0xFFAED581, // Light Green
                0xFFFF8A65, // Deep Orange
                0xFFD4E157, // Lime
                0xFFFFD54F, // Amber
                0xFFFFB74D, // Orange
                0xFFA1887F  // Brown
        };

        // Use the hash code of the string to pick a color
        int hashCode = contactNameOrNumber.hashCode();
        int index = Math.abs(hashCode) % colors.length;

        return colors[index];
    }

    /**
     * Gets the contact photo URI for a phone number.
     *
     * @param context     The context
     * @param phoneNumber The phone number
     * @return The contact photo URI, or null if not found
     */
    public static String getContactPhotoUri(Context context, String phoneNumber) {
        if (context == null || TextUtils.isEmpty(phoneNumber)) {
            return null;
        }

        try {
            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            Cursor cursor = contentResolver.query(
                    uri,
                    new String[]{ContactsContract.PhoneLookup.PHOTO_URI},
                    null,
                    null,
                    null);

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int photoUriIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI);
                        if (photoUriIndex >= 0) {
                            return cursor.getString(photoUriIndex);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact photo URI for " + phoneNumber, e);
        }

        return null;
    }

    /**
     * Gets contact details including name and photo URI for a phone number.
     *
     * @param context     The context
     * @param phoneNumber The phone number
     * @return ContactInfo object with name and photo URI
     */
    public static ContactInfo getContactInfo(Context context, String phoneNumber) {
        if (context == null || TextUtils.isEmpty(phoneNumber)) {
            return new ContactInfo(null, null);
        }

        try {
            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            Cursor cursor = contentResolver.query(
                    uri,
                    new String[]{
                        ContactsContract.PhoneLookup.DISPLAY_NAME,
                        ContactsContract.PhoneLookup.PHOTO_URI
                    },
                    null,
                    null,
                    null);

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                        int photoUriIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI);
                        
                        String name = nameIndex >= 0 ? cursor.getString(nameIndex) : null;
                        String photoUri = photoUriIndex >= 0 ? cursor.getString(photoUriIndex) : null;
                        
                        return new ContactInfo(name, photoUri);
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact info for " + phoneNumber, e);
        }

        return new ContactInfo(null, null);
    }

    /**
     * Simple class to hold contact information.
     */
    public static class ContactInfo {
        public final String name;
        public final String photoUri;

        public ContactInfo(String name, String photoUri) {
            this.name = name;
            this.photoUri = photoUri;
        }
    }
}




