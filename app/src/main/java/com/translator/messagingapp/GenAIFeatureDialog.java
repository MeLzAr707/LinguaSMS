package com.translator.messagingapp;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Dialog for GenAI messaging features selection and display.
 */
public class GenAIFeatureDialog {
    
    /**
     * Callback interface for GenAI feature selection.
     */
    public interface GenAIFeatureCallback {
        void onFeatureSelected(String feature);
        void onDismissed();
    }
    
    /**
     * Shows a dialog with GenAI feature options for conversation actions.
     */
    public static void showConversationFeatures(Context context, GenAIFeatureCallback callback) {
        String[] features = {
            "Summarize Conversation",
            "Generate Smart Replies"
        };
        
        String[] descriptions = {
            "Get a summary of the main topics discussed in this conversation",
            "Get AI-suggested quick replies based on the conversation context"
        };
        
        showFeatureSelectionDialog(context, "AI Features", features, descriptions, callback);
    }
    
    /**
     * Shows a dialog with GenAI feature options for message composition.
     */
    public static void showCompositionFeatures(Context context, GenAIFeatureCallback callback) {
        String[] features = {
            "Proofread Message",
            "Rewrite - Elaborate",
            "Rewrite - Emojify", 
            "Rewrite - Shorten",
            "Rewrite - Friendly",
            "Rewrite - Professional",
            "Rewrite - Rephrase"
        };
        
        String[] descriptions = {
            "Check grammar, spelling, and clarity",
            "Expand with more details and descriptive language",
            "Add relevant emojis to make it more expressive",
            "Condense to a shorter version while keeping the core message",
            "Make it more casual and conversational",
            "Make it more formal and business-like",
            "Rewrite using different words while maintaining meaning"
        };
        
        showFeatureSelectionDialog(context, "Improve Message", features, descriptions, callback);
    }
    
    /**
     * Shows the feature selection dialog.
     */
    private static void showFeatureSelectionDialog(Context context, String title, String[] features, 
                                                  String[] descriptions, GenAIFeatureCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        
        // Create custom adapter for features with descriptions
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_2, android.R.id.text1, features) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);
                
                text1.setText(features[position]);
                if (position < descriptions.length) {
                    text2.setText(descriptions[position]);
                    text2.setVisibility(View.VISIBLE);
                } else {
                    text2.setVisibility(View.GONE);
                }
                
                return view;
            }
        };
        
        builder.setAdapter(adapter, (dialog, which) -> {
            if (callback != null) {
                callback.onFeatureSelected(features[which]);
            }
            dialog.dismiss();
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            if (callback != null) {
                callback.onDismissed();
            }
            dialog.dismiss();
        });
        
        builder.setOnDismissListener(dialog -> {
            if (callback != null) {
                callback.onDismissed();
            }
        });
        
        builder.show();
    }
    
    /**
     * Shows a result dialog with AI-generated content.
     */
    public static void showResultDialog(Context context, String title, String content, ResultDialogCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(content);
        
        builder.setPositiveButton("Use", (dialog, which) -> {
            if (callback != null) {
                callback.onUse(content);
            }
            dialog.dismiss();
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            if (callback != null) {
                callback.onCancel();
            }
            dialog.dismiss();
        });
        
        builder.setNeutralButton("Copy", (dialog, which) -> {
            if (callback != null) {
                callback.onCopy(content);
            }
        });
        
        builder.show();
    }
    
    /**
     * Shows a loading dialog while AI is processing.
     */
    public static AlertDialog showLoadingDialog(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        
        // Create custom layout with progress bar
        View view = LayoutInflater.from(context).inflate(android.R.layout.activity_list_item, null);
        ProgressBar progressBar = new ProgressBar(context);
        TextView textView = new TextView(context);
        textView.setText(message);
        textView.setPadding(20, 20, 20, 20);
        
        // Simple vertical layout
        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.addView(progressBar);
        layout.addView(textView);
        
        builder.setView(layout);
        builder.setCancelable(true);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        return dialog;
    }
    
    /**
     * Shows smart reply options dialog.
     */
    public static void showSmartReplyDialog(Context context, String[] replies, SmartReplyCallback callback) {
        if (replies == null || replies.length == 0) {
            Toast.makeText(context, "No smart replies available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Smart Replies");
        
        builder.setItems(replies, (dialog, which) -> {
            if (callback != null) {
                callback.onReplySelected(replies[which]);
            }
            dialog.dismiss();
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            if (callback != null) {
                callback.onCancel();
            }
            dialog.dismiss();
        });
        
        builder.show();
    }
    
    /**
     * Callback interface for result dialogs.
     */
    public interface ResultDialogCallback {
        void onUse(String content);
        void onCopy(String content);
        void onCancel();
    }
    
    /**
     * Callback interface for smart reply selection.
     */
    public interface SmartReplyCallback {
        void onReplySelected(String reply);
        void onCancel();
    }
}