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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * RecyclerView adapter for displaying messages in a conversation.
 */
public class MessageRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "MessageRecyclerAdapter";
    private static final int VIEW_TYPE_INCOMING = 1;
    private static final int VIEW_TYPE_OUTGOING = 2;
    private static final int VIEW_TYPE_INCOMING_MEDIA = 3;
    private static final int VIEW_TYPE_OUTGOING_MEDIA = 4;

    private final Context context;
    private final List<Message> messages;
    private final OnMessageClickListener listener;
    private boolean isGroupConversation = false;

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
        // Auto-detect if this is a group conversation based on multiple unique addresses
        this.isGroupConversation = detectGroupConversation(messages);
    }

    /**
     * Set whether this is a group conversation.
     * This will affect how sender information is displayed for incoming messages.
     */
    public void setGroupConversation(boolean isGroupConversation) {
        this.isGroupConversation = isGroupConversation;
        notifyDataSetChanged();
    }

    /**
     * Detects if this is a group conversation by checking if there are multiple unique sender addresses.
     */
    private boolean detectGroupConversation(List<Message> messages) {
        if (messages == null || messages.size() < 2) {
            return false;
        }

        Set<String> uniqueAddresses = new HashSet<>();
        for (Message message : messages) {
            if (message != null && message.getAddress() != null && message.getType() == Message.TYPE_INBOX) {
                uniqueAddresses.add(message.getAddress());
                if (uniqueAddresses.size() > 1) {
                    return true; // Multiple senders = group conversation
                }
            }
        }
        return false;
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
        TextView senderName;
        View translateButton;
        LinearLayout reactionsLayout;

        MessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            originalText = itemView.findViewById(R.id.original_text);
            dateText = itemView.findViewById(R.id.message_date); // Fixed ID
            senderName = itemView.findViewById(R.id.sender_name);
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
            // SAFETY CHECK: Ensure messages always display, even with corrupted translation state
            if (message.isShowTranslation() && message.isTranslated() && 
                message.getTranslatedText() != null && !message.getTranslatedText().trim().isEmpty()) {
                // Show both original and translated text (only if translated text is valid)
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
                // Show only original text (this is the safe default path)
                // This ensures messages are ALWAYS visible, even with translation issues
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

            // Set sender name for group conversations (only for incoming messages)
            if (senderName != null) {
                if (isGroupConversation && message.getType() == Message.TYPE_INBOX) {
                    String senderDisplayName = getSenderDisplayName(message);
                    if (!TextUtils.isEmpty(senderDisplayName)) {
                        senderName.setText(senderDisplayName);
                        senderName.setVisibility(View.VISIBLE);
                    } else {
                        senderName.setVisibility(View.GONE);
                    }
                } else {
                    senderName.setVisibility(View.GONE);
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
        ImageView playButtonOverlay;

        MediaMessageViewHolder(View itemView) {
            super(itemView);
            mediaImage = itemView.findViewById(R.id.media_image);
            playButtonOverlay = itemView.findViewById(R.id.play_button_overlay);
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
                        
                        // Show play button overlay for videos
                        if (playButtonOverlay != null) {
                            if (attachment.isVideo()) {
                                playButtonOverlay.setVisibility(View.VISIBLE);
                            } else {
                                playButtonOverlay.setVisibility(View.GONE);
                            }
                        }
                        
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
                        
                        // Add long press listener for context menu
                        mediaImage.setOnLongClickListener(v -> {
                            if (listener != null) {
                                listener.onAttachmentLongClick(attachment, position);
                            }
                            return true;
                        });
                    } else if (attachmentUri != null) {
                        // For audio, document and other non-media attachments, show placeholder
                        mediaImage.setImageResource(R.drawable.ic_attachment);
                        mediaImage.setScaleType(ImageView.ScaleType.CENTER);
                        
                        // Hide play button overlay for non-video attachments
                        if (playButtonOverlay != null) {
                            playButtonOverlay.setVisibility(View.GONE);
                        }
                        
                        mediaImage.setOnClickListener(v -> {
                            if (listener != null) {
                                listener.onAttachmentClick(attachment, position);
                            }
                        });
                        
                        // Add long press listener for context menu
                        mediaImage.setOnLongClickListener(v -> {
                            if (listener != null) {
                                listener.onAttachmentLongClick(attachment, position);
                            }
                            return true;
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
                    
                    // For generic URI attachments, we can't easily determine if it's a video
                    // so we'll hide the play button overlay by default
                    if (playButtonOverlay != null) {
                        playButtonOverlay.setVisibility(View.GONE);
                    }
                    
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
                    
                    // Add long press listener for context menu
                    mediaImage.setOnLongClickListener(v -> {
                        if (listener != null) {
                            listener.onAttachmentLongClick(uri, position);
                        }
                        return true;
                    });
                }
            }
        }
        
        /**
         * Load media (image/video) using Glide with proper error handling and responsive sizing
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
                    .fitCenter(); // Changed from centerCrop to fitCenter for better aspect ratio handling
                
                Glide.with(context)
                    .load(mediaUri)
                    .apply(options)
                    .into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull android.graphics.drawable.Drawable resource, 
                                                  com.bumptech.glide.request.transition.Transition<? super android.graphics.drawable.Drawable> transition) {
                            if (mediaImage != null) {
                                mediaImage.setImageDrawable(resource);
                                // Use fitCenter to maintain aspect ratio while filling available width
                                mediaImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
                                mediaImage.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onLoadCleared(android.graphics.drawable.Drawable placeholder) {
                            if (mediaImage != null && placeholder != null) {
                                mediaImage.setImageDrawable(placeholder);
                                mediaImage.setScaleType(ImageView.ScaleType.CENTER);
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
     * Updates the messages list using DiffUtil for efficient RecyclerView updates.
     * This method calculates the difference between the old and new message lists
     * and only updates the items that have changed, improving performance.
     *
     * @param newMessages The new list of messages to display
     */
    public void updateMessages(List<Message> newMessages) {
        if (newMessages == null) {
            return;
        }
        
        // Clear contact name cache when messages are updated to ensure fresh lookups
        Log.d(TAG, "Clearing contact name cache due to message update");
        contactNameCache.clear();
        
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new MessageDiffCallback(messages, newMessages));
        
        messages.clear();
        messages.addAll(newMessages);
        
        diffResult.dispatchUpdatesTo(this);
    }

    /**
     * Updates text sizes for all visible message views.
     * Called when text size is changed via pinch-to-zoom.
     */
    public void updateTextSizes() {
        notifyDataSetChanged();
    }

    /**
     * Clears the contact name cache to force fresh contact lookups.
     * This can be called if contacts have been updated in the system.
     */
    public void clearContactNameCache() {
        Log.d(TAG, "Manually clearing contact name cache");
        contactNameCache.clear();
        notifyDataSetChanged(); // Refresh the display to show updated contact names
    }

    // Cache for contact name lookups to avoid repeated queries and ensure consistency
    private final Map<String, String> contactNameCache = new HashMap<>();

    /**
     * Gets the display name for a message sender in group conversations.
     * Returns contact name if available, otherwise the phone number.
     * Uses caching to ensure consistent contact name resolution across messages.
     */
    private String getSenderDisplayName(Message message) {
        if (message == null || TextUtils.isEmpty(message.getAddress())) {
            return null;
        }

        String address = message.getAddress();
        Log.d(TAG, "Getting sender display name for address: " + address + " (message ID: " + message.getId() + ")");
        
        // First try to get contact name from message
        String contactName = message.getContactName();
        if (!TextUtils.isEmpty(contactName) && !"null".equals(contactName)) {
            Log.d(TAG, "Using contact name from message: " + contactName);
            return contactName;
        }

        // Check cache first to ensure consistent results
        if (contactNameCache.containsKey(address)) {
            String cachedName = contactNameCache.get(address);
            Log.d(TAG, "Using cached contact name for " + address + ": " + cachedName);
            return cachedName != null ? cachedName : formatPhoneNumberForDisplay(address);
        }

        // Try to lookup contact name using ContactUtils with the exact message address
        String lookedUpName = ContactUtils.getContactName(context, address);
        
        // Cache the result (even if null) to ensure consistency
        contactNameCache.put(address, lookedUpName);
        
        if (!TextUtils.isEmpty(lookedUpName)) {
            Log.d(TAG, "Resolved contact name for " + address + ": " + lookedUpName);
            return lookedUpName;
        }

        // Fall back to formatted phone number
        String fallbackName = formatPhoneNumberForDisplay(address);
        Log.d(TAG, "Using fallback formatted number for " + address + ": " + fallbackName);
        return fallbackName;
    }

    /**
     * Formats a phone number for display as sender name.
     */
    private String formatPhoneNumberForDisplay(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return "Unknown";
        }

        // Remove any non-digit characters for formatting
        String digitsOnly = phoneNumber.replaceAll("[^\\d]", "");
        
        // If it's a 10-digit US number, format as (XXX) XXX-XXXX
        if (digitsOnly.length() == 10) {
            return String.format("(%s) %s-%s", 
                digitsOnly.substring(0, 3), 
                digitsOnly.substring(3, 6), 
                digitsOnly.substring(6));
        } else if (digitsOnly.length() == 11 && digitsOnly.startsWith("1")) {
            // Handle 11-digit number with country code
            return String.format("+1 (%s) %s-%s", 
                digitsOnly.substring(1, 4), 
                digitsOnly.substring(4, 7), 
                digitsOnly.substring(7));
        }
        
        // For other formats, just return the original phone number
        return phoneNumber;
    }
}