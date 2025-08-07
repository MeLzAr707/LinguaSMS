package com.translator.messagingapp;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

/**
 * Glide module for optimized contact avatar loading configuration.
 */
@GlideModule
public final class MyAppGlideModule extends AppGlideModule {
    
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        // Configure default request options for better performance with contact avatars
        RequestOptions defaultOptions = new RequestOptions()
            .format(DecodeFormat.PREFER_RGB_565) // Use less memory for small avatar images
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE) // Cache transformed images
            .skipMemoryCache(false); // Enable memory cache for frequently accessed avatars
            
        builder.setDefaultRequestOptions(defaultOptions);
    }

    @Override
    public boolean isManifestParsingEnabled() {
        // Disable manifest parsing for better performance
        return false;
    }
}

