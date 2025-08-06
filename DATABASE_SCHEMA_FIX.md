# Database Schema Fix Documentation

## Problem
The app was trying to query a column named `recipient_ids` that doesn't exist in the Android SMS/MMS content provider database tables, causing SQLiteException errors.

## Root Cause
The Android Telephony content provider has a specific schema with standard columns. The `recipient_ids` column used in the original code is not part of this standard schema.

## Solution
Replaced all `recipient_ids` column references with standard Android Telephony provider columns:

### Standard Android SMS/MMS Provider Columns:
- `Telephony.Sms.ADDRESS` ("address") - The phone number/address  
- `Telephony.Sms.THREAD_ID` ("thread_id") - Conversation thread identifier
- `Telephony.Mms.THREAD_ID` ("thread_id") - MMS conversation thread identifier
- `Telephony.Sms.DATE` ("date") - Message timestamp
- `Telephony.Mms.DATE` ("date") - MMS timestamp

### Files Modified:

1. **MessageService.java**
   - `getThreadIdForAddress()`: Now queries SMS/MMS tables directly using standard columns
   - `getAddressForThreadId()`: Uses `Telephony.Sms.ADDRESS` and `Telephony.Sms.THREAD_ID` 
   - Added `checkThreadContainsAddress()` helper for MMS validation

2. **DebugActivity.java**
   - `findPotentialThreads()`: Removed `recipient_ids` from query column list

## Key Changes:

### Before (Broken):
```java
// This caused SQLiteException: no such column: recipient_ids
cursor = context.getContentResolver().query(
    Uri.parse("content://mms-sms/conversations?simple=true"),
    new String[]{"recipient_ids"},
    "_id = ?",
    new String[]{threadId},
    null);
```

### After (Fixed):
```java  
// Direct query using standard Android columns
cursor = context.getContentResolver().query(
    Telephony.Sms.CONTENT_URI,
    new String[]{Telephony.Sms.ADDRESS},
    Telephony.Sms.THREAD_ID + " = ?",
    new String[]{threadId},
    Telephony.Sms.DATE + " DESC LIMIT 1");
```

## Benefits:
- ✅ Eliminates SQLiteException errors
- ✅ Uses standard Android API approach
- ✅ Compatible across Android versions
- ✅ Better error handling and fallbacks
- ✅ Resolves translation cache issues (secondary effect)

## Testing:
Created `DatabaseSchemaFixTest.java` to validate:
- Standard column usage
- Correct query structures
- No references to non-existent columns