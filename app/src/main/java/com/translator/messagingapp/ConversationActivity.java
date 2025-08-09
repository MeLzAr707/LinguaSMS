package com.translator.messagingapp;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Activity for displaying a conversation.
 * This version includes improved error handling and null checking.
 */
public class ConversationActivity extends BaseActivity implements MessageRecyclerAdapter.OnMessageClickListener {
    private static final String TAG = "ConversationActivity";
    private static final int EXECUTOR_SHUTDOWN_TIMEOUT = 5; // seconds

    // UI components
    private RecyclerView messagesRecyclerView;
    private MessageRecyclerAdapter adapter;
    private EditText messageInput;
    private Button sendButton;
    private ProgressBar progressBar;
    private TextView emptyStateTextView;
    private ImageButton translateInputButton;

    // Data
    private String threadId;
    private String address;
    private String contactName;
    private List<Message> messages;
    private ExecutorService executorService;

    // Service classes
    private MessageService messageService;
    private TranslationManager translationManager;
    private TranslationCache translationCache;
    private UserPreferences userPreferences;

    // Dialog management
    private AlertDialog progressDialog;
    private boolean isActivityDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Changed to use activity_conversation_updated.xml instead of activity_conversation.xml
        setContentView(R.layout.activity_conversation_updated);

        try {
            // Get service instances from TranslatorApp
            TranslatorApp app = (TranslatorApp) getApplication();
            if (app != null) {
                messageService = app.getMessageService();
                translationManager = app.getTranslationManager();
                translationCache = app.getTranslationCache();
            } else {
                Log.e(TAG, "Application is null");
                finish();
                return;
            }

            userPreferences = new UserPreferences(this);

            // Get thread ID and address from intent
            Intent intent = getIntent();
            if (intent != null) {
                threadId = intent.getStringExtra("thread_id");
                address = intent.getStringExtra("address");
                contactName = intent.getStringExtra("contact_name");
            }

            if (TextUtils.isEmpty(threadId) && TextUtils.isEmpty(address)) {
                Log.e(TAG, "No thread ID or address provided");
                Toast.makeText(this, "Missing conversation information", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Initialize executor service
            executorService = Executors.newCachedThreadPool();

            // Initialize data
            messages = new ArrayList<>();

            // Initialize UI components
            initializeComponents();

            // Load messages
            loadMessages();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing conversation", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Configure emoji support for the EditText
     */
    private void configureEmojiSupport(EditText editText) {
        try {
            // Set input type to support emoji
            editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT 
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE);
            
            // Set IME options to allow emoji input
            editText.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NONE);
            
            // Ensure the text field supports Unicode emoji characters
            editText.setKeyListener(android.text.method.TextKeyListener.getInstance());
        } catch (Exception e) {
            Log.e(TAG, "Error configuring emoji support: " + e.getMessage(), e);
        }
    }

    /**
     * Initialize UI components with improved error handling
     */
    private void initializeComponents() {
        try {
            // Set up toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setTitle(TextUtils.isEmpty(contactName) ? address : contactName);
                }
            }

            // Set up RecyclerView
            messagesRecyclerView = findViewById(R.id.messages_recycler_view);
            if (messagesRecyclerView != null) {
                LinearLayoutManager layoutManager = new LinearLayoutManager(this);
                layoutManager.setStackFromEnd(true);
                messagesRecyclerView.setLayoutManager(layoutManager);

                adapter = new MessageRecyclerAdapter(this, messages, this);
                messagesRecyclerView.setAdapter(adapter);
            } else {
                Log.e(TAG, "messages_recycler_view not found in layout");
            }

            // Set up progress bar
            progressBar = findViewById(R.id.progress_bar);

            // Set up empty state view
            emptyStateTextView = findViewById(R.id.empty_state_text_view);

            // Set up message input
            messageInput = findViewById(R.id.message_input);
            if (messageInput != null) {
                // Enable emoji support for the input field
                configureEmojiSupport(messageInput);
            }

            // Set up send button
            sendButton = findViewById(R.id.send_button);
            if (sendButton != null) {
                sendButton.setOnClickListener(v -> sendMessage());
            } else {
                Log.e(TAG, "send_button not found in layout");
            }

            // Set up translate input button
            translateInputButton = findViewById(R.id.translate_input_button);
            if (translateInputButton != null) {
                translateInputButton.setOnClickListener(v -> translateInputText());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing components: " + e.getMessage(), e);
        }
    }

    /**
     * Load messages with improved error handling and logging
     */
    private void loadMessages() {
        if (messageService == null) {
            Log.e(TAG, "MessageService is null");
            showEmptyState("Error: Message service unavailable");
            return;
        }

        if (isActivityDestroyed) {
            Log.d(TAG, "Activity destroyed, skipping message loading");
            return;
        }

        showLoading(true);

        executorService.execute(() -> {
            try {
                Log.d(TAG, "Loading messages - threadId: " + threadId + ", address: " + address);
                
                final List<Message> loadedMessages;

                if (!TextUtils.isEmpty(threadId)) {
                    Log.d(TAG, "Loading messages by thread ID: " + threadId);
                    loadedMessages = messageService.getMessagesByThreadId(threadId);
                } else if (!TextUtils.isEmpty(address)) {
                    Log.d(TAG, "Loading messages by address: " + address);
                    loadedMessages = messageService.getMessagesByAddress(address);
                } else {
                    Log.e(TAG, "Both threadId and address are empty");
                    loadedMessages = new ArrayList<>();
                }

                if (isActivityDestroyed) {
                    Log.d(TAG, "Activity destroyed during loading, not updating UI");
                    return;
                }

                runOnUiThread(() -> {
                    if (isActivityDestroyed) {
                        Log.d(TAG, "Activity destroyed before UI update");
                        return;
                    }

                    try {
                        showLoading(false);

                        if (loadedMessages != null && !loadedMessages.isEmpty()) {
                            Log.d(TAG, "Loaded " + loadedMessages.size() + " messages");
                            messages.clear();
                            messages.addAll(loadedMessages);
                            adapter.notifyDataSetChanged();
                            scrollToBottom();
                            showEmptyState(false);
                        } else {
                            Log.d(TAG, "No messages found");
                            showEmptyState(true);

                            // Add a test message if no messages are found and debug is enabled
                            if (userPreferences != null && userPreferences.isDebugModeEnabled()) {
                                Log.d(TAG, "Adding test message for debugging");
                                addTestMessage();
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating UI with loaded messages", e);
                        showEmptyState("Error displaying messages");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading messages: " + e.getMessage(), e);
                if (!isActivityDestroyed) {
                    runOnUiThread(() -> {
                        if (!isActivityDestroyed) {
                            showLoading(false);
                            showEmptyState("Error loading messages");
                            Toast.makeText(ConversationActivity.this, "Error loading messages", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    /**
     * Add a test message for debugging
     */
    private void addTestMessage() {
        try {
            Message testMessage = new Message();
            testMessage.setBody("This is a test message. You can translate this message to test the translation feature.");
            testMessage.setAddress(TextUtils.isEmpty(address) ? "Test Contact" : address);
            testMessage.setDate(System.currentTimeMillis());
            testMessage.setType(Message.TYPE_INBOX);

            messages.add(testMessage);
            adapter.notifyDataSetChanged();
            scrollToBottom();
            showEmptyState(false);
        } catch (Exception e) {
            Log.e(TAG, "Error adding test message: " + e.getMessage(), e);
        }
    }

    /**
     * Send a message with improved error handling
     */
    private void sendMessage() {
        if (messageInput == null || messageService == null) {
            Log.e(TAG, "MessageInput or MessageService is null");
            Toast.makeText(this, "Cannot send message", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageText = messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(address)) {
            Toast.makeText(this, "No recipient address", Toast.LENGTH_SHORT).show();
            return;
        }

        // Clear input
        messageInput.setText("");

        // Show loading
        showLoading(true);

        // Send message
        executorService.execute(() -> {
            try {
                final boolean success = messageService.sendSmsMessage(address, messageText);

                runOnUiThread(() -> {
                    showLoading(false);
                    if (success) {
                        // Reload messages to show the sent message
                        loadMessages();
                    } else {
                        Toast.makeText(ConversationActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error sending message: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(ConversationActivity.this, "Error sending message", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Translate input text with improved error handling
     */
    private void translateInputText() {
        if (messageInput == null || translationManager == null) {
            Log.e(TAG, "MessageInput or TranslationManager is null");
            Toast.makeText(this, "Cannot translate text", Toast.LENGTH_SHORT).show();
            return;
        }

        String text = messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "No text to translate", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        showProgressDialog("Translating...");

        // Get user's preferred language
        String targetLanguage = userPreferences.getPreferredOutgoingLanguage();
        if (TextUtils.isEmpty(targetLanguage)) {
            targetLanguage = userPreferences.getPreferredLanguage();
        }

        if (TextUtils.isEmpty(targetLanguage)) {
            hideProgressDialog();
            Toast.makeText(this, "No target language set in preferences", Toast.LENGTH_SHORT).show();
            return;
        }

        final String finalTargetLanguage = targetLanguage;

        // Translate text
        translationManager.translateText(text, finalTargetLanguage, new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                runOnUiThread(() -> {
                    hideProgressDialog();

                    if (success && !TextUtils.isEmpty(translatedText)) {
                        messageInput.setText(translatedText);
                        messageInput.setSelection(translatedText.length());
                    } else {
                        Toast.makeText(ConversationActivity.this,
                                "Translation failed: " + (errorMessage != null ? errorMessage : "Unknown error"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }



    /**
     * Show or hide loading indicator
     */
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Show or hide empty state
     */
    private void showEmptyState(boolean show) {
        if (emptyStateTextView != null) {
            emptyStateTextView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Show empty state with custom message
     */
    private void showEmptyState(String message) {
        if (emptyStateTextView != null) {
            emptyStateTextView.setText(message);
            emptyStateTextView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Scroll to bottom of messages
     */
    private void scrollToBottom() {
        if (messagesRecyclerView != null && adapter != null && adapter.getItemCount() > 0) {
            messagesRecyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
        }
    }

    /**
     * Show progress dialog
     */
    private void showProgressDialog(String message) {
        if (isActivityDestroyed) return;

        try {
            hideProgressDialog();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = getLayoutInflater().inflate(R.layout.dialog_progress, null);
            TextView messageView = view.findViewById(R.id.progress_message);

            if (messageView != null) {
                messageView.setText(message);
            }

            builder.setView(view);
            builder.setCancelable(false);

            progressDialog = builder.create();
            progressDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing progress dialog: " + e.getMessage(), e);
        }
    }

    /**
     * Hide progress dialog
     */
    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing() && !isActivityDestroyed) {
            try {
                progressDialog.dismiss();
            } catch (Exception e) {
                Log.e(TAG, "Error dismissing progress dialog: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onMessageClick(Message message, int position) {
        // Handle message click
    }

    @Override
    public void onMessageLongClick(Message message, int position) {
        // Show message options
        showMessageOptions(message);
    }

    @Override
    public void onTranslateClick(Message message, int position) {
        if (message == null || translationManager == null) {
            Log.e(TAG, "Message or TranslationManager is null");
            Toast.makeText(this, "Cannot translate message", Toast.LENGTH_SHORT).show();
            return;
        }

        // If already translated, restore original text
        if (message.isTranslated()) {
            message.setTranslated(false);
            adapter.notifyItemChanged(position);
            return;
        }

        // Show loading
        showProgressDialog("Translating message...");

        // Get user's preferred language
        String targetLanguage = message.isIncoming() ?
                userPreferences.getPreferredIncomingLanguage() :
                userPreferences.getPreferredOutgoingLanguage();

        if (TextUtils.isEmpty(targetLanguage)) {
            targetLanguage = userPreferences.getPreferredLanguage();
        }

        if (TextUtils.isEmpty(targetLanguage)) {
            hideProgressDialog();
            Toast.makeText(this, "No target language set in preferences", Toast.LENGTH_SHORT).show();
            return;
        }

        final String finalTargetLanguage = targetLanguage;

        // Translate message
        translationManager.translateMessage(message, new TranslationManager.TranslationCallback() {
            @Override
            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                runOnUiThread(() -> {
                    hideProgressDialog();

                    if (success && !TextUtils.isEmpty(translatedText)) {
                        message.setTranslated(true);
                        adapter.notifyItemChanged(position);
                    } else {
                        Toast.makeText(ConversationActivity.this,
                                "Translation failed: " + (errorMessage != null ? errorMessage : "Unknown error"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public void onAttachmentClick(MmsMessage.Attachment attachment, int position) {
        // Handle attachment click
        if (attachment == null) return;

        // Open attachment based on type
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(attachment.getUri(), attachment.getContentType());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening attachment: " + e.getMessage(), e);
            Toast.makeText(this, "Cannot open attachment", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onReactionClick(Message message, int position) {
        // Handle reaction click
    }

    @Override
    public void onAddReactionClick(Message message, int position) {
        // Show emoji picker for reactions
        try {
            EmojiPickerDialog dialog = new EmojiPickerDialog(this, emoji -> {
                // Add reaction to message
                if (message != null) {
                    // Add reaction logic here
                    Toast.makeText(this, "Added reaction: " + emoji, Toast.LENGTH_SHORT).show();
                    adapter.notifyItemChanged(position);
                }
            }, false);
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing emoji picker: " + e.getMessage(), e);
            Toast.makeText(this, "Error showing emoji picker", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show message options
     */
    private void showMessageOptions(Message message) {
        if (message == null) return;

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setItems(new String[]{"Copy", "Delete", "Forward"}, (dialog, which) -> {
                switch (which) {
                    case 0: // Copy
                        copyMessageToClipboard(message);
                        break;
                    case 1: // Delete
                        deleteMessage(message);
                        break;
                    case 2: // Forward
                        forwardMessage(message);
                        break;
                }
            });
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing message options: " + e.getMessage(), e);
        }
    }

    /**
     * Copy message text to clipboard
     */
    private void copyMessageToClipboard(Message message) {
        if (message == null) return;

        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("Message", message.getBody());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Message copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error copying to clipboard: " + e.getMessage(), e);
            Toast.makeText(this, "Error copying message", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Delete message
     */
    private void deleteMessage(Message message) {
        if (message == null || messageService == null) return;

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Delete Message");
            builder.setMessage("Are you sure you want to delete this message?");
            builder.setPositiveButton("Delete", (dialog, which) -> {
                showLoading(true);

                executorService.execute(() -> {
                    try {
                        final boolean success = messageService.deleteMessage(String.valueOf(message.getId()));

                        runOnUiThread(() -> {
                            showLoading(false);

                            if (success) {
                                int position = messages.indexOf(message);
                                if (position >= 0) {
                                    messages.remove(position);
                                    adapter.notifyItemRemoved(position);

                                    if (messages.isEmpty()) {
                                        showEmptyState(true);
                                    }
                                }
                                Toast.makeText(ConversationActivity.this, "Message deleted", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ConversationActivity.this, "Failed to delete message", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error deleting message: " + e.getMessage(), e);
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(ConversationActivity.this, "Error deleting message", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing delete dialog: " + e.getMessage(), e);
        }
    }

    /**
     * Forward message
     */
    private void forwardMessage(Message message) {
        if (message == null) return;

        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(android.net.Uri.parse("smsto:"));
            intent.putExtra("sms_body", message.getBody());
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error forwarding message: " + e.getMessage(), e);
            Toast.makeText(this, "Error forwarding message", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        isActivityDestroyed = true;
        hideProgressDialog();

        // Cleanup executor service properly
        if (executorService != null && !executorService.isShutdown()) {
            try {
                Log.d(TAG, "Shutting down executor service");
                executorService.shutdown();
                
                // Wait for tasks to complete or timeout
                if (!executorService.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS)) {
                    Log.w(TAG, "Executor service did not terminate, forcing shutdown");
                    executorService.shutdownNow();
                    
                    // Wait a bit more for forceful shutdown
                    if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                        Log.e(TAG, "Executor service did not terminate after force shutdown");
                    }
                }
            } catch (InterruptedException e) {
                Log.w(TAG, "Interrupted while shutting down executor service");
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        try {
            // Cancel any ongoing operations
            if (progressDialog != null && progressDialog.isShowing()) {
                hideProgressDialog();
            }
            
            // Finish activity safely
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error handling back press", e);
            // Force finish even if there's an error
            try {
                super.onBackPressed();
            } catch (Exception ex) {
                Log.e(TAG, "Error in super.onBackPressed()", ex);
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.conversation_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_call) {
            // Call contact
            if (!TextUtils.isEmpty(address)) {
                try {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(android.net.Uri.parse("tel:" + address));
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error making call: " + e.getMessage(), e);
                    Toast.makeText(this, "Error making call", Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        } else if (id == R.id.action_translate_all) {
            // Translate all messages
            translateAllMessages();
            return true;
        } else if (id == R.id.action_delete_conversation) {
            // Delete conversation
            deleteConversation();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Translate all messages
     */
    private void translateAllMessages() {
        if (messages == null || messages.isEmpty() || translationManager == null) {
            Toast.makeText(this, "No messages to translate", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        showProgressDialog("Translating all messages...");

        // Get user's preferred language
        String targetLanguage = userPreferences.getPreferredLanguage();
        if (TextUtils.isEmpty(targetLanguage)) {
            hideProgressDialog();
            Toast.makeText(this, "No target language set in preferences", Toast.LENGTH_SHORT).show();
            return;
        }

        final String finalTargetLanguage = targetLanguage;
        final List<Integer> translatedPositions = new ArrayList<>();
        final int[] processedCount = {0};
        final int totalMessages = messages.size();

        // Translate each message
        for (int i = 0; i < messages.size(); i++) {
            final int position = i;
            Message message = messages.get(i);

            if (message == null || !message.isTranslatable() || message.isTranslated()) {
                processedCount[0]++;
                if (processedCount[0] >= totalMessages) {
                    runOnUiThread(() -> {
                        hideProgressDialog();
                        Toast.makeText(ConversationActivity.this,
                                "Translated " + translatedPositions.size() + " messages",
                                Toast.LENGTH_SHORT).show();

                        // Update UI for translated messages
                        for (int pos : translatedPositions) {
                            adapter.notifyItemChanged(pos);
                        }
                    });
                }
                continue;
            }

            translationManager.translateMessage(message, new TranslationManager.TranslationCallback() {
                @Override
                public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                    processedCount[0]++;

                    if (success && !TextUtils.isEmpty(translatedText)) {
                        message.setTranslated(true);
                        translatedPositions.add(position);
                    }

                    // Check if all messages have been processed
                    if (processedCount[0] >= totalMessages) {
                        runOnUiThread(() -> {
                            hideProgressDialog();
                            Toast.makeText(ConversationActivity.this,
                                    "Translated " + translatedPositions.size() + " messages",
                                    Toast.LENGTH_SHORT).show();

                            // Update UI for translated messages
                            for (int pos : translatedPositions) {
                                adapter.notifyItemChanged(pos);
                            }
                        });
                    }
                }
            });
        }
    }

    /**
     * Delete conversation
     */
    private void deleteConversation() {
        if (TextUtils.isEmpty(threadId) && TextUtils.isEmpty(address)) {
            Toast.makeText(this, "Cannot identify conversation", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Delete Conversation");
            builder.setMessage("Are you sure you want to delete this entire conversation?");
            builder.setPositiveButton("Delete", (dialog, which) -> {
                showLoading(true);

                executorService.execute(() -> {
                    try {
                        final boolean success;

                        if (!TextUtils.isEmpty(threadId)) {
                            success = messageService.deleteConversation(threadId);
                        } else {
                            success = messageService.deleteConversationByAddress(address);
                        }

                        runOnUiThread(() -> {
                            showLoading(false);

                            if (success) {
                                Toast.makeText(ConversationActivity.this, "Conversation deleted", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(ConversationActivity.this, "Failed to delete conversation", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error deleting conversation: " + e.getMessage(), e);
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(ConversationActivity.this, "Error deleting conversation", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing delete dialog: " + e.getMessage(), e);
        }
    }
}



