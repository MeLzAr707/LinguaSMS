package com.translator.messagingapp;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Integration test for ContactAvatarHelper with real Android context.
 */
@RunWith(AndroidJUnit4.class)
public class ContactAvatarIntegrationTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void testContactUtils_getContactColor() {
        // Test color generation for different inputs
        int color1 = ContactUtils.getContactColor("John Doe");
        int color2 = ContactUtils.getContactColor("Jane Smith");
        int color3 = ContactUtils.getContactColor("+1234567890");
        
        // Colors should be different for different inputs
        assertNotEquals(color1, color2);
        assertNotEquals(color2, color3);
        
        // Same input should produce same color
        assertEquals(color1, ContactUtils.getContactColor("John Doe"));
    }

    @Test
    public void testContactUtils_getContactInitial() {
        // Test initial generation
        assertEquals("J", ContactUtils.getContactInitial("John"));
        assertEquals("J", ContactUtils.getContactInitial("john"));
        assertEquals("1", ContactUtils.getContactInitial("1234567890"));
        assertEquals("#", ContactUtils.getContactInitial(""));
        assertEquals("#", ContactUtils.getContactInitial(null));
    }

    @Test
    public void testContactInfo_creation() {
        ContactUtils.ContactInfo info = new ContactUtils.ContactInfo("John Doe", "content://photo/1");
        assertEquals("John Doe", info.getName());
        assertEquals("content://photo/1", info.getPhotoUri());

        ContactUtils.ContactInfo emptyInfo = new ContactUtils.ContactInfo(null, null);
        assertNull(emptyInfo.getName());
        assertNull(emptyInfo.getPhotoUri());
    }
}