package com.translator.messagingapp;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageButton;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for creating a new message.
 * FIXED VERSION: Changed AutoCompleteTextView to EditText to match layout file
 */
public class NewMessageActivity extends BaseActivity {
    private static final String TAG = "NewMessageActivity";
    private static final int CONTACT_PICKER_RESULT = 1001;

    // UI components
    private EditText recipientInput; // Changed from AutoCompleteTextView to EditText
    private EditText messageInput;
    private Button sendButton;
    private ImageButton contactPickerButton; // Changed from Button to ImageButton

    // Services
    private MessageService messageService;
    private TranslationManager translationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_message);

        // Initialize services
        TranslatorApp app = (TranslatorApp) getApplication();
        if (app != null) {
            messageService = app.getMessageService();
            translationManager = app.getTranslationManager();
            
            // Verify services are initialized
            if (messageService == null) {
                Log.e(TAG, "MessageService is null");
                Toast.makeText(this, "Error: Message service not available", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            if (translationManager == null) {
                Log.e(TAG, "TranslationManager is null");
                Toast.makeText(this, "Error: Translation service not available", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            Log.e(TAG, "TranslatorApp is null");
            Toast.makeText(this, "Error: Application not initialized properly", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.new_message);
        }

        // Initialize UI components
        try {
            recipientInput = findViewById(R.id.recipient_input); // Now using EditText
            if (recipientInput == null) {
                throw new RuntimeException("recipient_input not found in layout");
            }
            
            messageInput = findViewById(R.id.message_input);
            if (messageInput == null) {
                throw new RuntimeException("message_input not found in layout");
            }
            
            sendButton = findViewById(R.id.send_button);
            if (sendButton == null) {
                throw new RuntimeException("send_button not found in layout");
            }
            
            contactPickerButton = findViewById(R.id.contact_button); // Now using ImageButton
            if (contactPickerButton == null) {
                throw new RuntimeException("contact_button not found in layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing UI components", e);
            Toast.makeText(this, "Error initializing UI: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set up contact picker button
        contactPickerButton.setOnClickListener(v -> {
            Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
        });

        // Set up send button
        sendButton.setOnClickListener(v -> sendMessage());

        // Enable/disable send button based on input
        TextWatcher textWatcher = new TextWatcher() {
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

        recipientInput.addTextChangedListener(textWatcher);
        messageInput.addTextChangedListener(textWatcher);

        // Initial button state
        updateSendButtonState();

        // Check for intent data (e.g., if opened from contacts)
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    /**
     * Handles the intent that started this activity.
     *
     * @param intent The intent
     */
    private void handleIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            String type = intent.getType();

            if (Intent.ACTION_SEND.equals(action) && type != null) {
                if ("text/plain".equals(type)) {
                    // Handle text being sent
                    String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                    if (!TextUtils.isEmpty(sharedText)) {
                        messageInput.setText(sharedText);
                    }
                }
            }

            // Check for address in extras
            String address = intent.getStringExtra("address");
            if (!TextUtils.isEmpty(address)) {
                recipientInput.setText(address);
                messageInput.requestFocus();
            }
        }
    }

    /**
     * Updates the state of the send button based on input validity.
     */
    private void updateSendButtonState() {
        boolean hasRecipient = !TextUtils.isEmpty(recipientInput.getText());
        boolean hasMessage = !TextUtils.isEmpty(messageInput.getText());
        sendButton.setEnabled(hasRecipient && hasMessage);
    }

    /**
     * Sends the message.
     */
    private void sendMessage() {
        // Get input
        String recipient = recipientInput.getText().toString().trim();
        String messageText = messageInput.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(recipient)) {
            Toast.makeText(this, R.string.error_no_recipient, Toast.LENGTH_SHORT).show();
            recipientInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(this, R.string.error_no_message, Toast.LENGTH_SHORT).show();
            messageInput.requestFocus();
            return;
        }

        // Format phone number
        try {
            recipient = PhoneUtils.formatPhoneNumber(recipient);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting phone number", e);
            // Continue with unformatted number
        }

        // Disable send button to prevent double-sending
        sendButton.setEnabled(false);

        try {
            // Send message using MessageService
            final String finalRecipient = recipient;
            messageService.sendSmsMessage(recipient, messageText, null, MessageService.MessageCallback.onSuccess(() -> {
                // Success callback

                // Set result and finish
                setResult(RESULT_OK);

                try {
                    // Open conversation with this recipient
                    Intent intent = new Intent(NewMessageActivity.this, ConversationActivity.class);
                    intent.putExtra("address", finalRecipient);
                    startActivity(intent);

                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "Error opening conversation", e);
                    Toast.makeText(NewMessageActivity.this, "Message sent, but error opening conversation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }));
        } catch (Exception e) {
            Log.e(TAG, "Error sending message", e);
            Toast.makeText(this, R.string.error_sending_message, Toast.LENGTH_SHORT).show();
            sendButton.setEnabled(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CONTACT_PICKER_RESULT && resultCode == RESULT_OK) {
            // Get contact data
            Uri contactUri = data.getData();
            if (contactUri != null) {
                Cursor cursor = null;
                try {
                    cursor = getContentResolver().query(contactUri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                        if (numberIndex != -1) {
                            String number = cursor.getString(numberIndex);
                            recipientInput.setText(number);
                            messageInput.requestFocus();
                        } else {
                            Log.e(TAG, "Phone number column not found");
                            Toast.makeText(this, "Could not get phone number", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error getting contact data", e);
                    Toast.makeText(this, "Error getting contact: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}




