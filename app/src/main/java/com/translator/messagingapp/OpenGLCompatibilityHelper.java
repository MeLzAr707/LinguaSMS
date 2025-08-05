package com.translator.messagingapp;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

/**
 * Utility class to help manage OpenGL renderer configuration and prevent
 * "Unable to match the desired swap behavior" errors.
 */
public class OpenGLCompatibilityHelper {
    private static final String TAG = "OpenGLCompatibility";
    
    /**
     * Checks if hardware acceleration is enabled for the application.
     */
    public static boolean isHardwareAccelerated(Context context) {
        try {
            ApplicationInfo appInfo = context.getApplicationInfo();
            return (appInfo.flags & ApplicationInfo.FLAG_HARDWARE_ACCELERATED) != 0;
        } catch (Exception e) {
            Log.w(TAG, "Unable to check hardware acceleration status", e);
            return false;
        }
    }
    
    /**
     * Configures window settings for optimal OpenGL compatibility.
     * Call this method in Activity.onCreate() before setContentView().
     */
    public static void configureWindowForOpenGL(Activity activity) {
        try {
            Window window = activity.getWindow();
            
            // Ensure hardware acceleration is enabled for this window
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );
            
            // Clear any flags that might interfere with OpenGL rendering
            clearProblematicFlags(window);
            
            Log.d(TAG, "Window configured for OpenGL compatibility");
        } catch (Exception e) {
            Log.e(TAG, "Failed to configure window for OpenGL", e);
        }
    }
    
    /**
     * Removes window flags that are known to cause OpenGL swap behavior issues.
     */
    private static void clearProblematicFlags(Window window) {
        try {
            // FLAG_LAYOUT_NO_LIMITS is known to cause "Unable to match desired swap behavior"
            // Remove it if it was set
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            
            // Ensure we're not using translucent windows which can cause OpenGL issues
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            
            Log.d(TAG, "Cleared problematic window flags");
        } catch (Exception e) {
            Log.w(TAG, "Unable to clear some window flags", e);
        }
    }
    
    /**
     * Safely configures status bar and navigation bar colors without
     * interfering with OpenGL rendering.
     */
    public static void safelyConfigureSystemBars(Activity activity, int statusBarColor, int navigationBarColor) {
        try {
            Window window = activity.getWindow();
            
            // Set colors directly without layout-affecting flags
            window.setStatusBarColor(statusBarColor);
            window.setNavigationBarColor(navigationBarColor);
            
            // Ensure no conflicting flags are set
            clearProblematicFlags(window);
            
            Log.d(TAG, "System bars configured safely");
        } catch (Exception e) {
            Log.e(TAG, "Failed to configure system bars safely", e);
        }
    }
    
    /**
     * Logs current window configuration for debugging OpenGL issues.
     */
    public static void logWindowConfiguration(Activity activity) {
        try {
            Window window = activity.getWindow();
            WindowManager.LayoutParams params = window.getAttributes();
            
            Log.d(TAG, "Window flags: 0x" + Integer.toHexString(params.flags));
            Log.d(TAG, "Hardware accelerated: " + isHardwareAccelerated(activity));
            Log.d(TAG, "Status bar color: 0x" + Integer.toHexString(window.getStatusBarColor()));
            Log.d(TAG, "Navigation bar color: 0x" + Integer.toHexString(window.getNavigationBarColor()));
        } catch (Exception e) {
            Log.e(TAG, "Failed to log window configuration", e);
        }
    }
}