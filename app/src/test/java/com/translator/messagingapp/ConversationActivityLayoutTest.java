package com.translator.messagingapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Test to verify that the ConversationActivity layout can be inflated without crashing.
 * This test specifically addresses the InflateException issue in activity_conversation_updated.xml.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ConversationActivityLayoutTest {

    private Context context;
    private LayoutInflater layoutInflater;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        layoutInflater = LayoutInflater.from(context);
    }

    @Test
    public void testConversationActivityLayoutInflation() {
        // This test verifies that the layout can be inflated without the InflateException
        // that was occurring due to invalid dimension value for send_button_min_height
        try {
            View view = layoutInflater.inflate(R.layout.activity_conversation_updated, null);
            assertNotNull("Layout should inflate successfully", view);
            
            // Verify that the send button exists in the layout
            View sendButton = view.findViewById(R.id.send_button);
            assertNotNull("Send button should be found in the layout", sendButton);
            
        } catch (Exception e) {
            fail("Layout inflation should not throw exception: " + e.getMessage());
        }
    }

    @Test
    public void testDimensionResourcesAreValid() {
        // Test that all dimension resources used in the layout are valid
        try {
            int messageInputMinHeight = context.getResources().getDimensionPixelSize(R.dimen.message_input_min_height);
            int messageInputPadding = context.getResources().getDimensionPixelSize(R.dimen.message_input_padding);
            int compactButtonSize = context.getResources().getDimensionPixelSize(R.dimen.compact_button_size);
            int sendButtonMinHeight = context.getResources().getDimensionPixelSize(R.dimen.send_button_min_height);
            
            assertTrue("message_input_min_height should be positive", messageInputMinHeight > 0);
            assertTrue("message_input_padding should be positive", messageInputPadding > 0);
            assertTrue("compact_button_size should be positive", compactButtonSize > 0);
            assertTrue("send_button_min_height should be positive", sendButtonMinHeight > 0);
            
        } catch (Exception e) {
            fail("Dimension resources should be accessible: " + e.getMessage());
        }
    }
}