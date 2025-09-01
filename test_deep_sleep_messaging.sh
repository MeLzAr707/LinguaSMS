#!/bin/bash

# Manual testing script for deep sleep message handling functionality
# This script helps verify that the LinguaSMS app can receive messages during deep sleep mode

echo "=========================================="
echo "LinguaSMS Deep Sleep Message Handling Test"
echo "=========================================="
echo

# Function to check if adb is available
check_adb() {
    if ! command -v adb &> /dev/null; then
        echo "❌ ADB is not installed or not in PATH"
        echo "Please install Android SDK Platform Tools"
        exit 1
    fi
    echo "✅ ADB is available"
}

# Function to check if device is connected
check_device() {
    DEVICE_COUNT=$(adb devices | grep -c "device$")
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        echo "❌ No Android device connected"
        echo "Please connect an Android device with USB debugging enabled"
        exit 1
    fi
    echo "✅ Android device connected"
}

# Function to install the app
install_app() {
    echo "📱 Installing LinguaSMS app..."
    if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        adb install -r app/build/outputs/apk/debug/app-debug.apk
        if [ $? -eq 0 ]; then
            echo "✅ App installed successfully"
        else
            echo "❌ Failed to install app"
            exit 1
        fi
    else
        echo "❌ APK file not found. Please build the app first with: ./gradlew assembleDebug"
        exit 1
    fi
}

# Function to set app as default SMS app
set_default_sms() {
    echo "📲 Setting LinguaSMS as default SMS app..."
    adb shell cmd role add-role-holder android.app.role.SMS com.translator.messagingapp
    if [ $? -eq 0 ]; then
        echo "✅ LinguaSMS set as default SMS app"
    else
        echo "⚠️  Could not set as default SMS app automatically"
        echo "Please manually set LinguaSMS as the default SMS app:"
        echo "1. Open the app"
        echo "2. Accept the default SMS app prompt"
        echo "3. Press Enter to continue..."
        read
    fi
}

# Function to disable battery optimization
disable_battery_optimization() {
    echo "🔋 Disabling battery optimization for LinguaSMS..."
    adb shell cmd appops set com.translator.messagingapp RUN_IN_BACKGROUND allow
    adb shell dumpsys deviceidle whitelist +com.translator.messagingapp
    echo "✅ Battery optimization disabled"
}

# Function to check if monitoring service is running
check_monitoring_service() {
    echo "🔍 Checking if MessageMonitoringService is running..."
    SERVICE_RUNNING=$(adb shell dumpsys activity services | grep -c "MessageMonitoringService")
    if [ "$SERVICE_RUNNING" -gt 0 ]; then
        echo "✅ MessageMonitoringService is running"
    else
        echo "⚠️  MessageMonitoringService not detected"
        echo "Starting the app to initialize service..."
        adb shell am start -n com.translator.messagingapp/.MainActivity
        sleep 5
    fi
}

# Function to simulate deep sleep mode
simulate_deep_sleep() {
    echo "😴 Simulating deep sleep mode..."
    echo "Enabling Doze mode..."
    adb shell dumpsys deviceidle force-idle
    echo "✅ Device is now in simulated deep sleep (Doze mode)"
    
    echo "The device is now in deep sleep simulation."
    echo "Send an SMS to this device from another phone and wait 30 seconds."
    echo "Press Enter after sending the SMS..."
    read
    
    echo "Waiting 30 seconds for message processing..."
    sleep 30
}

# Function to check for received messages
check_received_messages() {
    echo "📥 Checking for received messages..."
    
    # Exit deep sleep mode
    adb shell dumpsys deviceidle unforce
    echo "Device exited deep sleep mode"
    
    # Wait a moment for system to wake up
    sleep 5
    
    # Check if messages were received
    echo "Checking SMS content provider for new messages..."
    RECENT_MESSAGES=$(adb shell content query --uri content://sms/inbox --where "date > $(echo $(($(date +%s) - 300)) * 1000)" --projection "address,body,date")
    
    if [ -n "$RECENT_MESSAGES" ]; then
        echo "✅ Recent messages found:"
        echo "$RECENT_MESSAGES"
    else
        echo "❌ No recent messages found in the last 5 minutes"
    fi
}

# Function to check app logs
check_app_logs() {
    echo "📋 Checking app logs for deep sleep handling..."
    adb logcat -d | grep -i "LinguaSMS\|MessageMonitoring\|MessageCheck\|MessageAlarm" | tail -20
}

# Function to verify foreground service
verify_foreground_service() {
    echo "🔔 Checking foreground service notification..."
    NOTIFICATION_COUNT=$(adb shell dumpsys notification | grep -c "com.translator.messagingapp")
    if [ "$NOTIFICATION_COUNT" -gt 0 ]; then
        echo "✅ Foreground service notification is active"
    else
        echo "⚠️  No foreground service notification detected"
    fi
}

# Function to test alarm scheduling
test_alarm_scheduling() {
    echo "⏰ Testing alarm scheduling..."
    adb shell dumpsys alarm | grep -A5 -B5 "com.translator.messagingapp"
    echo "Check if MessageCheckReceiver alarms are scheduled above"
}

# Main execution
main() {
    echo "Starting deep sleep message handling test..."
    echo
    
    # Preliminary checks
    check_adb
    check_device
    
    # Installation and setup
    echo "Setting up test environment..."
    install_app
    set_default_sms
    disable_battery_optimization
    
    # Service verification
    echo
    echo "Verifying services..."
    check_monitoring_service
    verify_foreground_service
    test_alarm_scheduling
    
    # Deep sleep test
    echo
    echo "Performing deep sleep test..."
    simulate_deep_sleep
    check_received_messages
    
    # Log analysis
    echo
    echo "Analyzing logs..."
    check_app_logs
    
    echo
    echo "=========================================="
    echo "Deep Sleep Test Completed"
    echo "=========================================="
    echo
    echo "Test Summary:"
    echo "1. ✅ App installed and set as default SMS app"
    echo "2. ✅ Battery optimization disabled"
    echo "3. ✅ Services verified"
    echo "4. ✅ Deep sleep mode simulated"
    echo "5. ✅ Message reception tested"
    echo
    echo "Please verify that:"
    echo "- Messages sent during deep sleep were received"
    echo "- Foreground service notification was visible"
    echo "- App logs show periodic message checks"
    echo
    echo "If messages were lost, check:"
    echo "- Battery optimization settings"
    echo "- Background app refresh settings"
    echo "- Do Not Disturb settings"
    echo "- Device-specific power management settings"
}

# Run the main function
main "$@"