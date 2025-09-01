# Offline Translation Troubleshooting Guide

This guide provides comprehensive troubleshooting steps for common issues with offline language model downloads and implementation in LinguaSMS.

## Common Issues and Solutions

### 1. Model Download Failures

#### Issue: "Download failed" or "Network error during download"
**Symptoms:**
- Download progress stops or fails
- Error message indicates network issues
- Models don't appear as downloaded after download attempt

**Solutions:**
1. **Check Internet Connection:**
   ```
   - Ensure stable internet connection
   - Try downloading over Wi-Fi instead of mobile data
   - Check if other apps can access the internet
   ```

2. **Check Available Storage:**
   ```
   - Verify device has sufficient storage space
   - Each language model requires 25-40MB of space
   - Free up space if needed and retry download
   ```

3. **Restart the Application:**
   ```
   - Close LinguaSMS completely
   - Clear app from recent apps
   - Restart the app and retry download
   ```

4. **Clear App Data (Last Resort):**
   ```
   - Go to Settings > Apps > LinguaSMS > Storage
   - Clear Cache (preserves settings)
   - If issues persist, Clear Data (resets all settings)
   ```

#### Issue: "Model already downloaded" error when model appears unavailable
**Symptoms:**
- Error says model is downloaded but not available for translation
- Model appears in downloaded list but doesn't work

**Solutions:**
1. **Verify Model Integrity:**
   - The app automatically checks model integrity
   - If integrity check fails, delete and re-download the model
   
2. **Clear Model Cache:**
   ```
   - Go to Settings > Offline Translation > Manage Models
   - Delete the problematic model
   - Re-download the model
   ```

### 2. Model Integrity Issues

#### Issue: "Model integrity verification failed"
**Symptoms:**
- Download completes but model fails verification
- Translation attempts fail with corruption errors
- Models appear downloaded but don't work

**Solutions:**
1. **Re-download the Model:**
   ```
   - Delete the corrupted model
   - Clear the download cache
   - Download the model again
   ```

2. **Check Storage Health:**
   ```
   - Run device storage diagnostics
   - Check for storage corruption
   - Consider freeing up more storage space
   ```

3. **Restart Device:**
   ```
   - Some storage issues resolve after restart
   - Retry download after restart
   ```

### 3. Translation Not Working Offline

#### Issue: Translation fails when offline despite downloaded models
**Symptoms:**
- Models show as downloaded
- Translation works online but fails offline
- "Network unavailable" errors when offline

**Solutions:**
1. **Check Translation Mode Settings:**
   ```
   - Go to Settings > Translation Settings
   - Ensure "Enable Offline Translation" is ON
   - Set "Prefer Offline Translation" to ON
   - Try "Offline Only" mode for testing
   ```

2. **Verify Model Synchronization:**
   ```
   - Close and reopen the app
   - Check that models appear in both:
     * Settings > Offline Translation > Manage Models
     * Individual conversation translation options
   ```

3. **Test Translation Mode:**
   ```
   - Switch to "Offline Only" mode temporarily
   - Attempt translation
   - If it works, the issue is with automatic mode selection
   ```

### 4. Platform-Specific Issues

#### Android 7.0 - 8.1 (API 24-27)
**Common Issues:**
- Slower download speeds
- Occasional MLKit compatibility issues

**Solutions:**
```
- Ensure Google Play Services is updated
- Download models one at a time
- Allow extra time for download completion
```

#### Android 9.0+ (API 28+)
**Common Issues:**
- Background download restrictions
- Network security policy restrictions

**Solutions:**
```
- Keep app in foreground during downloads
- Disable battery optimization for LinguaSMS
- Check if device has restrictive network policies
```

#### Low-End Devices (< 2GB RAM)
**Common Issues:**
- Out of memory during download
- Translation timeouts

**Solutions:**
```
- Download models one at a time
- Close other apps before downloading
- Restart device if memory issues persist
- Consider downloading only essential language pairs
```

### 5. Specific Error Messages

#### Error: "Language models not downloaded"
```
Cause: Required language models are missing
Solution:
1. Go to Settings > Offline Translation > Manage Models
2. Download models for both source and target languages
3. Wait for "Download Complete" confirmation
4. Retry translation
```

#### Error: "Unsupported language pair"
```
Cause: One or both languages not supported offline
Solution:
1. Check supported languages list in settings
2. Use alternative language if available
3. Use online translation for unsupported languages
```

#### Error: "No text to translate"
```
Cause: Empty or null input text
Solution:
1. Ensure message contains text
2. Check for invisible characters
3. Try copying and pasting the text
```

#### Error: "MLKit model verification failed"
```
Cause: Downloaded model is corrupted or incompatible
Solution:
1. Delete the affected model
2. Clear app cache
3. Re-download the model
4. If persistent, restart device and retry
```

### 6. Performance Issues

#### Issue: Slow translation speed offline
**Solutions:**
```
1. Ensure device is not low on memory
2. Close unnecessary background apps
3. Restart the app if it's been running for a long time
4. Consider device limitations for older hardware
```

#### Issue: High battery usage during downloads
**Solutions:**
```
1. Download models while device is charging
2. Use Wi-Fi instead of mobile data
3. Download during off-peak usage times
4. Monitor download progress and pause if needed
```

## Diagnostic Steps

### Step 1: Basic Verification
1. Check internet connectivity
2. Verify available storage space (at least 500MB free recommended)
3. Ensure app permissions are granted
4. Check if Google Play Services is installed and updated

### Step 2: Model Status Check
1. Go to Settings > Offline Translation > Manage Models
2. Note which models show as "Downloaded"
3. Try downloading a small model (like English) as a test
4. Verify download completes successfully

### Step 3: Translation Test
1. Set translation mode to "Offline Only"
2. Try translating simple text between downloaded languages
3. Check for any error messages
4. Switch back to "Auto" mode and test again

### Step 4: Advanced Diagnostics
1. Clear app cache (not data) and retry
2. Test with a different language pair
3. Try translation in airplane mode (true offline test)
4. Check system logs for detailed error information

## Recovery Procedures

### Complete Reset (When All Else Fails)
```
1. Go to Settings > Offline Translation > Manage Models
2. Delete all downloaded models
3. Clear app cache
4. Restart the app
5. Re-download essential models one by one
6. Test translation after each download
```

### Backup and Restore
```
Note: LinguaSMS offline models are tied to the device and cannot be backed up.
When reinstalling the app or switching devices:
1. Make note of which languages you had downloaded
2. After installation, re-download required models
3. Test functionality before relying on offline translation
```

## Prevention Tips

1. **Regular Maintenance:**
   - Periodically check model integrity
   - Keep adequate free storage space
   - Update the app when new versions are available

2. **Download Strategy:**
   - Download models during good network conditions
   - Download essential language pairs first
   - Test each model after download

3. **Storage Management:**
   - Monitor device storage regularly
   - Remove unused language models
   - Use cloud storage for other files to free up space

## Getting Help

If these troubleshooting steps don't resolve your issue:

1. **Check App Version:**
   - Ensure you're running the latest version
   - Check app store for updates

2. **Report Issues:**
   - Include specific error messages
   - Note your device model and Android version
   - Describe the exact steps that lead to the problem

3. **Community Support:**
   - Check existing issues on the project repository
   - Search for similar problems and solutions
   - Provide detailed information when reporting new issues

## Technical Notes

### Model Storage Location
```
Internal Storage: /data/data/com.translator.messagingapp/files/offline_models/
```

### Model File Format
```
Files are named: {language_code}.model
Example: en.model, es.model, fr.model
```

### Integrity Verification
```
Models use SHA-1 checksums for integrity verification
Checksums are stored in app preferences
Verification occurs during download and app startup
```

### Supported Languages
```
Current offline support includes 50+ languages
Full list available in Settings > Offline Translation
Language support may vary by device and region
```

---

**Last Updated:** [Current Date]
**App Version:** [Current Version]
**Minimum Android Version:** 7.0 (API 24)