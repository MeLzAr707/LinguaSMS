#!/bin/bash

echo "Verifying sent message storage fix..."
echo ""

# Check that storeSentSmsMessage method was added
echo "1. Checking sent message storage method..."
if grep -q "private void storeSentSmsMessage" app/src/main/java/com/translator/messagingapp/MessageService.java; then
    echo "✓ storeSentSmsMessage() method added"
else
    echo "✗ storeSentSmsMessage() method not found"
fi

# Check that sent messages use MESSAGE_TYPE_SENT
if grep -A 10 "private void storeSentSmsMessage" app/src/main/java/com/translator/messagingapp/MessageService.java | grep -q "MESSAGE_TYPE_SENT"; then
    echo "✓ Sent messages use MESSAGE_TYPE_SENT"
else
    echo "✗ Sent messages do not use MESSAGE_TYPE_SENT"
fi

# Check that sent messages are marked as read
if grep -A 10 "private void storeSentSmsMessage" app/src/main/java/com/translator/messagingapp/MessageService.java | grep -q "READ, 1"; then
    echo "✓ Sent messages marked as read"
else
    echo "✗ Sent messages not marked as read"
fi

# Check that storeSentSmsMessage is called before broadcast
echo ""
echo "2. Checking storage timing..."
if grep -B 20 -A 5 "broadcastMessageSent" app/src/main/java/com/translator/messagingapp/MessageService.java | grep -q "storeSentSmsMessage"; then
    echo "✓ storeSentSmsMessage() called before broadcastMessageSent()"
else
    echo "✗ storeSentSmsMessage() not called before broadcast"
fi

# Check that MESSAGE_SENT clears cache
echo ""
echo "3. Checking cache clearing for message broadcasts..."
if grep -A 5 "MESSAGE_SENT" app/src/main/java/com/translator/messagingapp/ConversationActivity.java | grep -q "clearCacheForThread"; then
    echo "✓ MESSAGE_SENT clears cache before loading messages"
else
    echo "✗ MESSAGE_SENT does not clear cache"
fi

if grep -A 5 "MESSAGE_RECEIVED" app/src/main/java/com/translator/messagingapp/ConversationActivity.java | grep -q "clearCacheForThread"; then
    echo "✓ MESSAGE_RECEIVED clears cache before loading messages"
else
    echo "✗ MESSAGE_RECEIVED does not clear cache"
fi

# Check that new test was added
echo ""
echo "4. Checking test coverage..."
if [ -f "app/src/test/java/com/translator/messagingapp/MessageDisplayFixTest.java" ]; then
    echo "✓ MessageDisplayFixTest.java created"
else
    echo "✗ MessageDisplayFixTest.java not found"
fi

echo ""
echo "Summary:"
echo "The fix should resolve the issue where sent messages don't appear in the conversation"
echo "and received messages are duplicated after reloading."
echo "Key changes:"
echo "• Sent messages are now stored in SMS database with MESSAGE_TYPE_SENT before broadcast"
echo "• Both MESSAGE_SENT and MESSAGE_RECEIVED broadcasts clear cache to ensure fresh data loading"
echo "• This prevents stale cache data from causing missing sent messages or duplicate received messages"
echo "• New test coverage documents the expected behavior"
echo ""
echo "Expected result: Sent messages should appear immediately, no message duplication on reload."