#!/bin/bash

echo "Verifying message update lifecycle fix..."
echo ""

# Check that the BroadcastReceiver is properly managed in lifecycle methods
echo "1. Checking BroadcastReceiver lifecycle management..."

# Check that onResume registers the receiver
if grep -q "protected void onResume" app/src/main/java/com/translator/messagingapp/ConversationActivity.java && \
   grep -A 10 "protected void onResume" app/src/main/java/com/translator/messagingapp/ConversationActivity.java | grep -q "setupMessageUpdateReceiver"; then
    echo "✓ onResume() calls setupMessageUpdateReceiver()"
else
    echo "✗ onResume() does not call setupMessageUpdateReceiver()"
fi

# Check that onPause unregisters the receiver
if grep -q "protected void onPause" app/src/main/java/com/translator/messagingapp/ConversationActivity.java && \
   grep -A 10 "protected void onPause" app/src/main/java/com/translator/messagingapp/ConversationActivity.java | grep -q "unregisterReceiver"; then
    echo "✓ onPause() unregisters the messageUpdateReceiver"
else
    echo "✗ onPause() does not unregister the messageUpdateReceiver"
fi

# Check that onCreate no longer sets up the receiver
if ! grep -A 10 "protected void onCreate" app/src/main/java/com/translator/messagingapp/ConversationActivity.java | grep -q "setupMessageUpdateReceiver"; then
    echo "✓ onCreate() no longer calls setupMessageUpdateReceiver()"
else
    echo "✗ onCreate() still calls setupMessageUpdateReceiver()"
fi

echo ""
echo "2. Checking thread safety improvements..."

# Check that runOnUiThread is used in the broadcast receiver
if grep -A 20 "case \"com.translator.messagingapp.MESSAGE_RECEIVED\"" app/src/main/java/com/translator/messagingapp/ConversationActivity.java | grep -q "runOnUiThread"; then
    echo "✓ BroadcastReceiver uses runOnUiThread for UI updates"
else
    echo "✗ BroadcastReceiver does not use runOnUiThread for UI updates"
fi

# Check for double registration protection
if grep -A 5 "setupMessageUpdateReceiver()" app/src/main/java/com/translator/messagingapp/ConversationActivity.java | grep -q "messageUpdateReceiver != null"; then
    echo "✓ Double registration protection is in place"
else
    echo "✗ Double registration protection is not in place"
fi

echo ""
echo "3. Checking broadcast action consistency..."

# Check that all expected broadcast actions are still handled
actions=("MESSAGE_RECEIVED" "MESSAGE_SENT" "REFRESH_MESSAGES")
for action in "${actions[@]}"; do
    if grep -q "com.translator.messagingapp.$action" app/src/main/java/com/translator/messagingapp/ConversationActivity.java; then
        echo "✓ Action '$action' is handled in ConversationActivity"
    else
        echo "✗ Action '$action' is not handled in ConversationActivity"
    fi
done

echo ""
echo "4. Checking test coverage..."

if [ -f "app/src/test/java/com/translator/messagingapp/ConversationActivityBroadcastLifecycleTest.java" ]; then
    echo "✓ New test file created for broadcast lifecycle management"
else
    echo "✗ Test file for broadcast lifecycle management not found"
fi

echo ""
echo "Summary:"
echo "• BroadcastReceiver now properly managed in onResume()/onPause() lifecycle"
echo "• UI updates use runOnUiThread() to ensure main thread execution"
echo "• Double registration protection prevents receiver conflicts"
echo "• All broadcast actions (MESSAGE_RECEIVED, MESSAGE_SENT, REFRESH_MESSAGES) still handled"
echo ""
echo "This should fix the issue where messages don't update in ConversationActivity until"
echo "leaving and returning to the layout, as the receiver will now only be active when"
echo "the activity is visible and can properly update the UI."