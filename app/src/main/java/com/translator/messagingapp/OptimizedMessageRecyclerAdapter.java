package com.translator.messagingapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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
    private final MessageClickListener clickListener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Executor imageLoadExecutor = Executors.newFixedThreadPool(3); // Use a thread pool for image loading
    private boolean showLoadingFooter = false;

    /**
     * Creates a new OptimizedMessageRecyclerAdapter.
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

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        
        switch (viewType) {
            case VIEW_TYPE_SENT:
                return new SentMessageViewHolder(
                        inflater.inflate(R.layout.item_message_sent, parent, false));
            case VIEW_TYPE_RECEIVED:
                return new ReceivedMessageViewHolder(
                        inflater.inflate(R.layout.item_message_received, parent, false));
            case VIEW_TYPE_SENT_MMS:
                return new SentMmsViewHolder(
                        inflater.inflate(R.layout.item_message_sent_mms, parent, false));
            case VIEW_TYPE_RECEIVED_MMS:
                return new ReceivedMmsViewHolder(
                        inflater.inflate(R.layout.item_message_received_mms, parent, false));
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
        
        // Get message at position
        Message message = messages.get(position);
        
        // Bind message based on view type
        if (holder instanceof SentMessageViewHolder) {
            bindSentMessage((SentMessageViewHolder) holder, message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            bindReceivedMessage((ReceivedMessageViewHolder) holder, message);
        } else if (holder instanceof SentMmsViewHolder) {
            bindSentMms((SentMmsViewHolder) holder, (MmsMessage) message);
        } else if (holder instanceof ReceivedMmsViewHolder) {
            bindReceivedMms((ReceivedMmsViewHolder) holder, (MmsMessage) message);
        }
    }

    /**
     * Binds a sent message to its view holder.
     *
     * @param holder The view holder
     * @param message The message
     */
    private void bindSentMessage(SentMessageViewHolder holder, Message message) {
        // Bind message text
        holder.messageText.setText(message.getBody());
        
        // Bind message time
        holder.timeText.setText(formatTime(message.getDate()));
        
        // Bind translated text if available
        if (message.isTranslated() && !TextUtils.isEmpty(message.getTranslatedText())) {
            holder.translatedText.setText(message.getTranslatedText());
            holder.translatedText.setVisibility(View.VISIBLE);
        } else {
            holder.translatedText.setVisibility(View.GONE);
        }
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onMessageClick(message);
            }
        });
        
        // Set long click listener
        holder.itemView.setOnLongClickListener(v -> {
            if (clickListener != null) {
                clickListener.onMessageLongClick(message);
                return true;
            }
            return false;
        });
    }

    /**
     * Binds a received message to its view holder.
     *
     * @param holder The view holder
     * @param message The message
     */
    private void bindReceivedMessage(ReceivedMessageViewHolder holder, Message message) {
        // Bind message text
        holder.messageText.setText(message.getBody());
        
        // Bind message time
        holder.timeText.setText(formatTime(message.getDate()));
        
        // Bind translated text if available
        if (message.isTranslated() && !TextUtils.isEmpty(message.getTranslatedText())) {
            holder.translatedText.setText(message.getTranslatedText());
            holder.translatedText.setVisibility(View.VISIBLE);
        } else {
            holder.translatedText.setVisibility(View.GONE);
        }
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onMessageClick(message);
            }
        });
        
        // Set long click listener
        holder.itemView.setOnLongClickListener(v -> {
            if (clickListener != null) {
                clickListener.onMessageLongClick(message);
                return true;
            }
            return false;
        });
    }

    /**
     * Binds a sent MMS message to its view holder with lazy loading of attachments.
     *
     * @param holder The view holder
     * @param message The MMS message
     */
    private void bindSentMms(SentMmsViewHolder holder, MmsMessage message) {
        // Bind message text
        holder.messageText.setText(message.getBody());
        
        // Bind message time
        holder.timeText.setText(formatTime(message.getDate()));
        
        // Bind translated text if available
        if (message.isTranslated() && !TextUtils.isEmpty(message.getTranslatedText())) {
            holder.translatedText.setText(message.getTranslatedText());
            holder.translatedText.setVisibility(View.VISIBLE);
        } else {
            holder.translatedText.setVisibility(View.GONE);
        }
        
        // Lazy load attachment
        loadAttachmentLazily(message, holder.attachmentImage, holder.attachmentProgress);
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onMessageClick(message);
            }
        });
        
        // Set long click listener
        holder.itemView.setOnLongClickListener(v -> {
            if (clickListener != null) {
                clickListener.onMessageLongClick(message);
                return true;
            }
            return false;
        });
    }

    /**
     * Binds a received MMS message to its view holder with lazy loading of attachments.
     *
     * @param holder The view holder
     * @param message The MMS message
     */
    private void bindReceivedMms(ReceivedMmsViewHolder holder, MmsMessage message) {
        // Bind message text
        holder.messageText.setText(message.getBody());
        
        // Bind message time
        holder.timeText.setText(formatTime(message.getDate()));
        
        // Bind translated text if available
        if (message.isTranslated() && !TextUtils.isEmpty(message.getTranslatedText())) {
            holder.translatedText.setText(message.getTranslatedText());
            holder.translatedText.setVisibility(View.VISIBLE);
        } else {
            holder.translatedText.setVisibility(View.GONE);
        }
        
        // Lazy load attachment
        loadAttachmentLazily(message, holder.attachmentImage, holder.attachmentProgress);
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onMessageClick(message);
            }
        });
        
        // Set long click listener
        holder.itemView.setOnLongClickListener(v -> {
            if (clickListener != null) {
                clickListener.onMessageLongClick(message);
                return true;
            }
            return false;
        });
    }

    /**
     * Loads an attachment lazily.
     *
     * @param message The MMS message
     * @param imageView The image view to display the attachment
     * @param progressBar The progress bar to show while loading
     */
    private void loadAttachmentLazily(MmsMessage message, ImageView imageView, ProgressBar progressBar) {
        // Check if message has attachments
        List<MmsMessage.Attachment> attachments = message.getAttachmentObjects();
        if (attachments == null || attachments.isEmpty()) {
            imageView.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            return;
        }
        
        // Show progress bar and hide image view
        imageView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        
        // Load attachment in background
        imageLoadExecutor.execute(() -> {
            try {
                MmsMessage.Attachment attachment = attachments.get(0);
                
                // Load thumbnail first for better performance
                final Bitmap thumbnail = loadAttachmentThumbnail(attachment);
                
                // Update UI on main thread
                handler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    
                    if (thumbnail != null) {
                        imageView.setImageBitmap(thumbnail);
                        imageView.setVisibility(View.VISIBLE);
                        
                        // Load full image after thumbnail is displayed
                        loadFullAttachment(attachment, imageView);
                    } else {
                        imageView.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading attachment", e);
                handler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    imageView.setVisibility(View.GONE);
                });
            }
        });
    }

    /**
     * Loads a thumbnail of an attachment.
     *
     * @param attachment The attachment
     * @return The thumbnail bitmap, or null if loading failed
     */
    private Bitmap loadAttachmentThumbnail(MmsMessage.Attachment attachment) {
        try {
            // Get content resolver
            InputStream is = context.getContentResolver().openInputStream(attachment.getUri());
            if (is == null) {
                return null;
            }
            
            // Load thumbnail with reduced size
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4; // Load at 1/4 the original size
            
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
            is.close();
            
            return bitmap;
        } catch (IOException e) {
            Log.e(TAG, "Error loading attachment thumbnail", e);
            return null;
        }
    }

    /**
     * Loads the full attachment.
     *
     * @param attachment The attachment
     * @param imageView The image view to display the attachment
     */
    private void loadFullAttachment(MmsMessage.Attachment attachment, ImageView imageView) {
        imageLoadExecutor.execute(() -> {
            try {
                // Get content resolver
                InputStream is = context.getContentResolver().openInputStream(attachment.getUri());
                if (is == null) {
                    return;
                }
                
                // Load full image
                final Bitmap fullBitmap = BitmapFactory.decodeStream(is);
                is.close();
                
                // Update UI on main thread
                if (fullBitmap != null) {
                    handler.post(() -> imageView.setImageBitmap(fullBitmap));
                }
            } catch (IOException e) {
                Log.e(TAG, "Error loading full attachment", e);
            }
        });
    }

    /**
     * Formats a timestamp into a readable time string.
     *
     * @param timestamp The timestamp
     * @return The formatted time string
     */
    private String formatTime(long timestamp) {
        // Simple time formatting - in a real app, use DateFormat
        return new java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault())
                .format(new java.util.Date(timestamp));
    }

    @Override
    public int getItemCount() {
        return messages.size() + (showLoadingFooter ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        // Check if this is the loading footer
        if (showLoadingFooter && position == messages.size()) {
            return VIEW_TYPE_LOADING;
        }
        
        // Get message at position
        Message message = messages.get(position);
        
        // Determine view type based on message type
        if (message instanceof MmsMessage) {
            // MMS message
            if (message.getType() == Message.TYPE_SENT || 
                message.getType() == Message.TYPE_OUTBOX ||
                message.getType() == Message.TYPE_QUEUED) {
                return VIEW_TYPE_SENT_MMS;
            } else {
                return VIEW_TYPE_RECEIVED_MMS;
            }
        } else {
            // SMS message
            if (message.getType() == Message.TYPE_SENT || 
                message.getType() == Message.TYPE_OUTBOX ||
                message.getType() == Message.TYPE_QUEUED) {
                return VIEW_TYPE_SENT;
            } else {
                return VIEW_TYPE_RECEIVED;
            }
        }
    }

    /**
     * Updates the messages list using DiffUtil for efficient RecyclerView updates.
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
     * Shows or hides the loading footer.
     *
     * @param show Whether to show the loading footer
     */
    public void showLoadingFooter(boolean show) {
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
     * View holder for sent messages.
     */
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timeText;
        TextView translatedText;
        
        SentMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
            translatedText = itemView.findViewById(R.id.text_message_translated);
        }
    }

    /**
     * View holder for received messages.
     */
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timeText;
        TextView translatedText;
        
        ReceivedMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
            translatedText = itemView.findViewById(R.id.text_message_translated);
        }
    }

    /**
     * View holder for sent MMS messages.
     */
    static class SentMmsViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timeText;
        TextView translatedText;
        ImageView attachmentImage;
        ProgressBar attachmentProgress;
        
        SentMmsViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
            translatedText = itemView.findViewById(R.id.text_message_translated);
            attachmentImage = itemView.findViewById(R.id.image_attachment);
            attachmentProgress = itemView.findViewById(R.id.attachment_progress);
        }
    }

    /**
     * View holder for received MMS messages.
     */
    static class ReceivedMmsViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timeText;
        TextView translatedText;
        ImageView attachmentImage;
        ProgressBar attachmentProgress;
        
        ReceivedMmsViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
            translatedText = itemView.findViewById(R.id.text_message_translated);
            attachmentImage = itemView.findViewById(R.id.image_attachment);
            attachmentProgress = itemView.findViewById(R.id.attachment_progress);
        }
    }

    /**
     * View holder for loading footer.
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