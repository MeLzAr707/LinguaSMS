package com.translator.messagingapp;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for search optimization and text highlighting functionality.
 */
public class SearchOptimizationTest {

    @Test
    public void testHighlightSearchTerms_basicHighlighting() {
        String text = "Hello world, this is a test message";
        String searchQuery = "test";
        
        CharSequence result = SearchHighlightUtils.highlightSearchTerms(text, searchQuery);
        
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be a SpannableString", result instanceof SpannableString);
        
        SpannableString spannable = (SpannableString) result;
        BackgroundColorSpan[] spans = spannable.getSpans(0, spannable.length(), BackgroundColorSpan.class);
        
        assertTrue("Should have at least one highlight span", spans.length > 0);
    }

    @Test
    public void testHighlightSearchTerms_caseInsensitive() {
        String text = "Hello World Test MESSAGE";
        String searchQuery = "test";
        
        CharSequence result = SearchHighlightUtils.highlightSearchTerms(text, searchQuery);
        
        assertNotNull("Result should not be null", result);
        assertTrue("Result should be a SpannableString", result instanceof SpannableString);
        
        // The highlight should work case-insensitively
        SpannableString spannable = (SpannableString) result;
        BackgroundColorSpan[] spans = spannable.getSpans(0, spannable.length(), BackgroundColorSpan.class);
        
        assertTrue("Should highlight case-insensitive matches", spans.length > 0);
    }

    @Test
    public void testHighlightSearchTerms_emptyQuery() {
        String text = "Hello world";
        String searchQuery = "";
        
        CharSequence result = SearchHighlightUtils.highlightSearchTerms(text, searchQuery);
        
        assertEquals("Should return original text for empty query", text, result);
    }

    @Test
    public void testHighlightSearchTerms_nullQuery() {
        String text = "Hello world";
        String searchQuery = null;
        
        CharSequence result = SearchHighlightUtils.highlightSearchTerms(text, searchQuery);
        
        assertEquals("Should return original text for null query", text, result);
    }

    @Test
    public void testHighlightSearchTerms_nullText() {
        String text = null;
        String searchQuery = "test";
        
        CharSequence result = SearchHighlightUtils.highlightSearchTerms(text, searchQuery);
        
        assertNull("Should return null for null text", result);
    }

    @Test
    public void testHighlightSearchTerms_noMatches() {
        String text = "Hello world";
        String searchQuery = "xyz";
        
        CharSequence result = SearchHighlightUtils.highlightSearchTerms(text, searchQuery);
        
        assertNotNull("Result should not be null", result);
        // Should still return SpannableString even with no matches
        assertTrue("Result should be a SpannableString", result instanceof SpannableString);
        
        SpannableString spannable = (SpannableString) result;
        BackgroundColorSpan[] spans = spannable.getSpans(0, spannable.length(), BackgroundColorSpan.class);
        
        assertEquals("Should have no highlight spans for no matches", 0, spans.length);
    }

    @Test
    public void testContainsSearchQuery_basic() {
        assertTrue("Should find exact match", 
            SearchHighlightUtils.containsSearchQuery("Hello world", "world"));
        
        assertTrue("Should find case-insensitive match", 
            SearchHighlightUtils.containsSearchQuery("Hello World", "world"));
        
        assertFalse("Should not find non-existent text", 
            SearchHighlightUtils.containsSearchQuery("Hello world", "xyz"));
    }

    @Test
    public void testContainsSearchQuery_nullValues() {
        assertFalse("Should handle null text", 
            SearchHighlightUtils.containsSearchQuery(null, "test"));
        
        assertFalse("Should handle null query", 
            SearchHighlightUtils.containsSearchQuery("Hello world", null));
        
        assertFalse("Should handle both null", 
            SearchHighlightUtils.containsSearchQuery(null, null));
    }

    @Test
    public void testSearchPerformanceOptimization() {
        // Test that search operations are reasonably fast
        long startTime = System.currentTimeMillis();
        
        String longText = "This is a long message with many words that could be searched. ".repeat(100);
        String query = "searched";
        
        CharSequence result = SearchHighlightUtils.highlightSearchTerms(longText, query);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue("Search highlighting should complete within reasonable time (< 100ms)", 
            duration < 100);
        
        assertNotNull("Result should not be null for long text", result);
    }
}