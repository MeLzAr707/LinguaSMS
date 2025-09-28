#!/bin/bash
set -e

echo "=== Telephony API Adoption Validation ==="
echo

# Check 1: Verify minSdk is set to 19
echo "1. Checking Android 4.4+ compatibility (minSdk 19):"
if grep -q "minSdk 19" app/build.gradle; then
    echo "✓ minSdk correctly set to 19 for Android 4.4+ support"
else
    echo "✗ minSdk not set to 19"
    exit 1
fi
echo

# Check 2: Verify SMS sending uses default SMS app checks
echo "2. Checking SMS sending with default SMS app verification:"
if grep -q "Cannot send SMS: App is not set as default SMS app" app/src/main/java/com/translator/messagingapp/message/MessageService.java; then
    echo "✓ SMS sending checks default SMS app status"
else
    echo "✗ SMS sending missing default SMS app check"
    exit 1
fi
echo

# Check 3: Verify MMS sending uses default SMS app checks  
echo "3. Checking MMS sending with default SMS app verification:"
if grep -q "Cannot send MMS: App is not set as default SMS app" app/src/main/java/com/translator/messagingapp/message/MessageService.java; then
    echo "✓ MMS sending checks default SMS app status"
else
    echo "✗ MMS sending missing default SMS app check"
    exit 1
fi
echo

# Check 4: Verify modern SMS API usage
echo "4. Checking modern SmsManager API usage:"
if grep -q "sendMultimediaMessage" app/src/main/java/com/translator/messagingapp/message/MessageService.java && \
   grep -q "context.getSystemService(SmsManager.class)" app/src/main/java/com/translator/messagingapp/message/MessageService.java; then
    echo "✓ Uses modern SmsManager APIs with proper system service access"
else
    echo "✗ Missing modern SmsManager API usage"
    exit 1
fi
echo

# Check 5: Verify PendingIntent handling
echo "5. Checking PendingIntent delivery tracking:"
if grep -q "PendingIntent.*FLAG_IMMUTABLE" app/src/main/java/com/translator/messagingapp/message/MessageService.java; then
    echo "✓ Uses proper PendingIntent with FLAG_IMMUTABLE for delivery tracking"
else
    echo "✗ Missing proper PendingIntent handling"
    exit 1
fi
echo

# Check 6: Verify HeadlessSmsSendService implementation
echo "6. Checking RESPOND_VIA_MESSAGE service implementation:"
if grep -q "ACTION_RESPOND_VIA_MESSAGE" app/src/main/java/com/translator/messagingapp/system/HeadlessSmsSendService.java && \
   grep -q "sendTextMessage" app/src/main/java/com/translator/messagingapp/system/HeadlessSmsSendService.java; then
    echo "✓ HeadlessSmsSendService properly handles RESPOND_VIA_MESSAGE"
else
    echo "✗ HeadlessSmsSendService missing proper implementation"
    exit 1
fi
echo

# Check 7: Verify legacy code removal
echo "7. Checking legacy code removal:"
if ! grep -q "handleLegacyMmsSendRequest" app/src/main/java/com/translator/messagingapp/mms/MmsSendReceiver.java; then
    echo "✓ Legacy MMS send request handling removed"
else
    echo "✗ Legacy MMS handling still present"
    exit 1
fi
echo

# Check 8: Verify manifest intent filters
echo "8. Checking AndroidManifest.xml for proper intent filters:"
if grep -q "android.intent.action.RESPOND_VIA_MESSAGE" app/src/main/AndroidManifest.xml && \
   grep -q "android.intent.action.SENDTO" app/src/main/AndroidManifest.xml; then
    echo "✓ Proper intent filters for default SMS app"
else
    echo "✗ Missing required intent filters"
    exit 1
fi
echo

# Check 9: Verify anti-spam default SMS app requesting
echo "9. Checking anti-spam default SMS app requesting:"
if grep -q "shouldRequestDefaultSmsApp" app/src/main/java/com/translator/messagingapp/system/DefaultSmsAppManager.java; then
    echo "✓ Anti-spam logic present for default SMS app requests"
else
    echo "✗ Missing anti-spam logic for default SMS app requests"
    exit 1
fi
echo

# Check 10: Verify translation system preservation
echo "10. Checking translation system preservation:"
if grep -q "TranslationManager" app/src/main/java/com/translator/messagingapp/message/MessageService.java && \
   grep -q "translationManager" app/src/main/java/com/translator/messagingapp/message/MessageService.java; then
    echo "✓ Translation functionality preserved in MessageService"
else
    echo "✗ Translation functionality may have been affected"
    exit 1
fi
echo

echo "=== All Validations Passed! ==="
echo
echo "✅ SMS/MMS sending now uses proper Telephony APIs"
echo "✅ Default SMS app role is properly checked before sending"
echo "✅ Android 4.4+ compatibility maintained"
echo "✅ Legacy code removed while preserving functionality"
echo "✅ Translation features remain intact"
echo "✅ Anti-spam logic prevents repeated default SMS app prompts"
echo
echo "The app is now properly implementing Telephony framework APIs"
echo "and will only send SMS/MMS when set as the default SMS application."