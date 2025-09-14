package com.translator.messagingapp.conversation;

import com.translator.messagingapp.R;
import com.translator.messagingapp.ui.*;

import com.translator.messagingapp.system.*;

import com.translator.messagingapp.sms.*;

import com.translator.messagingapp.mms.*;

import com.translator.messagingapp.message.*;

import com.translator.messagingapp.contact.*;

import com.translator.messagingapp.conversation.*;

import com.translator.messagingapp.translation.*;
import com.translator.messagingapp.util.EmojiPickerDialog;

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
import android.provider.ContactsContract;
import android.provider.OpenableColumns;
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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import java.io.InputStream;

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

    // Attachment preview components
    private LinearLayout attachmentPreviewContainer;
    private TextView attachmentPreviewText;
    private ImageButton attachmentRemoveButton;

    // Attachment menu components
    private View attachmentMenu;
    private FrameLayout attachmentMenuContainer;
    private LinearLayout attachmentGallery;
    private LinearLayout attachmentCamera;
    private LinearLayout attachmentGifs;
    private LinearLayout attachmentStickers;
    private LinearLayout attachmentFiles;
    private LinearLayout attachmentLocation;
    private LinearLayout attachmentContacts;
    private LinearLayout attachmentSchedule;
    private boolean isAttachmentMenuVisible = false;

    // Data
    private String threadId;
    private String address;
    private String contactName;
    private List<Message> messages;
    private ExecutorService executorService;
    
    // Selected attachments for sending
    private List<Uri> selectedAttachments;
    
    // Pagination variables
    private static final int PAGE_SIZE = 50;
    private static final int ATTACHMENT_PICK_REQUEST = 1001;
    private static final int GALLERY_PICK_REQUEST = 1003;
    private static final int CAMERA_REQUEST = 1004;
    private static final int GIF_PICK_REQUEST = 1005;
    private static final int FILES_PICK_REQUEST = 1006;
    private static final int LOCATION_PICK_REQUEST = 1007;
    private static final int CONTACTS_PICK_REQUEST = 1008;
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
        selectedAttachments = new ArrayList<>();

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
        
        // Set up text change listener for message input to update send button state
        if (messageInput != null) {
            messageInput.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // No action needed
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Update send button state as user types
                    updateSendButtonForTextInput();
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {
                    // No action needed
                }
            });
        }
        
        // Initialize attachment preview components
        attachmentPreviewContainer = findViewById(R.id.attachment_preview_container);
        attachmentPreviewText = findViewById(R.id.attachment_preview_text);
        attachmentRemoveButton = findViewById(R.id.attachment_remove_button);

        // Initialize attachment menu components
        attachmentMenuContainer = findViewById(R.id.attachment_menu_container);
        attachmentMenu = findViewById(R.id.attachment_menu);
        attachmentGallery = findViewById(R.id.attachment_gallery);
        attachmentCamera = findViewById(R.id.attachment_camera);
        attachmentGifs = findViewById(R.id.attachment_gifs);
        attachmentStickers = findViewById(R.id.attachment_stickers);
        attachmentFiles = findViewById(R.id.attachment_files);
        attachmentLocation = findViewById(R.id.attachment_location);
        attachmentContacts = findViewById(R.id.attachment_contacts);
        attachmentSchedule = findViewById(R.id.attachment_schedule);

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
            attachmentButton.setOnClickListener(v -> toggleAttachmentMenu());
            
            // Long press to clear selected attachments
            attachmentButton.setOnLongClickListener(v -> {
                if (selectedAttachments != null && !selectedAttachments.isEmpty()) {
                    clearAttachments();
                    return true;
                }
                return false;
            });
        }

        // Set up attachment remove button
        if (attachmentRemoveButton != null) {
            attachmentRemoveButton.setOnClickListener(v -> clearAttachments());
        }

        // Set up attachment menu listeners
        setupAttachmentMenuListeners();

        // Set up click listener to hide attachment menu when clicking outside
        if (attachmentMenuContainer != null) {
            attachmentMenuContainer.setOnClickListener(v -> {
                // Only hide if clicking on the container itself (not child views)
                if (v == attachmentMenuContainer) {
                    hideAttachmentMenu();
                }
            });
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
        boolean hasAttachments = selectedAttachments != null && !selectedAttachments.isEmpty();
        
        // For MMS, allow empty text if there are attachments
        if (messageText.isEmpty() && !hasAttachments) {
            return;
        }

        // Clear input and attachments
        messageInput.setText("");
        List<Uri> attachmentsToSend = new ArrayList<>(selectedAttachments);
        selectedAttachments.clear();
        updateAttachmentPreview(); // Clear the preview
        updateSendButtonForAttachments(); // Reset send button appearance

        // Show progress
        showLoadingIndicator();

        // Send message in background
        executorService.execute(() -> {
            try {
                boolean success;
                
                // Add logging as requested in issue #594 to confirm MMS vs SMS path
                if (hasAttachments) {
                    Log.d(TAG, "Sending MMS message to " + address + " with " + attachmentsToSend.size() + " attachments");
                    Log.d(TAG, "Attachment URIs: " + attachmentsToSend.toString());
                    
                    // Send as MMS with attachments
                    success = messageService.sendMmsMessage(address, null, messageText, attachmentsToSend);
                    
                    Log.d(TAG, "MMS send result: " + (success ? "SUCCESS" : "FAILED"));
                } else {
                    Log.d(TAG, "Sending SMS message to " + address + " (no attachments)");
                    
                    // Send as regular SMS
                    success = messageService.sendSmsMessage(address, messageText);
                    
                    Log.d(TAG, "SMS send result: " + (success ? "SUCCESS" : "FAILED"));
                }

                runOnUiThread(() -> {
                    hideLoadingIndicator();

                    if (success) {
                        // Note: Message refresh will be handled by broadcast receiver
                        // when MESSAGE_SENT broadcast is received from MessageService
                        Log.d(TAG, "Message sent successfully, waiting for broadcast to refresh UI");
                        
                        // Provide user feedback for successful MMS send
                        if (hasAttachments) {
                            Toast.makeText(ConversationActivity.this, 
                                    "MMS sent successfully", 
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Enhanced error messaging for better user experience
                        String errorMessage;
                        if (hasAttachments) {
                            errorMessage = "Failed to send MMS. Check network connection and try again.";
                            Log.e(TAG, "MMS sending failed for recipient: " + address);
                        } else {
                            errorMessage = getString(R.string.error_sending_message);
                            Log.e(TAG, "SMS sending failed for recipient: " + address);
                        }
                        
                        Toast.makeText(ConversationActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error sending message", e);
                runOnUiThread(() -> {
                    hideLoadingIndicator();
                    
                    // Enhanced error messaging based on message type
                    String errorMessage;
                    if (hasAttachments) {
                        errorMessage = "Failed to send MMS: " + e.getMessage() + 
                                      ". Check network connection, file permissions, and attachment sizes.";
                    } else {
                        errorMessage = getString(R.string.error_sending_message) + ": " + e.getMessage();
                    }
                    
                    Toast.makeText(ConversationActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Updates the send button appearance and state based on whether attachments are selected.
     * Enhanced to follow Android best practices for button state management.
     */
    private void updateSendButtonForAttachments() {
        if (sendButton != null) {
            boolean hasAttachments = selectedAttachments != null && !selectedAttachments.isEmpty();
            boolean hasText = messageInput != null && !messageInput.getText().toString().trim().isEmpty();
            
            // Enable/disable button based on content availability
            boolean shouldEnable = hasText || hasAttachments;
            sendButton.setEnabled(shouldEnable);
            
            if (hasAttachments) {
                // Change send button to indicate MMS mode
                sendButton.setAlpha(1.0f);
                // Could add visual indicator for MMS mode (e.g., different color/icon)
                sendButton.setContentDescription("Send MMS message with attachments");
                Log.d(TAG, "Send button configured for MMS mode with " + selectedAttachments.size() + " attachments");
            } else {
                // Reset to normal SMS mode
                sendButton.setAlpha(shouldEnable ? 1.0f : 0.5f);
                sendButton.setContentDescription("Send SMS message");
                Log.d(TAG, "Send button configured for SMS mode");
            }
        }
    }

    /**
     * Updates send button state when message text changes.
     * Called from text change listeners to ensure consistent button state.
     */
    private void updateSendButtonForTextInput() {
        if (sendButton != null && messageInput != null) {
            String currentText = messageInput.getText().toString().trim();
            boolean hasText = !currentText.isEmpty();
            boolean hasAttachments = selectedAttachments != null && !selectedAttachments.isEmpty();
            
            // Enable button if we have either text or attachments
            boolean shouldEnable = hasText || hasAttachments;
            sendButton.setEnabled(shouldEnable);
            sendButton.setAlpha(shouldEnable ? 1.0f : 0.5f);
            
            Log.d(TAG, "Send button updated for text input - enabled: " + shouldEnable + 
                      " (hasText: " + hasText + ", hasAttachments: " + hasAttachments + ")");
        }
    }

    /**
     * Clears all selected attachments and updates the UI
     */
    private void clearAttachments() {
        selectedAttachments.clear();
        updateAttachmentPreview();
        updateSendButtonForAttachments();
        Toast.makeText(this, "Attachments cleared", Toast.LENGTH_SHORT).show();
    }

    /**
     * Updates the attachment preview UI based on selected attachments
     */
    private void updateAttachmentPreview() {
        if (attachmentPreviewContainer == null || attachmentPreviewText == null) {
            return;
        }

        boolean hasAttachments = selectedAttachments != null && !selectedAttachments.isEmpty();
        
        if (hasAttachments) {
            // Show the preview container
            attachmentPreviewContainer.setVisibility(View.VISIBLE);
            
            // Update the preview text
            int count = selectedAttachments.size();
            String previewText;
            if (count == 1) {
                // Show filename for single attachment
                Uri uri = selectedAttachments.get(0);
                String fileName = getFileName(uri);
                previewText = fileName != null ? fileName : "Attachment";
            } else {
                // Show count for multiple attachments
                previewText = count + " attachments selected";
            }
            attachmentPreviewText.setText(previewText);
        } else {
            // Hide the preview container
            attachmentPreviewContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Gets the filename from a URI
     */
    private String getFileName(Uri uri) {
        if (uri == null) return null;
        
        String fileName = uri.getLastPathSegment();
        if (fileName != null && fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }
        return fileName;
    }
    
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
                String fileName = getFileName(uri);
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

    /**
     * Toggle the visibility of the attachment menu
     */
    private void toggleAttachmentMenu() {
        if (attachmentMenu != null) {
            if (isAttachmentMenuVisible) {
                hideAttachmentMenu();
            } else {
                showAttachmentMenu();
            }
        }
    }

    /**
     * Show the attachment menu with animation
     */
    private void showAttachmentMenu() {
        if (attachmentMenu != null && !isAttachmentMenuVisible) {
            attachmentMenu.setVisibility(View.VISIBLE);
            attachmentMenu.animate()
                .alpha(1.0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(200)
                .start();
            isAttachmentMenuVisible = true;
        }
    }

    /**
     * Hide the attachment menu with animation
     */
    private void hideAttachmentMenu() {
        if (attachmentMenu != null && isAttachmentMenuVisible) {
            attachmentMenu.animate()
                .alpha(0.0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(200)
                .withEndAction(() -> attachmentMenu.setVisibility(View.GONE))
                .start();
            isAttachmentMenuVisible = false;
        }
    }

    /**
     * Setup click listeners for attachment menu options
     */
    private void setupAttachmentMenuListeners() {
        if (attachmentGallery != null) {
            attachmentGallery.setOnClickListener(v -> {
                hideAttachmentMenu();
                openGalleryPicker();
            });
        }

        if (attachmentCamera != null) {
            attachmentCamera.setOnClickListener(v -> {
                hideAttachmentMenu();
                openCamera();
            });
        }

        if (attachmentGifs != null) {
            attachmentGifs.setOnClickListener(v -> {
                hideAttachmentMenu();
                openGifPicker();
            });
        }

        if (attachmentStickers != null) {
            attachmentStickers.setOnClickListener(v -> {
                hideAttachmentMenu();
                openStickerPicker();
            });
        }

        if (attachmentFiles != null) {
            attachmentFiles.setOnClickListener(v -> {
                hideAttachmentMenu();
                openFilesPicker();
            });
        }

        if (attachmentLocation != null) {
            attachmentLocation.setOnClickListener(v -> {
                hideAttachmentMenu();
                openLocationPicker();
            });
        }

        if (attachmentContacts != null) {
            attachmentContacts.setOnClickListener(v -> {
                hideAttachmentMenu();
                openContactsPicker();
            });
        }

        if (attachmentSchedule != null) {
            attachmentSchedule.setOnClickListener(v -> {
                hideAttachmentMenu();
                openScheduleDialog();
            });
        }
    }

    /**
     * Open gallery picker for images and videos
     */
    private void openGalleryPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*,video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        
        // Add flags for URI permissions as recommended in issue #594
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        
        try {
            startActivityForResult(Intent.createChooser(intent, getString(R.string.gallery)), GALLERY_PICK_REQUEST);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No gallery app found", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Open camera for taking photos or videos
     */
    private void openCamera() {
        // Check for camera permission
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 101);
            return;
        }
        
        // Create an intent to capture image or video
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Camera")
            .setMessage("Choose camera mode")
            .setPositiveButton("Photo", (dialog, which) -> {
                Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                try {
                    startActivityForResult(intent, CAMERA_REQUEST);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Video", (dialog, which) -> {
                Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
                intent.putExtra(android.provider.MediaStore.EXTRA_DURATION_LIMIT, 30); // 30 seconds max
                try {
                    startActivityForResult(intent, CAMERA_REQUEST);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
                }
            })
            .setNeutralButton("Cancel", null)
            .show();
    }

    /**
     * Open GIF picker
     */
    private void openGifPicker() {
        // For now, use a generic image picker but filter for GIFs
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/gif");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        
        // Add flags for URI permissions as recommended in issue #594
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        
        try {
            startActivityForResult(Intent.createChooser(intent, getString(R.string.gifs)), GIF_PICK_REQUEST);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No GIF picker found", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Open sticker picker (implemented as emoji picker)
     */
    private void openStickerPicker() {
        // Create a simple emoji picker dialog
        String[] emojis = {"😀", "😂", "🥰", "😎", "🤔", "👍", "❤️", "🎉", "🔥", "💯", "👌", "🙌", "🤝", "💪", "🌟", "⚡", "🎯", "🚀", "🏆", "💎"};
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Select Emoji Sticker")
            .setItems(emojis, (dialog, which) -> {
                String selectedEmoji = emojis[which];
                if (messageInput != null) {
                    String currentText = messageInput.getText().toString();
                    int cursorPosition = messageInput.getSelectionStart();
                    String newText = currentText.substring(0, cursorPosition) + selectedEmoji + currentText.substring(cursorPosition);
                    messageInput.setText(newText);
                    messageInput.setSelection(cursorPosition + selectedEmoji.length());
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    /**
     * Open files picker for documents
     */
    private void openFilesPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        
        // Add flags for URI permissions as recommended in issue #594
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        
        try {
            startActivityForResult(Intent.createChooser(intent, getString(R.string.files)), FILES_PICK_REQUEST);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No file manager found", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Open location picker or share current location
     */
    private void openLocationPicker() {
        // Check for location permission
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 102);
            return;
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Share Location")
            .setMessage("Choose location sharing method")
            .setPositiveButton("Current Location", (dialog, which) -> {
                getCurrentLocationAndShare();
            })
            .setNegativeButton("Choose on Map", (dialog, which) -> {
                // Open maps for location picking
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(android.net.Uri.parse("geo:0,0?q=location"));
                    startActivity(intent);
                    
                    // Add a generic location message as fallback
                    String locationText = "📍 Location shared via maps";
                    if (messageInput != null) {
                        String currentText = messageInput.getText().toString();
                        messageInput.setText(currentText + (currentText.isEmpty() ? "" : " ") + locationText);
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "No maps app available", Toast.LENGTH_SHORT).show();
                }
            })
            .setNeutralButton("Cancel", null)
            .show();
    }
    
    /**
     * Get current location and share it in the message
     */
    private void getCurrentLocationAndShare() {
        try {
            android.location.LocationManager locationManager = (android.location.LocationManager) getSystemService(Context.LOCATION_SERVICE);
            
            if (locationManager != null && (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) || 
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER))) {
                    
                // Create a location listener for a single location update
                android.location.LocationListener locationListener = new android.location.LocationListener() {
                    @Override
                    public void onLocationChanged(android.location.Location location) {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            String locationText = String.format("📍 Location: %.6f, %.6f\n🗺️ https://maps.google.com/maps?q=%.6f,%.6f", 
                                latitude, longitude, latitude, longitude);
                            
                            if (messageInput != null) {
                                String currentText = messageInput.getText().toString();
                                messageInput.setText(currentText + (currentText.isEmpty() ? "" : "\n") + locationText);
                            }
                            
                            Toast.makeText(ConversationActivity.this, "Current location added to message", Toast.LENGTH_SHORT).show();
                            locationManager.removeUpdates(this);
                        }
                    }
                    
                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {}
                    
                    @Override
                    public void onProviderEnabled(String provider) {}
                    
                    @Override
                    public void onProviderDisabled(String provider) {}
                };
                
                // Request location update
                if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestSingleUpdate(android.location.LocationManager.GPS_PROVIDER, locationListener, null);
                    locationManager.requestSingleUpdate(android.location.LocationManager.NETWORK_PROVIDER, locationListener, null);
                    Toast.makeText(this, "Getting current location...", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Location services not available, use a fallback
                String locationText = "📍 Location services not available";
                if (messageInput != null) {
                    String currentText = messageInput.getText().toString();
                    messageInput.setText(currentText + (currentText.isEmpty() ? "" : " ") + locationText);
                }
                Toast.makeText(this, "Location services not available", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting location", e);
            Toast.makeText(this, "Error getting location", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Open contacts picker
     */
    private void openContactsPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.ContactsContract.Contacts.CONTENT_URI);
        try {
            startActivityForResult(intent, CONTACTS_PICK_REQUEST);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No contacts app found", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Open schedule send dialog
     */
    private void openScheduleDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        
        // Create time picker options
        String[] options = {"In 1 minute", "In 5 minutes", "In 10 minutes", "In 30 minutes", "In 1 hour", "Choose custom time"};
        
        builder.setTitle("Schedule Message")
            .setItems(options, (dialog, which) -> {
                String message = messageInput != null ? messageInput.getText().toString().trim() : "";
                if (message.isEmpty()) {
                    Toast.makeText(this, "Please type a message first", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                long delayMillis;
                String scheduleName;
                
                switch (which) {
                    case 0: // 1 minute
                        delayMillis = 60 * 1000;
                        scheduleName = "1 minute";
                        break;
                    case 1: // 5 minutes
                        delayMillis = 5 * 60 * 1000;
                        scheduleName = "5 minutes";
                        break;
                    case 2: // 10 minutes
                        delayMillis = 10 * 60 * 1000;
                        scheduleName = "10 minutes";
                        break;
                    case 3: // 30 minutes
                        delayMillis = 30 * 60 * 1000;
                        scheduleName = "30 minutes";
                        break;
                    case 4: // 1 hour
                        delayMillis = 60 * 60 * 1000;
                        scheduleName = "1 hour";
                        break;
                    case 5: // Custom
                        showCustomTimePickerDialog(message);
                        return;
                    default:
                        return;
                }
                
                // Schedule the message (simplified implementation)
                scheduleMessage(message, delayMillis, scheduleName);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    /**
     * Show custom time picker dialog
     */
    private void showCustomTimePickerDialog(String message) {
        // For now, just show a toast. A full implementation would show date/time picker
        Toast.makeText(this, "Custom time scheduling coming soon!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Schedule a message to be sent later
     */
    private void scheduleMessage(String message, long delayMillis, String scheduleName) {
        // This is a simplified implementation - in production, you'd use WorkManager or AlarmManager
        Toast.makeText(this, "Message scheduled to send in " + scheduleName, Toast.LENGTH_LONG).show();
        
        // Clear the message input
        if (messageInput != null) {
            messageInput.setText("");
        }
        
        // In a real implementation, you would:
        // 1. Save the message to a database with timestamp
        // 2. Set up WorkManager or AlarmManager to send it
        // 3. Show in UI that message is scheduled
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        switch (requestCode) {
            case 101: // Camera permission
                if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
                }
                break;
                
            case 102: // Location permission
                if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    openLocationPicker();
                } else {
                    Toast.makeText(this, "Location permission is required to share location", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        // If attachment menu is visible, hide it instead of going back
        if (isAttachmentMenuVisible) {
            hideAttachmentMenu();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Legacy attachment picker method - kept for backward compatibility
     */
    private void openAttachmentPicker() {
        // Show the new attachment menu instead
        toggleAttachmentMenu();
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
        
        if (resultCode == RESULT_OK && data != null) {
            Uri selectedUri = data.getData();
            
            switch (requestCode) {
                case ATTACHMENT_PICK_REQUEST:
                case GALLERY_PICK_REQUEST:
                case GIF_PICK_REQUEST:
                case FILES_PICK_REQUEST:
                    handleFileAttachment(selectedUri, requestCode);
                    break;
                    
                case CAMERA_REQUEST:
                    handleCameraResult(data);
                    break;
                    
                case CONTACTS_PICK_REQUEST:
                    handleContactResult(selectedUri);
                    break;
                    
                default:
                    break;
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
     * Handle file attachment result from various pickers
     */
    private void handleFileAttachment(Uri selectedUri, int requestCode) {
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
            selectedAttachments.add(selectedUri);
            
            // Show confirmation and update UI to indicate attachment is selected
            String fileName = getFileName(selectedUri);
            String attachmentType = getAttachmentTypeName(requestCode);
            Toast.makeText(this, attachmentType + " selected: " + (fileName != null ? fileName : "file"), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Attachment selected: " + selectedUri.toString());
            
            // Update attachment preview and send button
            updateAttachmentPreview();
            updateSendButtonForAttachments();
        }
    }

    /**
     * Handle camera result
     */
    private void handleCameraResult(Intent data) {
        if (data == null) {
            Toast.makeText(this, "Camera capture cancelled", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Camera usually returns a bitmap in the extras for photo capture
        android.graphics.Bitmap photo = (android.graphics.Bitmap) data.getExtras().get("data");
        if (photo != null) {
            try {
                // Save the captured photo to internal storage
                String filename = "camera_" + System.currentTimeMillis() + ".jpg";
                java.io.FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
                photo.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos);
                fos.close();
                
                // Create URI for the saved file
                java.io.File savedFile = new java.io.File(getFilesDir(), filename);
                Uri photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", savedFile);
                
                // Add to selected attachments
                selectedAttachments.add(photoUri);
                
                // Update UI
                updateAttachmentPreview();
                updateSendButtonForAttachments();
                
                Toast.makeText(this, "Photo captured and ready to send", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Camera photo saved: " + photoUri.toString());
                
            } catch (Exception e) {
                Log.e(TAG, "Error saving camera photo", e);
                Toast.makeText(this, "Error saving photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            // Handle video capture result or other camera results
            Uri videoUri = data.getData();
            if (videoUri != null) {
                selectedAttachments.add(videoUri);
                updateAttachmentPreview();
                updateSendButtonForAttachments();
                Toast.makeText(this, "Video captured and ready to send", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Camera video captured: " + videoUri.toString());
            } else {
                Toast.makeText(this, "No photo or video captured", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Handle contact selection result
     */
    private void handleContactResult(Uri selectedUri) {
        if (selectedUri != null) {
            try {
                // Query contact data
                String[] projection = {
                    android.provider.ContactsContract.Contacts.DISPLAY_NAME,
                    android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER
                };
                
                Cursor cursor = getContentResolver().query(selectedUri, projection, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    String contactName = cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME));
                    boolean hasPhoneNumber = cursor.getInt(cursor.getColumnIndex(android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0;
                    
                    if (hasPhoneNumber) {
                        // Get phone number
                        String contactId = selectedUri.getLastPathSegment();
                        Cursor phoneCursor = getContentResolver().query(
                            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{contactId},
                            null
                        );
                        
                        if (phoneCursor != null && phoneCursor.moveToFirst()) {
                            String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER));
                            
                            // Create contact info string to insert into message
                            String contactInfo = "Contact: " + contactName + " - " + phoneNumber;
                            
                            // Insert contact info into message input
                            if (messageInput != null) {
                                String currentText = messageInput.getText().toString();
                                messageInput.setText(currentText + (currentText.isEmpty() ? "" : " ") + contactInfo);
                            }
                            
                            Toast.makeText(this, "Contact info added to message", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Contact selected: " + contactName + " - " + phoneNumber);
                            
                            phoneCursor.close();
                        } else {
                            Toast.makeText(this, "Could not get phone number for contact", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Just insert contact name
                        String contactInfo = "Contact: " + contactName;
                        if (messageInput != null) {
                            String currentText = messageInput.getText().toString();
                            messageInput.setText(currentText + (currentText.isEmpty() ? "" : " ") + contactInfo);
                        }
                        Toast.makeText(this, "Contact name added to message", Toast.LENGTH_SHORT).show();
                    }
                    
                    cursor.close();
                } else {
                    Toast.makeText(this, "Could not read contact information", Toast.LENGTH_SHORT).show();
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error handling contact selection", e);
                Toast.makeText(this, "Error reading contact: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No contact selected", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Get a human-readable name for the attachment type
     */
    private String getAttachmentTypeName(int requestCode) {
        switch (requestCode) {
            case GALLERY_PICK_REQUEST:
                return "Gallery item";
            case GIF_PICK_REQUEST:
                return "GIF";
            case FILES_PICK_REQUEST:
                return "File";
            case ATTACHMENT_PICK_REQUEST:
            default:
                return "Attachment";
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