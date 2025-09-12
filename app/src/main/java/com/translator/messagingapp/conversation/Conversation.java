package com.translator.messagingapp.conversation;

import com.translator.messagingapp.message.*;

import com.translator.messagingapp.conversation.*;

import android.text.TextUtils;
import java.util.Date;

public class Conversation {
    private String threadId;
    private String address;
    private String contactName;
    private String lastMessage;
    private long date;
    private int type;
    private int unreadCount;
    private int messageCount;
    private String snippet;
    private boolean read;

    // Default constructor
    public Conversation() {
    }

    /**
     * Constructor with all fields
     */
    public Conversation(String threadId, String address, String contactName, String lastMessage, long date, int type, int unreadCount) {
        this.threadId = threadId;
        this.address = address;
        this.contactName = contactName;
        this.lastMessage = lastMessage;
        this.date = date;
        this.type = type;
        this.unreadCount = unreadCount;
        this.messageCount = 0;
        this.read = (unreadCount == 0);
        // Initialize snippet with lastMessage for backward compatibility
        this.snippet = lastMessage;
    }

    /**
     * Constructor with essential fields only
     */
    public Conversation(String threadId, String address, String contactName) {
        this.threadId = threadId;
        this.address = address;
        this.contactName = contactName;
        this.lastMessage = "";
        this.date = 0;
        this.type = 0;
        this.unreadCount = 0;
        this.messageCount = 0;
        this.read = true;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
        // Update snippet when lastMessage is set for backward compatibility
        // Only if snippet is currently empty
        if (!TextUtils.isEmpty(lastMessage) && TextUtils.isEmpty(this.snippet)) {
            this.snippet = lastMessage;
        }
    }

    public Date getDate() {
        return new Date(date);
    }

    public void setDate(Date date) {
        this.date = date.getTime();
    }

    /**
     * Sets the date using a timestamp
     */
    public void setDate(long date) {
        this.date = date;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
        this.read = (unreadCount == 0);
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    /**
     * Gets the snippet (preview) of the conversation
     */
    public String getSnippet() {
        // First check if we have a dedicated snippet
        if (!TextUtils.isEmpty(snippet)) {
            return snippet;
        } 
        // Then check if we have a lastMessage
        else if (!TextUtils.isEmpty(lastMessage)) {
            return lastMessage;
        } 
        // Default message if no content is available
        else {
            return "No messages";
        }
    }

    /**
     * Sets the snippet (preview) of the conversation
     */
    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    /**
     * Checks if the conversation has been read
     */
    public boolean isRead() {
        return read;
    }

    /**
     * Sets whether the conversation has been read
     */
    public void setRead(boolean read) {
        this.read = read;
        if (read) {
            this.unreadCount = 0;
        }
    }

    @Override
    public String toString() {
        return "Conversation{" +
                "threadId='" + threadId + '\'' +
                ", address='" + address + '\'' +
                ", contactName='" + contactName + '\'' +
                ", lastMessage='" + lastMessage + '\'' +
                ", date=" + date +
                ", type=" + type +
                ", unreadCount=" + unreadCount +
                ", read=" + read +
                '}';
    }
}
