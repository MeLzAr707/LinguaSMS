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
            // Load the message from the content provider
            SendReq sendReq = loadSendRequest();
            if (sendReq == null) {
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setErrorMessage("Failed to load send request");
                return;
            }

            // Update the message with current timestamp and sender
            updateSendRequest(sendReq);

            // Create PDU data
            byte[] pduData = createPduData(sendReq);
            if (pduData == null) {
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setErrorMessage("Failed to create PDU data");
                return;
            }

            // Send the PDU
            byte[] response = sendPdu(pduData);
            if (response == null) {
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setErrorMessage("Failed to send PDU");
                return;
            }

            // Parse the response
            SendConf sendConf = parseResponse(response);
            if (sendConf == null) {
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setErrorMessage("Failed to parse response");
                return;
            }

            // Update message with response
            updateMessageWithResponse(sendConf);

            // Move message from outbox to sent
            Uri sentUri = moveToSent();
            if (sentUri != null) {
                mTransactionState.setContentUri(sentUri);
                mTransactionState.setState(TransactionState.SUCCESS);
                Log.d(TAG, "Send transaction completed successfully: " + mUri);
            } else {
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setErrorMessage("Failed to move message to sent folder");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in send transaction", e);
            mTransactionState.setState(TransactionState.FAILED);
            mTransactionState.setErrorMessage(e.getMessage());
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
        try {
            // Get MMSC URL and send PDU
            String mmscUrl = HttpUtils.getMmscUrl(mContext);
            if (mmscUrl == null) {
                Log.e(TAG, "No MMSC URL available");
                return null;
            }
            
            return HttpUtils.httpConnection(
                mContext, 
                mToken, 
                mmscUrl, 
                pduData, 
                HttpUtils.CONTENT_TYPE_MMS
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to send PDU", e);
            return null;
        }
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