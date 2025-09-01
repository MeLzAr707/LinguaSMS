# Deep Sleep Message Handling Implementation

## Overview

This document describes the implementation of deep sleep message handling for the LinguaSMS app to fix Issue #417 where incoming messages are lost when the phone enters deep sleep mode (Doze mode).

## Problem Description

When Android devices enter deep sleep mode (also known as Doze mode), the system aggressively restricts background activity to preserve battery life. This can cause:

- Background services to be killed or suspended
- Network access to be restricted
- Alarm timers to be deferred
- Broadcast receivers to not fire properly
- SMS/MMS messages to be lost or delayed

## Solution Architecture

The solution implements a multi-layered approach to ensure reliable message reception:

### 1. Foreground Service (`MessageMonitoringService`)

**Purpose**: Maintains app presence during deep sleep
**Key Features**:
- Runs as a foreground service with persistent notification
- Uses `PARTIAL_WAKE_LOCK` to keep CPU awake for message processing
- Automatically restarts if killed by system (`START_STICKY`)
- Minimal resource usage with efficient wake lock management

**Implementation**:
```java
public class MessageMonitoringService extends Service {
    // Foreground service that ensures app stays active
    // Uses notification channel with low importance
    // Acquires wake lock only when needed
}
```

### 2. Alarm-Based Periodic Checks (`MessageAlarmManager`)

**Purpose**: Bypasses Doze mode restrictions for critical message checks
**Key Features**:
- Uses `setExactAndAllowWhileIdle()` for API 23+ (bypasses Doze mode)
- Falls back to `setExact()` for API 19-22
- Schedules periodic checks every 15 minutes
- Automatically reschedules after each check

**Implementation**:
```java
// Uses the most reliable alarm method for each API level
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
}
```

### 3. Message Check Receiver (`MessageCheckReceiver`)

**Purpose**: Handles periodic message synchronization
**Key Features**:
- Triggered by alarm during deep sleep
- Acquires temporary wake lock for processing
- Schedules deep sleep compatible WorkManager tasks
- Self-reschedules for continuous operation

### 4. Battery Optimization Whitelist

**Purpose**: Prevents system from killing the app
**Key Features**:
- Automatically requests battery optimization exemption
- User-friendly dialog with explanation
- Remembers user preferences to avoid repeated requests
- Graceful handling of user rejection

**Implementation**:
```java
// Request battery optimization whitelist
Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
intent.setData(Uri.parse("package:" + packageName));
```

### 5. Deep Sleep Compatible WorkManager

**Purpose**: Executes background tasks with minimal constraints
**Key Features**:
- Reduced constraints for critical message sync
- Separate work chains for normal vs. deep sleep scenarios
- Optimized for battery efficiency while maintaining reliability

**Configuration**:
```java
// Minimal constraints for deep sleep compatibility
Constraints constraints = new Constraints.Builder()
    .setRequiresBatteryNotLow(false)    // Allow even with low battery
    .setRequiresCharging(false)         // Allow when not charging
    .setRequiresStorageNotLow(false)    // Allow with low storage
    .build();
```

### 6. Boot Integration

**Purpose**: Ensures service restart after device reboot
**Key Features**:
- Automatic service initialization on boot
- Only starts if app is default SMS app
- Initializes all periodic work tasks

## Technical Implementation Details

### Permissions Required

```xml
<!-- Deep sleep handling permissions -->
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
```

### Service Declaration

```xml
<service
    android:name=".MessageMonitoringService"
    android:exported="false"
    android:foregroundServiceType="dataSync" />

<receiver
    android:name=".MessageCheckReceiver"
    android:exported="false" />
```

### Lifecycle Management

1. **App becomes default SMS app** → Start monitoring service
2. **Device boots** → BootReceiver starts monitoring service (if default SMS app)
3. **Service starts** → Schedule periodic alarms
4. **Alarm fires** → MessageCheckReceiver triggers sync
5. **Sync completes** → Reschedule next alarm

## Testing

### Automated Tests

- Unit tests for all components (`DeepSleepMessageHandlingTest.java`)
- Battery optimization logic validation
- Wake lock handling verification
- Alarm scheduling confirmation
- Error handling validation

### Manual Testing

Use the provided script `test_deep_sleep_messaging.sh` to:

1. Install and configure the app
2. Set as default SMS app
3. Disable battery optimization
4. Simulate deep sleep mode
5. Test message reception
6. Verify service persistence

### Test Procedure

1. Set up device with LinguaSMS as default SMS app
2. Enable battery optimization whitelist
3. Simulate Doze mode: `adb shell dumpsys deviceidle force-idle`
4. Send SMS from another device
5. Wait 15+ minutes (for alarm to fire)
6. Exit Doze mode: `adb shell dumpsys deviceidle unforce`
7. Verify message was received and stored

## Performance Considerations

### Battery Usage
- Foreground service uses minimal resources
- Wake lock acquired only during message processing (max 30 seconds)
- Periodic checks limited to 15-minute intervals
- WorkManager respects system doze mode constraints when possible

### Memory Usage
- Lightweight service implementation
- No persistent connections maintained
- Efficient alarm scheduling
- Automatic cleanup of wake locks

### Network Usage
- No continuous network monitoring
- Uses existing SMS infrastructure
- Minimal data usage for message synchronization

## Troubleshooting

### Common Issues

1. **Messages still being lost**
   - Check battery optimization whitelist
   - Verify foreground service notification is visible
   - Check device-specific power management settings
   - Ensure alarm scheduling permissions are granted

2. **Service not starting**
   - Verify app is set as default SMS app
   - Check FOREGROUND_SERVICE permission
   - Review device logs for error messages

3. **High battery usage**
   - Check wake lock acquisition duration
   - Verify alarm frequency (should be 15 minutes)
   - Monitor foreground service resource usage

### Device-Specific Considerations

Some manufacturers implement aggressive power management that may interfere:

- **Xiaomi**: Disable MIUI optimization, add to autostart list
- **Huawei**: Add to protected apps list, disable power genie
- **Samsung**: Disable battery optimization, add to unmonitored apps
- **OnePlus**: Disable battery optimization, allow background activity

## Future Enhancements

1. **Adaptive Scheduling**: Adjust check frequency based on message volume
2. **Network State Monitoring**: More efficient sync based on connectivity
3. **Priority Message Detection**: Immediate processing for urgent messages
4. **User Configuration**: Allow users to customize check intervals
5. **Analytics**: Monitor effectiveness and battery impact

## Conclusion

This implementation provides a robust solution for deep sleep message handling by:

- Using multiple complementary technologies
- Respecting Android's power management while maintaining reliability
- Providing comprehensive testing and monitoring tools
- Offering graceful degradation when permissions are limited

The solution ensures that LinguaSMS users will receive messages reliably, even when their device is in deep sleep mode, while maintaining efficient battery usage and following Android best practices.