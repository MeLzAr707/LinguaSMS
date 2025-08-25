package com.translator.messagingapp;

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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
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
    private ImageButton filterButton;
    private LinearLayout filterPanel;
    private RadioGroup searchScopeRadioGroup;
    private Spinner messageTypeSpinner;
    private Button applyFiltersButton;
    private Button clearFiltersButton;
    private RecyclerView searchResultsRecyclerView;
    private MessageRecyclerAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyStateTextView;

    // Data
    private List<Message> searchResults;
    private ExecutorService executorService;
    private MessageService messageService;
    private TranslationCache translationCache;
    private SearchFilter currentSearchFilter;
    
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
        currentSearchFilter = new SearchFilter(); // Default filter

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
        filterButton = findViewById(R.id.filter_button);
        filterPanel = findViewById(R.id.filter_panel);
        searchScopeRadioGroup = findViewById(R.id.search_scope_radio_group);
        messageTypeSpinner = findViewById(R.id.message_type_spinner);
        applyFiltersButton = findViewById(R.id.apply_filters_button);
        clearFiltersButton = findViewById(R.id.clear_filters_button);
        searchResultsRecyclerView = findViewById(R.id.search_results_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        emptyStateTextView = findViewById(R.id.empty_state_text_view);

        // Set up RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        searchResultsRecyclerView.setLayoutManager(layoutManager);
        adapter = new MessageRecyclerAdapter(this, searchResults, this);
        searchResultsRecyclerView.setAdapter(adapter);

        // Set up filters
        setupFilters();

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
                // Search messages using MessageService with current filter
                List<Message> results = messageService.searchMessages(query, currentSearchFilter);

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
    
    /**
     * Sets up the filter UI components and their event handlers.
     */
    private void setupFilters() {
        // Set up filter button
        filterButton.setOnClickListener(v -> toggleFilterPanel());
        
        // Set up message type spinner
        setupMessageTypeSpinner();
        
        // Set up filter buttons
        applyFiltersButton.setOnClickListener(v -> applyFilters());
        clearFiltersButton.setOnClickListener(v -> clearFilters());
    }
    
    /**
     * Sets up the message type spinner with filter options.
     */
    private void setupMessageTypeSpinner() {
        String[] messageTypeOptions = {
            getString(R.string.filter_all_messages),
            getString(R.string.filter_sms_only),
            getString(R.string.filter_mms_only),
            getString(R.string.filter_translated_only),
            getString(R.string.filter_untranslated_only)
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, messageTypeOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        messageTypeSpinner.setAdapter(adapter);
        
        messageTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Handle selection (will be applied when Apply Filters is clicked)
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    /**
     * Toggles the visibility of the filter panel.
     */
    private void toggleFilterPanel() {
        if (filterPanel.getVisibility() == View.GONE) {
            filterPanel.setVisibility(View.VISIBLE);
        } else {
            filterPanel.setVisibility(View.GONE);
        }
    }
    
    /**
     * Applies the current filter settings and performs a new search.
     */
    private void applyFilters() {
        // Get search scope from radio group
        int checkedId = searchScopeRadioGroup.getCheckedRadioButtonId();
        SearchFilter.SearchScope scope;
        if (checkedId == R.id.radio_original_only) {
            scope = SearchFilter.SearchScope.ORIGINAL_ONLY;
        } else if (checkedId == R.id.radio_translation_only) {
            scope = SearchFilter.SearchScope.TRANSLATION_ONLY;
        } else {
            scope = SearchFilter.SearchScope.ALL_CONTENT;
        }
        
        // Get message type filter from spinner
        int selectedMessageType = messageTypeSpinner.getSelectedItemPosition();
        SearchFilter.MessageTypeFilter messageTypeFilter;
        switch (selectedMessageType) {
            case 1:
                messageTypeFilter = SearchFilter.MessageTypeFilter.SMS_ONLY;
                break;
            case 2:
                messageTypeFilter = SearchFilter.MessageTypeFilter.MMS_ONLY;
                break;
            case 3:
                messageTypeFilter = SearchFilter.MessageTypeFilter.TRANSLATED_ONLY;
                break;
            case 4:
                messageTypeFilter = SearchFilter.MessageTypeFilter.UNTRANSLATED_ONLY;
                break;
            default:
                messageTypeFilter = SearchFilter.MessageTypeFilter.ALL;
                break;
        }
        
        // Update current filter
        currentSearchFilter.setSearchScope(scope);
        currentSearchFilter.setMessageTypeFilter(messageTypeFilter);
        
        // Hide filter panel
        filterPanel.setVisibility(View.GONE);
        
        // Clear cache since filters changed
        lastSearchQuery = "";
        lastSearchResults = null;
        
        // Perform search with new filters if there's a query
        String currentQuery = searchEditText.getText().toString().trim();
        if (!currentQuery.isEmpty() && currentQuery.length() >= 2) {
            performSearch(currentQuery);
        }
        
        Toast.makeText(this, "Filters applied", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Clears all filter settings and resets to defaults.
     */
    private void clearFilters() {
        // Reset to default filter
        currentSearchFilter = new SearchFilter();
        
        // Reset UI components
        searchScopeRadioGroup.check(R.id.radio_all_content);
        messageTypeSpinner.setSelection(0);
        
        // Hide filter panel
        filterPanel.setVisibility(View.GONE);
        
        // Clear cache since filters changed
        lastSearchQuery = "";
        lastSearchResults = null;
        
        // Perform search with cleared filters if there's a query
        String currentQuery = searchEditText.getText().toString().trim();
        if (!currentQuery.isEmpty() && currentQuery.length() >= 2) {
            performSearch(currentQuery);
        }
        
        Toast.makeText(this, "Filters cleared", Toast.LENGTH_SHORT).show();
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
