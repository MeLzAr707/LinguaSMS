
package com.translator.messagingapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

/**
 * Main activity for the messaging app.
 * FIXED VERSION: Properly implements ConversationClickListener
 */
public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
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
    private DefaultSmsAppManager defaultSmsAppManager;
    private TranslationManager translationManager;

    // Data
    private List<Conversation> conversations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize services
        TranslatorApp app = (TranslatorApp) getApplication();
        if (app != null) {
            messageService = app.getMessageService();
            defaultSmsAppManager = app.getDefaultSmsAppManager();
            translationManager = app.getTranslationManager();
        }

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up drawer layout
        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Set up navigation view
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize UI components
        conversationsRecyclerView = findViewById(R.id.conversations_recycler_view);
        emptyStateTextView = findViewById(R.id.empty_state_text_view);
        newMessageFab = findViewById(R.id.new_message_fab);

        // Set up recycler view
        conversationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        conversations = new ArrayList<>();
        conversationAdapter = new ConversationRecyclerAdapter(this, conversations);
        conversationsRecyclerView.setAdapter(conversationAdapter);

        // Set up FAB
        newMessageFab.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(MainActivity.this, NewMessageActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting NewMessageActivity", e);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Set up conversation click listener
        conversationAdapter.setConversationClickListener(new ConversationRecyclerAdapter.ConversationClickListener() {
            @Override
            public void onConversationClick(Conversation conversation) {
                openConversation(conversation);
            }

            @Override
            public void onConversationLongClick(Conversation conversation) {
                showConversationOptions(conversation);
            }
        });

        // Check if we're the default SMS app
        if (defaultSmsAppManager != null) {
            // Check if we should show the default SMS request (first time or manually triggered)
            UserPreferences userPrefs = ((TranslatorApp) getApplication()).getUserPreferences();
            boolean shouldRequest = userPrefs.getBoolean("should_request_default_sms", false);
            
            if (shouldRequest) {
                // Clear the flag so we don't keep asking
                userPrefs.setBoolean("should_request_default_sms", false);
                
                // Force the request even if we've asked before on first run
                DefaultSmsAppManager.checkAndRequestDefaultSmsApp(this, SMS_REQUEST_CODE, () -> {
                    // This will run if we're already the default SMS app
                    loadConversations();
                });
            } else {
                // Normal check - respect the request count limits
                DefaultSmsAppManager.checkAndRequestDefaultSmsApp(this, SMS_REQUEST_CODE, () -> {
                    // This will run if we're already the default SMS app
                    loadConversations();
                });
            }
        } else {
            loadConversations();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConversations();
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
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        try {
            if (id == R.id.action_search) {
                Intent intent = new Intent(this, SearchActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.action_settings) {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.action_debug) {
                Intent intent = new Intent(this, DebugActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.action_default_sms) {
                handleDefaultSmsAction();
                return true;
            } else if (id == R.id.action_refresh) {
                loadConversations();
                Toast.makeText(this, "Refreshing conversations...", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.action_test_message) {
                addTestMessage();
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling menu item: " + id, e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return super.onOptionsItemSelected(item);
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
                showAboutDialog();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling navigation item: " + id, e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Shows the about dialog.
     */
    private void showAboutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.about)
                .setMessage("Messaging App with Translation / Version 1.0.0")
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * Loads conversations asynchronously.
     */
    private void loadConversations() {
        try {
            // Show loading state
            showLoadingState();

            // Load conversations in background thread
            new Thread(() -> {
                try {
                    if (messageService != null) {
                        List<Conversation> loadedConversations = messageService.loadConversations();

                        // Update UI on main thread
                        runOnUiThread(() -> {
                            try {
                                conversations.clear();
                                if (loadedConversations != null) {
                                    conversations.addAll(loadedConversations);
                                }

                                conversationAdapter.notifyDataSetChanged();

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
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error starting conversation loading", e);
            showErrorState();
        }
    }

    /**
     * Shows loading state.
     */
    private void showLoadingState() {
        emptyStateTextView.setVisibility(View.GONE);
        conversationsRecyclerView.setVisibility(View.GONE);
        // Note: Add progress bar if available in layout
    }

    /**
     * Shows empty state.
     */
    private void showEmptyState() {
        emptyStateTextView.setText(R.string.no_conversations);
        emptyStateTextView.setVisibility(View.VISIBLE);
        conversationsRecyclerView.setVisibility(View.GONE);
    }

    /**
     * Shows conversations state.
     */
    private void showConversationsState() {
        emptyStateTextView.setVisibility(View.GONE);
        conversationsRecyclerView.setVisibility(View.VISIBLE);
    }

    /**
     * Shows error state.
     */
    private void showErrorState() {
        emptyStateTextView.setText(R.string.error_loading_conversation);
        emptyStateTextView.setVisibility(View.VISIBLE);
        conversationsRecyclerView.setVisibility(View.GONE);
    }

    /**
     * Opens a conversation.
     *
     * @param conversation The conversation to open
     */
    private void openConversation(Conversation conversation) {
        try {
            Intent intent = new Intent(this, ConversationActivity.class);
            intent.putExtra("thread_id", conversation.getThreadId());
            intent.putExtra("address", conversation.getAddress());
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening conversation", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows options for a conversation.
     *
     * @param conversation The conversation
     */
    private void showConversationOptions(Conversation conversation) {
        try {
            String[] options = {"Delete", "Mark as read", "Call"};

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(conversation.getContactName())
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                deleteConversation(conversation);
                                break;
                            case 1:
                                markConversationAsRead(conversation);
                                break;
                            case 2:
                                callContact(conversation);
                                break;
                        }
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing conversation options", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Deletes a conversation.
     *
     * @param conversation The conversation to delete
     */
    private void deleteConversation(Conversation conversation) {
        try {
            if (messageService != null) {
                boolean success = messageService.deleteConversation(conversation.getThreadId());

                if (success) {
                    Toast.makeText(this, R.string.conversation_deleted, Toast.LENGTH_SHORT).show();
                    loadConversations();
                } else {
                    Toast.makeText(this, "Failed to delete conversation", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting conversation", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Marks a conversation as read.
     *
     * @param conversation The conversation to mark as read
     */
    private void markConversationAsRead(Conversation conversation) {
        try {
            if (messageService != null) {
                boolean success = messageService.markThreadAsRead(conversation.getThreadId());

                if (success) {
                    Toast.makeText(this, "Marked as read", Toast.LENGTH_SHORT).show();
                    loadConversations();
                } else {
                    Toast.makeText(this, "Failed to mark as read", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error marking conversation as read", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Calls the contact associated with a conversation.
     *
     * @param conversation The conversation
     */
    private void callContact(Conversation conversation) {
        try {
            String phoneNumber = conversation.getAddress();

            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                DefaultSmsAppManager.callPhoneNumber(this, phoneNumber);
            } else {
                Toast.makeText(this, "No phone number available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calling contact", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SMS_REQUEST_CODE) {
            if (defaultSmsAppManager != null) {
                DefaultSmsAppManager.handleDefaultSmsAppResult(this, requestCode, resultCode);

                // Check if we're now the default SMS app
                if (DefaultSmsAppManager.isDefaultSmsApp(this)) {
                    Toast.makeText(this, R.string.default_set_success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.default_set_failed, Toast.LENGTH_SHORT).show();
                }
            }

            // Load conversations regardless of result
            loadConversations();
        }
    }

    /**
     * Adds a test message (for debugging).
     */
    private void addTestMessage() {
        try {
            if (messageService != null) {
                boolean success = messageService.addTestMessage();

                if (success) {
                    Toast.makeText(this, "Test message added", Toast.LENGTH_SHORT).show();
                    loadConversations();
                } else {
                    Toast.makeText(this, "Failed to add test message", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding test message", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows the default SMS app dialog if needed.
     */
    private void showDefaultSmsAppDialogIfNeeded() {
        try {
            if (defaultSmsAppManager != null && !DefaultSmsAppManager.isDefaultSmsApp(this)) {
                DefaultSmsAppManager.checkAndRequestDefaultSmsApp(this, SMS_REQUEST_CODE, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing default SMS app dialog", e);
        }
    }

    /**
     * Handles the default SMS action from the menu.
     */
    private void handleDefaultSmsAction() {
        try {
            if (defaultSmsAppManager != null) {
                if (DefaultSmsAppManager.isDefaultSmsApp(this)) {
                    // Already default SMS app, show settings option
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle(R.string.default_sms_title)
                            .setMessage("This app is already the default SMS app. Would you like to open default app settings?")
                            .setPositiveButton("Open Settings", (dialog, which) -> {
                                DefaultSmsAppManager.tryDirectDefaultSmsAppSetting(this);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    // Not default SMS app, request to become default
                    DefaultSmsAppManager.checkAndRequestDefaultSmsApp(this, SMS_REQUEST_CODE, () -> {
                        Toast.makeText(this, R.string.default_set_success, Toast.LENGTH_SHORT).show();
                        loadConversations();
                    });
                }
            } else {
                Toast.makeText(this, "Default SMS manager not available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling default SMS action", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
