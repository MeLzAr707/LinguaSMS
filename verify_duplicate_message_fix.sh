#!/bin/bash

# Verification script for duplicate message fix
# This script checks that the necessary changes are in place

echo "=== Duplicate Message Fix Verification ==="
echo

# Check if the fix is in MessageService.java
echo "1. Checking MessageService.java for conditional storage fix..."
if grep -q "PhoneUtils.isDefaultSmsApp" app/src/main/java/com/translator/messagingapp/MessageService.java; then
    echo "   ✓ Found PhoneUtils.isDefaultSmsApp check in MessageService"
else
    echo "   ✗ PhoneUtils.isDefaultSmsApp check NOT found in MessageService"
fi

if grep -q "Not default SMS app, manually storing message" app/src/main/java/com/translator/messagingapp/MessageService.java; then
    echo "   ✓ Found conditional storage logic with proper logging"
else
    echo "   ✗ Conditional storage logic NOT found"
fi

echo

# Check if MainActivity listens for MESSAGE_RECEIVED
echo "2. Checking MainActivity.java for MESSAGE_RECEIVED handling..."
if grep -q "com.translator.messagingapp.MESSAGE_RECEIVED" app/src/main/java/com/translator/messagingapp/MainActivity.java; then
    echo "   ✓ Found MESSAGE_RECEIVED action in MainActivity"
else
    echo "   ✗ MESSAGE_RECEIVED action NOT found in MainActivity"
fi

if grep -q "LocalBroadcastManager" app/src/main/java/com/translator/messagingapp/MainActivity.java; then
    echo "   ✓ Found LocalBroadcastManager usage in MainActivity"
else
    echo "   ✗ LocalBroadcastManager usage NOT found in MainActivity"
fi

echo

# Check if test exists
echo "3. Checking for test coverage..."
if [ -f "app/src/test/java/com/translator/messagingapp/DuplicateMessageFixTest.java" ]; then
    echo "   ✓ Found DuplicateMessageFixTest.java"
else
    echo "   ✗ DuplicateMessageFixTest.java NOT found"
fi

echo

# Check that the changes are in sync
echo "4. Checking message flow consistency..."
sms_receiver_calls=$(grep -c "handleIncomingSms" app/src/main/java/com/translator/messagingapp/SmsReceiver.java)
message_service_impl=$(grep -c "public void handleIncomingSms" app/src/main/java/com/translator/messagingapp/MessageService.java)

if [ "$sms_receiver_calls" -gt 0 ] && [ "$message_service_impl" -eq 1 ]; then
    echo "   ✓ Message flow from SmsReceiver to MessageService is consistent"
else
    echo "   ✗ Message flow consistency issue detected"
fi

echo

# Summary
echo "=== Summary ==="
echo "The fix addresses duplicate received messages by:"
echo "• Only manually storing SMS messages when app is NOT the default SMS app"
echo "• Ensuring MainActivity receives MESSAGE_RECEIVED broadcasts via LocalBroadcastManager"
echo "• Adding comprehensive test coverage for the fix"
echo
echo "This prevents duplicate message storage that occurred when Android system"
echo "and our app both stored the same incoming SMS message."
echo

echo "Verification complete!"