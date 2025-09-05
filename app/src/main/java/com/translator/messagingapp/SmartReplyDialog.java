package com.translator.messagingapp;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Dialog for displaying Smart Reply suggestions.
 */
public class SmartReplyDialog {
    
    public interface SmartReplyCallback {
        void onReplySelected(String reply);
        void onDialogCancelled();
    }
    
    private final Context context;
    private final OnDeviceMLService mlService;
    private AlertDialog dialog;
    private ProgressBar progressBar;
    private TextView emptyMessage;
    private LinearLayout suggestionsContainer;
    private SmartReplyCallback callback;
    
    public SmartReplyDialog(Context context, OnDeviceMLService mlService) {
        this.context = context;
        this.mlService = mlService;
    }
    
    public void show(List<Message> conversationMessages, SmartReplyCallback callback) {
        this.callback = callback;
        
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_smart_reply, null);
        
        // Initialize views
        progressBar = dialogView.findViewById(R.id.progress_bar);
        emptyMessage = dialogView.findViewById(R.id.empty_message);
        suggestionsContainer = dialogView.findViewById(R.id.suggestions_container);
        Button cancelButton = dialogView.findViewById(R.id.btn_cancel);
        
        // Set up cancel button
        cancelButton.setOnClickListener(v -> {
            if (this.callback != null) {
                this.callback.onDialogCancelled();
            }
            dialog.dismiss();
        });
        
        // Create and show dialog
        dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .setOnCancelListener(d -> {
                    if (this.callback != null) {
                        this.callback.onDialogCancelled();
                    }
                })
                .create();
        
        dialog.show();
        
        // Start generating smart replies
        generateSmartReplies(conversationMessages);
    }
    
    private void generateSmartReplies(List<Message> conversationMessages) {
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        emptyMessage.setVisibility(View.GONE);
        suggestionsContainer.removeAllViews();
        
        mlService.generateSmartReplies(conversationMessages, new OnDeviceMLService.SmartReplyCallback() {
            @Override
            public void onSmartReplyComplete(boolean success, List<String> suggestions, String errorMessage) {
                // Update UI on main thread
                if (context instanceof ConversationActivity) {
                    ((ConversationActivity) context).runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        
                        if (success && suggestions != null && !suggestions.isEmpty()) {
                            displaySuggestions(suggestions);
                        } else {
                            emptyMessage.setVisibility(View.VISIBLE);
                            if (errorMessage != null) {
                                Toast.makeText(context, 
                                    context.getString(R.string.smart_reply_error, errorMessage), 
                                    Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        });
    }
    
    private void displaySuggestions(List<String> suggestions) {
        suggestionsContainer.removeAllViews();
        
        for (String suggestion : suggestions) {
            Button suggestionButton = new Button(context);
            suggestionButton.setText(suggestion);
            suggestionButton.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            
            // Style the button
            suggestionButton.setBackgroundResource(android.R.drawable.btn_default);
            suggestionButton.setPadding(16, 12, 16, 12);
            
            // Set click listener
            suggestionButton.setOnClickListener(v -> {
                if (callback != null) {
                    callback.onReplySelected(suggestion);
                }
                dialog.dismiss();
            });
            
            // Add margin between buttons
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) suggestionButton.getLayoutParams();
            params.topMargin = 8;
            params.bottomMargin = 8;
            suggestionButton.setLayoutParams(params);
            
            suggestionsContainer.addView(suggestionButton);
        }
    }
}