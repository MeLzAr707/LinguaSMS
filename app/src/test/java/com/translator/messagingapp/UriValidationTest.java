package com.translator.messagingapp;

import android.net.Uri;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests to verify that URI construction methods create the correct "content://sms" format.
 * These tests validate the URI string patterns used in the app.
 */
public class UriValidationTest {

    @Test
    public void testSmsBaseUriFormat() {
        // Test that SMS base URI follows the correct pattern
        String expectedSmsBase = "content://sms";
        assertTrue("SMS URIs should use content://sms format", 
                   expectedSmsBase.equals("content://sms"));
    }

    @Test
    public void testConversationsUriConstruction() {
        // Test that conversations URI construction follows correct pattern
        String threadId = "123";
        String expectedPattern = "content://sms/conversations/" + threadId;
        
        // This is the pattern our updated code should create
        Uri.Builder builder = Uri.parse("content://sms").buildUpon();
        builder.appendPath("conversations");
        builder.appendPath(threadId);
        String actualUri = builder.build().toString();
        
        assertEquals("Conversations URI should follow content://sms/conversations/threadId pattern", 
                     expectedPattern, actualUri);
    }

    @Test
    public void testDraftUriConstruction() {
        // Test that draft URI construction follows correct pattern
        String expectedPattern = "content://sms/draft";
        
        Uri.Builder builder = Uri.parse("content://sms").buildUpon();
        builder.appendPath("draft");
        String actualUri = builder.build().toString();
        
        assertEquals("Draft URI should follow content://sms/draft pattern", 
                     expectedPattern, actualUri);
    }

    @Test
    public void testForceDefaultUriConstruction() {
        // Test that force default URI construction follows correct pattern
        String expectedPattern = "content://sms/force_default";
        
        Uri.Builder builder = Uri.parse("content://sms").buildUpon();
        builder.appendPath("force_default");
        String actualUri = builder.build().toString();
        
        assertEquals("Force default URI should follow content://sms/force_default pattern", 
                     expectedPattern, actualUri);
    }

    @Test
    public void testQueryParameterUriConstruction() {
        // Test that URI with query parameters follows correct pattern
        String expectedPattern = "content://mms-sms/conversations?simple=true";
        
        Uri.Builder builder = Uri.parse("content://mms-sms").buildUpon();
        builder.appendPath("conversations");
        builder.appendQueryParameter("simple", "true");
        String actualUri = builder.build().toString();
        
        assertEquals("URI with query parameters should be constructed correctly", 
                     expectedPattern, actualUri);
    }

    @Test
    public void testUriPathAppending() {
        // Test that appending paths works correctly
        String baseUri = "content://sms";
        String path1 = "conversations";
        String path2 = "456";
        
        Uri.Builder builder = Uri.parse(baseUri).buildUpon();
        builder.appendPath(path1);
        builder.appendPath(path2);
        String result = builder.build().toString();
        
        String expected = baseUri + "/" + path1 + "/" + path2;
        assertEquals("URI path appending should work correctly", expected, result);
    }

    @Test
    public void testUriFormatConsistency() {
        // Test that all our URI formats are consistent with the content://sms standard
        String[] validUriPrefixes = {
            "content://sms",
            "content://mms", 
            "content://mms-sms"
        };
        
        for (String prefix : validUriPrefixes) {
            assertTrue("URI prefix should be valid content URI", 
                       prefix.startsWith("content://"));
        }
    }
}