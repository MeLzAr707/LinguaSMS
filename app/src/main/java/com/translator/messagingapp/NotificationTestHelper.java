package com.translator.messagingapp;

import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;

/**
 * Manual test class to demonstrate how notifications work.
 * This class can be used to manually trigger notifications for testing.
 */
public class NotificationTestHelper {
    
    private final Context context;
    private final MessageService messageService;
    
    public NotificationTestHelper(Context context, MessageService messageService) {
        this.context = context;
        this.messageService = messageService;
    }
    
    /**
     * Simulates receiving an SMS and triggering a notification.
     * This can be called from a test button in the app.
     */
    public void testSmsNotification() {
        try {
            // Create a test SMS intent
            Intent intent = new Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
            
            // In a real scenario, this would contain SMS PDU data
            // For testing, we'll just call the handler directly
            
            // Alternatively, use the NotificationHelper directly for testing
            NotificationHelper notificationHelper = new NotificationHelper(context);
            notificationHelper.showSmsReceivedNotification(
                "+15551234567", 
                "This is a test SMS notification", 
                "1"
            );
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Simulates receiving an MMS and triggering a notification.
     */
    public void testMmsNotification() {
        try {
            // Create a test MMS intent
            Intent intent = new Intent(Telephony.Mms.Intents.CONTENT_CHANGED_ACTION);
            
            // Use the NotificationHelper directly for testing
            NotificationHelper notificationHelper = new NotificationHelper(context);
            notificationHelper.showMmsReceivedNotification(
                "New MMS", 
                "This is a test MMS notification"
            );
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Tests the conversation notification style.
     */
    public void testConversationNotification() {
        try {
            NotificationHelper notificationHelper = new NotificationHelper(context);
            
            // Create a sample list of messages for testing
            java.util.List<Message> messages = new java.util.ArrayList<>();
            
            // Create a sample message
            SmsMessage message = new SmsMessage("+15551234567", "This is a test conversation notification");
            // Convert SmsMessage to Message for the notification
            Message notificationMessage = new Message();
            notificationMessage.setBody(message.getOriginalText());
            notificationMessage.setDate(System.currentTimeMillis());
            notificationMessage.setType(Message.TYPE_INBOX);
            notificationMessage.setAddress(message.getAddress());
            
            messages.add(notificationMessage);
            
            notificationHelper.showConversationNotification(
                "+15551234567",
                messages,
                "1",
                null
            );
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}