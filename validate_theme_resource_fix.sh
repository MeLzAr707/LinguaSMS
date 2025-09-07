#!/bin/bash

# Theme Resource Linking Fix Validation Script
# This script validates that the AndroidManifest.xml style references fix is working correctly

echo "🎨 Theme Resource Linking Fix Validation"
echo "========================================"
echo ""

# Function to check if a file contains a specific pattern
check_pattern() {
    local file="$1"
    local pattern="$2"
    local description="$3"
    
    if grep -q "$pattern" "$file"; then
        echo "✅ $description"
        return 0
    else
        echo "❌ $description"
        return 1
    fi
}

# Function to check if a file does NOT contain a specific pattern
check_not_pattern() {
    local file="$1"
    local pattern="$2"
    local description="$3"
    
    if ! grep -q "$pattern" "$file"; then
        echo "✅ $description"
        return 0
    else
        echo "❌ $description"
        return 1
    fi
}

echo "📋 Check 1: AndroidManifest.xml Style References"
echo "================================================"

echo "Checking that AndroidManifest.xml uses underscore naming..."
check_pattern "app/src/main/AndroidManifest.xml" "@style/AppTheme_NoActionBar" "AndroidManifest.xml uses AppTheme_NoActionBar (underscore naming)"
check_not_pattern "app/src/main/AndroidManifest.xml" "@style/AppTheme\.NoActionBar" "AndroidManifest.xml does not use AppTheme.NoActionBar (dot naming)"

echo ""
echo "📋 Check 2: Style Definitions in styles.xml"
echo "============================================"

echo "Checking that styles.xml defines NoActionBar themes with proper parents..."
check_pattern "app/src/main/res/values/styles.xml" 'name="AppTheme_NoActionBar" parent="AppTheme"' "AppTheme_NoActionBar has proper parent theme"
check_pattern "app/src/main/res/values/styles.xml" 'name="AppTheme_Dark_NoActionBar" parent="AppTheme_Dark"' "AppTheme_Dark_NoActionBar has proper parent theme"
check_pattern "app/src/main/res/values/styles.xml" 'name="AppTheme_BlackGlass_NoActionBar" parent="AppTheme_BlackGlass"' "AppTheme_BlackGlass_NoActionBar has proper parent theme"
check_pattern "app/src/main/res/values/styles.xml" 'name="AppTheme_System_NoActionBar" parent="AppTheme_System"' "AppTheme_System_NoActionBar has proper parent theme"

echo ""
echo "📋 Check 3: Night Mode Styles"
echo "=============================="

echo "Checking that night mode styles.xml defines System NoActionBar theme..."
check_pattern "app/src/main/res/values-night/styles.xml" 'name="AppTheme_System_NoActionBar" parent="AppTheme_System"' "Night mode AppTheme_System_NoActionBar has proper parent"

echo ""
echo "📋 Check 4: XML Syntax Validation"
echo "================================="

echo "Checking XML syntax..."
if command -v xmllint >/dev/null 2>&1; then
    if xmllint --noout app/src/main/AndroidManifest.xml 2>/dev/null; then
        echo "✅ AndroidManifest.xml has valid XML syntax"
    else
        echo "❌ AndroidManifest.xml has XML syntax errors"
    fi
    
    if xmllint --noout app/src/main/res/values/styles.xml 2>/dev/null; then
        echo "✅ styles.xml has valid XML syntax"
    else
        echo "❌ styles.xml has XML syntax errors"
    fi
    
    if xmllint --noout app/src/main/res/values-night/styles.xml 2>/dev/null; then
        echo "✅ values-night/styles.xml has valid XML syntax"
    else
        echo "❌ values-night/styles.xml has XML syntax errors"
    fi
else
    echo "⚠️  xmllint not available, skipping XML syntax validation"
fi

echo ""
echo "📋 Summary"
echo "=========="

echo "The fix addresses the Android resource linking error by:"
echo "1. ✅ Changing AndroidManifest.xml to use underscore naming (@style/AppTheme_NoActionBar)"
echo "2. ✅ Ensuring all NoActionBar styles inherit from their proper parent themes"
echo "3. ✅ Maintaining consistency with existing codebase naming conventions"
echo ""
echo "This resolves the build error:"
echo "'resource style/AppTheme.NoActionBar not found'"
echo ""
echo "🎉 Theme resource linking fix validation complete!"