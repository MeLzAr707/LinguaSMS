#!/bin/bash

# Script to validate the incoming language translation fix

echo "🔍 Validating Incoming Language Translation Fix"
echo "=============================================="

# Check that the fix was applied correctly
echo "1. Checking TranslationManager.translateSmsMessage() uses correct preference method..."

# Look for the corrected line
if grep -q "String targetLanguage = userPreferences.getPreferredIncomingLanguage();" app/src/main/java/com/translator/messagingapp/TranslationManager.java; then
    echo "   ✅ PASS: translateSmsMessage() now uses getPreferredIncomingLanguage()"
else
    echo "   ❌ FAIL: translateSmsMessage() still uses wrong method"
    exit 1
fi

# Check that no other incorrect usages were introduced
echo "2. Checking that translateMessage() already has correct logic..."

if grep -A 3 "message.isIncoming()" app/src/main/java/com/translator/messagingapp/TranslationManager.java | grep -q "getPreferredIncomingLanguage"; then
    echo "   ✅ PASS: translateMessage() correctly uses incoming/outgoing preferences"
else
    echo "   ❌ FAIL: translateMessage() logic may be broken"
    exit 1
fi

# Check that the test was created
echo "3. Checking that test file was created..."

if [ -f "app/src/test/java/com/translator/messagingapp/IncomingLanguageTranslationTest.java" ]; then
    echo "   ✅ PASS: IncomingLanguageTranslationTest.java created"
else
    echo "   ❌ FAIL: Test file not found"
    exit 1
fi

# Check test content
echo "4. Checking test verifies correct behavior..."

if grep -q "getPreferredIncomingLanguage" app/src/test/java/com/translator/messagingapp/IncomingLanguageTranslationTest.java; then
    echo "   ✅ PASS: Test verifies getPreferredIncomingLanguage() usage"
else
    echo "   ❌ FAIL: Test doesn't verify correct method"
    exit 1
fi

if grep -q "translate.*en.*zh" app/src/test/java/com/translator/messagingapp/IncomingLanguageTranslationTest.java; then
    echo "   ✅ PASS: Test verifies English to Chinese translation"
else
    echo "   ❌ FAIL: Test doesn't verify Chinese translation scenario"
    exit 1
fi

# Check that we don't translate en->en anymore
if grep -q "never.*translate.*en.*en" app/src/test/java/com/translator/messagingapp/IncomingLanguageTranslationTest.java; then
    echo "   ✅ PASS: Test verifies no English to English translation"
else
    echo "   ❌ FAIL: Test doesn't prevent English to English translation"
    exit 1
fi

echo ""
echo "🎉 All validation checks PASSED!"
echo ""
echo "Summary of fix:"
echo "- Issue: Chinese selected but translating English to English"
echo "- Root cause: translateSmsMessage() used getPreferredLanguage() instead of getPreferredIncomingLanguage()"
echo "- Fix: Changed to use getPreferredIncomingLanguage() for incoming SMS messages"
echo "- Expected result: English messages now translate to user's selected incoming language (e.g., Chinese)"
echo ""
echo "This minimal fix addresses the exact issue described in #530."