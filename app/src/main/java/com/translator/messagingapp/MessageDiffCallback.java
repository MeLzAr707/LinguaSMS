package com.translator.messagingapp;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

/**
 * DiffUtil callback for comparing lists of messages.
 * This improves RecyclerView performance by calculating the minimum number of changes
 * needed to update the UI when the data changes.
 */
public class MessageDiffCallback extends DiffUtil.Callback {
    private final List<Message> oldMessages;
    private final List<Message> newMessages;
    
    /**
     * Creates a new MessageDiffCallback.
     *
     * @param oldMessages The old list of messages
     * @param newMessages The new list of messages
     */
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
    
    /**
     * Determines if two items represent the same message.
     * This is based on the message ID.
     */
    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        Message oldMessage = oldMessages.get(oldItemPosition);
        Message newMessage = newMessages.get(newItemPosition);
        
        // If either message is null, they can't be the same
        if (oldMessage == null || newMessage == null) {
            return false;
        }
        
        // Compare by ID
        String oldId = oldMessage.getId();
        String newId = newMessage.getId();
        
        // If either ID is null, compare by reference
        if (oldId == null || newId == null) {
            return oldMessage == newMessage;
        }
        
        return oldId.equals(newId);
    }
    
    /**
     * Determines if two items have the same content.
     * This checks all relevant fields that affect how the message is displayed.
     */
    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Message oldMessage = oldMessages.get(oldItemPosition);
        Message newMessage = newMessages.get(newItemPosition);
        
        // If either message is null, they can't have the same content
        if (oldMessage == null || newMessage == null) {
            return false;
        }
        
        // Check all fields that affect how the message is displayed
        boolean sameBody = equals(oldMessage.getBody(), newMessage.getBody());
        boolean sameDate = oldMessage.getDate() == newMessage.getDate();
        boolean sameType = oldMessage.getType() == newMessage.getType();
        boolean sameRead = oldMessage.isRead() == newMessage.isRead();
        boolean sameTranslated = oldMessage.isTranslated() == newMessage.isTranslated();
        boolean sameTranslatedText = equals(oldMessage.getTranslatedText(), newMessage.getTranslatedText());
        
        // For MMS messages, also check attachments
        boolean sameAttachments = true;
        if (oldMessage instanceof MmsMessage && newMessage instanceof MmsMessage) {
            MmsMessage oldMms = (MmsMessage) oldMessage;
            MmsMessage newMms = (MmsMessage) newMessage;
            
            List<MmsMessage.Attachment> oldAttachments = oldMms.getAttachmentObjects();
            List<MmsMessage.Attachment> newAttachments = newMms.getAttachmentObjects();
            
            // Check if both are null or non-null
            if ((oldAttachments == null) != (newAttachments == null)) {
                sameAttachments = false;
            } 
            // If both are non-null, check size
            else if (oldAttachments != null && newAttachments != null) {
                if (oldAttachments.size() != newAttachments.size()) {
                    sameAttachments = false;
                }
                // For simplicity, we'll just check the first attachment
                else if (!oldAttachments.isEmpty()) {
                    MmsMessage.Attachment oldAttachment = oldAttachments.get(0);
                    MmsMessage.Attachment newAttachment = newAttachments.get(0);
                    sameAttachments = equals(oldAttachment.getContentType(), newAttachment.getContentType()) &&
                                     equals(oldAttachment.getPartId(), newAttachment.getPartId());
                }
            }
        }
        
        return sameBody && sameDate && sameType && sameRead && 
               sameTranslated && sameTranslatedText && sameAttachments;
    }
    
    /**
     * Helper method to compare two objects for equality, handling null values.
     */
    private boolean equals(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}