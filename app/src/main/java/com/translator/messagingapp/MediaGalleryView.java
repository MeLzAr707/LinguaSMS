package com.translator.messagingapp;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

/**
 * Enhanced media gallery view component with pinch-to-zoom functionality
 * and efficient image loading and caching capabilities.
 */
public class MediaGalleryView extends FrameLayout {
    private static final String TAG = "MediaGalleryView";
    
    // Zoom constraints
    private static final float MIN_SCALE = 1.0f;
    private static final float MAX_SCALE = 5.0f;
    private static final float DEFAULT_SCALE = 1.0f;
    
    // UI Components
    private ImageView mediaImageView;
    private ProgressBar loadingIndicator;
    private LinearLayout toolbarOverlay;
    private LinearLayout errorLayout;
    private TextView errorMessage;
    private ImageButton closeButton;
    private ImageButton shareButton;
    private ImageButton saveButton;
    
    // Gesture handling
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    
    // Image transformation
    private Matrix imageMatrix;
    private float currentScale = DEFAULT_SCALE;
    private float lastFocusX, lastFocusY;
    private int imageWidth, imageHeight;
    private int viewWidth, viewHeight;
    
    // Listeners
    private OnMediaGalleryListener listener;
    
    // Current media
    private Uri currentMediaUri;
    
    /**
     * Interface for media gallery events
     */
    public interface OnMediaGalleryListener {
        void onCloseRequested();
        void onShareRequested(Uri mediaUri);
        void onSaveRequested(Uri mediaUri);
        void onMediaLoadError(String error);
    }
    
    public MediaGalleryView(@NonNull Context context) {
        super(context);
        init();
    }
    
    public MediaGalleryView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public MediaGalleryView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // Inflate layout
        inflate(getContext(), R.layout.media_gallery_view, this);
        
        // Find views
        findViews();
        
        // Initialize transformation matrix
        imageMatrix = new Matrix();
        
        // Setup gesture detectors
        setupGestureDetectors();
        
        // Setup click listeners
        setupClickListeners();
        
        // Configure ImageView
        mediaImageView.setScaleType(ImageView.ScaleType.MATRIX);
        mediaImageView.setImageMatrix(imageMatrix);
    }
    
    private void findViews() {
        mediaImageView = findViewById(R.id.media_image_view);
        loadingIndicator = findViewById(R.id.loading_indicator);
        toolbarOverlay = findViewById(R.id.toolbar_overlay);
        errorLayout = findViewById(R.id.error_layout);
        errorMessage = findViewById(R.id.error_message);
        closeButton = findViewById(R.id.close_button);
        shareButton = findViewById(R.id.share_button);
        saveButton = findViewById(R.id.save_button);
    }
    
    private void setupGestureDetectors() {
        // Scale gesture detector for pinch-to-zoom
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                float newScale = currentScale * scaleFactor;
                
                // Constrain scale
                newScale = Math.max(MIN_SCALE, Math.min(newScale, MAX_SCALE));
                
                if (newScale != currentScale) {
                    float focusX = detector.getFocusX();
                    float focusY = detector.getFocusY();
                    
                    // Scale around focus point
                    imageMatrix.postScale(newScale / currentScale, newScale / currentScale, focusX, focusY);
                    
                    // Constrain translation to keep image in bounds
                    constrainMatrix();
                    
                    currentScale = newScale;
                    mediaImageView.setImageMatrix(imageMatrix);
                }
                
                return true;
            }
            
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                lastFocusX = detector.getFocusX();
                lastFocusY = detector.getFocusY();
                return true;
            }
        });
        
        // Gesture detector for pan and double-tap
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (currentScale > MIN_SCALE) {
                    imageMatrix.postTranslate(-distanceX, -distanceY);
                    constrainMatrix();
                    mediaImageView.setImageMatrix(imageMatrix);
                    return true;
                }
                return false;
            }
            
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (currentScale == MIN_SCALE) {
                    // Zoom in to 2x at tap location
                    float targetScale = Math.min(2.0f, MAX_SCALE);
                    zoomToPoint(e.getX(), e.getY(), targetScale);
                } else {
                    // Reset to fit screen
                    resetZoom();
                }
                return true;
            }
            
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // Toggle toolbar visibility
                toggleToolbarVisibility();
                return true;
            }
        });
    }
    
    private void setupClickListeners() {
        closeButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCloseRequested();
            }
        });
        
        shareButton.setOnClickListener(v -> {
            if (listener != null && currentMediaUri != null) {
                listener.onShareRequested(currentMediaUri);
            }
        });
        
        saveButton.setOnClickListener(v -> {
            if (listener != null && currentMediaUri != null) {
                listener.onSaveRequested(currentMediaUri);
            }
        });
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean scaleHandled = scaleGestureDetector.onTouchEvent(event);
        boolean gestureHandled = gestureDetector.onTouchEvent(event);
        return scaleHandled || gestureHandled || super.onTouchEvent(event);
    }
    
    /**
     * Load media from URI with efficient caching
     */
    public void loadMedia(Uri mediaUri) {
        if (mediaUri == null) {
            showError("Invalid media URI");
            return;
        }
        
        currentMediaUri = mediaUri;
        showLoading();
        
        RequestOptions options = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(null)
            .error(null);
        
        Glide.with(getContext())
            .load(mediaUri)
            .apply(options)
            .into(new CustomTarget<Drawable>() {
                @Override
                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                    hideLoading();
                    hideError();
                    
                    // Get intrinsic dimensions
                    imageWidth = resource.getIntrinsicWidth();
                    imageHeight = resource.getIntrinsicHeight();
                    
                    // Set the image
                    mediaImageView.setImageDrawable(resource);
                    
                    // Reset zoom and center image
                    post(() -> {
                        viewWidth = mediaImageView.getWidth();
                        viewHeight = mediaImageView.getHeight();
                        resetZoom();
                    });
                }
                
                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {
                    // Clean up
                }
                
                @Override
                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    hideLoading();
                    showError("Failed to load media");
                    
                    if (listener != null) {
                        listener.onMediaLoadError("Failed to load media from URI: " + mediaUri);
                    }
                }
            });
    }
    
    private void showLoading() {
        loadingIndicator.setVisibility(VISIBLE);
        errorLayout.setVisibility(GONE);
        mediaImageView.setVisibility(GONE);
    }
    
    private void hideLoading() {
        loadingIndicator.setVisibility(GONE);
        mediaImageView.setVisibility(VISIBLE);
    }
    
    private void showError(String error) {
        errorLayout.setVisibility(VISIBLE);
        errorMessage.setText(error);
        mediaImageView.setVisibility(GONE);
        loadingIndicator.setVisibility(GONE);
    }
    
    private void hideError() {
        errorLayout.setVisibility(GONE);
    }
    
    private void resetZoom() {
        if (imageWidth <= 0 || imageHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) {
            return;
        }
        
        imageMatrix.reset();
        currentScale = DEFAULT_SCALE;
        
        // Calculate scale to fit image in view
        float scaleX = (float) viewWidth / imageWidth;
        float scaleY = (float) viewHeight / imageHeight;
        float scale = Math.min(scaleX, scaleY);
        
        // Center the image
        float centerX = viewWidth / 2f;
        float centerY = viewHeight / 2f;
        float imageX = (viewWidth - imageWidth * scale) / 2f;
        float imageY = (viewHeight - imageHeight * scale) / 2f;
        
        imageMatrix.setScale(scale, scale);
        imageMatrix.postTranslate(imageX, imageY);
        
        currentScale = scale;
        mediaImageView.setImageMatrix(imageMatrix);
    }
    
    private void zoomToPoint(float x, float y, float targetScale) {
        float scaleFactor = targetScale / currentScale;
        imageMatrix.postScale(scaleFactor, scaleFactor, x, y);
        currentScale = targetScale;
        constrainMatrix();
        mediaImageView.setImageMatrix(imageMatrix);
    }
    
    private void constrainMatrix() {
        if (imageWidth <= 0 || imageHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) {
            return;
        }
        
        float[] values = new float[9];
        imageMatrix.getValues(values);
        
        float transX = values[Matrix.MTRANS_X];
        float transY = values[Matrix.MTRANS_Y];
        float scaleX = values[Matrix.MSCALE_X];
        float scaleY = values[Matrix.MSCALE_Y];
        
        float scaledImageWidth = imageWidth * scaleX;
        float scaledImageHeight = imageHeight * scaleY;
        
        // Constrain horizontal translation
        if (scaledImageWidth <= viewWidth) {
            // Center if image is smaller than view
            transX = (viewWidth - scaledImageWidth) / 2f;
        } else {
            // Constrain to bounds if image is larger than view
            if (transX > 0) {
                transX = 0;
            } else if (transX < viewWidth - scaledImageWidth) {
                transX = viewWidth - scaledImageWidth;
            }
        }
        
        // Constrain vertical translation
        if (scaledImageHeight <= viewHeight) {
            // Center if image is smaller than view
            transY = (viewHeight - scaledImageHeight) / 2f;
        } else {
            // Constrain to bounds if image is larger than view
            if (transY > 0) {
                transY = 0;
            } else if (transY < viewHeight - scaledImageHeight) {
                transY = viewHeight - scaledImageHeight;
            }
        }
        
        values[Matrix.MTRANS_X] = transX;
        values[Matrix.MTRANS_Y] = transY;
        imageMatrix.setValues(values);
    }
    
    private void toggleToolbarVisibility() {
        if (toolbarOverlay.getVisibility() == VISIBLE) {
            toolbarOverlay.setVisibility(GONE);
        } else {
            toolbarOverlay.setVisibility(VISIBLE);
        }
    }
    
    public void setOnMediaGalleryListener(OnMediaGalleryListener listener) {
        this.listener = listener;
    }
    
    public Uri getCurrentMediaUri() {
        return currentMediaUri;
    }
}