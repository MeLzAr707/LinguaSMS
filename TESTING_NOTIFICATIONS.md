# Testing Notifications in LinguaSMS

## Quick Test Guide

### Manual Testing with Debug Tool
1. **Build and install** the app on a device or emulator
2. **Grant permissions** when prompted (SMS, Notifications, etc.)
3. **Open the Debug activity**:
   - Open the app
   - Navigate to the Debug/Testing section (if available in the menu)
   - Or use an intent to launch `DebugActivity` directly
4. **Test notifications**:
   - Optionally enter a phone number in the text field
   - Tap the "Test Notification" button
   - You should see an SMS notification appear immediately
   - An MMS notification should appear after 2 seconds

### Testing with Real Messages
1. **Set as default SMS app** (if testing on device):
   - Go to Android Settings > Apps > Default apps > SMS app
   - Select LinguaSMS
2. **Send test messages**:
   - Send an SMS to the device from another phone
   - The notification should appear automatically
   - Tap the notification to open the conversation

### Verification Checklist
- ✅ SMS notification shows sender and message content
- ✅ Notification plays sound
- ✅ Tapping notification opens conversation
- ✅ MMS notification appears for multimedia messages
- ✅ Contact names are shown instead of numbers (when available)
- ✅ Multiple notifications don't crash the app

### Troubleshooting
- **No notifications appear**: Check app permissions in Settings
- **App crashes**: Check logcat for errors in MessageService
- **Notifications don't open conversation**: Verify ConversationActivity intent handling
- **Contact names not showing**: Check contacts permission

### Development Notes
- All notification logic is in `MessageService.handleIncomingSms/MMS()`
- Notification display is handled by existing `NotificationHelper` class
- Testing can be done programmatically using `NotificationTestHelper`
- Debug tools are available in `DebugActivity` for manual testing