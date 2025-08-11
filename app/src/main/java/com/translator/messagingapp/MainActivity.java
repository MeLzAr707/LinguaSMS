package com.translator.messagingapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST_READ_SMS = 1001;
    private static final int SMS_REQUEST_CODE = 2001;

    // UI components
    private RecyclerView conversationsRecyclerView;
    private ConversationRecyclerAdapter conversationAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView emptyStateTextView;
    private FloatingActionButton newMessageFab;
    private DrawerLayout drawerLayout;

    // Data
    private List<Conversation> conversations;
    private ExecutorService executorService;

    // Service classes
    private MessageService messageService;
    private DefaultSmsAppManager defaultSmsAppManager;
    private TranslationManager translationManager;
    private UserPreferences userPreferences;

    // Theme configuration
    protected boolean useNoActionBar = true; // Default to true since we're using NoActionBar theme

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set to use NoActionBar variant since we use custom toolbar
        setUseNoActionBar(true);
        
        super.onCreate(savedInstanceState);

        // Initialize UserPreferences - done in BaseActivity
        userPreferences = new UserPreferences(this);

        // Get service instances from TranslatorApp with null checks
        try {
            TranslatorApp app = (TranslatorApp) getApplication();
            messageService = app.getMessageService();
            defaultSmsAppManager = app.getDefaultSmsAppManager();
            translationManager = app.getTranslationManager();
        } catch (Exception e) {
            Log.e(TAG, "Error getting service instances", e);
            // Services may be null, will be handled in individual operations
        }

        // Set content view
        setContentView(R.layout.activity_main);

        try {
            // Initialize components
            initializeComponents();

            // Set up toolbar
            setupStandardToolbar();

            // Check permissions and load conversations
            checkPermissions();

            // Check if we're the default SMS app
            checkDefaultSmsAppStatus();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing MainActivity", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Check if we're the default SMS app and request if needed
     */
    private void checkDefaultSmsAppStatus() {
        if (defaultSmsAppManager != null) {
            defaultSmsAppManager.checkAndRequestDefaultSmsApp(this, SMS_REQUEST_CODE);
        } else {
            Log.w(TAG, "DefaultSmsAppManager is null, cannot check default SMS app status");
        }
    }

    /**
     * Set whether this activity should use a NoActionBar theme
     * @param useNoActionBar true to use NoActionBar theme, false otherwise
     */
    protected void setUseNoActionBar(boolean useNoActionBar) {
        this.useNoActionBar = useNoActionBar;
    }

    private void initializeComponents() {
        // Initialize executor service
        executorService = Executors.newCachedThreadPool();

        // Initialize data
        conversations = new ArrayList<>();

        // Find views
        conversationsRecyclerView = findViewById(R.id.conversations_recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        progressBar = findViewById(R.id.progress_bar);
        emptyStateTextView = findViewById(R.id.empty_state_text_view);
        newMessageFab = findViewById(R.id.new_message_fab);
        drawerLayout = findViewById(R.id.drawer_layout);

        // Set up RecyclerView
        setupRecyclerView();

        // Set up SwipeRefreshLayout
        // No need to call showLoadingIndicator here as the SwipeRefreshLayout
        // automatically shows its spinner when pulled
        swipeRefreshLayout.setOnRefreshListener(this::refreshConversations);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorAccent,
                R.color.colorPrimaryDark);

        // Set up FAB
        newMessageFab.setOnClickListener(v -> startNewMessageActivity());

        // Set up navigation drawer
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Update navigation drawer header with app info
        View headerView = navigationView.getHeaderView(0);
        TextView appNameTextView = headerView.findViewById(R.id.app_name_text_view);
        TextView appVersionTextView = headerView.findViewById(R.id.app_version_text_view);

        if (appNameTextView != null) {
            appNameTextView.setText(getString(R.string.app_name));
        }

        if (appVersionTextView != null) {
            try {
                String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                appVersionTextView.setText(getString(R.string.app_version));
            } catch (PackageManager.NameNotFoundException e) {
                appVersionTextView.setText("Version Unknown");
            }
        }
    }

    private void setupRecyclerView() {
        // Set layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        conversationsRecyclerView.setLayoutManager(layoutManager);

        // Initialize adapter with empty list
        conversationAdapter = new ConversationRecyclerAdapter(this, conversations);

        // Set click listener
        conversationAdapter.setConversationClickListener(new ConversationRecyclerAdapter.ConversationClickListener() {
            @Override
            public void onConversationClick(Conversation conversation, int position) {
                openConversation(conversation);
            }

            @Override
            public void onConversationLongClick(Conversation conversation, int position) {
                showConversationOptions(conversation, position);
            }
        });

        // Set adapter to RecyclerView
        conversationsRecyclerView.setAdapter(conversationAdapter);
    }

    private void setupStandardToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up drawer toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void checkPermissions() {
        // Check if we have SMS permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            // Request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_SMS,
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.READ_CONTACTS
                    },
                    PERMISSIONS_REQUEST_READ_SMS);
        } else {
            // We already have permission, load conversations
            loadConversations();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_SMS) {
            // Check if the permission was granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, load conversations
                loadConversations();
            } else {
                // Permission denied, show error message
                Toast.makeText(this, getString(R.string.sms_permission_required),
                        Toast.LENGTH_LONG).show();
                emptyStateTextView.setText(R.string.permission_denied);
                emptyStateTextView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void loadConversations() {
        // Check if messageService is available
        if (messageService == null) {
            Log.e(TAG, "MessageService is null, cannot load conversations");
            runOnUiThread(() -> {
                hideLoadingIndicator();
                emptyStateTextView.setText("Service unavailable");
                emptyStateTextView.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Message service unavailable", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        // Only show the center progress bar if SwipeRefreshLayout isn't already refreshing
        if (!swipeRefreshLayout.isRefreshing()) {
            showLoadingIndicator(false);
        }
        
        // Use a background thread to load conversations
        executorService.execute(() -> {
            try {
                // Debug SMS content provider access
                debugSmsContentProvider();

                // Load conversations using MessageService
                List<Conversation> loadedConversations = messageService.loadConversations();

                // Update UI on main thread
                runOnUiThread(() -> {
                    // Clear existing conversations and add loaded ones
                    conversations.clear();
                    if (loadedConversations != null) {
                        conversations.addAll(loadedConversations);
                    }

                    // Update UI
                    conversationAdapter.notifyDataSetChanged();
                    hideLoadingIndicator();

                    // Show empty state if no conversations
                    if (conversations.isEmpty()) {
                        emptyStateTextView.setText(R.string.no_conversations);
                        emptyStateTextView.setVisibility(View.VISIBLE);
                    } else {
                        emptyStateTextView.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading conversations", e);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                            getString(R.string.error_loading_conversations) + ": " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    hideLoadingIndicator();
                    emptyStateTextView.setText(R.string.error_loading_conversations);
                    emptyStateTextView.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void debugSmsContentProvider() {
        Log.d(TAG, "Debugging SMS content provider access");

        try {
            // Test direct access to SMS conversations
            Uri uri = Uri.parse("content://sms/conversations");
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);

            if (cursor != null) {
                Log.d(TAG, "SMS conversations query returned " + cursor.getCount() + " rows");

                if (cursor.moveToFirst()) {
                    Log.d(TAG, "First conversation found");

                    // Log all column names
                    String[] columnNames = cursor.getColumnNames();
                    StringBuilder columns = new StringBuilder("Columns: ");
                    for (String name : columnNames) {
                        columns.append(name).append(", ");
                    }
                    Log.d(TAG, columns.toString());

                    // Try to get thread_id
                    int threadIdIndex = cursor.getColumnIndex("thread_id");
                    if (threadIdIndex >= 0) {
                        String threadId = cursor.getString(threadIdIndex);
                        Log.d(TAG, "Thread ID: " + threadId);

                        // Now try to get messages for this thread
                        debugThreadMessages(threadId);
                    } else {
                        Log.e(TAG, "thread_id column not found");
                    }
                } else {
                    Log.d(TAG, "No conversations found");
                }

                cursor.close();
            } else {
                Log.e(TAG, "Cursor is null when querying SMS conversations");
            }

            // Also test direct access to SMS inbox
            testSmsInbox();

        } catch (Exception e) {
            Log.e(TAG, "Error debugging SMS content provider", e);
        }
    }

    private void debugThreadMessages(String threadId) {
        try {
            Uri uri = Uri.parse("content://sms");
            String selection = "thread_id = ?";
            String[] selectionArgs = { threadId };
            Cursor cursor = getContentResolver().query(uri, null, selection, selectionArgs, null);

            if (cursor != null) {
                Log.d(TAG, "Thread " + threadId + " has " + cursor.getCount() + " messages");
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying thread messages", e);
        }
    }

    private void testSmsInbox() {
        try {
            Uri uri = Uri.parse("content://sms/inbox");
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);

            if (cursor != null) {
                Log.d(TAG, "SMS inbox has " + cursor.getCount() + " messages");
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying SMS inbox", e);
        }
    }

    private void refreshConversations() {
        // Clear cache to force reload
        MessageCache.clearCache();

        // No need to show loading indicator here if it's a pull-to-refresh
        // as the SwipeRefreshLayout already shows its spinner

        // Load conversations
        loadConversations();
    }

    /**
     * Shows a loading indicator based on the trigger type
     * @param isUserInitiated true if refresh was initiated by user (pull-to-refresh)
     */
    private void showLoadingIndicator(boolean isUserInitiated) {
        if (isUserInitiated) {
            // For user-initiated refreshes (pull-to-refresh), use SwipeRefreshLayout
            swipeRefreshLayout.setRefreshing(true);
            progressBar.setVisibility(View.GONE);
        } else {
            // For automatic/programmatic refreshes, use ProgressBar
            progressBar.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void hideLoadingIndicator() {
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void startNewMessageActivity() {
        Intent intent = new Intent(this, NewMessageActivity.class);
        startActivity(intent);
    }

    private void openConversation(Conversation conversation) {
        // Open the conversation
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra("thread_id", conversation.getThreadId());
        intent.putExtra("address", conversation.getAddress());
        intent.putExtra("contact_name", conversation.getContactName());
        startActivity(intent);
    }

    private void showConversationOptions(Conversation conversation, int position) {
        // Show options for the conversation (e.g., delete, mark as read)
        new AlertDialog.Builder(this)
                .setTitle(R.string.conversation_options)
                .setItems(new String[]{
                        getString(R.string.action_delete),
                        getString(R.string.action_mark_read),
                        getString(R.string.action_call)
                }, (dialog, which) -> {
                    switch (which) {
                        case 0: // Delete
                            confirmDeleteConversation(conversation, position);
                            break;
                        case 1: // Mark as read
                            markConversationAsRead(conversation, position);
                            break;
                        case 2: // Call contact
                            callContact(conversation.getAddress());
                            break;
                    }
                })
                .show();
    }

    private void confirmDeleteConversation(Conversation conversation, int position) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_conversation_title)
                .setMessage(R.string.delete_conversation_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    deleteConversation(conversation, position);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteConversation(Conversation conversation, int position) {
        // Show loading indicator
        showLoadingIndicator(false);

        // Delete conversation in background
        executorService.execute(() -> {
            try {
                boolean success = messageService.deleteConversation(conversation.getThreadId());

                runOnUiThread(() -> {
                    hideLoadingIndicator();

                    if (success) {
                        // Remove from list and update UI
                        conversations.remove(position);
                        conversationAdapter.notifyDataSetChanged();

                        Toast.makeText(MainActivity.this, R.string.conversation_deleted, Toast.LENGTH_SHORT).show();

                        // Show empty state if no conversations left
                        if (conversations.isEmpty()) {
                            emptyStateTextView.setText(R.string.no_conversations);
                            emptyStateTextView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Toast.makeText(MainActivity.this, R.string.delete_conversation_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error deleting conversation", e);
                runOnUiThread(() -> {
                    hideLoadingIndicator();
                    Toast.makeText(MainActivity.this,
                            getString(R.string.error_deleting_conversation) + ": " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void markConversationAsRead(Conversation conversation, int position) {
        executorService.execute(() -> {
            try {
                boolean success = messageService.markThreadAsRead(conversation.getThreadId());

                runOnUiThread(() -> {
                    if (success) {
                        conversation.setUnreadCount(0);
                        conversationAdapter.notifyItemChanged(position);
                        Toast.makeText(MainActivity.this, R.string.marked_as_read, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, R.string.mark_as_read_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error marking conversation as read", e);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                            getString(R.string.error_generic) + ": " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void callContact(String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(android.net.Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error initiating call", e);
            Toast.makeText(this, getString(R.string.error_initiating_call) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        
        // Add search action to the toolbar
        MenuItem searchItem = menu.add(Menu.NONE, R.id.action_search, Menu.NONE, R.string.action_search);
        searchItem.setIcon(android.R.drawable.ic_menu_search);
        searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            refreshConversations();
            return true;
        } else if (id == R.id.action_settings) {
            // Open settings activity
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_test_message) {
            // Add a test message
            addTestMessage();
            return true;
        } else if (id == R.id.action_default_sms) {
            // Request to be the default SMS app
            if (defaultSmsAppManager != null) {
                defaultSmsAppManager.requestDefaultSmsApp(this, SMS_REQUEST_CODE);
            } else {
                Toast.makeText(this, "SMS manager unavailable", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (id == R.id.action_search) {
            // Open search activity
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void addTestMessage() {
        try {
            // Check if services are available
            if (defaultSmsAppManager == null) {
                Toast.makeText(this, "SMS manager unavailable", Toast.LENGTH_SHORT).show();
                return;
            }

            if (messageService == null) {
                Toast.makeText(this, "Message service unavailable", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if we're the default SMS app
            if (!defaultSmsAppManager.isDefaultSmsApp()) {
                Toast.makeText(this,
                        R.string.default_sms_required,
                        Toast.LENGTH_LONG).show();
                defaultSmsAppManager.requestDefaultSmsApp(this, SMS_REQUEST_CODE);
                return;
            }

            // Use the MessageService to add a test message
            boolean success = messageService.addTestMessage();

            if (success) {
                Toast.makeText(this, R.string.test_message_added, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Added test message");

                // Refresh conversations
                refreshConversations();
            } else {
                Toast.makeText(this, R.string.test_message_failed, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Failed to add test message");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding test message", e);
            Toast.makeText(this, getString(R.string.error_generic) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addDirectSmsMessage() {
        try {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(Telephony.Sms.ADDRESS, "1234567890");
            values.put(Telephony.Sms.BODY, "Test message created at " + new java.util.Date());
            values.put(Telephony.Sms.DATE, System.currentTimeMillis());
            values.put(Telephony.Sms.READ, 0);
            values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX);

            Uri uri = getContentResolver().insert(Telephony.Sms.CONTENT_URI, values);

            if (uri != null) {
                Log.d(TAG, "Direct SMS message created: " + uri);
                Toast.makeText(this, "Test message created", Toast.LENGTH_SHORT).show();
                refreshConversations();
            } else {
                Log.e(TAG, "Failed to create direct SMS message");
                Toast.makeText(this, "Failed to create test message", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating direct SMS message", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Already on home screen, do nothing
        } else if (id == R.id.nav_settings) {
            // Open settings activity
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_about) {
            // Show about dialog
            showAboutDialog();
        } else if (id == R.id.nav_default_sms) {
            // Request to be the default SMS app
            if (defaultSmsAppManager != null) {
                defaultSmsAppManager.requestDefaultSmsApp(this, SMS_REQUEST_CODE);
            } else {
                Toast.makeText(this, "SMS manager unavailable", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.nav_test_translation) {
            // Show translation test dialog
            showTranslationTestDialog();
        }

        // Close the drawer
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showAboutDialog() {
        String versionName;
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "Unknown";
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.about)
                .setMessage(getString(R.string.app_name) + " v" + versionName + "\n\n" +
                        getString(R.string.about_message))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showTranslationTestDialog() {
        // Check if we have a valid API key
        if (!((TranslatorApp) getApplication()).hasValidApiKey()) {
            Toast.makeText(this, R.string.api_key_required,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Show a dialog with translation test options
        new AlertDialog.Builder(this)
                .setTitle(R.string.test_translation)
                .setItems(new String[]{
                        getString(R.string.english_to_spanish),
                        getString(R.string.spanish_to_english),
                        getString(R.string.auto_detect)
                }, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            testTranslation("Hello, how are you?", "en", "es");
                            break;
                        case 1:
                            testTranslation("Hola, ¿cómo estás?", "es", "en");
                            break;
                        case 2:
                            testAutoDetectTranslation("Bonjour, comment ça va?");
                            break;
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void testTranslation(String text, String sourceLanguage, String targetLanguage) {
        if (translationManager == null) {
            Toast.makeText(this, "Translation service unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, getString(R.string.translating_text, text), Toast.LENGTH_SHORT).show();

        // Use TranslationManager to translate
        translationManager.translateText(text, targetLanguage, (success, translatedText, errorMessage) -> {
            if (success && translatedText != null) {
                runOnUiThread(() -> {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.translation_result)
                            .setMessage(getString(R.string.original_text_format,
                                    translationManager.getLanguageName(sourceLanguage), text) + "\n\n" +
                                    getString(R.string.translated_text_format,
                                            translationManager.getLanguageName(targetLanguage), translatedText))
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                            getString(R.string.translation_error) + ": " +
                                    (errorMessage != null ? errorMessage : getString(R.string.unknown_error)),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void testAutoDetectTranslation(String text) {
        if (translationManager == null) {
            Toast.makeText(this, "Translation service unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, getString(R.string.auto_detecting_text, text), Toast.LENGTH_SHORT).show();

        // Get preferred language
        String targetLanguage = userPreferences.getPreferredLanguage();

        // Use TranslationManager to translate
        translationManager.translateText(text, targetLanguage, (success, translatedText, errorMessage) -> {
            if (success && translatedText != null) {
                runOnUiThread(() -> {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.auto_detect_result)
                            .setMessage(getString(R.string.original_text) + ": " + text + "\n\n" +
                                    getString(R.string.translated_to_format,
                                            translationManager.getLanguageName(targetLanguage), translatedText))
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                            getString(R.string.translation_error) + ": " +
                                    (errorMessage != null ? errorMessage : getString(R.string.unknown_error)),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SMS_REQUEST_CODE) {
            Log.d(TAG, "Received result from default SMS app request: " + resultCode);

            // Check if we're now the default SMS app
            if (defaultSmsAppManager != null && defaultSmsAppManager.isDefaultSmsApp()) {
                Toast.makeText(this, "Successfully set as default SMS app", Toast.LENGTH_SHORT).show();
                loadConversations();
            } else {
                // If we're not the default SMS app, show a message
                Toast.makeText(this,
                        "This app needs to be the default SMS app to function properly.",
                        Toast.LENGTH_LONG).show();

                // Try to load conversations anyway
                loadConversations();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if we're the default SMS app
        if (defaultSmsAppManager == null || !defaultSmsAppManager.isDefaultSmsApp()) {
            checkDefaultSmsAppStatus();
        } else {
            // Refresh conversations when returning to the activity
            refreshConversations();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}