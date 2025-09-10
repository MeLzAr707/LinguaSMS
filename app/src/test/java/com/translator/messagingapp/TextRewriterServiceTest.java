package com.translator.messagingapp;

import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Unit tests for TextRewriterService
 */
@RunWith(RobolectricTestRunner.class)  
public class TextRewriterServiceTest {
    
    private TextRewriterService textRewriterService;
    private Context context;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = RuntimeEnvironment.getApplication();
        textRewriterService = new TextRewriterService(context);
    }
    
    @Test
    public void testGetAvailableTones() {
        TextRewriterService.Tone[] tones = textRewriterService.getAvailableTones();
        
        assertNotNull("Available tones should not be null", tones);
        assertEquals("Should have 4 available tones", 4, tones.length);
        
        // Check that all expected tones are present
        boolean hasFormal = false, hasFriendly = false, hasConcise = false, hasOriginal = false;
        
        for (TextRewriterService.Tone tone : tones) {
            switch (tone) {
                case FORMAL:
                    hasFormal = true;
                    assertEquals("Formal", tone.getDisplayName());
                    assertEquals("More professional and polite", tone.getDescription());
                    break;
                case FRIENDLY:
                    hasFriendly = true;
                    assertEquals("Friendly", tone.getDisplayName());
                    assertEquals("Warm and approachable", tone.getDescription());
                    break;
                case CONCISE:
                    hasConcise = true;
                    assertEquals("Concise", tone.getDisplayName());
                    assertEquals("Brief and to the point", tone.getDescription());
                    break;
                case ORIGINAL:
                    hasOriginal = true;
                    assertEquals("Original", tone.getDisplayName());
                    assertEquals("Keep original text", tone.getDescription());
                    break;
            }
        }
        
        assertTrue("Should contain FORMAL tone", hasFormal);
        assertTrue("Should contain FRIENDLY tone", hasFriendly);
        assertTrue("Should contain CONCISE tone", hasConcise);
        assertTrue("Should contain ORIGINAL tone", hasOriginal);
    }
    
    @Test
    public void testRewriteEmptyText() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] callbackCalled = {false};
        final boolean[] success = {true};
        
        textRewriterService.rewriteText("", TextRewriterService.Tone.FORMAL, 
            new TextRewriterService.RewriteCallback() {
                @Override
                public void onRewriteComplete(boolean isSuccess, String rewrittenText, String errorMessage) {
                    callbackCalled[0] = true;
                    success[0] = isSuccess;
                    latch.countDown();
                }
            });
        
        assertTrue("Callback should be called within timeout", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Callback should have been called", callbackCalled[0]);
        assertFalse("Should fail for empty text", success[0]);
    }
    
    @Test
    public void testRewriteOriginalTone() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final String originalText = "Hello, how are you?";
        final String[] resultText = {null};
        final boolean[] success = {false};
        
        textRewriterService.rewriteText(originalText, TextRewriterService.Tone.ORIGINAL,
            new TextRewriterService.RewriteCallback() {
                @Override
                public void onRewriteComplete(boolean isSuccess, String rewrittenText, String errorMessage) {
                    success[0] = isSuccess;
                    resultText[0] = rewrittenText;
                    latch.countDown();
                }
            });
        
        assertTrue("Callback should be called within timeout", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Should succeed for original tone", success[0]);
        assertEquals("Should return original text unchanged", originalText, resultText[0]);
    }
    
    @Test
    public void testRewriteFormalTone() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final String originalText = "hey can't you help me?";
        final String[] resultText = {null};
        final boolean[] success = {false};
        
        textRewriterService.rewriteText(originalText, TextRewriterService.Tone.FORMAL,
            new TextRewriterService.RewriteCallback() {
                @Override
                public void onRewriteComplete(boolean isSuccess, String rewrittenText, String errorMessage) {
                    success[0] = isSuccess;
                    resultText[0] = rewrittenText;
                    latch.countDown();
                }
            });
        
        assertTrue("Callback should be called within timeout", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Should succeed for formal tone", success[0]);
        assertNotNull("Result text should not be null", resultText[0]);
        assertNotEquals("Result should be different from original", originalText, resultText[0]);
        
        // Check formal transformations
        assertTrue("Should expand contractions", resultText[0].contains("cannot"));
        assertTrue("Should use formal greeting", resultText[0].contains("hello"));
    }
    
    @Test
    public void testRewriteFriendlyTone() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final String originalText = "Hello. I require assistance regarding this matter.";
        final String[] resultText = {null};
        final boolean[] success = {false};
        
        textRewriterService.rewriteText(originalText, TextRewriterService.Tone.FRIENDLY,
            new TextRewriterService.RewriteCallback() {
                @Override
                public void onRewriteComplete(boolean isSuccess, String rewrittenText, String errorMessage) {
                    success[0] = isSuccess;
                    resultText[0] = rewrittenText;
                    latch.countDown();
                }
            });
        
        assertTrue("Callback should be called within timeout", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Should succeed for friendly tone", success[0]);
        assertNotNull("Result text should not be null", resultText[0]);
        assertNotEquals("Result should be different from original", originalText, resultText[0]);
        
        // Check friendly transformations
        assertTrue("Should use friendly greeting", resultText[0].toLowerCase().contains("hi"));
        assertTrue("Should use friendly words", resultText[0].toLowerCase().contains("help") || 
                  resultText[0].toLowerCase().contains("about"));
    }
    
    @Test
    public void testRewriteConciseTone() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final String originalText = "I was just wondering if you could maybe help me with this really important task.";
        final String[] resultText = {null};
        final boolean[] success = {false};
        
        textRewriterService.rewriteText(originalText, TextRewriterService.Tone.CONCISE,
            new TextRewriterService.RewriteCallback() {
                @Override
                public void onRewriteComplete(boolean isSuccess, String rewrittenText, String errorMessage) {
                    success[0] = isSuccess;
                    resultText[0] = rewrittenText;
                    latch.countDown();
                }
            });
        
        assertTrue("Callback should be called within timeout", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Should succeed for concise tone", success[0]);
        assertNotNull("Result text should not be null", resultText[0]);
        assertNotEquals("Result should be different from original", originalText, resultText[0]);
        
        // Check concise transformations - result should be shorter
        assertTrue("Result should be shorter than original", 
                  resultText[0].length() < originalText.length());
        
        // Should remove redundant phrases
        assertFalse("Should remove 'just'", resultText[0].toLowerCase().contains("just"));
        assertFalse("Should remove 'really'", resultText[0].toLowerCase().contains("really"));
    }
    
    @Test
    public void testCapitalizationRules() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final String originalText = "hello. how are you? great to hear!";
        final String[] resultText = {null};
        final boolean[] success = {false};
        
        textRewriterService.rewriteText(originalText, TextRewriterService.Tone.FORMAL,
            new TextRewriterService.RewriteCallback() {
                @Override
                public void onRewriteComplete(boolean isSuccess, String rewrittenText, String errorMessage) {
                    success[0] = isSuccess;
                    resultText[0] = rewrittenText;
                    latch.countDown();
                }
            });
        
        assertTrue("Callback should be called within timeout", latch.await(5, TimeUnit.SECONDS));
        assertTrue("Should succeed", success[0]);
        assertNotNull("Result text should not be null", resultText[0]);
        
        // Check proper capitalization
        assertTrue("First letter should be capitalized", 
                  Character.isUpperCase(resultText[0].charAt(0)));
        
        // Check capitalization after periods
        String[] sentences = resultText[0].split("[.!?]\\s+");
        for (String sentence : sentences) {
            if (!sentence.trim().isEmpty()) {
                assertTrue("Each sentence should start with capital letter",
                          Character.isUpperCase(sentence.trim().charAt(0)));
            }
        }
    }
}