# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep the problematic dependencies that are causing NullPointerExceptions
-keep class androidx.constraintlayout.** { *; }
-keep class androidx.profileinstaller.** { *; }

# Prevent D8 from optimizing these classes too aggressively
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions

# Prevent issues with the Kotlin standard library
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-keepclassmembers class kotlin.Metadata { *; }

# Prevent issues with AndroidX libraries
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-keep class com.google.android.material.** { *; }
-dontwarn androidx.**
-dontwarn com.google.android.material.**

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# Remove debug-related classes in release builds
# Keep the DebugActivity class in debug builds, remove in release
-if class com.translator.messagingapp.BuildConfig {
  boolean DEBUG return false;
}
-keep class com.translator.messagingapp.DebugActivity
-keep class com.translator.messagingapp.DebugUtils {
    public static boolean isDebugBuild();
    public static void logError(...);
}

# Keep the problematic dependencies that are causing NullPointerExceptions
-keep class androidx.constraintlayout.** { *; }
-keep class androidx.profileinstaller.** { *; }

# Prevent D8 from optimizing these classes too aggressively
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions

# Prevent issues with the Kotlin standard library
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-keepclassmembers class kotlin.Metadata { *; }

# Prevent issues with AndroidX libraries
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-keep class com.google.android.material.** { *; }
-dontwarn androidx.**
-dontwarn com.google.android.material.**

# WebRTC specific rules for proper functionality
-keep class org.webrtc.** { *; }
-keepclassmembers class org.webrtc.** { *; }
-dontwarn org.webrtc.**
