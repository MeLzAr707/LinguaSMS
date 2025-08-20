package com.translator.messagingapp;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for highlighting search terms in text.
 */
public class SearchHighlightUtils {
    private static final String TAG = "SearchHighlightUtils";
    
    // Default highlight color (light yellow)
    private static final int DEFAULT_HIGHLIGHT_COLOR = Color.parseColor("#FFFF99");
    
    /**
     * Highlights all occurrences of the search query in the given text.
     * 
     * @param text The text to highlight
     * @param searchQuery The search query to highlight
     * @return A SpannableString with highlighted text, or original text if no highlighting needed
     */
    public static CharSequence highlightSearchTerms(String text, String searchQuery) {
        return highlightSearchTerms(text, searchQuery, DEFAULT_HIGHLIGHT_COLOR);
    }
    
    /**
     * Highlights all occurrences of the search query in the given text with a custom color.
     * 
     * @param text The text to highlight
     * @param searchQuery The search query to highlight
     * @param highlightColor The color to use for highlighting
     * @return A SpannableString with highlighted text, or original text if no highlighting needed
     */
    public static CharSequence highlightSearchTerms(String text, String searchQuery, int highlightColor) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            return text;
        }
        
        try {
            // Create a case-insensitive pattern for the search query
            // Escape special regex characters in the search query
            String escapedQuery = Pattern.quote(searchQuery.trim());
            Pattern pattern = Pattern.compile(escapedQuery, Pattern.CASE_INSENSITIVE);
            
            SpannableString spannableString = new SpannableString(text);
            Matcher matcher = pattern.matcher(text);
            
            // Find all matches and apply highlighting
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                
                // Apply background color span for highlighting
                BackgroundColorSpan highlightSpan = new BackgroundColorSpan(highlightColor);
                spannableString.setSpan(highlightSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            
            return spannableString;
            
        } catch (Exception e) {
            Log.e(TAG, "Error highlighting search terms", e);
            // Return original text if highlighting fails
            return text;
        }
    }
    
    /**
     * Checks if the given text contains the search query (case-insensitive).
     * 
     * @param text The text to search in
     * @param searchQuery The search query
     * @return true if the text contains the search query, false otherwise
     */
    public static boolean containsSearchQuery(String text, String searchQuery) {
        if (text == null || searchQuery == null) {
            return false;
        }
        
        return text.toLowerCase().contains(searchQuery.toLowerCase());
    }
}