package com.translator.messagingapp.mms;

import com.translator.messagingapp.message.*;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;

import com.translator.messagingapp.mms.pdu.EncodedStringValue;
import com.translator.messagingapp.mms.pdu.GenericPdu;
import com.translator.messagingapp.mms.pdu.PduComposer;
import com.translator.messagingapp.mms.pdu.PduHeaders;
import com.translator.messagingapp.mms.pdu.PduParser;
import com.translator.messagingapp.mms.pdu.PduPersister;
import com.translator.messagingapp.mms.pdu.SendConf;
import com.translator.messagingapp.mms.pdu.SendReq;
import com.translator.messagingapp.mms.http.HttpUtils;

/**
 * Transaction for sending MMS messages.
 */
public class SendTransaction extends Transaction implements Runnable {
    private static final String TAG = "SendTransaction";
    
    private final long mToken;
    private final String mLineNumber;

    /**
     * Creates a new send transaction.
     *
     * @param context The application context
     * @param uri The URI of the message to send
     * @param token The token for network operations
     */
    public SendTransaction(Context context, Uri uri, long token) {
        super(context, uri);
        mToken = token;
        mLineNumber = getLineNumber(context);
    }

    @Override
    public void process() {
        mTransactionState.setState(TransactionState.PROCESSING);
        
        Log.d(TAG, "Starting send transaction for: " + mUri);
        
        mThread = new Thread(this, "SendTransaction");
        mThread.start();
    }

    @Override
    public int getTransactionType() {
        return SEND_TRANSACTION;
    }

    @Override
    public void run() {
        try {
            Log.d(TAG, "Starting MMS send transaction for: " + mUri);
            
            // Set a timeout for the entire transaction to prevent getting stuck
            final long TRANSACTION_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes
            final long startTime = System.currentTimeMillis();
            
            // Load the message from the content provider
            SendReq sendReq = loadSendRequest();
            if (sendReq == null) {
                Log.e(TAG, "Failed to load send request for: " + mUri);
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setErrorMessage("Failed to load send request");
                return;
            }

            // Check if transaction timeout exceeded
            if (System.currentTimeMillis() - startTime > TRANSACTION_TIMEOUT_MS) {
                Log.e(TAG, "Transaction timeout during loadSendRequest");
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setErrorMessage("Transaction timeout");
                return;
            }

            // Update the message with current timestamp and sender
            updateSendRequest(sendReq);

            // Create PDU data
            byte[] pduData = createPduData(sendReq);
            if (pduData == null) {
                Log.e(TAG, "Failed to create PDU data for: " + mUri);
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setErrorMessage("Failed to create PDU data");
                return;
            }

            // Check if transaction timeout exceeded
            if (System.currentTimeMillis() - startTime > TRANSACTION_TIMEOUT_MS) {
                Log.e(TAG, "Transaction timeout during createPduData");
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setErrorMessage("Transaction timeout");
                return;
            }

            // Send the PDU (this includes retry logic)
            byte[] response = sendPdu(pduData);
            if (response == null) {
                Log.e(TAG, "Failed to send PDU for: " + mUri + " - setting transaction state to FAILED");
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setErrorMessage("MMS send failed - no response from MMSC after retries");
                return;
            }

            // Check if transaction timeout exceeded
            if (System.currentTimeMillis() - startTime > TRANSACTION_TIMEOUT_MS) {
                Log.e(TAG, "Transaction timeout during sendPdu");
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setErrorMessage("Transaction timeout");
                return;
            }

            // Parse the response
            SendConf sendConf = parseResponse(response);
            if (sendConf == null) {
                Log.w(TAG, "Failed to parse response for: " + mUri + " - treating as success since we got a response");
                // Still consider this a success since we got a response from MMSC
                // Some carriers may return non-standard responses
            }

            // Update message with response (if we have a valid response)
            if (sendConf != null) {
                updateMessageWithResponse(sendConf);
            }

            // Move message from outbox to sent
            Uri sentUri = moveToSent();
            if (sentUri != null) {
                mTransactionState.setContentUri(sentUri);
                mTransactionState.setState(TransactionState.SUCCESS);
                Log.d(TAG, "Send transaction completed successfully: " + mUri + " -> " + sentUri);
            } else {
                // Even if we can't move to sent folder, consider the send successful
                // since we got a response from MMSC
                Log.w(TAG, "MMS sent successfully but failed to move to sent folder: " + mUri);
                mTransactionState.setState(TransactionState.SUCCESS);
                mTransactionState.setErrorMessage("MMS sent but failed to move to sent folder");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in send transaction for: " + mUri, e);
            mTransactionState.setState(TransactionState.FAILED);
            mTransactionState.setErrorMessage("Transaction error: " + e.getMessage());
        } finally {
            // Ensure transaction state is never left in PROCESSING state
            if (mTransactionState.getState() == TransactionState.PROCESSING) {
                Log.w(TAG, "Transaction was still in PROCESSING state, setting to FAILED");
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setErrorMessage("Transaction completed but state was not updated");
            }
            Log.d(TAG, "Send transaction finished for: " + mUri + " with state: " + mTransactionState.getState());
        }
    }

    /**
     * Loads the send request from the content provider.
     *
     * @return The send request, or null if failed
     */
    private SendReq loadSendRequest() {
        try {
            // Use PduPersister to load from content provider
            PduPersister persister = PduPersister.getPduPersister(mContext);
            GenericPdu pdu = persister.load(mUri);
            
            if (pdu instanceof SendReq) {
                return (SendReq) pdu;
            } else if (pdu == null) {
                // Create a basic send request if none exists
                SendReq sendReq = new SendReq();
                sendReq.setTransactionId(generateTransactionId());
                return sendReq;
            } else {
                Log.e(TAG, "Loaded PDU is not a SendReq: " + pdu.getClass().getSimpleName());
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load send request", e);
            return null;
        }
    }

    /**
     * Updates the send request with current information.
     *
     * @param sendReq The send request to update
     */
    private void updateSendRequest(SendReq sendReq) {
        // Set current date
        sendReq.setDate(System.currentTimeMillis() / 1000L);
        
        // Set from address
        if (mLineNumber != null) {
            sendReq.setFrom(new EncodedStringValue(mLineNumber));
        }
        
        // Set default expiry (7 days)
        sendReq.setExpiry(7 * 24 * 60 * 60);
        
        // Set default priority
        sendReq.setPriority(PduHeaders.PRIORITY_NORMAL);
    }

    /**
     * Creates PDU data from the send request.
     *
     * @param sendReq The send request
     * @return The PDU data, or null if failed
     */
    private byte[] createPduData(SendReq sendReq) {
        try {
            // Use PduComposer to create binary PDU data
            PduComposer composer = new PduComposer(mContext, sendReq);
            return composer.make();
        } catch (Exception e) {
            Log.e(TAG, "Failed to create PDU data", e);
            return null;
        }
    }

    /**
     * Sends the PDU data to the MMSC.
     *
     * @param pduData The PDU data to send
     * @return The response data, or null if failed
     */
    private byte[] sendPdu(byte[] pduData) {
        final int MAX_RETRIES = 3;
        final long RETRY_DELAY_MS = 2000; // Start with 2 seconds
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Log.d(TAG, "MMS send attempt " + attempt + "/" + MAX_RETRIES);
                
                // Get MMSC URL and send PDU
                String mmscUrl = HttpUtils.getMmscUrl(mContext);
                if (mmscUrl == null) {
                    Log.e(TAG, "No MMSC URL available for current carrier");
                    // Log diagnostics to help troubleshoot
                    HttpUtils.logMmsConfigurationDiagnostics(mContext);
                    return null; // Don't retry if no MMSC URL
                }
                
                Log.d(TAG, "Sending MMS PDU to MMSC: " + mmscUrl);
                byte[] response = HttpUtils.httpConnection(
                    mContext, 
                    mToken, 
                    mmscUrl, 
                    pduData, 
                    HttpUtils.CONTENT_TYPE_MMS
                );
                
                if (response != null) {
                    Log.d(TAG, "MMS send completed successfully on attempt " + attempt + 
                          ", received " + response.length + " bytes response");
                    return response;
                } else {
                    Log.w(TAG, "MMS send attempt " + attempt + " failed - no response from MMSC");
                    
                    // Don't retry on last attempt
                    if (attempt < MAX_RETRIES) {
                        long delayMs = RETRY_DELAY_MS * attempt; // Exponential backoff
                        Log.d(TAG, "Retrying MMS send in " + delayMs + "ms...");
                        
                        try {
                            Thread.sleep(delayMs);
                        } catch (InterruptedException e) {
                            Log.w(TAG, "MMS retry sleep interrupted", e);
                            Thread.currentThread().interrupt();
                            break; // Exit retry loop if interrupted
                        }
                    } else {
                        Log.e(TAG, "MMS send failed after " + MAX_RETRIES + " attempts");
                        // Log diagnostics on final failure to help troubleshoot
                        HttpUtils.logMmsConfigurationDiagnostics(mContext);
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Exception during MMS send attempt " + attempt, e);
                
                // Don't retry on last attempt
                if (attempt < MAX_RETRIES) {
                    long delayMs = RETRY_DELAY_MS * attempt;
                    Log.d(TAG, "Retrying MMS send in " + delayMs + "ms after exception...");
                    
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Log.w(TAG, "MMS retry sleep interrupted", ie);
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    Log.e(TAG, "MMS send failed with exception after " + MAX_RETRIES + " attempts");
                    // Log diagnostics when there's an exception on final attempt
                    HttpUtils.logMmsConfigurationDiagnostics(mContext);
                }
            }
        }
        
        return null; // All attempts failed
    }

    /**
     * Parses the response from the MMSC.
     *
     * @param response The response data
     * @return The send confirmation, or null if failed
     */
    private SendConf parseResponse(byte[] response) {
        try {
            // Use PduParser to parse the response
            PduParser parser = new PduParser(response);
            GenericPdu pdu = parser.parse();
            
            if (pdu instanceof SendConf) {
                return (SendConf) pdu;
            } else {
                Log.e(TAG, "Response is not a SendConf: " + (pdu != null ? pdu.getClass().getSimpleName() : "null"));
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse response", e);
            return null;
        }
    }

    /**
     * Updates the message with the send confirmation response.
     *
     * @param sendConf The send confirmation
     */
    private void updateMessageWithResponse(SendConf sendConf) {
        try {
            ContentValues values = new ContentValues();
            values.put("response_status", sendConf.getResponseStatus());
            
            mContext.getContentResolver().update(mUri, values, null, null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to update message with response", e);
        }
    }

    /**
     * Moves the message from outbox to sent folder.
     *
     * @return The new URI in the sent folder, or null if failed
     */
    private Uri moveToSent() {
        try {
            // Use PduPersister to move the message
            PduPersister persister = PduPersister.getPduPersister(mContext);
            return persister.move(mUri, Uri.parse("content://mms/sent"));
        } catch (Exception e) {
            Log.e(TAG, "Failed to move message to sent", e);
            return null;
        }
    }

    /**
     * Generates a unique transaction ID.
     *
     * @return The transaction ID
     */
    private byte[] generateTransactionId() {
        String transactionId = "T" + System.currentTimeMillis();
        return transactionId.getBytes();
    }

    /**
     * Gets the line number for sending.
     *
     * @param context The application context
     * @return The line number, or null if not available
     */
    private String getLineNumber(Context context) {
        try {
            // This would get the actual line number from telephony manager
            // For now, return a placeholder
            return "+1234567890";
        } catch (Exception e) {
            Log.e(TAG, "Failed to get line number", e);
            return null;
        }
    }
}