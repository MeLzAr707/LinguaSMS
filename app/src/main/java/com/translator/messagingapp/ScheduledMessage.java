package com.translator.messagingapp;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a message that is scheduled to be sent at a specified time.
 */
public class ScheduledMessage implements Parcelable {
    private long id;
    private String recipient;
    private String messageBody;
    private long scheduledTime;
    private long createdTime;
    private boolean isDelivered;
    private String threadId;

    public ScheduledMessage() {
        this.createdTime = System.currentTimeMillis();
        this.isDelivered = false;
    }

    public ScheduledMessage(String recipient, String messageBody, long scheduledTime, String threadId) {
        this();
        this.recipient = recipient;
        this.messageBody = messageBody;
        this.scheduledTime = scheduledTime;
        this.threadId = threadId;
    }

    // Parcelable constructor
    protected ScheduledMessage(Parcel in) {
        id = in.readLong();
        recipient = in.readString();
        messageBody = in.readString();
        scheduledTime = in.readLong();
        createdTime = in.readLong();
        isDelivered = in.readByte() != 0;
        threadId = in.readString();
    }

    public static final Creator<ScheduledMessage> CREATOR = new Creator<ScheduledMessage>() {
        @Override
        public ScheduledMessage createFromParcel(Parcel in) {
            return new ScheduledMessage(in);
        }

        @Override
        public ScheduledMessage[] newArray(int size) {
            return new ScheduledMessage[size];
        }
    };

    // Getters and setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public long getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(long scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public boolean isDelivered() {
        return isDelivered;
    }

    public void setDelivered(boolean delivered) {
        isDelivered = delivered;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    /**
     * Check if this message is ready to be sent (scheduled time has passed).
     */
    public boolean isReadyToSend() {
        return !isDelivered && System.currentTimeMillis() >= scheduledTime;
    }

    /**
     * Check if this message is scheduled for the future.
     */
    public boolean isScheduledForFuture() {
        return !isDelivered && System.currentTimeMillis() < scheduledTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(recipient);
        dest.writeString(messageBody);
        dest.writeLong(scheduledTime);
        dest.writeLong(createdTime);
        dest.writeByte((byte) (isDelivered ? 1 : 0));
        dest.writeString(threadId);
    }

    @Override
    public String toString() {
        return "ScheduledMessage{" +
                "id=" + id +
                ", recipient='" + recipient + '\'' +
                ", messageBody='" + messageBody + '\'' +
                ", scheduledTime=" + scheduledTime +
                ", createdTime=" + createdTime +
                ", isDelivered=" + isDelivered +
                ", threadId='" + threadId + '\'' +
                '}';
    }
}