package com.translator.messagingapp;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for ContactUtils to verify contact avatar generation logic.
 */
public class ContactUtilsTest {

    @Test
    public void getContactInitial_withName_returnsFirstLetter() {
        assertEquals("J", ContactUtils.getContactInitial("John Doe"));
        assertEquals("A", ContactUtils.getContactInitial("alice"));
        assertEquals("M", ContactUtils.getContactInitial("Mary Jane Watson"));
    }

    @Test
    public void getContactInitial_withPhoneNumber_returnsFirstDigit() {
        assertEquals("1", ContactUtils.getContactInitial("123-456-7890"));
        assertEquals("5", ContactUtils.getContactInitial("555-0123"));
        assertEquals("9", ContactUtils.getContactInitial("9876543210"));
    }

    @Test
    public void getContactInitial_withEmptyOrNull_returnsHash() {
        assertEquals("#", ContactUtils.getContactInitial(""));
        assertEquals("#", ContactUtils.getContactInitial(null));
        assertEquals("#", ContactUtils.getContactInitial("   "));
    }

    @Test
    public void getContactInitial_withSpecialCharacters_returnsHash() {
        assertEquals("#", ContactUtils.getContactInitial("@#$%"));
        assertEquals("#", ContactUtils.getContactInitial("***"));
        assertEquals("#", ContactUtils.getContactInitial("---"));
    }

    @Test
    public void getContactColor_withSameName_returnsSameColor() {
        String name = "John Doe";
        int color1 = ContactUtils.getContactColor(name);
        int color2 = ContactUtils.getContactColor(name);
        assertEquals(color1, color2);
    }

    @Test
    public void getContactColor_withDifferentNames_returnsDifferentColors() {
        int color1 = ContactUtils.getContactColor("John Doe");
        int color2 = ContactUtils.getContactColor("Jane Smith");
        assertNotEquals(color1, color2);
    }

    @Test
    public void getContactColor_withEmptyOrNull_returnsDefaultGray() {
        assertEquals(0xFF9E9E9E, ContactUtils.getContactColor(""));
        assertEquals(0xFF9E9E9E, ContactUtils.getContactColor(null));
    }

    @Test
    public void getContactInitial_caseSensitivity() {
        assertEquals("J", ContactUtils.getContactInitial("john"));
        assertEquals("J", ContactUtils.getContactInitial("JOHN"));
        assertEquals("J", ContactUtils.getContactInitial("John"));
    }
}