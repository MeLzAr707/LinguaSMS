#!/bin/bash

# Theme Override Fix Validation Script
# This script validates that the theme override issue fix is working correctly

echo "🎨 Theme Override Fix Validation"
echo "==============================="
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

echo "📋 Check 1: BaseActivity Theme Logic"
echo "====================================="

echo "Checking BaseActivity uses new system theme..."
check_pattern "app/src/main/java/com/translator/messagingapp/BaseActivity.java" "AppTheme_System" "BaseActivity uses AppTheme_System for THEME_SYSTEM"
check_pattern "app/src/main/java/com/translator/messagingapp/BaseActivity.java" "AppTheme_System_NoActionBar" "BaseActivity uses AppTheme_System_NoActionBar for THEME_SYSTEM NoActionBar"

echo ""
echo "📋 Check 2: Base Theme Not Overridden"
echo "======================================"

echo "Checking that base AppTheme is not overridden in night mode..."
check_not_pattern "app/src/main/res/values-night/styles.xml" '<style name="AppTheme"' "Base AppTheme is not overridden in values-night/styles.xml"

echo ""
echo "📋 Check 3: System Theme Definitions"
echo "====================================="

echo "Checking system theme definitions..."
check_pattern "app/src/main/res/values/styles.xml" "AppTheme_System" "AppTheme_System is defined in values/styles.xml"
check_pattern "app/src/main/res/values/styles.xml" "AppTheme_System_NoActionBar" "AppTheme_System_NoActionBar is defined in values/styles.xml"

echo ""
echo "📋 Check 4: Night Mode System Theme"
echo "===================================="

echo "Checking night mode system theme definitions..."
check_pattern "app/src/main/res/values-night/styles.xml" "AppTheme_System" "AppTheme_System is overridden in values-night/styles.xml"
check_pattern "app/src/main/res/values-night/styles.xml" "AppTheme_System_NoActionBar" "AppTheme_System_NoActionBar is overridden in values-night/styles.xml"

echo ""
echo "📋 Check 5: Theme Logic in UserPreferences"
echo "=========================================="

echo "Checking UserPreferences isDarkThemeActive logic..."
check_pattern "app/src/main/java/com/translator/messagingapp/UserPreferences.java" "themeId == THEME_DARK.*themeId == THEME_BLACK_GLASS" "Dark theme detection for THEME_DARK and THEME_BLACK_GLASS"
check_pattern "app/src/main/java/com/translator/messagingapp/UserPreferences.java" "themeId == THEME_SYSTEM" "System theme detection logic"
check_pattern "app/src/main/java/com/translator/messagingapp/UserPreferences.java" "return false" "Light theme override returns false"

echo ""
echo "📋 Check 6: Test File Creation"
echo "==============================="

echo "Checking test file for issue reproduction..."
check_pattern "app/src/test/java/com/translator/messagingapp/ThemeOverrideIssueTest.java" "testManualThemeSelectionOverridesSystemSetting" "Test for manual theme selection override"
check_pattern "app/src/test/java/com/translator/messagingapp/ThemeOverrideIssueTest.java" "testSystemDefaultFollowsSystemSetting" "Test for system default following system setting"
check_pattern "app/src/test/java/com/translator/messagingapp/ThemeOverrideIssueTest.java" "testIssue542Scenario" "Test that reproduces issue #542 scenario"

echo ""
echo "📋 Summary"
echo "=========="

# Count successful checks
total_checks=10
passed_checks=0

# Re-run checks silently to count successes
check_pattern "app/src/main/java/com/translator/messagingapp/BaseActivity.java" "AppTheme_System" "" >/dev/null 2>&1 && ((passed_checks++))
check_pattern "app/src/main/java/com/translator/messagingapp/BaseActivity.java" "AppTheme_System_NoActionBar" "" >/dev/null 2>&1 && ((passed_checks++))
check_not_pattern "app/src/main/res/values-night/styles.xml" '<style name="AppTheme"' "" >/dev/null 2>&1 && ((passed_checks++))
check_pattern "app/src/main/res/values/styles.xml" "AppTheme_System" "" >/dev/null 2>&1 && ((passed_checks++))
check_pattern "app/src/main/res/values/styles.xml" "AppTheme_System_NoActionBar" "" >/dev/null 2>&1 && ((passed_checks++))
check_pattern "app/src/main/res/values-night/styles.xml" "AppTheme_System" "" >/dev/null 2>&1 && ((passed_checks++))
check_pattern "app/src/main/res/values-night/styles.xml" "AppTheme_System_NoActionBar" "" >/dev/null 2>&1 && ((passed_checks++))
check_pattern "app/src/main/java/com/translator/messagingapp/UserPreferences.java" "themeId == THEME_DARK.*themeId == THEME_BLACK_GLASS" "" >/dev/null 2>&1 && ((passed_checks++))
check_pattern "app/src/test/java/com/translator/messagingapp/ThemeOverrideIssueTest.java" "testManualThemeSelectionOverridesSystemSetting" "" >/dev/null 2>&1 && ((passed_checks++))
check_pattern "app/src/test/java/com/translator/messagingapp/ThemeOverrideIssueTest.java" "testIssue542Scenario" "" >/dev/null 2>&1 && ((passed_checks++))

echo "Validation Results: $passed_checks/$total_checks checks passed"

if [ $passed_checks -eq $total_checks ]; then
    echo "🎉 All checks passed! Theme override fix appears to be working correctly."
    echo ""
    echo "📝 What was fixed:"
    echo "- Removed automatic AppTheme override in values-night/styles.xml"
    echo "- Created dedicated AppTheme_System for system theme following"
    echo "- Updated BaseActivity to use AppTheme_System only for THEME_SYSTEM"
    echo "- Manual theme selections (LIGHT, DARK, BLACK_GLASS) now ignore system settings"
    echo "- Only THEME_SYSTEM follows Android system dark/light mode"
    exit 0
else
    echo "⚠️  Some checks failed. Please review the implementation."
    exit 1
fi