# LinguaSMS - Multilingual Messaging App

LinguaSMS is an Android messaging application with real-time translation capabilities. The app serves as a default SMS/MMS client with integrated translation features to enable cross-language communication.

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

## Working Effectively

### Prerequisites and Setup
- Install Android SDK 35 (API level 35) minimum, with build tools 35.0.0+
- Java 17 (OpenJDK recommended) - **CRITICAL**: Project requires Java 17, not newer versions
- Set ANDROID_HOME environment variable to your Android SDK path
- Set JAVA_HOME to your Java 17 installation

### Initial Setup Commands
Run these commands in order for a fresh clone:
```bash
# Verify Java version (must be 17)
java --version

# Verify Android SDK is available
which sdkmanager || echo "Install Android SDK first"

# Set up Android SDK if needed (adjust path as needed)
export ANDROID_HOME=/path/to/android-sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# Make gradlew executable
chmod +x ./gradlew

# Clean and build - NEVER CANCEL: First build takes 15-45 minutes
./gradlew clean build --timeout=3600 --no-daemon
```

**CRITICAL BUILD TIMING**:
- **NEVER CANCEL builds or tests** - Android builds take significant time
- Initial build: 15-45 minutes (depending on network and cache)
- Incremental builds: 2-5 minutes  
- Set timeout to 60+ minutes for initial builds
- Set timeout to 10+ minutes for incremental builds

### Build and Test Commands
```bash
# Full clean build - NEVER CANCEL: Takes 15-45 minutes
./gradlew clean build --timeout=3600

# Debug build only - NEVER CANCEL: Takes 10-30 minutes  
./gradlew assembleDebug --timeout=2400

# Release build - NEVER CANCEL: Takes 15-45 minutes
./gradlew assembleRelease --timeout=3600

# Run unit tests - NEVER CANCEL: Takes 5-15 minutes
./gradlew test --timeout=1800

# Run instrumented tests (requires emulator/device) - NEVER CANCEL: Takes 10-30 minutes
./gradlew connectedAndroidTest --timeout=3600

# Run all checks including lint - NEVER CANCEL: Takes 10-25 minutes
./gradlew check --timeout=2400
```

### Validation Requirements
**CRITICAL**: After making ANY changes, you MUST:
1. Build the app successfully: `./gradlew assembleDebug`
2. Run unit tests: `./gradlew test`  
3. Run lint checks: `./gradlew lint`
4. Test basic app functionality (see validation scenarios below)

**Manual Validation Scenarios**:
Since this is an Android app requiring device/emulator, validate these scenarios when possible:
- App launches without crashes
- SMS permissions can be requested
- Translation service initializes properly
- Default SMS app selection works
- Message database operations function correctly

### Development Workflow
```bash
# Install debug APK to connected device/emulator
./gradlew installDebug

# View logs from connected device
adb logcat | grep "LinguaSMS\|translator\|messagingapp"

# Check lint results
./gradlew lint
# View report: app/build/reports/lint-results-debug.html

# Format code (if ktlint/spotless is configured)
./gradlew ktlintFormat || echo "No automatic formatting configured"
```

## Project Structure and Navigation

### Key Source Directories
- `app/src/main/java/com/translator/messagingapp/` - Main application code (44 Java files)
- `app/src/test/java/` - Unit tests
- `app/src/androidTest/java/` - Instrumented tests  
- `app/src/main/res/` - Android resources (layouts, strings, etc.)
- `app/src/main/AndroidManifest.xml` - App configuration and permissions

### Core Components
**Activities** (UI screens):
- `SplashActivity` - App launcher and initialization
- `MainActivity` - Main conversation list
- `ConversationActivity` - Individual conversation view
- `NewMessageActivity` - Compose new messages
- `SettingsActivity` - App preferences
- `SearchActivity` - Message search functionality
- `DebugActivity` - Development debugging tools

**Key Services**:
- `MessageService` - Core messaging operations
- `HeadlessSmsSendService` - Required for default SMS app functionality
- `TranslationManager` - Handles message translation
- `GoogleTranslationService` - Google Translate API integration

**Data Models**:
- `Message` - Base message class
- `SmsMessage`, `MmsMessage`, `RcsMessage` - Message type implementations
- `Conversation` - Conversation thread model
- `MessageCache`, `TranslationCache` - Performance optimization

**Critical Files to Always Check**:
- `TranslationManager.java` - After any translation-related changes
- `MessageService.java` - After messaging functionality changes  
- `AndroidManifest.xml` - After permission or component changes
- `build.gradle.kts` - After dependency changes

### Configuration Files
- `build.gradle.kts` - Build configuration and dependencies
- `gradle.properties` - Gradle and Android build settings
- `gradle/libs.versions.toml` - Dependency version catalog
- `proguard-rules.pro` - Code obfuscation rules for release builds

## Testing

### Unit Tests Location
- `app/src/test/java/com/translator/messagingapp/`
- Run with: `./gradlew test --timeout=1800`
- Key test files:
  - `ExampleUnitTest.java` - Basic unit test template
  - `ConversationRecyclerAdapterTest.java` - Adapter testing
  - `ThemeTest.java` - Theme functionality testing

### Instrumented Tests Location  
- `app/src/androidTest/java/com/translator/messagingapp/`
- Requires Android device/emulator
- Run with: `./gradlew connectedAndroidTest --timeout=3600`

### Testing Best Practices
- Always run `./gradlew test` before committing changes
- Add unit tests for new business logic in translation or messaging
- Test translation caching functionality for performance
- Verify permission handling for SMS/MMS functionality

## Dependencies and Versions

### Key Dependencies (from libs.versions.toml)
- Android Gradle Plugin: 8.1.4
- CompileSDK: 35, MinSDK: 24, TargetSDK: 35
- Java: 17 (required)
- AndroidX AppCompat: 1.4.1+
- Material Design: 1.12.0+
- OkHttp: 4.9.3+ (for network requests)
- Glide: 4.12.0+ (for image loading)
- JUnit: 4.13.2+ (for testing)

### Updating Dependencies
```bash
# Check for dependency updates
./gradlew dependencyUpdates || echo "Plugin not configured"

# After changing dependencies in libs.versions.toml or build.gradle.kts
./gradlew build --refresh-dependencies --timeout=3600
```

## Common Issues and Solutions

### Build Issues
- **"Plugin not found"**: Ensure Android SDK is properly installed and ANDROID_HOME is set
  - Download Android SDK from: https://developer.android.com/studio#command-tools
  - Extract and set ANDROID_HOME environment variable
  - Install required SDK packages: `sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"`
- **"Java version mismatch"**: Use Java 17 exactly, not newer versions
- **Build timeout**: Increase timeout values, never cancel Android builds
- **Out of memory**: Add `org.gradle.jvmargs=-Xmx4g` to gradle.properties
- **Network/firewall issues**: If downloads fail, use corporate proxy settings in gradle.properties:
  ```
  systemProp.http.proxyHost=proxy.company.com
  systemProp.http.proxyPort=8080
  systemProp.https.proxyHost=proxy.company.com
  systemProp.https.proxyPort=8080
  ```

### Permission Issues
- App requires extensive SMS/MMS permissions - normal for messaging apps
- Default SMS app functionality requires specific manifest declarations
- Test permissions on different Android versions (API 24+)

### Translation Features
- Google Translate API integration requires network connectivity
- Translation caching improves performance and reduces API calls
- Always test offline behavior with cached translations

## Debug and Development Tools

### Logging
- Use `Log.d(TAG, message)` with appropriate TAG constants
- Filter logs: `adb logcat | grep "translator\|messagingapp"`
- Debug builds enable additional logging via `BuildConfig.ENABLE_DEBUG_TOOLS`

### Debug Activity
- Available in debug builds only
- Access via `DebugActivity` for development features
- Shows internal app state and debugging information

## Performance Considerations

### Build Performance
- Use `--no-daemon` flag if builds hang
- Clean build cache: `./gradlew cleanBuildCache`
- Gradle daemon may cause issues: `./gradlew --stop`

### Runtime Performance  
- Message caching reduces database queries
- Translation caching reduces API calls
- Image loading optimized with Glide library

## Common Development Tasks

### Repository Analysis and Understanding
When starting work on this codebase, always begin by running:
```bash
# Get familiar with the project structure
find app/src/main/java -name "*.java" | wc -l  # Should show 44 Java files
ls -la app/src/main/res/layout/ | wc -l       # Check UI layouts available
grep -r "TODO\|FIXME" app/src/main/java/      # Find areas needing attention
```

### Adding New Features
1. Always build and test first: `./gradlew assembleDebug test`
2. Add appropriate unit tests for business logic
3. Update AndroidManifest.xml if new permissions needed
4. Test translation integration if messaging features changed
5. Run full validation: `./gradlew check lint`

### Debugging SMS/Translation Issues
1. Check device logs: `adb logcat | grep "messagingapp"`
2. Verify app is set as default SMS app
3. Test translation cache functionality
4. Check network connectivity for translation API

### Release Preparation
```bash
# Full release build with all checks - NEVER CANCEL: Takes 20-45 minutes
./gradlew clean assembleRelease lint test --timeout=3600

# Check APK size and content
ls -la app/build/outputs/apk/release/
```

### Environment Verification
Before starting development, verify your environment:
```bash
# Check all requirements
java --version                    # Should show Java 17
echo $ANDROID_HOME               # Should point to Android SDK
./gradlew --version              # Should show Gradle 8.11.1
adb version                      # Should show ADB if available
```

Remember: This is a complex Android messaging app with translation features. Always allow sufficient time for builds and testing, and never cancel long-running Android build processes.

## Quick Reference - Common Outputs

### Repository Structure Overview
```
LinguaSMS/
├── app/
│   ├── build.gradle.kts           # App build configuration  
│   ├── src/main/
│   │   ├── AndroidManifest.xml    # App permissions and components
│   │   ├── java/com/translator/messagingapp/  # 44 Java source files
│   │   └── res/                   # UI layouts, strings, images
│   ├── src/test/                  # Unit tests (3 files)
│   └── src/androidTest/           # Instrumented tests (1 file)
├── gradle/
│   ├── libs.versions.toml         # Dependency versions
│   └── wrapper/                   # Gradle wrapper files
├── gradle.properties              # Build settings
├── gradlew                        # Gradle wrapper script
└── settings.gradle.kts            # Project settings
```

### App Permissions (from AndroidManifest.xml)
The app requires these critical permissions:
- SMS/MMS permissions: SEND_SMS, RECEIVE_SMS, READ_SMS, WRITE_SMS, RECEIVE_MMS
- Contacts: READ_CONTACTS, READ_PHONE_STATE, READ_PHONE_NUMBERS  
- Network: INTERNET (for translation API)
- System: POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED

### Key Build Outputs
Successful builds create:
- `app/build/outputs/apk/debug/app-debug.apk` - Debug APK for testing
- `app/build/outputs/apk/release/app-release.apk` - Release APK for distribution
- `app/build/reports/lint-results-debug.html` - Lint analysis report
- `app/build/reports/tests/testDebugUnitTest/` - Unit test results

### Application Package Info
- Package name: `com.translator.messagingapp`
- App name: "Multilingual Messaging App" (from settings.gradle.kts)
- Minimum Android version: API 24 (Android 7.0)
- Target Android version: API 35 (Android 15)