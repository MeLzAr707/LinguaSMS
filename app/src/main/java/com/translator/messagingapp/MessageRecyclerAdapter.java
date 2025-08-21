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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

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

        void onAttachmentLongClick(MmsMessage.Attachment attachment, int position);

        void onAttachmentLongClick(Uri uri, int position);

        void onReactionClick(Message message, int position);

        void onAddReactionClick(Message message, int position);
    }

    /**
     * Creates a new MessageRecyclerAdapter.
     *
     * @param context  The context
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
        TextView originalText;
        TextView dateText;
        View translateButton;
        LinearLayout reactionsLayout;

        MessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            originalText = itemView.findViewById(R.id.original_text);
            dateText = itemView.findViewById(R.id.message_date); // Fixed ID
            translateButton = itemView.findViewById(R.id.translate_button);
            reactionsLayout = itemView.findViewById(R.id.reactions_container); // Fixed ID
        }

        void bind(final Message message, final int position) {
            if (message == null) {
                return; // Safety check for null message
            }

            // Get search query for highlighting
            String searchQuery = message.getSearchQuery();
            boolean hasSearchQuery = searchQuery != null && !searchQuery.trim().isEmpty();

            // Handle dual text display for translations
            if (message.isShowTranslation() && message.isTranslated()) {
                // Show both original and translated text
                String originalBody = getOriginalTextForMessage(message);
                String translatedText = message.getTranslatedText();
                
                if (originalText != null) {
                    String originalLabel = "Original: " + originalBody;
                    // Apply highlighting to original text if search query exists
                    if (hasSearchQuery) {
                        originalText.setText(SearchHighlightUtils.highlightSearchTerms(originalLabel, searchQuery));
                    } else {
                        originalText.setText(originalLabel);
                    }
                    originalText.setVisibility(View.VISIBLE);
                }
                
                // Apply highlighting to translated text if search query exists
                if (hasSearchQuery) {
                    messageText.setText(SearchHighlightUtils.highlightSearchTerms(translatedText, searchQuery));
                } else {
                    messageText.setText(translatedText);
                }
            } else {
                // Show only original text
                String displayText = getDisplayTextForMessage(message);
                
                // Apply highlighting if search query exists
                if (hasSearchQuery) {
                    messageText.setText(SearchHighlightUtils.highlightSearchTerms(displayText, searchQuery));
                } else {
                    messageText.setText(displayText);
                }
                
                if (originalText != null) {
                    originalText.setVisibility(View.GONE);
                }
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
                    // Use dark background for Black Glass theme to differentiate from outgoing messages
                    messageCard.setCardBackgroundColor(context.getResources().getColor(R.color.background_dark));
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
        private CardView messageCard;

        OutgoingMessageViewHolder(View itemView) {
            super(itemView);
            messageCard = itemView.findViewById(R.id.message_card);
        }

        @Override
        void bind(Message message, int position) {
            super.bind(message, position);

            // Apply theme-specific styling for outgoing messages
            if (messageCard != null) {
                UserPreferences userPreferences = new UserPreferences(context);
                if (userPreferences.isUsingBlackGlassTheme()) {
                    // Use deep dark blue for Black Glass theme
                    messageCard.setCardBackgroundColor(context.getResources().getColor(R.color.deep_dark_blue));
                } else {
                    // Use theme-aware default color (colorPrimary for outgoing messages)
                    messageCard.setCardBackgroundColor(context.getResources().getColor(R.color.colorPrimary));
                }
            }
        }
    }

    /**
     * Base view holder for media messages.
     */
    abstract class MediaMessageViewHolder extends MessageViewHolder {
        ImageView mediaImage;

        MediaMessageViewHolder(View itemView) {
            super(itemView);
            mediaImage = itemView.findViewById(R.id.media_image);
        }

        @Override
        void bind(final Message message, final int position) {
            // Call parent bind method for common functionality first
            super.bind(message, position);

            // Set up media-specific functionality
            if (message.hasAttachments() && message instanceof MmsMessage) {
                MmsMessage mmsMessage = (MmsMessage) message;
                List<MmsMessage.Attachment> attachmentObjects = mmsMessage.getAttachmentObjects();
                
                if (attachmentObjects != null && !attachmentObjects.isEmpty()) {
                    // For simplicity, just show the first attachment
                    // In a real app, you'd handle multiple attachments
                    MmsMessage.Attachment attachment = attachmentObjects.get(0);
                    Uri attachmentUri = attachment.getUri();
                    
                    if (attachmentUri != null && (attachment.isImage() || attachment.isVideo())) {
                        // Load image/video using Glide
                        loadMediaWithGlide(attachmentUri, attachment);
                        
                        // Hide text when showing image and there's no text content
                        if (messageText != null) {
                            String body = message.getBody();
                            boolean hasTextContent = (body != null && !body.trim().isEmpty()) || 
                                                   (message.isTranslated() && message.getTranslatedText() != null && !message.getTranslatedText().trim().isEmpty());
                            
                            if (hasTextContent) {
                                messageText.setVisibility(View.VISIBLE);
                                // Original text visibility is handled by parent bind method
                            } else {
                                messageText.setVisibility(View.GONE);
                                // Also hide original text if main text is hidden
                                if (originalText != null) {
                                    originalText.setVisibility(View.GONE);
                                }
                            }
                        }
                        
                        mediaImage.setOnClickListener(v -> {
                            if (listener != null) {
                                listener.onAttachmentClick(attachment, position);
                            }
                        });
                    } else if (attachmentUri != null) {
                        // For non-image attachments (video, audio), show placeholder
                        mediaImage.setImageResource(R.drawable.ic_attachment);
                        mediaImage.setScaleType(ImageView.ScaleType.CENTER);
                        
                        mediaImage.setOnClickListener(v -> {
                            if (listener != null) {
                                listener.onAttachmentClick(attachment, position);
                            }
                        });
                    }
                }
            } else if (message.hasAttachments()) {
                // Handle generic URI attachments
                List<Uri> attachments = message.getAttachments();
                if (attachments != null && !attachments.isEmpty()) {
                    Uri uri = attachments.get(0);

                    // Load image/video using Glide
                    loadMediaWithGlide(uri, null);
                    
                    // Hide text when showing image
                    if (messageText != null) {
                        String body = message.getBody();
                        if (body == null || body.trim().isEmpty()) {
                            messageText.setVisibility(View.GONE);
                        } else {
                            messageText.setVisibility(View.VISIBLE);
                        }
                    }

                    mediaImage.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onAttachmentClick(uri, position);
                        }
                    });
                }
            }
        }
        
        /**
         * Load media (image/video) using Glide with proper error handling
         */
        private void loadMediaWithGlide(Uri mediaUri, MmsMessage.Attachment attachment) {
            if (mediaUri == null || mediaImage == null) {
                return;
            }
            
            try {
                RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.ic_attachment)
                    .error(R.drawable.ic_attachment)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop();
                
                Glide.with(context)
                    .load(mediaUri)
                    .apply(options)
                    .into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull android.graphics.drawable.Drawable resource, 
                                                  com.bumptech.glide.request.transition.Transition<? super android.graphics.drawable.Drawable> transition) {
                            if (mediaImage != null) {
                                mediaImage.setImageDrawable(resource);
                                mediaImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                mediaImage.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onLoadCleared(android.graphics.drawable.Drawable placeholder) {
                            if (mediaImage != null && placeholder != null) {
                                mediaImage.setImageDrawable(placeholder);
                            }
                        }
                        
                        @Override
                        public void onLoadFailed(android.graphics.drawable.Drawable errorDrawable) {
                            // Show attachment icon for failed loads
                            if (mediaImage != null) {
                                mediaImage.setImageResource(R.drawable.ic_attachment);
                                mediaImage.setScaleType(ImageView.ScaleType.CENTER);
                                mediaImage.setVisibility(View.VISIBLE);
                            }
                        }
                    });
            } catch (Exception e) {
                // Fallback to attachment icon
                if (mediaImage != null) {
                    mediaImage.setImageResource(R.drawable.ic_attachment);
                    mediaImage.setScaleType(ImageView.ScaleType.CENTER);
                    mediaImage.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    /**
     * View holder for incoming media messages.
     */
    class IncomingMediaMessageViewHolder extends MediaMessageViewHolder {
        private CardView messageCard;

        IncomingMediaMessageViewHolder(View itemView) {
            super(itemView);
            messageCard = itemView.findViewById(R.id.message_card);
        }

        @Override
        void bind(Message message, int position) {
            super.bind(message, position);

            // Apply theme-specific styling for incoming media messages
            if (messageCard != null) {
                UserPreferences userPreferences = new UserPreferences(context);
                if (userPreferences.isUsingBlackGlassTheme()) {
                    // Use dark background for Black Glass theme to differentiate from outgoing messages
                    messageCard.setCardBackgroundColor(context.getResources().getColor(R.color.background_dark));
                } else {
                    // Use theme-aware default color (will be overridden by theme)
                    messageCard.setCardBackgroundColor(context.getResources().getColor(R.color.incoming_message_background));
                }
            }
        }
    }

    /**
     * View holder for outgoing media messages.
     */
    class OutgoingMediaMessageViewHolder extends MediaMessageViewHolder {
        private CardView messageCard;

        OutgoingMediaMessageViewHolder(View itemView) {
            super(itemView);
            messageCard = itemView.findViewById(R.id.message_card);
        }

        @Override
        void bind(Message message, int position) {
            super.bind(message, position);

            // Apply theme-specific styling for outgoing media messages
            if (messageCard != null) {
                UserPreferences userPreferences = new UserPreferences(context);
                if (userPreferences.isUsingBlackGlassTheme()) {
                    // Use deep dark blue for Black Glass theme
                    messageCard.setCardBackgroundColor(context.getResources().getColor(R.color.deep_dark_blue));
                } else {
                    // Use theme-aware default color (colorPrimary for outgoing messages)
                    messageCard.setCardBackgroundColor(context.getResources().getColor(R.color.colorPrimary));
                }
            }
        }
    }

    /**
     * Gets the appropriate display text for a message, handling special cases for RCS messages.
     * This method always returns the original text, not the translated text.
     */
    private String getDisplayTextForMessage(Message message) {
        return getOriginalTextForMessage(message);
    }

    /**
     * Gets the original text for a message, handling special cases for RCS messages.
     */
    private String getOriginalTextForMessage(Message message) {
        // Handle message body
        String body = message.getBody();

        // Handle MMS messages specially
        if (message.isMms()) {
            boolean hasAttachments = message.hasAttachments();
            boolean hasText = body != null && !body.trim().isEmpty();
            
            if (hasText && hasAttachments) {
                // MMS with both text and attachments
                return body + " 📎";
            } else if (hasText) {
                // MMS with only text
                return body;
            } else if (hasAttachments) {
                // MMS with only attachments
                return "[Media Message]";
            } else {
                // MMS with no content (likely loading issue)
                return "[MMS Message]";
            }
        }

        // Handle null or empty body for non-MMS messages
        if (body == null || body.trim().isEmpty()) {
            // For RCS messages, provide a more descriptive placeholder
            if (message.isRcs()) {
                return "[RCS Message - Content not available]";
            }
            // For empty SMS messages
            else {
                return "[Empty Message]";
            }
        }

        return body;
    }

    /**
     * Updates text sizes for all visible message views.
     * Called when text size is changed via pinch-to-zoom.
     */
    public void updateTextSizes() {
        notifyDataSetChanged();
    }
}