package com.translator.messagingapp.ui;

import com.translator.messagingapp.ui.*;

import com.translator.messagingapp.system.*;

import com.translator.messagingapp.mms.*;

import com.translator.messagingapp.message.*;

import com.translator.messagingapp.contact.*;

import com.translator.messagingapp.conversation.*;

import com.translator.messagingapp.translation.*;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for searching messages across all conversations.
 */
public class SearchActivity extends BaseActivity implements MessageRecyclerAdapter.OnMessageClickListener {
    private static final String TAG = "SearchActivity";

    // UI components
    private EditText searchEditText;
    private ImageButton clearSearchButton;
    private RecyclerView searchResultsRecyclerView;
    private MessageRecyclerAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyStateTextView;

    // Data
    private List<Message> searchResults;
    private ExecutorService executorService;
    private MessageService messageService;
    private TranslationCache translationCache;
    
    // Search debouncing
    private Handler searchHandler;
    private Runnable searchRunnable;
    private static final int SEARCH_DELAY_MS = 300; // Delay before executing search
    
    // Simple cache for recent searches
    private String lastSearchQuery = "";
    private List<Message> lastSearchResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Get service instances from TranslatorApp with null checks
        try {
            TranslatorApp app = (TranslatorApp) getApplication();
            messageService = app.getMessageService();
            translationCache = app.getTranslationCache();
        } catch (Exception e) {
            Log.e(TAG, "Error getting service instances", e);
            // Services may be null, will be handled in individual operations
        }

        // Initialize executor service
        executorService = Executors.newCachedThreadPool();
        
        // Initialize search handler for debouncing
        searchHandler = new Handler(Looper.getMainLooper());

        // Initialize data
        searchResults = new ArrayList<>();

        // Initialize UI components
        initializeUI();
    }

    private void initializeUI() {
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.search_messages);
        }

        // Find views
        searchEditText = findViewById(R.id.search_edit_text);
        clearSearchButton = findViewById(R.id.clear_search_button);
        searchResultsRecyclerView = findViewById(R.id.search_results_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        emptyStateTextView = findViewById(R.id.empty_state_text_view);

        // Set up RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        searchResultsRecyclerView.setLayoutManager(layoutManager);
        adapter = new MessageRecyclerAdapter(this, searchResults, this);
        searchResultsRecyclerView.setAdapter(adapter);

        // Set up search input
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Show/hide clear button based on text
                clearSearchButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);

                // Cancel any pending search
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                // Perform search if text is at least 2 characters with debouncing
                if (s.length() >= 2) {
                    String query = s.toString();
                    
                    // Check cache first
                    if (query.equals(lastSearchQuery) && lastSearchResults != null) {
                        updateSearchResults(lastSearchResults);
                        return;
                    }
                    
                    // Create new search runnable with debouncing
                    searchRunnable = () -> performSearch(query);
                    searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
                } else if (s.length() == 0) {
                    // Clear results if search is empty
                    clearSearchResults();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not used
            }
        });

        // Set up clear button
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            clearSearchResults();
        });

        // Initially hide clear button
        clearSearchButton.setVisibility(View.GONE);

        // Show initial empty state
        showEmptyState(R.string.search_hint);
    }

    private void performSearch(String query) {
        // Show loading indicator
        showLoadingIndicator();

        // Check if messageService is available
        if (messageService == null) {
            Log.e(TAG, "MessageService is null, cannot perform search");
            runOnUiThread(() -> {
                showEmptyState(R.string.search_error);
                hideLoadingIndicator();
                Toast.makeText(this, "Search service unavailable", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        // Don't recreate executor service every time - just cancel previous search
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }

        // Perform search in background
        executorService.execute(() -> {
            try {
                // Search messages using MessageService
                List<Message> results = messageService.searchMessages(query);

                // Restore translation states for search results
                if (translationCache != null) {
                    for (Message message : results) {
                        message.restoreTranslationState(translationCache);
                    }
                }

                // Cache the results
                lastSearchQuery = query;
                lastSearchResults = new ArrayList<>(results);

                // Update UI on main thread
                runOnUiThread(() -> updateSearchResults(results));
                
            } catch (Exception e) {
                Log.e(TAG, "Error searching messages", e);
                runOnUiThread(() -> {
                    showEmptyState(R.string.search_error);
                    hideLoadingIndicator();
                    Toast.makeText(SearchActivity.this, 
                        "Search error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * Updates the search results in the UI.
     * 
     * @param results The search results to display
     */
    private void updateSearchResults(List<Message> results) {
        // Update search results
        searchResults.clear();
        if (results != null && !results.isEmpty()) {
            searchResults.addAll(results);
            adapter.notifyDataSetChanged();
            hideEmptyState();
        } else {
            showEmptyState(R.string.no_search_results);
        }

        // Hide loading indicator
        hideLoadingIndicator();
    }

    private void clearSearchResults() {
        searchResults.clear();
        adapter.notifyDataSetChanged();
        showEmptyState(R.string.search_hint);
        
        // Clear search cache
        lastSearchQuery = "";
        lastSearchResults = null;
    }

    private void showLoadingIndicator() {
        progressBar.setVisibility(View.VISIBLE);
        emptyStateTextView.setVisibility(View.GONE);
    }

    private void hideLoadingIndicator() {
        progressBar.setVisibility(View.GONE);
    }

    private void showEmptyState(int stringResId) {
        emptyStateTextView.setText(stringResId);
        emptyStateTextView.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        emptyStateTextView.setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMessageClick(Message message, int position) {
        // Open the conversation containing this message
        openConversation(message);
    }

    @Override
    public void onMessageLongClick(Message message, int position) {
        // Same as regular click for search results
        openConversation(message);
    }

    @Override
    public void onTranslateClick(Message message, int position) {
        // Not implemented for search results
    }

    @Override
    public void onAttachmentClick(MmsMessage.Attachment attachment, int position) {
        // Not implemented for search results - redirect to conversation
        if (attachment != null) {
            openConversationForAttachment(attachment);
        }
    }

    @Override
    public void onAttachmentClick(Uri uri, int position) {
        // Not implemented for search results - show message
        Toast.makeText(this, "Open the conversation to interact with attachments", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAttachmentLongClick(MmsMessage.Attachment attachment, int position) {
        // Not implemented for search results - redirect to conversation
        if (attachment != null) {
            openConversationForAttachment(attachment);
        }
    }

    @Override
    public void onAttachmentLongClick(Uri uri, int position) {
        // Not implemented for search results - show message
        Toast.makeText(this, "Open the conversation to interact with attachments", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReactionClick(Message message, int position) {
        // Not implemented for search results
        // Reactions are not supported in search results view
    }

    @Override
    public void onAddReactionClick(Message message, int position) {
        // Not implemented for search results
        // Reactions are not supported in search results view
    }

    private void openConversation(Message message) {
        // Open the conversation containing this message
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra("thread_id", String.valueOf(message.getThreadId()));
        intent.putExtra("address", message.getAddress());

        // Try to get contact name
        String contactName = ContactUtils.getContactName(this, message.getAddress());
        intent.putExtra("contact_name", contactName);

        startActivity(intent);
    }
    
    private void openConversationForAttachment(MmsMessage.Attachment attachment) {
        // For attachments, we need to find the message to open the conversation
        // For now, just show a message to the user
        Toast.makeText(this, "Open the conversation to interact with this attachment", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up resources
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
    }
}
