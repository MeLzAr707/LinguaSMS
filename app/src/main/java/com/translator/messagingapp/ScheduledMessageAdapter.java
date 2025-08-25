package com.translator.messagingapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying scheduled messages.
 */
public class ScheduledMessageAdapter extends RecyclerView.Adapter<ScheduledMessageAdapter.ViewHolder> {
    private List<ScheduledMessage> messages = new ArrayList<>();
    private final OnMessageClickListener onMessageClickListener;
    private final OnDeleteClickListener onDeleteClickListener;
    private final SimpleDateFormat dateTimeFormat;

    public interface OnMessageClickListener {
        void onMessageClicked(ScheduledMessage message);
    }

    public interface OnDeleteClickListener {
        void onDeleteClicked(ScheduledMessage message);
    }

    public ScheduledMessageAdapter(OnMessageClickListener onMessageClickListener, 
                                  OnDeleteClickListener onDeleteClickListener) {
        this.onMessageClickListener = onMessageClickListener;
        this.onDeleteClickListener = onDeleteClickListener;
        this.dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scheduled_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduledMessage message = messages.get(position);
        
        // Set recipient
        holder.recipientText.setText(message.getRecipient());
        
        // Set message body (truncate if too long)
        String body = message.getMessageBody();
        if (body.length() > 100) {
            body = body.substring(0, 97) + "...";
        }
        holder.messageBodyText.setText(body);
        
        // Set scheduled time
        String timeText = dateTimeFormat.format(new Date(message.getScheduledTime()));
        holder.scheduledTimeText.setText(timeText);
        
        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (onMessageClickListener != null) {
                onMessageClickListener.onMessageClicked(message);
            }
        });
        
        holder.deleteButton.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClicked(message);
            }
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateMessages(List<ScheduledMessage> newMessages) {
        this.messages.clear();
        this.messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView recipientText;
        final TextView messageBodyText;
        final TextView scheduledTimeText;
        final ImageButton deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            recipientText = itemView.findViewById(R.id.recipient_text);
            messageBodyText = itemView.findViewById(R.id.message_body_text);
            scheduledTimeText = itemView.findViewById(R.id.scheduled_time_text);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}