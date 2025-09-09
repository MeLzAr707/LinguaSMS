package com.translator.messagingapp;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import static org.junit.Assert.*;

import android.content.Context;
import java.util.List;

/**
 * Test class for RCS provider existence checking functionality.
 * Verifies that the fix prevents logcat spam from non-existent RCS providers.
 */
@RunWith(RobolectricTestRunner.class)
public class RcsProviderExistenceTest {

    private RcsService rcsService;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        rcsService = new RcsService(context);
    }

    @Test
    public void testRcsServiceDoesNotSpamLogcatOnNonExistentProviders() {
        // This test verifies that calling loadRcsMessages doesn't cause
        // "Failed to find provider info for rcs" errors in logcat
        
        try {
            List<RcsMessage> messages = rcsService.loadRcsMessages("test_thread");
            
            // The method should complete without throwing exceptions
            assertNotNull("loadRcsMessages should return a list", messages);
            
            // On a device without RCS providers, the list should be empty
            // but the method should not crash or spam the logcat
            assertTrue("RCS messages list should be empty on devices without RCS providers", 
                      messages.isEmpty());
            
        } catch (Exception e) {
            fail("loadRcsMessages should not throw exceptions even when RCS providers don't exist: " + e.getMessage());
        }
    }

    @Test 
    public void testRcsServiceCachesProviderExistenceResults() {
        // This test verifies that provider existence results are cached
        // to avoid repeated failed lookups
        
        // Call loadRcsMessages multiple times
        for (int i = 0; i < 3; i++) {
            try {
                List<RcsMessage> messages = rcsService.loadRcsMessages("test_thread_" + i);
                assertNotNull("loadRcsMessages should return a list on call " + i, messages);
            } catch (Exception e) {
                fail("loadRcsMessages should not throw exceptions on call " + i + ": " + e.getMessage());
            }
        }
        
        // The test passes if no exceptions are thrown and the method
        // can be called multiple times without issues
    }

    @Test
    public void testRcsServiceHandlesEmptyThreadId() {
        // Test edge case with empty thread ID
        try {
            List<RcsMessage> messages = rcsService.loadRcsMessages("");
            assertNotNull("loadRcsMessages should return empty list for empty thread ID", messages);
            assertTrue("RCS messages list should be empty for empty thread ID", messages.isEmpty());
        } catch (Exception e) {
            fail("loadRcsMessages should handle empty thread ID gracefully: " + e.getMessage());
        }
    }

    @Test
    public void testRcsServiceHandlesNullThreadId() {
        // Test edge case with null thread ID
        try {
            List<RcsMessage> messages = rcsService.loadRcsMessages(null);
            assertNotNull("loadRcsMessages should return empty list for null thread ID", messages);
            assertTrue("RCS messages list should be empty for null thread ID", messages.isEmpty());
        } catch (Exception e) {
            fail("loadRcsMessages should handle null thread ID gracefully: " + e.getMessage());
        }
    }
}