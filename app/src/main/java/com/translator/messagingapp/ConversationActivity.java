package com.translator.messagingapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ContentResolver;
import android.database.Cursor;
import android.media.RingtoneManager;
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
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for displaying a conversation.
 */
public class ConversationActivity extends BaseActivity implements MessageRecyclerAdapter.OnMessageClickListener, ContactSettingsDialog.OnToneSelectedListener {
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
    private ImageButton sendButton;
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
    private static final int ATTACHMENT_PICK_REQUEST = 1001;
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMoreMessages = true;

    // Service classes
    private MessageService messageService;
    private TranslationManager translationManager;
    private TranslationCache translationCache;
    private UserPreferences userPreferences;

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
            // Use improved display name logic that shows phone number instead of "Unknown"
            String displayTitle = getDisplayTitle(contactName, address);
            getSupportActionBar().setTitle(displayTitle);
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
        attachmentButton = findViewById(R.id.attachment_button);

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

        // Check if this is a group conversation based on the address
        updateGroupConversationStatus();

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

        // Set up attachment button
        if (attachmentButton != null) {
            attachmentButton.setOnClickListener(v -> openAttachmentPicker());
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
                        
                        // Update group conversation status after messages are loaded
                        updateGroupConversationStatus();
                        
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
                        
                        // Update group conversation status even for empty list
                        updateGroupConversationStatus();
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
                                case "com.translator.messagingapp.MESSAGE_TRANSLATED":
                                    Log.d(TAG, "Auto-translation completed for message in this thread");
                                    // Refresh to show translation results (cache already updated by MessageService)
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
            filter.addAction("com.translator.messagingapp.MESSAGE_TRANSLATED");

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
            translationManager.translateText(inputText, finalTargetLanguage, new TranslationManager.EnhancedTranslationCallback() {
                @Override
                public android.app.Activity getActivity() {
                    return ConversationActivity.this;
                }
                
                @Override
                public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
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
                }
            }, true); // Force translation for outgoing messages
        });
    }

    private void openAttachmentPicker() {
        // Create an intent to pick attachments
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        
        try {
            startActivityForResult(Intent.createChooser(intent, "Select attachment"), ATTACHMENT_PICK_REQUEST);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a file manager to select attachments", Toast.LENGTH_SHORT).show();
        }
    }

    private void makePhoneCall() {
        if (address == null || address.isEmpty()) {
            Toast.makeText(this, "No phone number available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create intent to make phone call
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + address));
        
        try {
            startActivity(callIntent);
        } catch (SecurityException e) {
            // Permission not granted, ask user to use dialer
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:" + address));
            startActivity(dialIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to make call", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error making phone call", e);
        }
    }

    private void translateAllMessages() {
        if (messages == null || messages.isEmpty()) {
            Toast.makeText(this, "No messages to translate", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get user's preferred language
        String targetLanguage = userPreferences.getPreferredLanguage();
        if (targetLanguage == null || targetLanguage.isEmpty()) {
            Toast.makeText(this, "Please set a preferred language in settings", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress dialog
        showProgressDialog("Translating all messages...");

        // Count messages that need translation
        int messagesToTranslate = 0;
        for (Message message : messages) {
            if (!message.isTranslated() && message.getBody() != null && !message.getBody().trim().isEmpty()) {
                messagesToTranslate++;
            }
        }

        if (messagesToTranslate == 0) {
            hideProgressDialog();
            Toast.makeText(this, "All messages are already translated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Translate messages in background
        final int totalToTranslate = messagesToTranslate;
        final java.util.concurrent.atomic.AtomicInteger completedTranslations = new java.util.concurrent.atomic.AtomicInteger(0);

        executorService.execute(() -> {
            for (int i = 0; i < messages.size(); i++) {
                Message message = messages.get(i);
                if (!message.isTranslated() && message.getBody() != null && !message.getBody().trim().isEmpty()) {
                    final int position = i;
                    translationManager.translateText(
                        message.getBody(),
                        targetLanguage,
                        new TranslationManager.EnhancedTranslationCallback() {
                            @Override
                            public android.app.Activity getActivity() {
                                return ConversationActivity.this;
                            }

                            @Override
                            public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
                                runOnUiThread(() -> {
                                    if (success && translatedText != null) {
                                        message.setTranslatedText(translatedText);
                                        message.setTranslated(true);
                                        message.setShowTranslation(true);
                                        adapter.notifyItemChanged(position);
                                    }

                                    int completed = completedTranslations.incrementAndGet();
                                    if (completed >= totalToTranslate) {
                                        hideProgressDialog();
                                        Toast.makeText(ConversationActivity.this, 
                                            "Translation complete", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }, true);
                }
            }
        });
    }

    private void showDeleteConversationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Delete Conversation")
            .setMessage("Are you sure you want to delete this entire conversation? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> deleteConversation())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteConversation() {
        if (threadId == null || threadId.isEmpty()) {
            Toast.makeText(this, "Unable to delete conversation", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        showProgressDialog("Deleting conversation...");

        // Delete in background
        executorService.execute(() -> {
            try {
                boolean success = messageService.deleteConversation(threadId);
                
                runOnUiThread(() -> {
                    hideProgressDialog();
                    
                    if (success) {
                        Toast.makeText(ConversationActivity.this, 
                            "Conversation deleted", Toast.LENGTH_SHORT).show();
                        finish(); // Close the activity
                    } else {
                        Toast.makeText(ConversationActivity.this, 
                            "Failed to delete conversation", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideProgressDialog();
                    Toast.makeText(ConversationActivity.this, 
                        "Error deleting conversation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
                Log.e(TAG, "Error deleting conversation", e);
            }
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
            translationManager.translateText(
                    message.getBody(),
                    targetLanguage,
                    new TranslationManager.EnhancedTranslationCallback() {
                        @Override
                        public android.app.Activity getActivity() {
                            return ConversationActivity.this;
                        }

                        @Override
                        public void onTranslationComplete(boolean success, String translatedText, String errorMessage) {
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
                                    Toast.makeText(
                                            ConversationActivity.this,
                                            getString(R.string.translation_error) + ": " +
                                                    (errorMessage != null ? errorMessage : getString(R.string.unknown_error)),
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });
                        }
                    },
                    true // Force translation for messages
            );
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
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
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
        } else if (id == R.id.action_call) {
            makePhoneCall();
            return true;
        } else if (id == R.id.action_translate) {
            translateInput();
            return true;
        } else if (id == R.id.action_contact_settings) {
            showContactSettingsDialog();
            return true;
        } else if (id == R.id.action_translate_all) {
            translateAllMessages();
            return true;
        } else if (id == R.id.action_delete_conversation) {
            showDeleteConversationDialog();
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
            // Get the proper content URI for sharing
            Uri shareableUri = getShareableUri(uri, contentType);
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            
            if (contentType != null) {
                intent.setDataAndType(shareableUri, contentType);  
            } else {
                intent.setData(shareableUri);
            }
            
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Try with a more generic content type for videos
                if (contentType != null && contentType.startsWith("video/")) {
                    Intent fallbackIntent = new Intent(Intent.ACTION_VIEW);
                    fallbackIntent.setDataAndType(shareableUri, "video/*");
                    fallbackIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    if (fallbackIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(fallbackIntent);
                        return;
                    }
                    
                    // Try to explicitly launch a video player
                    Intent videoIntent = new Intent(Intent.ACTION_VIEW);
                    videoIntent.setDataAndType(shareableUri, "video/*");
                    videoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    videoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    try {
                        startActivity(Intent.createChooser(videoIntent, "Choose video player"));
                        return;
                    } catch (Exception e) {
                        Log.w(TAG, "Could not launch video chooser", e);
                    }
                }
                
                // Try without content type as a final fallback
                Intent genericIntent = new Intent(Intent.ACTION_VIEW);
                genericIntent.setData(shareableUri);
                genericIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                genericIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                if (genericIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(genericIntent);
                } else {
                    Toast.makeText(this, "No app available to open this attachment", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening attachment", e);
            Toast.makeText(this, "Error opening attachment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Get a shareable URI for the given attachment URI
     */
    private Uri getShareableUri(Uri uri, String contentType) {
        if (uri == null) {
            return null;
        }
        
        // If it's already a content:// URI, return as is
        if ("content".equals(uri.getScheme())) {
            return uri;
        }
        
        // If it's a file:// URI, try to convert to content:// URI using FileProvider
        if ("file".equals(uri.getScheme())) {
            try {
                // For video files, we need to ensure proper sharing permissions
                if (contentType != null && contentType.startsWith("video/")) {
                    // Try to get a FileProvider URI
                    try {
                        return FileProvider.getUriForFile(
                            this,
                            "com.translator.messagingapp.fileprovider",
                            new java.io.File(uri.getPath())
                        );
                    } catch (Exception e) {
                        Log.w(TAG, "Could not create FileProvider URI for video: " + uri, e);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error converting file URI to content URI", e);
            }
        }
        
        // Return original URI if conversion fails
        return uri;
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == ATTACHMENT_PICK_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedUri = data.getData();
            if (selectedUri != null) {
                // For now, just show a toast that attachment was selected
                // In a full implementation, this would handle sending MMS
                Toast.makeText(this, "Attachment selected: " + selectedUri.getLastPathSegment(), Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Attachment selected: " + selectedUri.toString());
            }
        }
        
        if (requestCode == ContactSettingsDialog.getRingtonePickerRequestCode() && resultCode == RESULT_OK) {
            Uri selectedUri = data != null ? 
                data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) : null;
            
            // Create a new dialog instance to handle the result
            ContactSettingsDialog dialog = new ContactSettingsDialog(this, address, contactName, this);
            dialog.handleRingtonePickerResult(selectedUri);
        }
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

    /**
     * Updates the group conversation status in the adapter based on the current conversation.
     */
    private void updateGroupConversationStatus() {
        if (adapter == null) {
            return;
        }

        boolean isGroup = false;
        
        // Method 1: Check if address contains comma (multiple participants)
        if (!TextUtils.isEmpty(address) && address.contains(",")) {
            isGroup = true;
            Log.d(TAG, "Detected group conversation from address: " + address);
        }
        
        // Method 2: Check if we have messages from multiple senders
        if (!isGroup && messages != null && messages.size() > 1) {
            Set<String> uniqueAddresses = new HashSet<>();
            for (Message message : messages) {
                if (message != null && message.getAddress() != null && message.getType() == Message.TYPE_INBOX) {
                    uniqueAddresses.add(message.getAddress());
                    if (uniqueAddresses.size() > 1) {
                        isGroup = true;
                        Log.d(TAG, "Detected group conversation from messages with multiple senders");
                        break;
                    }
                }
            }
        }
        
        // Update the adapter
        adapter.setGroupConversation(isGroup);
        Log.d(TAG, "Group conversation status: " + isGroup);
    }

    /**
     * Shows the contact settings dialog.
     */
    private void showContactSettingsDialog() {
        if (TextUtils.isEmpty(address)) {
            Toast.makeText(this, "Unable to access contact settings", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ContactSettingsDialog dialog = new ContactSettingsDialog(this, address, contactName, this);
        dialog.show();
    }

    @Override
    public void onToneSelected(String contactAddress, String toneUri) {
        // Callback when user selects a notification tone
        Log.d(TAG, "Notification tone selected for " + contactAddress + ": " + toneUri);
        // No additional action needed as the preference is already saved in the dialog
    }

    @Override
    public void onRequestRingtoneSelection(int requestCode) {
        // Start ringtone picker activity
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.select_notification_tone));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        
        // Set current selection if any
        UserPreferences prefs = new UserPreferences(this);
        String currentTone = prefs.getContactNotificationTone(address);
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
        
        try {
            startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            Log.e(TAG, "Error starting ringtone picker", e);
            Toast.makeText(this, getString(R.string.error_setting_tone), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Gets the appropriate display title for the conversation toolbar.
     * Ensures we show phone number instead of "Unknown" when contact name is not available.
     */
    private String getDisplayTitle(String contactName, String address) {
        // Clean up contact name - handle string "null", empty, or actual null
        if (TextUtils.isEmpty(contactName) || "null".equals(contactName)) {
            contactName = null;
        }
        
        // If we have a valid contact name, use it
        if (contactName != null && !contactName.equals(address)) {
            return contactName;
        }
        
        // Try to look up contact name as fallback
        if (!TextUtils.isEmpty(address)) {
            String lookedUpName = ContactUtils.getContactName(this, address);
            if (!TextUtils.isEmpty(lookedUpName)) {
                return lookedUpName;
            }
            
            // Always show formatted phone number instead of "Unknown"
            if (address.contains(",")) {
                // Handle group conversation addresses
                return getGroupDisplayTitle(address);
            } else {
                // Single address - format as phone number
                return formatPhoneNumberForTitle(address);
            }
        }
        
        // Last resort - be descriptive instead of just "Unknown"
        return "No Contact Info";
    }

    /**
     * Formats a group address for the conversation title.
     */
    private String getGroupDisplayTitle(String addresses) {
        if (TextUtils.isEmpty(addresses)) {
            return "Group Chat";
        }
        
        String[] addressArray = addresses.split(",");
        if (addressArray.length <= 1) {
            String singleAddress = addresses.trim();
            return formatPhoneNumberForTitle(singleAddress);
        }
        
        // For group, show participant count and first few numbers
        StringBuilder titleBuilder = new StringBuilder();
        int participantCount = 0;
        int maxToShow = 2;
        
        for (int i = 0; i < addressArray.length && participantCount < maxToShow; i++) {
            String address = addressArray[i].trim();
            if (TextUtils.isEmpty(address)) {
                continue;
            }
            
            String contactName = ContactUtils.getContactName(this, address);
            String displayName = !TextUtils.isEmpty(contactName) ? contactName : formatCompactPhoneForTitle(address);
            
            if (participantCount > 0) {
                titleBuilder.append(", ");
            }
            titleBuilder.append(displayName);
            participantCount++;
        }
        
        if (addressArray.length > maxToShow) {
            int remaining = addressArray.length - maxToShow;
            titleBuilder.append(" +").append(remaining);
        }
        
        String result = titleBuilder.toString();
        return result.length() > 0 ? result : "Group (" + addressArray.length + ")";
    }

    /**
     * Formats a phone number for display in the title bar.
     */
    private String formatPhoneNumberForTitle(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return "No Number";
        }
        
        // Remove any non-digit characters except +
        String cleanNumber = phoneNumber.replaceAll("[^\\d+]", "");
        
        // Remove country code for cleaner display
        if (cleanNumber.startsWith("+1") && cleanNumber.length() == 12) {
            cleanNumber = cleanNumber.substring(2);
        } else if (cleanNumber.startsWith("1") && cleanNumber.length() == 11) {
            cleanNumber = cleanNumber.substring(1);
        }
        
        // Format as (XXX) XXX-XXXX for 10-digit numbers
        if (cleanNumber.length() == 10) {
            return String.format("(%s) %s-%s",
                    cleanNumber.substring(0, 3),
                    cleanNumber.substring(3, 6),
                    cleanNumber.substring(6));
        }
        
        // Return original if we can't format it
        return phoneNumber;
    }

    /**
     * Formats a phone number compactly for group titles.
     */
    private String formatCompactPhoneForTitle(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return "???";
        }
        
        // For titles, show last 4 digits with area code if available
        String cleanNumber = phoneNumber.replaceAll("[^\\d]", "");
        
        if (cleanNumber.length() >= 7) {
            return cleanNumber.substring(0, 3) + "-" + cleanNumber.substring(cleanNumber.length() - 4);
        } else if (cleanNumber.length() >= 4) {
            return "..." + cleanNumber.substring(cleanNumber.length() - 4);
        }
        
        return phoneNumber.length() > 10 ? phoneNumber.substring(0, 8) + "..." : phoneNumber;
    }

}