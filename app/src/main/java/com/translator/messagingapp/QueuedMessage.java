package com.translator.messagingapp;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a message in the offline message queue.
 * This is used for handling messages when network connectivity is limited.
 */
public class QueuedMessage {
    private long id;
    private String address;
    private String body;
    private long timestamp;
    private int retryCount;
    private boolean failed;
    
    public QueuedMessage(long id, String address, String body, long timestamp) {
        this.id = id;
        this.address = address;
        this.body = body;
        this.timestamp = timestamp;
        this.retryCount = 0;
        this.failed = false;
    }
    
    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    
    public boolean isFailed() { return failed; }
    public void setFailed(boolean failed) { this.failed = failed; }
}