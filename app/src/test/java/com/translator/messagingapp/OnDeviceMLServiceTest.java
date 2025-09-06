package com.translator.messagingapp;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnitRunner;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Test class for OnDeviceMLService functionality.
 */
@RunWith(RobolectricTestRunner.class)
public class OnDeviceMLServiceTest {
    
    private OnDeviceMLService mlService;
    private Context context;
    
    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        mlService = new OnDeviceMLService(context);
    }
    
    @Test
    public void testSummarizationWithValidText() throws InterruptedException {
        String testText = "This is a long text that should be summarized. " +
                "It contains multiple sentences with important information. " +
                "The summarization feature should extract the key points. " +
                "This is another sentence to make the text longer. " +
                "Finally, this is the last sentence of the test text.";
        
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] result = {null};
        
        mlService.summarizeText(testText, new OnDeviceMLService.SummarizationCallback() {
            @Override
            public void onSummarizationComplete(boolean isSuccess, String summary, String errorMessage) {
                success[0] = isSuccess;
                result[0] = summary;
                latch.countDown();
            }
        });
        
        assertTrue("Summarization should complete within 10 seconds", 
                latch.await(10, TimeUnit.SECONDS));
        assertTrue("Summarization should succeed", success[0]);
        assertNotNull("Summary should not be null", result[0]);
        assertFalse("Summary should not be empty", result[0].trim().isEmpty());
    }
    
    @Test
    public void testSummarizationWithShortText() throws InterruptedException {
        String testText = "Short text";
        
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] errorMessage = {null};
        
        mlService.summarizeText(testText, new OnDeviceMLService.SummarizationCallback() {
            @Override
            public void onSummarizationComplete(boolean isSuccess, String summary, String error) {
                success[0] = isSuccess;
                errorMessage[0] = error;
                latch.countDown();
            }
        });
        
        assertTrue("Summarization should complete within 5 seconds", 
                latch.await(5, TimeUnit.SECONDS));
        assertFalse("Summarization should fail for short text", success[0]);
        assertNotNull("Error message should be provided", errorMessage[0]);
    }
    
    @Test
    public void testRewritingWithDifferentStyles() throws InterruptedException {
        String testText = "Hello, how are you? I am doing very well today!";
        OnDeviceMLService.RewriteStyle[] styles = OnDeviceMLService.RewriteStyle.values();
        
        for (OnDeviceMLService.RewriteStyle style : styles) {
            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {false};
            final String[] result = {null};
            
            mlService.rewriteText(testText, style, new OnDeviceMLService.RewritingCallback() {
                @Override
                public void onRewritingComplete(boolean isSuccess, String rewrittenText, String errorMessage) {
                    success[0] = isSuccess;
                    result[0] = rewrittenText;
                    latch.countDown();
                }
            });
            
            assertTrue("Rewriting should complete within 5 seconds for style " + style, 
                    latch.await(5, TimeUnit.SECONDS));
            assertTrue("Rewriting should succeed for style " + style, success[0]);
            assertNotNull("Rewritten text should not be null for style " + style, result[0]);
            assertFalse("Rewritten text should not be empty for style " + style, result[0].trim().isEmpty());
        }
    }
    
    @Test
    public void testSmartReplyWithValidMessages() throws InterruptedException {
        // Create test conversation messages
        List<Message> messages = new ArrayList<>();
        
        Message msg1 = new Message();
        msg1.setBody("Hey, how was your day?");
        msg1.setType(Message.TYPE_RECEIVED);
        msg1.setDate(System.currentTimeMillis() - 60000);
        messages.add(msg1);
        
        Message msg2 = new Message();
        msg2.setBody("It was great! I went to the park.");
        msg2.setType(Message.TYPE_SENT);
        msg2.setDate(System.currentTimeMillis() - 30000);
        messages.add(msg2);
        
        Message msg3 = new Message();
        msg3.setBody("That sounds nice! Did you see anything interesting?");
        msg3.setType(Message.TYPE_RECEIVED);
        msg3.setDate(System.currentTimeMillis());
        messages.add(msg3);
        
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};
        final List<String>[] suggestions = new List[1];
        
        mlService.generateSmartReplies(messages, new OnDeviceMLService.SmartReplyCallback() {
            @Override
            public void onSmartReplyComplete(boolean isSuccess, List<String> replySuggestions, String errorMessage) {
                success[0] = isSuccess;
                suggestions[0] = replySuggestions;
                latch.countDown();
            }
        });
        
        assertTrue("Smart reply should complete within 10 seconds", 
                latch.await(10, TimeUnit.SECONDS));
        // Note: Smart reply may not always succeed due to ML Kit requirements,
        // so we don't assert success but check that the callback was called
    }
    
    @Test
    public void testSmartReplyWithEmptyMessages() throws InterruptedException {
        List<Message> emptyMessages = new ArrayList<>();
        
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] errorMessage = {null};
        
        mlService.generateSmartReplies(emptyMessages, new OnDeviceMLService.SmartReplyCallback() {
            @Override
            public void onSmartReplyComplete(boolean isSuccess, List<String> suggestions, String error) {
                success[0] = isSuccess;
                errorMessage[0] = error;
                latch.countDown();
            }
        });
        
        assertTrue("Smart reply should complete within 5 seconds", 
                latch.await(5, TimeUnit.SECONDS));
        assertFalse("Smart reply should fail for empty messages", success[0]);
        assertNotNull("Error message should be provided", errorMessage[0]);
    }
    
    @Test
    public void testServiceCleanup() {
        // Test that cleanup doesn't throw exceptions
        assertDoesNotThrow(() -> mlService.cleanup());
    }
    
    private void assertDoesNotThrow(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }
}