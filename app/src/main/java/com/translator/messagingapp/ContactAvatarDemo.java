package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Example usage of the ContactAvatarHelper in different scenarios.
 * This class demonstrates how the priority system works.
 */
public class ContactAvatarDemo {
    private static final String TAG = "ContactAvatarDemo";

    /**
     * Demo: Loading avatar for a contact with photo
     */
    public static void demoContactWithPhoto(Context context, CircleImageView imageView) {
        // Create a mock conversation for a contact with a photo
        Conversation conversation = new Conversation();
        conversation.setContactName("John Doe");
        conversation.setAddress("+1234567890");
        
        Log.d(TAG, "Loading avatar for contact with photo: " + conversation.getContactName());
        
        // Priority 1: Will try to load actual contact photo
        ContactAvatarHelper.loadContactAvatar(context, imageView, conversation);
        // If photo exists → shows contact photo
        // If photo doesn't exist → falls back to initials "JD" with color
    }

    /**
     * Demo: Loading avatar for phone number only (no contact name)
     */
    public static void demoPhoneNumberOnly(Context context, CircleImageView imageView) {
        Conversation conversation = new Conversation();
        conversation.setContactName(null); // No contact name
        conversation.setAddress("+1987654321");
        
        Log.d(TAG, "Loading avatar for phone number only: " + conversation.getAddress());
        
        // Priority 2: Will generate initials from phone number
        ContactAvatarHelper.loadContactAvatar(context, imageView, conversation);
        // Result: Shows "1" (first digit) with colored background
    }

    /**
     * Demo: Loading avatar for unknown contact
     */
    public static void demoUnknownContact(Context context, CircleImageView imageView) {
        Conversation conversation = new Conversation();
        conversation.setContactName("");
        conversation.setAddress("");
        
        Log.d(TAG, "Loading avatar for unknown contact");
        
        // Priority 4: Will show default avatar
        ContactAvatarHelper.loadContactAvatar(context, imageView, conversation);
        // Result: Shows "?" with gray background or default circle
    }

    /**
     * Demo: Loading avatar for named contact without photo
     */
    public static void demoNamedContactNoPhoto(Context context, CircleImageView imageView) {
        Conversation conversation = new Conversation();
        conversation.setContactName("Jane Smith");
        conversation.setAddress("+1555000123");
        
        Log.d(TAG, "Loading avatar for named contact: " + conversation.getContactName());
        
        // Priority 2: Will generate initials from name
        ContactAvatarHelper.loadContactAvatar(context, imageView, conversation);
        // Result: Shows "JS" with colored background based on "Jane Smith"
    }

    /**
     * Demo: Color consistency
     */
    public static void demoColorConsistency(Context context) {
        String contactName = "Alice Johnson";
        
        // Same contact should always get the same color
        int color1 = ContactUtils.getContactColor(contactName);
        int color2 = ContactUtils.getContactColor(contactName);
        
        Log.d(TAG, "Color consistency test:");
        Log.d(TAG, "Color 1: " + Integer.toHexString(color1));
        Log.d(TAG, "Color 2: " + Integer.toHexString(color2));
        Log.d(TAG, "Colors match: " + (color1 == color2));
        
        // Different contacts should get different colors
        int aliceColor = ContactUtils.getContactColor("Alice Johnson");
        int bobColor = ContactUtils.getContactColor("Bob Wilson");
        
        Log.d(TAG, "Alice color: " + Integer.toHexString(aliceColor));
        Log.d(TAG, "Bob color: " + Integer.toHexString(bobColor));
        Log.d(TAG, "Colors different: " + (aliceColor != bobColor));
    }

    /**
     * Demo: Initials generation
     */
    public static void demoInitialsGeneration() {
        Log.d(TAG, "Initials generation demo:");
        
        // Test various name formats
        String[] testNames = {
            "John Doe",           // Expected: "JD"
            "Alice",              // Expected: "A"
            "+1234567890",        // Expected: "1"
            "Mary Jane Watson",   // Expected: "MJ" (first two words)
            "",                   // Expected: "?"
            "123-456-7890",       // Expected: "1"
            "Dr. Smith"           // Expected: "DS"
        };
        
        for (String name : testNames) {
            // Note: This uses internal logic similar to ContactAvatarHelper
            String initials = getTestInitials(name);
            Log.d(TAG, "Name: '" + name + "' → Initials: '" + initials + "'");
        }
    }
    
    /**
     * Test version of initials generation for demo purposes
     */
    private static String getTestInitials(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return "?";
        }

        String trimmed = displayName.trim();
        
        // For phone numbers, use first digit
        if (trimmed.matches("^[+]?[0-9()\\s-]+$")) {
            for (char c : trimmed.toCharArray()) {
                if (Character.isDigit(c)) {
                    return String.valueOf(c);
                }
            }
            return "#";
        }

        // For names, get first letters of first two words
        String[] words = trimmed.split("\\s+");
        StringBuilder initials = new StringBuilder();
        
        for (int i = 0; i < Math.min(2, words.length); i++) {
            String word = words[i].trim();
            if (!word.isEmpty()) {
                char firstChar = Character.toUpperCase(word.charAt(0));
                if (Character.isLetter(firstChar)) {
                    initials.append(firstChar);
                }
            }
        }

        if (initials.length() > 0) {
            return initials.toString();
        }

        // Fallback to first character
        char firstChar = Character.toUpperCase(trimmed.charAt(0));
        return Character.isLetter(firstChar) ? String.valueOf(firstChar) : "?";
    }
}