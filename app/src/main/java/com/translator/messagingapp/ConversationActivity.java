package com.translator.messagingapp;

import android.annotation.SuppressLint;
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
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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
    
    // Static field to track currently active conversation for notification suppression
    private static String currentlyActiveThreadId = null;
    
    /**
     * Checks if a specific thread is currently being viewed.
     * This is used to suppress notifications for the active conversation.
     * 
     * @param threadId The thread ID to check
     * @return true if the thread is currently active, false otherwise
     */
    public static boolean isThreadCurrentlyActive(String threadId) {
        return threadId != null && threadId.equals(currentlyActiveThreadId);
    }

    // UI components
    private RecyclerView messagesRecyclerView;
    private MessageRecyclerAdapter adapter;
    private EditText messageInput;
    private Button sendButton;
    private ProgressBar progressBar;
    private TextView emptyStateTextView;
    private ImageButton translateInputButton;
    private ImageButton emojiButton;
    private ImageButton attachmentButton;
    private ImageButton translateButton;

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
    private TextSizeManager textSizeManager;
    
    // Gesture detection for pinch-to-zoom
    private ScaleGestureDetector scaleGestureDetector;
    
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
        textSizeManager = new TextSizeManager(this);

        // Get thread ID and address from intent
        threadId = getIntent().getStringExtra("thread_id");
        address = getIntent().getStringExtra("address");
        contactName = getIntent().getStringExtra("contact_name");

        if (TextUtils.isEmpty(threadId) && TextUtils.isEmpty(address)) {
            Log.e(TAG, "No thread ID or address provided");
            finish();
            return;
        }

        // If we have an address but no thread ID, try to resolve it
        if (TextUtils.isEmpty(threadId) && !TextUtils.isEmpty(address)) {
            Log.d(TAG, "Resolving thread ID for address: " + address);
            threadId = messageService.getThreadIdForAddress(address);
            if (TextUtils.isEmpty(threadId)) {
                Log.e(TAG, "Could not resolve thread ID for address: " + address);
                finish();
                return;
            }
            Log.d(TAG, "Resolved thread ID: " + threadId + " for address: " + address);
        }

        // Initialize executor service
        executorService = Executors.newCachedThreadPool();

        // Initialize data
        messages = new ArrayList<>();

        // Initialize UI components
        initializeComponents();

        // Load messages
        loadMessages();
        
        // Apply custom colors if using custom theme
        applyCustomColorsToViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Set this thread as currently active for notification suppression
        currentlyActiveThreadId = threadId;
        Log.d(TAG, "Set active thread ID: " + threadId);
        
        // Register message update receiver when activity becomes visible
        setupMessageUpdateReceiver();
        
        // Refresh messages to catch any updates that may have been missed
        // while the activity was not visible (MESSAGE_RECEIVED broadcasts
        // are only received when the receiver is active)
        loadMessages();
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        // Clear the active thread ID since this conversation is no longer visible
        currentlyActiveThreadId = null;
        Log.d(TAG, "Cleared active thread ID");
        
        // Unregister message update receiver when activity is not visible
        if (messageUpdateReceiver != null) {
            try {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(messageUpdateReceiver);
                Log.d(TAG, "Message update receiver unregistered from LocalBroadcastManager in onPause");
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering message update receiver in onPause", e);
            }
            messageUpdateReceiver = null;
        }
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

        // Set up pinch-to-zoom gesture detection
        setupGestureDetection();

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

    private void setupGestureDetection() {
        // Initialize scale gesture detector for pinch-to-zoom
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                
                // Update text size based on scale gesture
                textSizeManager.updateTextSize(scaleFactor);
                
                // Update all visible text views
                if (adapter != null) {
                    adapter.updateTextSizes();
                }
                
                return true;
            }
        });

        // Set touch listener on RecyclerView to intercept gestures
        messagesRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Pass touch events to scale gesture detector
                scaleGestureDetector.onTouchEvent(event);
                
                // Return false to allow normal RecyclerView scrolling when not scaling
                return false;
            }
        });
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
                    // Update UI with DiffUtil for better performance
                    if (loadedMessages != null && !loadedMessages.isEmpty()) {
                        // Use DiffUtil to efficiently update the RecyclerView
                        adapter.updateMessages(new ArrayList<>(loadedMessages));
                        
                        // Restore translation state for all loaded messages
                        restoreTranslationStateForMessages(loadedMessages);
                        
                        Log.d(TAG, "Updated UI with " + loadedMessages.size() + " messages using DiffUtil");
                        
                        // Set up pagination
                        setupPagination();
                        
                        // Check if we have more messages to load
                        hasMoreMessages = loadedMessages.size() >= PAGE_SIZE;
                    } else {
                        Log.d(TAG, "No messages to add to UI list");
                        hasMoreMessages = false;
                        // Update with empty list
                        adapter.updateMessages(new ArrayList<>());
                    }
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
                        
                        // Restore translation state for new messages
                        restoreTranslationStateForMessages(newMessages);
                        
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
     * Uses LocalBroadcastManager for reliable intra-app communication.
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void setupMessageUpdateReceiver() {
        try {
            // Prevent double registration
            if (messageUpdateReceiver != null) {
                Log.d(TAG, "Message update receiver already registered, skipping");
                return;
            }
            
            // Create the broadcast receiver
            messageUpdateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "Message update broadcast received: " + intent.getAction());
                    
                    // Handle different update actions on UI thread to ensure UI updates work
                    if (intent != null && intent.getAction() != null) {
                        runOnUiThread(() -> {
                            // Check if this broadcast is relevant to our current thread
                            String updatedThreadId = intent.getStringExtra("thread_id");
                            
                            // Only process broadcasts for our thread or broadcasts without thread ID
                            if (updatedThreadId != null && !updatedThreadId.equals(threadId)) {
                                // This update is for a different thread, ignore it
                                Log.d(TAG, "Ignoring update for different thread: " + updatedThreadId);
                                return;
                            }
                            
                            switch (intent.getAction()) {
                                case "com.translator.messagingapp.MESSAGE_RECEIVED":
                                    Log.d(TAG, "Refreshing messages due to received message in this thread");
                                    // Clear cache only for this thread
                                    MessageCache.clearCacheForThread(threadId);
                                    loadMessages();
                                    break;
                                case "com.translator.messagingapp.MESSAGE_SENT":
                                    Log.d(TAG, "Message sent broadcast received, refreshing messages");
                                    MessageCache.clearCacheForThread(threadId);
                                    loadMessages();
                                    break;
                                case "com.translator.messagingapp.REFRESH_MESSAGES":
                                    Log.d(TAG, "General refresh request received");
                                    MessageCache.clearCacheForThread(threadId);
                                    loadMessages();
                                    break;
                                default:
                                    Log.d(TAG, "Unknown update action: " + intent.getAction());
                                    break;
                            }
                        });
                    }
                }
            };

            // Create intent filter for message update events
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.translator.messagingapp.MESSAGE_RECEIVED");
            filter.addAction("com.translator.messagingapp.REFRESH_MESSAGES");
            filter.addAction("com.translator.messagingapp.MESSAGE_SENT");

            // Register receiver with LocalBroadcastManager for more reliable delivery
            LocalBroadcastManager.getInstance(this).registerReceiver(messageUpdateReceiver, filter);
            Log.d(TAG, "Message update receiver registered successfully with LocalBroadcastManager");
            
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

        // Get user's preferred outgoing language
        String targetLanguage = userPreferences.getPreferredOutgoingLanguage();
        if (targetLanguage == null || targetLanguage.isEmpty()) {
            targetLanguage = userPreferences.getPreferredLanguage();
        }

        // Translate in background
        String finalTargetLanguage = targetLanguage;
        executorService.execute(() -> {
            translationManager.translateText(inputText, finalTargetLanguage, (success, translatedText, errorMessage) -> {
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
            }, true); // Force translation for outgoing messages
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
                        
                        // Set translation language info if available
                        message.setTranslatedLanguage(targetLanguage);

                        // Save translation state to cache
                        if (translationCache != null) {
                            message.saveTranslationState(translationCache);
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
            }, true); // Force translation for messages
        });
    }

    private void toggleMessageTranslation(Message message, int position) {
        if (message.isTranslated()) {
            // Toggle between original and translated text
            message.setShowTranslation(!message.isShowTranslation());
            
            // Save the updated show state to cache
            if (translationCache != null) {
                message.saveTranslationState(translationCache);
            }
            
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
        // Handle attachment click - open with appropriate app
        if (attachment != null && attachment.getUri() != null) {
            openAttachment(attachment.getUri(), attachment.getContentType());
        }
    }
    
    @Override
    public void onAttachmentClick(Uri uri, int position) {
        // Handle attachment click for URI - open with appropriate app
        if (uri != null) {
            openAttachment(uri, null);
        }
    }
    
    @Override
    public void onAttachmentLongClick(MmsMessage.Attachment attachment, int position) {
        // Handle attachment long click - show options menu
        if (attachment != null && attachment.getUri() != null) {
            showAttachmentOptionsDialog(attachment.getUri(), attachment.getContentType(), attachment.getFileName());
        }
    }
    
    @Override
    public void onAttachmentLongClick(Uri uri, int position) {
        // Handle attachment long click for URI - show options menu
        if (uri != null) {
            showAttachmentOptionsDialog(uri, null, null);
        }
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

    /**
     * Open attachment using the appropriate system app
     */
    private void openAttachment(Uri uri, String contentType) {
        try {
            // Check if this is an image or video - use MediaGalleryActivity for enhanced viewing
            if (contentType != null && (contentType.startsWith("image/") || contentType.startsWith("video/"))) {
                Intent galleryIntent = MediaGalleryActivity.createIntent(this, uri);
                startActivity(galleryIntent);
                return;
            }
            
            // For non-media files or unknown content types, try to determine if it's an image/video by URI
            if (contentType == null) {
                String uriString = uri.toString().toLowerCase();
                if (uriString.contains("image") || 
                    uriString.endsWith(".jpg") || uriString.endsWith(".jpeg") || 
                    uriString.endsWith(".png") || uriString.endsWith(".gif") ||
                    uriString.endsWith(".mp4") || uriString.endsWith(".mov") || 
                    uriString.endsWith(".avi")) {
                    Intent galleryIntent = MediaGalleryActivity.createIntent(this, uri);
                    startActivity(galleryIntent);
                    return;
                }
            }
            
            // Fall back to default system app for other file types
            Intent intent = new Intent(Intent.ACTION_VIEW);
            
            if (contentType != null) {
                intent.setDataAndType(uri, contentType);
            } else {
                intent.setData(uri);
            }
            
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "No app available to open this attachment", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening attachment", e);
            Toast.makeText(this, "Error opening attachment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Show options dialog for attachment (save, share, view)
     */
    private void showAttachmentOptionsDialog(Uri uri, String contentType, String fileName) {
        String[] options = {"View", "Save", "Share"};
        
        new AlertDialog.Builder(this)
                .setTitle("Attachment Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // View
                            openAttachment(uri, contentType);
                            break;
                        case 1: // Save
                            saveAttachment(uri, contentType, fileName);
                            break;
                        case 2: // Share
                            shareAttachment(uri, contentType);
                            break;
                    }
                })
                .show();
    }
    
    /**
     * Save attachment to device storage
     */
    private void saveAttachment(Uri uri, String contentType, String fileName) {
        try {
            // For Android 10+ (API 29+), use MediaStore
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                saveAttachmentModern(uri, contentType, fileName);
            } else {
                saveAttachmentLegacy(uri, contentType, fileName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving attachment", e);
            Toast.makeText(this, "Error saving attachment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Save attachment using modern MediaStore API (Android 10+)
     */
    @androidx.annotation.RequiresApi(api = android.os.Build.VERSION_CODES.Q)
    private void saveAttachmentModern(Uri uri, String contentType, String fileName) {
        try {
            android.content.ContentResolver resolver = getContentResolver();
            android.content.ContentValues values = new android.content.ContentValues();
            
            // Determine the appropriate collection based on content type
            android.net.Uri collection;
            if (contentType != null && contentType.startsWith("image/")) {
                collection = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName != null ? fileName : "attachment_" + System.currentTimeMillis());
                values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, contentType);
                values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/LinguaSMS");
            } else if (contentType != null && contentType.startsWith("video/")) {
                collection = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                values.put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, fileName != null ? fileName : "attachment_" + System.currentTimeMillis());
                values.put(android.provider.MediaStore.Video.Media.MIME_TYPE, contentType);
                values.put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_MOVIES + "/LinguaSMS");
            } else {
                collection = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName != null ? fileName : "attachment_" + System.currentTimeMillis());
                if (contentType != null) {
                    values.put(android.provider.MediaStore.Downloads.MIME_TYPE, contentType);
                }
                values.put(android.provider.MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/LinguaSMS");
            }
            
            android.net.Uri newUri = resolver.insert(collection, values);
            if (newUri != null) {
                try (java.io.InputStream input = resolver.openInputStream(uri);
                     java.io.OutputStream output = resolver.openOutputStream(newUri)) {
                    
                    if (input != null && output != null) {
                        byte[] buffer = new byte[8192];
                        int length;
                        while ((length = input.read(buffer)) > 0) {
                            output.write(buffer, 0, length);
                        }
                        Toast.makeText(this, "Attachment saved successfully", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving attachment with MediaStore", e);
            Toast.makeText(this, "Error saving attachment", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Save attachment using legacy method (Android 9 and below)
     */
    private void saveAttachmentLegacy(Uri uri, String contentType, String fileName) {
        // For legacy versions, just show a message that saving is not supported
        // or implement legacy file saving if needed
        Toast.makeText(this, "Please use the share option to save this attachment", Toast.LENGTH_LONG).show();
    }
    
    /**
     * Share attachment using system share dialog
     */
    private void shareAttachment(Uri uri, String contentType) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            
            if (contentType != null) {
                shareIntent.setType(contentType);
            } else {
                shareIntent.setType("*/*");
            }
            
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "Share attachment"));
        } catch (Exception e) {
            Log.e(TAG, "Error sharing attachment", e);
            Toast.makeText(this, "Error sharing attachment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
        
        // Clean up resources
        if (executorService != null) {
            executorService.shutdownNow();
        }
        
        // Note: BroadcastReceiver cleanup is handled in onPause()
        Log.d(TAG, "ConversationActivity destroyed");
    }

    /**
     * Restores translation state for a list of messages from the cache.
     */
    private void restoreTranslationStateForMessages(List<Message> messagesToRestore) {
        if (translationCache == null || messagesToRestore == null) {
            return;
        }
        
        for (Message message : messagesToRestore) {
            try {
                boolean restored = message.restoreTranslationState(translationCache);
                if (restored) {
                    Log.d(TAG, "Restored translation state for message " + message.getId());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error restoring translation state for message " + message.getId(), e);
            }
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
        
        // Update button colors for custom theme
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
            
            // Apply custom color to emoji button
            if (emojiButton != null) {
                emojiButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customButtonColor));
            }
            
            // Apply custom color to attachment button
            if (attachmentButton != null) {
                attachmentButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customButtonColor));
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
            // Apply custom colors to send button and other UI elements
            int defaultColor = getResources().getColor(R.color.colorPrimary);
            int customButtonColor = userPreferences.getCustomButtonColor(defaultColor);
            
            // Apply to send button
            if (sendButton != null) {
                sendButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customButtonColor));
            }
            
            // Apply to emoji button
            if (emojiButton != null) {
                emojiButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customButtonColor));
            }
            
            // Apply to attachment button
            if (attachmentButton != null) {
                attachmentButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customButtonColor));
            }
            
            // Apply to translate button
            if (translateButton != null) {
                translateButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customButtonColor));
            }
        }
    }
}