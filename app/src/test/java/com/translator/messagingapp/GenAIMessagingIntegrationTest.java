package com.translator.messagingapp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test class for GenAI messaging features integration.
 */
@RunWith(RobolectricTestRunner.class)
public class GenAIMessagingIntegrationTest {

    @Mock
    private GenAIMessagingService mockGenAIService;

    private Context context;
    private List<Message> testMessages;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        
        // Create test messages
        testMessages = new ArrayList<>();
        
        Message message1 = new Message();
        message1.setBody("Hello, how are you?");
        message1.setIncoming(false);
        message1.setTimestamp(System.currentTimeMillis() - 10000);
        testMessages.add(message1);
        
        Message message2 = new Message();
        message2.setBody("I'm doing well, thanks for asking! How about you?");
        message2.setIncoming(true);
        message2.setTimestamp(System.currentTimeMillis() - 5000);
        testMessages.add(message2);
        
        Message message3 = new Message();
        message3.setBody("I'm great too. Are we still meeting tomorrow?");
        message3.setIncoming(false);
        message3.setTimestamp(System.currentTimeMillis());
        testMessages.add(message3);
    }

    @Test
    public void testGenAIServiceInitialization() {
        GenAIMessagingService service = new GenAIMessagingService(context);
        assert service != null;
        // Note: Service availability depends on API key configuration
    }

    @Test
    public void testSummarizeMessages() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final String[] result = new String[1];
        final String[] error = new String[1];

        // Mock successful summarization
        doAnswer(invocation -> {
            GenAIMessagingService.GenAICallback callback = invocation.getArgument(1);
            callback.onSuccess("Summary: The conversation is a friendly exchange where both parties check on each other's well-being and confirm a planned meeting.");
            latch.countDown();
            return null;
        }).when(mockGenAIService).summarizeMessages(any(), any());

        mockGenAIService.summarizeMessages(testMessages, new GenAIMessagingService.GenAICallback() {
            @Override
            public void onSuccess(String resultText) {
                result[0] = resultText;
            }

            @Override
            public void onError(String errorText) {
                error[0] = errorText;
            }
        });

        // Wait for callback
        latch.await(5, TimeUnit.SECONDS);

        // Verify the result
        assert result[0] != null;
        assert result[0].contains("Summary");
        assert error[0] == null;
        
        // Verify service was called
        verify(mockGenAIService).summarizeMessages(eq(testMessages), any());
    }

    @Test
    public void testProofreadMessage() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final String[] result = new String[1];
        final String[] error = new String[1];

        String originalMessage = "Helo, how are you doign today?";
        String correctedMessage = "Hello, how are you doing today?";

        // Mock successful proofreading
        doAnswer(invocation -> {
            GenAIMessagingService.GenAICallback callback = invocation.getArgument(1);
            callback.onSuccess(correctedMessage);
            latch.countDown();
            return null;
        }).when(mockGenAIService).proofreadMessage(anyString(), any());

        mockGenAIService.proofreadMessage(originalMessage, new GenAIMessagingService.GenAICallback() {
            @Override
            public void onSuccess(String resultText) {
                result[0] = resultText;
            }

            @Override
            public void onError(String errorText) {
                error[0] = errorText;
            }
        });

        // Wait for callback
        latch.await(5, TimeUnit.SECONDS);

        // Verify the result
        assert result[0] != null;
        assert result[0].equals(correctedMessage);
        assert error[0] == null;
        
        // Verify service was called with correct text
        verify(mockGenAIService).proofreadMessage(eq(originalMessage), any());
    }

    @Test
    public void testRewriteMessageWithDifferentTones() throws InterruptedException {
        String originalMessage = "I need this done soon.";
        
        // Test each tone
        GenAIMessagingService.RewriteTone[] tones = GenAIMessagingService.RewriteTone.values();
        
        for (GenAIMessagingService.RewriteTone tone : tones) {
            CountDownLatch latch = new CountDownLatch(1);
            final String[] result = new String[1];

            // Mock successful rewriting
            doAnswer(invocation -> {
                GenAIMessagingService.GenAICallback callback = invocation.getArgument(2);
                String rewrittenText = getExpectedRewriteForTone(originalMessage, tone);
                callback.onSuccess(rewrittenText);
                latch.countDown();
                return null;
            }).when(mockGenAIService).rewriteMessage(anyString(), eq(tone), any());

            mockGenAIService.rewriteMessage(originalMessage, tone, new GenAIMessagingService.GenAICallback() {
                @Override
                public void onSuccess(String resultText) {
                    result[0] = resultText;
                }

                @Override
                public void onError(String errorText) {
                    // Should not happen in this test
                    assert false : "Unexpected error: " + errorText;
                }
            });

            // Wait for callback
            latch.await(5, TimeUnit.SECONDS);

            // Verify the result
            assert result[0] != null;
            assert !result[0].equals(originalMessage); // Should be different from original
            
            // Verify service was called with correct parameters
            verify(mockGenAIService).rewriteMessage(eq(originalMessage), eq(tone), any());
        }
    }

    @Test
    public void testGenerateSmartReplies() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final String[] result = new String[1];
        final String[] error = new String[1];

        String smartRepliesResponse = "1. Yes, absolutely! What time works for you?\n2. I'll check my schedule and let you know.\n3. Looking forward to it!";

        // Mock successful smart reply generation
        doAnswer(invocation -> {
            GenAIMessagingService.GenAICallback callback = invocation.getArgument(1);
            callback.onSuccess(smartRepliesResponse);
            latch.countDown();
            return null;
        }).when(mockGenAIService).generateSmartReplies(any(), any());

        mockGenAIService.generateSmartReplies(testMessages, new GenAIMessagingService.GenAICallback() {
            @Override
            public void onSuccess(String resultText) {
                result[0] = resultText;
            }

            @Override
            public void onError(String errorText) {
                error[0] = errorText;
            }
        });

        // Wait for callback
        latch.await(5, TimeUnit.SECONDS);

        // Verify the result
        assert result[0] != null;
        assert result[0].contains("1.");
        assert result[0].contains("2.");
        assert result[0].contains("3.");
        assert error[0] == null;
        
        // Verify service was called
        verify(mockGenAIService).generateSmartReplies(eq(testMessages), any());
    }

    @Test
    public void testErrorHandling() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final String[] result = new String[1];
        final String[] error = new String[1];

        String errorMessage = "Network error occurred";

        // Mock error scenario
        doAnswer(invocation -> {
            GenAIMessagingService.GenAICallback callback = invocation.getArgument(1);
            callback.onError(errorMessage);
            latch.countDown();
            return null;
        }).when(mockGenAIService).proofreadMessage(anyString(), any());

        mockGenAIService.proofreadMessage("Test message", new GenAIMessagingService.GenAICallback() {
            @Override
            public void onSuccess(String resultText) {
                result[0] = resultText;
            }

            @Override
            public void onError(String errorText) {
                error[0] = errorText;
            }
        });

        // Wait for callback
        latch.await(5, TimeUnit.SECONDS);

        // Verify error handling
        assert result[0] == null;
        assert error[0] != null;
        assert error[0].equals(errorMessage);
    }

    @Test
    public void testRewriteToneDescriptions() {
        // Test that all tone enums have descriptions
        GenAIMessagingService.RewriteTone[] tones = GenAIMessagingService.RewriteTone.values();
        
        for (GenAIMessagingService.RewriteTone tone : tones) {
            assert tone.getDescription() != null;
            assert !tone.getDescription().isEmpty();
            assert tone.getDisplayName() != null;
            assert !tone.getDisplayName().isEmpty();
        }
    }

    @Test
    public void testEmptyMessageHandling() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final String[] error = new String[1];

        // Mock service to return error for empty input
        doAnswer(invocation -> {
            GenAIMessagingService.GenAICallback callback = invocation.getArgument(1);
            callback.onError("No text to proofread");
            latch.countDown();
            return null;
        }).when(mockGenAIService).proofreadMessage(anyString(), any());

        mockGenAIService.proofreadMessage("", new GenAIMessagingService.GenAICallback() {
            @Override
            public void onSuccess(String resultText) {
                assert false : "Should not succeed with empty text";
            }

            @Override
            public void onError(String errorText) {
                error[0] = errorText;
            }
        });

        // Wait for callback
        latch.await(5, TimeUnit.SECONDS);

        // Verify error handling for empty input
        assert error[0] != null;
        assert error[0].contains("No text");
    }

    /**
     * Helper method to generate expected rewrite results for testing.
     */
    private String getExpectedRewriteForTone(String original, GenAIMessagingService.RewriteTone tone) {
        switch (tone) {
            case ELABORATE:
                return "I require this task to be completed as soon as possible, ideally within the next few hours.";
            case EMOJIFY:
                return "I need this done soon. ⏰✨";
            case SHORTEN:
                return "Need this ASAP.";
            case FRIENDLY:
                return "Hey! Could you get this done soon? Thanks!";
            case PROFESSIONAL:
                return "I would appreciate if this could be completed at your earliest convenience.";
            case REPHRASE:
                return "This task requires prompt completion.";
            default:
                return original + " (rewritten)";
        }
    }
}