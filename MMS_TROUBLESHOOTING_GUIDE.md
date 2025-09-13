# MMS Troubleshooting Guide

## Common MMS Issues and Solutions

### SecurityException: No permission to access APN settings

**Issue:** You see errors like "SecurityException: No permission to access APN settings" in the logs.

**Cause:** Modern Android versions (5.0+) restrict access to APN settings for regular apps. This is expected behavior.

**Solution:** The app automatically handles this by:
1. Using CarrierConfigManager (preferred method)
2. Falling back to carrier-specific database
3. Using direct connection without proxy (works for most carriers)

**Status:** ✅ Fixed - The app now handles this gracefully and should work without manual intervention.

### MMS fails to send on Verizon

**Issue:** MMS messages fail to send on Verizon network (MCC/MNC: 311480).

**Troubleshooting Steps:**
1. Check network connectivity
2. Ensure you're the default SMS app
3. Verify MMS is enabled in device settings
4. Check carrier data plan includes MMS

**Technical Details:**
- Verizon MMSC URL: `http://mms.vtext.com/servlets/mms`
- Verizon typically does NOT require proxy settings
- Direct connection should work for most Verizon configurations

### Debug Information

To get detailed MMS configuration information, check the logs for "MMS Configuration Diagnostics" which shows:
- Current carrier information
- MMSC URL being used
- Proxy settings (if any)
- Network connectivity status
- Android version compatibility

### Supported Carriers

The app includes built-in configuration for:

**US Carriers:**
- Verizon (all MCC/MNC variants including 311480)
- T-Mobile
- AT&T
- Sprint (now part of T-Mobile)
- MVNOs using these networks

**Canadian Carriers:**
- Rogers
- Bell
- Telus

### Network Security

The app allows cleartext HTTP traffic for known MMS carrier domains including:
- mms.vtext.com (Verizon)
- mmsc.mobile.att.net (AT&T)
- mms.msg.eng.t-mobile.com (T-Mobile)

This is required because most carriers use HTTP (not HTTPS) for MMS.

### Common Error Messages

| Error | Meaning | Solution |
|-------|---------|----------|
| "No MMSC URL available" | Cannot determine carrier MMSC | Check network connection and carrier support |
| "HTTP error: 403" | Authentication/authorization failed | Check APN settings or contact carrier |
| "HTTP error: 404" | MMSC URL not found | Verify carrier configuration |
| "Connection refused" | Cannot connect to MMSC | Check network connectivity |
| "Connection timeout" | Network too slow or MMSC unavailable | Retry later or check network |

### When to Contact Support

Contact support if:
1. MMS fails consistently after trying basic troubleshooting
2. Your carrier is not in the supported list
3. You get persistent HTTP errors even with good network connectivity

### Technical Implementation

The MMS implementation uses:
1. **Android 5.0+ (API 21+):** SmsManager for MMS sending
2. **Fallback chain:** CarrierConfigManager → Carrier database → APN settings
3. **SecurityException handling:** Graceful fallback when APN access is denied
4. **Enhanced logging:** Detailed diagnostics for troubleshooting

This ensures maximum compatibility across Android versions and carriers.