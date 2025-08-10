
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private static final String TAG = "MessageRecyclerAdapter";

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
        void onAttachmentClick(MmsMessage.Attachment attachment, int position);
        void onReactionClick(Message message, int position);
        void onAddReactionClick(Message message, int position);
    }

    /**
     * Creates a new message adapter.
     *
     * @param context The context
     * @param messages The list of messages
     */
    public MessageRecyclerAdapter(Context context, List<Message> messages) {
        this.context = context;
        this.messages = messages != null ? messages : new ArrayList<>();
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
                // Fallback to incoming message layout if view type is invalid
                view = inflater.inflate(R.layout.item_message_incoming_updated, parent, false);
                return new IncomingMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position < 0 || position >= messages.size()) {
            return; // Prevent index out of bounds
        }

        Message message = messages.get(position);
        if (message == null) {
            return; // Skip binding if message is null
        }

        try {
            switch (holder.getItemViewType()) {
                case VIEW_TYPE_INCOMING:
                    bindIncomingMessage((IncomingMessageViewHolder) holder, message, position);
                    break;
                case VIEW_TYPE_OUTGOING:
                    bindOutgoingMessage((OutgoingMessageViewHolder) holder, message, position);
                    break;
                case VIEW_TYPE_INCOMING_MEDIA:
                    if (message instanceof MmsMessage) {
                        bindIncomingMediaMessage((IncomingMediaMessageViewHolder) holder, (MmsMessage) message, position);
                    }
                    break;
                case VIEW_TYPE_OUTGOING_MEDIA:
                    if (message instanceof MmsMessage) {
                        bindOutgoingMediaMessage((OutgoingMediaMessageViewHolder) holder, (MmsMessage) message, position);
                    }
                    break;
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error binding view holder: " + e.getMessage(), e);
        }
    }

    private void bindIncomingMessage(IncomingMessageViewHolder holder, Message message, int position) {
        if (holder == null || message == null) {
            return;
        }

        // Set message text with highlighting if needed
        if (holder.messageText != null) {
            setMessageTextWithHighlighting(holder.messageText, message);
        }

        // Set date - handle both date_text and message_date IDs
        if (holder.dateText != null) {
            holder.dateText.setText(formatMessageDate(message.getDate()));
        }

        // Set up click listeners
        setupMessageClickListeners(holder.itemView, holder.translateButton, message, position);

        // Set up reactions
        setupReactions(holder.reactionsLayout, holder.addReactionButton, message, position);
    }

    private void bindOutgoingMessage(OutgoingMessageViewHolder holder, Message message, int position) {
        if (holder == null || message == null) {
            return;
        }

        // Set message text with highlighting if needed
        if (holder.messageText != null) {
            setMessageTextWithHighlighting(holder.messageText, message);
        }

        // Set date - handle both date_text and message_date IDs
        if (holder.dateText != null) {
            holder.dateText.setText(formatMessageDate(message.getDate()));
        }

        // Set message status
        if (holder.messageStatus != null) {
            setMessageStatus(holder.messageStatus, message);
        }

        // Set up click listeners
        setupMessageClickListeners(holder.itemView, holder.translateButton, message, position);

        // Set up reactions
        setupReactions(holder.reactionsLayout, holder.addReactionButton, message, position);
    }

    private void bindIncomingMediaMessage(IncomingMediaMessageViewHolder holder, MmsMessage message, int position) {
        if (holder == null || message == null) {
            return;
        }

        // Set message text with highlighting if needed
        if (holder.messageText != null) {
            setMessageTextWithHighlighting(holder.messageText, message);
        }

        // Set date - handle both date_text and message_date IDs
        if (holder.dateText != null) {
            holder.dateText.setText(formatMessageDate(message.getDate()));
        }

        // Set up media
        setupMedia(holder.mediaContainer, holder.mediaIcon, holder.mediaImage, message, position);

        // Set up click listeners
        setupMessageClickListeners(holder.itemView, holder.translateButton, message, position);

        // Set up reactions
        setupReactions(holder.reactionsLayout, holder.addReactionButton, message, position);
    }

    private void bindOutgoingMediaMessage(OutgoingMediaMessageViewHolder holder, MmsMessage message, int position) {
        if (holder == null || message == null) {
            return;
        }

        // Set message text with highlighting if needed
        if (holder.messageText != null) {
            setMessageTextWithHighlighting(holder.messageText, message);
        }

        // Set date - handle both date_text and message_date IDs
        if (holder.dateText != null) {
            holder.dateText.setText(formatMessageDate(message.getDate()));
        }

        // Set message status
        if (holder.messageStatus != null) {
            setMessageStatus(holder.messageStatus, message);
        }

        // Set up media
        setupMedia(holder.mediaContainer, holder.mediaIcon, holder.mediaImage, message, position);

        // Set up click listeners
        setupMessageClickListeners(holder.itemView, holder.translateButton, message, position);

        // Set up reactions
        setupReactions(holder.reactionsLayout, holder.addReactionButton, message, position);
    }

    /**
     * Format message date safely
     */
    private String formatMessageDate(long timestamp) {
        try {
            return dateFormat.format(new Date(timestamp));
        } catch (Exception e) {
            return ""; // Return empty string if date formatting fails
        }
    }

    private void setMessageTextWithHighlighting(TextView textView, Message message) {
        if (textView == null || message == null) {
            return;
        }

        String messageText = message.getBody();
        if (messageText == null) {
            messageText = ""; // Use empty string if body is null
        }

        String searchQuery = message.getSearchQuery();

        if (message.isTranslated() && !TextUtils.isEmpty(message.getTranslatedText())) {
            messageText = message.getTranslatedText();
        }

        if (!TextUtils.isEmpty(searchQuery) && !TextUtils.isEmpty(messageText)) {
            try {
                // Create a spannable string for highlighting
                SpannableString spannableString = new SpannableString(messageText);

                // Case insensitive search
                String lowerCaseText = messageText.toLowerCase();
                String lowerCaseQuery = searchQuery.toLowerCase();

                int startIndex = 0;
                while (startIndex >= 0 && startIndex < lowerCaseText.length()) {
                    int index = lowerCaseText.indexOf(lowerCaseQuery, startIndex);
                    if (index >= 0 && index + searchQuery.length() <= messageText.length()) {
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
            } catch (Exception e) {
                // Fallback to plain text if highlighting fails
                textView.setText(messageText);
            }
        } else {
            textView.setText(messageText);
        }

        // Set typeface based on translation state
        try {
            if (message.isTranslated()) {
                textView.setTypeface(textView.getTypeface(), Typeface.ITALIC);
            } else {
                textView.setTypeface(textView.getTypeface(), Typeface.NORMAL);
            }
        } catch (Exception e) {
            // Ignore typeface errors
        }
    }

    private void setMessageStatus(ImageView statusIcon, Message message) {
        if (statusIcon == null || message == null) {
            return;
        }

        try {
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
        } catch (Exception e) {
            statusIcon.setVisibility(View.GONE);
        }
    }

    /**
     * Enhanced method for handling media attachments with proper image display and video thumbnails
     */
    private void setupMedia(ViewGroup mediaContainer, ImageView mediaIcon, ImageView mediaImage, Message message, int position) {
        if (mediaContainer == null || message == null) {
            return;
        }

        try {
            if (message.hasAttachments() && message instanceof MmsMessage) {
                mediaContainer.setVisibility(View.VISIBLE);

                // Get the first attachment
                List<MmsMessage.Attachment> attachments = ((MmsMessage) message).getAttachmentObjects();
                if (attachments != null && !attachments.isEmpty()) {
                    MmsMessage.Attachment attachment = attachments.get(0);
                    if (attachment == null) {
                        mediaContainer.setVisibility(View.GONE);
                        return;
                    }

                    // Handle different attachment types
                    if (attachment.isImage() && attachment.getUri() != null) {
                        // Show image in media_image view
                        displayImage(mediaImage, mediaIcon, attachment, position);
                    } else if (attachment.isVideo() && attachment.getUri() != null) {
                        // Show video thumbnail in media_image view
                        displayVideoThumbnail(mediaImage, mediaIcon, attachment, position);
                    } else {
                        // Show generic attachment icon in media_icon view
                        displayGenericAttachment(mediaImage, mediaIcon, attachment, position);
                    }
                } else {
                    mediaContainer.setVisibility(View.GONE);
                }
            } else {
                mediaContainer.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error setting up media: " + e.getMessage(), e);
            mediaContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Display an image attachment
     */
    private void displayImage(ImageView mediaImage, ImageView mediaIcon, MmsMessage.Attachment attachment, int position) {
        if (mediaImage == null) {
            return;
        }

        try {
            // Show image view, hide icon view
            if (mediaIcon != null) {
                mediaIcon.setVisibility(View.GONE);
            }
            mediaImage.setVisibility(View.VISIBLE);

            // Load image with Glide
            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.ic_attachment)
                    .error(R.drawable.ic_attachment)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop();

            Glide.with(context)
                    .load(attachment.getUri())
                    .apply(options)
                    .into(mediaImage);

            // Set click listener for attachment
            mediaImage.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onAttachmentClick(attachment, position);
                }
            });

        } catch (Exception e) {
            android.util.Log.e(TAG, "Error displaying image: " + e.getMessage(), e);
            // Fallback to generic icon
            displayGenericAttachment(mediaImage, mediaIcon, attachment, position);
        }
    }

    /**
     * Display a video thumbnail
     */
    private void displayVideoThumbnail(ImageView mediaImage, ImageView mediaIcon, MmsMessage.Attachment attachment, int position) {
        if (mediaImage == null) {
            return;
        }

        try {
            // Show image view, hide icon view
            if (mediaIcon != null) {
                mediaIcon.setVisibility(View.GONE);
            }
            mediaImage.setVisibility(View.VISIBLE);

            // Load video thumbnail with Glide
            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.ic_attachment)
                    .error(R.drawable.ic_attachment)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .frame(1000000); // Get frame at 1 second

            Glide.with(context)
                    .load(attachment.getUri())
                    .apply(options)
                    .into(mediaImage);

            // Set click listener for attachment
            mediaImage.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onAttachmentClick(attachment, position);
                }
            });

        } catch (Exception e) {
            android.util.Log.e(TAG, "Error displaying video thumbnail: " + e.getMessage(), e);
            // Fallback to generic icon
            displayGenericAttachment(mediaImage, mediaIcon, attachment, position);
        }
    }

    /**
     * Display a generic attachment icon
     */
    private void displayGenericAttachment(ImageView mediaImage, ImageView mediaIcon, MmsMessage.Attachment attachment, int position) {
        try {
            // Hide image view, show icon view
            if (mediaImage != null) {
                mediaImage.setVisibility(View.GONE);
            }
            if (mediaIcon != null) {
                mediaIcon.setVisibility(View.VISIBLE);
                mediaIcon.setImageResource(R.drawable.ic_attachment);

                // Set click listener for attachment
                mediaIcon.setOnClickListener(v -> {
                    if (clickListener != null) {
                        clickListener.onAttachmentClick(attachment, position);
                    }
                });
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error displaying generic attachment: " + e.getMessage(), e);
        }
    }

    private void setupMessageClickListeners(View itemView, View translateButton, Message message, int position) {
        if (itemView == null || message == null) {
            return;
        }

        // Set click listener for the whole message
        itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onMessageClick(message, position);
            }
        });

        // Set long click listener for the whole message
        itemView.setOnLongClickListener(v -> {
            if (clickListener != null) {
                clickListener.onMessageLongClick(message, position);
                return true;
            }
            return false;
        });

        // Set up translate button using the new method
        setupTranslateButton(translateButton, message, position);
    }

    /**
     * New method for handling translate button based on its type
     */
    private void setupTranslateButton(View translateButton, Message message, int position) {
        if (translateButton != null) {
            try {
                if (message.isTranslatable()) {
                    translateButton.setVisibility(View.VISIBLE);

                    if (translateButton instanceof ImageButton) {
                        if (message.isTranslated()) {
                            ((ImageButton) translateButton).setImageResource(R.drawable.ic_restore);
                        } else {
                            ((ImageButton) translateButton).setImageResource(R.drawable.ic_translate);
                        }
                    } else if (translateButton instanceof ImageView) {
                        if (message.isTranslated()) {
                            ((ImageView) translateButton).setImageResource(R.drawable.ic_restore);
                        } else {
                            ((ImageView) translateButton).setImageResource(R.drawable.ic_translate);
                        }
                    }

                    translateButton.setOnClickListener(v -> {
                        if (clickListener != null) {
                            clickListener.onTranslateClick(message, position);
                        }
                    });
                } else {
                    translateButton.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                translateButton.setVisibility(View.GONE);
            }
        }
    }

    private void setupReactions(LinearLayout reactionsLayout, View addReactionButton, Message message, int position) {
        if (reactionsLayout == null || message == null) {
            return;
        }

        try {
            // Check if message has reactions
            if (message.hasReactions() && message.getReactions() != null && !message.getReactions().isEmpty()) {
                reactionsLayout.setVisibility(View.VISIBLE);
                reactionsLayout.removeAllViews();

                // Add reaction views
                for (MessageReaction reaction : message.getReactions()) {
                    if (reaction == null) continue;

                    View reactionView = LayoutInflater.from(context).inflate(R.layout.reaction_item, reactionsLayout, false);
                    if (reactionView == null) continue;

                    TextView emojiText = reactionView.findViewById(R.id.emoji_text);
                    TextView countText = reactionView.findViewById(R.id.count_text);

                    if (emojiText != null && !TextUtils.isEmpty(reaction.getEmoji())) {
                        emojiText.setText(reaction.getEmoji());
                    }

                    if (countText != null) {
                        countText.setText(String.valueOf(reaction.getCount()));
                    }

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
        } catch (Exception e) {
            reactionsLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < 0 || position >= messages.size() || messages.get(position) == null) {
            return VIEW_TYPE_INCOMING; // Default to incoming if position is invalid
        }

        Message message = messages.get(position);

        try {
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
        } catch (Exception e) {
            return VIEW_TYPE_INCOMING; // Default to incoming if there's an error
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

            dateText = itemView.findViewById(R.id.message_date);

            translateButton = itemView.findViewById(R.id.translate_button);

            // Try to find reactions layout with either ID
            reactionsLayout = itemView.findViewById(R.id.reactions_layout);
            if (reactionsLayout == null) {
                reactionsLayout = itemView.findViewById(R.id.reactions_container);
            }

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

            dateText = itemView.findViewById(R.id.message_date);

            messageStatus = itemView.findViewById(R.id.message_status);
            translateButton = itemView.findViewById(R.id.translate_button);

            // Try to find reactions layout with either ID
            reactionsLayout = itemView.findViewById(R.id.reactions_layout);
            if (reactionsLayout == null) {
                reactionsLayout = itemView.findViewById(R.id.reactions_container);
            }

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
        ImageView mediaImage; // Add reference to media_image
        LinearLayout reactionsLayout;
        View addReactionButton;

        IncomingMediaMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);

            dateText = itemView.findViewById(R.id.message_date);

            translateButton = itemView.findViewById(R.id.translate_button);
            mediaContainer = itemView.findViewById(R.id.media_container);
            mediaIcon = itemView.findViewById(R.id.media_icon);
            mediaImage = itemView.findViewById(R.id.media_image); // Add reference to media_image

            // Try to find reactions layout with either ID
            reactionsLayout = itemView.findViewById(R.id.reactions_layout);
            if (reactionsLayout == null) {
                reactionsLayout = itemView.findViewById(R.id.reactions_container);
            }

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
        ImageView mediaImage; // Add reference to media_image
        LinearLayout reactionsLayout;
        View addReactionButton;

        OutgoingMediaMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);

            dateText = itemView.findViewById(R.id.message_date);

            messageStatus = itemView.findViewById(R.id.message_status);
            translateButton = itemView.findViewById(R.id.translate_button);
            mediaContainer = itemView.findViewById(R.id.media_container);
            mediaIcon = itemView.findViewById(R.id.media_icon);
            mediaImage = itemView.findViewById(R.id.media_image); // Add reference to media_image

            // Try to find reactions layout with either ID
            reactionsLayout = itemView.findViewById(R.id.reactions_layout);
            if (reactionsLayout == null) {
                reactionsLayout = itemView.findViewById(R.id.reactions_container);
            }

            addReactionButton = itemView.findViewById(R.id.add_reaction_button);
        }
    }
}
