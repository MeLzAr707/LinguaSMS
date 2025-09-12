package com.translator.messagingapp;

import com.translator.messagingapp.message.*;
import com.translator.messagingapp.conversation.*;
import com.translator.messagingapp.mms.*;
import com.translator.messagingapp.contact.*;
import com.translator.messagingapp.notification.*;

import android.content.Context;
import android.provider.Telephony;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;

/**
 * Test for the thread ID fix to handle new conversations properly.
 * Tests that received messages from new contacts get proper thread IDs.
 */
@RunWith(RobolectricTestRunner.class)
public class ThreadIdFixTest {

    private MessageService messageService;
    
    @Mock
    private TranslationManager mockTranslationManager;
    
    @Mock
    private TranslationCache mockTranslationCache;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        Context context = RuntimeEnvironment.getApplication();
        messageService = new MessageService(context, mockTranslationManager, mockTranslationCache);
    }

    /**
     * Test that getThreadIdForAddress returns a valid thread ID for any address.
     * This tests the fix for issue #277 where notifications for new conversations
     * would have null thread IDs.
     */
    @Test
    public void testGetThreadIdForAddress_NewContact_ReturnsValidThreadId() {
        // Test with a new phone number that doesn't exist in SMS database
        String newContactAddress = "+1234567890";
        
        String threadId = messageService.getThreadIdForAddress(newContactAddress);
        
        // Should return a valid thread ID even for new contacts
        assertNotNull("Thread ID should not be null for new contacts", threadId);
        assertFalse("Thread ID should not be empty for new contacts", threadId.isEmpty());
        assertTrue("Thread ID should be a valid number", 
                   threadId.matches("\\d+"));
    }

    /**
     * Test that getThreadIdForAddress handles null address gracefully.
     */
    @Test
    public void testGetThreadIdForAddress_NullAddress_ReturnsNull() {
        String threadId = messageService.getThreadIdForAddress(null);
        assertNull("Thread ID should be null for null address", threadId);
    }

    /**
     * Test that getThreadIdForAddress handles empty address gracefully.
     */
    @Test
    public void testGetThreadIdForAddress_EmptyAddress_ReturnsNull() {
        String threadId = messageService.getThreadIdForAddress("");
        assertNull("Thread ID should be null for empty address", threadId);
    }

    /**
     * Test that multiple calls for the same address return the same thread ID.
     */
    @Test
    public void testGetThreadIdForAddress_SameAddress_ReturnsConsistentThreadId() {
        String testAddress = "+1987654321";
        
        String threadId1 = messageService.getThreadIdForAddress(testAddress);
        String threadId2 = messageService.getThreadIdForAddress(testAddress);
        
        assertNotNull("First thread ID should not be null", threadId1);
        assertNotNull("Second thread ID should not be null", threadId2);
        assertEquals("Thread IDs should be consistent for same address", threadId1, threadId2);
    }
}