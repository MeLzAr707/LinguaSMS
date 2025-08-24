# Manual Testing Guide: Non-English Translation Fix

This guide helps verify that the translation fixes work correctly for the issues described in #361.

## Test Environment Setup
1. Ensure you have the app installed with the latest changes
2. Verify offline translation models are downloaded (Settings > Translation > Offline Models)
3. Set up different language preferences for testing

## Test Cases

### Test 1: Non-English Outgoing Text Translation
**Issue**: Non-English input not translated in textbox

**Steps**:
1. Open NewMessageActivity or ConversationActivity
2. Set outgoing language to Spanish (es) in settings
3. Type non-English text in input box: 
   - "Bonjour" (French)
   - "Hola" (Spanish)
   - "Guten Tag" (German)
4. Click translate button

**Expected Result**:
- Text should be translated to Spanish
- Should NOT see "Translation returned identical text" error
- Logcat should show proper language detection (not "en -> en")

**Before Fix**: Only English input was translated
**After Fix**: All supported languages should translate properly

### Test 2: Chat Bubble Translation (Incoming Messages)
**Issue**: Chat bubbles don't translate incoming messages

**Steps**:
1. Open a conversation with existing messages
2. Set incoming language to English (en) in settings
3. Find a message in another language (or send one from another device)
4. Tap the translate button on the message bubble

**Expected Result**:
- Message should translate successfully
- Should work even if detected language matches target language
- Should NOT see "already in [language]" error message

**Before Fix**: Translation failed with "Translation returned identical text"
**After Fix**: Force translation should work for all messages

### Test 3: Offline Translation Language Detection
**Issue**: Offline translation defaulted to English detection

**Steps**:
1. Disable online translation (remove API key or set offline-only mode)
2. Ensure multiple language models are downloaded (en, es, fr, de)
3. Try translating text in different languages:
   - "Merci" (French) -> English
   - "Gracias" (Spanish) -> English
   - "Danke" (German) -> English

**Expected Result**:
- Should properly infer source language based on available models
- Should translate successfully without assuming source is English
- Logcat should show inferred language: "Inferred source language for offline translation: fr"

**Before Fix**: Always assumed source was English ("en")
**After Fix**: Intelligently infers source language from available models

### Test 4: Error Handling Improvements
**Steps**:
1. Try translating with limited offline models
2. Attempt translation with unsupported language pairs
3. Check error messages in UI and logcat

**Expected Result**:
- Clear error messages: "Could not infer source language for offline translation"
- Proper fallback behavior
- No generic "Translation failed" messages

## Logcat Monitoring

Monitor these log tags while testing:
```bash
adb logcat | grep -E "(TranslationManager|OfflineTranslationService)"
```

### Expected Log Messages (After Fix):
```
TranslationManager: Inferred source language for offline translation: fr
OfflineTranslationService: Offline translation successful: 'Bonjour' -> 'Hello'
OfflineTranslationService: Models found in internal tracking: fr -> en
```

### Problematic Log Messages (Should NOT see):
```
OfflineTranslationService: Models found in internal tracking: en -> en
OfflineTranslationService: Translation returned identical text for 'Hello', likely model issue
```

## Verification Checklist

- [ ] Non-English text in textbox gets translated properly
- [ ] Chat bubble translation works for incoming messages  
- [ ] Offline translation infers correct source language
- [ ] No "Translation returned identical text" errors
- [ ] Proper error messages when translation fails
- [ ] Force translation works even when languages appear to match
- [ ] Online language detection used when available, even for offline translation
- [ ] Fallback to online translation works when offline fails

## Common Issues to Check

1. **API Key Configuration**: Ensure Google Translate API key is set for online detection
2. **Model Downloads**: Verify required language models are actually downloaded
3. **Permissions**: Check app has necessary permissions for network and storage
4. **Language Settings**: Confirm preferred languages are set correctly in settings

## Success Criteria

The fix is successful if:
1. All non-English input gets translated (not just English)
2. Chat bubble translation works consistently
3. Offline translation properly detects source languages
4. No "identical text" errors in normal usage
5. Better user experience with clear error messages