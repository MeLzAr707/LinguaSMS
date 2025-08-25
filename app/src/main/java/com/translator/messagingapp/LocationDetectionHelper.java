package com.translator.messagingapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for location detection and map services integration.
 * Detects addresses, coordinates, and location references in message content
 * and provides methods to interact with map services.
 */
public class LocationDetectionHelper {
    private static final String TAG = "LocationDetectionHelper";
    
    // Address patterns
    private static final Pattern[] ADDRESS_PATTERNS = {
        // Street addresses with numbers
        Pattern.compile("\\d+\\s+[A-Za-z0-9\\s]+\\s+(street|st|avenue|ave|road|rd|boulevard|blvd|drive|dr|lane|ln|way|place|pl|court|ct)\\b", Pattern.CASE_INSENSITIVE),
        // City, State ZIP patterns
        Pattern.compile("[A-Za-z\\s]+,\\s*[A-Z]{2}\\s+\\d{5}(-\\d{4})?", Pattern.CASE_INSENSITIVE),
        // International postal codes
        Pattern.compile("[A-Za-z\\s]+,\\s*[A-Za-z\\s]+\\s+[A-Z0-9]{3,10}", Pattern.CASE_INSENSITIVE),
        // Simple address indicators
        Pattern.compile("(at|@)\\s+\\d+\\s+[A-Za-z\\s]+", Pattern.CASE_INSENSITIVE)
    };
    
    // Coordinate patterns (latitude, longitude)
    private static final Pattern[] COORDINATE_PATTERNS = {
        Pattern.compile("(-?\\d+\\.\\d+),\\s*(-?\\d+\\.\\d+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("lat:?\\s*(-?\\d+\\.\\d+)\\s*,?\\s*lon:?\\s*(-?\\d+\\.\\d+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("latitude:?\\s*(-?\\d+\\.\\d+)\\s*,?\\s*longitude:?\\s*(-?\\d+\\.\\d+)", Pattern.CASE_INSENSITIVE)
    };
    
    // Location keywords and landmarks
    private static final Pattern[] LOCATION_KEYWORDS = {
        Pattern.compile("(meet\\s+at|at\\s+the|location:?|address:?)\\s*([^\\n]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(restaurant|cafe|coffee|shop|store|mall|park|school|hospital|airport|station|hotel|office|building)\\s*:?\\s*([A-Za-z0-9\\s]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\w+\\s+)*(restaurant|cafe|coffee|shop|store|mall|park|school|hospital|airport|station|hotel|office|building)", Pattern.CASE_INSENSITIVE)
    };
    
    // Shared location indicators
    private static final Pattern[] SHARED_LOCATION_PATTERNS = {
        Pattern.compile("shared\\s+location", Pattern.CASE_INSENSITIVE),
        Pattern.compile("my\\s+location", Pattern.CASE_INSENSITIVE),
        Pattern.compile("current\\s+location", Pattern.CASE_INSENSITIVE),
        Pattern.compile("i'm\\s+at", Pattern.CASE_INSENSITIVE),
        Pattern.compile("here's\\s+where\\s+i\\s+am", Pattern.CASE_INSENSITIVE)
    };
    
    /**
     * Data class to hold detected location information
     */
    public static class LocationInfo {
        public String fullText;
        public String address;
        public String coordinates;
        public String locationName;
        public String locationDescription;
        public LocationType type;
        public double latitude = Double.NaN;
        public double longitude = Double.NaN;
        public boolean isSharedLocation;
        
        public enum LocationType {
            ADDRESS,
            COORDINATES,
            LANDMARK,
            SHARED_LOCATION,
            UNKNOWN
        }
        
        public LocationInfo() {
            this.type = LocationType.UNKNOWN;
            this.isSharedLocation = false;
        }
        
        public boolean hasValidLocation() {
            return !TextUtils.isEmpty(address) || 
                   !TextUtils.isEmpty(coordinates) || 
                   (!Double.isNaN(latitude) && !Double.isNaN(longitude)) ||
                   !TextUtils.isEmpty(locationName);
        }
        
        public String getDisplayText() {
            if (!TextUtils.isEmpty(locationName)) {
                return locationName;
            } else if (!TextUtils.isEmpty(address)) {
                return address;
            } else if (!TextUtils.isEmpty(coordinates)) {
                return coordinates;
            } else if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
                return String.format("%.6f, %.6f", latitude, longitude);
            }
            return fullText;
        }
    }
    
    /**
     * Detects location information in message content.
     *
     * @param messageBody The message text to analyze
     * @return List of LocationInfo objects with detected locations
     */
    public static List<LocationInfo> detectLocations(String messageBody) {
        List<LocationInfo> locations = new ArrayList<>();
        
        if (TextUtils.isEmpty(messageBody)) {
            return locations;
        }
        
        // Check for shared location indicators
        LocationInfo sharedLocation = detectSharedLocation(messageBody);
        if (sharedLocation != null) {
            locations.add(sharedLocation);
        }
        
        // Detect coordinates
        List<LocationInfo> coordinateLocations = detectCoordinates(messageBody);
        locations.addAll(coordinateLocations);
        
        // Detect addresses
        List<LocationInfo> addressLocations = detectAddresses(messageBody);
        locations.addAll(addressLocations);
        
        // Detect landmarks and location keywords
        List<LocationInfo> landmarkLocations = detectLandmarks(messageBody);
        locations.addAll(landmarkLocations);
        
        return locations;
    }
    
    /**
     * Detects shared location indicators.
     */
    private static LocationInfo detectSharedLocation(String messageBody) {
        for (Pattern pattern : SHARED_LOCATION_PATTERNS) {
            Matcher matcher = pattern.matcher(messageBody);
            if (matcher.find()) {
                LocationInfo location = new LocationInfo();
                location.type = LocationInfo.LocationType.SHARED_LOCATION;
                location.isSharedLocation = true;
                location.fullText = matcher.group();
                location.locationDescription = "Shared location";
                
                // Try to extract additional location info after the indicator
                String remaining = messageBody.substring(matcher.end()).trim();
                if (!remaining.isEmpty()) {
                    // Look for coordinates or address in the remaining text
                    LocationInfo additionalInfo = detectCoordinatesInText(remaining);
                    if (additionalInfo == null) {
                        additionalInfo = detectAddressInText(remaining);
                    }
                    
                    if (additionalInfo != null) {
                        location.coordinates = additionalInfo.coordinates;
                        location.address = additionalInfo.address;
                        location.latitude = additionalInfo.latitude;
                        location.longitude = additionalInfo.longitude;
                    }
                }
                
                return location;
            }
        }
        return null;
    }
    
    /**
     * Detects coordinate patterns in text.
     */
    private static List<LocationInfo> detectCoordinates(String messageBody) {
        List<LocationInfo> locations = new ArrayList<>();
        
        for (Pattern pattern : COORDINATE_PATTERNS) {
            Matcher matcher = pattern.matcher(messageBody);
            while (matcher.find()) {
                LocationInfo location = new LocationInfo();
                location.type = LocationInfo.LocationType.COORDINATES;
                location.fullText = matcher.group();
                
                try {
                    if (matcher.groupCount() >= 2) {
                        location.latitude = Double.parseDouble(matcher.group(1));
                        location.longitude = Double.parseDouble(matcher.group(2));
                        location.coordinates = String.format("%.6f, %.6f", location.latitude, location.longitude);
                    }
                } catch (NumberFormatException e) {
                    Log.d(TAG, "Error parsing coordinates: " + e.getMessage());
                    continue;
                }
                
                locations.add(location);
            }
        }
        
        return locations;
    }
    
    /**
     * Detects coordinate patterns in a specific text segment.
     */
    private static LocationInfo detectCoordinatesInText(String text) {
        for (Pattern pattern : COORDINATE_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                LocationInfo location = new LocationInfo();
                location.type = LocationInfo.LocationType.COORDINATES;
                location.fullText = matcher.group();
                
                try {
                    if (matcher.groupCount() >= 2) {
                        location.latitude = Double.parseDouble(matcher.group(1));
                        location.longitude = Double.parseDouble(matcher.group(2));
                        location.coordinates = String.format("%.6f, %.6f", location.latitude, location.longitude);
                        return location;
                    }
                } catch (NumberFormatException e) {
                    Log.d(TAG, "Error parsing coordinates: " + e.getMessage());
                }
            }
        }
        return null;
    }
    
    /**
     * Detects address patterns in text.
     */
    private static List<LocationInfo> detectAddresses(String messageBody) {
        List<LocationInfo> locations = new ArrayList<>();
        
        for (Pattern pattern : ADDRESS_PATTERNS) {
            Matcher matcher = pattern.matcher(messageBody);
            while (matcher.find()) {
                LocationInfo location = new LocationInfo();
                location.type = LocationInfo.LocationType.ADDRESS;
                location.fullText = matcher.group();
                location.address = matcher.group().trim();
                
                locations.add(location);
            }
        }
        
        return locations;
    }
    
    /**
     * Detects address patterns in a specific text segment.
     */
    private static LocationInfo detectAddressInText(String text) {
        for (Pattern pattern : ADDRESS_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                LocationInfo location = new LocationInfo();
                location.type = LocationInfo.LocationType.ADDRESS;
                location.fullText = matcher.group();
                location.address = matcher.group().trim();
                return location;
            }
        }
        return null;
    }
    
    /**
     * Detects landmark and location keyword patterns.
     */
    private static List<LocationInfo> detectLandmarks(String messageBody) {
        List<LocationInfo> locations = new ArrayList<>();
        
        for (Pattern pattern : LOCATION_KEYWORDS) {
            Matcher matcher = pattern.matcher(messageBody);
            while (matcher.find()) {
                LocationInfo location = new LocationInfo();
                location.type = LocationInfo.LocationType.LANDMARK;
                location.fullText = matcher.group();
                
                if (matcher.groupCount() >= 2 && matcher.group(2) != null) {
                    location.locationName = matcher.group(2).trim();
                } else {
                    location.locationName = matcher.group().trim();
                }
                
                locations.add(location);
            }
        }
        
        return locations;
    }
    
    /**
     * Creates an intent to open a location in a map application.
     *
     * @param locationInfo The location information
     * @return Intent to open map application
     */
    public static Intent createMapIntent(LocationInfo locationInfo) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        
        if (!Double.isNaN(locationInfo.latitude) && !Double.isNaN(locationInfo.longitude)) {
            // Use coordinates
            Uri geoUri = Uri.parse(String.format("geo:%.6f,%.6f", locationInfo.latitude, locationInfo.longitude));
            intent.setData(geoUri);
        } else if (!TextUtils.isEmpty(locationInfo.address)) {
            // Use address
            Uri geoUri = Uri.parse("geo:0,0?q=" + Uri.encode(locationInfo.address));
            intent.setData(geoUri);
        } else if (!TextUtils.isEmpty(locationInfo.locationName)) {
            // Use location name
            Uri geoUri = Uri.parse("geo:0,0?q=" + Uri.encode(locationInfo.locationName));
            intent.setData(geoUri);
        } else {
            // Use full text as search query
            Uri geoUri = Uri.parse("geo:0,0?q=" + Uri.encode(locationInfo.fullText));
            intent.setData(geoUri);
        }
        
        return intent;
    }
    
    /**
     * Creates an intent to get directions to a location.
     *
     * @param locationInfo The destination location
     * @return Intent to open navigation application
     */
    public static Intent createDirectionsIntent(LocationInfo locationInfo) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        
        String destination;
        if (!Double.isNaN(locationInfo.latitude) && !Double.isNaN(locationInfo.longitude)) {
            destination = String.format("%.6f,%.6f", locationInfo.latitude, locationInfo.longitude);
        } else if (!TextUtils.isEmpty(locationInfo.address)) {
            destination = locationInfo.address;
        } else if (!TextUtils.isEmpty(locationInfo.locationName)) {
            destination = locationInfo.locationName;
        } else {
            destination = locationInfo.fullText;
        }
        
        Uri directionsUri = Uri.parse("google.navigation:q=" + Uri.encode(destination));
        intent.setData(directionsUri);
        
        // Fallback to regular maps if navigation app not available
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        return intent;
    }
    
    /**
     * Creates a Google Maps static map URL for preview.
     *
     * @param locationInfo The location information
     * @param width The desired width of the map image
     * @param height The desired height of the map image
     * @return URL string for static map image
     */
    public static String createStaticMapPreviewUrl(LocationInfo locationInfo, int width, int height) {
        StringBuilder url = new StringBuilder("https://maps.googleapis.com/maps/api/staticmap?");
        url.append("size=").append(width).append("x").append(height);
        url.append("&maptype=roadmap");
        url.append("&markers=color:red");
        
        if (!Double.isNaN(locationInfo.latitude) && !Double.isNaN(locationInfo.longitude)) {
            url.append("%7C").append(locationInfo.latitude).append(",").append(locationInfo.longitude);
            url.append("&center=").append(locationInfo.latitude).append(",").append(locationInfo.longitude);
        } else if (!TextUtils.isEmpty(locationInfo.address)) {
            String encodedAddress = Uri.encode(locationInfo.address);
            url.append("%7C").append(encodedAddress);
            url.append("&center=").append(encodedAddress);
        } else if (!TextUtils.isEmpty(locationInfo.locationName)) {
            String encodedName = Uri.encode(locationInfo.locationName);
            url.append("%7C").append(encodedName);
            url.append("&center=").append(encodedName);
        } else {
            String encodedText = Uri.encode(locationInfo.fullText);
            url.append("%7C").append(encodedText);
            url.append("&center=").append(encodedText);
        }
        
        url.append("&zoom=15");
        // Note: In production, you would need to add your Google Maps API key
        // url.append("&key=YOUR_API_KEY");
        
        return url.toString();
    }
    
    /**
     * Checks if the message content contains location information.
     *
     * @param messageBody The message text to check
     * @return true if the message appears to contain location information
     */
    public static boolean hasLocationIndicators(String messageBody) {
        List<LocationInfo> locations = detectLocations(messageBody);
        return !locations.isEmpty();
    }
    
    /**
     * Gets the primary location from a message (first detected location).
     *
     * @param messageBody The message text to analyze
     * @return LocationInfo object or null if no location detected
     */
    public static LocationInfo getPrimaryLocation(String messageBody) {
        List<LocationInfo> locations = detectLocations(messageBody);
        return locations.isEmpty() ? null : locations.get(0);
    }
    
    /**
     * Checks if the message appears to be a shared location.
     *
     * @param messageBody The message text to check
     * @return true if the message appears to be a shared location
     */
    public static boolean isSharedLocation(String messageBody) {
        LocationInfo location = detectSharedLocation(messageBody);
        return location != null && location.isSharedLocation;
    }
}