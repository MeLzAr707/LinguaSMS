#!/bin/bash

# Test script to validate MMS manifest and default app handling fixes for issue #614
echo "=== MMS Manifest and Default App Handling Fix Validation ==="
echo "Issue #614: Fix MMS Sending: Manifest and Default App Handling"
echo

# Check that the key changes are in place
echo "Verifying manifest and code changes..."
echo

# Test 1: Check for system MMS transaction service in manifest
echo "Test 1: System MMS Transaction Service Declaration"
if grep -q 'android:name="com.android.mms.transaction.TransactionService"' app/src/main/AndroidManifest.xml; then
    echo "✓ System MMS transaction service properly declared in manifest"
else
    echo "✗ System MMS transaction service missing from manifest"
    exit 1
fi

if grep -A2 -B2 "com.android.mms.transaction.TransactionService" app/src/main/AndroidManifest.xml | grep -q 'android:exported="false"'; then
    echo "✓ System MMS transaction service properly configured as non-exported"
else
    echo "✗ System MMS transaction service export configuration issue"
    exit 1
fi

echo

# Test 2: Check for custom MMS transaction service still present
echo "Test 2: Custom MMS Transaction Service Declaration"
if grep -q 'android:name=".mms.TransactionService"' app/src/main/AndroidManifest.xml; then
    echo "✓ Custom MMS transaction service still properly declared"
else
    echo "✗ Custom MMS transaction service missing"
    exit 1
fi

echo

# Test 3: Check for MMS sent receiver
echo "Test 3: MMS Send Receiver Declaration"
if grep -q 'android:name=".mms.MmsSendReceiver"' app/src/main/AndroidManifest.xml; then
    echo "✓ MMS send receiver properly declared in manifest"
else
    echo "✗ MMS send receiver missing from manifest"
    exit 1
fi

if grep -q 'com.translator.messagingapp.MMS_SENT' app/src/main/AndroidManifest.xml; then
    echo "✓ MMS send receiver has proper action filter"
else
    echo "✗ MMS send receiver action filter missing"
    exit 1
fi

echo

# Test 4: Check default SMS app manager implementation
echo "Test 4: Default SMS App Manager Implementation"
if grep -q "RoleManager.ROLE_SMS" app/src/main/java/com/translator/messagingapp/system/DefaultSmsAppManager.java; then
    echo "✓ RoleManager properly implemented for Android 10+"
else
    echo "✗ RoleManager implementation missing"
    exit 1
fi

if grep -q "Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT" app/src/main/java/com/translator/messagingapp/system/DefaultSmsAppManager.java; then
    echo "✓ Legacy default SMS app request properly implemented"
else
    echo "✗ Legacy default SMS app request missing"
    exit 1
fi

echo

# Test 5: Check UI integration of default SMS app requests
echo "Test 5: UI Integration of Default SMS App Requests"
if grep -q "checkAndRequestDefaultSmsApp" app/src/main/java/com/translator/messagingapp/ui/MainActivity.java; then
    echo "✓ Default SMS app check integrated in main activity"
else
    echo "✗ Default SMS app check missing from main activity"
    exit 1
fi

if grep -q "requestDefaultSmsApp" app/src/main/java/com/translator/messagingapp/ui/MainActivity.java; then
    echo "✓ Default SMS app request available in UI"
else
    echo "✗ Default SMS app request missing from UI"
    exit 1
fi

echo

# Test 6: Check proper permissions for MMS functionality
echo "Test 6: MMS Permissions"
required_permissions=("SEND_MMS" "RECEIVE_MMS" "RECEIVE_WAP_PUSH")
for perm in "${required_permissions[@]}"; do
    if grep -q "android.permission.$perm" app/src/main/AndroidManifest.xml; then
        echo "✓ $perm permission declared"
    else
        echo "✗ $perm permission missing"
        exit 1
    fi
done

echo

echo "=== Manifest and Code Validation Summary ==="
echo "✓ All key fixes are in place for MMS manifest and default app handling"
echo

echo "Expected behavior after fix:"
echo "1. Both system and custom MMS transaction services are properly declared"
echo "2. MMS send receiver is properly configured with correct action filters"
echo "3. Default SMS app requests use appropriate API for device Android version"
echo "4. UI properly integrates default SMS app checking and requesting"
echo "5. All required MMS permissions are properly declared"
echo

echo "Manual testing steps:"
echo "1. Install the app and verify it requests to be default SMS app"
echo "2. Send MMS messages and verify they are processed correctly"
echo "3. Check that both transaction services can handle MMS operations"
echo "4. Verify MMS send status is properly reported via receiver"
echo

echo "Test completed successfully! 🎉"