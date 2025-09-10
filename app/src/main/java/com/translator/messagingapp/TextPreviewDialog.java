package com.translator.messagingapp;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * Dialog for previewing rewritten text before sending
 */
public class TextPreviewDialog {
    
    /**
     * Interface for handling preview actions
     */
    public interface OnPreviewActionListener {
        void onSendConfirmed(String finalText);
        void onEditRequested();
        void onDialogCancelled();
    }
    
    private final Context context;
    private final String originalText;
    private final String rewrittenText;
    private final TextRewriterService.Tone selectedTone;
    private final OnPreviewActionListener listener;
    private AlertDialog dialog;
    
    /**
     * Creates a new TextPreviewDialog
     *
     * @param context The context for creating the dialog
     * @param originalText The original text
     * @param rewrittenText The rewritten text
     * @param selectedTone The tone that was applied
     * @param listener Listener for preview actions
     */
    public TextPreviewDialog(Context context, String originalText, String rewrittenText, 
                           TextRewriterService.Tone selectedTone, OnPreviewActionListener listener) {
        this.context = context;
        this.originalText = originalText;
        this.rewrittenText = rewrittenText;
        this.selectedTone = selectedTone;
        this.listener = listener;
    }
    
    /**
     * Shows the preview dialog
     */
    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Preview " + selectedTone.getDisplayName() + " Message");
        
        // Create custom layout for preview
        View dialogView = createDialogView();
        builder.setView(dialogView);
        
        builder.setPositiveButton("Send", (dialog, which) -> {
            if (listener != null) {
                listener.onSendConfirmed(rewrittenText);
            }
        });
        
        builder.setNegativeButton("Edit", (dialog, which) -> {
            if (listener != null) {
                listener.onEditRequested();
            }
        });
        
        builder.setNeutralButton("Cancel", (dialog, which) -> {
            if (listener != null) {
                listener.onDialogCancelled();
            }
        });
        
        dialog = builder.create();
        dialog.show();
    }
    
    /**
     * Creates the dialog view with text preview
     */
    private View createDialogView() {
        // Create container programmatically
        android.widget.LinearLayout container = new android.widget.LinearLayout(context);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(24, 16, 24, 16);
        
        // Original text label
        TextView originalLabel = new TextView(context);
        originalLabel.setText("Original:");
        originalLabel.setTextSize(12);
        originalLabel.setAlpha(0.6f);
        originalLabel.setPadding(0, 0, 0, 4);
        
        // Original text
        TextView originalTextView = new TextView(context);
        originalTextView.setText("\"" + originalText + "\"");
        originalTextView.setTextSize(14);
        originalTextView.setAlpha(0.7f);
        originalTextView.setPadding(8, 0, 8, 16);
        originalTextView.setBackgroundColor(0x0F000000); // Light gray background
        
        // Rewritten text label
        TextView rewrittenLabel = new TextView(context);
        rewrittenLabel.setText(selectedTone.getDisplayName() + " Version:");
        rewrittenLabel.setTextSize(12);
        rewrittenLabel.setAlpha(0.6f);
        rewrittenLabel.setPadding(0, 8, 0, 4);
        
        // Rewritten text
        TextView rewrittenTextView = new TextView(context);
        rewrittenTextView.setText("\"" + rewrittenText + "\"");
        rewrittenTextView.setTextSize(16);
        rewrittenTextView.setPadding(8, 8, 8, 8);
        rewrittenTextView.setBackgroundColor(0x0F0066CC); // Light blue background
        
        // Add all views to container
        container.addView(originalLabel);
        container.addView(originalTextView);
        container.addView(rewrittenLabel);
        container.addView(rewrittenTextView);
        
        return container;
    }
    
    /**
     * Dismisses the dialog
     */
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}