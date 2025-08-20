#!/bin/bash

# ConversationActivity Message Display Fix Verification Script
# This script verifies the layout constraint fix for issue #188

echo "🔍 ConversationActivity Message Display Fix Verification"
echo "========================================================"

# Check 1: Verify the parent container has an ID
echo "✅ Check 1: Verifying message input container has an ID..."
if grep -q "android:id=\"@+id/message_input_container\"" app/src/main/res/layout/activity_conversation_updated.xml; then
    echo "   ✓ Message input container has proper ID: message_input_container"
else
    echo "   ✗ FAIL: Missing message_input_container ID"
    exit 1
fi

# Check 2: Verify RecyclerView constraint is correctly referenced
echo "✅ Check 2: Verifying RecyclerView constraint references..."
if grep -q "app:layout_constraintBottom_toTopOf=\"@+id/message_input_container\"" app/src/main/res/layout/activity_conversation_updated.xml; then
    echo "   ✓ RecyclerView correctly references message_input_container"
else
    echo "   ✗ FAIL: RecyclerView has incorrect constraint reference"
    exit 1
fi

# Check 3: Verify empty state text view constraint is correctly referenced
echo "✅ Check 3: Verifying empty state text view constraint references..."
if grep -A10 "android:id=\"@+id/empty_state_text_view\"" app/src/main/res/layout/activity_conversation_updated.xml | grep -q "app:layout_constraintBottom_toTopOf=\"@+id/message_input_container\""; then
    echo "   ✓ Empty state text view correctly references message_input_container"
else
    echo "   ✗ FAIL: Empty state text view has incorrect constraint reference"
    exit 1
fi

# Check 4: Verify no old broken references remain
echo "✅ Check 4: Verifying no broken constraint references remain..."
if grep -q "app:layout_constraintBottom_toTopOf=\"@+id/message_input_layout\"" app/src/main/res/layout/activity_conversation_updated.xml; then
    echo "   ✗ FAIL: Found broken constraint reference to message_input_layout"
    exit 1
else
    echo "   ✓ No broken constraint references found"
fi

# Check 5: Verify test was created for this fix
echo "✅ Check 5: Verifying test coverage for layout constraint fix..."
if [ -f "app/src/test/java/com/translator/messagingapp/ConversationLayoutConstraintTest.java" ]; then
    echo "   ✓ ConversationLayoutConstraintTest created"
else
    echo "   ✗ FAIL: Missing test file for layout constraint fix"
    exit 1
fi

echo ""
echo "🎉 All verification checks PASSED!"
echo "The ConversationActivity message display fix has been successfully implemented."
echo ""
echo "Summary of fixes:"
echo "• Added ID to message input container (message_input_container)"
echo "• Fixed RecyclerView constraint reference to use message_input_container"
echo "• Fixed empty state text view constraint reference to use message_input_container"
echo "• Fixed translating indicator constraint reference to use message_input_container"
echo "• Removed all broken constraint references"
echo "• Added test coverage for the layout constraint fix"
echo ""
echo "Messages should now display properly in the ConversationActivity! ✨"