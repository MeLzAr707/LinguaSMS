package com.translator.messagingapp.mms;

import com.translator.messagingapp.message.*;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.translator.messagingapp.mms.pdu.GenericPdu;
import com.translator.messagingapp.mms.pdu.NotificationInd;
import com.translator.messagingapp.mms.pdu.PduHeaders;
import com.translator.messagingapp.mms.pdu.PduParser;
import com.translator.messagingapp.mms.pdu.PduPersister;
import com.translator.messagingapp.mms.pdu.RetrieveConf;
import com.translator.messagingapp.mms.http.HttpUtils;

/**
 * Transaction for handling MMS notification indications and downloading MMS content.
 */
public class NotificationTransaction extends Transaction implements Runnable {
    private static final String TAG = "NotificationTransaction";

    /**
     * Creates a new notification transaction.
     *
     * @param context The application context
     * @param uri The URI of the notification message
     */
    public NotificationTransaction(Context context, Uri uri) {
        super(context, uri);
    }

    @Override
    public void process() {
        mTransactionState.setState(TransactionState.PROCESSING);
        
        Log.d(TAG, "Starting notification transaction for: " + mUri);
        
        mThread = new Thread(this, "NotificationTransaction");
        mThread.start();
    }

    @Override
    public int getTransactionType() {
        return NOTIFICATION_TRANSACTION;
    }

    @Override
    public void run() {
        try {
            // Check if auto-download is enabled
            if (!allowAutoDownload(mContext)) {
                Log.d(TAG, "Auto-download disabled, skipping notification transaction");
                mTransactionState.setState(TransactionState.SUCCESS);
                return;
            }

            // Load the notification indication
            NotificationInd notificationInd = loadNotificationInd();
            if (notificationInd == null) {
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setErrorMessage("Failed to load notification indication");
                return;
            }

            // Get the content location for download
            byte[] contentLocation = notificationInd.getContentLocation();
            if (contentLocation == null) {
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setErrorMessage("No content location in notification");
                return;
            }

            // Download the MMS content
            byte[] mmsData = downloadMmsContent(new String(contentLocation));
            if (mmsData == null) {
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setErrorMessage("Failed to download MMS content");
                return;
            }

            // Parse and store the downloaded MMS
            Uri messageUri = storeDownloadedMms(mmsData);
            if (messageUri != null) {
                // Delete the notification
                deleteNotification();
                
                mTransactionState.setContentUri(messageUri);
                mTransactionState.setState(TransactionState.SUCCESS);
                Log.d(TAG, "Notification transaction completed successfully: " + mUri);
            } else {
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setErrorMessage("Failed to store downloaded MMS");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in notification transaction", e);
            mTransactionState.setState(TransactionState.FAILED);
            mTransactionState.setErrorMessage(e.getMessage());
        }
    }

    /**
     * Checks if auto-download is allowed.
     *
     * @param context The application context
     * @return True if auto-download is allowed
     */
    public static boolean allowAutoDownload(Context context) {
        // Check user preferences, network conditions, etc.
        // For now, always allow auto-download
        return true;
    }

    /**
     * Loads the notification indication from the content provider.
     *
     * @return The notification indication, or null if failed
     */
    private NotificationInd loadNotificationInd() {
        try {
            // Use PduPersister to load from content provider
            PduPersister persister = PduPersister.getPduPersister(mContext);
            GenericPdu pdu = persister.load(mUri);
            
            if (pdu instanceof NotificationInd) {
                return (NotificationInd) pdu;
            } else {
                Log.e(TAG, "Loaded PDU is not a NotificationInd: " + (pdu != null ? pdu.getClass().getSimpleName() : "null"));
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load notification indication", e);
            return null;
        }
    }

    /**
     * Downloads MMS content from the specified URL.
     *
     * @param contentLocation The URL to download from
     * @return The downloaded data, or null if failed
     */
    private byte[] downloadMmsContent(String contentLocation) {
        try {
            Log.d(TAG, "Downloading MMS content from: " + contentLocation);
            
            // Use HttpUtils to download from MMSC
            return HttpUtils.httpConnection(
                mContext, 
                0, // No token needed for GET
                contentLocation, 
                null, // No data to send for GET
                HttpUtils.CONTENT_TYPE_MMS,
                HttpUtils.HTTP_GET_METHOD
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to download MMS content", e);
            return null;
        }
    }

    /**
     * Stores the downloaded MMS data.
     *
     * @param mmsData The downloaded MMS data
     * @return The URI of the stored message, or null if failed
     */
    private Uri storeDownloadedMms(byte[] mmsData) {
        try {
            // Parse the downloaded data
            PduParser parser = new PduParser(mmsData);
            GenericPdu pdu = parser.parse();
            
            if (!(pdu instanceof RetrieveConf)) {
                Log.e(TAG, "Downloaded data is not a RetrieveConf: " + (pdu != null ? pdu.getClass().getSimpleName() : "null"));
                return null;
            }
            
            // Store the retrieved MMS in inbox
            PduPersister persister = PduPersister.getPduPersister(mContext);
            Uri inboxUri = Uri.parse("content://mms/inbox");
            
            return persister.persist(pdu, inboxUri, true, false, null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to store downloaded MMS", e);
            return null;
        }
    }

    /**
     * Deletes the notification indication.
     */
    private void deleteNotification() {
        try {
            mContext.getContentResolver().delete(mUri, null, null);
            Log.d(TAG, "Deleted notification: " + mUri);
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete notification", e);
        }
    }
}