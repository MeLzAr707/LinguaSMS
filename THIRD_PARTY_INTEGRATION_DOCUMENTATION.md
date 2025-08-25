# Third-Party Service Integration Enhancements

This document describes the implementation of third-party service integration enhancements for the LinguaSMS messaging application, addressing issue #373.

## Overview

The implementation adds three major third-party service integrations:

1. **Calendar Integration** - Detect and create calendar events from message content
2. **Map Services** - Improved location handling and map previews  
3. **Contact Synchronization** - Enhanced cross-platform contact sync

## Features Implemented

### 1. Calendar Integration (`CalendarIntegrationHelper.java`)

#### Key Features:
- **Smart Date Detection**: Recognizes various date formats including:
  - Numeric formats: `12/25/2024`, `2024-01-15` 
  - Text formats: `January 15, 2024`, `tomorrow`, `Friday`
  - Relative dates: `today`, `tomorrow`, `yesterday`

- **Time Recognition**: Detects time patterns like:
  - `2:00 PM`, `14:30`, `at 3 PM`
  - Various formats with/without AM/PM

- **Event Keywords**: Identifies meeting-related terms:
  - `meeting`, `appointment`, `conference`, `call`
  - `scheduled for`, `meet at`, `reminder`

- **Calendar Event Creation**: Generates Android Calendar intents with:
  - Event title extracted from message
  - Start/end times (defaults to 1-hour duration)
  - Location information if detected
  - Full message as description

#### Usage Example:
```java
// Detect calendar event in message
CalendarIntegrationHelper.CalendarEventInfo eventInfo = 
    CalendarIntegrationHelper.detectCalendarEvent("Team meeting tomorrow at 2:00 PM");

if (eventInfo.isValidEvent()) {
    Intent calendarIntent = CalendarIntegrationHelper.createCalendarEventIntent(eventInfo);
    context.startActivity(calendarIntent);
}
```

### 2. Location Detection and Map Services (`LocationDetectionHelper.java`)

#### Key Features:
- **Multiple Location Types**:
  - Street addresses: `123 Main Street, New York, NY`
  - GPS coordinates: `37.7749, -122.4194`
  - Landmarks: `Starbucks`, `Central Park`, `Airport Terminal 2`
  - Shared locations: `shared my location`, `I'm at`

- **Address Pattern Recognition**:
  - Street addresses with numbers and street types
  - City, State, ZIP code combinations
  - International postal codes

- **Map Integration**:
  - Creates intents for Google Maps/Apple Maps
  - Generates navigation intents for directions
  - Creates static map preview URLs for embedding

- **Shared Location Handling**:
  - Detects when users share their current location
  - Extracts coordinates or addresses from shared location messages

#### Usage Example:
```java
// Detect locations in message
List<LocationDetectionHelper.LocationInfo> locations = 
    LocationDetectionHelper.detectLocations("Meet me at 123 Main Street");

LocationDetectionHelper.LocationInfo primaryLocation = 
    LocationDetectionHelper.getPrimaryLocation(messageBody);

if (primaryLocation != null && primaryLocation.hasValidLocation()) {
    Intent mapIntent = LocationDetectionHelper.createMapIntent(primaryLocation);
    context.startActivity(mapIntent);
}
```

### 3. Enhanced Contact Synchronization (`ContactUtils.java` extensions)

#### Key Features:
- **Enhanced Contact Information**:
  - Email addresses, organization data
  - Social media profile links
  - Cross-platform user IDs
  - Last sync timestamps

- **Cross-Platform Sync Framework**:
  - Pluggable sync provider interface
  - Support for multiple messaging platforms
  - Contact deduplication and merging
  - Sync status tracking

- **Multi-Platform Contact Detection**:
  - Identifies contacts available on multiple platforms
  - Maintains platform-specific user IDs
  - Enables unified contact management

#### Usage Example:
```java
// Get enhanced contact information
ContactUtils.EnhancedContactInfo contact = 
    ContactUtils.getEnhancedContactInfo(context, phoneNumber);

// Check if contact exists on multiple platforms
boolean isMultiPlatform = ContactUtils.isMultiPlatformContact(context, phoneNumber);

// Set up cross-platform sync
ContactUtils.CrossPlatformContactSync syncManager = 
    new ContactUtils.CrossPlatformContactSync();
syncManager.addSyncProvider(customProvider);
syncManager.synchronizeContacts(context);
```

### 4. Message Class Integration

The `Message.java` class has been extended with convenience methods:

- `hasCalendarEventIndicators()` - Quick check for calendar events
- `detectCalendarEvent()` - Get detailed event information
- `createCalendarEventIntent()` - Create calendar intent directly
- `hasLocationIndicators()` - Quick check for locations
- `detectLocations()` - Get all detected locations
- `getPrimaryLocation()` - Get the main location
- `createMapIntent()` / `createDirectionsIntent()` - Map integration
- `getIntegrationSummary()` - Overview of available integrations

#### Usage Example:
```java
Message message = getMessageFromSms();

if (message.hasCalendarEventIndicators()) {
    Intent calendarIntent = message.createCalendarEventIntent();
    if (calendarIntent != null) {
        context.startActivity(calendarIntent);
    }
}

if (message.hasLocationIndicators()) {
    Intent mapIntent = message.createMapIntent();
    if (mapIntent != null) {
        context.startActivity(mapIntent);
    }
}
```

## Permissions Added

The following permissions were added to `AndroidManifest.xml`:

```xml
<!-- Calendar integration permissions -->
<uses-permission android:name="android.permission.READ_CALENDAR" />
<uses-permission android:name="android.permission.WRITE_CALENDAR" />

<!-- Location services permissions (optional for map previews) -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- Additional contact permissions for enhanced synchronization -->
<uses-permission android:name="android.permission.WRITE_CONTACTS" />
```

## Testing

Comprehensive tests have been implemented in `ThirdPartyIntegrationTest.java`:

- **Calendar Integration Tests**: Date/time detection, event creation, intent validation
- **Location Detection Tests**: Address recognition, coordinate parsing, map intent creation  
- **Contact Sync Tests**: Enhanced contact info, cross-platform sync, multi-platform detection
- **Message Integration Tests**: End-to-end integration with Message class
- **Edge Case Tests**: Empty messages, multiple locations, validation scenarios

## Architecture Benefits

### Minimal Changes Approach
- New functionality added as separate utility classes
- Existing Message class extended with convenience methods only
- No breaking changes to existing API
- Backward compatibility maintained

### Modular Design
- Each integration is self-contained and can be used independently
- Clean separation between detection logic and action creation
- Extensible framework for adding new platforms/services

### Performance Considerations
- Lazy evaluation - integration checks only performed when requested
- Efficient regex patterns for content detection
- Caching of detection results in Message.MessageIntegrationSummary

## Integration Points

### UI Integration
The enhanced Message class provides easy integration points for UI components:

```java
Message.MessageIntegrationSummary summary = message.getIntegrationSummary();
List<String> availableActions = summary.getAvailableActions();

// UI can display action buttons based on available integrations
for (String action : availableActions) {
    addActionButton(action, message);
}
```

### Background Processing
The contact synchronization framework supports background sync:

```java
// Can be integrated with WorkManager for periodic sync
ContactUtils.CrossPlatformContactSync syncManager = getSyncManager();
boolean success = syncManager.synchronizeContacts(context);
```

### Notification Integration
Location and calendar events can enhance notifications:

```java
if (message.isSharedLocation()) {
    // Add map preview to notification
    String previewUrl = message.createMapPreviewUrl(300, 200);
    // Load preview image and add to notification
}
```

## Future Enhancements

The framework is designed to support future enhancements:

1. **Additional Calendar Providers**: Support for different calendar apps
2. **More Location Services**: Integration with ride-sharing, weather services
3. **Platform-Specific Sync**: WhatsApp, Telegram, Signal integration
4. **AI-Enhanced Detection**: Machine learning for better content recognition
5. **Privacy Controls**: User preferences for integration features

## API Key Configuration

For production use, some features may require API keys:

- **Google Maps Static API**: For map previews (optional)
- **Platform-specific APIs**: For cross-platform contact sync

These should be added to the app's configuration and the relevant helper classes updated accordingly.

## Conclusion

This implementation provides a solid foundation for third-party service integration while maintaining the app's existing architecture and ensuring backward compatibility. The modular design allows for easy extension and customization based on user needs and platform requirements.