package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

/**
 * Simple test class to verify AI services functionality.
 * This is for manual testing and verification of the AI features.
 */
public class AIServiceTester {
    private static final String TAG = "AIServiceTester";
    
    private final Context context;
    private final OfflineAIService aiService;
    
    public AIServiceTester(Context context) {
        this.context = context;
        this.aiService = new OfflineAIService(context);
    }
    
    /**
     * Runs basic tests for all AI services.
     */
    public void runBasicTests() {
        Log.d(TAG, "Starting AI Service tests...");
        
        testSummarization();
        testProofreading();
        testSmartReply();
        testRewriting();
    }
    
    private void testSummarization() {
        Log.d(TAG, "Testing Summarization Service...");
        
        String testText = "This is a long message that should be summarized. It contains multiple sentences with different topics. The first topic is about testing. The second topic is about AI services. The third topic is about mobile applications.";
        
        aiService.summarizeText(testText, new OfflineAIService.SummarizationCallback() {
            @Override
            public void onSummarizationComplete(boolean success, String summary, String errorMessage) {
                if (success) {
                    Log.d(TAG, "Summarization SUCCESS: " + summary);
                } else {
                    Log.e(TAG, "Summarization FAILED: " + errorMessage);
                }
            }
        });
        
        // Test conversation summarization
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
            public void onSummarizationComplete(boolean success, String summary, String errorMessage) {
                if (success) {
                    Log.d(TAG, "Conversation Summarization SUCCESS: " + summary);
                } else {
                    Log.e(TAG, "Conversation Summarization FAILED: " + errorMessage);
                }
            }
        });
    }
    
    private void testProofreading() {
        Log.d(TAG, "Testing Proofreading Service...");
        
        String testText = "this is a test mesage with speling erors and  extra spaces. it also has grammer issues and missing capitalization.";
        
        aiService.proofreadText(testText, new OfflineAIService.ProofreadingCallback() {
            @Override
            public void onProofreadingComplete(boolean success, List<OfflineAIService.ProofreadingSuggestion> suggestions, String errorMessage) {
                if (success) {
                    Log.d(TAG, "Proofreading SUCCESS: Found " + (suggestions != null ? suggestions.size() : 0) + " suggestions");
                    if (suggestions != null) {
                        for (OfflineAIService.ProofreadingSuggestion suggestion : suggestions) {
                            Log.d(TAG, "  - " + suggestion.type + ": '" + suggestion.originalText + "' -> '" + suggestion.suggestedText + "'");
                        }
                    }
                } else {
                    Log.e(TAG, "Proofreading FAILED: " + errorMessage);
                }
            }
        });
    }
    
    private void testSmartReply() {
        Log.d(TAG, "Testing Smart Reply Service...");
        
        String incomingMessage = "Hey, how are you doing today?";
        String context = "We were talking about meeting up later.";
        
        aiService.generateSmartReplies(incomingMessage, context, new OfflineAIService.SmartReplyCallback() {
            @Override
            public void onSmartRepliesGenerated(boolean success, List<String> replies, String errorMessage) {
                if (success) {
                    Log.d(TAG, "Smart Reply SUCCESS: Generated " + (replies != null ? replies.size() : 0) + " replies");
                    if (replies != null) {
                        for (int i = 0; i < replies.size(); i++) {
                            Log.d(TAG, "  " + (i + 1) + ". " + replies.get(i));
                        }
                    }
                } else {
                    Log.e(TAG, "Smart Reply FAILED: " + errorMessage);
                }
            }
        });
    }
    
    private void testRewriting() {
        Log.d(TAG, "Testing Text Rewriting Service...");
        
        String testText = "hey can you help me with this?";
        
        // Test formal rewriting
        aiService.rewriteText(testText, OfflineAIService.RewriteMode.FORMAL, new OfflineAIService.RewritingCallback() {
            @Override
            public void onRewritingComplete(boolean success, String rewrittenText, String errorMessage) {
                if (success) {
                    Log.d(TAG, "Formal Rewriting SUCCESS: '" + testText + "' -> '" + rewrittenText + "'");
                } else {
                    Log.e(TAG, "Formal Rewriting FAILED: " + errorMessage);
                }
            }
        });
        
        // Test polite rewriting
        aiService.rewriteText(testText, OfflineAIService.RewriteMode.POLITE, new OfflineAIService.RewritingCallback() {
            @Override
            public void onRewritingComplete(boolean success, String rewrittenText, String errorMessage) {
                if (success) {
                    Log.d(TAG, "Polite Rewriting SUCCESS: '" + testText + "' -> '" + rewrittenText + "'");
                } else {
                    Log.e(TAG, "Polite Rewriting FAILED: " + errorMessage);
                }
            }
        });
    }
}