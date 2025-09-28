package com.translator.messagingapp.system;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

import com.translator.messagingapp.util.PhoneUtils;

/**
 * A headless service for sending SMS messages via RESPOND_VIA_MESSAGE intents.
 * This service is required for an app to be set as the default SMS app in Android.
 * It handles quick reply functionality from notifications and other system integrations.
 */
public class HeadlessSmsSendService extends Service {
    private static final String TAG = "HeadlessSmsSendService";

    /**
     * Called when the service is created.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SMS send service created");
    }

    /**
     * Called when the service is started with a RESPOND_VIA_MESSAGE intent.
     *
     * @param intent The intent that started the service
     * @param flags Additional data about this start request
     * @param startId A unique integer representing this specific request to start
     * @return The return value indicates what semantics the system should use for the service's current started state
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        Log.d(TAG, "Service started with action: " + action);

        if (Intent.ACTION_RESPOND_VIA_MESSAGE.equals(action)) {
            handleRespondViaMessage(intent);
        } else {
            Log.w(TAG, "Unknown action received: " + action);
        }

        // Stop the service after handling the intent
        stopSelf(startId);
        return Service.START_NOT_STICKY;
    }

    /**
     * Handles RESPOND_VIA_MESSAGE intents for quick SMS replies.
     */
    private void handleRespondViaMessage(Intent intent) {
        if (intent == null) {
            Log.e(TAG, "Intent is null");
            return;
        }

        // Check if app is set as default SMS app
        if (!PhoneUtils.isDefaultSmsApp(this)) {
            Log.e(TAG, "Cannot send SMS response: App is not default SMS app");
            return;
        }

        try {
            // Extract the recipient and message from the intent
            String recipient = getRecipientFromIntent(intent);
            String message = intent.getStringExtra(Intent.EXTRA_TEXT);

            if (TextUtils.isEmpty(recipient)) {
                Log.e(TAG, "No recipient found in RESPOND_VIA_MESSAGE intent");
                return;
            }

            if (TextUtils.isEmpty(message)) {
                Log.e(TAG, "No message text found in RESPOND_VIA_MESSAGE intent");
                return;
            }

            Log.d(TAG, "Sending SMS response to: " + recipient);
            sendSmsResponse(recipient, message);

        } catch (Exception e) {
            Log.e(TAG, "Error handling RESPOND_VIA_MESSAGE intent", e);
        }
    }

    /**
     * Extracts the recipient phone number from the intent URI.
     */
    private String getRecipientFromIntent(Intent intent) {
        String recipient = null;
        
        if (intent.getData() != null) {
            String uriString = intent.getData().toString();
            Log.d(TAG, "Intent URI: " + uriString);
            
            // Parse different URI formats (sms:, smsto:, tel:)
            if (uriString.startsWith("sms:") || uriString.startsWith("smsto:")) {
                recipient = uriString.substring(uriString.indexOf(':') + 1);
            } else if (uriString.startsWith("tel:")) {
                recipient = uriString.substring(4);
            }
        }
        
        // Remove any query parameters
        if (recipient != null && recipient.contains("?")) {
            recipient = recipient.substring(0, recipient.indexOf('?'));
        }
        
        return recipient;
    }

    /**
     * Sends an SMS response using the Telephony API.
     */
    private void sendSmsResponse(String recipient, String message) {
        try {
            // Use SmsManager from system service (API 19+ compatible)
            SmsManager smsManager;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                smsManager = getSystemService(SmsManager.class);
            } else {
                smsManager = SmsManager.getDefault();
            }

            // Create PendingIntents for delivery tracking
            PendingIntent sentIntent = PendingIntent.getBroadcast(
                this, 0, 
                new Intent("SMS_SENT"), 
                PendingIntent.FLAG_IMMUTABLE
            );
            PendingIntent deliveryIntent = PendingIntent.getBroadcast(
                this, 0, 
                new Intent("SMS_DELIVERED"), 
                PendingIntent.FLAG_IMMUTABLE
            );

            // Send the SMS
            if (message.length() > 160) {
                // Split message into parts if too long
                java.util.ArrayList<String> parts = smsManager.divideMessage(message);
                smsManager.sendMultipartTextMessage(recipient, null, parts, null, null);
            } else {
                smsManager.sendTextMessage(recipient, null, message, sentIntent, deliveryIntent);
            }

            Log.d(TAG, "SMS response sent successfully to: " + recipient);

        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS response", e);
        }
    }

    /**
     * Called when a client binds to the service.
     * This method must be implemented, but can return null if clients can't bind to the service.
     *
     * @param intent The intent that was used to bind to this service
     * @return An IBinder through which clients can call on to the service
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service bound with intent: " + (intent != null ? intent.getAction() : "null"));
        return null;
    }

    /**
     * Called when the service is being destroyed.
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "SMS send service destroyed");
        super.onDestroy();
    }
}
