package com.translator.messagingapp.mms;

import com.translator.messagingapp.message.*;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service that manages MMS transactions.
 * Handles the lifecycle of sending and receiving MMS messages.
 */
public class TransactionService extends Service {
    private static final String TAG = "TransactionService";
    
    // Intent extras
    public static final String EXTRA_URI = "uri";
    public static final String EXTRA_TRANSACTION_TYPE = "transaction_type";
    public static final String EXTRA_TOKEN = "token";

    // Message types for handler
    private static final int MSG_PROCESS_TRANSACTION = 1;
    private static final int MSG_RETRY_TRANSACTION = 2;

    private final ConcurrentMap<String, Transaction> mTransactions = new ConcurrentHashMap<>();
    private Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Create handler on main thread
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_PROCESS_TRANSACTION:
                        processTransaction((Transaction) msg.obj);
                        break;
                    case MSG_RETRY_TRANSACTION:
                        retryTransaction((Transaction) msg.obj);
                        break;
                }
            }
        };
        
        Log.d(TAG, "TransactionService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            handleIntent(intent);
        }
        
        // Don't restart automatically if killed
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Cancel all ongoing transactions
        for (Transaction transaction : mTransactions.values()) {
            transaction.cancel();
        }
        mTransactions.clear();
        
        Log.d(TAG, "TransactionService destroyed");
    }

    /**
     * Handles incoming intents to start transactions.
     *
     * @param intent The intent containing transaction parameters
     */
    private void handleIntent(Intent intent) {
        String uriString = intent.getStringExtra(EXTRA_URI);
        int transactionType = intent.getIntExtra(EXTRA_TRANSACTION_TYPE, -1);
        long token = intent.getLongExtra(EXTRA_TOKEN, 0);

        if (uriString == null || transactionType == -1) {
            Log.e(TAG, "Invalid intent parameters");
            return;
        }

        Uri uri = Uri.parse(uriString);
        Transaction transaction = createTransaction(transactionType, uri, token);
        
        if (transaction != null) {
            String key = generateTransactionKey(uri, transactionType);
            mTransactions.put(key, transaction);
            
            Message msg = mHandler.obtainMessage(MSG_PROCESS_TRANSACTION, transaction);
            mHandler.sendMessage(msg);
        } else {
            Log.e(TAG, "Failed to create transaction for type: " + transactionType);
        }
    }

    /**
     * Creates a transaction based on the type.
     *
     * @param type The transaction type
     * @param uri The message URI
     * @param token The token for network operations
     * @return The created transaction, or null if type is invalid
     */
    private Transaction createTransaction(int type, Uri uri, long token) {
        switch (type) {
            case Transaction.SEND_TRANSACTION:
                return new SendTransaction(this, uri, token);
            case Transaction.NOTIFICATION_TRANSACTION:
                return new NotificationTransaction(this, uri);
            default:
                Log.e(TAG, "Unknown transaction type: " + type);
                return null;
        }
    }

    /**
     * Processes a transaction.
     *
     * @param transaction The transaction to process
     */
    private void processTransaction(Transaction transaction) {
        try {
            Log.d(TAG, "Processing transaction: " + transaction);
            transaction.process();
            
            // Check if we need to retry
            if (transaction.getTransactionState().isFailed() && transaction.canRetry()) {
                scheduleRetry(transaction);
            } else if (transaction.isCompleted()) {
                // Remove completed transaction
                String key = generateTransactionKey(transaction.getUri(), transaction.getTransactionType());
                mTransactions.remove(key);
                
                // Stop service if no more transactions
                if (mTransactions.isEmpty()) {
                    stopSelf();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing transaction", e);
            transaction.markFailed();
        }
    }

    /**
     * Schedules a retry for a failed transaction.
     *
     * @param transaction The transaction to retry
     */
    private void scheduleRetry(Transaction transaction) {
        transaction.setRetryCount(transaction.getRetryCount() + 1);
        
        // Exponential backoff: 1s, 2s, 4s
        long delay = 1000L << (transaction.getRetryCount() - 1);
        
        Log.d(TAG, "Scheduling retry for transaction " + transaction + " in " + delay + "ms");
        
        Message msg = mHandler.obtainMessage(MSG_RETRY_TRANSACTION, transaction);
        mHandler.sendMessageDelayed(msg, delay);
    }

    /**
     * Retries a transaction.
     *
     * @param transaction The transaction to retry
     */
    private void retryTransaction(Transaction transaction) {
        Log.d(TAG, "Retrying transaction: " + transaction);
        transaction.getTransactionState().setState(TransactionState.INITIALIZED);
        processTransaction(transaction);
    }

    /**
     * Generates a unique key for a transaction.
     *
     * @param uri The message URI
     * @param type The transaction type
     * @return A unique key
     */
    private String generateTransactionKey(Uri uri, int type) {
        return uri.toString() + "_" + type;
    }

    /**
     * Gets all active transactions (for debugging).
     *
     * @return The map of active transactions
     */
    public ConcurrentMap<String, Transaction> getActiveTransactions() {
        return mTransactions;
    }
}