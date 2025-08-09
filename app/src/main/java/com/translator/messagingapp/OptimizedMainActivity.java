
package com.translator.messagingapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Optimized main activity for the messaging app.
 * Uses improved background processing and RecyclerView updates.
 */
public class OptimizedMainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "OptimizedMainActivity";
    private static final int SMS_REQUEST_CODE = 1001;
    private static final int PERMISSION_REQUEST_CODE = 1002;

    // UI components
    private DrawerLayout drawer;
    private RecyclerView conversationsRecyclerView;
    private ConversationRecyclerAdapter conversationAdapter;
    private TextView emptyStateTextView;
    private FloatingActionButton newMessageFab;

    // Services
    private MessageService messageService;
    private OptimizedMessageService optimizedMessageService;
    private DefaultSmsAppManager defaultSmsAppManager;
    private TranslationManager translationManager;

    // Optimized components
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();

    // Data
    private List<Conversation> conversations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            // Initialize services
            translationManager = getTranslationManager();
            messageService = new MessageService(this, translationManager);
            optimizedMessageService = new OptimizedMessageService(this, translationManager);
            defaultSmsAppManager = new DefaultSmsAppManager(this);

            // Initialize UI components
            initializeComponents();

            // Initialize data
            conversations = new ArrayList<>();
            conversationAdapter = new ConversationRecyclerAdapter(this, conversations);
            conversationsRecyclerView.setAdapter(conversationAdapter);

            // Check if we're the default SMS app - using instance methods
            if (defaultSmsAppManager.isDefaultSmsApp()) {
                // Already default SMS app, load conversations
                loadConversationsOptimized();
            } else {
                // Request to be default SMS app
                defaultSmsAppManager.checkAndRequestDefaultSmsApp(this, SMS_REQUEST_CODE, () -> {
                    // This will run if we're already the default SMS app
                    loadConversationsOptimized();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing app", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Gets the translation manager.
     *
     * @return The translation manager
     */
    private TranslationManager getTranslationManager() {
        if (translationManager == null) {
            GoogleTranslationService translationService = new GoogleTranslationService(this);
            UserPreferences userPreferences = new UserPreferences(this);
            translationManager = new TranslationManager(this, translationService, userPreferences);
        }
        return translationManager;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConversationsOptimized();
    }

    /**
     * Loads conversations using optimized background processing.
     */
    private void loadConversationsOptimized() {
        try {
            // Show loading state
            showLoadingState();

            // Load conversations in background thread with proper executor
            backgroundExecutor.execute(() -> {
                try {
                    if (messageService != null) {
                        List<Conversation> loadedConversations = messageService.loadConversations();

                        // Calculate diff to optimize RecyclerView updates
                        final List<Conversation> oldConversations = new ArrayList<>(conversations);
                        final List<Conversation> newConversations = loadedConversations != null
                                ? loadedConversations
                                : new ArrayList<>();

                        // Update UI on main thread with DiffUtil
                        runOnUiThread(() -> {
                            try {
                                // Use DiffUtil to calculate minimal changes
                                DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                                        new ConversationDiffCallback(oldConversations, newConversations));

                                // Update data
                                conversations.clear();
                                conversations.addAll(newConversations);

                                // Apply changes to adapter
                                diffResult.dispatchUpdatesTo(conversationAdapter);

                                // Show appropriate state
                                if (conversations.isEmpty()) {
                                    showEmptyState();
                                } else {
                                    showConversationsState();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error updating UI with conversations", e);
                                showErrorState();
                            }
                        });
                    } else {
                        Log.e(TAG, "MessageService is null");
                        runOnUiThread(this::showErrorState);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading conversations in background", e);
                    runOnUiThread(this::showErrorState);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadConversationsOptimized: " + e.getMessage(), e);
            showErrorState();
        }
    }

    /**
     * Shows the loading state.
     */
    private void showLoadingState() {
        if (conversationsRecyclerView != null) {
            conversationsRecyclerView.setVisibility(View.GONE);
        }

        if (emptyStateTextView != null) {
            emptyStateTextView.setVisibility(View.GONE);
        }

        // Show loading indicator
        ProgressBar progressBar = findViewById(R.id.progress_bar);
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Shows the empty state.
     */
    private void showEmptyState() {
        if (conversationsRecyclerView != null) {
            conversationsRecyclerView.setVisibility(View.GONE);
        }

        if (emptyStateTextView != null) {
            emptyStateTextView.setVisibility(View.VISIBLE);
        }

        // Hide loading indicator
        ProgressBar progressBar = findViewById(R.id.progress_bar);
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Shows the conversations state.
     */
    private void showConversationsState() {
        if (conversationsRecyclerView != null) {
            conversationsRecyclerView.setVisibility(View.VISIBLE);
        }

        if (emptyStateTextView != null) {
            emptyStateTextView.setVisibility(View.GONE);
        }

        // Hide loading indicator
        ProgressBar progressBar = findViewById(R.id.progress_bar);
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Shows the error state.
     */
    private void showErrorState() {
        if (conversationsRecyclerView != null) {
            conversationsRecyclerView.setVisibility(View.GONE);
        }

        if (emptyStateTextView != null) {
            emptyStateTextView.setText(R.string.error_loading_conversations);
            emptyStateTextView.setVisibility(View.VISIBLE);
        }

        // Hide loading indicator
        ProgressBar progressBar = findViewById(R.id.progress_bar);
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Initializes UI components.
     */
    private void initializeComponents() {
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up drawer
        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Set up navigation view
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Set up RecyclerView
        conversationsRecyclerView = findViewById(R.id.conversations_recycler_view);
        conversationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set up empty state text view - fixed ID
        emptyStateTextView = findViewById(R.id.empty_state_text_view);

        // Set up FAB
        newMessageFab = findViewById(R.id.new_message_fab);
        newMessageFab.setOnClickListener(v -> {
            Intent intent = new Intent(this, NewMessageActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        try {
            if (id == R.id.nav_home) {
                // Already on home screen
            } else if (id == R.id.nav_settings) {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_about) {
                // Show about dialog
                new AlertDialog.Builder(this)
                        .setTitle(R.string.about)
                        .setMessage(R.string.about_message)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling navigation item: " + id, e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * DiffUtil callback for comparing lists of conversations.
     */
    private static class ConversationDiffCallback extends DiffUtil.Callback {
        private final List<Conversation> oldConversations;
        private final List<Conversation> newConversations;

        public ConversationDiffCallback(List<Conversation> oldConversations, List<Conversation> newConversations) {
            this.oldConversations = oldConversations;
            this.newConversations = newConversations;
        }

        @Override
        public int getOldListSize() {
            return oldConversations.size();
        }

        @Override
        public int getNewListSize() {
            return newConversations.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            Conversation oldConversation = oldConversations.get(oldItemPosition);
            Conversation newConversation = newConversations.get(newItemPosition);

            // Compare by thread ID
            return oldConversation.getThreadId().equals(newConversation.getThreadId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Conversation oldConversation = oldConversations.get(oldItemPosition);
            Conversation newConversation = newConversations.get(newItemPosition);

            // Compare relevant fields
            boolean sameSnippet = TextUtils.equals(oldConversation.getSnippet(), newConversation.getSnippet());
            boolean sameDate = oldConversation.getDate() == newConversation.getDate();
            boolean sameUnreadCount = oldConversation.getUnreadCount() == newConversation.getUnreadCount();
            boolean sameAddress = TextUtils.equals(oldConversation.getAddress(), newConversation.getAddress());

            return sameSnippet && sameDate && sameUnreadCount && sameAddress;
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu); // Fixed menu resource name
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_search) {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

