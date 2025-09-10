package com.translator.messagingapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for creating and showing notifications.
 * Handles notification channels and different notification types.
 */
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    
    // Notification channel IDs
    private static final String CHANNEL_ID_MESSAGES = "messages_channel";
    private static final String CHANNEL_ID_TRANSLATIONS = "translations_channel";
    private static final String CHANNEL_ID_MMS = "mms_channel";
    
    // Notification IDs
    private static final int NOTIFICATION_ID_SMS = 1001;
    private static final int NOTIFICATION_ID_MMS = 1002;
    private static final int NOTIFICATION_ID_TRANSLATION = 1003;
    
    private final Context context;
    private final NotificationManager notificationManager;
    private final UserPreferences userPreferences;
    
    /**
     * Creates a new notification helper.
     *
     * @param context The context
     */
    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.userPreferences = new UserPreferences(context);
        createNotificationChannels();
    }
    
    /**
     * Creates notification channels for Android O and above.
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Messages channel
            NotificationChannel messagesChannel = new NotificationChannel(
                    CHANNEL_ID_MESSAGES,
                    "Messages",
                    NotificationManager.IMPORTANCE_HIGH);
            messagesChannel.setDescription("Notifications for new messages");
            messagesChannel.enableVibration(true);
            messagesChannel.enableLights(true);
            // Set sound to null to allow individual notifications to control their own sound
            messagesChannel.setSound(null, null);
            
            // Translations channel
            NotificationChannel translationsChannel = new NotificationChannel(
                    CHANNEL_ID_TRANSLATIONS,
                    "Translations",
                    NotificationManager.IMPORTANCE_DEFAULT);
            translationsChannel.setDescription("Notifications for translated messages");
            translationsChannel.enableVibration(true);
            
            // MMS channel
            NotificationChannel mmsChannel = new NotificationChannel(
                    CHANNEL_ID_MMS,
                    "Multimedia Messages",
                    NotificationManager.IMPORTANCE_HIGH);
            mmsChannel.setDescription("Notifications for multimedia messages");
            mmsChannel.enableVibration(true);
            mmsChannel.enableLights(true);
            // Set sound to null to allow individual notifications to control their own sound
            mmsChannel.setSound(null, null);
            
            // Create the channels
            notificationManager.createNotificationChannel(messagesChannel);
            notificationManager.createNotificationChannel(translationsChannel);
            notificationManager.createNotificationChannel(mmsChannel);
        }
    }
    
    /**
     * Gets the notification sound URI for a contact.
     * Returns custom tone if set, otherwise returns default tone.
     *
     * @param contactAddress The contact's phone number or address
     * @return The notification sound URI
     */
    private Uri getNotificationSoundForContact(String contactAddress) {
        // Check if contact has a custom notification tone
        String customToneUri = userPreferences.getContactNotificationTone(contactAddress);
        if (customToneUri != null && !customToneUri.isEmpty()) {
            try {
                return Uri.parse(customToneUri);
            } catch (Exception e) {
                Log.w(TAG, "Invalid custom tone URI for contact " + contactAddress + ": " + customToneUri, e);
            }
        }
        
        // Fall back to default notification sound
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }
    
    /**
     * Shows a notification for a received SMS message.
     *
     * @param senderAddress The sender's phone number/address
     * @param body The message body
     * @param threadId The conversation thread ID
     * @param senderDisplayName The sender's display name (optional, can be null)
     */
    public void showSmsReceivedNotification(String senderAddress, String body, String threadId, String senderDisplayName) {
        // Create intent for opening the conversation
        Intent intent = new Intent(context, ConversationActivity.class);
        intent.putExtra("thread_id", threadId);
        intent.putExtra("address", senderAddress); // Use the actual address for the intent
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // Create pending intent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
        
        // Get notification sound for this contact using the address
        Uri notificationSound = getNotificationSoundForContact(senderAddress);
        
        // Use display name for notification title, fall back to address if not provided
        String notificationTitle = !TextUtils.isEmpty(senderDisplayName) ? senderDisplayName : senderAddress;
        
        // Build notification with BigTextStyle to show full message content
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(notificationTitle)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(body)
                        .setBigContentTitle(notificationTitle))
                .setAutoCancel(true)
                .setSound(notificationSound)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);
        
        // Show notification
        notificationManager.notify(NOTIFICATION_ID_SMS, builder.build());
    }
    
    /**
     * Backward compatibility method for the old signature.
     * @deprecated Use showSmsReceivedNotification(String, String, String, String) instead
     */
    @Deprecated
    public void showSmsReceivedNotification(String sender, String body, String threadId) {
        // For backward compatibility, assume sender is the address if no display name
        showSmsReceivedNotification(sender, body, threadId, null);
    }
    
    /**
     * Shows a notification for a received MMS message.
     *
     * @param title The notification title
     * @param body The notification body
     */
    public void showMmsReceivedNotification(String title, String body) {
        showMmsReceivedNotification(title, body, null);
    }
    
    /**
     * Shows a notification for a received MMS message with optional contact-specific tone support.
     *
     * @param title The notification title
     * @param body The notification body
     * @param senderAddress The sender's phone number/address (null if not available)
     */
    public void showMmsReceivedNotification(String title, String body, String senderAddress) {
        // Create intent for opening the main activity
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // Create pending intent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
        
        // Get notification sound - use contact-specific if sender address is available
        Uri notificationSound;
        if (senderAddress != null && !senderAddress.isEmpty() && !"Unknown".equals(senderAddress)) {
            notificationSound = getNotificationSoundForContact(senderAddress);
        } else {
            notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_MMS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(notificationSound)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);
        
        // Show notification
        notificationManager.notify(NOTIFICATION_ID_MMS, builder.build());
    }
    
    /**
     * Shows a notification for a translated message.
     *
     * @param sender The sender of the message
     * @param originalText The original message text
     * @param translatedText The translated message text
     * @param originalLanguage The original language
     * @param translatedLanguage The translated language
     * @param threadId The conversation thread ID
     */
    public void showTranslationNotification(String sender, String originalText, String translatedText,
                                           String originalLanguage, String translatedLanguage, String threadId) {
        // Create intent for opening the conversation
        Intent intent = new Intent(context, ConversationActivity.class);
        intent.putExtra("thread_id", threadId);
        intent.putExtra("address", sender);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // Create pending intent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
        
        // Build notification content
        String title = "Message from " + sender;
        String content = translatedText + "\n\n[" + originalLanguage + " → " + translatedLanguage + "]\n" + originalText;
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_TRANSLATIONS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(translatedText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent);
        
        // Show notification
        notificationManager.notify(NOTIFICATION_ID_TRANSLATION, builder.build());
    }
    
    /**
     * Shows a message style notification for a conversation.
     *
     * @param sender The sender name or phone number
     * @param messages List of messages in the conversation
     * @param threadId The conversation thread ID
     * @param senderUri The sender's contact URI or null
     */
    public void showConversationNotification(String sender, List<Message> messages, String threadId, Uri senderUri) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        
        // Create intent for opening the conversation
        Intent intent = new Intent(context, ConversationActivity.class);
        intent.putExtra("thread_id", threadId);
        intent.putExtra("address", sender);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // Create pending intent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));
        
        // Create sender person
        Person.Builder senderPersonBuilder = new Person.Builder()
                .setName(sender);
        
        // Add icon if available
        if (senderUri != null) {
            try {
                Bitmap contactPhoto = BitmapFactory.decodeStream(
                        context.getContentResolver().openInputStream(senderUri));
                if (contactPhoto != null) {
                    senderPersonBuilder.setIcon(IconCompat.createWithBitmap(contactPhoto));
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        
        Person senderPerson = senderPersonBuilder.build();
        
        // Create self person
        Person selfPerson = new Person.Builder()
                .setName("Me")
                .build();
        
        // Create message style
        NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle(selfPerson);
        messagingStyle.setConversationTitle(sender);
        
        // Add messages
        for (Message message : messages) {
            String messageText = message.isTranslated() ? message.getTranslatedText() : message.getBody();
            
            if (message.isIncoming()) {
                messagingStyle.addMessage(messageText, message.getDate(), senderPerson);
            } else {
                messagingStyle.addMessage(messageText, message.getDate(), selfPerson);
            }
        }
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setStyle(messagingStyle)
                .setAutoCancel(true)
                .setSound(getNotificationSoundForContact(sender))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);
        
        // Show notification
        int notificationId = ("conversation_" + threadId).hashCode();
        notificationManager.notify(notificationId, builder.build());
    }
    
    /**
     * Cancels a notification.
     *
     * @param notificationId The notification ID
     */
    public void cancelNotification(int notificationId) {
        notificationManager.cancel(notificationId);
    }
    
    /**
     * Cancels all notifications.
     */
    public void cancelAllNotifications() {
        notificationManager.cancelAll();
    }
}