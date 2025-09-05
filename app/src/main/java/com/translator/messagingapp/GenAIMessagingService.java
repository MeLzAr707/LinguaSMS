package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Service for GenAI-powered messaging features using Google's Generative AI.
 * Provides summarization, proofreading, rewriting, and smart reply capabilities.
 */
public class GenAIMessagingService {
    private static final String TAG = "GenAIMessagingService";
    
    // Model name for Gemini
    private static final String MODEL_NAME = "gemini-1.5-flash";
    
    private final Context context;
    private final GenerativeModelFutures model;
    private final Executor executor;
    
    /**
     * Enum for rewriting tone options.
     */
    public enum RewriteTone {
        ELABORATE("Elaborate - Expands the input text with more details and descriptive language"),
        EMOJIFY("Emojify - Adds relevant emoji to the input text, making it more expressive and fun"),
        SHORTEN("Shorten - Condenses the input text to a shorter version, keeping the core message intact"),
        FRIENDLY("Friendly - Rewrites the input text to be more casual and approachable, using a conversational tone"),
        PROFESSIONAL("Professional - Rewrites the input text to be more formal and business-like, using a respectful tone"),
        REPHRASE("Rephrase - Rewrites the input text using different words and sentence structures, while maintaining the original meaning");
        
        private final String description;
        
        RewriteTone(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getDisplayName() {
            return name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
        }
    }
    
    /**
     * Callback interface for GenAI operations.
     */
    public interface GenAICallback {
        void onSuccess(String result);
        void onError(String error);
    }
    
    /**
     * Constructor for GenAIMessagingService.
     */
    public GenAIMessagingService(Context context) {
        this.context = context;
        this.executor = Executors.newCachedThreadPool();
        
        // Initialize the GenerativeModel
        // Note: In production, the API key should be configured properly
        GenerativeModel gm = new GenerativeModel(MODEL_NAME, "your-api-key-here");
        this.model = GenerativeModelFutures.from(gm);
    }
    
    /**
     * Summarizes a list of messages in a conversation thread.
     */
    public void summarizeMessages(List<Message> messages, GenAICallback callback) {
        if (messages == null || messages.isEmpty()) {
            callback.onError("No messages to summarize");
            return;
        }
        
        StringBuilder messageHistory = new StringBuilder();
        for (Message message : messages) {
            String sender = message.isIncoming() ? "Other" : "You";
            messageHistory.append(sender).append(": ").append(message.getBody()).append("\n");
        }
        
        String prompt = "Please provide a concise summary of this conversation. Focus on the main topics and key points discussed:\n\n" + messageHistory.toString();
        
        generateContent(prompt, callback);
    }
    
    /**
     * Proofreads a message for grammar, spelling, and clarity.
     */
    public void proofreadMessage(String messageText, GenAICallback callback) {
        if (messageText == null || messageText.trim().isEmpty()) {
            callback.onError("No text to proofread");
            return;
        }
        
        String prompt = "Please proofread the following message for grammar, spelling, punctuation, and clarity. " +
                       "Provide the corrected version if changes are needed, or return the original if it's already correct. " +
                       "Only return the corrected text without explanations:\n\n" + messageText;
        
        generateContent(prompt, callback);
    }
    
    /**
     * Rewrites a message with the specified tone.
     */
    public void rewriteMessage(String messageText, RewriteTone tone, GenAICallback callback) {
        if (messageText == null || messageText.trim().isEmpty()) {
            callback.onError("No text to rewrite");
            return;
        }
        
        String toneInstruction = getToneInstruction(tone);
        String prompt = "Please rewrite the following message with this style: " + toneInstruction + 
                       "\n\nOriginal message: " + messageText + 
                       "\n\nPlease provide only the rewritten message without explanations:";
        
        generateContent(prompt, callback);
    }
    
    /**
     * Generates smart reply suggestions for a conversation.
     */
    public void generateSmartReplies(List<Message> recentMessages, GenAICallback callback) {
        if (recentMessages == null || recentMessages.isEmpty()) {
            callback.onError("No messages for context");
            return;
        }
        
        StringBuilder context = new StringBuilder();
        // Use the last few messages for context
        int startIndex = Math.max(0, recentMessages.size() - 5);
        for (int i = startIndex; i < recentMessages.size(); i++) {
            Message message = recentMessages.get(i);
            String sender = message.isIncoming() ? "Other" : "You";
            context.append(sender).append(": ").append(message.getBody()).append("\n");
        }
        
        String prompt = "Based on this conversation context, suggest 3 brief, appropriate reply options that are natural and helpful. " +
                       "Provide them as a numbered list (1. 2. 3.) with each reply on a new line:\n\n" + context.toString();
        
        generateContent(prompt, callback);
    }
    
    /**
     * Gets the tone instruction for rewriting.
     */
    private String getToneInstruction(RewriteTone tone) {
        switch (tone) {
            case ELABORATE:
                return "Expand the text with more details and descriptive language, making it more comprehensive";
            case EMOJIFY:
                return "Add relevant emojis to make the text more expressive and fun, while keeping the original meaning";
            case SHORTEN:
                return "Condense the text to a shorter version while keeping the core message intact";
            case FRIENDLY:
                return "Make the text more casual and approachable with a conversational tone";
            case PROFESSIONAL:
                return "Make the text more formal and business-like with a respectful, professional tone";
            case REPHRASE:
                return "Rewrite using different words and sentence structures while maintaining the original meaning";
            default:
                return "Improve the text while maintaining its original meaning";
        }
    }
    
    /**
     * Generates content using the AI model.
     */
    private void generateContent(String prompt, GenAICallback callback) {
        try {
            Content content = new Content.Builder()
                    .addText(prompt)
                    .build();
            
            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
            
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    try {
                        String text = result.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            callback.onSuccess(text.trim());
                        } else {
                            callback.onError("Empty response from AI model");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing AI response", e);
                        callback.onError("Failed to process response: " + e.getMessage());
                    }
                }
                
                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "AI generation failed", t);
                    callback.onError("AI generation failed: " + t.getMessage());
                }
            }, executor);
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating content", e);
            callback.onError("Failed to generate content: " + e.getMessage());
        }
    }
    
    /**
     * Checks if the GenAI service is available.
     */
    public boolean isAvailable() {
        return model != null;
    }
    
    /**
     * Cleanup resources.
     */
    public void cleanup() {
        // No specific cleanup needed for the current implementation
    }
}