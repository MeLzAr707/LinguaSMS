# Manual Testing Guide for Issue #359 Fix

## Summary of the Fix

The issue was **NOT** about inconsistent language selection between message bubble and input translation. The original language selection logic was correct:

- **Message bubble translation** (incoming): Translates TO your preferred reading language (`getPreferredLanguage()`)
- **Input translation** (outgoing): Translates TO your preferred sending language (`getPreferredOutgoingLanguage()`)

The real issue was in the **offline translation language detection logic** in `TranslationManager.java`.

## Root Cause

When the system needed to detect the source language for offline translation:

1. **Availability Check**: `shouldUseOfflineTranslation(null, targetLanguage)` would return `false` because source language was null
2. **Fallback**: System would try online language detection instead  
3. **Inconsistency**: Later, when actual translation happened, it might still try offline translation
4. **Result**: "Models may not be loaded correctly" errors due to inconsistent logic paths

## The Fix

Modified `shouldUseOfflineTranslation()` to return `true` when:
- Offline translation is preferred
- Source language is not yet known (null)
- This allows proper offline language detection to be attempted first

## Manual Testing Steps

### Test Scenario 1: Basic Message Translation
1. **Setup**: Ensure you have offline models downloaded for at least one language pair
2. **Test**: Receive a message in a foreign language (or create one in conversation)
3. **Action**: Tap the message to translate it
4. **Expected**: Translation should work without "model loading" errors

### Test Scenario 2: Input Translation  
1. **Setup**: Same offline models as above
2. **Test**: Type text in your native language in the message input
3. **Action**: Tap the translate button (if available)
4. **Expected**: Translation should work consistently with message translation

### Test Scenario 3: Mixed Language Preferences
1. **Setup**: 
   - Set preferred language to English
   - Set preferred outgoing language to Spanish  
   - Download Spanish models but not English models
2. **Test**: 
   - Send Spanish text (input translation): Should work
   - Receive Spanish message (message translation): Should show clear error about English models
3. **Expected**: No confusing "models may not be loaded correctly" errors

### Test Scenario 4: Offline-Preferred Mode
1. **Setup**: 
   - Enable offline translation preference
   - Set translation mode to "Auto" 
   - Have some offline models downloaded
2. **Test**: Translate messages when offline or with poor internet
3. **Expected**: Should attempt offline translation first, with clear fallback behavior

## What to Look For

✅ **Fixed**: No more "models may not be loaded correctly" errors when models are actually available  
✅ **Fixed**: Consistent offline translation behavior between input and message translation  
✅ **Fixed**: Proper offline language detection when preferred  

❌ **Still expected**: Legitimate errors when models are actually not downloaded (but with clear error messages)  
❌ **Still expected**: Different behavior between incoming/outgoing due to different target languages (this is correct)

## Error Messages That Are Now Clear

- ✅ "Language models not downloaded" - when specific models are missing
- ✅ "Could not detect language" - when language detection fails  
- ✅ "No translation service available" - when neither online nor offline are available

## Notes

- The language selection logic remains unchanged and correct
- Input translation uses outgoing language preference
- Message translation uses incoming language preference  
- This difference is intentional and should be preserved