package com.translator.messagingapp;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying conversations in a RecyclerView.
 * FIXED VERSION: Adds proper error handling and logging for contact display
 */
public class ConversationRecyclerAdapter extends RecyclerView.Adapter<ConversationRecyclerAdapter.ViewHolder> {
    private static final String TAG = "ConversationAdapter";
    private final Context context;
    private List<Conversation> conversations;
    private ConversationClickListener conversationClickListener;

    /**
     * Constructor.
     *
     * @param context The context
     * @param conversations The list of conversations
     */
    public ConversationRecyclerAdapter(Context context, List<Conversation> conversations) {
        this.context = context;
        this.conversations = conversations;
        
        // Log conversations for debugging
        logConversations();
    }

    /**
     * Sets the conversation click listener.
     *
     * @param conversationClickListener The listener
     */
    public void setConversationClickListener(ConversationClickListener conversationClickListener) {
        this.conversationClickListener = conversationClickListener;
    }

    /**
     * Updates the conversations list.
     *
     * @param conversations The new list of conversations
     */
    public void updateConversations(List<Conversation> conversations) {
        this.conversations = conversations;
        logConversations();
        notifyDataSetChanged();
    }

    /**
     * Logs the conversations for debugging.
     */
    private void logConversations() {
        if (conversations == null) {
            Log.e(TAG, "Conversations list is null");
            return;
        }
        
        Log.d(TAG, "Conversations count: " + conversations.size());
        for (int i = 0; i < conversations.size(); i++) {
            Conversation conversation = conversations.get(i);
            if (conversation == null) {
                Log.e(TAG, "Conversation at position " + i + " is null");
                continue;
            }
            
            Log.d(TAG, "Conversation[" + i + "]: " +
                  "threadId=" + conversation.getThreadId() +
                  ", address=" + conversation.getAddress() +
                  ", contactName=" + conversation.getContactName() +
                  ", snippet=" + conversation.getSnippet() +
                  ", date=" + conversation.getDate() +
                  ", messageCount=" + conversation.getMessageCount() +
                  ", read=" + conversation.isRead());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            if (conversations == null || position >= conversations.size()) {
                Log.e(TAG, "Invalid position or null conversations: " + position);
                holder.contactNameTextView.setText(R.string.error_loading_conversation);
                return;
            }
            
            Conversation conversation = conversations.get(position);
            if (conversation == null) {
                Log.e(TAG, "Conversation at position " + position + " is null");
                holder.contactNameTextView.setText(R.string.error_loading_conversation);
                return;
            }
            
            // Set contact name or phone number with improved fallback logic
            String displayName = getDisplayName(conversation);
            holder.contactNameTextView.setText(displayName);
            
            // Log for debugging
            Log.d(TAG, "Displaying conversation: name='" + conversation.getContactName() + 
                      "', address='" + conversation.getAddress() + 
                      "', displayName='" + displayName + "'");
            
            // Set snippet
            String snippet = conversation.getSnippet();
            if (!TextUtils.isEmpty(snippet)) {
                holder.snippetTextView.setText(snippet);
                holder.snippetTextView.setVisibility(View.VISIBLE);
            } else {
                holder.snippetTextView.setVisibility(View.GONE);
            }
            
            // Set date
            Date date = conversation.getDate();
            if (date != null) {
                holder.dateTextView.setText(formatDate(date));
                holder.dateTextView.setVisibility(View.VISIBLE);
            } else {
                holder.dateTextView.setVisibility(View.GONE);
            }
            
            // Set unread indicator
            boolean isRead = conversation.isRead();
            holder.unreadIndicator.setVisibility(isRead ? View.INVISIBLE : View.VISIBLE);
            
            // Set unread count
            int unreadCount = conversation.getUnreadCount();
            if (unreadCount > 0) {
                holder.unreadCountTextView.setText(String.valueOf(unreadCount));
                holder.unreadCountTextView.setVisibility(View.VISIBLE);
            } else {
                holder.unreadCountTextView.setVisibility(View.GONE);
            }
            
            // Set click listeners
            holder.itemView.setOnClickListener(v -> {
                if (conversationClickListener != null) {
                    conversationClickListener.onConversationClick(conversation);
                }
            });
            
            holder.itemView.setOnLongClickListener(v -> {
                if (conversationClickListener != null) {
                    conversationClickListener.onConversationLongClick(conversation);
                    return true;
                }
                return false;
            });
        } catch (Exception e) {
            Log.e(TAG, "Error binding conversation at position " + position, e);
            holder.contactNameTextView.setText(R.string.error_loading_conversation);
        }
    }

    @Override
    public int getItemCount() {
        return conversations != null ? conversations.size() : 0;
    }

    /**
     * Gets the display name for a conversation with proper fallback logic.
     *
     * @param conversation The conversation
     * @return The display name to show
     */
    private String getDisplayName(Conversation conversation) {
        if (conversation == null) {
            return context.getString(R.string.unknown_contact);
        }
        
        String contactName = conversation.getContactName();
        String address = conversation.getAddress();
        String threadId = conversation.getThreadId();
        
        // Safety check: never display threadId as the contact name
        if (!TextUtils.isEmpty(contactName) && contactName.equals(threadId)) {
            Log.e(TAG, "WARNING: Contact name equals threadId (" + threadId + ") - this should not happen!");
            contactName = null; // Force fallback logic
        }
        
        // First priority: Non-empty contact name that's not just the phone number
        if (!TextUtils.isEmpty(contactName) && !contactName.equals(address)) {
            return contactName;
        }
        
        // Second priority: Phone number/address
        if (!TextUtils.isEmpty(address)) {
            // Additional safety check: make sure address is not threadId
            if (address.equals(threadId)) {
                Log.e(TAG, "WARNING: Address equals threadId (" + threadId + ") - this should not happen!");
                return context.getString(R.string.unknown_contact);
            }
            return address;
        }
        
        // Third priority: Contact name even if it might be a phone number
        if (!TextUtils.isEmpty(contactName)) {
            return contactName;
        }
        
        // Last resort: Unknown contact
        return context.getString(R.string.unknown_contact);
    }

    /**
     * Formats the date for display.
     *
     * @param date The date to format
     * @return The formatted date
     */
    private String formatDate(Date date) {
        try {
            Date now = new Date();
            
            // If the message is from today, show only the time
            if (isSameDay(date, now)) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                return timeFormat.format(date);
            } else {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
                return dateFormat.format(date);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date", e);
            return "";
        }
    }

    /**
     * Checks if two dates are on the same day.
     *
     * @param date1 The first date
     * @param date2 The second date
     * @return true if the dates are on the same day, false otherwise
     */
    private boolean isSameDay(Date date1, Date date2) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return dateFormat.format(date1).equals(dateFormat.format(date2));
    }

    /**
     * ViewHolder for conversation items.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView contactNameTextView;
        TextView snippetTextView;
        TextView dateTextView;
        View unreadIndicator;
        TextView unreadCountTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            contactNameTextView = itemView.findViewById(R.id.contact_name);
            snippetTextView = itemView.findViewById(R.id.snippet);
            dateTextView = itemView.findViewById(R.id.date);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
            unreadCountTextView = itemView.findViewById(R.id.unread_count);
        }
    }

    /**
     * Interface for conversation click events.
     */
    public interface ConversationClickListener {
        void onConversationClick(Conversation conversation);
        void onConversationLongClick(Conversation conversation);
    }
}