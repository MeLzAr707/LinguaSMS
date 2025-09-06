package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;

import java.util.List;

/**
 * Main service for managing offline AI features including summarization,
 * proofreading, smart reply, and text rewriting capabilities.
 * 
 * This service provides Google Nano-like functionality using alternative
 * offline-capable libraries and algorithms.
 */
public class OfflineAIService {
    private static final String TAG = "OfflineAIService";
    
    private final Context context;
    private final TextSummarizationService summarizationService;
    private final ProofreadingService proofreadingService;
    private final SmartReplyService smartReplyService;
    private final TextRewritingService rewritingService;
    
    public OfflineAIService(Context context) {
        this.context = context;
        this.summarizationService = new TextSummarizationService(context);
        this.proofreadingService = new ProofreadingService(context);
        this.smartReplyService = new SmartReplyService(context);
        this.rewritingService = new TextRewritingService(context);
        
        Log.d(TAG, "OfflineAIService initialized with all AI services");
    }
    
    // Summarization methods
    public void summarizeText(String text, SummarizationCallback callback) {
        summarizationService.summarizeText(text, callback);
    }
    
    public void summarizeConversation(List<String> messages, SummarizationCallback callback) {
        summarizationService.summarizeConversation(messages, callback);
    }
    
    // Proofreading methods
    public void proofreadText(String text, ProofreadingCallback callback) {
        proofreadingService.proofreadText(text, callback);
    }
    
    // Smart reply methods
    public void generateSmartReplies(String incomingMessage, String conversationContext, SmartReplyCallback callback) {
        smartReplyService.generateSmartReplies(incomingMessage, conversationContext, callback);
    }
    
    // Text rewriting methods
    public void rewriteText(String text, RewriteMode mode, RewritingCallback callback) {
        rewritingService.rewriteText(text, mode, callback);
    }
    
    // Callback interfaces
    public interface SummarizationCallback {
        void onSummarizationComplete(boolean success, String summary, String errorMessage);
    }
    
    public interface ProofreadingCallback {
        void onProofreadingComplete(boolean success, List<ProofreadingSuggestion> suggestions, String errorMessage);
    }
    
    public interface SmartReplyCallback {
        void onSmartRepliesGenerated(boolean success, List<String> replies, String errorMessage);
    }
    
    public interface RewritingCallback {
        void onRewritingComplete(boolean success, String rewrittenText, String errorMessage);
    }
    
    // Data classes
    public static class ProofreadingSuggestion {
        public final String originalText;
        public final String suggestedText;
        public final int startIndex;
        public final int endIndex;
        public final SuggestionType type;
        public final String explanation;
        
        public ProofreadingSuggestion(String originalText, String suggestedText, int startIndex, 
                                    int endIndex, SuggestionType type, String explanation) {
            this.originalText = originalText;
            this.suggestedText = suggestedText;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.type = type;
            this.explanation = explanation;
        }
    }
    
    public enum SuggestionType {
        SPELLING, GRAMMAR, STYLE, CLARITY
    }
    
    public enum RewriteMode {
        FORMAL, CASUAL, CONCISE, ELABORATE, POLITE
    }
}