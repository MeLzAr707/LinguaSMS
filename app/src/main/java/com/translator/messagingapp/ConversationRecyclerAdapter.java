package com.translator.messagingapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import de.hdodenhof.circleimageview.CircleImageView;

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
                // Set a default avatar for error cases
                if (holder.contactAvatarImageView != null) {
                    holder.contactAvatarImageView.setImageDrawable(createInitialsDrawable("?"));
                }
                return;
            }
            
            Conversation conversation = conversations.get(position);
            if (conversation == null) {
                Log.e(TAG, "Conversation at position " + position + " is null");
                holder.contactNameTextView.setText(R.string.error_loading_conversation);
                // Set a default avatar for error cases
                if (holder.contactAvatarImageView != null) {
                    holder.contactAvatarImageView.setImageDrawable(createInitialsDrawable("?"));
                }
                return;
            }
            
            // Set contact name or phone number
            String displayName = conversation.getContactName();
            if (TextUtils.isEmpty(displayName)) {
                displayName = conversation.getAddress();
                if (TextUtils.isEmpty(displayName)) {
                    displayName = context.getString(R.string.unknown_contact);
                    Log.w(TAG, "No contact name or address for conversation at position " + position);
                }
            }
            
            holder.contactNameTextView.setText(displayName);
            
            // Set contact avatar
            if (holder.contactAvatarImageView != null) {
                loadContactAvatar(holder.contactAvatarImageView, conversation, displayName);
            } else {
                Log.w(TAG, "Contact avatar ImageView is null at position " + position);
            }
            
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
            // Set a default error avatar
            if (holder.contactAvatarImageView != null) {
                holder.contactAvatarImageView.setImageDrawable(createInitialsDrawable("!"));
            }
        }
    }

    @Override
    public int getItemCount() {
        return conversations != null ? conversations.size() : 0;
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
     * Loads the contact avatar for a conversation.
     *
     * @param avatarImageView The CircleImageView to load the avatar into
     * @param conversation    The conversation
     * @param displayName     The display name for the contact
     */
    private void loadContactAvatar(CircleImageView avatarImageView, Conversation conversation, String displayName) {
        if (avatarImageView == null) {
            Log.w(TAG, "Avatar ImageView is null, skipping avatar load");
            return;
        }
        
        try {
            String phoneNumber = conversation.getAddress();
            
            // Try to load contact photo from contacts
            Uri contactPhotoUri = getContactPhotoUri(phoneNumber);
            
            if (contactPhotoUri != null) {
                // Load actual contact photo using Glide
                RequestOptions options = new RequestOptions()
                        .placeholder(createInitialsDrawable(displayName))
                        .error(createInitialsDrawable(displayName))
                        .diskCacheStrategy(DiskCacheStrategy.ALL);
                
                Glide.with(context)
                        .load(contactPhotoUri)
                        .apply(options)
                        .into(avatarImageView);
            } else {
                // No contact photo found, create initials drawable
                avatarImageView.setImageDrawable(createInitialsDrawable(displayName));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading contact avatar", e);
            // Fallback to initials drawable
            avatarImageView.setImageDrawable(createInitialsDrawable(displayName));
        }
    }
    
    /**
     * Gets the contact photo URI for a phone number.
     *
     * @param phoneNumber The phone number
     * @return The contact photo URI, or null if not found
     */
    private Uri getContactPhotoUri(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return null;
        }
        
        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            android.database.Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[]{ContactsContract.PhoneLookup.PHOTO_URI},
                    null,
                    null,
                    null);
                    
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int photoIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI);
                        if (photoIndex >= 0) {
                            String photoUriString = cursor.getString(photoIndex);
                            if (!TextUtils.isEmpty(photoUriString)) {
                                return Uri.parse(photoUriString);
                            }
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact photo URI for " + phoneNumber, e);
        }
        
        return null;
    }
    
    /**
     * Creates a drawable with the contact's initials and background color.
     *
     * @param displayName The display name or phone number
     * @return A drawable with initials
     */
    private Drawable createInitialsDrawable(String displayName) {
        String initial = ContactUtils.getContactInitial(displayName);
        int backgroundColor = ContactUtils.getContactColor(displayName);
        
        return new InitialsDrawable(initial, backgroundColor);
    }
    
    /**
     * Custom drawable for displaying contact initials with a colored background.
     */
    private static class InitialsDrawable extends Drawable {
        private final String initials;
        private final Paint backgroundPaint;
        private final Paint textPaint;
        private final int textColor = Color.WHITE;
        
        public InitialsDrawable(String initials, int backgroundColor) {
            this.initials = initials != null ? initials : "#";
            
            backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            backgroundPaint.setColor(backgroundColor);
            backgroundPaint.setStyle(Paint.Style.FILL);
            
            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(textColor);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }
        
        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect bounds = getBounds();
            
            // Draw circular background
            float centerX = bounds.exactCenterX();
            float centerY = bounds.exactCenterY();
            float radius = Math.min(bounds.width(), bounds.height()) / 2f;
            canvas.drawCircle(centerX, centerY, radius, backgroundPaint);
            
            // Draw text
            textPaint.setTextSize(radius * 0.8f); // Text size relative to circle size
            
            // Calculate text position to center it vertically
            Rect textBounds = new Rect();
            textPaint.getTextBounds(initials, 0, initials.length(), textBounds);
            float textY = centerY + textBounds.height() / 2f;
            
            canvas.drawText(initials, centerX, textY, textPaint);
        }
        
        @Override
        public void setAlpha(int alpha) {
            backgroundPaint.setAlpha(alpha);
            textPaint.setAlpha(alpha);
        }
        
        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            backgroundPaint.setColorFilter(colorFilter);
            textPaint.setColorFilter(colorFilter);
        }
        
        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    /**
     * ViewHolder for conversation items.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView contactAvatarImageView;
        TextView contactNameTextView;
        TextView snippetTextView;
        TextView dateTextView;
        View unreadIndicator;
        TextView unreadCountTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            contactAvatarImageView = itemView.findViewById(R.id.contact_avatar);
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