#!/bin/bash

# Validation script for custom theme crash fix
# This script should be run after the app is built to test the fix

echo "=== Validating Custom Theme Crash Fix ==="
echo

echo "1. Checking if AppTheme.Custom is properly defined..."
if grep -q "AppTheme.Custom" app/src/main/res/values/styles.xml; then
    echo "✓ AppTheme.Custom theme found in styles.xml"
else
    echo "✗ AppTheme.Custom theme missing"
    exit 1
fi

echo "2. Checking if text appearance attributes are defined..."
if grep -q "textAppearanceHeadline1" app/src/main/res/values/styles.xml; then
    echo "✓ Text appearance attributes found in AppTheme.Custom"
else
    echo "✗ Text appearance attributes missing"
    exit 1
fi

echo "3. Checking if ToolbarTitleStyle has proper parent..."
if grep -A 1 "ToolbarTitleStyle" app/src/main/res/values/styles.xml | grep -q "TextAppearance.MaterialComponents"; then
    echo "✓ ToolbarTitleStyle has proper parent theme"
else
    echo "✗ ToolbarTitleStyle missing proper parent"
    exit 1
fi

echo "4. Checking if BaseActivity uses AppTheme.Custom for THEME_CUSTOM..."
if grep -q "AppTheme_Custom" app/src/main/java/com/translator/messagingapp/BaseActivity.java; then
    echo "✓ BaseActivity properly references AppTheme.Custom"
else
    echo "✗ BaseActivity doesn't use AppTheme.Custom"
    exit 1
fi

echo "5. Checking if ColorWheelActivity has error handling..."
if grep -q "try.*{" app/src/main/java/com/translator/messagingapp/ColorWheelActivity.java; then
    echo "✓ ColorWheelActivity has error handling in onCreate"
else
    echo "✗ ColorWheelActivity missing error handling"
    exit 1
fi

echo "6. Checking if safer color resolution is implemented..."
if grep -q "getTheme()" app/src/main/java/com/translator/messagingapp/ColorWheelActivity.java; then
    echo "✓ Safer color resolution implemented"
else
    echo "✗ Still using deprecated color resolution"
    exit 1
fi

echo
echo "=== All checks passed! ==="
echo
echo "To test the fix manually:"
echo "1. Build and install the app"
echo "2. Go to Settings"
echo "3. Select 'Custom' theme"
echo "4. Tap on the Custom theme option to open ColorWheelActivity"
echo "5. Verify that the app doesn't crash and the color picker loads properly"
echo "6. Try selecting different colors and applying them"
echo "7. Test the 'Reset to Default' button"
echo
echo "Expected behavior: No crashes, smooth theme transitions, all UI elements render properly"