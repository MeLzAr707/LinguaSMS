package com.translator.messagingapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Simple test activity to verify OpenGL configuration works correctly.
 * This can be used for manual testing to ensure the fixes work.
 */
public class OpenGLTestActivity extends BaseActivity {
    private static final String TAG = "OpenGLTestActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create a simple layout programmatically to test rendering
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.DKGRAY);
        
        // Add test views
        TextView title = new TextView(this);
        title.setText("OpenGL Renderer Test");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setPadding(20, 20, 20, 10);
        layout.addView(title);
        
        TextView status = new TextView(this);
        status.setText("Hardware Acceleration: " + 
            (OpenGLCompatibilityHelper.isHardwareAccelerated(this) ? "ENABLED" : "DISABLED"));
        status.setTextColor(Color.LTGRAY);
        status.setTextSize(16);
        status.setPadding(20, 10, 20, 20);
        layout.addView(status);
        
        TextView info = new TextView(this);
        info.setText("If you can see this text clearly without rendering issues, " +
                    "the OpenGL renderer fix is working correctly.");
        info.setTextColor(Color.WHITE);
        info.setTextSize(14);
        info.setPadding(20, 10, 20, 20);
        layout.addView(info);
        
        setContentView(layout);
        
        // Debug log the OpenGL configuration
        debugOpenGLConfiguration();
        
        Log.i(TAG, "OpenGL test activity created successfully");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "OpenGL test activity resumed - checking for rendering issues");
    }
    
    /**
     * Debug the OpenGL configuration and log details.
     * This helps identify potential rendering issues.
     */
    private void debugOpenGLConfiguration() {
        boolean isHardwareAccelerated = OpenGLCompatibilityHelper.isHardwareAccelerated(this);
        Log.d(TAG, "Hardware Acceleration: " + (isHardwareAccelerated ? "ENABLED" : "DISABLED"));
        
        // Log device information
        Log.d(TAG, "Device: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
        Log.d(TAG, "Android Version: " + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + ")");
        
        // Get OpenGL info from helper class if available
        try {
            String glInfo = OpenGLCompatibilityHelper.getOpenGLInfo(this);
            Log.d(TAG, "OpenGL Info: " + glInfo);
        } catch (Exception e) {
            Log.e(TAG, "Error getting OpenGL info", e);
        }
        
        // Check for known problematic configurations
        boolean hasKnownIssues = false;
        try {
            hasKnownIssues = OpenGLCompatibilityHelper.hasKnownRenderingIssues();
            Log.d(TAG, "Known Rendering Issues: " + (hasKnownIssues ? "YES" : "NO"));
        } catch (Exception e) {
            Log.e(TAG, "Error checking for known issues", e);
        }
        
        // Log the current renderer
        View view = getWindow().getDecorView();
        boolean viewAccelerated = view != null && view.isHardwareAccelerated();
        Log.d(TAG, "View Hardware Acceleration: " + (viewAccelerated ? "ENABLED" : "DISABLED"));
    }
}