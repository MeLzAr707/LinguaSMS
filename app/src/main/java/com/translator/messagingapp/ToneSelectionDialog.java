package com.translator.messagingapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

/**
 * Dialog for selecting tone options for text rewriting
 */
public class ToneSelectionDialog {
    
    /**
     * Interface for handling tone selection
     */
    public interface OnToneSelectedListener {
        void onToneSelected(TextRewriterService.Tone tone);
        void onDialogCancelled();
    }
    
    private final Context context;
    private final String originalText;
    private final OnToneSelectedListener listener;
    private AlertDialog dialog;
    
    /**
     * Creates a new ToneSelectionDialog
     *
     * @param context The context for creating the dialog
     * @param originalText The original text to be rewritten
     * @param listener Listener for tone selection events
     */
    public ToneSelectionDialog(Context context, String originalText, OnToneSelectedListener listener) {
        this.context = context;
        this.originalText = originalText;
        this.listener = listener;
    }
    
    /**
     * Shows the tone selection dialog
     */
    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose Message Tone");
        
        // Create custom layout for tone selection
        View dialogView = createDialogView();
        builder.setView(dialogView);
        
        builder.setPositiveButton("Rewrite", (dialog, which) -> {
            TextRewriterService.Tone selectedTone = getSelectedTone(dialogView);
            if (listener != null) {
                listener.onToneSelected(selectedTone);
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            if (listener != null) {
                listener.onDialogCancelled();
            }
        });
        
        builder.setNeutralButton("Send Original", (dialog, which) -> {
            if (listener != null) {
                listener.onToneSelected(TextRewriterService.Tone.ORIGINAL);
            }
        });
        
        dialog = builder.create();
        dialog.show();
    }
    
    /**
     * Creates the dialog view with tone options
     */
    private View createDialogView() {
        LayoutInflater inflater = LayoutInflater.from(context);
        
        // Create a simple linear layout programmatically since we're making minimal changes
        View dialogView = inflater.inflate(android.R.layout.select_dialog_singlechoice, null);
        
        // Create RadioGroup programmatically
        RadioGroup radioGroup = new RadioGroup(context);
        radioGroup.setOrientation(RadioGroup.VERTICAL);
        radioGroup.setPadding(24, 16, 24, 16);
        
        // Add tone options
        TextRewriterService.Tone[] tones = {
            TextRewriterService.Tone.FORMAL,
            TextRewriterService.Tone.FRIENDLY,
            TextRewriterService.Tone.CONCISE
        };
        
        for (int i = 0; i < tones.length; i++) {
            TextRewriterService.Tone tone = tones[i];
            RadioButton radioButton = new RadioButton(context);
            radioButton.setId(i);
            radioButton.setText(tone.getDisplayName() + " - " + tone.getDescription());
            radioButton.setPadding(8, 8, 8, 8);
            
            // Set first option as selected by default
            if (i == 0) {
                radioButton.setChecked(true);
            }
            
            radioGroup.addView(radioButton);
        }
        
        // Add original text preview
        TextView originalTextView = new TextView(context);
        originalTextView.setText("Original: \"" + originalText + "\"");
        originalTextView.setPadding(24, 16, 24, 8);
        originalTextView.setTextSize(14);
        originalTextView.setAlpha(0.7f);
        
        // Create container
        android.widget.LinearLayout container = new android.widget.LinearLayout(context);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.addView(originalTextView);
        container.addView(radioGroup);
        
        return container;
    }
    
    /**
     * Gets the selected tone from the dialog
     */
    private TextRewriterService.Tone getSelectedTone(View dialogView) {
        android.widget.LinearLayout container = (android.widget.LinearLayout) dialogView;
        RadioGroup radioGroup = (RadioGroup) container.getChildAt(1);
        
        int selectedId = radioGroup.getCheckedRadioButtonId();
        
        // Map radio button IDs to tones
        switch (selectedId) {
            case 0:
                return TextRewriterService.Tone.FORMAL;
            case 1:
                return TextRewriterService.Tone.FRIENDLY;
            case 2:
                return TextRewriterService.Tone.CONCISE;
            default:
                return TextRewriterService.Tone.FORMAL; // Default fallback
        }
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