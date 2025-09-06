package com.translator.messagingapp;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Unit tests for SmartReplyService.
 */
@RunWith(RobolectricTestRunner.class)
public class SmartReplyServiceTest {

    private Context context;
    private SmartReplyService smartReplyService;
    private CountDownLatch testLatch;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        smartReplyService = new SmartReplyService(context);
    }

    @Test
    public void testGreetingReply() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final List<String>[] result = new List[1];

        smartReplyService.generateSmartReplies("Hello, how are you?", "", new OfflineAIService.SmartReplyCallback() {
            @Override
            public void onSmartRepliesGenerated(boolean isSuccess, List<String> replies, String errorMessage) {
                success[0] = isSuccess;
                result[0] = replies;
                testLatch.countDown();
            }
        });

        assertTrue("Greeting reply should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Greeting reply should succeed", success[0]);
        assertNotNull("Replies should not be null", result[0]);
        assertTrue("Should generate at least one reply", result[0].size() > 0);
        
        // Check that at least one reply is greeting-related
        boolean hasGreetingReply = false;
        for (String reply : result[0]) {
            String lowerReply = reply.toLowerCase();
            if (lowerReply.contains("hi") || lowerReply.contains("hello") || lowerReply.contains("hey")) {
                hasGreetingReply = true;
                break;
            }
        }
        assertTrue("Should generate greeting-related replies", hasGreetingReply);
    }

    @Test
    public void testQuestionReply() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final List<String>[] result = new List[1];

        smartReplyService.generateSmartReplies("What time is the meeting?", "", new OfflineAIService.SmartReplyCallback() {
            @Override
            public void onSmartRepliesGenerated(boolean isSuccess, List<String> replies, String errorMessage) {
                success[0] = isSuccess;
                result[0] = replies;
                testLatch.countDown();
            }
        });

        assertTrue("Question reply should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Question reply should succeed", success[0]);
        assertNotNull("Replies should not be null", result[0]);
        assertTrue("Should generate at least one reply", result[0].size() > 0);
        
        // Check that replies are reasonable for a question
        for (String reply : result[0]) {
            assertNotNull("Reply should not be null", reply);
            assertFalse("Reply should not be empty", reply.trim().isEmpty());
            assertTrue("Reply should be reasonably short", reply.length() < 100);
        }
    }

    @Test
    public void testThanksReply() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final List<String>[] result = new List[1];

        smartReplyService.generateSmartReplies("Thank you so much for your help!", "", new OfflineAIService.SmartReplyCallback() {
            @Override
            public void onSmartRepliesGenerated(boolean isSuccess, List<String> replies, String errorMessage) {
                success[0] = isSuccess;
                result[0] = replies;
                testLatch.countDown();
            }
        });

        assertTrue("Thanks reply should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Thanks reply should succeed", success[0]);
        assertNotNull("Replies should not be null", result[0]);
        assertTrue("Should generate at least one reply", result[0].size() > 0);
        
        // Check that at least one reply acknowledges thanks
        boolean hasThankYouReply = false;
        for (String reply : result[0]) {
            String lowerReply = reply.toLowerCase();
            if (lowerReply.contains("welcome") || lowerReply.contains("no problem") || lowerReply.contains("anytime")) {
                hasThankYouReply = true;
                break;
            }
        }
        assertTrue("Should generate thank-you acknowledgment replies", hasThankYouReply);
    }

    @Test
    public void testPositiveSentimentReply() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final List<String>[] result = new List[1];

        smartReplyService.generateSmartReplies("That's awesome! I'm so excited about this!", "", new OfflineAIService.SmartReplyCallback() {
            @Override
            public void onSmartRepliesGenerated(boolean isSuccess, List<String> replies, String errorMessage) {
                success[0] = isSuccess;
                result[0] = replies;
                testLatch.countDown();
            }
        });

        assertTrue("Positive sentiment reply should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Positive sentiment reply should succeed", success[0]);
        assertNotNull("Replies should not be null", result[0]);
        assertTrue("Should generate at least one reply", result[0].size() > 0);
        
        // Check that replies match positive sentiment
        boolean hasPositiveReply = false;
        for (String reply : result[0]) {
            String lowerReply = reply.toLowerCase();
            if (lowerReply.contains("great") || lowerReply.contains("awesome") || lowerReply.contains("wonderful")) {
                hasPositiveReply = true;
                break;
            }
        }
        assertTrue("Should generate positive replies for positive messages", hasPositiveReply);
    }

    @Test
    public void testNegativeSentimentReply() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final List<String>[] result = new List[1];

        smartReplyService.generateSmartReplies("I'm really disappointed about this situation.", "", new OfflineAIService.SmartReplyCallback() {
            @Override
            public void onSmartRepliesGenerated(boolean isSuccess, List<String> replies, String errorMessage) {
                success[0] = isSuccess;
                result[0] = replies;
                testLatch.countDown();
            }
        });

        assertTrue("Negative sentiment reply should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Negative sentiment reply should succeed", success[0]);
        assertNotNull("Replies should not be null", result[0]);
        assertTrue("Should generate at least one reply", result[0].size() > 0);
        
        // Check that replies are appropriate for negative sentiment
        boolean hasEmpathyReply = false;
        for (String reply : result[0]) {
            String lowerReply = reply.toLowerCase();
            if (lowerReply.contains("sorry") || lowerReply.contains("understand") || lowerReply.contains("tough")) {
                hasEmpathyReply = true;
                break;
            }
        }
        assertTrue("Should generate empathetic replies for negative messages", hasEmpathyReply);
    }

    @Test
    public void testReplyCount() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final List<String>[] result = new List[1];

        smartReplyService.generateSmartReplies("How's your day going?", "", new OfflineAIService.SmartReplyCallback() {
            @Override
            public void onSmartRepliesGenerated(boolean isSuccess, List<String> replies, String errorMessage) {
                success[0] = isSuccess;
                result[0] = replies;
                testLatch.countDown();
            }
        });

        assertTrue("Reply generation should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Reply generation should succeed", success[0]);
        assertNotNull("Replies should not be null", result[0]);
        assertTrue("Should generate at least one reply", result[0].size() >= 1);
        assertTrue("Should generate at most 3 replies", result[0].size() <= 3);
    }

    @Test
    public void testEmptyMessageHandling() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {true};
        final String[] errorMessage = {null};

        smartReplyService.generateSmartReplies("", "", new OfflineAIService.SmartReplyCallback() {
            @Override
            public void onSmartRepliesGenerated(boolean isSuccess, List<String> replies, String error) {
                success[0] = isSuccess;
                errorMessage[0] = error;
                testLatch.countDown();
            }
        });

        assertTrue("Empty message handling should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertFalse("Empty message should return failure", success[0]);
        assertNotNull("Error message should be provided", errorMessage[0]);
    }

    @Test
    public void testNullMessageHandling() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {true};
        final String[] errorMessage = {null};

        smartReplyService.generateSmartReplies(null, "", new OfflineAIService.SmartReplyCallback() {
            @Override
            public void onSmartRepliesGenerated(boolean isSuccess, List<String> replies, String error) {
                success[0] = isSuccess;
                errorMessage[0] = error;
                testLatch.countDown();
            }
        });

        assertTrue("Null message handling should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertFalse("Null message should return failure", success[0]);
        assertNotNull("Error message should be provided", errorMessage[0]);
    }

    @Test
    public void testContextualReplies() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final List<String>[] result = new List[1];

        String message = "Are you available?";
        String context = "We were discussing the project deadline and meeting schedule.";

        smartReplyService.generateSmartReplies(message, context, new OfflineAIService.SmartReplyCallback() {
            @Override
            public void onSmartRepliesGenerated(boolean isSuccess, List<String> replies, String errorMessage) {
                success[0] = isSuccess;
                result[0] = replies;
                testLatch.countDown();
            }
        });

        assertTrue("Contextual reply should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Contextual reply should succeed", success[0]);
        assertNotNull("Replies should not be null", result[0]);
        assertTrue("Should generate at least one reply", result[0].size() > 0);
        
        // Replies should be relevant to availability questions
        for (String reply : result[0]) {
            assertNotNull("Reply should not be null", reply);
            assertFalse("Reply should not be empty", reply.trim().isEmpty());
        }
    }
}