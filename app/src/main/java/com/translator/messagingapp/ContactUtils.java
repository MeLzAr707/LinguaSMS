package com.translator.messagingapp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
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

        // Try multiple phone number formats for better contact matching
        String[] phoneVariants = getPhoneNumberVariants(phoneNumber);
        
        for (String variant : phoneVariants) {
            String contactName = lookupContactName(context, variant);
            if (!TextUtils.isEmpty(contactName)) {
                return contactName;
            }
        }

        return null;
    }
    
    /**
     * Looks up contact name for a specific phone number variant.
     */
    private static String lookupContactName(Context context, String phoneNumber) {
        // Validate phone number to prevent IllegalArgumentException with empty URIs
        if (TextUtils.isEmpty(phoneNumber)) {
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
                            String contactName = cursor.getString(nameIndex);
                            // Ensure we don't return string "null" or empty strings
                            if (!TextUtils.isEmpty(contactName) && !"null".equals(contactName)) {
                                return contactName;
                            }
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
     * Gets different phone number variants to try for contact lookup.
     */
    private static String[] getPhoneNumberVariants(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return new String[]{phoneNumber};
        }
        
        // Remove any non-digit characters for processing
        String digitsOnly = phoneNumber.replaceAll("[^\\d]", "");
        
        // Create variants to try
        String[] variants = new String[4];
        variants[0] = phoneNumber; // Original format
        
        if (digitsOnly.length() >= 10) {
            // US number variants
            if (digitsOnly.length() == 11 && digitsOnly.startsWith("1")) {
                // Remove US country code
                String withoutCountryCode = digitsOnly.substring(1);
                variants[1] = withoutCountryCode;
                variants[2] = formatAsPhoneNumber(withoutCountryCode);
                variants[3] = "+" + digitsOnly;
            } else if (digitsOnly.length() == 10) {
                // Add US country code
                variants[1] = "1" + digitsOnly;
                variants[2] = "+" + "1" + digitsOnly;
                variants[3] = formatAsPhoneNumber(digitsOnly);
            } else {
                // International number
                variants[1] = "+" + digitsOnly;
                variants[2] = digitsOnly;
                variants[3] = phoneNumber;
            }
        } else {
            // Short number, try as-is
            variants[1] = digitsOnly;
            variants[2] = phoneNumber;
            variants[3] = phoneNumber;
        }
        
        return variants;
    }
    
    /**
     * Formats a phone number as (XXX) XXX-XXXX.
     */
    private static String formatAsPhoneNumber(String digitsOnly) {
        if (digitsOnly.length() == 10) {
            return String.format("(%s) %s-%s",
                    digitsOnly.substring(0, 3),
                    digitsOnly.substring(3, 6),
                    digitsOnly.substring(6));
        }
        return digitsOnly;
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

                // Validate normalized number before using in URI
                if (TextUtils.isEmpty(normalizedNumber)) {
                    continue;
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
     * Class to hold contact information including name and photo URI.
     */
    public static class ContactInfo {
        private final String name;
        private final String photoUri;

        /**
         * Creates a new ContactInfo.
         *
         * @param name The contact name
         * @param photoUri The photo URI as a string
         */
        public ContactInfo(String name, String photoUri) {
            this.name = name;
            this.photoUri = photoUri;
        }

        /**
         * Gets the contact name.
         *
         * @return The contact name
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the photo URI as a string.
         *
         * @return The photo URI string
         */
        public String getPhotoUri() {
            return photoUri;
        }

        /**
         * Gets the photo URI.
         *
         * @return The photo URI, or null if not available
         */
        public Uri getPhotoUriObject() {
            return photoUri != null ? Uri.parse(photoUri) : null;
        }

        /**
         * Checks if this contact has a photo.
         *
         * @return True if the contact has a photo, false otherwise
         */
        public boolean hasPhoto() {
            return photoUri != null && !photoUri.isEmpty();
        }
    }
    /**
     * Gets contact information for a phone number.
     *
     * @param context The context
     * @param phoneNumber The phone number
     * @return The ContactInfo object, or a ContactInfo with null values if not found
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
     * Enhanced contact synchronization utilities for cross-platform messaging.
     */
    
    /**
     * Extended contact information class with additional synchronization data.
     */
    public static class EnhancedContactInfo {
        private String name;
        private String photoUri;
        public String email;
        public List<String> socialMediaProfiles;
        public String organization;
        public long lastSyncTime;
        public boolean isSyncEnabled;
        public Map<String, String> platformIds; // Platform name -> User ID mapping
        
        public EnhancedContactInfo(String name, String photoUri) {
            this.name = name;
            this.photoUri = photoUri;
            this.socialMediaProfiles = new ArrayList<>();
            this.platformIds = new HashMap<>();
            this.lastSyncTime = 0;
            this.isSyncEnabled = false;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getPhotoUri() {
            return photoUri;
        }
        
        public void setPhotoUri(String photoUri) {
            this.photoUri = photoUri;
        }
        
        public void addPlatformId(String platform, String userId) {
            if (platformIds == null) {
                platformIds = new HashMap<>();
            }
            platformIds.put(platform, userId);
        }
        
        public String getPlatformId(String platform) {
            return platformIds != null ? platformIds.get(platform) : null;
        }
        
        public boolean hasMultiplePlatforms() {
            return platformIds != null && platformIds.size() > 1;
        }
    }
    
    /**
     * Gets enhanced contact information including cross-platform data.
     *
     * @param context The context
     * @param phoneNumber The phone number
     * @return Enhanced contact information
     */
    public static EnhancedContactInfo getEnhancedContactInfo(Context context, String phoneNumber) {
        if (context == null || TextUtils.isEmpty(phoneNumber)) {
            return new EnhancedContactInfo(null, null);
        }

        try {
            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            Cursor cursor = contentResolver.query(
                    uri,
                    new String[]{
                            ContactsContract.PhoneLookup.DISPLAY_NAME,
                            ContactsContract.PhoneLookup.PHOTO_URI,
                            ContactsContract.PhoneLookup._ID,
                            ContactsContract.PhoneLookup.LOOKUP_KEY
                    },
                    null,
                    null,
                    null);

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                        int photoUriIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI);
                        int idIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID);

                        String name = nameIndex >= 0 ? cursor.getString(nameIndex) : null;
                        String photoUri = photoUriIndex >= 0 ? cursor.getString(photoUriIndex) : null;
                        String contactId = idIndex >= 0 ? cursor.getString(idIndex) : null;

                        EnhancedContactInfo enhancedInfo = new EnhancedContactInfo(name, photoUri);
                        
                        // Get additional contact details
                        if (contactId != null) {
                            loadAdditionalContactData(context, contactId, enhancedInfo);
                        }
                        
                        return enhancedInfo;
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting enhanced contact info for " + phoneNumber, e);
        }

        return new EnhancedContactInfo(null, null);
    }
    
    /**
     * Loads additional contact data like email and organization.
     */
    private static void loadAdditionalContactData(Context context, String contactId, EnhancedContactInfo contactInfo) {
        ContentResolver contentResolver = context.getContentResolver();
        
        // Get email addresses
        Cursor emailCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Email.ADDRESS},
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=?",
                new String[]{contactId},
                null);
        
        if (emailCursor != null) {
            try {
                if (emailCursor.moveToFirst()) {
                    int emailIndex = emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS);
                    if (emailIndex >= 0) {
                        contactInfo.email = emailCursor.getString(emailIndex);
                    }
                }
            } finally {
                emailCursor.close();
            }
        }
        
        // Get organization
        Cursor orgCursor = contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Organization.COMPANY},
                ContactsContract.Data.CONTACT_ID + "=? AND " + 
                ContactsContract.Data.MIMETYPE + "=?",
                new String[]{contactId, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE},
                null);
        
        if (orgCursor != null) {
            try {
                if (orgCursor.moveToFirst()) {
                    int orgIndex = orgCursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY);
                    if (orgIndex >= 0) {
                        contactInfo.organization = orgCursor.getString(orgIndex);
                    }
                }
            } finally {
                orgCursor.close();
            }
        }
    }
    
    /**
     * Interface for cross-platform contact synchronization.
     */
    public interface ContactSyncProvider {
        String getPlatformName();
        boolean isAvailable(Context context);
        List<EnhancedContactInfo> getContacts(Context context);
        boolean syncContact(Context context, EnhancedContactInfo contact);
        long getLastSyncTime(Context context);
    }
    
    /**
     * Manages synchronization across multiple messaging platforms.
     */
    public static class CrossPlatformContactSync {
        private static final String TAG = "CrossPlatformContactSync";
        private List<ContactSyncProvider> syncProviders;
        
        public CrossPlatformContactSync() {
            this.syncProviders = new ArrayList<>();
        }
        
        public void addSyncProvider(ContactSyncProvider provider) {
            if (provider != null && !syncProviders.contains(provider)) {
                syncProviders.add(provider);
            }
        }
        
        public void removeSyncProvider(ContactSyncProvider provider) {
            syncProviders.remove(provider);
        }
        
        /**
         * Synchronizes contacts across all registered platforms.
         */
        public boolean synchronizeContacts(Context context) {
            if (context == null || syncProviders.isEmpty()) {
                return false;
            }
            
            boolean overallSuccess = true;
            Map<String, EnhancedContactInfo> unifiedContacts = new HashMap<>();
            
            // Collect contacts from all platforms
            for (ContactSyncProvider provider : syncProviders) {
                if (provider.isAvailable(context)) {
                    try {
                        List<EnhancedContactInfo> platformContacts = provider.getContacts(context);
                        for (EnhancedContactInfo contact : platformContacts) {
                            String key = generateContactKey(contact);
                            if (unifiedContacts.containsKey(key)) {
                                // Merge contact information
                                mergeContactInfo(unifiedContacts.get(key), contact, provider.getPlatformName());
                            } else {
                                contact.addPlatformId(provider.getPlatformName(), contact.getName());
                                unifiedContacts.put(key, contact);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error syncing contacts from " + provider.getPlatformName(), e);
                        overallSuccess = false;
                    }
                }
            }
            
            // Update sync timestamps
            long currentTime = System.currentTimeMillis();
            for (EnhancedContactInfo contact : unifiedContacts.values()) {
                contact.lastSyncTime = currentTime;
                contact.isSyncEnabled = true;
            }
            
            Log.d(TAG, "Synchronized " + unifiedContacts.size() + " contacts across " + syncProviders.size() + " platforms");
            return overallSuccess;
        }
        
        /**
         * Generates a unique key for contact deduplication.
         */
        private String generateContactKey(EnhancedContactInfo contact) {
            StringBuilder key = new StringBuilder();
            if (!TextUtils.isEmpty(contact.getName())) {
                key.append(contact.getName().toLowerCase().trim());
            }
            if (!TextUtils.isEmpty(contact.email)) {
                key.append("|").append(contact.email.toLowerCase().trim());
            }
            return key.toString();
        }
        
        /**
         * Merges contact information from different platforms.
         */
        private void mergeContactInfo(EnhancedContactInfo existing, EnhancedContactInfo newContact, String platformName) {
            // Add platform ID
            existing.addPlatformId(platformName, newContact.getName());
            
            // Merge email if missing
            if (TextUtils.isEmpty(existing.email) && !TextUtils.isEmpty(newContact.email)) {
                existing.email = newContact.email;
            }
            
            // Merge organization if missing
            if (TextUtils.isEmpty(existing.organization) && !TextUtils.isEmpty(newContact.organization)) {
                existing.organization = newContact.organization;
            }
            
            // Merge photo URI if missing
            if (TextUtils.isEmpty(existing.getPhotoUri()) && !TextUtils.isEmpty(newContact.getPhotoUri())) {
                existing.setPhotoUri(newContact.getPhotoUri());
            }
            
            // Merge social media profiles
            if (newContact.socialMediaProfiles != null) {
                for (String profile : newContact.socialMediaProfiles) {
                    if (!existing.socialMediaProfiles.contains(profile)) {
                        existing.socialMediaProfiles.add(profile);
                    }
                }
            }
        }
        
        /**
         * Gets contacts that are available across multiple platforms.
         */
        public List<EnhancedContactInfo> getMultiPlatformContacts(Context context) {
            List<EnhancedContactInfo> multiPlatformContacts = new ArrayList<>();
            
            // This would be implemented to return contacts that exist on multiple platforms
            // For now, return empty list as this would require actual platform integration
            
            return multiPlatformContacts;
        }
        
        /**
         * Gets the sync status for all platforms.
         */
        public Map<String, Long> getSyncStatus(Context context) {
            Map<String, Long> syncStatus = new HashMap<>();
            
            for (ContactSyncProvider provider : syncProviders) {
                if (provider.isAvailable(context)) {
                    syncStatus.put(provider.getPlatformName(), provider.getLastSyncTime(context));
                }
            }
            
            return syncStatus;
        }
    }
    
    /**
     * Utility methods for enhanced contact integration.
     */
    
    /**
     * Checks if a contact exists across multiple messaging platforms.
     */
    public static boolean isMultiPlatformContact(Context context, String phoneNumber) {
        EnhancedContactInfo contact = getEnhancedContactInfo(context, phoneNumber);
        return contact.hasMultiplePlatforms();
    }
    
    /**
     * Gets suggested contacts based on recent message activity.
     */
    public static List<String> getSuggestedContacts(Context context, int limit) {
        List<String> suggestedContacts = new ArrayList<>();
        
        // This would analyze recent message patterns and suggest contacts
        // Implementation would depend on message history analysis
        
        return suggestedContacts;
    }
    
    /**
     * Updates contact information with data from messaging platforms.
     */
    public static boolean updateContactFromPlatforms(Context context, String phoneNumber) {
        // This would update local contact with information gathered from various platforms
        // Implementation would depend on specific platform APIs
        
        Log.d(TAG, "Contact platform update requested for: " + phoneNumber);
        return true; // Placeholder return
    }
}




