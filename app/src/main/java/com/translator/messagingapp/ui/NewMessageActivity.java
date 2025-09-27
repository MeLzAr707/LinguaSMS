package com.translator.messagingapp.ui;

import com.translator.messagingapp.R;
import com.translator.messagingapp.ui.*;

import com.translator.messagingapp.system.*;

import com.translator.messagingapp.sms.*;

import com.translator.messagingapp.mms.*;

import com.translator.messagingapp.message.*;

import com.translator.messagingapp.contact.*;

import com.translator.messagingapp.conversation.*;

import com.translator.messagingapp.translation.*;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.OpenableColumns;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;
import java.util.List;
import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.translator.messagingapp.util.PhoneUtils;
import com.translator.messagingapp.util.SecretMessageDialog;
import com.translator.messagingapp.util.SecretMessageUtils;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.BroadcastReceiver;

import java.io.InputStream;

public class NewMessageActivity extends BaseActivity {
    private static final String TAG = "NewMessageActivity";
    private static final int CONTACT_PICKER_RESULT = 1001;
    private static final int ATTACHMENT_PICK_REQUEST = 1009;

    private EditText recipientInput;
    private EditText messageInput;
    private ImageButton sendButton;  // Changed from Button to ImageButton
    private ImageButton contactButton;
    private ImageButton translateButton;
    private ImageButton genAIButton;
    private ImageButton attachmentButton;
    private CheckBox secretMessageCheckbox; // New field for secret message checkbox
    private final AtomicBoolean isTranslating = new AtomicBoolean(false);
    private String originalComposedText = "";
    private boolean isComposedTextTranslated = false;
    private TextWatcher recipientTextWatcher;
    private TextWatcher messageTextWatcher;
    protected boolean isActivityActive = true;
    
    // Attachment preview components
    private LinearLayout attachmentPreviewContainer;
    private ImageView attachmentPreviewImage;
    private ImageButton removeAttachmentButton;

    // Service classes
    private MessageService messageService;
    private TranslationManager translationManager;
    private DefaultSmsAppManager defaultSmsAppManager;
    
    // MMS send result receiver
    private BroadcastReceiver mmsSendResultReceiver;
    private UserPreferences userPreferences;
    
    // Selected attachments for sending
    private List<Uri> selectedAttachments;
    
    // Secret message storage
    private String currentSecretMessage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_new_message);

            // Get service instances from TranslatorApp
            messageService = ((TranslatorApp) getApplication()).getMessageService();
            translationManager = ((TranslatorApp) getApplication()).getTranslationManager();
            defaultSmsAppManager = ((TranslatorApp) getApplication()).getDefaultSmsAppManager();
            userPreferences = ((TranslatorApp) getApplication()).getUserPreferences();

            // Initialize data
            selectedAttachments = new ArrayList<>();

            // Set up toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("New Message");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            
            // Apply theme-specific toolbar styling
            updateToolbarTheme(toolbar);

            // Initialize UI components with correct types
            recipientInput = findViewById(R.id.recipient_input);
            messageInput = findViewById(R.id.message_input);
            sendButton = findViewById(R.id.send_button);  // This is a Button in XML
            contactButton = findViewById(R.id.contact_button);  // This is an ImageButton in XML
            translateButton = findViewById(R.id.translate_button);  // This is an ImageButton in XML
            genAIButton = findViewById(R.id.genai_button);  // This is an ImageButton in XML
            attachmentButton = findViewById(R.id.attachment_button);  // This is an ImageButton in XML
            secretMessageCheckbox = findViewById(R.id.secret_message_checkbox); // Initialize secret message checkbox
            
            // Initialize attachment preview components
            attachmentPreviewContainer = findViewById(R.id.attachment_preview_container);
            attachmentPreviewImage = findViewById(R.id.attachment_preview_image);
            removeAttachmentButton = findViewById(R.id.remove_attachment_button);

            // Restore state if available
            if (savedInstanceState != null) {
                isComposedTextTranslated = savedInstanceState.getBoolean("isComposedTextTranslated", false);
                originalComposedText = savedInstanceState.getString("originalComposedText", "");
            }

            // Set up send button
            if (sendButton != null) {
                sendButton.setOnClickListener(v -> {
                    if (!PhoneUtils.hasTelephonyFeature(this)) {
                        Toast.makeText(NewMessageActivity.this,
                                "SMS messaging is not available on this device",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendMessage();
                });
                sendButton.setEnabled(false);
                sendButton.setAlpha(0.5f);
            }

            // Set up contact button
            if (contactButton != null) {
                contactButton.setOnClickListener(v -> pickContact());
            }

            // Set up attachment button
            if (attachmentButton != null) {
                attachmentButton.setOnClickListener(v -> openAttachmentPicker());
                
                // Long press to clear selected attachments
                attachmentButton.setOnLongClickListener(v -> {
                    if (selectedAttachments != null && !selectedAttachments.isEmpty()) {
                        selectedAttachments.clear();
                        updateAttachmentPreview();
                        Toast.makeText(this, "Attachments cleared", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    return false;
                });
            }
            
            // Set up remove attachment button
            if (removeAttachmentButton != null) {
                removeAttachmentButton.setOnClickListener(v -> {
                    selectedAttachments.clear();
                    updateAttachmentPreview();
                    Toast.makeText(this, "Attachment removed", Toast.LENGTH_SHORT).show();
                });
            }

            // Set up translate button
            if (translateButton != null) {
                translateButton.setImageResource(android.R.drawable.ic_menu_edit);
                translateButton.setContentDescription("Translate message input");
                translateButton.setOnClickListener(v -> translateMessageInput());
                updateInputTranslationState();
            }

            // Set up GenAI button
            if (genAIButton != null) {
                genAIButton.setImageResource(android.R.drawable.ic_menu_help);
                genAIButton.setContentDescription("AI Features");
            }

            // Set up secret message checkbox
            if (secretMessageCheckbox != null) {
                secretMessageCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        showSecretMessageDialog();
                    }
                });
            }

            // Set up text watchers for input validation
            setupTextWatchers();
            
            // Set up MMS send result receiver
            setupMmsSendResultReceiver();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing NewMessageActivity", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isComposedTextTranslated", isComposedTextTranslated);
        outState.putString("originalComposedText", originalComposedText);
    }

    private void setupTextWatchers() {
        // Create TextWatcher instances and store references
        recipientTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not used
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateSendButtonState();
            }
        };

        messageTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not used
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateSendButtonState();
            }
        };

        if (recipientInput != null) {
            recipientInput.addTextChangedListener(recipientTextWatcher);
        }

        if (messageInput != null) {
            messageInput.addTextChangedListener(messageTextWatcher);
        }
        
        // Apply custom colors if using custom theme
        applyCustomColorsToViews();
    }

    /**
     * Sets up the broadcast receiver to handle MMS send results
     */
    private void setupMmsSendResultReceiver() {
        mmsSendResultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.translator.messagingapp.MMS_SEND_RESULT".equals(intent.getAction())) {
                    boolean success = intent.getBooleanExtra("success", false);
                    String messageUri = intent.getStringExtra("message_uri");
                    
                    Log.d(TAG, "Received MMS send result: " + (success ? "SUCCESS" : "FAILED"));
                    
                    if (success) {
                        // MMS sent successfully - navigate to conversation
                        String recipient = recipientInput != null ? recipientInput.getText().toString().trim() : "";
                        if (!recipient.isEmpty()) {
                            // Extract phone number if in format "Name <number>"
                            if (recipient.contains("<") && recipient.contains(">")) {
                                recipient = recipient.substring(
                                        recipient.indexOf("<") + 1,
                                        recipient.indexOf(">"));
                            }
                            recipient = PhoneNumberUtils.stripSeparators(recipient);
                            
                            Intent conversationIntent = new Intent(NewMessageActivity.this, ConversationActivity.class);
                            conversationIntent.putExtra("address", recipient);
                            startActivity(conversationIntent);
                            finish();
                        } else {
                            Toast.makeText(NewMessageActivity.this, "MMS sent successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        // MMS send failed - show error
                        Toast.makeText(NewMessageActivity.this, "Failed to send MMS message", Toast.LENGTH_LONG).show();
                    }
                }
            }
        };
        
        // Register the receiver
        android.content.IntentFilter filter = new android.content.IntentFilter("com.translator.messagingapp.MMS_SEND_RESULT");
        LocalBroadcastManager.getInstance(this).registerReceiver(mmsSendResultReceiver, filter);
    }

    private void updateSendButtonState() {
        if (sendButton != null) {
            boolean hasRecipient = recipientInput != null &&
                    !recipientInput.getText().toString().trim().isEmpty();
            boolean hasMessage = messageInput != null &&
                    !messageInput.getText().toString().trim().isEmpty();

            boolean enabled = hasRecipient && hasMessage;
            sendButton.setEnabled(enabled);
            sendButton.setAlpha(enabled ? 1.0f : 0.5f);
        }
    }

    private void pickContact() {
        try {
            Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
        } catch (Exception e) {
            Log.e(TAG, "Error launching contact picker", e);
            Toast.makeText(this, "Error accessing contacts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openAttachmentPicker() {
        // Create an intent to pick attachments with proper permissions
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        
        // Add flags for URI permissions as recommended in issue #594
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        
        try {
            startActivityForResult(Intent.createChooser(intent, "Select attachment"), ATTACHMENT_PICK_REQUEST);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a file manager to select attachments", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CONTACT_PICKER_RESULT && resultCode == RESULT_OK) {
            try {
                Uri contactUri = data.getData();
                if (contactUri != null) {
                    try (Cursor cursor = getContentResolver().query(contactUri, new String[]{
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                    }, null, null, null)) {

                        if (cursor != null && cursor.moveToFirst()) {
                            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);

                            if (numberIndex >= 0 && nameIndex >= 0) {
                                String number = cursor.getString(numberIndex);
                                String name = cursor.getString(nameIndex);

                                // Format as "Name <number>"
                                String formattedContact = name + " <" + number + ">";

                                // Set recipient
                                if (recipientInput != null) {
                                    recipientInput.setText(formattedContact);
                                    // Move cursor to end
                                    recipientInput.setSelection(formattedContact.length());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing contact selection", e);
                Toast.makeText(this, "Error selecting contact: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == ATTACHMENT_PICK_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedUri = data.getData();
            if (selectedUri != null) {
                // Take persistent URI permission as recommended in issue #594
                try {
                    getContentResolver().takePersistableUriPermission(selectedUri, 
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    Log.d(TAG, "Persistent URI permission granted for: " + selectedUri);
                } catch (SecurityException e) {
                    Log.w(TAG, "Could not take persistent permission for URI: " + selectedUri, e);
                    // Continue anyway - some content providers don't support persistent permissions
                }
                
                // Validate file size against MMS limits
                if (!validateAttachmentSize(selectedUri)) {
                    return; // Error message already shown by validateAttachmentSize
                }
                
                // Store the attachment for sending
                selectedAttachments.clear(); // Clear previous attachments (single attachment for now)
                selectedAttachments.add(selectedUri);
                
                // Show confirmation and update preview
                String fileName = getFileNameFromUri(selectedUri);
                Toast.makeText(this, "Attachment selected: " + fileName, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Attachment selected: " + selectedUri.toString());
                
                // Update preview and send button
                updateAttachmentPreview();
            }
        }
    }

    private void sendMessage() {
        // Check telephony feature again
        if (!PhoneUtils.hasTelephonyFeature(this)) {
            Toast.makeText(this,
                    "SMS messaging is not available on this device",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String recipient = recipientInput != null ? recipientInput.getText().toString().trim() : "";
        String messageText = messageInput != null ? messageInput.getText().toString().trim() : "";
        boolean hasAttachments = selectedAttachments != null && !selectedAttachments.isEmpty();

        // For MMS, allow empty text if there are attachments
        if (recipient.isEmpty() || (messageText.isEmpty() && !hasAttachments)) {
            return;
        }

        // Extract phone number if in format "Name <number>"
        if (recipient.contains("<") && recipient.contains(">")) {
            recipient = recipient.substring(
                    recipient.indexOf("<") + 1,
                    recipient.indexOf(">"));
        }

        // Normalize phone number
        recipient = PhoneNumberUtils.stripSeparators(recipient);

        try {
            final String finalRecipient = recipient;
            List<Uri> attachmentsToSend = hasAttachments ? new ArrayList<>(selectedAttachments) : null;
            
            // Add logging as requested in issue #594 to confirm MMS vs SMS path
            if (hasAttachments) {
                Log.d(TAG, "Sending MMS message to " + finalRecipient + " with " + attachmentsToSend.size() + " attachments");
                Log.d(TAG, "Attachment URIs: " + attachmentsToSend.toString());
                
                // Handle secret message encoding if enabled
                String finalMessageText = messageText;
                if (secretMessageCheckbox != null && secretMessageCheckbox.isChecked() && !currentSecretMessage.isEmpty()) {
                    finalMessageText = SecretMessageUtils.encodeSecretMessage(messageText, currentSecretMessage);
                    Log.d(TAG, "Secret message encoded into MMS");
                }
                
                // Send as MMS with attachments
                boolean success = messageService.sendMmsMessage(finalRecipient, null, finalMessageText, attachmentsToSend);
                
                Log.d(TAG, "MMS send initiated: " + (success ? "SUCCESS" : "FAILED"));
                
                if (success) {
                    // MMS send initiated successfully - wait for result callback
                    // Clear attachments since send was initiated
                    selectedAttachments.clear();
                    updateAttachmentPreview();
                    
                    // Show sending indicator to user
                    Toast.makeText(this, "Sending MMS...", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error sending MMS message", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.d(TAG, "Sending SMS message to " + finalRecipient + " (no attachments)");
                
                // Handle secret message encoding if enabled
                String finalMessageText = messageText;
                if (secretMessageCheckbox != null && secretMessageCheckbox.isChecked() && !currentSecretMessage.isEmpty()) {
                    finalMessageText = SecretMessageUtils.encodeSecretMessage(messageText, currentSecretMessage);
                    Log.d(TAG, "Secret message encoded into SMS");
                }
                
                // Send as regular SMS with callback
                messageService.sendSmsMessage(recipient, finalMessageText, null, () -> {
                    // Success callback
                    setResult(RESULT_OK);
                    Intent intent = new Intent(NewMessageActivity.this, ConversationActivity.class);
                    intent.putExtra("address", finalRecipient);
                    startActivity(intent);
                    finish();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending message", e);
            Toast.makeText(this, "Error sending message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows the secret message composition dialog
     */
    private void showSecretMessageDialog() {
        String currentMessage = messageInput != null ? messageInput.getText().toString() : "";
        
        SecretMessageDialog dialog = SecretMessageDialog.newInstance(currentMessage, currentSecretMessage);
        dialog.setSecretMessageDialogListener(new SecretMessageDialog.SecretMessageDialogListener() {
            @Override
            public void onSecretMessageComposed(String visibleMessage, String secretMessage) {
                // Update the message input with the visible message
                if (messageInput != null) {
                    messageInput.setText(visibleMessage);
                }
                
                // Store the secret message
                currentSecretMessage = secretMessage;
                
                // Update checkbox state and show feedback
                if (secretMessage.isEmpty()) {
                    if (secretMessageCheckbox != null) {
                        secretMessageCheckbox.setChecked(false);
                    }
                    currentSecretMessage = "";
                    Toast.makeText(NewMessageActivity.this, R.string.secret_message, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(NewMessageActivity.this, R.string.secret_message_enabled, Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        try {
            dialog.show(getSupportFragmentManager(), "SecretMessageDialog");
        } catch (Exception e) {
            Log.e(TAG, "Error showing secret message dialog", e);
            // Fallback: uncheck the checkbox
            if (secretMessageCheckbox != null) {
                secretMessageCheckbox.setChecked(false);
            }
        }
    }

    /**
     * Updates the attachment preview display based on selected attachments
     */
    private void updateAttachmentPreview() {
        if (attachmentPreviewContainer == null || attachmentPreviewImage == null) {
            return;
        }

        boolean hasAttachments = selectedAttachments != null && !selectedAttachments.isEmpty();
        
        if (hasAttachments) {
            // Show preview container
            attachmentPreviewContainer.setVisibility(View.VISIBLE);
            
            // Load the first attachment (single attachment for now)
            Uri attachmentUri = selectedAttachments.get(0);
            String mimeType = getContentResolver().getType(attachmentUri);
            
            if (mimeType != null && mimeType.startsWith("image/")) {
                // Load image using Glide
                try {
                    Glide.with(this)
                        .load(attachmentUri)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(attachmentPreviewImage);
                } catch (Exception e) {
                    Log.e(TAG, "Error loading image preview", e);
                    // Fallback to attachment icon
                    attachmentPreviewImage.setImageResource(R.drawable.ic_attachment);
                }
            } else {
                // Show attachment icon for non-image files
                attachmentPreviewImage.setImageResource(R.drawable.ic_attachment);
            }
            
            updateSendButtonForAttachments();
        } else {
            // Hide preview container
            attachmentPreviewContainer.setVisibility(View.GONE);
            updateSendButtonForAttachments();
        }
    }

    /**
     * Gets the display name of a file from its URI
     */
    private String getFileNameFromUri(Uri uri) {
        String fileName = "Unknown";
        
        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIndex != -1) {
                            fileName = cursor.getString(nameIndex);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
            
            // Fallback to path segment if display name not found
            if ("Unknown".equals(fileName)) {
                String lastSegment = uri.getLastPathSegment();
                if (lastSegment != null) {
                    fileName = lastSegment;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file name from URI", e);
        }
        
        return fileName;
    }

    /**
     * Updates the send button appearance based on whether attachments are selected
     */
    private void updateSendButtonForAttachments() {
        if (sendButton != null) {
            boolean hasAttachments = selectedAttachments != null && !selectedAttachments.isEmpty();
            if (hasAttachments) {
                // Change send button to indicate MMS mode
                sendButton.setAlpha(1.0f);
            } else {
                // Reset to normal SMS mode - check if we have text content
                updateSendButtonState();
            }
        }
    }

    private void translateMessageInput() {
        if (!isActivityActive || messageInput == null) {
            return;
        }

        // Get the current text from the input field
        String inputText = messageInput.getText().toString().trim();

        if (inputText.isEmpty()) {
            Toast.makeText(this, "Please enter text to translate", Toast.LENGTH_SHORT).show();
            return;
        }

        // If text is already translated, revert to original
        if (isComposedTextTranslated) {
            messageInput.setText(originalComposedText);
            isComposedTextTranslated = false;
            updateInputTranslationState();
            Toast.makeText(this, "Reverted to original text", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save original text
        originalComposedText = inputText;

        // Show translation status
        isTranslating.set(true);

        // Get user's preferred outgoing language
        String targetLanguage = userPreferences.getPreferredOutgoingLanguage();
        if (targetLanguage == null || targetLanguage.isEmpty()) {
            targetLanguage = userPreferences.getPreferredLanguage();
        }

        final String finalTargetLanguage = targetLanguage;

        // Use TranslationManager to translate the text with force translation for outgoing messages
        translationManager.translateText(inputText,
                targetLanguage,
                new TranslationManager.EnhancedTranslationCallback() {
                    @Override
                    public android.app.Activity getActivity() {
                        return NewMessageActivity.this;
                    }

                    @Override
                    public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                        isTranslating.set(false);

                        if (!success) {
                            runOnUiThread(() -> Toast.makeText(NewMessageActivity.this,
                                    "Translation error: " + (errorMessage != null ? errorMessage : "Unknown error"),
                                    Toast.LENGTH_SHORT).show());
                            return;
                        } else {// Use the translatedText parameter directly instead of retrieving from cache
                            if (translatedText == null) {
                                runOnUiThread(() -> Toast.makeText(NewMessageActivity.this,
                                        "Translation error: Could not retrieve translated text",
                                        Toast.LENGTH_SHORT).show());
                                return;
                            }

                            // Update UI on main thread
                            runOnUiThread(() -> {
                                // Update input field with translated text
                                messageInput.setText(translatedText);
                                messageInput.setSelection(translatedText.length());

                                // Mark as translated
                                isComposedTextTranslated = true;

                                // Update UI state
                                updateInputTranslationState();

                                // Show toast with language info
                                Toast.makeText(NewMessageActivity.this,
                                        "Translated to " + translationManager.getLanguageName(finalTargetLanguage),
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                }        ,true); // Force translation for outgoing messages
    }

    private void updateInputTranslationState() {
        if (messageInput == null || translateButton == null) {
            return;
        }

        if (isComposedTextTranslated) {
            // Visual indicator for translated text
            messageInput.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.colorAccent_blue)));
            translateButton.setImageResource(android.R.drawable.ic_menu_revert);
        } else {
            // Normal state
            messageInput.setBackgroundTintList(null); // Use default background
            translateButton.setImageResource(android.R.drawable.ic_menu_edit);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityActive = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityActive = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActivityActive = false;
        cleanupResources();
    }

    protected void cleanupResources() {
        try {
            // Stop any ongoing operations
            isTranslating.set(false);

            // Detach listeners - use the stored TextWatcher references
            if (recipientInput != null) {
                recipientInput.removeTextChangedListener(recipientTextWatcher);
                recipientInput = null;
            }

            if (messageInput != null) {
                messageInput.removeTextChangedListener(messageTextWatcher);
                messageInput = null;
            }

            // Clear TextWatcher references
            recipientTextWatcher = null;
            messageTextWatcher = null;

            if (sendButton != null) {
                sendButton.setOnClickListener(null);
                sendButton = null;
            }

            if (contactButton != null) {
                contactButton.setOnClickListener(null);
                contactButton = null;
            }

            if (translateButton != null) {
                translateButton.setOnClickListener(null);
                translateButton = null;
            }

            if (attachmentButton != null) {
                attachmentButton.setOnClickListener(null);
                attachmentButton = null;
            }
            
            if (removeAttachmentButton != null) {
                removeAttachmentButton.setOnClickListener(null);
                removeAttachmentButton = null;
            }

            // Clear references
            attachmentPreviewContainer = null;
            attachmentPreviewImage = null;

            // Unregister MMS send result receiver
            if (mmsSendResultReceiver != null) {
                try {
                    LocalBroadcastManager.getInstance(this).unregisterReceiver(mmsSendResultReceiver);
                    mmsSendResultReceiver = null;
                } catch (Exception e) {
                    Log.w(TAG, "Error unregistering MMS send result receiver", e);
                }
            }

            // Clear references
            messageService = null;
            translationManager = null;
            defaultSmsAppManager = null;
            userPreferences = null;

            // Force garbage collection
            System.gc();
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up resources", e);
        }
    }

    /**
     * Update toolbar colors based on current theme
     */
    private void updateToolbarTheme(Toolbar toolbar) {
        if (toolbar != null && userPreferences != null && userPreferences.isUsingBlackGlassTheme()) {
            // Use deep dark blue for Black Glass theme
            toolbar.setBackgroundColor(getResources().getColor(R.color.deep_dark_blue));
        }
        // Other themes will be handled by the theme system automatically
    }
    
    @Override
    protected void onThemeChanged() {
        super.onThemeChanged();
        
        // Update toolbar colors when theme changes
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            updateToolbarTheme(toolbar);
        }
        
        // Apply custom theme colors if using custom theme
        applyCustomButtonColors();
    }
    
    /**
     * Apply custom button colors if using custom theme
     */
    private void applyCustomButtonColors() {
        if (userPreferences != null && userPreferences.isUsingCustomTheme()) {
            int defaultColor = getResources().getColor(android.R.color.holo_blue_dark);
            int customButtonColor = userPreferences.getCustomButtonColor(defaultColor);
            
            // Apply custom color to send button
            if (sendButton != null) {
                sendButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customButtonColor));
            }
            
            // Apply custom color to contact button
            if (contactButton != null) {
                contactButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customButtonColor));
            }
            
            // Apply custom color to translate button
            if (translateButton != null) {
                translateButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customButtonColor));
            }
        }
    }
    
    @Override
    protected void applyCustomColorsToViews() {
        super.applyCustomColorsToViews();
        
        if (userPreferences != null && userPreferences.isUsingCustomTheme()) {
            // Apply custom colors to buttons
            int defaultColor = getResources().getColor(R.color.colorPrimary);
            int customButtonColor = userPreferences.getCustomButtonColor(defaultColor);
            
            if (sendButton != null) {
                sendButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customButtonColor));
            }
        }
    }

    /**
     * Shows GenAI features dialog for message composition.
     */



    /**
     * Validates that an attachment size is within MMS limits.
     * 
     * @param uri The attachment URI to validate
     * @return true if size is valid, false if too large
     */
    private boolean validateAttachmentSize(Uri uri) {
        try {
            long fileSize = getFileSizeFromUri(uri);
            
            // Check against MMS size limit (1MB)
            final long MAX_MMS_SIZE = 1024 * 1024; // 1MB - matches MessageService.MAX_MMS_SIZE
            
            if (fileSize > MAX_MMS_SIZE) {
                String fileName = getFileNameFromUri(uri);
                String fileSizeStr = String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
                
                Toast.makeText(this, 
                    "File too large for MMS: " + fileName + " (" + fileSizeStr + "). " +
                    "Maximum size is 1MB. Please choose a smaller file.", 
                    Toast.LENGTH_LONG).show();
                    
                Log.w(TAG, "Attachment too large: " + fileName + " - " + fileSize + " bytes (limit: " + MAX_MMS_SIZE + ")");
                return false;
            }
            
            Log.d(TAG, "Attachment size validation passed: " + fileSize + " bytes");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error validating attachment size for URI: " + uri, e);
            Toast.makeText(this, "Error checking file size. Please try a different file.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    
    /**
     * Gets the file size in bytes for a given URI.
     * 
     * @param uri The URI to get size for
     * @return The file size in bytes, or 0 if unable to determine
     */
    private long getFileSizeFromUri(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, 
                new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex >= 0) {
                    long size = cursor.getLong(sizeIndex);
                    if (size > 0) {
                        return size;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file size from cursor for URI: " + uri, e);
        }
        
        // Fallback: try to get size via input stream
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                long size = 0;
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    size += read;
                }
                return size;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file size from stream for URI: " + uri, e);
        }
        
        return 0; // Unable to determine size
    }

    /**
     * Copies text to clipboard.
     */
    private void copyToClipboard(String label, String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(label, text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error copying to clipboard", e);
            Toast.makeText(this, "Error copying to clipboard", Toast.LENGTH_SHORT).show();
        }
    }
}





