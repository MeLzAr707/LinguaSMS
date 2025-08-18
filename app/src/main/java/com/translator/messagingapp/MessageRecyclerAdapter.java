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
     * Common method to set up reactions for any message view holder.
     * This method is shared between MessageViewHolder and MediaMessageViewHolder.
     */
    private void setupReactions(Message message, int position, LinearLayout reactionsLayout) {
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
            
            // Set message text with better null handling
            String messageBody;
            if (message.isShowTranslation() && message.isTranslated()) {
                messageBody = message.getTranslatedText();
            } else {
                messageBody = message.getBody();
            }
            
            // Handle null or empty message body
            if (messageBody == null || messageBody.trim().isEmpty()) {
                if (message.hasAttachments()) {
                    messageBody = "[Media message]";
                } else {
                    messageBody = "[No content]";
                }
            }
            
            messageText.setText(messageBody);

            // Set date
            if (dateText != null) {
                String formattedDate = message.getFormattedDate();
                dateText.setText(formattedDate != null ? formattedDate : "");
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
            MessageRecyclerAdapter.this.setupReactions(message, position, reactionsLayout);
        }
    }

    /**
     * View holder for incoming messages.
     */
    class IncomingMessageViewHolder extends MessageViewHolder {
        IncomingMessageViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        void bind(Message message, int position) {
            super.bind(message, position);

            // Add any incoming-specific binding here
            if (message.getType() == Message.TYPE_INBOX || message.getType() == Message.TYPE_ALL) {
                // Incoming message specific styling
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
            if (message == null) {
                return; // Safety check for null message
            }
            
            // Set message text with improved null handling
            String messageBody = message.getBody();
            if (!TextUtils.isEmpty(messageBody)) {
                messageText.setVisibility(View.VISIBLE);
                if (message.isShowTranslation() && message.isTranslated()) {
                    messageText.setText(message.getTranslatedText());
                } else {
                    messageText.setText(messageBody);
                }
            } else {
                // Show placeholder for media-only messages
                messageText.setVisibility(View.VISIBLE);
                messageText.setText("[Media message]");
            }

            // Set date with null check
            if (dateText != null) {
                String formattedDate = message.getFormattedDate();
                dateText.setText(formattedDate != null ? formattedDate : "");
            }

            // Set up media with better error handling
            if (mediaImage != null) {
                if (message.hasAttachments() && message instanceof MmsMessage) {
                    MmsMessage mmsMessage = (MmsMessage) message;
                    List<Uri> attachments = mmsMessage.getAttachments();
                    
                    if (attachments != null && !attachments.isEmpty()) {
                        mediaImage.setVisibility(View.VISIBLE);
                        
                        // For simplicity, just show the first attachment
                        // In a real app, you'd handle multiple attachments
                        Uri attachment = attachments.get(0);

                        // TODO: Load image using Glide or similar library
                        // For now, show a placeholder
                        mediaImage.setImageResource(android.R.drawable.ic_menu_gallery);

                        mediaImage.setOnClickListener(v -> {
                            if (listener != null) {
                                listener.onAttachmentClick(attachment, position);
                            }
                        });
                    } else {
                        mediaImage.setVisibility(View.GONE);
                    }
                } else if (message.hasAttachments()) {
                    // Handle generic URI attachments
                    List<Uri> attachments = message.getAttachments();
                    if (attachments != null && !attachments.isEmpty()) {
                        mediaImage.setVisibility(View.VISIBLE);
                        Uri uri = attachments.get(0);

                        // TODO: Load image using Glide or similar library
                        // For now, show a placeholder
                        mediaImage.setImageResource(android.R.drawable.ic_menu_gallery);

                        mediaImage.setOnClickListener(v -> {
                            if (listener != null) {
                                listener.onAttachmentClick(uri, position);
                            }
                        });
                    } else {
                        mediaImage.setVisibility(View.GONE);
                    }
                } else {
                    mediaImage.setVisibility(View.GONE);
                }
            }
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
            MessageRecyclerAdapter.this.setupReactions(message, position, reactionsLayout);
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