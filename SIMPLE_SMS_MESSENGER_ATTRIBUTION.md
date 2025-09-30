# Simple-SMS-Messenger Attribution

This document provides attribution for code adapted from the Simple-SMS-Messenger project.

## Source Repository

**Repository:** https://github.com/SimpleMobileTools/Simple-SMS-Messenger  
**License:** GPL-3.0 License  
**Copyright:** SimpleMobileTools

## License Compatibility

The Simple-SMS-Messenger project is licensed under GPL-3.0, which is compatible with this project's licensing terms.

## Adapted Components

### 1. Enhanced MMS Receiver (KlinkerMmsReceiver.java)

**Source Files:**
- `app/src/main/kotlin/com/simplemobiletools/smsmessenger/receivers/MmsReceiver.kt`

**Adaptations Made:**
- Converted from Kotlin to Java
- Enhanced error handling and logging
- Improved address blocking with phone number normalization
- Added image preview support for notifications using Glide
- Integrated with existing LinguaSMS architecture

**Key Features Adapted:**
- Address blocking with `isAddressBlocked()` method
- Enhanced error handling in `onError()` method
- Improved notification handling with attachment previews
- Better integration with image loading using Glide

### 2. MMS Part Model (MmsPart.java)

**Source Files:**
- `app/src/main/kotlin/com/simplemobiletools/smsmessenger/models/MmsPart.kt`

**Adaptations Made:**
- Converted from Kotlin to Java
- Maintained serialization support through ContentValues
- Added helper methods for content type detection
- Enhanced documentation and error handling

**Key Features Adapted:**
- `toContentValues()` method for database operations
- `isNonText()` method for attachment detection
- Content type detection methods (`isImage()`, `isVideo()`, `isAudio()`)
- Comprehensive getter/setter methods

### 3. MMS Message Enhancement (MmsMessage.java)

**Enhancements Inspired by:**
- `app/src/main/kotlin/com/simplemobiletools/smsmessenger/models/MmsAddress.kt`
- `app/src/main/kotlin/com/simplemobiletools/smsmessenger/models/MmsBackup.kt`

**Adaptations Made:**
- Added `getSender()` method for consistent address handling
- Enhanced `getFirstImageAttachmentUri()` for notification previews
- Improved thread ID handling with `getThreadId()` method

## Implementation Notes

### Phone Number Normalization

The phone number normalization logic in `KlinkerMmsReceiver.normalizePhoneNumber()` is adapted from Simple-SMS-Messenger's address handling:

```java
private String normalizePhoneNumber(String phoneNumber) {
    // Logic adapted from Simple-SMS-Messenger for consistent number formatting
    // Handles international formats, US numbers, and various formatting styles
}
```

### Image Preview Support

The attachment preview functionality in `processAttachmentPreview()` is inspired by Simple-SMS-Messenger's notification system:

```java
private void processAttachmentPreview(Context context, MmsMessage mms, String address) {
    // Uses Glide for image loading, similar to Simple-SMS-Messenger
    // Handles notification icon sizing and async image loading
}
```

### Enhanced Error Handling

The error handling patterns throughout the enhanced receiver follow Simple-SMS-Messenger's approach:
- Graceful handling of null parameters
- User-friendly error messages
- Proper logging for debugging
- Fallback mechanisms for failed operations

## Testing

Tests have been created to verify the adapted functionality:
- `EnhancedKlinkerMmsReceiverTest.java` - Tests the enhanced receiver
- `MmsPartTest.java` - Tests the MMS part model

## Compliance Statement

This implementation:
1. ✅ Properly attributes the source code from Simple-SMS-Messenger
2. ✅ Maintains GPL-3.0 license compatibility
3. ✅ Documents all adaptations made
4. ✅ Preserves original functionality while integrating with LinguaSMS
5. ✅ Provides enhanced error handling and logging

## Future Enhancements

The following features from Simple-SMS-Messenger could be further integrated:
- Enhanced conversation management
- Advanced MMS backup/restore functionality
- Additional attachment type support
- Improved notification customization

---

**Acknowledgment:** We thank the SimpleMobileTools team for their excellent work on Simple-SMS-Messenger, which has significantly improved the MMS functionality in LinguaSMS.