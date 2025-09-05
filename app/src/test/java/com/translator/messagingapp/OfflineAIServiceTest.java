package com.translator.messagingapp;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Unit tests for OfflineAIService and its sub-services.
 */
@RunWith(RobolectricTestRunner.class)
public class OfflineAIServiceTest {

    private Context context;
    private OfflineAIService aiService;
    private CountDownLatch testLatch;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        aiService = new OfflineAIService(context);
    }

    @Test
    public void testTextSummarization() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] result = {null};

        String testText = "This is a long message that should be summarized. It contains multiple sentences with different topics. The first topic is about testing. The second topic is about AI services. The third topic is about mobile applications. This text is long enough to trigger summarization.";

        aiService.summarizeText(testText, new OfflineAIService.SummarizationCallback() {
            @Override
            public void onSummarizationComplete(boolean isSuccess, String summary, String errorMessage) {
                success[0] = isSuccess;
                result[0] = summary;
                testLatch.countDown();
            }
        });

        assertTrue("Summarization callback should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Summarization should succeed", success[0]);
        assertNotNull("Summary should not be null", result[0]);
        assertFalse("Summary should not be empty", result[0].trim().isEmpty());
    }

    @Test
    public void testConversationSummarization() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] result = {null};

        List<String> messages = Arrays.asList(
            "Hello, how are you?",
            "I'm doing well, thanks! How about you?",
            "Pretty good! I wanted to ask about the meeting tomorrow.",
            "Sure, what do you need to know?",
            "What time is it scheduled for?",
            "It's at 2 PM in the conference room."
        );

        aiService.summarizeConversation(messages, new OfflineAIService.SummarizationCallback() {
            @Override
            public void onSummarizationComplete(boolean isSuccess, String summary, String errorMessage) {
                success[0] = isSuccess;
                result[0] = summary;
                testLatch.countDown();
            }
        });

        assertTrue("Conversation summarization callback should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Conversation summarization should succeed", success[0]);
        assertNotNull("Conversation summary should not be null", result[0]);
        assertFalse("Conversation summary should not be empty", result[0].trim().isEmpty());
        assertTrue("Summary should contain 'meeting' topic", result[0].toLowerCase().contains("meeting"));
    }

    @Test
    public void testProofreading() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final List<OfflineAIService.ProofreadingSuggestion>[] result = new List[1];

        String testText = "this is a test mesage with speling erors and  extra spaces.";

        aiService.proofreadText(testText, new OfflineAIService.ProofreadingCallback() {
            @Override
            public void onProofreadingComplete(boolean isSuccess, List<OfflineAIService.ProofreadingSuggestion> suggestions, String errorMessage) {
                success[0] = isSuccess;
                result[0] = suggestions;
                testLatch.countDown();
            }
        });

        assertTrue("Proofreading callback should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Proofreading should succeed", success[0]);
        assertNotNull("Suggestions should not be null", result[0]);
        assertTrue("Should find at least one suggestion", result[0].size() > 0);
        
        // Check that suggestions contain expected types
        boolean hasSpellingOrGrammar = false;
        for (OfflineAIService.ProofreadingSuggestion suggestion : result[0]) {
            if (suggestion.type == OfflineAIService.SuggestionType.SPELLING || 
                suggestion.type == OfflineAIService.SuggestionType.GRAMMAR) {
                hasSpellingOrGrammar = true;
                break;
            }
        }
        assertTrue("Should find spelling or grammar suggestions", hasSpellingOrGrammar);
    }

    @Test
    public void testSmartReply() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final List<String>[] result = new List[1];

        String incomingMessage = "Hey, how are you doing today?";
        String context = "We were talking about meeting up later.";

        aiService.generateSmartReplies(incomingMessage, context, new OfflineAIService.SmartReplyCallback() {
            @Override
            public void onSmartRepliesGenerated(boolean isSuccess, List<String> replies, String errorMessage) {
                success[0] = isSuccess;
                result[0] = replies;
                testLatch.countDown();
            }
        });

        assertTrue("Smart reply callback should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Smart reply should succeed", success[0]);
        assertNotNull("Replies should not be null", result[0]);
        assertTrue("Should generate at least one reply", result[0].size() > 0);
        assertTrue("Should generate at most 3 replies", result[0].size() <= 3);
        
        // Check that replies are reasonable
        for (String reply : result[0]) {
            assertNotNull("Reply should not be null", reply);
            assertFalse("Reply should not be empty", reply.trim().isEmpty());
            assertTrue("Reply should be reasonably short", reply.length() < 100);
        }
    }

    @Test
    public void testTextRewriting() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] result = {null};

        String testText = "hey can you help me with this?";

        aiService.rewriteText(testText, OfflineAIService.RewriteMode.FORMAL, new OfflineAIService.RewritingCallback() {
            @Override
            public void onRewritingComplete(boolean isSuccess, String rewrittenText, String errorMessage) {
                success[0] = isSuccess;
                result[0] = rewrittenText;
                testLatch.countDown();
            }
        });

        assertTrue("Rewriting callback should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Rewriting should succeed", success[0]);
        assertNotNull("Rewritten text should not be null", result[0]);
        assertFalse("Rewritten text should not be empty", result[0].trim().isEmpty());
        assertNotEquals("Rewritten text should be different from original", testText, result[0]);
        
        // Check that formal rewriting worked
        assertFalse("Formal text should not contain 'hey'", result[0].toLowerCase().contains("hey"));
        assertTrue("Formal text should be properly capitalized", 
                   Character.isUpperCase(result[0].charAt(0)));
    }

    @Test
    public void testTextRewritingModes() throws InterruptedException {
        String testText = "hey, can you help me with this thing quickly?";
        
        // Test different rewriting modes
        OfflineAIService.RewriteMode[] modes = {
            OfflineAIService.RewriteMode.FORMAL,
            OfflineAIService.RewriteMode.CASUAL,
            OfflineAIService.RewriteMode.CONCISE,
            OfflineAIService.RewriteMode.POLITE
        };
        
        for (OfflineAIService.RewriteMode mode : modes) {
            testLatch = new CountDownLatch(1);
            final boolean[] success = {false};
            final String[] result = {null};
            
            aiService.rewriteText(testText, mode, new OfflineAIService.RewritingCallback() {
                @Override
                public void onRewritingComplete(boolean isSuccess, String rewrittenText, String errorMessage) {
                    success[0] = isSuccess;
                    result[0] = rewrittenText;
                    testLatch.countDown();
                }
            });
            
            assertTrue("Rewriting callback should complete for mode " + mode, 
                       testLatch.await(5, TimeUnit.SECONDS));
            assertTrue("Rewriting should succeed for mode " + mode, success[0]);
            assertNotNull("Rewritten text should not be null for mode " + mode, result[0]);
            assertFalse("Rewritten text should not be empty for mode " + mode, result[0].trim().isEmpty());
        }
    }

    @Test
    public void testEmptyTextHandling() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {true}; // Expect failure for empty text
        final String[] errorMessage = {null};

        aiService.summarizeText("", new OfflineAIService.SummarizationCallback() {
            @Override
            public void onSummarizationComplete(boolean isSuccess, String summary, String error) {
                success[0] = isSuccess;
                errorMessage[0] = error;
                testLatch.countDown();
            }
        });

        assertTrue("Empty text callback should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertFalse("Empty text should fail", success[0]);
        assertNotNull("Error message should be provided", errorMessage[0]);
    }

    @Test
    public void testShortTextHandling() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {true}; // Expect failure for very short text
        final String[] errorMessage = {null};

        aiService.summarizeText("Hi", new OfflineAIService.SummarizationCallback() {
            @Override
            public void onSummarizationComplete(boolean isSuccess, String summary, String error) {
                success[0] = isSuccess;
                errorMessage[0] = error;
                testLatch.countDown();
            }
        });

        assertTrue("Short text callback should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertFalse("Very short text should fail", success[0]);
        assertNotNull("Error message should be provided", errorMessage[0]);
    }
}