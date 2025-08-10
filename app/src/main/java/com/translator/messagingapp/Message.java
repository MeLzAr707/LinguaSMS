package com.translator.messagingapp;

import android.net.Uri;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents a message in a conversation.
 */
public class Message {
    private static final String TAG = "Message";
    public static final int MESSAGE_TYPE_SMS = 1;
    public static final int MESSAGE_TYPE_MMS = 2;
    public static final int MESSAGE_TYPE_RCS = 3;
    private long id;
    private String body;
    private long date;
    private int type;
    private boolean read;
    private String address;
    private long threadId;
    private String contactName;

    // Added field for message type (SMS or MMS)
    private int messageType;

    // Translation fields
    private String translatedText;
    private String originalLanguage;
    private String translatedLanguage;
    private boolean showTranslation;
    
    // Search-related fields
    private String searchQuery;
    
    // Reaction-related fields
    private MessageReaction.ReactionManager reactionManager;

    // Constants for message types
    public static final int TYPE_INBOX = 1;
    public static final int TYPE_SENT = 2;
    public static final int TYPE_DRAFT = 3;
    public static final int TYPE_OUTBOX = 4;
    public static final int TYPE_FAILED = 5;
    public static final int TYPE_QUEUED = 6;
    public static final int TYPE_ALL = 0; // Show all message types
    public static final int TYPE_SMS = 7; // SMS message type
    // Constants for message types
    public static final int TYPE_MMS = 128; // Or any value that doesn't conflict with existing types

    /**
     * Default constructor.
     */
    public Message() {
    }

    /**
     * Creates a new message with the specified parameters.
     *
     * @param id The message ID
     * @param body The message body
     * @param date The message date
     * @param type The message type
     * @param read Whether the message has been read
     * @param address The sender/recipient address
     * @param threadId The conversation thread ID
     */
    public Message(long id, String body, long date, int type, boolean read, String address, long threadId) {
        this.id = id;
        this.body = body;
        this.date = date;
        this.type = type;
        this.read = read;
        this.address = address;
        this.threadId = threadId;
    }

    /**
     * Creates a new message with the specified parameters.
     *
     * @param id The message ID as a String
     * @param body The message body
     * @param date The message date
     * @param type The message type
     */
    public Message(String id, String body, long date, int type) {
        this(id != null ? Long.parseLong(id) : -1, body, date, type, false, null, -1);
    }

    // Getters and setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * Sets the message ID from a String.
     *
     * @param id The message ID as a String
     */
    public void setId(String id) {
        try {
            this.id = id != null ? Long.parseLong(id) : -1;
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid ID format: " + id, e);
            this.id = -1;
        }
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    /**
     * Sets the thread ID from a String.
     *
     * @param threadId The thread ID as a String
     */
    public void setThreadId(String threadId) {
        try {
            this.threadId = threadId != null ? Long.parseLong(threadId) : -1;
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid thread ID format: " + threadId, e);
            this.threadId = -1;
        }
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public void setTranslatedText(String translatedText) {
        this.translatedText = translatedText;
    }

    public String getOriginalLanguage() {
        return originalLanguage;
    }

    public void setOriginalLanguage(String originalLanguage) {
        this.originalLanguage = originalLanguage;
    }

    public String getTranslatedLanguage() {
        return translatedLanguage;
    }

    public void setTranslatedLanguage(String translatedLanguage) {
        this.translatedLanguage = translatedLanguage;
    }

    public boolean isShowTranslation() {
        return showTranslation;
    }

    public void setShowTranslation(boolean showTranslation) {
        this.showTranslation = showTranslation;
    }
    
    /**
     * Gets the search query used to find this message.
     * 
     * @return The search query
     */
    public String getSearchQuery() {
        return searchQuery;
    }
    
    /**
     * Sets the search query used to find this message.
     * 
     * @param searchQuery The search query
     */
    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }
    
    /**
     * Checks if this message has a search query.
     * 
     * @return true if the message has a search query, false otherwise
     */
    public boolean hasSearchQuery() {
        return searchQuery != null && !searchQuery.isEmpty();
    }
    
    /**
     * Gets the reaction manager for this message.
     * 
     * @return The reaction manager
     */
    public MessageReaction.ReactionManager getReactionManager() {
        if (reactionManager == null) {
            reactionManager = new MessageReaction.ReactionManager();
        }
        return reactionManager;
    }
    
    /**
     * Adds a reaction to this message.
     * 
     * @param emoji The emoji to add
     * @param userId The user ID who reacted
     * @return True if the reaction was added, false if it already exists
     */
    public boolean addReaction(String emoji, String userId) {
        MessageReaction reaction = new MessageReaction(emoji, userId, System.currentTimeMillis());
        return getReactionManager().addReaction(reaction);
    }
    
    /**
     * Removes a reaction from this message.
     * 
     * @param emoji The emoji to remove
     * @param userId The user ID who reacted
     * @return True if the reaction was removed, false if it doesn't exist
     */
    public boolean removeReaction(String emoji, String userId) {
        return getReactionManager().removeReaction(userId, emoji);
    }
    
    /**
     * Checks if this message has any reactions.
     * 
     * @return true if the message has reactions, false otherwise
     */
    public boolean hasReactions() {
        return reactionManager != null && reactionManager.getTotalReactionCount() > 0;
    }

    /**
     * Gets the message type (SMS or MMS).
     *
     * @return The message type
     */
    public int getMessageType() {
        return messageType;
    }

    /**
     * Sets the message type (SMS or MMS).
     *
     * @param messageType The message type
     */
    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    /**
     * Sets the attachments for this message.
     *
     * @param attachments The list of attachment URIs
     */
    public void setAttachments(List<Uri> attachments) {
        // Default implementation does nothing
        // Override in subclasses like MmsMessage
    }

    /**
     * Gets the attachments for this message.
     *
     * @return The list of attachment URIs
     */
    public List<Uri> getAttachments() {
        return null; // Default implementation, override in subclasses
    }

    /**
     * Checks if this message has attachments.
     *
     * @return true if the message has attachments, false otherwise
     */
    public boolean hasAttachments() {
        return false; // Default implementation, override in subclasses
    }

    /**
     * Checks if the message has been translated.
     *
     * @return true if the message has been translated, false otherwise
     */
    public boolean isTranslated() {
        return translatedText != null && !translatedText.isEmpty();
    }

    /**
     * Checks if the message is incoming.
     *
     * @return true if the message is incoming, false otherwise
     */
    public boolean isIncoming() {
        return type == TYPE_INBOX;
    }

    /**
     * Gets a formatted date string.
     *
     * @return A formatted date string
     */
    public String getFormattedDate() {
        return new Date(date).toString();
    }

    /**
     * Saves the translation state to the cache.
     *
     * @param cache The translation cache
     */
    public void saveTranslationState(TranslationCache cache) {
        if (translatedText != null && !translatedText.isEmpty()) {
            // Create a unique key for this message
            String cacheKey = generateCacheKey();

            // Store translation data as JSON
            JSONObject data = new JSONObject();
            try {
                data.put("translatedText", translatedText);
                data.put("originalLanguage", originalLanguage != null ? originalLanguage : "");
                data.put("translatedLanguage", translatedLanguage != null ? translatedLanguage : "");
                data.put("showTranslation", showTranslation);

                cache.put(cacheKey, data.toString());
            } catch (JSONException e) {
                Log.e(TAG, "Error saving translation state", e);
            }
        }
    }

    /**
     * Restores the translation state from the cache.
     *
     * @param cache The translation cache
     * @return true if translation state was restored, false otherwise
     */
    public boolean restoreTranslationState(TranslationCache cache) {
        String cacheKey = generateCacheKey();
        String savedData = cache.get(cacheKey);

        if (savedData != null) {
            try {
                JSONObject data = new JSONObject(savedData);
                translatedText = data.optString("translatedText");
                originalLanguage = data.optString("originalLanguage");
                translatedLanguage = data.optString("translatedLanguage");
                showTranslation = data.optBoolean("showTranslation");
                return true;
            } catch (JSONException e) {
                Log.e(TAG, "Error restoring translation state", e);
            }
        }
        return false;
    }

    /**
     * Clears the translation state from the cache.
     *
     * @param cache The translation cache
     */
    public void clearTranslationState(TranslationCache cache) {
        String cacheKey = generateCacheKey();
        cache.delete(cacheKey);

        // Also clear local translation data
        translatedText = null;
        originalLanguage = null;
        translatedLanguage = null;
        showTranslation = false;
    }

    /**
     * Generates a unique cache key for this message.
     *
     * @return A unique cache key
     */
    private String generateCacheKey() {
        return "msg_" + id + "_translation_state";
    }

    /**
     * Checks if the message is an MMS message.
     *
     * @return true if the message is MMS, false otherwise
     */
    public boolean isMms() {
        // Use our own TYPE_MMS constant instead of Telephony.Sms.MESSAGE_TYPE_MMS
        return type == TYPE_MMS || messageType == MESSAGE_TYPE_MMS ||
                (getClass() == MmsMessage.class);
    }
    
    /**
     * Checks if the message is an RCS message.
     *
     * @return true if the message is RCS, false otherwise
     */
    public boolean isRcs() {
        return messageType == MESSAGE_TYPE_RCS;
    }

    /**
     * Sets whether the message has been translated.
     *
     * @param translated true if the message has been translated, false otherwise
     */
    public void setTranslated(boolean translated) {
        // This method updates the internal state to reflect translation status
        if (translated) {
            // If marking as translated but no translated text exists, 
            // this indicates the original text should be shown as translated
            if (translatedText == null || translatedText.isEmpty()) {
                translatedText = body; // Use original body as translated text
            }
        } else {
            // Clear translation when marking as not translated
            translatedText = null;
            showTranslation = false;
        }
    }

    /**
     * Checks if the message has been delivered.
     *
     * @return true if the message has been delivered, false otherwise
     */
    public boolean isDelivered() {
        // A message is considered delivered if it's sent, queued, or in outbox
        return type == TYPE_SENT || type == TYPE_QUEUED || type == TYPE_OUTBOX;
    }

    /**
     * Checks if the message is translatable.
     *
     * @return true if the message can be translated, false otherwise
     */
    public boolean isTranslatable() {
        // A message is translatable if it has text content and is not empty
        return body != null && !body.trim().isEmpty() && !body.matches("^[\\s\\p{Punct}]*$");
    }

    /**
     * Gets the reactions for this message.
     *
     * @return A list of message reactions
     */
    public List<MessageReaction> getReactions() {
        if (reactionManager != null) {
            return reactionManager.getAllReactions();
        }
        return new ArrayList<>(); // Return empty list if no reactions
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", body='" + body + '\'' +
                ", date=" + date +
                ", type=" + type +
                ", read=" + read +
                ", address='" + address + '\'' +
                ", threadId=" + threadId +
                '}';
    }
}




