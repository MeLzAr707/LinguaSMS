#!/bin/bash

# Script to verify button functionality fixes for issue #515

echo "=== Verifying Button Functionality Fixes ==="
echo

# Check 1: ConversationActivity attach button ID in layout
echo "1. Checking ConversationActivity attach button ID..."
if grep -q 'android:id="@+id/attachment_button"' app/src/main/res/layout/activity_conversation_updated.xml; then
    echo "   ✓ Attachment button ID added to layout"
else
    echo "   ✗ Attachment button ID missing from layout"
fi

# Check 2: ConversationActivity attach button findViewById
echo "2. Checking ConversationActivity attach button initialization..."
if grep -q 'attachmentButton = findViewById(R.id.attachment_button)' app/src/main/java/com/translator/messagingapp/ConversationActivity.java; then
    echo "   ✓ Attachment button findViewById added"
else
    echo "   ✗ Attachment button findViewById missing"
fi

# Check 3: ConversationActivity attach button click listener
echo "3. Checking ConversationActivity attach button click listener..."
if grep -q 'attachmentButton.setOnClickListener.*openAttachmentPicker' app/src/main/java/com/translator/messagingapp/ConversationActivity.java; then
    echo "   ✓ Attachment button click listener added"
else
    echo "   ✗ Attachment button click listener missing"
fi

# Check 4: Menu items have IDs
echo "4. Checking menu items have IDs..."
MENU_IDS_COUNT=$(grep -c 'android:id="@+id/action_' app/src/main/res/menu/conversation_menu.xml)
if [ "$MENU_IDS_COUNT" -ge 4 ]; then
    echo "   ✓ Menu items have proper IDs ($MENU_IDS_COUNT found)"
else
    echo "   ✗ Menu items missing IDs (only $MENU_IDS_COUNT found)"
fi

# Check 5: Menu handling in onOptionsItemSelected
echo "5. Checking menu handling in onOptionsItemSelected..."
MENU_HANDLERS=$(grep -c 'R.id.action_' app/src/main/java/com/translator/messagingapp/ConversationActivity.java)
if [ "$MENU_HANDLERS" -ge 4 ]; then
    echo "   ✓ Menu items are handled in onOptionsItemSelected ($MENU_HANDLERS handlers found)"
else
    echo "   ✗ Menu items not properly handled (only $MENU_HANDLERS handlers found)"
fi

# Check 6: Call functionality methods
echo "6. Checking call functionality..."
if grep -q 'private void makePhoneCall()' app/src/main/java/com/translator/messagingapp/ConversationActivity.java; then
    echo "   ✓ makePhoneCall method implemented"
else
    echo "   ✗ makePhoneCall method missing"
fi

# Check 7: Delete conversation functionality
echo "7. Checking delete conversation functionality..."
if grep -q 'private void showDeleteConversationDialog()' app/src/main/java/com/translator/messagingapp/ConversationActivity.java; then
    echo "   ✓ showDeleteConversationDialog method implemented"
else
    echo "   ✗ showDeleteConversationDialog method missing"
fi

# Check 8: Translate all messages functionality
echo "8. Checking translate all messages functionality..."
if grep -q 'private void translateAllMessages()' app/src/main/java/com/translator/messagingapp/ConversationActivity.java; then
    echo "   ✓ translateAllMessages method implemented"
else
    echo "   ✗ translateAllMessages method missing"
fi

# Check 9: NewMessageActivity attach button
echo "9. Checking NewMessageActivity attach button..."
if grep -q 'android:id="@+id/attachment_button"' app/src/main/res/layout/activity_new_message.xml; then
    echo "   ✓ Attach button added to NewMessageActivity layout"
else
    echo "   ✗ Attach button missing from NewMessageActivity layout"
fi

# Check 10: NewMessageActivity attach button initialization
echo "10. Checking NewMessageActivity attach button initialization..."
if grep -q 'attachmentButton = findViewById(R.id.attachment_button)' app/src/main/java/com/translator/messagingapp/NewMessageActivity.java; then
    echo "    ✓ Attach button findViewById added to NewMessageActivity"
else
    echo "    ✗ Attach button findViewById missing from NewMessageActivity"
fi

# Check 11: CALL_PHONE permission
echo "11. Checking CALL_PHONE permission..."
if grep -q 'android.permission.CALL_PHONE' app/src/main/AndroidManifest.xml; then
    echo "    ✓ CALL_PHONE permission added to manifest"
else
    echo "    ✗ CALL_PHONE permission missing from manifest"
fi

# Check 12: onActivityResult methods for attachment handling
echo "12. Checking attachment handling in onActivityResult..."
CONV_ACTIVITY_COUNT=$(grep -c 'ATTACHMENT_PICK_REQUEST' app/src/main/java/com/translator/messagingapp/ConversationActivity.java)
NEW_MSG_COUNT=$(grep -c 'ATTACHMENT_PICK_REQUEST' app/src/main/java/com/translator/messagingapp/NewMessageActivity.java)
ACTIVITY_RESULT_COUNT=$((CONV_ACTIVITY_COUNT + NEW_MSG_COUNT))
if [ "$ACTIVITY_RESULT_COUNT" -ge 4 ]; then
    echo "    ✓ Attachment handling added to onActivityResult methods ($ACTIVITY_RESULT_COUNT references found)"
else
    echo "    ✗ Attachment handling incomplete (only $ACTIVITY_RESULT_COUNT references found)"
fi

echo
echo "=== Summary ==="
TOTAL_CHECKS=12
PASSED_CHECKS=0

# Count passed checks by re-running the checks
if grep -q 'android:id="@+id/attachment_button"' app/src/main/res/layout/activity_conversation_updated.xml; then ((PASSED_CHECKS++)); fi
if grep -q 'attachmentButton = findViewById(R.id.attachment_button)' app/src/main/java/com/translator/messagingapp/ConversationActivity.java; then ((PASSED_CHECKS++)); fi
if grep -q 'attachmentButton.setOnClickListener.*openAttachmentPicker' app/src/main/java/com/translator/messagingapp/ConversationActivity.java; then ((PASSED_CHECKS++)); fi
MENU_IDS_COUNT=$(grep -c 'android:id="@+id/action_' app/src/main/res/menu/conversation_menu.xml)
if [ "$MENU_IDS_COUNT" -ge 4 ]; then ((PASSED_CHECKS++)); fi
MENU_HANDLERS=$(grep -c 'R.id.action_' app/src/main/java/com/translator/messagingapp/ConversationActivity.java)
if [ "$MENU_HANDLERS" -ge 4 ]; then ((PASSED_CHECKS++)); fi
if grep -q 'private void makePhoneCall()' app/src/main/java/com/translator/messagingapp/ConversationActivity.java; then ((PASSED_CHECKS++)); fi
if grep -q 'private void showDeleteConversationDialog()' app/src/main/java/com/translator/messagingapp/ConversationActivity.java; then ((PASSED_CHECKS++)); fi
if grep -q 'private void translateAllMessages()' app/src/main/java/com/translator/messagingapp/ConversationActivity.java; then ((PASSED_CHECKS++)); fi
if grep -q 'android:id="@+id/attachment_button"' app/src/main/res/layout/activity_new_message.xml; then ((PASSED_CHECKS++)); fi
if grep -q 'attachmentButton = findViewById(R.id.attachment_button)' app/src/main/java/com/translator/messagingapp/NewMessageActivity.java; then ((PASSED_CHECKS++)); fi
if grep -q 'android.permission.CALL_PHONE' app/src/main/AndroidManifest.xml; then ((PASSED_CHECKS++)); fi
CONV_ACTIVITY_COUNT=$(grep -c 'ATTACHMENT_PICK_REQUEST' app/src/main/java/com/translator/messagingapp/ConversationActivity.java)
NEW_MSG_COUNT=$(grep -c 'ATTACHMENT_PICK_REQUEST' app/src/main/java/com/translator/messagingapp/NewMessageActivity.java)
ACTIVITY_RESULT_COUNT=$((CONV_ACTIVITY_COUNT + NEW_MSG_COUNT))
if [ "$ACTIVITY_RESULT_COUNT" -ge 4 ]; then ((PASSED_CHECKS++)); fi

echo "Passed: $PASSED_CHECKS/$TOTAL_CHECKS checks"

if [ "$PASSED_CHECKS" -eq "$TOTAL_CHECKS" ]; then
    echo "🎉 All button functionality fixes have been successfully implemented!"
    exit 0
else
    echo "⚠️  Some checks failed. Please review the implementation."
    exit 1
fi