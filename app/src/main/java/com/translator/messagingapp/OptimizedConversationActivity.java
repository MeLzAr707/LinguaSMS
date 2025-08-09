package com.translator.messagingapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Optimized activity for displaying and sending messages in a conversation.
 * Uses pagination, efficient background processing, and optimized RecyclerView updates.
 */
public class OptimizedConversationActivity extends BaseActivity {
    private static final String TAG = "OptimizedConversationActivity";
    private static final int PAGE_SIZE = 50;

    // UI components
    private RecyclerView messagesRecyclerView;
    private OptimizedMessageRecyclerAdapter adapter;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton translateButton;
    private ProgressBar progressBar;
    private TextView emptyStateTextView;
    private ProgressBar loadingIndicator;

    // Services
    private MessageService messageService;
    private OptimizedMessageService optimizedMessageService;
    private TranslationManager translationManager;

    // Data
    private List<Message> messages;
    private String threadId;
    private String address;
    private String contactName;
    private boolean isActivityDestroyed = false;

    // Optimized components
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMoreMessages = true;
    private PaginationUtils.PaginationScrollListener paginationScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        try {
            // Get intent data
            Intent intent = getIntent();
            if (intent != null) {
                threadId = intent.getStringExtra("thread_id");
                address = intent.getStringExtra("address");
                contactName = intent.getStringExtra("contact_name");
            }

            // Initialize services
            translationManager = getTranslationManager();
            optimizedMessageService = new OptimizedMessageService(this, translationManager);
            messageService = optimizedMessageService; // Use optimized service

            // Initialize UI components
            initializeComponents();

            // Initialize data
            messages = new ArrayList<>();
            adapter = new OptimizedMessageRecyclerAdapter(this, messages, new OptimizedMessageRecyclerAdapter.MessageClickListener() {
                @Override
                public void onMessageClick(Message message) {
                    // Handle message click
                    Toast.makeText(OptimizedConversationActivity.this, "Message clicked", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onMessageLongClick(Message message) {
                    // Handle message long click
                    showMessageOptions(message);
                }
            });
            messagesRecyclerView.setAdapter(adapter);

            // Set up pagination
            setupPagination();

            // Load initial messages
            loadMessagesOptimized(0, PAGE_SIZE);
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing conversation", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Shows options for a message.
     *
     * @param message The message
     */
    private void showMessageOptions(Message message) {
        // Implementation for showing message options
        Toast.makeText(this, "Message options", Toast.LENGTH_SHORT).show();
    }

    /**
     * Sets up pagination for the RecyclerView.
     */
    private void setupPagination() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) messagesRecyclerView.getLayoutManager();
        
        paginationScrollListener = (PaginationUtils.PaginationScrollListener) PaginationUtils.setupPagination(
            messagesRecyclerView,
            onLoadingComplete -> {
                // Load next page
                int nextPage = currentPage + 1;
                int offset = nextPage * PAGE_SIZE;
                
                loadMessagesOptimized(offset, PAGE_SIZE, onLoadingComplete);
            },
            10, // threshold - start loading when 10 items from the end
            loadingIndicator
        );
    }

    /**
     * Loads messages using the optimized message service with pagination.
     *
     * @param offset The offset to start loading from
     * @param limit The maximum number of messages to load
     */
    private void loadMessagesOptimized(int offset, int limit) {
        loadMessagesOptimized(offset, limit, null);
    }

    /**
     * Loads messages using the optimized message service with pagination.
     *
     * @param offset The offset to start loading from
     * @param limit The maximum number of messages to load
     * @param onLoadingComplete Callback to notify when loading is complete
     */
    private void loadMessagesOptimized(int offset, int limit, PaginationUtils.OnLoadingCompleteCallback onLoadingComplete) {
        if (optimizedMessageService == null) {
            Log.e(TAG, "OptimizedMessageService is null");
            showEmptyState("Error: Message service unavailable");
            if (onLoadingComplete != null) {
                onLoadingComplete.onLoadingComplete();
            }
            return;
        }

        if (isActivityDestroyed) {
            Log.d(TAG, "Activity destroyed, skipping message loading");
            if (onLoadingComplete != null) {
                onLoadingComplete.onLoadingComplete();
            }
            return;
        }

        if (offset == 0) {
            showLoading(true);
        }

        isLoading = true;

        // Use the appropriate method based on available data
        if (!TextUtils.isEmpty(threadId)) {
            Log.d(TAG, "Loading messages by thread ID: " + threadId + ", offset: " + offset + ", limit: " + limit);
            
            optimizedMessageService.getMessagesByThreadIdPaginated(threadId, offset, limit, loadedMessages -> {
                handleLoadedMessages(loadedMessages, offset, onLoadingComplete);
            });
        } else if (!TextUtils.isEmpty(address)) {
            Log.d(TAG, "Loading messages by address: " + address + ", offset: " + offset + ", limit: " + limit);
            
            // For address-based loading, we'll use the regular service for now
            // In a real implementation, we would create an optimized version for this as well
            backgroundExecutor.execute(() -> {
                List<Message> loadedMessages = messageService.getMessagesByAddress(address);
                
                // Apply pagination manually for now
                int endIndex = Math.min(offset + limit, loadedMessages.size());
                List<Message> pagedMessages = offset < loadedMessages.size() 
                    ? loadedMessages.subList(offset, endIndex) 
                    : new ArrayList<>();
                
                handleLoadedMessages(pagedMessages, offset, onLoadingComplete);
            });
        } else {
            Log.e(TAG, "Both threadId and address are empty");
            runOnUiThread(() -> {
                showEmptyState("No conversation selected");
                isLoading = false;
                if (onLoadingComplete != null) {
                    onLoadingComplete.onLoadingComplete();
                }
            });
        }
    }

    /**
     * Handles loaded messages and updates the UI.
     *
     * @param loadedMessages The loaded messages
     * @param offset The offset that was used
     * @param onLoadingComplete Callback to notify when loading is complete
     */
    private void handleLoadedMessages(List<Message> loadedMessages, int offset, PaginationUtils.OnLoadingCompleteCallback onLoadingComplete) {
        runOnUiThread(() -> {
            try {
                if (isActivityDestroyed) {
                    Log.d(TAG, "Activity destroyed, skipping UI update");
                    if (onLoadingComplete != null) {
                        onLoadingComplete.onLoadingComplete();
                    }
                    return;
                }

                // Check if this is the first page or a subsequent page
                if (offset == 0) {
                    // First page - replace all messages
                    updateMessages(loadedMessages);
                } else {
                    // Subsequent page - append messages
                    appendMessages(loadedMessages);
                }

                // Update pagination state
                currentPage = offset / PAGE_SIZE;
                hasMoreMessages = !loadedMessages.isEmpty();
                
                if (paginationScrollListener != null) {
                    paginationScrollListener.setHasMoreItems(hasMoreMessages);
                }

                // Show appropriate state
                if (messages.isEmpty()) {
                    showEmptyState("No messages");
                } else {
                    showMessagesState();
                    
                    // Scroll to bottom on first load
                    if (offset == 0) {
                        messagesRecyclerView.scrollToPosition(messages.size() - 1);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating UI with messages", e);
                showEmptyState("Error loading messages");
            } finally {
                showLoading(false);
                isLoading = false;
                
                // Notify that loading is complete
                if (onLoadingComplete != null) {
                    onLoadingComplete.onLoadingComplete();
                }
            }
        });
    }

    /**
     * Updates the messages list using DiffUtil for efficient RecyclerView updates.
     *
     * @param newMessages The new list of messages
     */
    private void updateMessages(List<Message> newMessages) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new MessageDiffCallback(messages, newMessages));
        
        messages.clear();
        messages.addAll(newMessages);
        
        diffResult.dispatchUpdatesTo(adapter);
    }

    /**
     * Appends messages to the existing list.
     *
     * @param newMessages The new messages to append
     */
    private void appendMessages(List<Message> newMessages) {
        if (newMessages.isEmpty()) {
            return;
        }
        
        int startPosition = messages.size();
        messages.addAll(newMessages);
        adapter.notifyItemRangeInserted(startPosition, newMessages.size());
    }

    /**
     * Shows or hides the loading indicator.
     *
     * @param show Whether to show the loading indicator
     */
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Shows the empty state with a message.
     *
     * @param message The message to display
     */
    private void showEmptyState(String message) {
        if (messagesRecyclerView != null) {
            messagesRecyclerView.setVisibility(View.GONE);
        }
        
        if (emptyStateTextView != null) {
            emptyStateTextView.setText(message);
            emptyStateTextView.setVisibility(View.VISIBLE);
        }
        
        showLoading(false);
    }

    /**
     * Shows the messages state.
     */
    private void showMessagesState() {
        if (messagesRecyclerView != null) {
            messagesRecyclerView.setVisibility(View.VISIBLE);
        }
        
        if (emptyStateTextView != null) {
            emptyStateTextView.setVisibility(View.GONE);
        }
        
        showLoading(false);
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
            }

            // Set up other UI components
            messageInput = findViewById(R.id.message_input);
            sendButton = findViewById(R.id.send_button);
            translateButton = findViewById(R.id.translate_button);
            progressBar = findViewById(R.id.progress_bar);
            emptyStateTextView = findViewById(R.id.empty_state_text);
            loadingIndicator = findViewById(R.id.loading_indicator);

            // Set up click listeners
            if (sendButton != null) {
                sendButton.setOnClickListener(v -> sendMessage());
            }

            if (translateButton != null) {
                translateButton.setOnClickListener(v -> translateInputText());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing components: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing UI components", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sends a message.
     */
    private void sendMessage() {
        // Implementation for sending a message
        Toast.makeText(this, "Send message", Toast.LENGTH_SHORT).show();
    }

    /**
     * Translates the input text.
     */
    private void translateInputText() {
        // Implementation for translating input text
        Toast.makeText(this, "Translate input text", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        isActivityDestroyed = true;
        super.onDestroy();
    }

    // Rest of the ConversationActivity implementation remains the same
    // ...
}