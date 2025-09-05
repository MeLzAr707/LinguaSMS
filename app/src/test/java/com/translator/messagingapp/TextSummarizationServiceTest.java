package com.translator.messagingapp;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Unit tests for TextSummarizationService.
 */
@RunWith(RobolectricTestRunner.class)
public class TextSummarizationServiceTest {

    private Context context;
    private TextSummarizationService summarizationService;
    private CountDownLatch testLatch;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        summarizationService = new TextSummarizationService(context);
    }

    @Test
    public void testBasicSummarization() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] result = {null};

        String longText = "This is the first sentence about artificial intelligence. " +
                         "The second sentence discusses machine learning algorithms. " +
                         "The third sentence talks about natural language processing. " +
                         "The fourth sentence mentions deep learning neural networks. " +
                         "The fifth sentence covers computer vision applications. " +
                         "The sixth sentence explores robotics and automation.";

        summarizationService.summarizeText(longText, new OfflineAIService.SummarizationCallback() {
            @Override
            public void onSummarizationComplete(boolean isSuccess, String summary, String errorMessage) {
                success[0] = isSuccess;
                result[0] = summary;
                testLatch.countDown();
            }
        });

        assertTrue("Summarization should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Summarization should succeed", success[0]);
        assertNotNull("Summary should not be null", result[0]);
        assertTrue("Summary should be shorter than original", result[0].length() < longText.length());
    }

    @Test
    public void testConversationSummarization() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] result = {null};

        List<String> conversation = Arrays.asList(
            "Hey, did you finish the project report?",
            "Yes, I completed it yesterday evening.",
            "That's great! What about the presentation slides?",
            "I'm working on them now. Should be done by tonight.",
            "Perfect. The meeting is scheduled for tomorrow at 10 AM.",
            "Got it. I'll have everything ready by then."
        );

        summarizationService.summarizeConversation(conversation, new OfflineAIService.SummarizationCallback() {
            @Override
            public void onSummarizationComplete(boolean isSuccess, String summary, String errorMessage) {
                success[0] = isSuccess;
                result[0] = summary;
                testLatch.countDown();
            }
        });

        assertTrue("Conversation summarization should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Conversation summarization should succeed", success[0]);
        assertNotNull("Summary should not be null", result[0]);
        
        String summaryLower = result[0].toLowerCase();
        assertTrue("Summary should mention project", summaryLower.contains("project"));
        assertTrue("Summary should mention meeting", summaryLower.contains("meeting"));
    }

    @Test
    public void testEmptyMessageHandling() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {true};
        final String[] errorMessage = {null};

        summarizationService.summarizeText("", new OfflineAIService.SummarizationCallback() {
            @Override
            public void onSummarizationComplete(boolean isSuccess, String summary, String error) {
                success[0] = isSuccess;
                errorMessage[0] = error;
                testLatch.countDown();
            }
        });

        assertTrue("Empty text handling should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertFalse("Empty text should return failure", success[0]);
        assertNotNull("Error message should be provided", errorMessage[0]);
    }

    @Test
    public void testShortTextHandling() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] result = {null};

        // Text that's already short enough
        String shortText = "This is a short message.";

        summarizationService.summarizeText(shortText, new OfflineAIService.SummarizationCallback() {
            @Override
            public void onSummarizationComplete(boolean isSuccess, String summary, String errorMessage) {
                success[0] = isSuccess;
                result[0] = summary;
                testLatch.countDown();
            }
        });

        assertTrue("Short text handling should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        // For very short text, it should either succeed with the original text or fail gracefully
        if (success[0]) {
            assertNotNull("Summary should not be null", result[0]);
            assertFalse("Summary should not be empty", result[0].trim().isEmpty());
        }
    }

    @Test
    public void testNullMessageHandling() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {true};
        final String[] errorMessage = {null};

        summarizationService.summarizeText(null, new OfflineAIService.SummarizationCallback() {
            @Override
            public void onSummarizationComplete(boolean isSuccess, String summary, String error) {
                success[0] = isSuccess;
                errorMessage[0] = error;
                testLatch.countDown();
            }
        });

        assertTrue("Null text handling should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertFalse("Null text should return failure", success[0]);
        assertNotNull("Error message should be provided", errorMessage[0]);
    }

    @Test
    public void testEmptyConversationHandling() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {true};
        final String[] errorMessage = {null};

        summarizationService.summarizeConversation(Arrays.asList(), new OfflineAIService.SummarizationCallback() {
            @Override
            public void onSummarizationComplete(boolean isSuccess, String summary, String error) {
                success[0] = isSuccess;
                errorMessage[0] = error;
                testLatch.countDown();
            }
        });

        assertTrue("Empty conversation handling should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertFalse("Empty conversation should return failure", success[0]);
        assertNotNull("Error message should be provided", errorMessage[0]);
    }
}