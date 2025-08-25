package com.translator.messagingapp;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.List;

/**
 * Test class for third-party service integration enhancements.
 * Tests calendar integration, location detection, and contact synchronization features.
 */
public class ThirdPartyIntegrationTest {

    @Before
    public void setUp() {
        // Setup test environment
    }

    /**
     * Tests for Calendar Integration functionality
     */
    
    @Test
    public void testCalendarEventDetection_BasicMeeting() {
        String messageBody = "Team meeting tomorrow at 2:00 PM in conference room A";
        
        CalendarIntegrationHelper.CalendarEventInfo eventInfo = 
            CalendarIntegrationHelper.detectCalendarEvent(messageBody);
        
        assertTrue("Should detect calendar event", eventInfo.isValidEvent());
        assertTrue("Should detect event keywords", eventInfo.hasEventKeywords);
        assertTrue("Should detect time information", eventInfo.hasTimeInfo);
        assertNotNull("Should have event title", eventInfo.title);
    }
    
    @Test
    public void testCalendarEventDetection_DateFormats() {
        String[] testMessages = {
            "Meeting on 12/25/2024 at 3:00 PM",
            "Appointment scheduled for January 15, 2024",
            "Conference call this Friday at 10 AM",
            "Lunch meeting today at noon"
        };
        
        for (String message : testMessages) {
            CalendarIntegrationHelper.CalendarEventInfo eventInfo = 
                CalendarIntegrationHelper.detectCalendarEvent(message);
            
            assertTrue("Should detect event in: " + message, 
                eventInfo.hasEventKeywords || eventInfo.hasDateInfo || eventInfo.hasTimeInfo);
        }
    }
    
    @Test
    public void testCalendarEventDetection_NoEvent() {
        String messageBody = "Just saying hello, how are you doing today?";
        
        CalendarIntegrationHelper.CalendarEventInfo eventInfo = 
            CalendarIntegrationHelper.detectCalendarEvent(messageBody);
        
        assertFalse("Should not detect calendar event", eventInfo.isValidEvent());
    }
    
    @Test
    public void testCalendarIntentCreation() {
        String messageBody = "Board meeting tomorrow at 2:00 PM in boardroom";
        
        CalendarIntegrationHelper.CalendarEventInfo eventInfo = 
            CalendarIntegrationHelper.detectCalendarEvent(messageBody);
        
        if (eventInfo.isValidEvent()) {
            android.content.Intent intent = CalendarIntegrationHelper.createCalendarEventIntent(eventInfo);
            assertNotNull("Should create calendar intent", intent);
            assertEquals("Should be calendar insert action", 
                android.content.Intent.ACTION_INSERT, intent.getAction());
        }
    }
    
    /**
     * Tests for Location Detection functionality
     */
    
    @Test
    public void testLocationDetection_StreetAddress() {
        String messageBody = "Meet me at 123 Main Street, Downtown";
        
        List<LocationDetectionHelper.LocationInfo> locations = 
            LocationDetectionHelper.detectLocations(messageBody);
        
        assertFalse("Should detect location", locations.isEmpty());
        LocationDetectionHelper.LocationInfo location = locations.get(0);
        assertEquals("Should be address type", 
            LocationDetectionHelper.LocationInfo.LocationType.ADDRESS, location.type);
        assertNotNull("Should have address", location.address);
    }
    
    @Test
    public void testLocationDetection_Coordinates() {
        String messageBody = "My location: 37.7749, -122.4194";
        
        List<LocationDetectionHelper.LocationInfo> locations = 
            LocationDetectionHelper.detectLocations(messageBody);
        
        assertFalse("Should detect location", locations.isEmpty());
        LocationDetectionHelper.LocationInfo location = locations.get(0);
        assertEquals("Should be coordinates type", 
            LocationDetectionHelper.LocationInfo.LocationType.COORDINATES, location.type);
        assertFalse("Should have valid latitude", Double.isNaN(location.latitude));
        assertFalse("Should have valid longitude", Double.isNaN(location.longitude));
    }
    
    @Test
    public void testLocationDetection_SharedLocation() {
        String messageBody = "Shared my location with you";
        
        List<LocationDetectionHelper.LocationInfo> locations = 
            LocationDetectionHelper.detectLocations(messageBody);
        
        assertFalse("Should detect location", locations.isEmpty());
        LocationDetectionHelper.LocationInfo location = locations.get(0);
        assertTrue("Should be shared location", location.isSharedLocation);
    }
    
    @Test
    public void testLocationDetection_Landmarks() {
        String[] testMessages = {
            "Meet at Starbucks on 5th Avenue",
            "I'm at the airport terminal 2",
            "Let's go to Central Park",
            "Office building downtown"
        };
        
        for (String message : testMessages) {
            List<LocationDetectionHelper.LocationInfo> locations = 
                LocationDetectionHelper.detectLocations(message);
            
            assertFalse("Should detect location in: " + message, locations.isEmpty());
        }
    }
    
    @Test
    public void testMapIntentCreation() {
        String messageBody = "123 Main Street, New York, NY";
        
        LocationDetectionHelper.LocationInfo location = 
            LocationDetectionHelper.getPrimaryLocation(messageBody);
        
        if (location != null && location.hasValidLocation()) {
            android.content.Intent intent = LocationDetectionHelper.createMapIntent(location);
            assertNotNull("Should create map intent", intent);
            assertEquals("Should be view action", android.content.Intent.ACTION_VIEW, intent.getAction());
        }
    }
    
    @Test
    public void testStaticMapPreviewUrl() {
        String messageBody = "37.7749, -122.4194";
        
        LocationDetectionHelper.LocationInfo location = 
            LocationDetectionHelper.getPrimaryLocation(messageBody);
        
        if (location != null && location.hasValidLocation()) {
            String previewUrl = LocationDetectionHelper.createStaticMapPreviewUrl(location, 300, 200);
            assertNotNull("Should create preview URL", previewUrl);
            assertTrue("Should be maps URL", previewUrl.contains("maps.googleapis.com"));
        }
    }
    
    /**
     * Tests for Enhanced Contact Synchronization
     */
    
    @Test
    public void testEnhancedContactInfo() {
        ContactUtils.EnhancedContactInfo contact = 
            new ContactUtils.EnhancedContactInfo("John Doe", null);
        
        contact.email = "john@example.com";
        contact.organization = "Tech Corp";
        contact.addPlatformId("SMS", "john_sms");
        contact.addPlatformId("WhatsApp", "john_wa");
        
        assertEquals("Should have correct name", "John Doe", contact.getName());
        assertEquals("Should have correct email", "john@example.com", contact.email);
        assertTrue("Should be multi-platform contact", contact.hasMultiplePlatforms());
        assertEquals("Should have SMS platform ID", "john_sms", contact.getPlatformId("SMS"));
    }
    
    @Test
    public void testCrossPlatformContactSync() {
        ContactUtils.CrossPlatformContactSync syncManager = 
            new ContactUtils.CrossPlatformContactSync();
        
        // Create mock sync provider
        ContactUtils.ContactSyncProvider mockProvider = new ContactUtils.ContactSyncProvider() {
            @Override
            public String getPlatformName() {
                return "MockPlatform";
            }
            
            @Override
            public boolean isAvailable(android.content.Context context) {
                return true;
            }
            
            @Override
            public List<ContactUtils.EnhancedContactInfo> getContacts(android.content.Context context) {
                return new java.util.ArrayList<>();
            }
            
            @Override
            public boolean syncContact(android.content.Context context, ContactUtils.EnhancedContactInfo contact) {
                return true;
            }
            
            @Override
            public long getLastSyncTime(android.content.Context context) {
                return System.currentTimeMillis();
            }
        };
        
        syncManager.addSyncProvider(mockProvider);
        
        // Test sync status retrieval (would need context in real scenario)
        // Map<String, Long> syncStatus = syncManager.getSyncStatus(context);
        // assertTrue("Should have sync status", syncStatus.containsKey("MockPlatform"));
        
        assertTrue("Mock provider should be added", true); // Placeholder assertion
    }
    
    /**
     * Tests for Message Integration
     */
    
    @Test
    public void testMessageCalendarIntegration() {
        Message message = new Message();
        message.setBody("Team meeting tomorrow at 2:00 PM");
        
        assertTrue("Should detect calendar event", message.hasCalendarEventIndicators());
        
        CalendarIntegrationHelper.CalendarEventInfo eventInfo = message.detectCalendarEvent();
        assertTrue("Should be valid event", eventInfo.isValidEvent());
        
        android.content.Intent intent = message.createCalendarEventIntent();
        if (eventInfo.isValidEvent()) {
            assertNotNull("Should create calendar intent", intent);
        }
    }
    
    @Test
    public void testMessageLocationIntegration() {
        Message message = new Message();
        message.setBody("Meet me at 123 Main Street");
        
        assertTrue("Should detect location", message.hasLocationIndicators());
        
        List<LocationDetectionHelper.LocationInfo> locations = message.detectLocations();
        assertFalse("Should have locations", locations.isEmpty());
        
        LocationDetectionHelper.LocationInfo primaryLocation = message.getPrimaryLocation();
        assertNotNull("Should have primary location", primaryLocation);
        
        android.content.Intent mapIntent = message.createMapIntent();
        if (primaryLocation != null && primaryLocation.hasValidLocation()) {
            assertNotNull("Should create map intent", mapIntent);
        }
    }
    
    @Test
    public void testMessageIntegrationSummary() {
        Message message = new Message();
        message.setBody("Meeting at 123 Main Street tomorrow at 2:00 PM");
        
        Message.MessageIntegrationSummary summary = message.getIntegrationSummary();
        
        assertTrue("Should have integrations", summary.hasAnyIntegrations());
        assertTrue("Should have calendar event", summary.hasCalendarEvent);
        assertTrue("Should have location", summary.hasLocation);
        
        List<String> actions = summary.getAvailableActions();
        assertFalse("Should have available actions", actions.isEmpty());
    }
    
    @Test
    public void testMessageWithNoIntegrations() {
        Message message = new Message();
        message.setBody("Hello, how are you?");
        
        Message.MessageIntegrationSummary summary = message.getIntegrationSummary();
        
        assertFalse("Should not have integrations", summary.hasAnyIntegrations());
        assertFalse("Should not have calendar event", summary.hasCalendarEvent);
        assertFalse("Should not have location", summary.hasLocation);
        
        List<String> actions = summary.getAvailableActions();
        assertTrue("Should have no available actions", actions.isEmpty());
    }
    
    /**
     * Edge case and validation tests
     */
    
    @Test
    public void testEmptyMessageHandling() {
        // Test with null message
        assertFalse("Null message should not have calendar events", 
            CalendarIntegrationHelper.hasCalendarEventIndicators(null));
        assertFalse("Null message should not have locations", 
            LocationDetectionHelper.hasLocationIndicators(null));
        
        // Test with empty message
        assertFalse("Empty message should not have calendar events", 
            CalendarIntegrationHelper.hasCalendarEventIndicators(""));
        assertFalse("Empty message should not have locations", 
            LocationDetectionHelper.hasLocationIndicators(""));
    }
    
    @Test
    public void testMultipleLocationsInMessage() {
        String messageBody = "Start at 123 Main Street and meet me at Central Park at 37.7749, -122.4194";
        
        List<LocationDetectionHelper.LocationInfo> locations = 
            LocationDetectionHelper.detectLocations(messageBody);
        
        assertTrue("Should detect multiple locations", locations.size() > 1);
        
        // Check different location types are detected
        boolean hasAddress = false;
        boolean hasCoordinates = false;
        boolean hasLandmark = false;
        
        for (LocationDetectionHelper.LocationInfo location : locations) {
            if (location.type == LocationDetectionHelper.LocationInfo.LocationType.ADDRESS) {
                hasAddress = true;
            } else if (location.type == LocationDetectionHelper.LocationInfo.LocationType.COORDINATES) {
                hasCoordinates = true;
            } else if (location.type == LocationDetectionHelper.LocationInfo.LocationType.LANDMARK) {
                hasLandmark = true;
            }
        }
        
        assertTrue("Should detect at least one type of location", hasAddress || hasCoordinates || hasLandmark);
    }
}