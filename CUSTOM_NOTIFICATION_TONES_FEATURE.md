# Custom Notification Tones Per Contact Feature

This document describes the implementation of custom notification tones per contact in LinguaSMS.

## Feature Overview

Users can now assign different notification tones to individual contacts within LinguaSMS. This allows users to easily identify important messages and enhance personalization by hearing different sounds for different people.

## How to Use

### Setting a Custom Notification Tone

1. Open a conversation with the contact you want to customize
2. Tap the menu button (⋮) in the conversation toolbar
3. Select "Contact Settings"
4. Choose "Notification Tone"
5. Select "Select Custom Tone" to open the system ringtone picker
6. Choose your desired notification sound from the available options
7. The custom tone is automatically saved for this contact

### Resetting to Default Tone

1. Follow steps 1-4 above
2. Select "Default Tone" instead
3. The contact will now use the system's default notification sound

### Behavior

- **Custom Tone Set**: When a message arrives from this contact, your selected custom tone will play
- **No Custom Tone**: When no custom tone is set, the system's default notification sound will play
- **Multiple Contacts**: Each contact can have their own unique notification tone
- **Persistent Storage**: Your tone selections are saved and persist across app restarts

## Technical Implementation

### Architecture

The feature consists of several key components:

1. **UserPreferences Enhancement**: Extended to store per-contact notification tones using SharedPreferences
2. **NotificationHelper Updates**: Modified to check for custom tones before playing notifications
3. **ContactSettingsDialog**: New dialog for managing contact-specific settings
4. **ConversationActivity Integration**: Added menu option and activity result handling

### Key Classes Modified

#### UserPreferences.java
- Added `getContactNotificationTone(String contactAddress)`
- Added `setContactNotificationTone(String contactAddress, String toneUri)`
- Added `hasContactNotificationTone(String contactAddress)`
- Implemented phone number normalization for consistent storage

#### NotificationHelper.java
- Added `getNotificationSoundForContact(String contactAddress)`
- Modified notification methods to use contact-specific tones
- Maintained backward compatibility with existing notification behavior

#### ContactSettingsDialog.java
- New class providing UI for contact settings
- Integrates with Android's system ringtone picker
- Handles tone selection and storage

#### ConversationActivity.java
- Added "Contact Settings" menu item
- Implemented OnToneSelectedListener interface
- Added activity result handling for ringtone picker

### Phone Number Normalization

The system normalizes phone numbers to ensure consistent storage and retrieval across different formats:

- `(123) 456-7890` → `1234567890`
- `123-456-7890` → `1234567890` 
- `+11234567890` → `1234567890`
- `11234567890` → `1234567890`

This ensures that the same contact gets the same tone regardless of how their number is formatted in different parts of the system.

### Storage Format

Custom tones are stored in SharedPreferences with the key pattern:
```
contact_notification_tone_[normalized_phone_number] = [tone_uri]
```

Example:
```
contact_notification_tone_1234567890 = content://settings/system/notification_sound
```

## User Experience

### Menu Integration

The feature is seamlessly integrated into the existing conversation interface:
- New "Contact Settings" menu item appears in the conversation menu
- Familiar Android ringtone picker provides consistent UX
- Toast messages confirm tone changes

### Error Handling

The implementation includes robust error handling:
- Invalid URIs are handled gracefully with fallback to default
- Null/empty addresses are safely handled
- Network/storage errors show user-friendly messages

## Testing

Comprehensive unit tests cover:
- Contact tone storage and retrieval
- Phone number normalization accuracy
- Edge cases (null/empty values)
- Multiple contacts with different tones
- Dialog creation and interaction

Test file: `ContactNotificationToneTest.java`

## Backward Compatibility

The feature maintains full backward compatibility:
- Existing notifications continue to work unchanged
- No impact on users who don't use custom tones
- Default behavior preserved when no custom tone is set

## Future Enhancements

Potential future improvements could include:
- Notification tone preview in settings
- Import/export of tone settings
- Group conversation tone customization
- Tone categories or favorites