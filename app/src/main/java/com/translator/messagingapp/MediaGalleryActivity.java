package com.translator.messagingapp;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Full-screen media gallery activity that displays media with enhanced viewing capabilities
 */
public class MediaGalleryActivity extends BaseActivity implements MediaGalleryView.OnMediaGalleryListener {
    private static final String TAG = "MediaGalleryActivity";
    private static final String EXTRA_MEDIA_URI = "extra_media_uri";
    
    private MediaGalleryView mediaGalleryView;
    private Uri mediaUri;
    
    /**
     * Create an intent to launch MediaGalleryActivity
     */
    public static Intent createIntent(Context context, Uri mediaUri) {
        Intent intent = new Intent(context, MediaGalleryActivity.class);
        intent.putExtra(EXTRA_MEDIA_URI, mediaUri);
        return intent;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable immersive full-screen mode
        enableImmersiveMode();
        
        setContentView(R.layout.activity_media_gallery);
        
        // Get media URI from intent
        Intent intent = getIntent();
        if (intent != null) {
            mediaUri = intent.getParcelableExtra(EXTRA_MEDIA_URI);
        }
        
        if (mediaUri == null) {
            Log.e(TAG, "No media URI provided");
            finish();
            return;
        }
        
        // Initialize views
        initViews();
        
        // Load media
        mediaGalleryView.loadMedia(mediaUri);
    }
    
    private void enableImmersiveMode() {
        // Hide status bar and navigation bar for immersive experience
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }
    
    private void initViews() {
        mediaGalleryView = findViewById(R.id.media_gallery_view);
        mediaGalleryView.setOnMediaGalleryListener(this);
    }
    
    @Override
    public void onCloseRequested() {
        finish();
    }
    
    @Override
    public void onShareRequested(Uri mediaUri) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(getContentResolver().getType(mediaUri));
            shareIntent.putExtra(Intent.EXTRA_STREAM, mediaUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            Intent chooser = Intent.createChooser(shareIntent, getString(R.string.share));
            startActivity(chooser);
        } catch (Exception e) {
            Log.e(TAG, "Error sharing media", e);
            Toast.makeText(this, "Failed to share media", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onSaveRequested(Uri mediaUri) {
        // Save image to device storage
        saveImageToDevice(mediaUri);
    }
    
    @Override
    public void onMediaLoadError(String error) {
        Log.e(TAG, "Media load error: " + error);
        Toast.makeText(this, "Failed to load media", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private void saveImageToDevice(Uri mediaUri) {
        // Load the image using Glide and save it
        Glide.with(this)
            .asBitmap()
            .load(mediaUri)
            .into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    saveBitmapToDevice(resource);
                }
                
                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {
                    // Not needed for this use case
                }
                
                @Override
                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    Toast.makeText(MediaGalleryActivity.this, "Failed to save image", Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void saveBitmapToDevice(Bitmap bitmap) {
        try {
            String filename = "LinguaSMS_" + System.currentTimeMillis() + ".jpg";
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/LinguaSMS");
                
                ContentResolver resolver = getContentResolver();
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                
                if (imageUri != null) {
                    try (OutputStream outputStream = resolver.openOutputStream(imageUri)) {
                        if (outputStream != null) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                            Toast.makeText(this, "Image saved to Pictures/LinguaSMS", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            } else {
                // Use legacy storage for older versions
                File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File appDir = new File(picturesDir, "LinguaSMS");
                
                if (!appDir.exists()) {
                    appDir.mkdirs();
                }
                
                File imageFile = new File(appDir, filename);
                
                try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                    
                    // Notify media scanner about the new file
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaScanIntent.setData(Uri.fromFile(imageFile));
                    sendBroadcast(mediaScanIntent);
                    
                    Toast.makeText(this, "Image saved to Pictures/LinguaSMS", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error saving image", e);
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Re-enable immersive mode when focus is regained
            enableImmersiveMode();
        }
    }
}