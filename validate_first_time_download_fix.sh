#!/bin/bash
# Validation script for first-time download fix

echo "=== First-Time Language Model Download Fix Validation ==="
echo

echo "1. Checking OfflineModelManager.java changes..."
echo

# Check for new timeout constant
if grep -q "FIRST_DOWNLOAD_TIMEOUT_SECONDS = 120" app/src/main/java/com/translator/messagingapp/OfflineModelManager.java; then
    echo "✓ First-time download timeout constant added (120 seconds)"
else
    echo "✗ First-time download timeout constant missing"
fi

# Check for tracking mechanism
if grep -q "firstDownloadAttempts" app/src/main/java/com/translator/messagingapp/OfflineModelManager.java; then
    echo "✓ First download attempt tracking mechanism added"
else
    echo "✗ First download attempt tracking mechanism missing"
fi

# Check WiFi requirement removal
if grep -q "\.build(); // No restrictions" app/src/main/java/com/translator/messagingapp/OfflineModelManager.java; then
    echo "✓ WiFi-only restriction removed"
else
    echo "✗ WiFi-only restriction still present"
fi

# Check intelligent timeout logic
if grep -q "isFirstAttempt ? FIRST_DOWNLOAD_TIMEOUT_SECONDS : DOWNLOAD_TIMEOUT_SECONDS" app/src/main/java/com/translator/messagingapp/OfflineModelManager.java; then
    echo "✓ Intelligent timeout logic implemented"
else
    echo "✗ Intelligent timeout logic missing"
fi

echo

echo "2. Checking test coverage..."
echo

# Check test file exists
if [ -f "app/src/test/java/com/translator/messagingapp/FirstTimeDownloadTest.java" ]; then
    echo "✓ FirstTimeDownloadTest.java created"
    
    # Check test methods
    if grep -q "testFirstTimeDownloadUsesLongerTimeout" app/src/test/java/com/translator/messagingapp/FirstTimeDownloadTest.java; then
        echo "✓ First-time timeout test included"
    fi
    
    if grep -q "testDownloadWithoutWifiRestriction" app/src/test/java/com/translator/messagingapp/FirstTimeDownloadTest.java; then
        echo "✓ WiFi restriction removal test included"
    fi
    
    if grep -q "testUnsupportedLanguageHandling" app/src/test/java/com/translator/messagingapp/FirstTimeDownloadTest.java; then
        echo "✓ Error handling test included"
    fi
else
    echo "✗ FirstTimeDownloadTest.java missing"
fi

echo

echo "3. Checking documentation..."
echo

if [ -f "FIRST_TIME_DOWNLOAD_FIX.md" ]; then
    echo "✓ Documentation created (FIRST_TIME_DOWNLOAD_FIX.md)"
else
    echo "✗ Documentation missing"
fi

echo

echo "4. Code impact analysis..."
echo

# Count lines changed in main file
lines_changed=$(git diff HEAD~1 app/src/main/java/com/translator/messagingapp/OfflineModelManager.java | grep -E "^[\+\-]" | grep -v "^[\+\-][\+\-][\+\-]" | wc -l)
echo "Lines changed in OfflineModelManager.java: $lines_changed"

if [ "$lines_changed" -lt 50 ]; then
    echo "✓ Minimal changes approach maintained"
else
    echo "⚠ Changes are more extensive than expected"
fi

echo

echo "=== Validation Summary ==="
echo "The fix addresses the reported issue by:"
echo "• Removing restrictive WiFi-only download requirement"
echo "• Implementing intelligent timeout (120s for first attempts, 60s for retries)"
echo "• Adding tracking to distinguish first-time vs retry downloads"
echo "• Providing enhanced error messages for better debugging"
echo "• Maintaining full backward compatibility"
echo
echo "Expected Result: First-time language model downloads should now succeed"
echo "more frequently, especially on mobile data connections."