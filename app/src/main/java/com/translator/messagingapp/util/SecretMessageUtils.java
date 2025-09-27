package com.translator.messagingapp.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for encoding and decoding secret messages using zero-width whitespace characters.
 * This implementation uses Unicode zero-width characters to hide secret text within normal messages.
 */
public class SecretMessageUtils {
    
    // Zero-width Unicode characters for binary encoding
    private static final char ZERO_WIDTH_SPACE = '\u200B';           // Binary 0
    private static final char ZERO_WIDTH_NON_JOINER = '\u200C';      // Binary 1
    private static final char ZERO_WIDTH_JOINER = '\u200D';          // Start marker
    private static final char ZERO_WIDTH_NO_BREAK_SPACE = '\uFEFF';  // End marker
    
    /**
     * Encodes a secret message into zero-width characters and embeds it in the visible message.
     * 
     * @param visibleMessage The visible message text
     * @param secretMessage The secret message to encode
     * @return The visible message with embedded secret message
     */
    public static String encodeSecretMessage(String visibleMessage, String secretMessage) {
        if (visibleMessage == null || secretMessage == null || secretMessage.isEmpty()) {
            return visibleMessage;
        }
        
        // Convert secret message to binary representation using zero-width characters
        StringBuilder encodedSecret = new StringBuilder();
        encodedSecret.append(ZERO_WIDTH_JOINER); // Start marker
        
        // Encode each character of the secret message
        for (char c : secretMessage.toCharArray()) {
            // Convert character to 8-bit binary and encode using zero-width chars
            String binary = String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0');
            for (char bit : binary.toCharArray()) {
                if (bit == '0') {
                    encodedSecret.append(ZERO_WIDTH_SPACE);
                } else {
                    encodedSecret.append(ZERO_WIDTH_NON_JOINER);
                }
            }
        }
        
        encodedSecret.append(ZERO_WIDTH_NO_BREAK_SPACE); // End marker
        
        // Insert the encoded secret at word boundaries in the visible message
        return insertAtWordBoundaries(visibleMessage, encodedSecret.toString());
    }
    
    /**
     * Detects and decodes a secret message from text containing zero-width characters.
     * 
     * @param messageText The message text that may contain encoded secret
     * @return The decoded secret message, or null if no secret message found
     */
    public static String decodeSecretMessage(String messageText) {
        if (messageText == null || messageText.isEmpty()) {
            return null;
        }
        
        // Find start and end markers
        int startIndex = messageText.indexOf(ZERO_WIDTH_JOINER);
        int endIndex = messageText.indexOf(ZERO_WIDTH_NO_BREAK_SPACE);
        
        if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
            return null; // No secret message found
        }
        
        // Extract the encoded portion
        String encodedPortion = messageText.substring(startIndex + 1, endIndex);
        
        // Remove all non-zero-width characters from the encoded portion
        StringBuilder binaryString = new StringBuilder();
        for (char c : encodedPortion.toCharArray()) {
            if (c == ZERO_WIDTH_SPACE) {
                binaryString.append('0');
            } else if (c == ZERO_WIDTH_NON_JOINER) {
                binaryString.append('1');
            }
            // Ignore other characters
        }
        
        // Convert binary back to characters
        StringBuilder decodedMessage = new StringBuilder();
        String binary = binaryString.toString();
        
        // Process 8-bit chunks
        for (int i = 0; i < binary.length(); i += 8) {
            if (i + 8 <= binary.length()) {
                String byteString = binary.substring(i, i + 8);
                try {
                    int charValue = Integer.parseInt(byteString, 2);
                    if (charValue > 0 && charValue <= 127) { // Valid ASCII range
                        decodedMessage.append((char) charValue);
                    }
                } catch (NumberFormatException e) {
                    // Invalid binary sequence, skip
                }
            }
        }
        
        return decodedMessage.length() > 0 ? decodedMessage.toString() : null;
    }
    
    /**
     * Checks if a message contains a secret message.
     * 
     * @param messageText The message text to check
     * @return True if the message contains secret markers
     */
    public static boolean hasSecretMessage(String messageText) {
        if (messageText == null || messageText.isEmpty()) {
            return false;
        }
        
        return messageText.indexOf(ZERO_WIDTH_JOINER) != -1 && 
               messageText.indexOf(ZERO_WIDTH_NO_BREAK_SPACE) != -1;
    }
    
    /**
     * Removes secret message encoding from text, returning only the visible message.
     * 
     * @param messageText The message text that may contain encoded secret
     * @return The visible message with secret encoding removed
     */
    public static String removeSecretMessage(String messageText) {
        if (messageText == null || messageText.isEmpty()) {
            return messageText;
        }
        
        // Remove all zero-width characters
        return messageText.replaceAll("[\u200B\u200C\u200D\uFEFF]", "");
    }
    
    /**
     * Inserts encoded secret at word boundaries in the visible message.
     */
    private static String insertAtWordBoundaries(String visibleMessage, String encodedSecret) {
        if (visibleMessage.isEmpty()) {
            return encodedSecret;
        }
        
        // Find word boundaries (spaces, punctuation) and distribute the encoded secret
        List<Integer> wordBoundaries = new ArrayList<>();
        wordBoundaries.add(0); // Start of message
        
        for (int i = 0; i < visibleMessage.length(); i++) {
            char c = visibleMessage.charAt(i);
            if (Character.isWhitespace(c) || !Character.isLetterOrDigit(c)) {
                wordBoundaries.add(i + 1);
            }
        }
        wordBoundaries.add(visibleMessage.length()); // End of message
        
        // If we have a short secret or few word boundaries, just append at the end
        if (wordBoundaries.size() <= 2 || encodedSecret.length() <= 10) {
            return visibleMessage + encodedSecret;
        }
        
        // Distribute the encoded secret across word boundaries
        StringBuilder result = new StringBuilder();
        int secretIndex = 0;
        int charsPerBoundary = Math.max(1, encodedSecret.length() / (wordBoundaries.size() - 1));
        
        for (int i = 0; i < wordBoundaries.size() - 1; i++) {
            int startPos = wordBoundaries.get(i);
            int endPos = wordBoundaries.get(i + 1);
            
            result.append(visibleMessage, startPos, endPos);
            
            // Insert part of the encoded secret
            if (secretIndex < encodedSecret.length()) {
                int endSecretIndex = Math.min(secretIndex + charsPerBoundary, encodedSecret.length());
                result.append(encodedSecret, secretIndex, endSecretIndex);
                secretIndex = endSecretIndex;
            }
        }
        
        // Append any remaining encoded secret
        if (secretIndex < encodedSecret.length()) {
            result.append(encodedSecret.substring(secretIndex));
        }
        
        return result.toString();
    }
}