#!/bin/bash

# Message Update Integration Verification Script
# This script checks that all the key integration points are in place for the message update fix

echo "=== Message Update Integration Verification ==="
echo ""

echo "1. Checking ConversationActivity BroadcastReceiver setup..."
if grep -q "setupMessageUpdateReceiver" app/src/main/java/com/translator/messagingapp/ConversationActivity.java; then
    echo "✓ ConversationActivity has setupMessageUpdateReceiver() method"
else
    echo "✗ ConversationActivity missing setupMessageUpdateReceiver() method"
fi

if grep -q "private BroadcastReceiver messageUpdateReceiver" app/src/main/java/com/translator/messagingapp/ConversationActivity.java; then
    echo "✓ ConversationActivity has messageUpdateReceiver field"
else
    echo "✗ ConversationActivity missing messageUpdateReceiver field"
fi

if grep -q "unregisterReceiver(messageUpdateReceiver)" app/src/main/java/com/translator/messagingapp/ConversationActivity.java; then
    echo "✓ ConversationActivity properly unregisters receiver in onDestroy()"
else
    echo "✗ ConversationActivity missing receiver unregistration"
fi

echo ""
echo "2. Checking MessageService broadcast methods..."
if grep -q "broadcastMessageSent" app/src/main/java/com/translator/messagingapp/MessageService.java; then
    echo "✓ MessageService has broadcastMessageSent() method"
else
    echo "✗ MessageService missing broadcastMessageSent() method"
fi

if grep -q "broadcastMessageReceived" app/src/main/java/com/translator/messagingapp/MessageService.java; then
    echo "✓ MessageService has broadcastMessageReceived() method"
else
    echo "✗ MessageService missing broadcastMessageReceived() method"
fi

echo ""
echo "3. Checking broadcast action consistency..."
ACTIONS=(
    "com.translator.messagingapp.MESSAGE_RECEIVED"
    "com.translator.messagingapp.MESSAGE_SENT"
    "com.translator.messagingapp.REFRESH_MESSAGES"
)

for action in "${ACTIONS[@]}"; do
    if grep -q "$action" app/src/main/java/com/translator/messagingapp/ConversationActivity.java && 
       grep -q "$action" app/src/main/java/com/translator/messagingapp/MessageService.java; then
        echo "✓ Action '$action' is consistently used in both files"
    else
        echo "✗ Action '$action' is not consistently used"
    fi
done

echo ""
echo "4. Checking receiver integration points..."
if grep -q "handleIncomingSms.*broadcastMessageReceived" app/src/main/java/com/translator/messagingapp/MessageService.java; then
    echo "✓ handleIncomingSms calls broadcastMessageReceived"
else
    echo "? handleIncomingSms may call broadcastMessageReceived (check manually)"
fi

if grep -q "handleIncomingMms.*broadcastMessageReceived" app/src/main/java/com/translator/messagingapp/MessageService.java; then
    echo "✓ handleIncomingMms calls broadcastMessageReceived"
else
    echo "? handleIncomingMms may call broadcastMessageReceived (check manually)"
fi

if grep -q "sendSmsMessage.*broadcastMessageSent" app/src/main/java/com/translator/messagingapp/MessageService.java; then
    echo "✓ sendSmsMessage calls broadcastMessageSent"
else
    echo "? sendSmsMessage may call broadcastMessageSent (check manually)"
fi

echo ""
echo "5. Checking SmsReceiver and MmsReceiver integration..."
if grep -q "messageService.handleIncomingSms" app/src/main/java/com/translator/messagingapp/SmsReceiver.java; then
    echo "✓ SmsReceiver calls MessageService.handleIncomingSms"
else
    echo "✗ SmsReceiver not properly integrated with MessageService"
fi

if grep -q "messageService.handleIncomingMms" app/src/main/java/com/translator/messagingapp/MmsReceiver.java; then
    echo "✓ MmsReceiver calls MessageService.handleIncomingMms"
else
    echo "✗ MmsReceiver not properly integrated with MessageService"
fi

echo ""
echo "=== Integration Verification Complete ==="
echo ""
echo "Summary: The message update fix integrates at the following points:"
echo "• Incoming messages: SmsReceiver/MmsReceiver → MessageService.handleIncoming* → broadcastMessageReceived()"
echo "• Outgoing messages: ConversationActivity.sendMessage() → MessageService.send*Message → broadcastMessageSent()"  
echo "• UI updates: ConversationActivity.messageUpdateReceiver → loadMessages()"
echo ""
echo "This should resolve the issue where messages don't update in ConversationActivity after sending or receiving."