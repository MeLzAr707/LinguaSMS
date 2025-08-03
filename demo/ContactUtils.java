package com.translator.messagingapp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for contact-related operations.
 * DEMO VERSION: Simplified without Android dependencies
 */
public class ContactUtils {
    private static final String TAG = "ContactUtils";

    /**
     * Gets the first letter of a contact name or phone number.
     *
     * @param contactName The contact name or phone number
     * @return The first letter, or "#" if not available
     */
    public static String getContactInitial(String contactName) {
        if (contactName == null || contactName.trim().isEmpty()) {
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
        if (contactNameOrNumber == null || contactNameOrNumber.trim().isEmpty()) {
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
}




