package com.translator.messagingapp;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Dialog for ML text operations (Summarization and Rewriting).
 */
public class MLTextOperationDialog {
    
    public enum OperationType {
        SUMMARIZE,
        REWRITE
    }
    
    public interface MLTextOperationCallback {
        void onOperationComplete(String result);
        void onOperationCancelled();
    }
    
    private final Context context;
    private final OnDeviceMLService mlService;
    private AlertDialog dialog;
    private EditText inputText;
    private Spinner styleSpinner;
    private TextView styleLabel;
    private ProgressBar progressBar;
    private TextView resultText;
    private Button actionButton;
    private Button useResultButton;
    private MLTextOperationCallback callback;
    
    public MLTextOperationDialog(Context context, OnDeviceMLService mlService) {
        this.context = context;
        this.mlService = mlService;
    }
    
    public void show(OperationType operationType, String initialText, MLTextOperationCallback callback) {
        this.callback = callback;
        
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_ml_text_operation, null);
        
        // Initialize views
        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        inputText = dialogView.findViewById(R.id.input_text);
        styleLabel = dialogView.findViewById(R.id.style_label);
        styleSpinner = dialogView.findViewById(R.id.style_spinner);
        progressBar = dialogView.findViewById(R.id.progress_bar);
        resultText = dialogView.findViewById(R.id.result_text);
        actionButton = dialogView.findViewById(R.id.btn_action);
        useResultButton = dialogView.findViewById(R.id.btn_use_result);
        Button cancelButton = dialogView.findViewById(R.id.btn_cancel);
        
        // Configure based on operation type
        if (operationType == OperationType.SUMMARIZE) {
            dialogTitle.setText(R.string.summarize_text);
            actionButton.setText(R.string.action_summarize);
            styleLabel.setVisibility(View.GONE);
            styleSpinner.setVisibility(View.GONE);
        } else {
            dialogTitle.setText(R.string.rewrite_text);
            actionButton.setText(R.string.action_rewrite);
            styleLabel.setVisibility(View.VISIBLE);
            styleSpinner.setVisibility(View.VISIBLE);
            setupStyleSpinner();
        }
        
        // Set initial text
        if (initialText != null) {
            inputText.setText(initialText);
        }
        
        // Set up button listeners
        actionButton.setOnClickListener(v -> performOperation(operationType));
        useResultButton.setOnClickListener(v -> {
            if (callback != null) {
                callback.onOperationComplete(resultText.getText().toString());
            }
            dialog.dismiss();
        });
        cancelButton.setOnClickListener(v -> {
            if (callback != null) {
                callback.onOperationCancelled();
            }
            dialog.dismiss();
        });
        
        // Create and show dialog
        dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .setOnCancelListener(d -> {
                    if (callback != null) {
                        callback.onOperationCancelled();
                    }
                })
                .create();
        
        dialog.show();
    }
    
    private void setupStyleSpinner() {
        String[] styles = {
            context.getString(R.string.rewrite_elaborate),
            context.getString(R.string.rewrite_emojify),
            context.getString(R.string.rewrite_shorten),
            context.getString(R.string.rewrite_friendly),
            context.getString(R.string.rewrite_professional),
            context.getString(R.string.rewrite_rephrase)
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, 
                android.R.layout.simple_spinner_item, styles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        styleSpinner.setAdapter(adapter);
    }
    
    private void performOperation(OperationType operationType) {
        String text = inputText.getText().toString().trim();
        
        if (text.isEmpty()) {
            Toast.makeText(context, "Please enter some text", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        actionButton.setEnabled(false);
        
        if (operationType == OperationType.SUMMARIZE) {
            performSummarization(text);
        } else {
            performRewriting(text);
        }
    }
    
    private void performSummarization(String text) {
        mlService.summarizeText(text, new OnDeviceMLService.SummarizationCallback() {
            @Override
            public void onSummarizationComplete(boolean success, String summary, String errorMessage) {
                // Update UI on main thread
                if (context instanceof ConversationActivity) {
                    ((ConversationActivity) context).runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        actionButton.setEnabled(true);
                        
                        if (success) {
                            resultText.setText(summary);
                            resultText.setVisibility(View.VISIBLE);
                            useResultButton.setVisibility(View.VISIBLE);
                            Toast.makeText(context, R.string.summary_generated, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, 
                                context.getString(R.string.summarization_error, errorMessage), 
                                Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }
    
    private void performRewriting(String text) {
        // Get selected style
        OnDeviceMLService.RewriteStyle style = getSelectedRewriteStyle();
        
        mlService.rewriteText(text, style, new OnDeviceMLService.RewritingCallback() {
            @Override
            public void onRewritingComplete(boolean success, String rewrittenText, String errorMessage) {
                // Update UI on main thread
                if (context instanceof ConversationActivity) {
                    ((ConversationActivity) context).runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        actionButton.setEnabled(true);
                        
                        if (success) {
                            resultText.setText(rewrittenText);
                            resultText.setVisibility(View.VISIBLE);
                            useResultButton.setVisibility(View.VISIBLE);
                            Toast.makeText(context, R.string.rewrite_completed, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, 
                                context.getString(R.string.rewrite_error, errorMessage), 
                                Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }
    
    private OnDeviceMLService.RewriteStyle getSelectedRewriteStyle() {
        int position = styleSpinner.getSelectedItemPosition();
        switch (position) {
            case 0: return OnDeviceMLService.RewriteStyle.ELABORATE;
            case 1: return OnDeviceMLService.RewriteStyle.EMOJIFY;
            case 2: return OnDeviceMLService.RewriteStyle.SHORTEN;
            case 3: return OnDeviceMLService.RewriteStyle.FRIENDLY;
            case 4: return OnDeviceMLService.RewriteStyle.PROFESSIONAL;
            case 5: return OnDeviceMLService.RewriteStyle.REPHRASE;
            default: return OnDeviceMLService.RewriteStyle.REPHRASE;
        }
    }
}