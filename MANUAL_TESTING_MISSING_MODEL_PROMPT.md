# Manual Testing Guide: Missing Language Model Download Prompt

## Overview
This guide helps verify that the missing language model download prompt is working correctly after the fix.

## Prerequisites
- Android device or emulator
- LinguaSMS app installed
- No language models downloaded (or specific models removed for testing)

## Test Scenarios

### Test 1: Manual Translation in ConversationActivity

**Setup:**
1. Ensure Spanish language model is NOT downloaded
2. Set user preference to Auto or Offline translation mode
3. Open a conversation

**Steps:**
1. Tap on a message to translate it
2. Select Spanish as target language
3. Observe the behavior

**Expected Result:**
- Dialog appears: "Download Language Model"
- Message explains need to download Spanish language models
- Shows [Cancel] and [Download] buttons
- If Download selected: Progress dialog shows download
- After download: Translation completes automatically

**Previous Behavior (Bug):**
- No prompt shown
- Translation would fail silently or show confusing error

### Test 2: Composing New Message Translation

**Setup:**
1. Ensure French language model is NOT downloaded
2. Set translation mode to Auto or Offline
3. Open NewMessageActivity

**Steps:**
1. Type a message in English
2. Tap translate button
3. Select French as target language

**Expected Result:**
- Download prompt appears for French models
- User can choose to download or cancel
- If downloaded, message translates automatically

### Test 3: Background Translation (Graceful Handling)

**Setup:**
1. Ensure German language model is NOT downloaded
2. Enable auto-translation in settings
3. Send SMS from another device

**Steps:**
1. Receive SMS in foreign language
2. Background translation attempts to process it

**Expected Result:**
- Background process detects missing models
- Fails gracefully without infinite retries
- No crash or excessive resource usage
- Falls back to online translation if available

**Previous Behavior (Bug):**
- Background worker would retry indefinitely
- Could cause performance issues

### Test 4: Translation Mode Verification

**Test Different Modes:**

#### Online Mode
1. Set translation mode to "Online Only"
2. Attempt translation with missing models
3. Should skip offline translation entirely
4. Should use online translation without prompting

#### Offline Mode  
1. Set translation mode to "Offline Only"
2. Attempt translation with missing models
3. Should prompt to download models
4. Should not fall back to online if user cancels

#### Auto Mode (Default)
1. Set translation mode to "Auto"
2. Attempt translation with missing models  
3. Should prompt to download models
4. Should fall back to online if user cancels (if API key available)

## Verification Points

### UI Elements
- [ ] Dialog title: "Download Language Model"
- [ ] Message mentions specific language names
- [ ] Warning about mobile data usage
- [ ] Cancel and Download buttons present
- [ ] Progress dialog shows download progress
- [ ] Progress percentage updates in real-time

### Functional Behavior
- [ ] Translation retries automatically after download
- [ ] User can cancel download without issues
- [ ] Multiple missing models handled correctly
- [ ] Activity lifecycle respected (no crashes on rotation)
- [ ] Background processes don't show UI dialogs

### Error Handling
- [ ] Download failures handled gracefully
- [ ] Network issues don't crash the app
- [ ] Invalid language codes handled properly
- [ ] Activity destruction during download handled

## Testing Tips

1. **Clear Model Cache**: Uninstall/reinstall app to ensure no models are cached
2. **Network Conditions**: Test on WiFi and mobile data
3. **Language Variety**: Test with different language pairs
4. **Interruption Testing**: Rotate device during download, background app, etc.
5. **Permission Testing**: Ensure network permissions are granted

## Common Issues to Watch For

- Dialog not appearing (main bug this fix addresses)
- Download progress not updating
- App crash during model download
- Translation not retrying after successful download
- Background translation showing UI dialogs (inappropriate)
- Infinite retry loops in background processes

## Success Criteria

✅ **Primary Goal**: Users are prompted to download missing language models when attempting translation
✅ **Secondary Goal**: Background processes handle missing models gracefully
✅ **Tertiary Goal**: All existing functionality continues to work

## Logs to Check

When testing, monitor logs for these key messages:
- "Models available for [lang1] -> [lang2]: false"
- "Language models not downloaded for [lang1] -> [lang2]"
- "Prompting user to download missing models"
- "Models downloaded successfully, retrying translation"
- "Translation failed due to missing language models - background workers cannot prompt"

These logs confirm the fix is working as intended.