package com.translator.messagingapp;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import java.util.concurrent.atomic.AtomicBoolean;
import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;

public class NewMessageActivity extends BaseActivity {
    private static final String TAG = "NewMessageActivity";
    private static final int CONTACT_PICKER_RESULT = 1001;

    private EditText recipientInput;
    private EditText messageInput;
    private Button sendButton;  // Changed from ImageButton to Button
    private ImageButton contactButton;
    private ImageButton translateButton;
    private final AtomicBoolean isTranslating = new AtomicBoolean(false);
    private String originalComposedText = "";
    private boolean isComposedTextTranslated = false;
    private TextWatcher recipientTextWatcher;
    private TextWatcher messageTextWatcher;
    protected boolean isActivityActive = true;

    // Service classes
    private MessageService messageService;
    private TranslationManager translationManager;
    private DefaultSmsAppManager defaultSmsAppManager;
    private UserPreferences userPreferences;

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

            // Set up toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("New Message");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            // Initialize UI components with correct types
            recipientInput = findViewById(R.id.recipient_input);
            messageInput = findViewById(R.id.message_input);
            sendButton = findViewById(R.id.send_button);  // This is a Button in XML
            contactButton = findViewById(R.id.contact_button);  // This is an ImageButton in XML
            translateButton = findViewById(R.id.translate_button);  // This is an ImageButton in XML

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

            // Set up translate button
            if (translateButton != null) {
                translateButton.setImageResource(android.R.drawable.ic_menu_edit);
                translateButton.setContentDescription("Translate message input");
                translateButton.setOnClickListener(v -> translateMessageInput());
                updateInputTranslationState();
            }

            // Set up text watchers for input validation
            setupTextWatchers();

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

        if (recipient.isEmpty() || messageText.isEmpty()) {
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
            // Send message using MessageService
            final String finalRecipient = recipient;
            messageService.sendSmsMessage(recipient, messageText, null, () -> {
                // Success callback

                // Set result and finish
                setResult(RESULT_OK);

                // Open conversation with this recipient
                Intent intent = new Intent(NewMessageActivity.this, ConversationActivity.class);
                intent.putExtra("address", finalRecipient);
                startActivity(intent);

                finish();
            });
        } catch (Exception e) {
            Log.e(TAG, "Error sending message", e);
            Toast.makeText(this, "Error sending message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                (success, translatedText, errorMessage) -> {
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
                }, true);
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
}





