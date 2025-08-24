# Manual Testing Guide for Issue #359 Fix

## Issue Description
Message bubble translation failure with error toast saying "models may not be loaded correctly" while text input translation succeeds.

## Root Cause Fixed
The issue was caused by inconsistent language selection between message bubble translation and text input translation:
- Text input used: `getPreferredOutgoingLanguage() ?? getPreferredLanguage()`
- Message bubble used: `getPreferredLanguage()` only

This caused different offline model availability checks, leading to failures.

## Fix Applied
Modified `translateMessage()` method in ConversationActivity.java to use the same language selection logic as `translateInput()` method.

## Manual Testing Steps

### Setup
1. Set up different language preferences:
   - Go to Settings > Language Preferences
   - Set "Preferred Language" to English (en)
   - Set "Preferred Outgoing Language" to Spanish (es)

2. Download offline models:
   - Go to Settings > Manage Offline Models
   - Download Spanish language model
   - Do NOT download English model (to test the specific scenario)

3. Set translation mode to "Auto" or "Offline Only"

### Test Scenario 1: Text Input Translation
1. Open any conversation
2. Type "Hello world" in the message input
3. Tap the translate button (🌐) next to the input
4. **Expected**: Translation succeeds to Spanish
5. **Verify**: No error about models not being loaded

### Test Scenario 2: Message Bubble Translation  
1. In the same conversation, find any English message
2. Tap the translate button on the message bubble
3. **Expected**: Translation succeeds to Spanish (same as input)
4. **Verify**: No error about models not being loaded
5. **Verify**: Uses the same target language as text input translation

### Test Scenario 3: Consistency Check
1. Both translation methods should now:
   - Use Spanish as target language (from preferredOutgoingLanguage)
   - Successfully translate using available Spanish offline models
   - NOT attempt English translation (which would fail due to missing models)

### Test Scenario 4: Fallback Behavior
1. Clear the "Preferred Outgoing Language" setting (set to empty/null)
2. Keep "Preferred Language" as English
3. Ensure English offline models are available
4. Test both translation methods
5. **Expected**: Both should use English as target language

## Before vs After Behavior

### Before Fix
- Text input translation: ✅ Works (uses Spanish models)
- Message bubble translation: ❌ Fails with "models may not be loaded correctly" (tries English models)

### After Fix
- Text input translation: ✅ Works (uses Spanish models)
- Message bubble translation: ✅ Works (uses Spanish models, same as input)

## Success Criteria
1. ✅ Both translation methods use identical target language selection
2. ✅ No more "models may not be loaded correctly" errors when models are actually available
3. ✅ Consistent behavior between text input and message bubble translation
4. ✅ Backwards compatibility maintained for users without outgoing language preference

## Code Changes Summary
- File: `app/src/main/java/com/translator/messagingapp/ConversationActivity.java`
- Method: `translateMessage()` (around line 720)
- Change: Added same language selection logic as `translateInput()` method
- Lines added: 4 lines for consistent language selection
- Impact: Minimal, surgical fix addressing only the specific inconsistency