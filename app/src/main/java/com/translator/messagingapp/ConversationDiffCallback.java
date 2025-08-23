package com.translator.messagingapp;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

/**
 * DiffUtil callback for efficient RecyclerView updates in ConversationRecyclerAdapter.
 * Calculates the difference between old and new conversation lists to provide
 * minimal UI updates instead of full dataset changes.
 */
public class ConversationDiffCallback extends DiffUtil.Callback {
    private final List<Conversation> oldConversations;
    private final List<Conversation> newConversations;
    
    public ConversationDiffCallback(List<Conversation> oldConversations, List<Conversation> newConversations) {
        this.oldConversations = oldConversations;
        this.newConversations = newConversations;
    }
    
    @Override
    public int getOldListSize() {
        return oldConversations.size();
    }
    
    @Override
    public int getNewListSize() {
        return newConversations.size();
    }
    
    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        Conversation oldConversation = oldConversations.get(oldItemPosition);
        Conversation newConversation = newConversations.get(newItemPosition);
        // Use thread ID as the unique identifier for conversations
        return oldConversation.getThreadId().equals(newConversation.getThreadId());
    }
    
    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Conversation oldConversation = oldConversations.get(oldItemPosition);
        Conversation newConversation = newConversations.get(newItemPosition);
        
        // Compare all relevant fields for content equality
        return areConversationsContentEqual(oldConversation, newConversation);
    }
    
    /**
     * Helper method to compare conversation content for equality.
     * This includes all fields that could affect the UI display.
     */
    private boolean areConversationsContentEqual(Conversation oldConversation, Conversation newConversation) {
        // Basic conversation fields
        boolean basicFieldsEqual = oldConversation.getThreadId().equals(newConversation.getThreadId()) &&
                oldConversation.getMessageCount() == newConversation.getMessageCount() &&
                oldConversation.getDate() == newConversation.getDate() &&
                oldConversation.isRead() == newConversation.isRead();
        
        // String fields (handling nulls)
        boolean stringFieldsEqual = isStringEqual(oldConversation.getSnippet(), newConversation.getSnippet()) &&
                isStringEqual(oldConversation.getContactName(), newConversation.getContactName()) &&
                isStringEqual(oldConversation.getAddress(), newConversation.getAddress());
        
        return basicFieldsEqual && stringFieldsEqual;
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