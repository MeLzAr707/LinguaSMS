package com.translator.messagingapp;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for displaying a conversation.
 */
public class ConversationActivity extends BaseActivity implements MessageRecyclerAdapter.OnMessageClickListener {
    private static final String TAG = "ConversationActivity";

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
    
    // Pagination variables
    private static final int PAGE_SIZE = 50;
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMoreMessages = true;

    // Service classes
    private MessageService messageService;
    private TranslationManager translationManager;
    private TranslationCache translationCache;
    private UserPreferences userPreferences;
    
    // BroadcastReceiver for message updates
    private BroadcastReceiver messageUpdateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_updated);

        // Get service instances from TranslatorApp
        messageService = ((TranslatorApp) getApplication()).getMessageService();
        translationManager = ((TranslatorApp) getApplication()).getTranslationManager();
        translationCache = ((TranslatorApp) getApplication()).getTranslationCache();
        userPreferences = new UserPreferences(this);

        // Get thread ID and address from intent
        threadId = getIntent().getStringExtra("thread_id");
        address = getIntent().getStringExtra("address");
        contactName = getIntent().getStringExtra("contact_name");

        if (TextUtils.isEmpty(threadId) && TextUtils.isEmpty(address)) {
            Log.e(TAG, "No thread ID or address provided");
            finish();
            return;
        }

        // Initialize executor service
        executorService = Executors.newCachedThreadPool();

        // Initialize data
        messages = new ArrayList<>();

        // Initialize UI components
        initializeComponents();
        
        // Set up message update receiver
        setupMessageUpdateReceiver();

        // Load messages
        loadMessages();
    }

    private void initializeComponents() {
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(contactName != null ? contactName : address);
        }
        
        // Apply theme-specific toolbar styling
        updateToolbarTheme(toolbar);

        // Find views
        messagesRecyclerView = findViewById(R.id.messages_recycler_view);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        progressBar = findViewById(R.id.progress_bar);
        emptyStateTextView = findViewById(R.id.empty_state_text_view);
        translateInputButton = findViewById(R.id.translate_outgoing_button);

        // Check for critical views to prevent null pointer exceptions
        if (messagesRecyclerView == null) {
            Log.e(TAG, "messagesRecyclerView not found in layout");
            finish();
            return;
        }

        // Set up RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesRecyclerView.setLayoutManager(layoutManager);
        
        // Try to use the updated adapter class if available
        try {
            // Use reflection to check if the updated adapter class exists
            Class<?> updatedAdapterClass = Class.forName("com.translator.messagingapp.MessageRecyclerAdapter_updated");
            if (updatedAdapterClass != null) {
                // Use the updated adapter
                adapter = new MessageRecyclerAdapter(this, messages, this);
            }
        } catch (ClassNotFoundException e) {
            // Use the regular adapter
            adapter = new MessageRecyclerAdapter(this, messages, this);
        }
        
        messagesRecyclerView.setAdapter(adapter);

        // Set up send button
        if (sendButton != null) {
            sendButton.setOnClickListener(v -> sendMessage());
        }

        // Set up translate input button
        if (translateInputButton != null) {
            translateInputButton.setOnClickListener(v -> translateInput());
        }

        // Update UI based on theme
        updateUIForTheme();
    }

    private void updateUIForTheme() {
        // Apply theme-specific styling
        boolean isDarkTheme = userPreferences.isDarkThemeEnabled();

        if (isDarkTheme) {
            // Apply dark theme styling
            if (messageInput != null) {
                messageInput.setBackgroundResource(R.drawable.message_input_background_dark);
                messageInput.setTextColor(getResources().getColor(R.color.textColorPrimaryDark));
                messageInput.setHintTextColor(getResources().getColor(R.color.textColorSecondaryDark));
            }

            // Apply additional dark theme styling as needed
        } else {
            // Apply light theme styling
            if (messageInput != null) {
                messageInput.setBackgroundResource(R.drawable.message_input_background_light);
                messageInput.setTextColor(getResources().getColor(R.color.textColorPrimary));
                messageInput.setHintTextColor(getResources().getColor(R.color.textColorSecondary));
            }

            // Apply additional light theme styling as needed
        }
    }

    private void loadMessages() {
        // Check if messageService is available
        if (messageService == null) {
            Log.e(TAG, "MessageService is null, cannot load messages");
            runOnUiThread(() -> {
                hideLoadingIndicator();
                emptyStateTextView.setText("Service unavailable");
                emptyStateTextView.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Message service unavailable", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        // Check if threadId is valid
        if (TextUtils.isEmpty(threadId)) {
            Log.e(TAG, "Thread ID is empty, cannot load messages");
            runOnUiThread(() -> {
                hideLoadingIndicator();
                emptyStateTextView.setText("Invalid conversation");
                emptyStateTextView.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Invalid conversation thread", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        // Show loading indicator
        showLoadingIndicator();

        // Use a background thread to load messages
        executorService.execute(() -> {
            try {
                // First, check cache for existing messages
                Log.d(TAG, "Checking cache for thread ID: " + threadId);
                List<Message> cachedMessages = MessageCache.getCachedMessages(threadId);
                
                final List<Message> loadedMessages;
                if (cachedMessages != null && !cachedMessages.isEmpty()) {
                    Log.d(TAG, "Found " + cachedMessages.size() + " cached messages");
                    loadedMessages = cachedMessages;
                    
                    // For cached messages, we might already have multiple pages
                    // Set hasMoreMessages based on cache size
                    hasMoreMessages = cachedMessages.size() >= PAGE_SIZE;
                } else {
                    // Load first page of messages from MessageService
                    Log.d(TAG, "Loading first page of messages for thread ID: " + threadId);
                    loadedMessages = loadMessagesPage(0, PAGE_SIZE);
                    
                    // Sort messages chronologically (oldest first) for display
                    if (loadedMessages != null && !loadedMessages.isEmpty()) {
                        Collections.sort(loadedMessages, (m1, m2) -> Long.compare(m1.getDate(), m2.getDate()));
                        
                        // Cache the loaded messages
                        MessageCache.cacheMessages(threadId, new ArrayList<>(loadedMessages));
                    }
                }
                
                Log.d(TAG, "Loaded " + (loadedMessages != null ? loadedMessages.size() : 0) + " messages");

                // Update UI on main thread
                runOnUiThread(() -> {
                    // Clear existing messages and add loaded ones
                    messages.clear();
                    if (loadedMessages != null && !loadedMessages.isEmpty()) {
                        messages.addAll(loadedMessages);
                        Log.d(TAG, "Added " + loadedMessages.size() + " messages to UI list");
                        
                        // Set up pagination
                        setupPagination();
                        
                        // Check if we have more messages to load
                        hasMoreMessages = loadedMessages.size() >= PAGE_SIZE;
                    } else {
                        Log.d(TAG, "No messages to add to UI list");
                        hasMoreMessages = false;
                    }

                    // Update UI
                    adapter.notifyDataSetChanged();
                    hideLoadingIndicator();

                    // Scroll to bottom
                    if (!messages.isEmpty()) {
                        messagesRecyclerView.scrollToPosition(messages.size() - 1);
                    }

                    // Show empty state if no messages
                    if (messages.isEmpty()) {
                        Log.d(TAG, "Displaying empty state - no messages found");
                        emptyStateTextView.setText(R.string.no_messages);
                        emptyStateTextView.setVisibility(View.VISIBLE);
                    } else {
                        Log.d(TAG, "Hiding empty state - " + messages.size() + " messages displayed");
                        emptyStateTextView.setVisibility(View.GONE);
                    }

                    // Mark thread as read
                    markThreadAsRead();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading messages for thread " + threadId, e);
                runOnUiThread(() -> {
                    Toast.makeText(ConversationActivity.this,
                            getString(R.string.error_loading_messages) + ": " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    hideLoadingIndicator();
                    emptyStateTextView.setText(R.string.error_loading_messages);
                    emptyStateTextView.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void markThreadAsRead() {
        executorService.execute(() -> {
            try {
                messageService.markThreadAsRead(threadId);
            } catch (Exception e) {
                Log.e(TAG, "Error marking thread as read", e);
            }
        });
    }

    /**
     * Loads a specific page of messages using true database pagination
     */
    private List<Message> loadMessagesPage(int page, int pageSize) {
        try {
            int offset = page * pageSize;
            return messageService.loadMessagesPaginated(threadId, offset, pageSize);
        } catch (Exception e) {
            Log.e(TAG, "Error loading messages page " + page, e);
            return new ArrayList<>();
        }
    }

    /**
     * Sets up pagination for the RecyclerView
     */
    private void setupPagination() {
        messagesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;
                
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                
                // Load more when scrolling up (towards older messages)
                if (!isLoading && hasMoreMessages && firstVisibleItemPosition == 0 && dy < 0) {
                    loadMoreMessages();
                }
            }
        });
    }

    /**
     * Loads more messages (older messages) when scrolling up
     */
    private void loadMoreMessages() {
        if (isLoading || !hasMoreMessages) {
            return;
        }
        
        isLoading = true;
        currentPage++;
        
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Loading more messages - page " + currentPage);
                List<Message> newMessages = loadMessagesPage(currentPage, PAGE_SIZE);
                
                runOnUiThread(() -> {
                    if (newMessages.isEmpty()) {
                        hasMoreMessages = false;
                        Log.d(TAG, "No more messages to load");
                    } else {
                        // Sort new messages chronologically (oldest first) for consistency
                        Collections.sort(newMessages, (m1, m2) -> Long.compare(m1.getDate(), m2.getDate()));
                        
                        // Insert at the beginning (older messages)
                        messages.addAll(0, newMessages);
                        adapter.notifyItemRangeInserted(0, newMessages.size());
                        
                        // Maintain scroll position
                        LinearLayoutManager layoutManager = (LinearLayoutManager) messagesRecyclerView.getLayoutManager();
                        if (layoutManager != null) {
                            layoutManager.scrollToPositionWithOffset(newMessages.size(), 0);
                        }
                        
                        Log.d(TAG, "Added " + newMessages.size() + " older messages");
                    }
                    isLoading = false;
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading more messages", e);
                runOnUiThread(() -> {
                    isLoading = false;
                    Toast.makeText(ConversationActivity.this, "Error loading more messages", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }

        // Clear input
        messageInput.setText("");

        // Show progress
        showLoadingIndicator();

        // Send message in background
        executorService.execute(() -> {
            try {
                boolean success = messageService.sendSmsMessage(address, messageText);

                runOnUiThread(() -> {
                    hideLoadingIndicator();

                    if (success) {
                        // Note: Message refresh will be handled by broadcast receiver
                        // when MESSAGE_SENT broadcast is received from MessageService
                        Log.d(TAG, "Message sent successfully, waiting for broadcast to refresh UI");
                    } else {
                        Toast.makeText(ConversationActivity.this,
                                R.string.error_sending_message,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error sending message", e);
                runOnUiThread(() -> {
                    hideLoadingIndicator();
                    Toast.makeText(ConversationActivity.this,
                            getString(R.string.error_sending_message) + ": " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showLoadingIndicator() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideLoadingIndicator() {
        progressBar.setVisibility(View.GONE);
    }

    /**
     * Set up broadcast receiver for message update events.
     * Uses proper Android 13+ RECEIVER_EXPORTED/RECEIVER_NOT_EXPORTED flags.
     */
    private void setupMessageUpdateReceiver() {
        try {
            // Create the broadcast receiver
            messageUpdateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "Message update broadcast received: " + intent.getAction());
                    
                    // Handle different update actions
                    if (intent != null && intent.getAction() != null) {
                        switch (intent.getAction()) {
                            case "com.translator.messagingapp.MESSAGE_RECEIVED":
                            case "com.translator.messagingapp.REFRESH_MESSAGES":
                                // Refresh messages when received or refresh requested
                                Log.d(TAG, "Refreshing messages due to broadcast: " + intent.getAction());
                                loadMessages();
                                break;
                            case "com.translator.messagingapp.MESSAGE_SENT":
                                // Handle sent message: clear cache, reset pagination, then refresh
                                Log.d(TAG, "Message sent broadcast received, clearing cache and refreshing");
                                
                                // Clear cache to ensure fresh data
                                MessageCache.clearCacheForThread(threadId);
                                
                                // Reset pagination state
                                currentPage = 0;
                                hasMoreMessages = true;
                                
                                // Refresh messages
                                loadMessages();
                                break;
                            default:
                                Log.d(TAG, "Unknown update action: " + intent.getAction());
                                break;
                        }
                    }
                }
            };

            // Create intent filter for message update events
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.translator.messagingapp.MESSAGE_RECEIVED");
            filter.addAction("com.translator.messagingapp.REFRESH_MESSAGES");
            filter.addAction("com.translator.messagingapp.MESSAGE_SENT");

            // Register receiver with proper Android 13+ flags
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ requires explicit RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED
                registerReceiver(messageUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                // Pre-Android 13 registration
                registerReceiver(messageUpdateReceiver, filter);
            }
            
            Log.d(TAG, "Message update receiver registered successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up message update receiver", e);
            // Don't throw the exception to prevent app crash
        }
    }

    private void showProgressDialog(String message) {
        // Show a progress dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_progress, null);
        TextView messageTextView = dialogView.findViewById(R.id.progress_message);
        messageTextView.setText(message);
        builder.setView(dialogView);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Store the dialog in a tag
        messageInput.setTag(dialog);
    }

    private void hideProgressDialog() {
        // Hide the progress dialog
        if (messageInput.getTag() instanceof AlertDialog) {
            AlertDialog dialog = (AlertDialog) messageInput.getTag();
            dialog.dismiss();
            messageInput.setTag(null);
        }
    }

    private void translateInput() {
        String inputText = messageInput.getText().toString().trim();
        if (inputText.isEmpty()) {
            Toast.makeText(this, "Please enter text to translate", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show translation in progress
        showProgressDialog("Translating...");

        // Get target language
        String targetLanguage = userPreferences.getPreferredLanguage();

        // Translate in background
        executorService.execute(() -> {
            translationManager.translateText(inputText, targetLanguage, (success, translatedText, errorMessage) -> {
                runOnUiThread(() -> {
                    hideProgressDialog();

                    if (success && translatedText != null) {
                        // Replace input text with translated text
                        messageInput.setText(translatedText);
                        messageInput.setSelection(translatedText.length());
                    } else {
                        Toast.makeText(ConversationActivity.this,
                                getString(R.string.translation_error) + ": " +
                                        (errorMessage != null ? errorMessage : getString(R.string.unknown_error)),
                                Toast.LENGTH_LONG).show();
                    }
                });
            });
        });
    }

    private void translateMessage(Message message, int position) {
        if (message == null || message.getBody() == null) {
            return;
        }

        // Skip if already translated
        if (message.isTranslated() && message.getTranslatedText() != null) {
            message.setShowTranslation(true);
            adapter.notifyItemChanged(position);
            return;
        }

        // Get target language
        String targetLanguage = userPreferences.getPreferredLanguage();

        // Show translation in progress
        showLoadingIndicator();

        // Translate in background
        executorService.execute(() -> {
            translationManager.translateText(message.getBody(), targetLanguage, (success, translatedText, errorMessage) -> {
                runOnUiThread(() -> {
                    hideLoadingIndicator();

                    if (success && translatedText != null) {
                        // Update message with translated text
                        message.setTranslatedText(translatedText);
                        message.setTranslated(true);
                        message.setShowTranslation(true);

                        // Save to cache
                        if (translationCache != null) {
                            // Use the put method instead of saveTranslation
                            String cacheKey = "msg_" + message.getId() + "_translation";
                            translationCache.put(cacheKey, translatedText);
                        }

                        // Update UI
                        adapter.notifyItemChanged(position);
                    } else {
                        Toast.makeText(ConversationActivity.this,
                                getString(R.string.translation_error) + ": " +
                                        (errorMessage != null ? errorMessage : getString(R.string.unknown_error)),
                                Toast.LENGTH_LONG).show();
                    }
                });
            });
        });
    }

    private void toggleMessageTranslation(Message message, int position) {
        if (message.isTranslated()) {
            // Toggle between original and translated text
            message.setShowTranslation(!message.isShowTranslation());
            adapter.notifyItemChanged(position);
        } else {
            // Translate the message
            translateMessage(message, position);
        }
    }

    /**
     * Shows the reaction picker dialog for a message.
     * 
     * @param message The message to react to
     * @param position The position of the message in the list
     */
    private void showReactionPicker(Message message, int position) {
        EmojiPickerDialog.show(this, emoji -> {
            // Add the reaction to the message
            String userId = "self"; // In a real app, this would be the user's ID
            boolean added = message.addReaction(emoji, userId);
            
            if (added) {
                // Update the UI
                adapter.notifyItemChanged(position);
                Toast.makeText(this, R.string.reaction_added, Toast.LENGTH_SHORT).show();
            } else {
                // User already reacted with this emoji
                Toast.makeText(this, R.string.reaction_removed, Toast.LENGTH_SHORT).show();
                
                // Remove the reaction
                message.removeReaction(emoji, userId);
                adapter.notifyItemChanged(position);
            }
        }, true); // true indicates this is for reactions
    }

    private void showDeleteMessageConfirmationDialog(Message message, int position) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_message_title)
                .setMessage(R.string.delete_message_confirmation)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    deleteMessage(message, position);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteMessage(Message message, int position) {
        // This is a simplified implementation
        // In a real app, you would delete the message from the content provider
        showLoadingIndicator();

        executorService.execute(() -> {
            try {
                // Simulate deletion
                Thread.sleep(500);

                runOnUiThread(() -> {
                    hideLoadingIndicator();

                    // Remove from list and update UI
                    messages.remove(position);
                    adapter.notifyDataSetChanged();

                    Toast.makeText(ConversationActivity.this, R.string.message_deleted, Toast.LENGTH_SHORT).show();

                    // Show empty state if no messages left
                    if (messages.isEmpty()) {
                        emptyStateTextView.setText(R.string.no_messages);
                        emptyStateTextView.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error deleting message", e);
                runOnUiThread(() -> {
                    hideLoadingIndicator();
                    Toast.makeText(ConversationActivity.this,
                            getString(R.string.error_deleting_message) + ": " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void copyMessageToClipboard(Message message) {
        try {
            String textToCopy = message.isShowTranslation() && message.isTranslated() ?
                    message.getTranslatedText() : message.getBody();

            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip =
                    ClipData.newPlainText("Message", textToCopy);

            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error copying to clipboard", e);
            Toast.makeText(this, R.string.error_copying_to_clipboard, Toast.LENGTH_SHORT).show();
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
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMessageClick(Message message, int position) {
        // Do nothing on click
    }

    @Override
    public void onMessageLongClick(Message message, int position) {
        // Show message options
        showMessageOptionsDialog(message, position);
    }

    @Override
    public void onTranslateClick(Message message, int position) {
        // Translate the message
        translateMessage(message, position);
    }

    @Override
    public void onAttachmentClick(MmsMessage.Attachment attachment, int position) {
        // Handle attachment click
        Toast.makeText(this, "Attachment clicked", Toast.LENGTH_SHORT).show();
    }
    
    // Add the missing method to fix the compilation error
    @Override
    public void onAttachmentClick(Uri uri, int position) {
        // Handle attachment click for URI
        Toast.makeText(this, "Attachment clicked: " + uri.toString(), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onReactionClick(Message message, int position) {
        // Show reaction details or toggle user's reaction
        showReactionPicker(message, position);
    }
    
    @Override
    public void onAddReactionClick(Message message, int position) {
        // Show reaction picker
        showReactionPicker(message, position);
    }

    private void showMessageOptionsDialog(Message message, int position) {
        String[] options;

        if (message.isTranslated()) {
            options = new String[]{"Add Reaction", "Copy message", "Show original", "Translate again", "Delete message"};
        } else {
            options = new String[]{"Add Reaction", "Copy message", "Translate", "Delete message"};
        }

        new AlertDialog.Builder(this)
                .setTitle("Message Options")
                .setItems(options, (dialog, which) -> {
                    if (options[which].equals("Add Reaction")) {
                        showReactionPicker(message, position);
                    } else if (options[which].equals("Copy message")) {
                        copyMessageToClipboard(message);
                    } else if (options[which].equals("Show original") || options[which].equals("Translate")) {
                        toggleMessageTranslation(message, position);
                    } else if (options[which].equals("Translate again")) {
                        translateMessage(message, position);
                    } else if (options[which].equals("Delete message")) {
                        showDeleteMessageConfirmationDialog(message, position);
                    }
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Unregister message update receiver
        if (messageUpdateReceiver != null) {
            try {
                unregisterReceiver(messageUpdateReceiver);
                Log.d(TAG, "Message update receiver unregistered");
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering message update receiver", e);
            }
        }
        
        // Clean up resources
        if (executorService != null) {
            executorService.shutdownNow();
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
    }
}