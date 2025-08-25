package com.translator.messagingapp;

import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for calendar integration functionality.
 * Detects dates, times, and meeting information in message content
 * and provides methods to create calendar events.
 */
public class CalendarIntegrationHelper {
    private static final String TAG = "CalendarIntegrationHelper";
    
    // Date patterns to detect various date formats
    private static final Pattern[] DATE_PATTERNS = {
        Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d{1,2})-(\\d{1,2})-(\\d{4})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(january|february|march|april|may|june|july|august|september|october|november|december)\\s+(\\d{1,2}),?\\s+(\\d{4})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\\s+(\\d{1,2}),?\\s+(\\d{4})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d{1,2})\\s+(january|february|march|april|may|june|july|august|september|october|november|december)\\s+(\\d{4})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d{1,2})\\s+(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\\s+(\\d{4})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(today|tomorrow|yesterday)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(monday|tuesday|wednesday|thursday|friday|saturday|sunday)", Pattern.CASE_INSENSITIVE)
    };
    
    // Time patterns to detect various time formats
    private static final Pattern[] TIME_PATTERNS = {
        Pattern.compile("(\\d{1,2}):(\\d{2})\\s*(am|pm)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d{1,2})\\s*(am|pm)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d{1,2}):(\\d{2})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("at\\s+(\\d{1,2}):(\\d{2})\\s*(am|pm)?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("at\\s+(\\d{1,2})\\s*(am|pm)", Pattern.CASE_INSENSITIVE)
    };
    
    // Meeting/event keywords
    private static final Pattern[] EVENT_PATTERNS = {
        Pattern.compile("(meeting|appointment|conference|call|interview|lunch|dinner|event)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(scheduled for|meet at|appointment at|conference at)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(reminder|remind me|don't forget)", Pattern.CASE_INSENSITIVE)
    };
    
    /**
     * Data class to hold detected calendar event information
     */
    public static class CalendarEventInfo {
        public String title;
        public String description;
        public Date startTime;
        public Date endTime;
        public String location;
        public boolean hasDateInfo;
        public boolean hasTimeInfo;
        public boolean hasEventKeywords;
        
        public CalendarEventInfo() {
            this.hasDateInfo = false;
            this.hasTimeInfo = false;
            this.hasEventKeywords = false;
        }
        
        public boolean isValidEvent() {
            return (hasDateInfo || hasTimeInfo) && hasEventKeywords && !TextUtils.isEmpty(title);
        }
    }
    
    /**
     * Analyzes message content for calendar event information.
     *
     * @param messageBody The message text to analyze
     * @return CalendarEventInfo object with detected information
     */
    public static CalendarEventInfo detectCalendarEvent(String messageBody) {
        CalendarEventInfo eventInfo = new CalendarEventInfo();
        
        if (TextUtils.isEmpty(messageBody)) {
            return eventInfo;
        }
        
        String text = messageBody.toLowerCase().trim();
        
        // Check for event keywords
        eventInfo.hasEventKeywords = detectEventKeywords(text);
        
        // Detect date information
        Date detectedDate = detectDate(text);
        if (detectedDate != null) {
            eventInfo.hasDateInfo = true;
            eventInfo.startTime = detectedDate;
        }
        
        // Detect time information and combine with date
        Date detectedTime = detectTime(text, detectedDate);
        if (detectedTime != null) {
            eventInfo.hasTimeInfo = true;
            if (eventInfo.startTime != null) {
                // Combine date and time
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(eventInfo.startTime);
                
                Calendar timeCalendar = Calendar.getInstance();
                timeCalendar.setTime(detectedTime);
                
                calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
                calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
                
                eventInfo.startTime = calendar.getTime();
            } else {
                eventInfo.startTime = detectedTime;
            }
        }
        
        // Set end time (default to 1 hour after start)
        if (eventInfo.startTime != null) {
            Calendar endCalendar = Calendar.getInstance();
            endCalendar.setTime(eventInfo.startTime);
            endCalendar.add(Calendar.HOUR, 1);
            eventInfo.endTime = endCalendar.getTime();
        }
        
        // Extract title and description
        if (eventInfo.hasEventKeywords) {
            eventInfo.title = extractEventTitle(messageBody);
            eventInfo.description = messageBody;
        }
        
        // Detect location information
        eventInfo.location = detectLocation(messageBody);
        
        return eventInfo;
    }
    
    /**
     * Checks if the message contains event-related keywords.
     */
    private static boolean detectEventKeywords(String text) {
        for (Pattern pattern : EVENT_PATTERNS) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Detects date information in the text.
     */
    private static Date detectDate(String text) {
        for (Pattern pattern : DATE_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                try {
                    return parseDate(matcher);
                } catch (Exception e) {
                    Log.d(TAG, "Error parsing date: " + e.getMessage());
                }
            }
        }
        return null;
    }
    
    /**
     * Detects time information in the text.
     */
    private static Date detectTime(String text, Date baseDate) {
        for (Pattern pattern : TIME_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                try {
                    return parseTime(matcher, baseDate);
                } catch (Exception e) {
                    Log.d(TAG, "Error parsing time: " + e.getMessage());
                }
            }
        }
        return null;
    }
    
    /**
     * Parses date from matcher groups.
     */
    private static Date parseDate(Matcher matcher) throws ParseException {
        String dateText = matcher.group().toLowerCase();
        Calendar calendar = Calendar.getInstance();
        
        // Handle relative dates
        if (dateText.contains("today")) {
            return calendar.getTime();
        } else if (dateText.contains("tomorrow")) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            return calendar.getTime();
        } else if (dateText.contains("yesterday")) {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            return calendar.getTime();
        }
        
        // Handle day names
        String[] dayNames = {"sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"};
        for (int i = 0; i < dayNames.length; i++) {
            if (dateText.contains(dayNames[i])) {
                int currentDay = calendar.get(Calendar.DAY_OF_WEEK) - 1;
                int targetDay = i;
                int daysToAdd = (targetDay - currentDay + 7) % 7;
                if (daysToAdd == 0) daysToAdd = 7; // Next week if same day
                calendar.add(Calendar.DAY_OF_MONTH, daysToAdd);
                return calendar.getTime();
            }
        }
        
        // Try various date formats
        SimpleDateFormat[] formats = {
            new SimpleDateFormat("MM/dd/yyyy", Locale.US),
            new SimpleDateFormat("MM-dd-yyyy", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd", Locale.US),
            new SimpleDateFormat("MMMM dd, yyyy", Locale.US),
            new SimpleDateFormat("MMM dd, yyyy", Locale.US),
            new SimpleDateFormat("dd MMMM yyyy", Locale.US),
            new SimpleDateFormat("dd MMM yyyy", Locale.US)
        };
        
        for (SimpleDateFormat format : formats) {
            try {
                return format.parse(dateText);
            } catch (ParseException e) {
                // Continue to next format
            }
        }
        
        throw new ParseException("Unable to parse date: " + dateText, 0);
    }
    
    /**
     * Parses time from matcher groups.
     */
    private static Date parseTime(Matcher matcher, Date baseDate) throws ParseException {
        String timeText = matcher.group();
        
        SimpleDateFormat[] timeFormats = {
            new SimpleDateFormat("hh:mm a", Locale.US),
            new SimpleDateFormat("h a", Locale.US),
            new SimpleDateFormat("HH:mm", Locale.US),
            new SimpleDateFormat("'at' hh:mm a", Locale.US),
            new SimpleDateFormat("'at' h a", Locale.US)
        };
        
        for (SimpleDateFormat format : timeFormats) {
            try {
                Date time = format.parse(timeText);
                if (baseDate != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(baseDate);
                    
                    Calendar timeCalendar = Calendar.getInstance();
                    timeCalendar.setTime(time);
                    
                    calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
                    calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
                    
                    return calendar.getTime();
                } else {
                    return time;
                }
            } catch (ParseException e) {
                // Continue to next format
            }
        }
        
        throw new ParseException("Unable to parse time: " + timeText, 0);
    }
    
    /**
     * Extracts event title from message text.
     */
    private static String extractEventTitle(String messageBody) {
        // Simple extraction - take first 50 characters or until newline
        String title = messageBody.trim();
        if (title.length() > 50) {
            title = title.substring(0, 50) + "...";
        }
        
        int newlineIndex = title.indexOf('\n');
        if (newlineIndex > 0) {
            title = title.substring(0, newlineIndex);
        }
        
        return title;
    }
    
    /**
     * Detects location information in message text.
     */
    private static String detectLocation(String messageBody) {
        // Simple location detection - look for "at" followed by potential location
        Pattern locationPattern = Pattern.compile("at\\s+([^\\n,]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = locationPattern.matcher(messageBody);
        
        if (matcher.find()) {
            String location = matcher.group(1).trim();
            // Remove time information if present
            location = location.replaceAll("\\d{1,2}:\\d{2}\\s*(am|pm)?", "").trim();
            if (!location.isEmpty()) {
                return location;
            }
        }
        
        return null;
    }
    
    /**
     * Creates an intent to add a calendar event.
     *
     * @param eventInfo The calendar event information
     * @return Intent to create calendar event
     */
    public static Intent createCalendarEventIntent(CalendarEventInfo eventInfo) {
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setData(CalendarContract.Events.CONTENT_URI);
        
        if (!TextUtils.isEmpty(eventInfo.title)) {
            intent.putExtra(CalendarContract.Events.TITLE, eventInfo.title);
        }
        
        if (!TextUtils.isEmpty(eventInfo.description)) {
            intent.putExtra(CalendarContract.Events.DESCRIPTION, eventInfo.description);
        }
        
        if (eventInfo.startTime != null) {
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, eventInfo.startTime.getTime());
        }
        
        if (eventInfo.endTime != null) {
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, eventInfo.endTime.getTime());
        }
        
        if (!TextUtils.isEmpty(eventInfo.location)) {
            intent.putExtra(CalendarContract.Events.EVENT_LOCATION, eventInfo.location);
        }
        
        return intent;
    }
    
    /**
     * Checks if the message content suggests a calendar event.
     *
     * @param messageBody The message text to check
     * @return true if the message appears to contain calendar event information
     */
    public static boolean hasCalendarEventIndicators(String messageBody) {
        CalendarEventInfo eventInfo = detectCalendarEvent(messageBody);
        return eventInfo.isValidEvent();
    }
}