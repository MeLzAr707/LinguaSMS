# Manual Testing Guide for Message Display Fix

## Issue Description
After merging changes from copilot/fix-302, incoming text messages were no longer visible to users, even after leaving and returning to the thread. Translation was interfering with message display.

## Testing the Fix

### 1. Basic Message Display Test
**Steps:**
1. Send an SMS to the device
2. Open the conversation in LinguaSMS
3. Verify the incoming message is immediately visible
4. Leave and return to the conversation
5. Verify the message is still visible

**Expected Result:** Message should display immediately with original text, no blank or missing messages.

### 2. Translation Functionality Test
**Steps:**
1. Send an SMS to the device
2. Verify message displays original text first
3. Tap the translate button on the message
4. Verify translation appears with both original and translated text
5. Tap translate button again to toggle back to original
6. Verify original text displays correctly

**Expected Result:** Translation should work as a secondary feature, not interfere with initial display.

### 3. Auto-Translation Test (if enabled)
**Steps:**
1. Enable auto-translate in settings
2. Set preferred language different from message language
3. Send an SMS in different language
4. Verify message displays original text immediately
5. If auto-translation triggers, verify it happens in background without hiding message

**Expected Result:** Message should be visible immediately, translation should enhance not replace.

### 4. Cache Corruption Recovery Test
**Steps:**
1. Clear app data/cache
2. Send several messages and translate them
3. Force-close the app
4. Manually corrupt translation cache if possible (or simulate by clearing partial cache)
5. Reopen app and navigate to conversation
6. Verify all messages are visible with original text

**Expected Result:** Messages should display original text even if translation cache is corrupted.

### 5. Empty Translation State Test
**Scenario:** Test recovery from corrupted translation state
1. Create a message that might have empty translation state
2. Verify it displays original text instead of blank
3. Try translating it fresh
4. Verify translation works correctly

**Expected Result:** No blank messages, always fallback to original text.

## Key Behaviors to Verify

### ✅ MUST WORK
- [ ] All incoming messages are immediately visible
- [ ] Messages display original text by default
- [ ] Leaving and returning to conversation shows all messages
- [ ] Manual translation still works correctly
- [ ] Toggle between original and translated text works
- [ ] No blank or missing messages in any scenario

### ✅ SHOULD WORK
- [ ] Auto-translation (if enabled) doesn't block initial display
- [ ] App handles corrupted translation cache gracefully
- [ ] Performance is not degraded
- [ ] Translation state persists correctly when valid

### ❌ MUST NOT HAPPEN
- [ ] Messages should never be invisible due to translation issues
- [ ] Empty/blank message bubbles should not appear
- [ ] Translation should never block initial message display
- [ ] App should not crash due to translation state issues

## Verification Points

1. **Immediate Visibility**: Every message should be visible as soon as it's received
2. **Original Text Priority**: Original text should always be the fallback display
3. **Translation as Enhancement**: Translation should enhance, not replace or block original content
4. **Graceful Degradation**: If translation fails/corrupts, original text should always show
5. **No UI Blocking**: Translation processing should never block the UI or hide messages

## Edge Cases to Test

1. Very long messages
2. Messages with special characters/emojis
3. Messages in unsupported languages
4. Rapid message arrival
5. Poor network conditions during translation
6. App restart scenarios
7. Low memory conditions

## Success Criteria

The fix is successful if:
1. ✅ No incoming messages are missing or invisible
2. ✅ All messages display their original content immediately
3. ✅ Translation works as a secondary feature without interference
4. ✅ App gracefully handles all edge cases and corrupted states
5. ✅ No regression in existing translation functionality