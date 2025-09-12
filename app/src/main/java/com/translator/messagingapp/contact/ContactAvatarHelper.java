package com.translator.messagingapp.contact;

import com.translator.messagingapp.contact.*;

import com.translator.messagingapp.conversation.*;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Helper class for managing contact avatars with the following priority order:
 * 1. Try to load actual contact photo from contacts database
 * 2. If no photo available, generate avatar with contact's initials
 * 3. If that fails, fall back to simple colored background
 * 4. As last resort, use default gray circle
 */
public class ContactAvatarHelper {
    private static final String TAG = "ContactAvatarHelper";
    private static final int DEFAULT_AVATAR_SIZE = 120; // Default size in pixels

    /**
     * Load contact avatar into CircleImageView following the priority order.
     *
     * @param context Context for accessing resources and content resolver
     * @param imageView The CircleImageView to load the avatar into
     * @param conversation The conversation containing contact information
     */
    public static void loadContactAvatar(@NonNull Context context, 
                                       @NonNull CircleImageView imageView, 
                                       @NonNull Conversation conversation) {
        if (context == null || imageView == null || conversation == null) {
            Log.w(TAG, "Invalid parameters provided to loadContactAvatar");
            setDefaultAvatar(imageView);
            return;
        }

        try {
            String contactName = conversation.getContactName();
            String address = conversation.getAddress();
            
            // Step 1: Try to load actual contact photo
            Uri photoUri = getContactPhotoUri(context, address);
            if (photoUri != null) {
                loadContactPhoto(context, imageView, photoUri, contactName, address);
                return;
            }

            // Step 2: Generate initials-based avatar
            generateInitialsAvatar(context, imageView, contactName, address);

        } catch (Exception e) {
            Log.e(TAG, "Error loading contact avatar", e);
            setDefaultAvatar(imageView);
        }
    }

    /**
     * Get the contact photo URI for a phone number.
     *
     * @param context Context for accessing content resolver
     * @param phoneNumber The phone number to look up
     * @return URI of the contact photo, or null if not found
     */
    private static Uri getContactPhotoUri(@NonNull Context context, String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return null;
        }

        try {
            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, 
                                         Uri.encode(phoneNumber));
            
            Cursor cursor = contentResolver.query(
                uri,
                new String[]{ContactsContract.PhoneLookup.PHOTO_URI},
                null,
                null,
                null
            );

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int photoUriIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI);
                        if (photoUriIndex >= 0) {
                            String photoUriString = cursor.getString(photoUriIndex);
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
     * Load contact photo using Glide with fallback to initials avatar.
     *
     * @param context Context for Glide
     * @param imageView Target ImageView
     * @param photoUri URI of the contact photo
     * @param contactName Contact name for fallback
     * @param address Phone number for fallback
     */
    private static void loadContactPhoto(@NonNull Context context, 
                                       @NonNull CircleImageView imageView,
                                       @NonNull Uri photoUri,
                                       String contactName, 
                                       String address) {
        try {
            Glide.with(context)
                .load(photoUri)
                .apply(new RequestOptions()
                    .override(DEFAULT_AVATAR_SIZE, DEFAULT_AVATAR_SIZE)
                    .centerCrop()
                    .placeholder(R.drawable.circle_background)
                    .error(R.drawable.circle_background))
                .into(new com.bumptech.glide.request.target.CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, 
                                              com.bumptech.glide.request.transition.Transition<? super Drawable> transition) {
                        imageView.setImageDrawable(resource);
                        Log.d(TAG, "Successfully loaded contact photo for " + address);
                    }

                    @Override
                    public void onLoadCleared(Drawable placeholder) {
                        // Generate initials avatar as fallback
                        generateInitialsAvatar(context, imageView, contactName, address);
                    }

                    @Override
                    public void onLoadFailed(Drawable errorDrawable) {
                        Log.d(TAG, "Failed to load contact photo for " + address + ", falling back to initials");
                        // Generate initials avatar as fallback
                        generateInitialsAvatar(context, imageView, contactName, address);
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Error loading contact photo", e);
            generateInitialsAvatar(context, imageView, contactName, address);
        }
    }

    /**
     * Generate an initials-based avatar.
     *
     * @param context Context for resources
     * @param imageView Target ImageView
     * @param contactName Contact name
     * @param address Phone number
     */
    private static void generateInitialsAvatar(@NonNull Context context,
                                             @NonNull CircleImageView imageView,
                                             String contactName,
                                             String address) {
        try {
            // Determine display name and initials
            String displayName = getDisplayName(contactName, address);
            String initials = getInitials(displayName);
            int backgroundColor = ContactUtils.getContactColor(displayName);
            
            // Create bitmap for the avatar
            Bitmap avatarBitmap = createInitialsAvatar(initials, backgroundColor, DEFAULT_AVATAR_SIZE);
            
            if (avatarBitmap != null) {
                imageView.setImageBitmap(avatarBitmap);
                Log.d(TAG, "Generated initials avatar for " + displayName + " with initials: " + initials);
            } else {
                setColoredBackground(imageView, backgroundColor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generating initials avatar", e);
            setDefaultAvatar(imageView);
        }
    }

    /**
     * Create a bitmap with initials text.
     *
     * @param initials The initials to display
     * @param backgroundColor Background color
     * @param size Size of the bitmap
     * @return Bitmap containing the initials avatar
     */
    private static Bitmap createInitialsAvatar(String initials, int backgroundColor, int size) {
        try {
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            
            // Draw background
            Paint backgroundPaint = new Paint();
            backgroundPaint.setColor(backgroundColor);
            backgroundPaint.setAntiAlias(true);
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, backgroundPaint);
            
            // Draw initials text
            Paint textPaint = new Paint();
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(size * 0.4f);
            textPaint.setAntiAlias(true);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            textPaint.setTextAlign(Paint.Align.CENTER);
            
            // Calculate text position
            Rect textBounds = new Rect();
            textPaint.getTextBounds(initials, 0, initials.length(), textBounds);
            float textX = size / 2f;
            float textY = size / 2f + textBounds.height() / 2f;
            
            canvas.drawText(initials, textX, textY, textPaint);
            
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error creating initials avatar bitmap", e);
            return null;
        }
    }

    /**
     * Get display name with proper fallback logic.
     *
     * @param contactName Contact name
     * @param address Phone number
     * @return Display name to use
     */
    private static String getDisplayName(String contactName, String address) {
        if (!TextUtils.isEmpty(contactName) && !contactName.equals(address)) {
            return contactName;
        }
        if (!TextUtils.isEmpty(address)) {
            return address;
        }
        return "?";
    }

    /**
     * Get initials from a display name.
     *
     * @param displayName The display name
     * @return Initials (1-2 characters)
     */
    private static String getInitials(String displayName) {
        if (TextUtils.isEmpty(displayName)) {
            return "?";
        }

        String trimmed = displayName.trim();
        if (trimmed.isEmpty()) {
            return "?";
        }

        // For phone numbers, use first digit
        if (trimmed.matches("^[+]?[0-9()\\s-]+$")) {
            for (char c : trimmed.toCharArray()) {
                if (Character.isDigit(c)) {
                    return String.valueOf(c);
                }
            }
            return "#";
        }

        // For names, get first letters of first two words
        String[] words = trimmed.split("\\s+");
        StringBuilder initials = new StringBuilder();
        
        for (int i = 0; i < Math.min(2, words.length); i++) {
            String word = words[i].trim();
            if (!word.isEmpty()) {
                char firstChar = Character.toUpperCase(word.charAt(0));
                if (Character.isLetter(firstChar)) {
                    initials.append(firstChar);
                }
            }
        }

        if (initials.length() > 0) {
            return initials.toString();
        }

        // Fallback to first character
        char firstChar = Character.toUpperCase(trimmed.charAt(0));
        return Character.isLetter(firstChar) ? String.valueOf(firstChar) : "?";
    }

    /**
     * Set a colored background as fallback.
     *
     * @param imageView Target ImageView
     * @param backgroundColor Background color
     */
    private static void setColoredBackground(@NonNull CircleImageView imageView, int backgroundColor) {
        try {
            Bitmap bitmap = Bitmap.createBitmap(DEFAULT_AVATAR_SIZE, DEFAULT_AVATAR_SIZE, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            
            Paint paint = new Paint();
            paint.setColor(backgroundColor);
            paint.setAntiAlias(true);
            canvas.drawCircle(DEFAULT_AVATAR_SIZE / 2f, DEFAULT_AVATAR_SIZE / 2f, DEFAULT_AVATAR_SIZE / 2f, paint);
            
            imageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            Log.e(TAG, "Error setting colored background", e);
            setDefaultAvatar(imageView);
        }
    }

    /**
     * Set default gray circle as last resort.
     *
     * @param imageView Target ImageView
     */
    private static void setDefaultAvatar(@NonNull CircleImageView imageView) {
        try {
            imageView.setImageResource(R.drawable.circle_background);
        } catch (Exception e) {
            Log.e(TAG, "Error setting default avatar", e);
            // Last resort: hide the avatar
            imageView.setVisibility(android.view.View.GONE);
        }
    }
}