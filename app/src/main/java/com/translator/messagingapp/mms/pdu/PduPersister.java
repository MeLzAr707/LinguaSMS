package com.translator.messagingapp.mms.pdu;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;

/**
 * Handles persistence of MMS PDUs to and from the content provider.
 */
public class PduPersister {
    private static final String TAG = "PduPersister";
    
    private final Context mContext;
    private final ContentResolver mContentResolver;
    private static PduPersister sInstance;

    /**
     * Creates a new PDU persister.
     *
     * @param context The application context
     */
    private PduPersister(Context context) {
        mContext = context.getApplicationContext();
        mContentResolver = mContext.getContentResolver();
    }

    /**
     * Gets the singleton instance of PduPersister.
     *
     * @param context The application context
     * @return The PDU persister instance
     */
    public static synchronized PduPersister getPduPersister(Context context) {
        if (sInstance == null) {
            sInstance = new PduPersister(context);
        }
        return sInstance;
    }

    /**
     * Loads a PDU from the content provider.
     *
     * @param uri The URI of the PDU to load
     * @return The loaded PDU, or null if failed
     */
    public GenericPdu load(Uri uri) {
        try {
            Log.d(TAG, "Loading PDU from: " + uri);
            
            // Query the MMS table for the message
            Cursor cursor = mContentResolver.query(uri, null, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                Log.w(TAG, "No data found for URI: " + uri);
                return null;
            }

            try {
                // Get message type to determine PDU type
                int messageType = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_TYPE));
                
                GenericPdu pdu = createPduFromType(messageType);
                if (pdu == null) {
                    Log.e(TAG, "Unknown message type: " + messageType);
                    return null;
                }

                // Load PDU data based on type
                loadPduData(cursor, pdu);
                
                // Load message body if applicable
                if (pdu instanceof SendReq) {
                    PduBody body = loadBody(uri);
                    ((SendReq) pdu).setBody(body);
                } else if (pdu instanceof RetrieveConf) {
                    PduBody body = loadBody(uri);
                    ((RetrieveConf) pdu).setBody(body);
                }
                
                return pdu;
                
            } finally {
                cursor.close();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading PDU", e);
            return null;
        }
    }

    /**
     * Persists a PDU to the content provider.
     *
     * @param pdu The PDU to persist
     * @param uri The target URI (e.g., Inbox, Outbox)
     * @param createThreadId Whether to create a thread ID
     * @param groupMmsEnabled Whether group MMS is enabled
     * @param excludePhoneNumbers Phone numbers to exclude
     * @return The URI of the persisted PDU, or null if failed
     */
    public Uri persist(GenericPdu pdu, Uri uri, boolean createThreadId, 
                      boolean groupMmsEnabled, String[] excludePhoneNumbers) {
        try {
            Log.d(TAG, "Persisting PDU to: " + uri);
            
            ContentValues values = new ContentValues();
            
            // Set common values
            values.put(Telephony.Mms.MESSAGE_TYPE, pdu.getMessageType());
            values.put(Telephony.Mms.DATE, System.currentTimeMillis() / 1000L);
            values.put(Telephony.Mms.MMS_VERSION, PduHeaders.CURRENT_MMS_VERSION);
            
            // Set PDU-specific values
            setPduSpecificValues(pdu, values);
            
            // Insert the message
            Uri messageUri = mContentResolver.insert(uri, values);
            if (messageUri == null) {
                Log.e(TAG, "Failed to insert PDU");
                return null;
            }
            
            // Add addresses
            addAddressesToPdu(messageUri, pdu);
            
            // Add body parts if applicable
            if (pdu instanceof SendReq) {
                SendReq sendReq = (SendReq) pdu;
                if (sendReq.getBody() != null) {
                    persistBody(messageUri, sendReq.getBody());
                }
            } else if (pdu instanceof RetrieveConf) {
                RetrieveConf retrieveConf = (RetrieveConf) pdu;
                if (retrieveConf.getBody() != null) {
                    persistBody(messageUri, retrieveConf.getBody());
                }
            }
            
            Log.d(TAG, "PDU persisted successfully: " + messageUri);
            return messageUri;
            
        } catch (Exception e) {
            Log.e(TAG, "Error persisting PDU", e);
            return null;
        }
    }

    /**
     * Moves a PDU from one location to another.
     *
     * @param from The source URI
     * @param to The destination URI
     * @return The new URI, or null if failed
     */
    public Uri move(Uri from, Uri to) {
        try {
            Log.d(TAG, "Moving PDU from " + from + " to " + to);
            
            // Update the message box
            ContentValues values = new ContentValues();
            
            // Determine the target message box based on the URI
            if (to.toString().contains("sent")) {
                values.put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_SENT);
            } else if (to.toString().contains("inbox")) {
                values.put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_INBOX);
            } else if (to.toString().contains("outbox")) {
                values.put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_OUTBOX);
            } else if (to.toString().contains("drafts")) {
                values.put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_DRAFTS);
            }
            
            int updated = mContentResolver.update(from, values, null, null);
            if (updated > 0) {
                Log.d(TAG, "PDU moved successfully");
                return from; // URI doesn't change, just the message box
            } else {
                Log.e(TAG, "Failed to move PDU");
                return null;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error moving PDU", e);
            return null;
        }
    }

    /**
     * Creates a PDU object based on the message type.
     *
     * @param messageType The message type
     * @return The created PDU, or null if unknown type
     */
    private GenericPdu createPduFromType(int messageType) {
        switch (messageType) {
            case PduHeaders.MESSAGE_TYPE_SEND_REQ:
                return new SendReq();
            case PduHeaders.MESSAGE_TYPE_SEND_CONF:
                return new SendConf();
            case PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND:
                return new NotificationInd();
            case PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF:
                return new RetrieveConf();
            default:
                return null;
        }
    }

    /**
     * Loads PDU data from a cursor.
     *
     * @param cursor The cursor containing PDU data
     * @param pdu The PDU to populate
     */
    private void loadPduData(Cursor cursor, GenericPdu pdu) {
        try {
            if (pdu instanceof SendReq) {
                loadSendReqData(cursor, (SendReq) pdu);
            } else if (pdu instanceof SendConf) {
                loadSendConfData(cursor, (SendConf) pdu);
            } else if (pdu instanceof NotificationInd) {
                loadNotificationIndData(cursor, (NotificationInd) pdu);
            } else if (pdu instanceof RetrieveConf) {
                loadRetrieveConfData(cursor, (RetrieveConf) pdu);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading PDU data", e);
        }
    }

    /**
     * Loads data for a SendReq PDU.
     */
    private void loadSendReqData(Cursor cursor, SendReq sendReq) {
        // Load basic fields
        try {
            int dateIndex = cursor.getColumnIndex(Telephony.Mms.DATE);
            if (dateIndex >= 0) {
                sendReq.setDate(cursor.getLong(dateIndex));
            }
            
            int expiryIndex = cursor.getColumnIndex(Telephony.Mms.EXPIRY);
            if (expiryIndex >= 0) {
                sendReq.setExpiry(cursor.getInt(expiryIndex));
            }
            
            // Set default transaction ID if not present
            if (sendReq.getTransactionId() == null) {
                String transactionId = "T" + System.currentTimeMillis();
                sendReq.setTransactionId(transactionId.getBytes());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading SendReq data", e);
        }
    }

    /**
     * Loads data for a SendConf PDU.
     */
    private void loadSendConfData(Cursor cursor, SendConf sendConf) {
        try {
            int responseStatusIndex = cursor.getColumnIndex(Telephony.Mms.RESPONSE_STATUS);
            if (responseStatusIndex >= 0) {
                sendConf.setResponseStatus(cursor.getInt(responseStatusIndex));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading SendConf data", e);
        }
    }

    /**
     * Loads data for a NotificationInd PDU.
     */
    private void loadNotificationIndData(Cursor cursor, NotificationInd notificationInd) {
        try {
            int contentLocationIndex = cursor.getColumnIndex(Telephony.Mms.CONTENT_LOCATION);
            if (contentLocationIndex >= 0) {
                String contentLocation = cursor.getString(contentLocationIndex);
                if (contentLocation != null) {
                    notificationInd.setContentLocation(contentLocation.getBytes());
                }
            }
            
            int messageSizeIndex = cursor.getColumnIndex(Telephony.Mms.MESSAGE_SIZE);
            if (messageSizeIndex >= 0) {
                notificationInd.setMessageSize(cursor.getLong(messageSizeIndex));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading NotificationInd data", e);
        }
    }

    /**
     * Loads data for a RetrieveConf PDU.
     */
    private void loadRetrieveConfData(Cursor cursor, RetrieveConf retrieveConf) {
        try {
            int dateIndex = cursor.getColumnIndex(Telephony.Mms.DATE);
            if (dateIndex >= 0) {
                retrieveConf.setDate(cursor.getLong(dateIndex));
            }
            
            int priorityIndex = cursor.getColumnIndex(Telephony.Mms.PRIORITY);
            if (priorityIndex >= 0) {
                retrieveConf.setPriority(cursor.getInt(priorityIndex));
            }
            
            int contentTypeIndex = cursor.getColumnIndex(Telephony.Mms.CONTENT_TYPE);
            if (contentTypeIndex >= 0) {
                String contentType = cursor.getString(contentTypeIndex);
                if (contentType != null) {
                    retrieveConf.setContentType(contentType.getBytes());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading RetrieveConf data", e);
        }
    }

    /**
     * Sets PDU-specific values in ContentValues.
     */
    private void setPduSpecificValues(GenericPdu pdu, ContentValues values) {
        if (pdu instanceof SendReq) {
            SendReq sendReq = (SendReq) pdu;
            values.put(Telephony.Mms.DATE, sendReq.getDate());
            values.put(Telephony.Mms.EXPIRY, sendReq.getExpiry());
            values.put(Telephony.Mms.PRIORITY, sendReq.getPriority());
            values.put(Telephony.Mms.DELIVERY_REPORT, sendReq.getDeliveryReport());
            values.put(Telephony.Mms.READ_REPORT, sendReq.getReadReport());
        } else if (pdu instanceof SendConf) {
            SendConf sendConf = (SendConf) pdu;
            values.put(Telephony.Mms.RESPONSE_STATUS, sendConf.getResponseStatus());
        } else if (pdu instanceof NotificationInd) {
            NotificationInd notificationInd = (NotificationInd) pdu;
            if (notificationInd.getContentLocation() != null) {
                values.put(Telephony.Mms.CONTENT_LOCATION, new String(notificationInd.getContentLocation()));
            }
            values.put(Telephony.Mms.MESSAGE_SIZE, notificationInd.getMessageSize());
        } else if (pdu instanceof RetrieveConf) {
            RetrieveConf retrieveConf = (RetrieveConf) pdu;
            values.put(Telephony.Mms.DATE, retrieveConf.getDate());
            values.put(Telephony.Mms.PRIORITY, retrieveConf.getPriority());
            if (retrieveConf.getContentType() != null) {
                values.put(Telephony.Mms.CONTENT_TYPE, new String(retrieveConf.getContentType()));
            }
        }
    }

    /**
     * Loads the body of an MMS message.
     */
    private PduBody loadBody(Uri messageUri) {
        // For now, return an empty body
        // In a real implementation, this would query the parts table
        return new PduBody();
    }

    /**
     * Adds addresses to a PDU.
     */
    private void addAddressesToPdu(Uri messageUri, GenericPdu pdu) {
        // For now, do nothing
        // In a real implementation, this would add addresses to the addr table
    }

    /**
     * Persists the body of an MMS message.
     */
    private void persistBody(Uri messageUri, PduBody body) {
        // For now, do nothing
        // In a real implementation, this would insert parts into the parts table
    }
}