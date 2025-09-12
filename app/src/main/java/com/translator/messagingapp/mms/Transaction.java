package com.translator.messagingapp.mms;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

/**
 * Base class for all MMS transactions.
 * Manages the lifecycle of MMS operations with retry support.
 */
public abstract class Transaction {
    private static final String TAG = "Transaction";

    // Transaction types
    public static final int SEND_TRANSACTION = 1;
    public static final int NOTIFICATION_TRANSACTION = 2;
    public static final int RETRIEVE_TRANSACTION = 3;

    protected final Context mContext;
    protected final Uri mUri;
    protected final TransactionState mTransactionState;
    protected Thread mThread;
    protected int mRetryCount = 0;
    protected static final int MAX_RETRY_COUNT = 3;

    /**
     * Creates a new transaction.
     *
     * @param context The application context
     * @param uri The URI of the message being processed
     */
    public Transaction(Context context, Uri uri) {
        mContext = context;
        mUri = uri;
        mTransactionState = new TransactionState();
    }

    /**
     * Processes the transaction.
     * This method should be called to start the transaction.
     */
    public abstract void process();

    /**
     * Gets the transaction type.
     *
     * @return The transaction type constant
     */
    public abstract int getTransactionType();

    /**
     * Gets the transaction state.
     *
     * @return The current transaction state
     */
    public TransactionState getTransactionState() {
        return mTransactionState;
    }

    /**
     * Gets the message URI.
     *
     * @return The message URI
     */
    public Uri getUri() {
        return mUri;
    }

    /**
     * Gets the current retry count.
     *
     * @return The retry count
     */
    public int getRetryCount() {
        return mRetryCount;
    }

    /**
     * Sets the retry count.
     *
     * @param retryCount The retry count
     */
    public void setRetryCount(int retryCount) {
        this.mRetryCount = retryCount;
    }

    /**
     * Checks if the transaction can be retried.
     *
     * @return True if the transaction can be retried
     */
    public boolean canRetry() {
        return mRetryCount < MAX_RETRY_COUNT;
    }

    /**
     * Marks the transaction as failed.
     */
    public void markFailed() {
        mTransactionState.setState(TransactionState.FAILED);
        Log.e(TAG, "Transaction failed after " + mRetryCount + " retries: " + mUri);
    }

    /**
     * Cancels the transaction.
     */
    public void cancel() {
        if (mThread != null && mThread.isAlive()) {
            mThread.interrupt();
        }
        mTransactionState.setState(TransactionState.CANCELLED);
    }

    /**
     * Checks if the transaction is completed.
     *
     * @return True if the transaction is completed (success or failed)
     */
    public boolean isCompleted() {
        int state = mTransactionState.getState();
        return state == TransactionState.SUCCESS || 
               state == TransactionState.FAILED || 
               state == TransactionState.CANCELLED;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "uri=" + mUri +
                ", state=" + mTransactionState.getState() +
                ", retryCount=" + mRetryCount +
                '}';
    }
}