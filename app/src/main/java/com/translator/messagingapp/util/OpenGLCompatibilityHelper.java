package com.translator.messagingapp.util;

import com.translator.messagingapp.message.*;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.util.Locale;

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

    /**
     * Gets OpenGL information for the device.
     * 
     * @param context The context
     * @return A string containing OpenGL information
     */
    public static String getOpenGLInfo(Context context) {
        StringBuilder info = new StringBuilder();
        
        try {
            // Get OpenGL version
            String glVersion = GLES20.glGetString(GLES20.GL_VERSION);
            String glVendor = GLES20.glGetString(GLES20.GL_VENDOR);
            String glRenderer = GLES20.glGetString(GLES20.GL_RENDERER);
            String glExtensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
            
            info.append("OpenGL Version: ").append(glVersion != null ? glVersion : "Unknown").append("\n");
            info.append("OpenGL Vendor: ").append(glVendor != null ? glVendor : "Unknown").append("\n");
            info.append("OpenGL Renderer: ").append(glRenderer != null ? glRenderer : "Unknown").append("\n");
            
            // Add device information
            info.append("\nDevice Information:\n");
            info.append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");
            info.append("Model: ").append(Build.MODEL).append("\n");
            info.append("Android Version: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
            
            // Add hardware acceleration status
            info.append("Hardware Acceleration: ").append(isHardwareAccelerated(context) ? "Enabled" : "Disabled").append("\n");
            
            // Check for known problematic devices
            boolean hasKnownIssues = hasKnownRenderingIssues();
            info.append("Known Rendering Issues: ").append(hasKnownIssues ? "Yes" : "No").append("\n");
            
            // Add a subset of extensions (can be very long)
            if (glExtensions != null && !glExtensions.isEmpty()) {
                String[] extensions = glExtensions.split(" ");
                info.append("\nSelected Extensions (").append(extensions.length).append(" total):\n");
                int count = 0;
                for (String extension : extensions) {
                    if (extension.contains("texture") || extension.contains("compression") || 
                        extension.contains("blend") || extension.contains("shader")) {
                        info.append("- ").append(extension).append("\n");
                        count++;
                        if (count >= 10) break; // Limit to 10 extensions
                    }
                }
            }
            
        } catch (Exception e) {
            info.append("Error retrieving OpenGL information: ").append(e.getMessage());
            Log.e(TAG, "Error retrieving OpenGL information", e);
        }
        
        return info.toString();
    }
    
    /**
     * Checks if the device has known OpenGL rendering issues.
     * 
     * @return True if the device has known issues, false otherwise
     */
    public static boolean hasKnownRenderingIssues() {
        String manufacturer = Build.MANUFACTURER.toLowerCase(Locale.US);
        String model = Build.MODEL.toLowerCase(Locale.US);
        int sdkVersion = Build.VERSION.SDK_INT;
        
        // Check for known problematic devices/configurations
        
        // Some older Samsung devices had swap behavior issues
        if (manufacturer.contains("samsung") && sdkVersion < Build.VERSION_CODES.N) {
            if (model.contains("sm-g9") || model.contains("sm-n9") || model.contains("sm-a5")) {
                return true;
            }
        }
        
        // Some MediaTek devices had issues
        if (manufacturer.contains("mediatek") && sdkVersion < Build.VERSION_CODES.O) {
            return true;
        }
        
        // Some older Huawei devices had issues
        if (manufacturer.contains("huawei") && sdkVersion < Build.VERSION_CODES.N) {
            if (model.contains("p8") || model.contains("mate 8") || model.contains("honor")) {
                return true;
            }
        }
        
        // Check for specific device/model combinations known to have issues
        String deviceFingerprint = manufacturer + ":" + model;
        String[] knownProblematicDevices = {
            "xiaomi:redmi note 4",
            "asus:zenfone 2",
            "lenovo:a6000",
            "lge:nexus 5",
            "oneplus:one"
        };
        
        for (String device : knownProblematicDevices) {
            if (deviceFingerprint.contains(device)) {
                return true;
            }
        }
        
        return false;
    }
}