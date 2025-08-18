package com.translator.messagingapp;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

/**
 * RecyclerView adapter for displaying messages in a conversation.
 */
public class MessageRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_INCOMING = 1;
    private static final int VIEW_TYPE_OUTGOING = 2;
    private static final int VIEW_TYPE_INCOMING_MEDIA = 3;
    private static final int VIEW_TYPE_OUTGOING_MEDIA = 4;

    private final Context context;
    private final List<Message> messages;
    private final OnMessageClickListener listener;

    /**
     * Interface for message click events.
     */
    public interface OnMessageClickListener {
        void onMessageClick(Message message, int position);
        void onMessageLongClick(Message message, int position);
        void onTranslateClick(Message message, int position);
        void onAttachmentClick(MmsMessage.Attachment attachment, int position);
        void onAttachmentClick(Uri uri, int position);
        void onReactionClick(Message message, int position);
        void onAddReactionClick(Message message, int position);
    }

    /**
     * Creates a new MessageRecyclerAdapter.
     *
     * @param context The context
     * @param messages The list of messages
     * @param listener The click listener
     */
    public MessageRecyclerAdapter(Context context, List<Message> messages, OnMessageClickListener listener) {
        this.context = context;
        this.messages = messages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;

        switch (viewType) {
            case VIEW_TYPE_INCOMING:
                view = inflater.inflate(R.layout.item_message_incoming, parent, false);
                return new IncomingMessageViewHolder(view);
            case VIEW_TYPE_OUTGOING:
                view = inflater.inflate(R.layout.item_message_outgoing, parent, false);
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
        if (position >= messages.size()) {
            return; // Safety check for position bounds
        }
        
        Message message = messages.get(position);
        if (message == null) {
            return; // Safety check for null message
        }

        if (holder instanceof MessageViewHolder) {
            ((MessageViewHolder) holder).bind(message, position);
        } else if (holder instanceof MediaMessageViewHolder) {
            ((MediaMessageViewHolder) holder).bind(message, position);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        boolean isIncoming = message.getType() == Message.TYPE_INBOX;
        boolean hasAttachments = message.hasAttachments();

        if (isIncoming) {
            return hasAttachments ? VIEW_TYPE_INCOMING_MEDIA : VIEW_TYPE_INCOMING;
        } else {
            return hasAttachments ? VIEW_TYPE_OUTGOING_MEDIA : VIEW_TYPE_OUTGOING;
        }
    }

    /**
     * Base view holder for messages.
     */
    abstract class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView dateText;
        View translateButton;
        LinearLayout reactionsLayout;

        MessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            dateText = itemView.findViewById(R.id.message_date); // Fixed ID
            translateButton = itemView.findViewById(R.id.translate_button);
            reactionsLayout = itemView.findViewById(R.id.reactions_container); // Fixed ID
        }

        void bind(final Message message, final int position) {
            if (message == null) {
                return; // Safety check for null message
            }
            
            // Set message text
            if (message.isShowTranslation() && message.isTranslated()) {
                messageText.setText(message.getTranslatedText());
            } else {
                messageText.setText(message.getBody() != null ? message.getBody() : "");
            }

            // Set date
            if (dateText != null) {
                dateText.setText(message.getFormattedDate());
            }

            // Set up click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMessageClick(message, position);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onMessageLongClick(message, position);
                    return true;
                }
                return false;
            });

            // Set up translate button
            if (translateButton != null) {
                if (message.isTranslatable()) {
                    translateButton.setVisibility(View.VISIBLE);

                    // Cast to ImageButton to use setImageResource
                    if (translateButton instanceof ImageButton) {
                        ImageButton translateImageButton = (ImageButton) translateButton;
                        if (message.isShowTranslation() && message.isTranslated()) {
                            translateImageButton.setImageResource(R.drawable.ic_restore);
                        } else {
                            translateImageButton.setImageResource(R.drawable.ic_translate);
                        }
                    }

                    translateButton.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onTranslateClick(message, position);
                        }
                    });
                } else {
                    translateButton.setVisibility(View.GONE);
                }
            }

            // Set up reactions
            setupReactions(message, position);
        }

        void setupReactions(Message message, int position) {
            if (reactionsLayout != null) {
                reactionsLayout.removeAllViews();

                if (message.hasReactions()) {
                    reactionsLayout.setVisibility(View.VISIBLE);

                    // Get reaction counts
                    Map<String, Integer> reactionCounts = message.getReactionManager().getReactionCounts();

                    // Add reaction views
                    LayoutInflater inflater = LayoutInflater.from(context);
                    for (Map.Entry<String, Integer> entry : reactionCounts.entrySet()) {
                        String emoji = entry.getKey();
                        int count = entry.getValue();

                        View reactionView = inflater.inflate(R.layout.reaction_item, reactionsLayout, false);
                        TextView emojiText = reactionView.findViewById(R.id.reaction_emoji); // Fixed ID
                        TextView countText = reactionView.findViewById(R.id.reaction_count); // Fixed ID

                        emojiText.setText(emoji);
                        countText.setText(String.valueOf(count));

                        reactionView.setOnClickListener(v -> {
                            if (listener != null) {
                                listener.onReactionClick(message, position);
                            }
                        });

                        reactionsLayout.addView(reactionView);
                    }

                    // Add "add reaction" button
                    View addReactionView = inflater.inflate(R.layout.reaction_item, reactionsLayout, false);
                    TextView addEmojiText = addReactionView.findViewById(R.id.reaction_emoji); // Fixed ID
                    TextView addCountText = addReactionView.findViewById(R.id.reaction_count); // Fixed ID

                    addEmojiText.setText("+");
                    addCountText.setVisibility(View.GONE);

                    addReactionView.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onAddReactionClick(message, position);
                        }
                    });

                    reactionsLayout.addView(addReactionView);
                } else {
                    reactionsLayout.setVisibility(View.GONE);
                }
            }
        }
    }

    /**
     * View holder for incoming messages.
     */
    class IncomingMessageViewHolder extends MessageViewHolder {
        private CardView messageCard;
        
        IncomingMessageViewHolder(View itemView) {
            super(itemView);
            messageCard = itemView.findViewById(R.id.message_card);
        }

        @Override
        void bind(Message message, int position) {
            super.bind(message, position);

            // Apply theme-specific styling for incoming messages
            if (messageCard != null) {
                UserPreferences userPreferences = new UserPreferences(context);
                if (userPreferences.isUsingBlackGlassTheme()) {
                    // Use deep dark blue for Black Glass theme
                    messageCard.setCardBackgroundColor(context.getResources().getColor(R.color.deep_dark_blue));
                } else {
                    // Use theme-aware default color (will be overridden by theme)
                    messageCard.setCardBackgroundColor(context.getResources().getColor(R.color.incoming_message_background));
                }
            }
        }
    }

    /**
     * View holder for outgoing messages.
     */
    class OutgoingMessageViewHolder extends MessageViewHolder {
        OutgoingMessageViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        void bind(Message message, int position) {
            super.bind(message, position);

            // Add any outgoing-specific binding here
            if (message.getType() == Message.TYPE_INBOX || message.getType() == Message.TYPE_ALL) {
                // Outgoing message specific styling
            }
        }
    }

    /**
     * Base view holder for media messages.
     */
    abstract class MediaMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView dateText;
        ImageView mediaImage;
        View translateButton;
        LinearLayout reactionsLayout;

        MediaMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            dateText = itemView.findViewById(R.id.message_date); // Fixed ID
            mediaImage = itemView.findViewById(R.id.media_image);
            translateButton = itemView.findViewById(R.id.translate_button);
            reactionsLayout = itemView.findViewById(R.id.reactions_container); // Fixed ID
        }

        void bind(final Message message, final int position) {
            // Set message text
            if (!TextUtils.isEmpty(message.getBody())) {
                messageText.setVisibility(View.VISIBLE);
                if (message.isShowTranslation() && message.isTranslated()) {
                    messageText.setText(message.getTranslatedText());
                } else {
                    messageText.setText(message.getBody());
                }
            } else {
                messageText.setVisibility(View.GONE);
            }

            // Set date
            dateText.setText(message.getFormattedDate());

            // Set up media
            if (message.hasAttachments() && message instanceof MmsMessage) {
                MmsMessage mmsMessage = (MmsMessage) message;
                if (!mmsMessage.getAttachments().isEmpty()) {
                    // For simplicity, just show the first attachment
                    // In a real app, you'd handle multiple attachments
                    Uri attachment = mmsMessage.getAttachments().get(0);

                    // Load image using Glide or similar library
                    // Glide.with(context).load(attachment.getUri()).into(mediaImage);

                    mediaImage.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onAttachmentClick(attachment, position);
                        }
                    });
                }
            } else if (message.hasAttachments()) {
                // Handle generic URI attachments
                List<Uri> attachments = message.getAttachments();
                if (attachments != null && !attachments.isEmpty()) {
                    Uri uri = attachments.get(0);

                    // Load image using Glide or similar library
                    // Glide.with(context).load(uri).into(mediaImage);

                    mediaImage.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onAttachmentClick(uri, position);
                        }
                    });
                }
            }

            // Set up click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMessageClick(message, position);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onMessageLongClick(message, position);
                    return true;
                }
                return false;
            });

            // Set up translate button
            if (translateButton != null && !TextUtils.isEmpty(message.getBody())) {
                if (message.isTranslatable()) {
                    translateButton.setVisibility(View.VISIBLE);

                    // Cast to ImageButton to use setImageResource
                    if (translateButton instanceof ImageButton) {
                        ImageButton translateImageButton = (ImageButton) translateButton;
                        if (message.isShowTranslation() && message.isTranslated()) {
                            translateImageButton.setImageResource(R.drawable.ic_restore);
                        } else {
                            translateImageButton.setImageResource(R.drawable.ic_translate);
                        }
                    }

                    translateButton.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onTranslateClick(message, position);
                        }
                    });
                } else {
                    translateButton.setVisibility(View.GONE);
                }
            } else if (translateButton != null) {
                translateButton.setVisibility(View.GONE);
            }

            // Set up reactions
            setupReactions(message, position);
        }

        void setupReactions(Message message, int position) {
            if (reactionsLayout != null) {
                reactionsLayout.removeAllViews();

                if (message.hasReactions()) {
                    reactionsLayout.setVisibility(View.VISIBLE);

                    // Get reaction counts
                    Map<String, Integer> reactionCounts = message.getReactionManager().getReactionCounts();

                    // Add reaction views
                    LayoutInflater inflater = LayoutInflater.from(context);
                    for (Map.Entry<String, Integer> entry : reactionCounts.entrySet()) {
                        String emoji = entry.getKey();
                        int count = entry.getValue();

                        View reactionView = inflater.inflate(R.layout.reaction_item, reactionsLayout, false);
                        TextView emojiText = reactionView.findViewById(R.id.reaction_emoji); // Fixed ID
                        TextView countText = reactionView.findViewById(R.id.reaction_count); // Fixed ID

                        emojiText.setText(emoji);
                        countText.setText(String.valueOf(count));

                        reactionView.setOnClickListener(v -> {
                            if (listener != null) {
                                listener.onReactionClick(message, position);
                            }
                        });

                        reactionsLayout.addView(reactionView);
                    }

                    // Add "add reaction" button
                    View addReactionView = inflater.inflate(R.layout.reaction_item, reactionsLayout, false);
                    TextView addEmojiText = addReactionView.findViewById(R.id.reaction_emoji); // Fixed ID
                    TextView addCountText = addReactionView.findViewById(R.id.reaction_count); // Fixed ID

                    addEmojiText.setText("+");
                    addCountText.setVisibility(View.GONE);

                    addReactionView.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onAddReactionClick(message, position);
                        }
                    });

                    reactionsLayout.addView(addReactionView);
                } else {
                    reactionsLayout.setVisibility(View.GONE);
                }
            }
        }
    }

    /**
     * View holder for incoming media messages.
     */
    class IncomingMediaMessageViewHolder extends MediaMessageViewHolder {
        IncomingMediaMessageViewHolder(View itemView) {
            super(itemView);
        }
    }

    /**
     * View holder for outgoing media messages.
     */
    class OutgoingMediaMessageViewHolder extends MediaMessageViewHolder {
        OutgoingMediaMessageViewHolder(View itemView) {
            super(itemView);
        }
    }
}