package com.translator.messagingapp;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstration script showing how the contact avatar feature would work.
 * This simulates the behavior without requiring a full Android environment.
 */
public class ContactAvatarDemo {
    
    public static void main(String[] args) {
        System.out.println("=== LinguaSMS Contact Avatar Feature Demo ===\n");
        
        // Create sample conversations
        List<Conversation> sampleConversations = createSampleConversations();
        
        System.out.println("Sample conversations and their avatar display logic:\n");
        
        for (int i = 0; i < sampleConversations.size(); i++) {
            Conversation conv = sampleConversations.get(i);
            demonstrateContactAvatar(i + 1, conv);
            System.out.println();
        }
        
        System.out.println("=== Feature Summary ===");
        System.out.println("✓ Contact photos will be loaded from device contacts when available");
        System.out.println("✓ Fallback colored circles with initials for contacts without photos");
        System.out.println("✓ Consistent color generation based on contact name/number");
        System.out.println("✓ Proper error handling with default avatars");
        System.out.println("✓ Integration with existing Glide image loading");
        System.out.println("✓ Null safety checks throughout");
    }
    
    private static List<Conversation> createSampleConversations() {
        List<Conversation> conversations = new ArrayList<>();
        
        // Conversation with contact name
        Conversation conv1 = new Conversation("1", "+1234567890", "John Doe");
        conv1.setSnippet("Hey, how are you doing?");
        conv1.setDate(System.currentTimeMillis());
        conversations.add(conv1);
        
        // Conversation with only phone number
        Conversation conv2 = new Conversation("2", "+9876543210", null);
        conv2.setSnippet("Can we meet tomorrow?");
        conv2.setDate(System.currentTimeMillis() - 3600000);
        conversations.add(conv2);
        
        // Conversation with empty contact name
        Conversation conv3 = new Conversation("3", "+5555551234", "");
        conv3.setSnippet("Thanks for the help!");
        conv3.setDate(System.currentTimeMillis() - 86400000);
        conversations.add(conv3);
        
        // Conversation with special characters in name
        Conversation conv4 = new Conversation("4", "+1111222333", "Alice Smith-Johnson");
        conv4.setSnippet("See you at the meeting");
        conv4.setDate(System.currentTimeMillis() - 172800000);
        conversations.add(conv4);
        
        // Conversation with number-like name
        Conversation conv5 = new Conversation("5", "+4444555666", "123 Pizza Delivery");
        conv5.setSnippet("Your order is ready");
        conv5.setDate(System.currentTimeMillis() - 259200000);
        conversations.add(conv5);
        
        return conversations;
    }
    
    private static void demonstrateContactAvatar(int index, Conversation conversation) {
        System.out.println("Conversation " + index + ":");
        System.out.println("  Thread ID: " + conversation.getThreadId());
        System.out.println("  Phone: " + conversation.getAddress());
        System.out.println("  Contact Name: " + (conversation.getContactName() != null ? 
            "\"" + conversation.getContactName() + "\"" : "null"));
        System.out.println("  Snippet: " + conversation.getSnippet());
        
        // Simulate display name logic from adapter
        String displayName = conversation.getContactName();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = conversation.getAddress();
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = "Unknown Contact";
        }
        
        System.out.println("  Display Name: \"" + displayName + "\"");
        
        // Simulate avatar generation logic
        String initial = ContactUtils.getContactInitial(displayName);
        int backgroundColor = ContactUtils.getContactColor(displayName);
        
        System.out.println("  Avatar Initial: \"" + initial + "\"");
        System.out.println("  Avatar Color: " + String.format("#%08X", backgroundColor));
        
        // Simulate contact photo lookup
        boolean hasContactPhoto = simulateContactPhotoLookup(conversation.getAddress());
        
        if (hasContactPhoto) {
            System.out.println("  Avatar Type: Contact Photo (with fallback to initials)");
        } else {
            System.out.println("  Avatar Type: Generated initials circle");
        }
        
        System.out.println("  Expected Result: " + (hasContactPhoto ? 
            "Circular photo with " + initial + " fallback" : 
            "Colored circle (" + initial + ") with color " + String.format("#%08X", backgroundColor)));
    }
    
    private static boolean simulateContactPhotoLookup(String phoneNumber) {
        // Simulate that some numbers have contact photos
        if (phoneNumber == null) return false;
        
        // For demo purposes, assume numbers ending in 0 have photos
        return phoneNumber.endsWith("0");
    }
}