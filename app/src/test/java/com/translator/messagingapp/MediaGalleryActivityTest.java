package com.translator.messagingapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;

/**
 * Test for MediaGalleryActivity functionality
 */
@RunWith(RobolectricTestRunner.class)
public class MediaGalleryActivityTest {
    
    @Test
    public void testCreateIntent() {
        Context context = RuntimeEnvironment.getApplication();
        Uri testUri = Uri.parse("content://test/image.jpg");
        
        Intent intent = MediaGalleryActivity.createIntent(context, testUri);
        
        assertNotNull("Intent should not be null", intent);
        assertEquals("Intent should target MediaGalleryActivity", 
                MediaGalleryActivity.class.getName(), 
                intent.getComponent().getClassName());
        
        Uri intentUri = intent.getParcelableExtra("extra_media_uri");
        assertEquals("Intent should contain the media URI", testUri, intentUri);
    }
    
    @Test
    public void testCreateIntentWithNullUri() {
        Context context = RuntimeEnvironment.getApplication();
        
        Intent intent = MediaGalleryActivity.createIntent(context, null);
        
        assertNotNull("Intent should not be null even with null URI", intent);
        assertEquals("Intent should target MediaGalleryActivity", 
                MediaGalleryActivity.class.getName(), 
                intent.getComponent().getClassName());
        
        Uri intentUri = intent.getParcelableExtra("extra_media_uri");
        assertNull("Intent should contain null URI", intentUri);
    }
}