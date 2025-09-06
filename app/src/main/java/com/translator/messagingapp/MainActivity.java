package com.translator.messagingapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST_READ_SMS = 1001;
    private static final int SMS_REQUEST_CODE = 2001;

    // UI components
    private RecyclerView conversationsRecyclerView;
    private ConversationRecyclerAdapter conversationAdapter;
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
    
    // Optimized services for better performance
    private OptimizedConversationService optimizedConversationService;
    
    // Pagination state
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMoreConversations = true;
    private PaginationUtils.PaginationScrollListener paginationScrollListener;
    
    // Message refresh receiver
    private BroadcastReceiver messageRefreshReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set to use NoActionBar variant since we use custom toolbar
        setUseNoActionBar(true);
        
        super.onCreate(savedInstanceState);

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
            
            // Set up message refresh receiver
            setupMessageRefreshReceiver();
            
            // Apply custom colors if using custom theme
            applyCustomColorsToViews();

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
     * Check battery optimization status and request whitelist if needed
     */
    private void checkBatteryOptimizationStatus() {
        if (PhoneUtils.shouldRequestBatteryOptimizationWhitelist(this)) {
            Log.d(TAG, "Should request battery optimization whitelist");
            PhoneUtils.showBatteryOptimizationDialog(this);
        } else if (PhoneUtils.isIgnoringBatteryOptimizations(this)) {
            Log.d(TAG, "App is already ignoring battery optimizations");
        } else {
            Log.d(TAG, "Battery optimization whitelist not needed or user declined");
        }
    }

    /**
     * Set up broadcast receiver for message refresh events.
     * Uses proper Android 13+ RECEIVER_EXPORTED/RECEIVER_NOT_EXPORTED flags.
     */
    private void setupMessageRefreshReceiver() {
        try {
            // Create the broadcast receiver
            messageRefreshReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "Message refresh broadcast received: " + intent.getAction());
                    
                    // Handle different refresh actions
                    if (intent != null && intent.getAction() != null) {
                        switch (intent.getAction()) {
                            case "com.translator.messagingapp.REFRESH_MESSAGES":
                                // Refresh conversations when new messages arrive
                                refreshConversations();
                                break;
                            case "com.translator.messagingapp.MESSAGE_RECEIVED":
                                // Refresh conversations when new messages are received
                                refreshConversations();
                                break;
                            case "com.translator.messagingapp.MESSAGE_SENT":
                                // Update UI when message is sent
                                refreshConversations();
                                break;
                            case "com.translator.messagingapp.MESSAGE_TRANSLATED":
                                // Update UI when auto-translation completes
                                refreshConversations();
                                break;
                            default:
                                Log.d(TAG, "Unknown refresh action: " + intent.getAction());
                                break;
                        }
                    }
                }
            };

            // Create intent filter for message refresh events
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.translator.messagingapp.REFRESH_MESSAGES");
            filter.addAction("com.translator.messagingapp.MESSAGE_RECEIVED");
            filter.addAction("com.translator.messagingapp.MESSAGE_SENT");
            filter.addAction("com.translator.messagingapp.MESSAGE_TRANSLATED");

            // Register receiver with LocalBroadcastManager for intra-app communication
            LocalBroadcastManager.getInstance(this).registerReceiver(messageRefreshReceiver, filter);
            
            // Also register with system broadcast for fallback
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ requires explicit RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED
                registerReceiver(messageRefreshReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                // Pre-Android 13 registration
                registerReceiver(messageRefreshReceiver, filter);
            }
            
            Log.d(TAG, "Message refresh receiver registered successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up message refresh receiver", e);
            // Don't throw the exception to prevent app crash
        }
    }

    /**
     * Send a broadcast to refresh messages. This can be called from other components
     * to trigger UI refresh when messages are updated.
     */
    public static void sendMessageRefreshBroadcast(Context context, String action) {
        try {
            Intent refreshIntent = new Intent(action);
            context.sendBroadcast(refreshIntent);
            Log.d("MainActivity", "Sent message refresh broadcast: " + action);
        } catch (Exception e) {
            Log.e("MainActivity", "Error sending message refresh broadcast", e);
        }
    }

    private void initializeComponents() {
        // Initialize executor service
        executorService = Executors.newCachedThreadPool();

        // Initialize data
        conversations = new ArrayList<>();

        // Initialize optimized conversation service
        optimizedConversationService = new OptimizedConversationService(this);

        // Find views
        conversationsRecyclerView = findViewById(R.id.conversations_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        emptyStateTextView = findViewById(R.id.empty_state_text_view);
        newMessageFab = findViewById(R.id.new_message_fab);
        drawerLayout = findViewById(R.id.drawer_layout);

        // Set up RecyclerView with pagination
        setupRecyclerView();

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

        // Apply theme-specific styling to navigation header
        if (headerView != null && userPreferences != null && userPreferences.isUsingBlackGlassTheme()) {
            headerView.setBackgroundColor(getResources().getColor(R.color.deep_dark_blue));
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
        
        // Set up pagination for smoother loading
        setupPagination(layoutManager);
    }
    
    /**
     * Sets up pagination for the conversation list to improve loading performance.
     */
    private void setupPagination(LinearLayoutManager layoutManager) {
        paginationScrollListener = PaginationUtils.setupPagination(
            conversationsRecyclerView,
            () -> {
                if (!isLoading && hasMoreConversations) {
                    loadMoreConversations();
                }
            },
            5, // Load more when 5 items from bottom
            progressBar
        );
    }

    private void setupStandardToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        
        // Apply theme-specific toolbar colors
        updateToolbarTheme(toolbar);
        
        setSupportActionBar(toolbar);

        // Set up drawer toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    /**
     * Update toolbar colors based on current theme
     */
    private void updateToolbarTheme(Toolbar toolbar) {
        if (userPreferences != null && userPreferences.isUsingBlackGlassTheme()) {
            // Use deep dark blue for Black Glass theme
            toolbar.setBackgroundColor(getResources().getColor(R.color.deep_dark_blue));
        } else {
            // Use default primary color for other themes
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }
    }
    
    @Override
    protected void onThemeChanged() {
        super.onThemeChanged();
        
        // Update toolbar colors when theme changes
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            updateToolbarTheme(toolbar);
        }
        
        // Update navigation view theme if needed
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null && userPreferences != null && userPreferences.isUsingBlackGlassTheme()) {
            // Apply Black Glass theme to navigation view header
            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                headerView.setBackgroundColor(getResources().getColor(R.color.deep_dark_blue));
            }
        }
        
        // Apply custom theme colors if using custom theme
        applyCustomThemeColors();
    }
    
    /**
     * Apply custom theme colors to UI elements
     */
    private void applyCustomThemeColors() {
        if (userPreferences != null && userPreferences.isUsingCustomTheme()) {
            int defaultColor = getResources().getColor(android.R.color.holo_blue_dark);
            int customNavBarColor = userPreferences.getCustomNavBarColor(defaultColor);
            
            // Apply custom navigation bar color
            NavigationView navigationView = findViewById(R.id.nav_view);
            if (navigationView != null) {
                View headerView = navigationView.getHeaderView(0);
                if (headerView != null) {
                    headerView.setBackgroundColor(customNavBarColor);
                }
            }
            
            // Apply custom color to FAB
            FloatingActionButton fab = findViewById(R.id.fab);
            if (fab == null) {
                // Try to find with the actual variable name
                fab = newMessageFab;
            }
            if (fab != null) {
                fab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customNavBarColor));
            }
        }
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
        // Reset pagination state for initial load
        currentPage = 0;
        hasMoreConversations = true;
        
        // Try to show cached conversations first to minimize blank state
        if (optimizedConversationService != null) {
            List<Conversation> cachedConversations = optimizedConversationService.getCachedConversations();
            if (!cachedConversations.isEmpty()) {
                Log.d(TAG, "Showing " + cachedConversations.size() + " cached conversations during initial load");
                // Update UI with cached data immediately
                conversationAdapter.updateConversations(new ArrayList<>(cachedConversations));
                // Update our local list
                conversations.clear();
                conversations.addAll(cachedConversations);
            } else {
                // Clear existing conversations for fresh load if no cache available
                conversationAdapter.updateConversations(new ArrayList<>());
            }
        } else {
            // Clear existing conversations for fresh load if no optimized service
            conversationAdapter.updateConversations(new ArrayList<>());
        }
        
        // Load first page of conversations
        loadMoreConversations();
    }
    
    /**
     * Loads the next page of conversations using optimized pagination.
     */
    private void loadMoreConversations() {
        if (isLoading || !hasMoreConversations) {
            return;
        }
        
        // Check if optimized service is available, fallback to original if needed
        if (optimizedConversationService == null) {
            loadConversationsFallback();
            return;
        }
        
        isLoading = true;
        
        // Show loading indicator only for initial load
        if (currentPage == 0) {
            showLoadingIndicator();
        }
        
        // Calculate offset for pagination
        int offset = currentPage * OptimizedConversationService.DEFAULT_PAGE_SIZE;
        
        // Load conversations using optimized service with pagination
        optimizedConversationService.loadConversationsPaginated(
            offset, 
            OptimizedConversationService.DEFAULT_PAGE_SIZE,
            new OptimizedConversationService.ConversationLoadCallback() {
                @Override
                public void onConversationsLoaded(List<Conversation> loadedConversations, boolean hasMore) {
                    runOnUiThread(() -> {
                        // Add new conversations to existing list
                        int oldSize = conversations.size();
                        conversations.addAll(loadedConversations);
                        
                        // Notify adapter of new items
                        if (currentPage == 0) {
                            // First load - use DiffUtil for efficient updates
                            conversationAdapter.updateConversations(new ArrayList<>(conversations));
                        } else {
                            // Subsequent loads - notify items inserted
                            conversationAdapter.notifyItemRangeInserted(oldSize, loadedConversations.size());
                        }
                        
                        // Update pagination state
                        currentPage++;
                        hasMoreConversations = hasMore;
                        isLoading = false;
                        
                        // Hide loading indicator
                        if (currentPage == 1) { // First page completed
                            hideLoadingIndicator();
                        }
                        
                        // Show empty state if no conversations after first load
                        if (conversations.isEmpty() && currentPage == 1) {
                            emptyStateTextView.setText(R.string.no_conversations);
                            emptyStateTextView.setVisibility(View.VISIBLE);
                        } else {
                            emptyStateTextView.setVisibility(View.GONE);
                        }
                        
                        Log.d(TAG, "Loaded page " + (currentPage - 1) + " with " + 
                              loadedConversations.size() + " conversations. Total: " + conversations.size() + 
                              ", Has more: " + hasMore);
                    });
                }
                
                @Override
                public void onError(Exception error) {
                    Log.e(TAG, "Error loading conversations with optimized service", error);
                    runOnUiThread(() -> {
                        isLoading = false;
                        hideLoadingIndicator();
                        
                        // Show error message
                        Toast.makeText(MainActivity.this,
                                getString(R.string.error_loading_conversations) + ": " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        
                        // Show empty state with error
                        if (conversations.isEmpty()) {
                            emptyStateTextView.setText(R.string.error_loading_conversations);
                            emptyStateTextView.setVisibility(View.VISIBLE);
                        }
                        
                        // Fallback to original method if first page fails
                        if (currentPage == 0) {
                            Log.d(TAG, "Falling back to original conversation loading method");
                            loadConversationsFallback();
                        }
                    });
                }
            }
        );
    }
    
    /**
     * Fallback to original conversation loading method if optimized service fails.
     */
    private void loadConversationsFallback() {
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

        // Show loading indicator for fallback method
        showLoadingIndicator();
        
        // Use a background thread to load conversations
        executorService.execute(() -> {
            try {
                // Load conversations using original MessageService (limited to first 50 for performance)
                List<Conversation> loadedConversations = messageService.loadConversations();
                
                // Limit conversations for performance in fallback mode
                if (loadedConversations.size() > 50) {
                    loadedConversations = loadedConversations.subList(0, 50);
                    Log.d(TAG, "Limited fallback conversations to 50 for performance");
                }

                // Update UI on main thread
                List<Conversation> finalLoadedConversations = loadedConversations;
                runOnUiThread(() -> {
                    // Update UI with DiffUtil for better performance
                    List<Conversation> newConversations = new ArrayList<>();
                    if (finalLoadedConversations != null) {
                        newConversations.addAll(finalLoadedConversations);
                    }
                    conversationAdapter.updateConversations(newConversations);
                    hideLoadingIndicator();
                    isLoading = false;
                    hasMoreConversations = false; // No pagination in fallback mode

                    // Show empty state if no conversations
                    if (conversations.isEmpty()) {
                        emptyStateTextView.setText(R.string.no_conversations);
                        emptyStateTextView.setVisibility(View.VISIBLE);
                    } else {
                        emptyStateTextView.setVisibility(View.GONE);
                    }
                    
                    Log.d(TAG, "Fallback method loaded " + conversations.size() + " conversations");
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading conversations with fallback method", e);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                            getString(R.string.error_loading_conversations) + ": " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    hideLoadingIndicator();
                    isLoading = false;
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
        Log.d(TAG, "Refreshing conversations");
        
        // Try to show cached conversations first to avoid blank state
        if (optimizedConversationService != null) {
            List<Conversation> cachedConversations = optimizedConversationService.getCachedConversations();
            if (!cachedConversations.isEmpty()) {
                Log.d(TAG, "Showing " + cachedConversations.size() + " cached conversations instantly");
                // Update UI with cached data immediately to avoid blank state
                conversationAdapter.updateConversations(new ArrayList<>(cachedConversations));
                // Update our local list
                conversations.clear();
                conversations.addAll(cachedConversations);
            }
        }
        
        // Load fresh data in background without clearing the UI first
        loadConversationsInBackground();
    }
    
    /**
     * Loads conversations in background without clearing the UI first.
     * This prevents the blank state issue when refreshing conversations.
     */
    private void loadConversationsInBackground() {
        // Reset pagination state for fresh data load
        currentPage = 0;
        hasMoreConversations = true;
        
        // Load fresh data without clearing the UI first
        loadMoreConversationsInBackground();
    }
    
    /**
     * Loads more conversations in background and replaces existing data using DiffUtil.
     * This method doesn't clear the UI first, preventing blank state.
     */
    private void loadMoreConversationsInBackground() {
        if (isLoading || !hasMoreConversations) {
            return;
        }
        
        // Check if optimized service is available, fallback to original if needed
        if (optimizedConversationService == null) {
            loadConversationsFallback();
            return;
        }
        
        isLoading = true;
        
        // Clear cache to ensure fresh data only for background refresh
        MessageCache.clearCache();
        if (optimizedConversationService != null) {
            optimizedConversationService.clearCache();
        }
        
        // Calculate offset for pagination (start from 0 for refresh)
        int offset = currentPage * OptimizedConversationService.DEFAULT_PAGE_SIZE;
        
        // Load conversations using optimized service with pagination
        optimizedConversationService.loadConversationsPaginated(
            offset, 
            OptimizedConversationService.DEFAULT_PAGE_SIZE,
            new OptimizedConversationService.ConversationLoadCallback() {
                @Override
                public void onConversationsLoaded(List<Conversation> loadedConversations, boolean hasMore) {
                    runOnUiThread(() -> {
                        // For refresh, replace all conversations instead of adding
                        if (currentPage == 0) {
                            // Replace existing conversations with fresh data
                            List<Conversation> newConversations = new ArrayList<>(loadedConversations);
                            conversationAdapter.updateConversations(newConversations);
                            conversations.clear();
                            conversations.addAll(newConversations);
                        } else {
                            // Add new conversations to existing list (for pagination)
                            int oldSize = conversations.size();
                            conversations.addAll(loadedConversations);
                            conversationAdapter.notifyItemRangeInserted(oldSize, loadedConversations.size());
                        }
                        
                        // Update pagination state
                        currentPage++;
                        hasMoreConversations = hasMore;
                        isLoading = false;
                        
                        // Show empty state if no conversations after first load
                        if (conversations.isEmpty() && currentPage == 1) {
                            emptyStateTextView.setText(R.string.no_conversations);
                            emptyStateTextView.setVisibility(View.VISIBLE);
                        } else {
                            emptyStateTextView.setVisibility(View.GONE);
                        }
                        
                        Log.d(TAG, "Background loaded page " + (currentPage - 1) + " with " + 
                              loadedConversations.size() + " conversations. Total: " + conversations.size() + 
                              ", Has more: " + hasMore);
                    });
                }
                
                @Override
                public void onError(Exception error) {
                    Log.e(TAG, "Error loading conversations in background", error);
                    runOnUiThread(() -> {
                        isLoading = false;
                        
                        // Don't show error message for background refresh to avoid disrupting user
                        Log.w(TAG, "Background conversation refresh failed, keeping existing data");
                        
                        // Fallback to original method if first page fails
                        if (currentPage == 0 && conversations.isEmpty()) {
                            Log.d(TAG, "No cached data available, falling back to original conversation loading method");
                            loadConversationsFallback();
                        }
                    });
                }
            }
        );
    }

    /**
     * Shows the loading indicator
     */
    private void showLoadingIndicator() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideLoadingIndicator() {
        progressBar.setVisibility(View.GONE);
    }

    private void startNewMessageActivity() {
        Intent intent = new Intent(this, NewMessageActivity.class);
        if (!ReflectionUtils.safeStartActivity(this, intent)) {
            Toast.makeText(this, "Unable to create new message", Toast.LENGTH_SHORT).show();
        }
    }

    private void openConversation(Conversation conversation) {
        // Open the conversation with safe activity starting
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra("thread_id", conversation.getThreadId());
        intent.putExtra("address", conversation.getAddress());
        intent.putExtra("contact_name", conversation.getContactName());
        
        // Use safe activity starting to prevent crashes
        if (!ReflectionUtils.safeStartActivity(this, intent)) {
            // Fallback if safe start fails
            Toast.makeText(this, "Unable to open conversation", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Failed to open conversation for: " + conversation.getAddress());
        }
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
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    deleteConversation(conversation, position);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteConversation(Conversation conversation, int position) {
        // Show loading indicator
        showLoadingIndicator();

        // Delete conversation in background
        executorService.execute(() -> {
            try {
                boolean success = messageService.deleteConversation(conversation.getThreadId());

                runOnUiThread(() -> {
                    hideLoadingIndicator();

                    if (success) {
                        // Create updated list without deleted conversation
                        List<Conversation> updatedConversations = new ArrayList<>(conversations);
                        updatedConversations.remove(position);
                        conversationAdapter.updateConversations(updatedConversations);

                        Toast.makeText(MainActivity.this, R.string.conversation_deleted, Toast.LENGTH_SHORT).show();

                        // Show empty state if no conversations left
                        if (updatedConversations.isEmpty()) {
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
        TranslatorApp app = (TranslatorApp) getApplication();
        
        // Check if we have translation capability (either online or offline)
        if (!app.hasValidApiKey()) {
            // No translation capability available
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
        String targetLanguage = userPreferences != null ? userPreferences.getPreferredLanguage() : "en";

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
                // Reset the declined flag since user accepted
                PhoneUtils.setUserDeclinedDefaultSms(this, false);
                
                // Start message monitoring service for deep sleep handling
                MessageMonitoringService.startService(this);
                
                // Check and request battery optimization whitelist if needed
                checkBatteryOptimizationStatus();
                
                loadConversations();
            } else {
                // User declined or canceled the request
                Log.d(TAG, "User did not set app as default SMS app");
                
                // Check if we've reached the maximum number of requests
                int requestCount = PhoneUtils.getDefaultSmsRequestCount(this);
                if (requestCount >= 3) {
                    // User has been asked 3 times, stop asking
                    PhoneUtils.setUserDeclinedDefaultSms(this, true);
                    Log.d(TAG, "Maximum request count reached, setting user declined flag");
                }

                // Show a less intrusive message
                Toast.makeText(this,
                        "This app works best as the default SMS app, but you can still use basic features.",
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
            // Start message monitoring service if not already running
            MessageMonitoringService.startService(this);
            
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
        
        // Unregister message refresh receiver
        if (messageRefreshReceiver != null) {
            try {
                // Unregister from LocalBroadcastManager
                LocalBroadcastManager.getInstance(this).unregisterReceiver(messageRefreshReceiver);
                // Unregister from system broadcasts
                unregisterReceiver(messageRefreshReceiver);
                Log.d(TAG, "Message refresh receiver unregistered from both LocalBroadcastManager and system");
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering message refresh receiver", e);
            }
            messageRefreshReceiver = null;
        }
    }
    
    @Override
    protected void applyCustomColorsToViews() {
        super.applyCustomColorsToViews();
        
        if (userPreferences != null && userPreferences.isUsingCustomTheme()) {
            // Apply custom colors to navigation view and FAB
            int defaultColor = getResources().getColor(R.color.colorPrimary);
            int customNavBarColor = userPreferences.getCustomNavBarColor(defaultColor);
            
            // Apply to Navigation View header if present
            NavigationView navigationView = findViewById(R.id.nav_view);
            if (navigationView != null && navigationView.getHeaderView(0) != null) {
                navigationView.getHeaderView(0).setBackgroundColor(customNavBarColor);
            }
            
            // Apply to FloatingActionButton
            FloatingActionButton fab = findViewById(R.id.fab);
            if (fab != null) {
                fab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customNavBarColor));
            }
        }
    }
}