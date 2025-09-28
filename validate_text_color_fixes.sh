#!/bin/bash

echo "🔍 Validating Text Color Fixes for Dark Mode Visibility"
echo "========================================================="
echo ""

# Function to check if a file contains a specific line
check_line() {
    local file="$1"
    local search="$2" 
    local description="$3"
    
    if [ -f "$file" ]; then
        if grep -q "$search" "$file"; then
            echo "✅ $description"
        else
            echo "❌ $description"
            echo "   Expected: $search"
        fi
    else
        echo "❌ File not found: $file"
    fi
}

echo "📋 Check 1: Night Mode Text Color Overrides"
echo "============================================"

echo "Checking night mode color overrides..."
check_line "app/src/main/res/values-night/colors-night.xml" \
           '<color name="textColorPrimary">#FFFFFF</color>' \
           "Primary text color overridden to white in night mode"

check_line "app/src/main/res/values-night/colors-night.xml" \
           '<color name="textColorSecondary">#B3FFFFFF</color>' \
           "Secondary text color overridden to light gray in night mode"

echo ""
echo "📋 Check 2: Navigation Header Text Colors" 
echo "=========================================="

echo "Checking navigation header text colors..."
check_line "app/src/main/res/layout/nav_header_main.xml" \
           'android:textColor="@android:color/white"' \
           "App name text explicitly set to white"

check_line "app/src/main/res/layout/nav_header_main.xml" \
           'android:text="@string/app_version"' \
           "App version text found"

echo ""
echo "📋 Check 3: Toolbar Theme Fixes"
echo "================================"

echo "Checking toolbar themes for white text and icons..."
check_line "app/src/main/res/layout/activity_conversation_updated.xml" \
           'android:theme="@style/ThemeOverlay.AppCompat.Dark.ActionBar"' \
           "Conversation activity toolbar uses dark theme overlay"

check_line "app/src/main/res/layout/activity_offline_models.xml" \
           'android:theme="@style/ThemeOverlay.AppCompat.Dark.ActionBar"' \
           "Offline models activity toolbar uses dark theme overlay"

echo ""
echo "📋 Check 4: Incoming Message Layout Text Colors"
echo "==============================================="

echo "Checking incoming message layouts use theme-aware colors..."
check_line "app/src/main/res/layout/item_message_incoming.xml" \
           'android:textColor="@color/textColorPrimary"' \
           "Incoming message text uses theme-aware primary color"

check_line "app/src/main/res/layout/item_message_incoming.xml" \
           'android:textColor="@color/textColorSecondary"' \
           "Incoming message date/metadata uses theme-aware secondary color"

check_line "app/src/main/res/layout/item_message_incoming_media.xml" \
           'android:textColor="@color/textColorPrimary"' \
           "Incoming media message text uses theme-aware primary color"

echo ""
echo "📋 Check 5: Original Color Definitions"
echo "======================================"

echo "Verifying base color definitions are intact..."
check_line "app/src/main/res/values/colors.xml" \
           '<color name="textColorPrimary">#212121</color>' \
           "Light theme primary text color remains dark"

check_line "app/src/main/res/values/colors.xml" \
           '<color name="textColorPrimary_dark">#FFFFFF</color>' \
           "Dark theme specific primary text color is white"

echo ""
echo "🎯 Summary of Changes"
echo "===================="
echo "1. Added textColorPrimary and textColorSecondary overrides in night mode"
echo "2. Made navigation header text explicitly white for all themes"
echo "3. Fixed toolbar themes to use dark overlay (white text/icons)"
echo "4. Ensured incoming message layouts use theme-aware text colors"
echo ""
echo "🧪 Expected Results:"
echo "- Dark Theme: All text appears white or light gray"
echo "- Black Glass Theme: All text appears white or light gray"  
echo "- Light Theme: Remains unchanged with dark text"
echo "- Header elements (contact names, back button, menu) are white in dark themes"
echo "- Chat bubble text is white in dark themes"
echo ""
echo "✅ Validation complete! Please test the app to verify visual changes."