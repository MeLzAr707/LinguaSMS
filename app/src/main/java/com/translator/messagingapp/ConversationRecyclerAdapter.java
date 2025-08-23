package com.translator.messagingapp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * RecyclerView adapter for displaying conversations.
 */
public class ConversationRecyclerAdapter extends RecyclerView.Adapter<ConversationRecyclerAdapter.ViewHolder> {
    private static final String TAG = "ConversationRecyclerAdapter";

    private final Context context;
    private final List<Conversation> conversations;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat timeFormat;
    private final SimpleDateFormat fullDateFormat;
    private ConversationClickListener clickListener;

    /**
     * Interface for conversation click events.
     */
    public interface ConversationClickListener {
        void onConversationClick(Conversation conversation, int position);
        void onConversationLongClick(Conversation conversation, int position);
    }

    public ConversationRecyclerAdapter(Context context, List<Conversation> conversations) {
        this.context = context;
        this.conversations = conversations;
        this.dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
        this.timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        this.fullDateFormat = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());
    }

    public void setConversationClickListener(ConversationClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Conversation conversation = conversations.get(position);

        // Set the contact name with improved null handling
        String displayName = getDisplayNameForConversation(conversation);
        holder.contactName.setText(displayName);

        // Set the last message/snippet with proper null checks
        String snippet = conversation.getSnippet();
        if (!TextUtils.isEmpty(snippet)) {
            holder.lastMessage.setText(snippet);
            holder.lastMessage.setVisibility(View.VISIBLE);
        } else {
            // Fallback to last message if snippet is not available
            String lastMessage = conversation.getLastMessage();
            if (!TextUtils.isEmpty(lastMessage)) {
                holder.lastMessage.setText(lastMessage);
                holder.lastMessage.setVisibility(View.VISIBLE);
            } else {
                // If both are empty, show a default message
                holder.lastMessage.setText("No messages");
                holder.lastMessage.setVisibility(View.VISIBLE);
            }
        }


        // Set the timestamp
        Date date = conversation.getDate();
        if (date != null && date.getTime() > 0) {
            holder.timestamp.setText(formatTimestamp(date));
        } else {
            // Use current time as fallback if date is null or invalid
            holder.timestamp.setText(formatTimestamp(new Date()));
        }

        // Set unread count
        int unreadCount = conversation.getUnreadCount();
        if (unreadCount > 0) {
            holder.unreadCount.setText(String.valueOf(unreadCount));
            holder.unreadCount.setVisibility(View.VISIBLE);
        } else {
            holder.unreadCount.setVisibility(View.GONE);
        }

        // Safely set the contact image
        setContactImageSafely(holder, conversation.getAddress(), displayName);

        // Set click listeners
        final int finalPosition = position;
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onConversationClick(conversation, finalPosition);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (clickListener != null) {
                clickListener.onConversationLongClick(conversation, finalPosition);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return conversations != null ? conversations.size() : 0;
    }

    /**
     * Gets the appropriate display name for a conversation with improved logic.
     */
    private String getDisplayNameForConversation(Conversation conversation) {
        if (conversation == null) {
            return "Unknown Contact";
        }
        
        String contactName = conversation.getContactName();
        String address = conversation.getAddress();
        
        // Clean up contact name - handle string "null", empty, or actual null
        if (TextUtils.isEmpty(contactName) || "null".equals(contactName)) {
            contactName = null;
        }
        
        // If we have a valid contact name, use it
        if (contactName != null) {
            // Check if it's a group message indicator
            if (contactName.contains(",") || contactName.contains("+") && contactName.contains("others")) {
                return contactName; // Already formatted group name
            }
            
            // Check if contact name is same as address (not useful)
            if (!contactName.equals(address)) {
                return contactName;
            }
        }
        
        // If no contact name, try to look it up from device contacts as fallback
        if (contactName == null && !TextUtils.isEmpty(address)) {
            // Handle group conversations (comma-separated addresses)
            if (address.contains(",")) {
                return getGroupDisplayName(address);
            }
            
            // Single address - try contact lookup
            String lookedUpName = ContactUtils.getContactName(context, address);
            if (!TextUtils.isEmpty(lookedUpName)) {
                return lookedUpName;
            }
        }
        
        // Fall back to formatted phone number
        if (!TextUtils.isEmpty(address)) {
            // Check if this looks like a group message
            if (address.contains(",") || address.contains("+") && address.contains("others")) {
                return address; // Already formatted group address
            }
            
            return formatPhoneNumber(address);
        }
        
        // Last resort
        return "Unknown Contact";
    }
    
    /**
     * Get display name for group conversations with multiple addresses.
     */
    private String getGroupDisplayName(String addresses) {
        if (TextUtils.isEmpty(addresses)) {
            return "Unknown Contact";
        }
        
        String[] addressArray = addresses.split(",");
        if (addressArray.length <= 1) {
            // Not actually a group, treat as single address
            String singleAddress = addresses.trim();
            String contactName = ContactUtils.getContactName(context, singleAddress);
            return !TextUtils.isEmpty(contactName) ? contactName : formatPhoneNumber(singleAddress);
        }
        
        // For group conversations, try to get contact names for each participant
        StringBuilder groupName = new StringBuilder();
        int nameCount = 0;
        int maxNamesToShow = 3; // Show max 3 names, then "and X others"
        
        for (int i = 0; i < addressArray.length && nameCount < maxNamesToShow; i++) {
            String address = addressArray[i].trim();
            if (TextUtils.isEmpty(address)) {
                continue;
            }
            
            String contactName = ContactUtils.getContactName(context, address);
            String displayName = !TextUtils.isEmpty(contactName) ? contactName : formatPhoneNumber(address);
            
            if (nameCount > 0) {
                groupName.append(", ");
            }
            groupName.append(displayName);
            nameCount++;
        }
        
        // If there are more participants than we showed
        if (addressArray.length > maxNamesToShow) {
            int remaining = addressArray.length - maxNamesToShow;
            groupName.append(" + ").append(remaining).append(" other");
            if (remaining > 1) {
                groupName.append("s");
            }
        }
        
        return groupName.length() > 0 ? groupName.toString() : "Group Chat";
    }

    /**
     * Format a phone number for display (removes country code for cleaner display)
     * Package-private for testing
     */
    String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "Unknown";
        }

        // Remove any spaces, dashes, parentheses, and plus signs for processing
        String cleanNumber = phoneNumber.replaceAll("[\\s\\-\\(\\)\\+]", "");
        
        // If the number starts with country code (like +1 for US), remove it for display
        if (cleanNumber.startsWith("1") && cleanNumber.length() == 11) {
            // Remove US country code (1) if present
            cleanNumber = cleanNumber.substring(1);
        }
        
        // Format as (XXX) XXX-XXXX for 10-digit numbers
        if (cleanNumber.length() == 10) {
            return String.format("(%s) %s-%s",
                    cleanNumber.substring(0, 3),
                    cleanNumber.substring(3, 6),
                    cleanNumber.substring(6));
        } else if (cleanNumber.length() == 7) {
            // Format 7-digit numbers as XXX-XXXX
            return String.format("%s-%s",
                    cleanNumber.substring(0, 3),
                    cleanNumber.substring(3));
        } else if (cleanNumber.length() > 10) {
            // For other international numbers, show without country code if possible
            // Try to extract the last 10 digits for display
            if (cleanNumber.length() >= 10) {
                String lastTenDigits = cleanNumber.substring(cleanNumber.length() - 10);
                return String.format("(%s) %s-%s",
                        lastTenDigits.substring(0, 3),
                        lastTenDigits.substring(3, 6),
                        lastTenDigits.substring(6));
            }
        }

        // If we can't format it nicely, return as-is
        return phoneNumber;
    }

    /**
     * Format a timestamp for display
     */
    private String formatTimestamp(Date date) {
        if (date == null) {
            return "";
        }

        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTime(date);

        // Today
        if (now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)) {
            return timeFormat.format(date);
        }

        // This week
        if (now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
                now.get(Calendar.WEEK_OF_YEAR) == then.get(Calendar.WEEK_OF_YEAR)) {
            return new SimpleDateFormat("EEE", Locale.getDefault()).format(date);
        }

        // This year
        if (now.get(Calendar.YEAR) == then.get(Calendar.YEAR)) {
            return dateFormat.format(date);
        }

        // Older
        return new SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(date);
    }

    /**
     * Safely set the contact image with fallback to initials
     */
    private void setContactImageSafely(ViewHolder holder, String address, String displayName) {
        // Create a default colored background based on the address or name
        int backgroundColor = generateColor(address != null ? address : displayName);
        ColorDrawable defaultBackground = new ColorDrawable(backgroundColor);

        // First, set the default background to avoid null drawables
        holder.contactImage.setImageDrawable(defaultBackground);

        // Set up initials as a fallback
        String initials = getInitials(displayName);
        holder.contactInitials.setText(initials);
        holder.contactInitials.setVisibility(View.VISIBLE);

        // Try to load contact photo if available
        if (address != null && !address.isEmpty()) {
            try {
                Uri photoUri = getContactPhotoUri(address);
                if (photoUri != null) {
                    // Use Glide to load the image
                    Glide.with(context)
                            .load(photoUri)
                            .apply(new RequestOptions()
                                    .placeholder(defaultBackground)
                                    .error(defaultBackground)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL))
                            .listener(new RequestListener<>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                            Target<Drawable> target, boolean isFirstResource) {
                                    // Show initials on failure
                                    holder.contactInitials.setVisibility(View.VISIBLE);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model,
                                                               Target<Drawable> target, DataSource dataSource,
                                                               boolean isFirstResource) {
                                    // Hide initials when image is loaded
                                    holder.contactInitials.setVisibility(View.GONE);
                                    return false;
                                }
                            })
                            .into(holder.contactImage);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading contact photo", e);
                // Fallback to initials
                holder.contactInitials.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Get the contact photo URI for a phone number
     */
    private Uri getContactPhotoUri(String phoneNumber) {
        // Validate phone number to prevent IllegalArgumentException with empty URIs
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }
        
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));

        try (Cursor cursor = contentResolver.query(uri,
                new String[]{ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.PHOTO_URI},
                null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int photoUriIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI);
                if (photoUriIndex >= 0) {
                    String photoUriString = cursor.getString(photoUriIndex);
                    if (photoUriString != null) {
                        return Uri.parse(photoUriString);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact photo URI", e);
        }

        return null;
    }

    /**
     * Generate a consistent color based on a string
     */
    private int generateColor(String key) {
        if (key == null || key.isEmpty()) {
            return Color.LTGRAY;
        }

        // Use the hash code of the string to generate a consistent color
        int hash = key.hashCode();
        float hue = Math.abs(hash % 360);
        return Color.HSVToColor(new float[]{hue, 0.6f, 0.8f});
    }

    /**
     * Get the initials from a name
     */
    private String getInitials(String name) {
        if (name == null || name.isEmpty() || name.equals("null")) {
            return "#";
        }

        StringBuilder initials = new StringBuilder();
        String[] parts = name.split("\\s+");

        for (int i = 0; i < Math.min(2, parts.length); i++) {
            if (!parts[i].isEmpty()) {
                initials.append(parts[i].charAt(0));
            }
        }

        // If we couldn't extract initials, use the first character
        if (initials.length() == 0 && !name.isEmpty()) {
            initials.append(name.charAt(0));
        }

        return initials.toString().toUpperCase();
    }

    /**
     * ViewHolder pattern class
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView contactName;
        TextView lastMessage;
        TextView timestamp;
        TextView unreadCount;
        CircleImageView contactImage;
        TextView contactInitials;

        ViewHolder(View itemView) {
            super(itemView);
            contactName = itemView.findViewById(R.id.contact_name);
            lastMessage = itemView.findViewById(R.id.last_message);
            timestamp = itemView.findViewById(R.id.timestamp);
            unreadCount = itemView.findViewById(R.id.unread_count);
            contactImage = itemView.findViewById(R.id.contact_image);
            contactInitials = itemView.findViewById(R.id.contact_initials);
        }
    }

    /**
     * Updates the conversations list using DiffUtil for efficient RecyclerView updates.
     * This method calculates the difference between the old and new conversation lists
     * and only updates the items that have changed, improving performance.
     *
     * @param newConversations The new list of conversations to display
     */
    public void updateConversations(List<Conversation> newConversations) {
        if (newConversations == null) {
            return;
        }
        
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new ConversationDiffCallback(conversations, newConversations));
        
        conversations.clear();
        conversations.addAll(newConversations);
        
        diffResult.dispatchUpdatesTo(this);
    }
}
