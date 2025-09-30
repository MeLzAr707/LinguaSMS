package com.translator.messagingapp.mms;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.translator.messagingapp.R;
import com.translator.messagingapp.system.TranslatorApp;
import com.translator.messagingapp.message.MessageService;

/**
 * Enhanced MMS receiver that extends the Klinker library's MmsReceivedReceiver.
 * This replaces the complex custom MMS receiving implementation with the simplified Klinker approach.
 * 
 * Integrated improvements from Simple-SMS-Messenger:
 * - Enhanced address blocking with phone number normalization
 * - Better error handling with user-friendly messages
 * - Improved notification handling with attachment previews
 * - Better integration with image loading using Glide
 */
public class KlinkerMmsReceiver extends com.klinker.android.send_message.MmsReceivedReceiver {
    private static final String TAG = "KlinkerMmsReceiver";

    @Override
    public boolean isAddressBlocked(Context context, String address) {
        try {
            // Normalize phone number for consistent blocking checks
            String normalizedAddress = normalizePhoneNumber(address);
            
            // Get MessageService to check blocking
            TranslatorApp app = (TranslatorApp) context.getApplicationContext();
            MessageService messageService = app.getMessageService();
            
            if (messageService != null) {
                return messageService.isNumberBlocked(normalizedAddress);
            }
            
            // Fallback: no blocking if service unavailable
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking if address is blocked: " + address, e);
            // Default to not blocked on error
            return false;
        }
    }

    @Override
    public void onMessageReceived(Context context, Uri messageUri) {
        Log.d(TAG, "MMS received via Klinker library: " + messageUri);
        
        try {
            // Process the received MMS with enhanced handling
            TranslatorApp app = (TranslatorApp) context.getApplicationContext();
            MessageService messageService = app.getMessageService();
            
            if (messageService == null) {
                Log.w(TAG, "MessageService is null, unable to process MMS");
                showErrorToUser(context, "Message service unavailable");
                return;
            }
            
            // Get MMS details for enhanced processing
            MmsMessage mms = getMmsFromUri(context, messageUri);
            if (mms == null) {
                Log.w(TAG, "Could not retrieve MMS details from URI: " + messageUri);
                // Still try to process with basic handling
                messageService.processMmsMessage(messageUri);
                return;
            }
            
            // Process with enhanced notification handling
            processEnhancedMmsMessage(context, messageService, mms, messageUri);
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing received MMS", e);
            showErrorToUser(context, "Error processing MMS message");
            
            // Fallback to basic processing
            try {
                TranslatorApp app = (TranslatorApp) context.getApplicationContext();
                MessageService messageService = app.getMessageService();
                if (messageService != null) {
                    messageService.processMmsMessage(messageUri);
                }
            } catch (Exception fallbackError) {
                Log.e(TAG, "Fallback processing also failed", fallbackError);
            }
        }
    }

    @Override
    public void onError(Context context, String error) {
        Log.e(TAG, "Error receiving MMS via Klinker library: " + error);
        
        // Show user-friendly error message
        String userMessage = context.getString(R.string.couldnt_download_mms);
        showErrorToUser(context, userMessage);
    }
    
    /**
     * Normalizes a phone number for consistent processing.
     * Adapted from Simple-SMS-Messenger implementation.
     */
    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }
        
        // Remove all non-digit characters except +
        String normalized = phoneNumber.replaceAll("[^\\d+]", "");
        
        // Handle different international formats
        if (normalized.startsWith("+")) {
            return normalized;
        } else if (normalized.startsWith("00")) {
            return "+" + normalized.substring(2);
        } else if (normalized.length() == 10) {
            // Assume US number if 10 digits
            return "+1" + normalized;
        }
        
        return normalized;
    }
    
    /**
     * Retrieves MMS details from the content provider URI.
     */
    private MmsMessage getMmsFromUri(Context context, Uri messageUri) {
        try {
            // Use MmsHelper to query MMS details
            MmsHelper mmsHelper = new MmsHelper(context);
            return mmsHelper.getMmsFromUri(messageUri);
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving MMS from URI: " + messageUri, e);
            return null;
        }
    }
    
    /**
     * Processes MMS message with enhanced notification and image handling.
     * Inspired by Simple-SMS-Messenger implementation.
     */
    private void processEnhancedMmsMessage(Context context, MessageService messageService, 
                                         MmsMessage mms, Uri messageUri) {
        try {
            // Get sender information
            String address = mms.getSender();
            if (address == null || address.isEmpty()) {
                address = "Unknown";
            }
            
            // Process attachment for notification preview
            if (mms.hasAttachments()) {
                processAttachmentPreview(context, mms, address);
            }
            
            // Process the message through MessageService
            messageService.processMmsMessage(messageUri);
            
            // Update conversation and notification
            updateConversationAndNotification(context, mms, address);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in enhanced MMS processing", e);
            throw e; // Re-throw to trigger fallback
        }
    }
    
    /**
     * Processes attachment preview for notifications using Glide.
     * Adapted from Simple-SMS-Messenger.
     */
    private void processAttachmentPreview(Context context, MmsMessage mms, String address) {
        try {
            if (!mms.hasAttachments()) {
                return;
            }
            
            // Get the first image attachment for preview
            Uri attachmentUri = mms.getFirstImageAttachmentUri();
            if (attachmentUri == null) {
                Log.d(TAG, "No image attachments found for preview");
                return;
            }
            
            // Get notification icon size
            int size = context.getResources().getDimensionPixelSize(R.dimen.notification_large_icon_size);
            if (size <= 0) {
                size = 64; // Default size in dp converted to pixels
                float density = context.getResources().getDisplayMetrics().density;
                size = (int) (size * density);
            }
            
            // Load image asynchronously for notification
            new Thread(() -> {
                try {
                    Bitmap bitmap = Glide.with(context)
                            .asBitmap()
                            .load(attachmentUri)
                            .centerCrop()
                            .into(size, size)
                            .get();
                    
                    // Show notification with image preview on main thread
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    mainHandler.post(() -> {
                        showMmsNotificationWithPreview(context, mms, address, bitmap);
                    });
                    
                } catch (Exception e) {
                    Log.w(TAG, "Could not load attachment preview", e);
                    // Show notification without preview
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    mainHandler.post(() -> {
                        showMmsNotificationWithPreview(context, mms, address, null);
                    });
                }
            }).start();
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing attachment preview", e);
        }
    }
    
    /**
     * Shows MMS notification with optional image preview.
     */
    private void showMmsNotificationWithPreview(Context context, MmsMessage mms, 
                                              String address, Bitmap previewBitmap) {
        try {
            TranslatorApp app = (TranslatorApp) context.getApplicationContext();
            MessageService messageService = app.getMessageService();
            
            if (messageService != null) {
                messageService.showMmsNotification(mms.getId(), address, mms.getBody(), 
                                                 mms.getThreadId(), previewBitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing MMS notification", e);
        }
    }
    
    /**
     * Updates conversation and notification badges.
     * Based on Simple-SMS-Messenger implementation.
     */
    private void updateConversationAndNotification(Context context, MmsMessage mms, String address) {
        try {
            new Thread(() -> {
                try {
                    TranslatorApp app = (TranslatorApp) context.getApplicationContext();
                    MessageService messageService = app.getMessageService();
                    
                    if (messageService != null) {
                        // Update conversation
                        messageService.updateConversationForMms(mms);
                        
                        // Update unread count badge
                        messageService.updateUnreadCountBadge();
                        
                        // Refresh messages in UI
                        messageService.notifyMessagesChanged();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating conversation and notifications", e);
                }
            }).start();
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting conversation update", e);
        }
    }
    
    /**
     * Shows error message to user with proper UI thread handling.
     */
    private void showErrorToUser(Context context, String message) {
        try {
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> {
                try {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Log.e(TAG, "Error showing error toast", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in showErrorToUser", e);
        }
    }
}