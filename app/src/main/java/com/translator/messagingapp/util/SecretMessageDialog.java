package com.translator.messagingapp.util;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.translator.messagingapp.R;

/**
 * Dialog for composing secret messages with both visible and hidden text.
 */
public class SecretMessageDialog extends DialogFragment {
    
    public interface SecretMessageDialogListener {
        void onSecretMessageComposed(String visibleMessage, String secretMessage);
    }
    
    private SecretMessageDialogListener listener;
    private String initialVisibleMessage = "";
    private String initialSecretMessage = "";
    
    public static SecretMessageDialog newInstance(String visibleMessage, String secretMessage) {
        SecretMessageDialog dialog = new SecretMessageDialog();
        Bundle args = new Bundle();
        args.putString("visible_message", visibleMessage != null ? visibleMessage : "");
        args.putString("secret_message", secretMessage != null ? secretMessage : "");
        dialog.setArguments(args);
        return dialog;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            initialVisibleMessage = getArguments().getString("visible_message", "");
            initialSecretMessage = getArguments().getString("secret_message", "");
        }
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_secret_message, null);
        
        EditText visibleMessageInput = dialogView.findViewById(R.id.visible_message_input);
        EditText secretMessageInput = dialogView.findViewById(R.id.secret_message_input);
        
        // Set initial values
        visibleMessageInput.setText(initialVisibleMessage);
        secretMessageInput.setText(initialSecretMessage);
        
        // Set cursor to end of visible message input
        if (!initialVisibleMessage.isEmpty()) {
            visibleMessageInput.setSelection(initialVisibleMessage.length());
        }
        
        return new AlertDialog.Builder(context)
                .setTitle(R.string.secret_message_dialog_title)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String visibleMessage = visibleMessageInput.getText().toString().trim();
                    String secretMessage = secretMessageInput.getText().toString().trim();
                    
                    if (listener != null) {
                        listener.onSecretMessageComposed(visibleMessage, secretMessage);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }
    
    public void setSecretMessageDialogListener(SecretMessageDialogListener listener) {
        this.listener = listener;
    }
}