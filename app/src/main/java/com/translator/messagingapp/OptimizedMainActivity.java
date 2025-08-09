package com.translator.messagingapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
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

            // Check if we're the default SMS app
            if (UserPreferences.shouldCheckDefaultSmsApp(this)) {
                if (UserPreferences.isFirstRun(this)) {
                    // First run - force the default SMS app check
                    DefaultSmsAppManager.forceDefaultSmsApp(this, () -> {
                        loadConversationsOptimized();
                    });
                } else {
                    // Normal check - respect the request count limits
                    DefaultSmsAppManager.checkAndRequestDefaultSmsApp(this, SMS_REQUEST_CODE, () -> {
                        // This will run if we're already the default SMS app
                        loadConversationsOptimized();
                    });
                }
            } else {
                loadConversationsOptimized();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing app", Toast.LENGTH_SHORT).show();
        }
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

    // Rest of the MainActivity implementation remains the same
    // ...
}