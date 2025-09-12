package com.translator.messagingapp.contact;

import com.translator.messagingapp.message.*;

import com.translator.messagingapp.contact.*;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

/**
 * Dialog for managing contact-specific settings like notification tones.
 */
public class ContactSettingsDialog {
    private static final String TAG = "ContactSettingsDialog";
    private static final int REQUEST_CODE_RINGTONE_PICKER = 1002;
    
    private final Context context;
    private final String contactAddress;
    private final String contactName;
    private final UserPreferences userPreferences;
    private final OnToneSelectedListener toneSelectedListener;
    
    public interface OnToneSelectedListener {
        void onToneSelected(String contactAddress, String toneUri);
        void onRequestRingtoneSelection(int requestCode);
    }
    
    /**
     * Creates a new contact settings dialog.
     *
     * @param context The context
     * @param contactAddress The contact's phone number or address
     * @param contactName The contact's display name
     * @param listener Callback for tone selection events
     */
    public ContactSettingsDialog(Context context, String contactAddress, String contactName, OnToneSelectedListener listener) {
        this.context = context;
        this.contactAddress = contactAddress;
        this.contactName = contactName;
        this.userPreferences = new UserPreferences(context);
        this.toneSelectedListener = listener;
    }
    
    /**
     * Shows the contact settings dialog.
     */
    public void show() {
        String displayName = !TextUtils.isEmpty(contactName) ? contactName : contactAddress;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.contact_settings_title) + " - " + displayName);
        
        // Create options array
        String[] options = {
            context.getString(R.string.notification_tone)
        };
        
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // Notification tone
                        showNotificationToneSelection();
                        break;
                }
            }
        });
        
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }
    
    /**
     * Shows the notification tone selection dialog.
     */
    private void showNotificationToneSelection() {
        // Check current tone setting
        String currentTone = userPreferences.getContactNotificationTone(contactAddress);
        boolean hasCustomTone = currentTone != null && !currentTone.isEmpty();
        
        String currentToneDescription = hasCustomTone ? "Custom tone" : context.getString(R.string.default_tone);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.select_notification_tone));
        builder.setMessage("Current: " + currentToneDescription);
        
        // Options: Select custom tone, Use default tone, Cancel
        String[] toneOptions = {
            "Select Custom Tone",
            context.getString(R.string.default_tone)
        };
        
        builder.setItems(toneOptions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // Select custom tone
                        openRingtonePicker();
                        break;
                    case 1: // Use default tone
                        setDefaultTone();
                        break;
                }
            }
        });
        
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }
    
    /**
     * Opens the system ringtone picker.
     */
    private void openRingtonePicker() {
        try {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.select_notification_tone));
            
            // Set current selection if any
            String currentTone = userPreferences.getContactNotificationTone(contactAddress);
            if (currentTone != null && !currentTone.isEmpty()) {
                try {
                    Uri currentUri = Uri.parse(currentTone);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri);
                } catch (Exception e) {
                    Log.w(TAG, "Invalid current tone URI: " + currentTone, e);
                }
            } else {
                // Set default notification sound as current selection
                Uri defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, defaultUri);
            }
            
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
            
            if (toneSelectedListener != null) {
                toneSelectedListener.onRequestRingtoneSelection(REQUEST_CODE_RINGTONE_PICKER);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening ringtone picker", e);
            Toast.makeText(context, context.getString(R.string.error_setting_tone), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Sets the notification tone to default.
     */
    private void setDefaultTone() {
        userPreferences.setContactNotificationTone(contactAddress, null);
        Toast.makeText(context, context.getString(R.string.tone_reset_to_default), Toast.LENGTH_SHORT).show();
        
        if (toneSelectedListener != null) {
            toneSelectedListener.onToneSelected(contactAddress, null);
        }
    }
    
    /**
     * Handles the result from ringtone picker.
     *
     * @param selectedUri The selected ringtone URI, null if default/none selected
     */
    public void handleRingtonePickerResult(Uri selectedUri) {
        try {
            if (selectedUri != null) {
                String toneUriString = selectedUri.toString();
                userPreferences.setContactNotificationTone(contactAddress, toneUriString);
                Toast.makeText(context, context.getString(R.string.custom_tone_set), Toast.LENGTH_SHORT).show();
                
                if (toneSelectedListener != null) {
                    toneSelectedListener.onToneSelected(contactAddress, toneUriString);
                }
            } else {
                // User selected default or canceled
                setDefaultTone();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling ringtone picker result", e);
            Toast.makeText(context, context.getString(R.string.error_setting_tone), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Gets the request code for the ringtone picker.
     *
     * @return The request code
     */
    public static int getRingtonePickerRequestCode() {
        return REQUEST_CODE_RINGTONE_PICKER;
    }
}