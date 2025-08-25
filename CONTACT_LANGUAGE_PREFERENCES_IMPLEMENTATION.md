# Contact Language Preferences Feature

## Overview
This feature adds support for storing and using preferred languages on a per-contact basis within LinguaSMS. When sending outgoing messages, the app will automatically translate to each contact's saved language preference, falling back gracefully to global settings when no contact-specific preference is set.

## Implementation Details

### Core Components

#### 1. UserPreferences Extensions
The `UserPreferences` class has been extended with the following methods:

- `getContactLanguagePreference(String phoneNumber)` - Retrieves the language preference for a specific contact
- `setContactLanguagePreference(String phoneNumber, String languageCode)` - Sets the language preference for a contact
- `getEffectiveOutgoingLanguageForContact(String phoneNumber)` - Gets the effective language using fallback logic
- `hasContactLanguagePreference(String phoneNumber)` - Checks if a contact has a specific preference set
- `removeContactLanguagePreference(String phoneNumber)` - Removes a contact's language preference
- `normalizePhoneNumber(String phoneNumber)` - Internal method for consistent phone number handling

#### 2. ContactUtils Helper Methods
Static helper methods in `ContactUtils` provide convenient access to contact language preferences:

- `ContactUtils.setContactLanguagePreference(Context, String, String)`
- `ContactUtils.getContactLanguagePreference(Context, String)`
- `ContactUtils.getEffectiveOutgoingLanguageForContact(Context, String)`
- `ContactUtils.hasContactLanguagePreference(Context, String)`
- `ContactUtils.removeContactLanguagePreference(Context, String)`

### Data Storage
- Uses the existing SharedPreferences system for consistency and simplicity
- Storage key format: `"contact_lang_preference_" + normalizedPhoneNumber`
- Phone numbers are normalized to handle different formatting (removes non-digits, handles country codes)
- No database schema changes required

### Fallback Logic
The effective language for a contact is determined using this priority order:
1. Contact-specific language preference (if set)
2. Global outgoing language preference
3. General language preference

### Phone Number Normalization
To ensure consistent storage and retrieval regardless of phone number formatting:
- Removes all non-digit characters
- Handles US country codes (removes leading "1" for 11-digit numbers)
- Supports various input formats: `(123) 456-7890`, `+1-123-456-7890`, `123.456.7890`, etc.

## Integration Points

### 1. Translation Workflow
When translating messages for a specific recipient, use:
```java
String targetLanguage = ContactUtils.getEffectiveOutgoingLanguageForContact(context, recipientPhoneNumber);
// Pass targetLanguage to TranslationManager
```

### 2. Contact Management UI
For contact settings screens:
```java
// Get current preference
String currentLanguage = ContactUtils.getContactLanguagePreference(context, phoneNumber);

// Set new preference
ContactUtils.setContactLanguagePreference(context, phoneNumber, "es");

// Check if preference is set
boolean hasPreference = ContactUtils.hasContactLanguagePreference(context, phoneNumber);

// Remove preference (reset to global)
ContactUtils.removeContactLanguagePreference(context, phoneNumber);
```

### 3. Message Sending Logic
Replace static global language preferences with dynamic contact-specific preferences:
```java
// Old approach:
String targetLanguage = userPreferences.getPreferredOutgoingLanguage();

// New approach:
String targetLanguage = ContactUtils.getEffectiveOutgoingLanguageForContact(context, recipientPhoneNumber);
```

## Example Usage

### Setting Up Contact Preferences
```java
Context context = getApplicationContext();

// Set Spanish for contact 1
ContactUtils.setContactLanguagePreference(context, "1234567890", "es");

// Set French for contact 2  
ContactUtils.setContactLanguagePreference(context, "9876543210", "fr");

// Contact 3 uses global settings (no specific preference)
```

### Using in Translation
```java
public void sendTranslatedMessage(String message, String recipientPhoneNumber) {
    // Get the effective language for this contact
    String targetLanguage = ContactUtils.getEffectiveOutgoingLanguageForContact(context, recipientPhoneNumber);
    
    // Translate using the appropriate language
    translationManager.translateText(message, "auto", targetLanguage, new TranslationCallback() {
        @Override
        public void onTranslationComplete(String translatedText) {
            // Send the translated message
            sendMessage(translatedText, recipientPhoneNumber);
        }
        
        @Override
        public void onTranslationError(String error) {
            // Handle error - maybe send original message
            sendMessage(message, recipientPhoneNumber);
        }
    });
}
```

## UI Integration Ideas

### Contact List Enhancement
- Add language indicator icons next to contacts with specific preferences
- Show effective language in contact details

### Contact Settings Screen
- Add language selection dropdown/dialog
- Show current effective language with explanation
- Option to "Use Global Settings" or set specific language

### Message Composition
- Show target language indicator when composing messages
- Quick toggle to temporarily override contact preference

## Testing

### Unit Tests
- Comprehensive test suite in `ContactLanguagePreferencesTest.java`
- Covers all functionality including edge cases
- Tests phone number normalization
- Validates fallback logic
- Ensures backward compatibility

### Integration Examples
- `ContactLanguageIntegrationExample.java` shows usage patterns
- Demonstrates workflow integration
- Provides UI management examples

## Benefits

### For Users
- Personalized communication experience
- Automatic language selection based on contact
- Reduced friction in multilingual conversations
- Maintains context of language preferences

### For Developers
- Clean, backward-compatible API
- Minimal performance impact
- Easy integration with existing code
- Comprehensive error handling

## Backward Compatibility
- All existing UserPreferences methods unchanged
- Global language preferences continue to work exactly as before
- New functionality is purely additive
- No breaking changes to existing APIs

## Privacy & Storage
- All data stored locally on device using SharedPreferences
- No external data transmission required for preference storage
- User maintains full control over language preferences
- Can be easily cleared with app data reset

## Performance Considerations
- Lightweight SharedPreferences storage
- Fast preference lookups
- Minimal memory overhead
- Phone number normalization computed once per operation

## Future Enhancements
- Bulk import/export of contact language preferences
- Language detection based on received messages
- Smart suggestions for contact languages
- Integration with contact sync services
- Group language preferences for family/work contacts