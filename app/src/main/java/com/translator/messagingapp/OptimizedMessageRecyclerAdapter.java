
package com.translator.messagingapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Optimized RecyclerView adapter for displaying messages with lazy loading of attachments.
 */
public class OptimizedMessageRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "OptimizedMessageAdapter";
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_SENT_MMS = 3;
    private static final int VIEW_TYPE_RECEIVED_MMS = 4;
    private static final int VIEW_TYPE_LOADING = 5;

    private final Context context;
    private final List<Message> messages;
    private MessageClickListener clickListener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Executor imageLoadExecutor = Executors.newFixedThreadPool(3); // Use a thread pool for image loading
    private boolean showLoadingFooter = false;

    // Cache for attachment thumbnails and full images
    private final Map<String, Bitmap> thumbnailCache = new HashMap<>();
    private final Map<String, Bitmap> fullImageCache = new HashMap<>();

    /**
     * Creates a new OptimizedMessageRecyclerAdapter.
     *
     * @param context The context
     * @param messages The list of messages
     */
    public OptimizedMessageRecyclerAdapter(Context context, List<Message> messages) {
        this.context = context;
        this.messages = messages;
    }

    /**
     * Creates a new OptimizedMessageRecyclerAdapter with a click listener.
     *
     * @param context The context
     * @param messages The list of messages
     * @param clickListener The click listener
     */
    public OptimizedMessageRecyclerAdapter(Context context, List<Message> messages, MessageClickListener clickListener) {
        this.context = context;
        this.messages = messages;
        this.clickListener = clickListener;
    }

    /**
     * Sets the click listener.
     *
     * @param clickListener The click listener
     */
    public void setClickListener(MessageClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case VIEW_TYPE_SENT:
                return new SentMessageHolder(
                        inflater.inflate(R.layout.item_message_sent, parent, false));
            case VIEW_TYPE_RECEIVED:
                return new ReceivedMessageHolder(
                        inflater.inflate(R.layout.item_message_received, parent, false));
            case VIEW_TYPE_SENT_MMS:
                return new SentMmsMessageHolder(
                        inflater.inflate(R.layout.item_message_outgoing_media, parent, false));
            case VIEW_TYPE_RECEIVED_MMS:
                return new ReceivedMmsMessageHolder(
                        inflater.inflate(R.layout.item_message_incoming_media, parent, false));
            case VIEW_TYPE_LOADING:
                return new LoadingViewHolder(
                        inflater.inflate(R.layout.item_loading, parent, false));
            default:
                throw new IllegalArgumentException("Invalid view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LoadingViewHolder) {
            // No binding needed for loading view
            return;
        }

        // Get the message at this position
        final Message message = getItem(position);
        if (message == null) {
            return;
        }

        // Bind the message based on the holder type
        if (holder instanceof SentMessageHolder) {
            bindSentMessage((SentMessageHolder) holder, message, position);
        } else if (holder instanceof ReceivedMessageHolder) {
            bindReceivedMessage((ReceivedMessageHolder) holder, message, position);
        } else if (holder instanceof SentMmsMessageHolder) {
            bindSentMmsMessage((SentMmsMessageHolder) holder, (MmsMessage) message, position);
        } else if (holder instanceof ReceivedMmsMessageHolder) {
            bindReceivedMmsMessage((ReceivedMmsMessageHolder) holder, (MmsMessage) message, position);
        }
    }

    /**
     * Gets the item at the specified position, accounting for the loading footer.
     *
     * @param position The position
     * @return The message at the position, or null if it's the loading footer
     */
    private Message getItem(int position) {
        if (showLoadingFooter && position == messages.size()) {
            return null; // Loading footer
        }
        return messages.get(position);
    }

    /**
     * Binds a sent message to its view holder.
     *
     * @param holder The view holder
     * @param message The message
     * @param position The position
     */
    private void bindSentMessage(SentMessageHolder holder, Message message, int position) {
        // Set message text
        holder.messageText.setText(message.getBody());

        // Set time
        holder.timeText.setText(formatTime(message.getDate()));

        // Handle translation
        if (message.isTranslated() && message.getTranslatedText() != null) {
            holder.translatedText.setText(message.getTranslatedText());
            holder.translatedText.setVisibility(message.isShowTranslation() ? View.VISIBLE : View.GONE);
        } else {
            holder.translatedText.setVisibility(View.GONE);
        }

        // Set click listeners
        if (clickListener != null) {
            holder.itemView.setOnClickListener(v -> clickListener.onMessageClick(message));
            holder.itemView.setOnLongClickListener(v -> {
                clickListener.onMessageLongClick(message);
                return true;
            });
        }
    }

    /**
     * Binds a received message to its view holder.
     *
     * @param holder The view holder
     * @param message The message
     * @param position The position
     */
    private void bindReceivedMessage(ReceivedMessageHolder holder, Message message, int position) {
        // Set message text
        holder.messageText.setText(message.getBody());

        // Set time
        holder.timeText.setText(formatTime(message.getDate()));

        // Handle translation
        if (message.isTranslated() && message.getTranslatedText() != null) {
            holder.translatedText.setText(message.getTranslatedText());
            holder.translatedText.setVisibility(message.isShowTranslation() ? View.VISIBLE : View.GONE);
        } else {
            holder.translatedText.setVisibility(View.GONE);
        }

        // Set click listeners
        if (clickListener != null) {
            holder.itemView.setOnClickListener(v -> clickListener.onMessageClick(message));
            holder.itemView.setOnLongClickListener(v -> {
                clickListener.onMessageLongClick(message);
                return true;
            });
        }
    }

    /**
     * Binds a sent MMS message to its view holder.
     *
     * @param holder The view holder
     * @param message The MMS message
     * @param position The position
     */
    private void bindSentMmsMessage(SentMmsMessageHolder holder, MmsMessage message, int position) {
        // Set message text
        holder.messageText.setText(message.getBody());

        // Set time
        holder.timeText.setText(formatTime(message.getDate()));

        // Handle translation
        if (message.isTranslated() && message.getTranslatedText() != null) {
            holder.translatedText.setText(message.getTranslatedText());
            holder.translatedText.setVisibility(message.isShowTranslation() ? View.VISIBLE : View.GONE);
        } else {
            holder.translatedText.setVisibility(View.GONE);
        }

        // Handle attachments with lazy loading
        if (message.hasAttachments() && !message.getAttachmentObjects().isEmpty()) {
            MmsMessage.Attachment attachment = message.getAttachmentObjects().get(0);
            holder.attachmentImage.setVisibility(View.VISIBLE);
            holder.attachmentProgress.setVisibility(View.VISIBLE);

            // Load image in background
            loadAttachmentImage(attachment, holder.attachmentImage, holder.attachmentProgress);
        } else {
            holder.attachmentImage.setVisibility(View.GONE);
            holder.attachmentProgress.setVisibility(View.GONE);
        }

        // Set click listeners
        if (clickListener != null) {
            holder.itemView.setOnClickListener(v -> clickListener.onMessageClick(message));
            holder.itemView.setOnLongClickListener(v -> {
                clickListener.onMessageLongClick(message);
                return true;
            });
        }
    }

    /**
     * Binds a received MMS message to its view holder.
     *
     * @param holder The view holder
     * @param message The MMS message
     * @param position The position
     */
    private void bindReceivedMmsMessage(ReceivedMmsMessageHolder holder, MmsMessage message, int position) {
        // Set message text
        holder.messageText.setText(message.getBody());

        // Set time
        holder.timeText.setText(formatTime(message.getDate()));

        // Handle translation
        if (message.isTranslated() && message.getTranslatedText() != null) {
            holder.translatedText.setText(message.getTranslatedText());
            holder.translatedText.setVisibility(message.isShowTranslation() ? View.VISIBLE : View.GONE);
        } else {
            holder.translatedText.setVisibility(View.GONE);
        }

        // Handle attachments with lazy loading
        if (message.hasAttachments() && !message.getAttachmentObjects().isEmpty()) {
            MmsMessage.Attachment attachment = message.getAttachmentObjects().get(0);
            holder.attachmentImage.setVisibility(View.VISIBLE);
            holder.attachmentProgress.setVisibility(View.VISIBLE);

            // Load image in background
            loadAttachmentImage(attachment, holder.attachmentImage, holder.attachmentProgress);
        } else {
            holder.attachmentImage.setVisibility(View.GONE);
            holder.attachmentProgress.setVisibility(View.GONE);
        }

        // Set click listeners
        if (clickListener != null) {
            holder.itemView.setOnClickListener(v -> clickListener.onMessageClick(message));
            holder.itemView.setOnLongClickListener(v -> {
                clickListener.onMessageLongClick(message);
                return true;
            });
        }
    }

    /**
     * Loads an attachment image with lazy loading.
     *
     * @param attachment The attachment
     * @param imageView The image view
     * @param progressBar The progress bar
     */
    private void loadAttachmentImage(MmsMessage.Attachment attachment, ImageView imageView, ProgressBar progressBar) {
        if (attachment == null || attachment.getUri() == null) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Generate a unique key for this attachment
        String attachmentKey = attachment.getUri().toString();

        // First check if we have a thumbnail in our cache
        if (thumbnailCache.containsKey(attachmentKey)) {
            imageView.setImageBitmap(thumbnailCache.get(attachmentKey));

            // If we also have the full image, hide the progress bar
            if (fullImageCache.containsKey(attachmentKey)) {
                progressBar.setVisibility(View.GONE);
            }

            return;
        }

        // Load the image in the background
        imageLoadExecutor.execute(() -> {
            try {
                // First try to load a thumbnail
                Bitmap thumbnail = null;
                try {
                    InputStream thumbnailStream = context.getContentResolver().openInputStream(attachment.getUri());
                    if (thumbnailStream != null) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 4; // Load a smaller version first
                        thumbnail = BitmapFactory.decodeStream(thumbnailStream, null, options);
                        thumbnailStream.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error loading thumbnail", e);
                }

                // Update UI with thumbnail
                final Bitmap finalThumbnail = thumbnail;
                handler.post(() -> {
                    if (finalThumbnail != null) {
                        imageView.setImageBitmap(finalThumbnail);
                        thumbnailCache.put(attachmentKey, finalThumbnail);
                    }
                });

                // Then load the full image
                try {
                    InputStream fullStream = context.getContentResolver().openInputStream(attachment.getUri());
                    if (fullStream != null) {
                        Bitmap fullImage = BitmapFactory.decodeStream(fullStream);
                        fullStream.close();

                        // Update UI with full image
                        handler.post(() -> {
                            imageView.setImageBitmap(fullImage);
                            progressBar.setVisibility(View.GONE);
                            fullImageCache.put(attachmentKey, fullImage);
                        });
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error loading full image", e);
                    handler.post(() -> progressBar.setVisibility(View.GONE));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading image", e);
                handler.post(() -> progressBar.setVisibility(View.GONE));
            }
        });
    }

    /**
     * Formats a timestamp into a readable time string.
     *
     * @param timestamp The timestamp
     * @return A formatted time string
     */
    private String formatTime(long timestamp) {
        // Simple time formatting for now
        return new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(new java.util.Date(timestamp));
    }

    @Override
    public int getItemCount() {
        return messages.size() + (showLoadingFooter ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (showLoadingFooter && position == messages.size()) {
            return VIEW_TYPE_LOADING;
        }

        Message message = messages.get(position);
        boolean isSent = message.getType() == Message.TYPE_SENT ||
                message.getType() == Message.TYPE_OUTBOX ||
                message.getType() == Message.TYPE_QUEUED;

        if (message instanceof MmsMessage || message.isMms()) {
            return isSent ? VIEW_TYPE_SENT_MMS : VIEW_TYPE_RECEIVED_MMS;
        } else {
            return isSent ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
        }
    }

    /**
     * Shows or hides the loading footer.
     *
     * @param show Whether to show the loading footer
     */
    public void setShowLoadingFooter(boolean show) {
        if (this.showLoadingFooter != show) {
            this.showLoadingFooter = show;
            if (show) {
                notifyItemInserted(messages.size());
            } else {
                notifyItemRemoved(messages.size());
            }
        }
    }

    /**
     * Updates the messages list using DiffUtil for efficient updates.
     *
     * @param newMessages The new list of messages
     */
    public void updateMessages(List<Message> newMessages) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new MessageDiffCallback(messages, newMessages));

        messages.clear();
        messages.addAll(newMessages);

        diffResult.dispatchUpdatesTo(this);
    }

    /**
     * View holder for sent messages.
     */
    static class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timeText;
        TextView translatedText;

        SentMessageHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message);
            timeText = itemView.findViewById(R.id.text_time);
            translatedText = itemView.findViewById(R.id.translation_text);
        }
    }

    /**
     * View holder for received messages.
     */
    static class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timeText;
        TextView translatedText;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message);
            timeText = itemView.findViewById(R.id.text_time);
            translatedText = itemView.findViewById(R.id.translation_text);
        }
    }

    /**
     * View holder for sent MMS messages.
     */
    static class SentMmsMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timeText;
        TextView translatedText;
        ImageView attachmentImage;
        ProgressBar attachmentProgress;

        SentMmsMessageHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            timeText = itemView.findViewById(R.id.message_date);
            translatedText = itemView.findViewById(R.id.translation_text);
            attachmentImage = itemView.findViewById(R.id.media_image);
            attachmentProgress = itemView.findViewById(R.id.media_icon); // Using media_icon as progress indicator
        }
    }

    /**
     * View holder for received MMS messages.
     */
    static class ReceivedMmsMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timeText;
        TextView translatedText;
        ImageView attachmentImage;
        ProgressBar attachmentProgress;

        ReceivedMmsMessageHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            timeText = itemView.findViewById(R.id.message_date);
            translatedText = itemView.findViewById(R.id.translation_text);
            attachmentImage = itemView.findViewById(R.id.media_image);
            attachmentProgress = itemView.findViewById(R.id.media_icon); // Using media_icon as progress indicator
        }
    }

    /**
     * View holder for the loading footer.
     */
    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(View itemView) {
            super(itemView);
        }
    }

    /**
     * Interface for message click events.
     */
    public interface MessageClickListener {
        void onMessageClick(Message message);
        void onMessageLongClick(Message message);
    }
}

