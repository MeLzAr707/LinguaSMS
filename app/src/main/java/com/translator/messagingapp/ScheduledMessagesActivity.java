package com.translator.messagingapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Activity to view, edit, and delete scheduled messages.
 */
public class ScheduledMessagesActivity extends BaseActivity {
    private static final String TAG = "ScheduledMessagesActivity";
    private static final int REQUEST_EDIT_MESSAGE = 1001;

    private RecyclerView recyclerView;
    private ScheduledMessageAdapter adapter;
    private ScheduledMessageManager scheduledMessageManager;
    private View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scheduled_messages);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Scheduled Messages");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize services
        scheduledMessageManager = ((TranslatorApp) getApplication()).getScheduledMessageManager();

        // Initialize views
        recyclerView = findViewById(R.id.recycler_view);
        emptyView = findViewById(R.id.empty_view);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScheduledMessageAdapter(this::onMessageClicked, this::onDeleteClicked);
        recyclerView.setAdapter(adapter);

        // Load scheduled messages
        loadScheduledMessages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list when returning to this activity
        loadScheduledMessages();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadScheduledMessages() {
        try {
            List<ScheduledMessage> pendingMessages = scheduledMessageManager.getPendingScheduledMessages();
            
            if (pendingMessages.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                adapter.updateMessages(pendingMessages);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading scheduled messages", e);
            Toast.makeText(this, "Error loading scheduled messages", Toast.LENGTH_SHORT).show();
        }
    }

    private void onMessageClicked(ScheduledMessage message) {
        // Open edit dialog or new message activity for editing
        Intent intent = new Intent(this, NewMessageActivity.class);
        intent.putExtra("edit_scheduled_message", message);
        startActivityForResult(intent, REQUEST_EDIT_MESSAGE);
    }

    private void onDeleteClicked(ScheduledMessage message) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Scheduled Message")
            .setMessage("Are you sure you want to delete this scheduled message?")
            .setPositiveButton("Delete", (dialog, which) -> {
                try {
                    boolean success = scheduledMessageManager.cancelScheduledMessage(message.getId());
                    if (success) {
                        Toast.makeText(this, "Scheduled message deleted", Toast.LENGTH_SHORT).show();
                        loadScheduledMessages(); // Refresh the list
                    } else {
                        Toast.makeText(this, "Failed to delete scheduled message", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error deleting scheduled message", e);
                    Toast.makeText(this, "Error deleting scheduled message", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT_MESSAGE && resultCode == RESULT_OK) {
            // Refresh the list after editing
            loadScheduledMessages();
        }
    }
}