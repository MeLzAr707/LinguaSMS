package com.translator.messagingapp;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Unit tests for TextRewritingService.
 */
@RunWith(RobolectricTestRunner.class)
public class TextRewritingServiceTest {

    private Context context;
    private TextRewritingService rewritingService;
    private CountDownLatch testLatch;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        rewritingService = new TextRewritingService(context);
    }

    @Test
    public void testFormalRewriting() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] result = {null};

        String casualText = "hey, can you help me with this thing?";

        rewritingService.rewriteText(casualText, OfflineAIService.RewriteMode.FORMAL, new OfflineAIService.RewritingCallback() {
            @Override
            public void onRewritingComplete(boolean isSuccess, String rewrittenText, String errorMessage) {
                success[0] = isSuccess;
                result[0] = rewrittenText;
                testLatch.countDown();
            }
        });

        assertTrue("Formal rewriting should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Formal rewriting should succeed", success[0]);
        assertNotNull("Rewritten text should not be null", result[0]);
        assertNotEquals("Rewritten text should be different from original", casualText, result[0]);
        
        // Check formal characteristics
        assertFalse("Formal text should not contain 'hey'", result[0].toLowerCase().contains("hey"));
        assertTrue("Formal text should be properly capitalized", 
                   Character.isUpperCase(result[0].charAt(0)));
        assertFalse("Formal text should not contain contractions", result[0].contains("'"));
    }

    @Test
    public void testCasualRewriting() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] result = {null};

        String formalText = "Hello, would you be able to assist me with this matter?";

        rewritingService.rewriteText(formalText, OfflineAIService.RewriteMode.CASUAL, new OfflineAIService.RewritingCallback() {
            @Override
            public void onRewritingComplete(boolean isSuccess, String rewrittenText, String errorMessage) {
                success[0] = isSuccess;
                result[0] = rewrittenText;
                testLatch.countDown();
            }
        });

        assertTrue("Casual rewriting should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Casual rewriting should succeed", success[0]);
        assertNotNull("Rewritten text should not be null", result[0]);
        assertNotEquals("Rewritten text should be different from original", formalText, result[0]);
        
        // Check casual characteristics - should be more informal
        String lowerResult = result[0].toLowerCase();
        assertTrue("Casual text should be less formal", 
                   lowerResult.contains("hi") || lowerResult.contains("hey") || result[0].contains("'"));
    }

    @Test
    public void testConciseRewriting() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] result = {null};

        String verboseText = "I was wondering if you might be able to help me with this particular issue that I'm having trouble with.";

        rewritingService.rewriteText(verboseText, OfflineAIService.RewriteMode.CONCISE, new OfflineAIService.RewritingCallback() {
            @Override
            public void onRewritingComplete(boolean isSuccess, String rewrittenText, String errorMessage) {
                success[0] = isSuccess;
                result[0] = rewrittenText;
                testLatch.countDown();
            }
        });

        assertTrue("Concise rewriting should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Concise rewriting should succeed", success[0]);
        assertNotNull("Rewritten text should not be null", result[0]);
        assertTrue("Concise text should be shorter", result[0].length() <= verboseText.length());
        assertFalse("Concise text should not be empty", result[0].trim().isEmpty());
    }

    @Test
    public void testElaborateRewriting() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] result = {null};

        String briefText = "Need help.";

        rewritingService.rewriteText(briefText, OfflineAIService.RewriteMode.ELABORATE, new OfflineAIService.RewritingCallback() {
            @Override
            public void onRewritingComplete(boolean isSuccess, String rewrittenText, String errorMessage) {
                success[0] = isSuccess;
                result[0] = rewrittenText;
                testLatch.countDown();
            }
        });

        assertTrue("Elaborate rewriting should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Elaborate rewriting should succeed", success[0]);
        assertNotNull("Rewritten text should not be null", result[0]);
        assertTrue("Elaborate text should be longer", result[0].length() >= briefText.length());
        assertNotEquals("Elaborate text should be different", briefText, result[0]);
    }

    @Test
    public void testPoliteRewriting() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] result = {null};

        String directText = "Do this now.";

        rewritingService.rewriteText(directText, OfflineAIService.RewriteMode.POLITE, new OfflineAIService.RewritingCallback() {
            @Override
            public void onRewritingComplete(boolean isSuccess, String rewrittenText, String errorMessage) {
                success[0] = isSuccess;
                result[0] = rewrittenText;
                testLatch.countDown();
            }
        });

        assertTrue("Polite rewriting should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Polite rewriting should succeed", success[0]);
        assertNotNull("Rewritten text should not be null", result[0]);
        assertNotEquals("Rewritten text should be different from original", directText, result[0]);
        
        // Check polite characteristics
        String lowerResult = result[0].toLowerCase();
        assertTrue("Polite text should contain courtesy words", 
                   lowerResult.contains("please") || lowerResult.contains("could") || 
                   lowerResult.contains("would") || lowerResult.contains("thank"));
    }

    @Test
    public void testAllRewriteModes() throws InterruptedException {
        String testText = "hey can you help me?";
        
        OfflineAIService.RewriteMode[] modes = {
            OfflineAIService.RewriteMode.FORMAL,
            OfflineAIService.RewriteMode.CASUAL,
            OfflineAIService.RewriteMode.CONCISE,
            OfflineAIService.RewriteMode.ELABORATE,
            OfflineAIService.RewriteMode.POLITE
        };
        
        for (OfflineAIService.RewriteMode mode : modes) {
            testLatch = new CountDownLatch(1);
            final boolean[] success = {false};
            final String[] result = {null};
            
            rewritingService.rewriteText(testText, mode, new OfflineAIService.RewritingCallback() {
                @Override
                public void onRewritingComplete(boolean isSuccess, String rewrittenText, String errorMessage) {
                    success[0] = isSuccess;
                    result[0] = rewrittenText;
                    testLatch.countDown();
                }
            });
            
            assertTrue("Rewriting should complete for mode " + mode, 
                       testLatch.await(5, TimeUnit.SECONDS));
            assertTrue("Rewriting should succeed for mode " + mode, success[0]);
            assertNotNull("Rewritten text should not be null for mode " + mode, result[0]);
            assertFalse("Rewritten text should not be empty for mode " + mode, result[0].trim().isEmpty());
        }
    }

    @Test
    public void testEmptyTextHandling() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {true};
        final String[] errorMessage = {null};

        rewritingService.rewriteText("", OfflineAIService.RewriteMode.FORMAL, new OfflineAIService.RewritingCallback() {
            @Override
            public void onRewritingComplete(boolean isSuccess, String rewrittenText, String error) {
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
    public void testNullTextHandling() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {true};
        final String[] errorMessage = {null};

        rewritingService.rewriteText(null, OfflineAIService.RewriteMode.FORMAL, new OfflineAIService.RewritingCallback() {
            @Override
            public void onRewritingComplete(boolean isSuccess, String rewrittenText, String error) {
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
    public void testContractionExpansion() throws InterruptedException {
        testLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] result = {null};

        String textWithContractions = "I'm can't won't don't";

        rewritingService.rewriteText(textWithContractions, OfflineAIService.RewriteMode.FORMAL, new OfflineAIService.RewritingCallback() {
            @Override
            public void onRewritingComplete(boolean isSuccess, String rewrittenText, String errorMessage) {
                success[0] = isSuccess;
                result[0] = rewrittenText;
                testLatch.countDown();
            }
        });

        assertTrue("Contraction expansion should complete within 5 seconds", 
                   testLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Contraction expansion should succeed", success[0]);
        assertNotNull("Result should not be null", result[0]);
        
        // Check that contractions are expanded
        assertTrue("Should expand 'I'm' to 'I am'", result[0].contains("I am"));
        assertTrue("Should expand 'can't' to 'cannot'", result[0].contains("cannot"));
        assertTrue("Should expand 'won't' to 'will not'", result[0].contains("will not"));
        assertTrue("Should expand 'don't' to 'do not'", result[0].contains("do not"));
    }
}