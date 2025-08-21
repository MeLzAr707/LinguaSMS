package com.translator.messagingapp;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Test class to verify contact lookup URI validation.
 * This test specifically addresses the issue where empty/null phone numbers
 * cause IllegalArgumentException in contact lookup operations.
 */
public class ContactUtilsUriValidationTest {

    /**
     * Test that getContactName handles null phone numbers without throwing exceptions.
     */
    @Test
    public void testGetContactName_withNull_returnsNullWithoutException() {
        // This should not throw an IllegalArgumentException
        String result = ContactUtils.getContactName(null, null);
        assertNull("Should return null for null context and phone number", result);
    }

    /**
     * Test that getContactName handles empty phone numbers without throwing exceptions.
     */
    @Test
    public void testGetContactName_withEmptyPhoneNumber_returnsNullWithoutException() {
        // This should not throw an IllegalArgumentException
        // Note: We can't easily test with a real context in unit tests,
        // but we can verify that empty strings are handled
        String result = ContactUtils.getContactName(null, "");
        assertNull("Should return null for null context", result);
        
        result = ContactUtils.getContactName(null, "   ");
        assertNull("Should return null for null context", result);
    }

    /**
     * Test that getContactName handles whitespace-only phone numbers.
     */
    @Test
    public void testGetContactName_withWhitespacePhoneNumber_returnsNullWithoutException() {
        // Test various whitespace scenarios
        String result = ContactUtils.getContactName(null, " ");
        assertNull("Should return null for null context", result);
        
        result = ContactUtils.getContactName(null, "\t");
        assertNull("Should return null for null context", result);
        
        result = ContactUtils.getContactName(null, "\n");
        assertNull("Should return null for null context", result);
    }

    /**
     * Test getPhoneNumberVariants with null/empty inputs to ensure it doesn't
     * return variants that would cause URI issues.
     */
    @Test
    public void testPhoneNumberVariants_withNullInput_handlesGracefully() {
        // We can't access private method directly, but we can test the public
        // getContactName method which uses it internally
        
        // These calls should not throw IllegalArgumentException
        ContactUtils.getContactName(null, null);
        ContactUtils.getContactName(null, "");
        ContactUtils.getContactName(null, "   ");
        
        // If we reach here without exception, the test passes
        assertTrue("Phone number variant handling should not throw exceptions", true);
    }

    /**
     * Test that contact name lookup validation works with edge cases.
     */
    @Test
    public void testContactNameLookup_edgeCases_noExceptions() {
        // Test various edge case inputs that previously caused IllegalArgumentException
        ContactUtils.getContactName(null, "");
        ContactUtils.getContactName(null, " ");
        ContactUtils.getContactName(null, "\t\n ");
        ContactUtils.getContactName(null, null);
        
        // Test that the methods can handle these without crashing
        assertTrue("Edge case phone numbers should be handled gracefully", true);
    }

    /**
     * Test that getContactNamesForNumbers handles null/empty phone numbers properly.
     */
    @Test
    public void testGetContactNamesForNumbers_withEmptyInputs_noExceptions() {
        // Test with null context and null list
        Map<String, String> result = ContactUtils.getContactNamesForNumbers(null, null);
        assertNotNull("Should return empty map, not null", result);
        assertTrue("Should return empty map for null inputs", result.isEmpty());
        
        // Test with empty list
        List<String> emptyList = Arrays.asList();
        result = ContactUtils.getContactNamesForNumbers(null, emptyList);
        assertNotNull("Should return empty map, not null", result);
        assertTrue("Should return empty map for empty input list", result.isEmpty());
        
        // Test with list containing null/empty phone numbers
        List<String> problematicList = Arrays.asList(null, "", "   ", "\t\n");
        result = ContactUtils.getContactNamesForNumbers(null, problematicList);
        assertNotNull("Should return empty map, not null", result);
        assertTrue("Should return empty map for list with invalid phone numbers", result.isEmpty());
        
        // Test that no exceptions are thrown
        assertTrue("Batch contact lookup should handle invalid phone numbers gracefully", true);
    }

    /**
     * Test that getContactInfo handles null/empty phone numbers properly.
     */
    @Test
    public void testGetContactInfo_withEmptyInputs_noExceptions() {
        // Test various edge cases
        ContactUtils.ContactInfo result = ContactUtils.getContactInfo(null, null);
        assertNotNull("Should return ContactInfo object, not null", result);
        assertNull("Should have null name for invalid input", result.getName());
        assertNull("Should have null photo URI for invalid input", result.getPhotoUri());
        
        result = ContactUtils.getContactInfo(null, "");
        assertNotNull("Should return ContactInfo object, not null", result);
        assertNull("Should have null name for empty phone number", result.getName());
        
        result = ContactUtils.getContactInfo(null, "   ");
        assertNotNull("Should return ContactInfo object, not null", result);
        assertNull("Should have null name for whitespace phone number", result.getName());
        
        // Test that no exceptions are thrown
        assertTrue("getContactInfo should handle invalid phone numbers gracefully", true);
    }
}