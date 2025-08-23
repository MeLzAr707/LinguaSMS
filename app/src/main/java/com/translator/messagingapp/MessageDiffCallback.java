package com.translator.messagingapp;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

/**
 * DiffUtil callback for efficient RecyclerView updates in MessageRecyclerAdapter.
 * Calculates the difference between old and new message lists to provide
 * minimal UI updates instead of full dataset changes.
 */
public class MessageDiffCallback extends DiffUtil.Callback {
    private final List<Message> oldMessages;
    private final List<Message> newMessages;
    
    public MessageDiffCallback(List<Message> oldMessages, List<Message> newMessages) {
        this.oldMessages = oldMessages;
        this.newMessages = newMessages;
    }
    
    @Override
    public int getOldListSize() {
        return oldMessages.size();
    }
    
    @Override
    public int getNewListSize() {
        return newMessages.size();
    }
    
    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        Message oldMessage = oldMessages.get(oldItemPosition);
        Message newMessage = newMessages.get(newItemPosition);
        return oldMessage.getId() == newMessage.getId();
    }
    
    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Message oldMessage = oldMessages.get(oldItemPosition);
        Message newMessage = newMessages.get(newItemPosition);
        
        // Compare all relevant fields for content equality
        return areMessagesContentEqual(oldMessage, newMessage);
    }
    
    /**
     * Helper method to compare message content for equality.
     * This includes all fields that could affect the UI display.
     */
    private boolean areMessagesContentEqual(Message oldMessage, Message newMessage) {
        // Basic message fields
        boolean basicFieldsEqual = oldMessage.getId() == newMessage.getId() &&
                oldMessage.getDate() == newMessage.getDate() &&
                oldMessage.getType() == newMessage.getType() &&
                oldMessage.isRead() == newMessage.isRead() &&
                oldMessage.getThreadId() == newMessage.getThreadId();
        
        // String fields (handling nulls)
        boolean stringFieldsEqual = isStringEqual(oldMessage.getBody(), newMessage.getBody()) &&
                isStringEqual(oldMessage.getAddress(), newMessage.getAddress()) &&
                isStringEqual(oldMessage.getContactName(), newMessage.getContactName());
        
        // Translation fields
        boolean translationFieldsEqual = oldMessage.isTranslated() == newMessage.isTranslated() &&
                isStringEqual(oldMessage.getTranslatedText(), newMessage.getTranslatedText()) &&
                isStringEqual(oldMessage.getOriginalLanguage(), newMessage.getOriginalLanguage()) &&
                isStringEqual(oldMessage.getTranslatedLanguage(), newMessage.getTranslatedLanguage()) &&
                oldMessage.isShowTranslation() == newMessage.isShowTranslation();
        
        // Search-related fields
        boolean searchFieldsEqual = isStringEqual(oldMessage.getSearchQuery(), newMessage.getSearchQuery());
        
        // Reaction fields - check if both have same reaction state
        boolean reactionFieldsEqual = oldMessage.hasReactions() == newMessage.hasReactions();
        
        return basicFieldsEqual && stringFieldsEqual && translationFieldsEqual && 
               searchFieldsEqual && reactionFieldsEqual;
    }
    
    /**
     * Helper method to safely compare strings, handling null values.
     */
    private boolean isStringEqual(String str1, String str2) {
        if (str1 == null && str2 == null) {
            return true;
        }
        if (str1 == null || str2 == null) {
            return false;
        }
        return str1.equals(str2);
    }
}