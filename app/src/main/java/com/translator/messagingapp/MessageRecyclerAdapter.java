package com.translator.messagingapp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying messages in a RecyclerView.
 */
public class MessageRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_INCOMING = 1;
    private static final int VIEW_TYPE_OUTGOING = 2;
    private static final int VIEW_TYPE_INCOMING_MEDIA = 3;
    private static final int VIEW_TYPE_OUTGOING_MEDIA = 4;

    private final Context context;
    private final List<Message> messages;
    private final SimpleDateFormat dateFormat;
    private OnMessageClickListener clickListener;

    /**
     * Interface for message click events.
     */
    public interface OnMessageClickListener {
        void onMessageClick(Message message, int position);
        void onMessageLongClick(Message message, int position);
        void onTranslateClick(Message message, int position);
        void onAttachmentClick(Uri attachmentUri, int position);
        void onReactionClick(Message message, int position);
        void onAddReactionClick(Message message, int position);
    }
    
    /**
     * Legacy interface for message click events.
     * @deprecated Use OnMessageClickListener instead
     */
    @Deprecated
    public interface MessageClickListener {
        void onMessageClick(Message message);
        void onMessageLongClick(Message message);
    }

    /**
     * Creates a new message adapter.
     *
     * @param context The context
     * @param messages The list of messages
     */
    public MessageRecyclerAdapter(Context context, List<Message> messages) {
        this.context = context;
        this.messages = messages;
        this.dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
    }

    /**
     * Creates a new message adapter with click listener.
     *
     * @param context The context
     * @param messages The list of messages
     * @param listener The click listener
     */
    public MessageRecyclerAdapter(Context context, List<Message> messages, OnMessageClickListener listener) {
        this(context, messages);
        this.clickListener = listener;
    }

    /**
     * Sets the message click listener.
     *
     * @param listener The click listener
     */
    public void setOnMessageClickListener(OnMessageClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;

        switch (viewType) {
            case VIEW_TYPE_INCOMING:
                view = inflater.inflate(R.layout.item_message_incoming_updated, parent, false);
                return new IncomingMessageViewHolder(view);
            case VIEW_TYPE_OUTGOING:
                view = inflater.inflate(R.layout.item_message_outgoing_updated, parent, false);
                return new OutgoingMessageViewHolder(view);
            case VIEW_TYPE_INCOMING_MEDIA:
                view = inflater.inflate(R.layout.item_message_incoming_media, parent, false);
                return new IncomingMediaMessageViewHolder(view);
            case VIEW_TYPE_OUTGOING_MEDIA:
                view = inflater.inflate(R.layout.item_message_outgoing_media, parent, false);
                return new OutgoingMediaMessageViewHolder(view);
            default:
                throw new IllegalArgumentException("Invalid view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_INCOMING:
                bindIncomingMessage((IncomingMessageViewHolder) holder, message, position);
                break;
            case VIEW_TYPE_OUTGOING:
                bindOutgoingMessage((OutgoingMessageViewHolder) holder, message, position);
                break;
            case VIEW_TYPE_INCOMING_MEDIA:
                bindIncomingMediaMessage((IncomingMediaMessageViewHolder) holder, (MmsMessage) message, position);
                break;
            case VIEW_TYPE_OUTGOING_MEDIA:
                bindOutgoingMediaMessage((OutgoingMediaMessageViewHolder) holder, (MmsMessage) message, position);
                break;
        }
    }

    private void bindIncomingMessage(IncomingMessageViewHolder holder, Message message, int position) {
        // Set message text with highlighting if needed
        setMessageTextWithHighlighting(holder.messageText, message);
        
        // Set date
        holder.dateText.setText(dateFormat.format(new Date(message.getDate())));
        
        // Set up click listeners
        setupMessageClickListeners(holder.itemView, holder.translateButton, message, position);
        
        // Set up reactions
        setupReactions(holder.reactionsLayout, holder.addReactionButton, message, position);
    }

    private void bindOutgoingMessage(OutgoingMessageViewHolder holder, Message message, int position) {
        // Set message text with highlighting if needed
        setMessageTextWithHighlighting(holder.messageText, message);
        
        // Set date
        holder.dateText.setText(dateFormat.format(new Date(message.getDate())));
        
        // Set message status
        setMessageStatus(holder.messageStatus, message);
        
        // Set up click listeners
        setupMessageClickListeners(holder.itemView, holder.translateButton, message, position);
        
        // Set up reactions
        setupReactions(holder.reactionsLayout, holder.addReactionButton, message, position);
    }

    private void bindIncomingMediaMessage(IncomingMediaMessageViewHolder holder, MmsMessage message, int position) {
        // Set message text with highlighting if needed
        setMessageTextWithHighlighting(holder.messageText, message);
        
        // Set date
        holder.dateText.setText(dateFormat.format(new Date(message.getDate())));
        
        // Set up media
        setupMedia(holder.mediaContainer, holder.mediaIcon, message, position);
        
        // Set up click listeners
        setupMessageClickListeners(holder.itemView, holder.translateButton, message, position);
        
        // Set up reactions
        setupReactions(holder.reactionsLayout, holder.addReactionButton, message, position);
    }

    private void bindOutgoingMediaMessage(OutgoingMediaMessageViewHolder holder, MmsMessage message, int position) {
        // Set message text with highlighting if needed
        setMessageTextWithHighlighting(holder.messageText, message);
        
        // Set date
        holder.dateText.setText(dateFormat.format(new Date(message.getDate())));
        
        // Set message status
        setMessageStatus(holder.messageStatus, message);
        
        // Set up media
        setupMedia(holder.mediaContainer, holder.mediaIcon, message, position);
        
        // Set up click listeners
        setupMessageClickListeners(holder.itemView, holder.translateButton, message, position);
        
        // Set up reactions
        setupReactions(holder.reactionsLayout, holder.addReactionButton, message, position);
    }

    private void setMessageTextWithHighlighting(TextView textView, Message message) {
        String messageText = message.getBody();
        String searchQuery = message.getSearchQuery();
        
        if (message.isTranslated() && !TextUtils.isEmpty(message.getTranslatedText())) {
            messageText = message.getTranslatedText();
        }
        
        if (!TextUtils.isEmpty(searchQuery) && !TextUtils.isEmpty(messageText)) {
            // Create a spannable string for highlighting
            SpannableString spannableString = new SpannableString(messageText);
            
            // Case insensitive search
            String lowerCaseText = messageText.toLowerCase();
            String lowerCaseQuery = searchQuery.toLowerCase();
            
            int startIndex = 0;
            while (startIndex >= 0) {
                int index = lowerCaseText.indexOf(lowerCaseQuery, startIndex);
                if (index >= 0) {
                    // Highlight the search query
                    spannableString.setSpan(
                            new BackgroundColorSpan(Color.YELLOW),
                            index,
                            index + searchQuery.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    startIndex = index + searchQuery.length();
                } else {
                    break;
                }
            }
            
            textView.setText(spannableString);
        } else {
            textView.setText(messageText);
        }
        
        // Set typeface based on translation state
        if (message.isTranslated()) {
            textView.setTypeface(textView.getTypeface(), Typeface.ITALIC);
        } else {
            textView.setTypeface(textView.getTypeface(), Typeface.NORMAL);
        }
    }

    private void setMessageStatus(ImageView statusIcon, Message message) {
        if (message.getType() == Message.TYPE_SENT || message.getType() == Message.TYPE_OUTBOX) {
            if (message.isRead()) {
                statusIcon.setImageResource(R.drawable.ic_read);
            } else if (message.isDelivered()) {
                statusIcon.setImageResource(R.drawable.ic_delivered);
            } else {
                statusIcon.setImageResource(R.drawable.ic_sent);
            }
            statusIcon.setVisibility(View.VISIBLE);
        } else {
            statusIcon.setVisibility(View.GONE);
        }
    }

    private void setupMedia(ViewGroup mediaContainer, ImageView mediaIcon, MmsMessage message, int position) {
        if (message.hasAttachments()) {
            mediaContainer.setVisibility(View.VISIBLE);
            
            // Get the first attachment
            MmsMessage.Attachment attachment = message.getAttachments().get(0);
            
            // Check if it's an image
            if (attachment.isImage()) {
                // Load image with Glide
                Glide.with(context)
                        .load(attachment.getUri())
                        .placeholder(R.drawable.ic_attachment)
                        .error(R.drawable.ic_attachment)
                        .into(mediaIcon);
            } else {
                // Show generic attachment icon
                mediaIcon.setImageResource(R.drawable.ic_attachment);
            }
            
            // Set click listener for attachment
            mediaIcon.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onAttachmentClick(attachment.getUri(), position);
                }
            });
        } else {
            mediaContainer.setVisibility(View.GONE);
        }
    }

    private void setupMessageClickListeners(View itemView, View translateButton, Message message, int position) {
        // Set click listener for the whole message
        itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                if (clickListener instanceof OnMessageClickListener) {
                    ((OnMessageClickListener) clickListener).onMessageClick(message, position);
                } else if (clickListener instanceof MessageClickListener) {
                    ((MessageClickListener) clickListener).onMessageClick(message);
                }
            }
        });
        
        // Set long click listener for the whole message
        itemView.setOnLongClickListener(v -> {
            if (clickListener != null) {
                if (clickListener instanceof OnMessageClickListener) {
                    ((OnMessageClickListener) clickListener).onMessageLongClick(message, position);
                } else if (clickListener instanceof MessageClickListener) {
                    ((MessageClickListener) clickListener).onMessageLongClick(message);
                }
                return true;
            }
            return false;
        });
        
        // Set click listener for translate button
        if (translateButton != null) {
            if (message.isTranslatable()) {
                translateButton.setVisibility(View.VISIBLE);
                
                if (message.isTranslated()) {
                    translateButton.setImageResource(R.drawable.ic_restore);
                } else {
                    translateButton.setImageResource(R.drawable.ic_translate);
                }
                
                translateButton.setOnClickListener(v -> {
                    if (clickListener != null) {
                        clickListener.onTranslateClick(message, position);
                    }
                });
            } else {
                translateButton.setVisibility(View.GONE);
            }
        }
    }

    private void setupReactions(LinearLayout reactionsLayout, View addReactionButton, Message message, int position) {
        // Check if message has reactions
        if (message.hasReactions()) {
            reactionsLayout.setVisibility(View.VISIBLE);
            reactionsLayout.removeAllViews();
            
            // Add reaction views
            for (MessageReaction reaction : message.getReactions()) {
                View reactionView = LayoutInflater.from(context).inflate(R.layout.reaction_item, reactionsLayout, false);
                TextView emojiText = reactionView.findViewById(R.id.emoji_text);
                TextView countText = reactionView.findViewById(R.id.count_text);
                
                emojiText.setText(reaction.getEmoji());
                countText.setText(String.valueOf(reaction.getCount()));
                
                // Set click listener for reaction
                reactionView.setOnClickListener(v -> {
                    if (clickListener != null) {
                        clickListener.onReactionClick(message, position);
                    }
                });
                
                reactionsLayout.addView(reactionView);
            }
        } else {
            reactionsLayout.setVisibility(View.GONE);
        }
        
        // Set up add reaction button
        if (addReactionButton != null) {
            addReactionButton.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onAddReactionClick(message, position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        
        if (message instanceof MmsMessage && ((MmsMessage) message).hasAttachments()) {
            // Media message
            if (message.getType() == Message.TYPE_INBOX || message.getType() == Message.TYPE_ALL) {
                return VIEW_TYPE_INCOMING_MEDIA;
            } else {
                return VIEW_TYPE_OUTGOING_MEDIA;
            }
        } else {
            // Text message
            if (message.getType() == Message.TYPE_INBOX || message.getType() == Message.TYPE_ALL) {
                return VIEW_TYPE_INCOMING;
            } else {
                return VIEW_TYPE_OUTGOING;
            }
        }
    }

    /**
     * ViewHolder for incoming text messages.
     */
    static class IncomingMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView dateText;
        View translateButton;
        LinearLayout reactionsLayout;
        View addReactionButton;

        IncomingMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            dateText = itemView.findViewById(R.id.date_text);
            translateButton = itemView.findViewById(R.id.translate_button);
            reactionsLayout = itemView.findViewById(R.id.reactions_layout);
            addReactionButton = itemView.findViewById(R.id.add_reaction_button);
        }
    }

    /**
     * ViewHolder for outgoing text messages.
     */
    static class OutgoingMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView dateText;
        ImageView messageStatus;
        View translateButton;
        LinearLayout reactionsLayout;
        View addReactionButton;

        OutgoingMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            dateText = itemView.findViewById(R.id.date_text);
            messageStatus = itemView.findViewById(R.id.message_status);
            translateButton = itemView.findViewById(R.id.translate_button);
            reactionsLayout = itemView.findViewById(R.id.reactions_layout);
            addReactionButton = itemView.findViewById(R.id.add_reaction_button);
        }
    }

    /**
     * ViewHolder for incoming media messages.
     */
    static class IncomingMediaMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView dateText;
        View translateButton;
        ViewGroup mediaContainer;
        ImageView mediaIcon;
        LinearLayout reactionsLayout;
        View addReactionButton;

        IncomingMediaMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            dateText = itemView.findViewById(R.id.date_text);
            translateButton = itemView.findViewById(R.id.translate_button);
            mediaContainer = itemView.findViewById(R.id.media_container);
            mediaIcon = itemView.findViewById(R.id.media_icon);
            reactionsLayout = itemView.findViewById(R.id.reactions_layout);
            addReactionButton = itemView.findViewById(R.id.add_reaction_button);
        }
    }

    /**
     * ViewHolder for outgoing media messages.
     */
    static class OutgoingMediaMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView dateText;
        ImageView messageStatus;
        View translateButton;
        ViewGroup mediaContainer;
        ImageView mediaIcon;
        LinearLayout reactionsLayout;
        View addReactionButton;

        OutgoingMediaMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            dateText = itemView.findViewById(R.id.date_text);
            messageStatus = itemView.findViewById(R.id.message_status);
            translateButton = itemView.findViewById(R.id.translate_button);
            mediaContainer = itemView.findViewById(R.id.media_container);
            mediaIcon = itemView.findViewById(R.id.media_icon);
            reactionsLayout = itemView.findViewById(R.id.reactions_layout);
            addReactionButton = itemView.findViewById(R.id.add_reaction_button);
        }
    }
}