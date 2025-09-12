package com.translator.messagingapp.mms;

import com.translator.messagingapp.message.*;

import android.net.Uri;

/**
 * Represents the state of an MMS transaction.
 */
public class TransactionState {
    
    // Transaction states
    public static final int INITIALIZED = 0;
    public static final int PROCESSING = 1;
    public static final int SUCCESS = 2;
    public static final int FAILED = 3;
    public static final int CANCELLED = 4;

    private int mState = INITIALIZED;
    private String mErrorMessage;
    private Uri mContentUri;
    private long mTimestamp;

    /**
     * Creates a new transaction state.
     */
    public TransactionState() {
        mTimestamp = System.currentTimeMillis();
    }

    /**
     * Gets the current state.
     *
     * @return The current state
     */
    public int getState() {
        return mState;
    }

    /**
     * Sets the state.
     *
     * @param state The new state
     */
    public void setState(int state) {
        mState = state;
        mTimestamp = System.currentTimeMillis();
    }

    /**
     * Gets the error message.
     *
     * @return The error message, or null if no error
     */
    public String getErrorMessage() {
        return mErrorMessage;
    }

    /**
     * Sets the error message.
     *
     * @param errorMessage The error message
     */
    public void setErrorMessage(String errorMessage) {
        mErrorMessage = errorMessage;
    }

    /**
     * Gets the content URI.
     *
     * @return The content URI
     */
    public Uri getContentUri() {
        return mContentUri;
    }

    /**
     * Sets the content URI.
     *
     * @param contentUri The content URI
     */
    public void setContentUri(Uri contentUri) {
        mContentUri = contentUri;
    }

    /**
     * Gets the timestamp of the last state change.
     *
     * @return The timestamp
     */
    public long getTimestamp() {
        return mTimestamp;
    }

    /**
     * Checks if the transaction is in progress.
     *
     * @return True if the transaction is processing
     */
    public boolean isProcessing() {
        return mState == PROCESSING;
    }

    /**
     * Checks if the transaction completed successfully.
     *
     * @return True if the transaction succeeded
     */
    public boolean isSuccess() {
        return mState == SUCCESS;
    }

    /**
     * Checks if the transaction failed.
     *
     * @return True if the transaction failed
     */
    public boolean isFailed() {
        return mState == FAILED;
    }

    /**
     * Checks if the transaction was cancelled.
     *
     * @return True if the transaction was cancelled
     */
    public boolean isCancelled() {
        return mState == CANCELLED;
    }

    /**
     * Gets a string representation of the state.
     *
     * @return The state as a string
     */
    public String getStateString() {
        switch (mState) {
            case INITIALIZED:
                return "INITIALIZED";
            case PROCESSING:
                return "PROCESSING";
            case SUCCESS:
                return "SUCCESS";
            case FAILED:
                return "FAILED";
            case CANCELLED:
                return "CANCELLED";
            default:
                return "UNKNOWN";
        }
    }

    @Override
    public String toString() {
        return "TransactionState{" +
                "state=" + getStateString() +
                ", errorMessage='" + mErrorMessage + '\'' +
                ", contentUri=" + mContentUri +
                ", timestamp=" + mTimestamp +
                '}';
    }
}