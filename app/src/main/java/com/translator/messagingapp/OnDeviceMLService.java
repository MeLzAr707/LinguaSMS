package com.translator.messagingapp;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.nl.smartreply.SmartReply;
import com.google.mlkit.nl.smartreply.SmartReplyGenerator;
import com.google.mlkit.nl.smartreply.SmartReplySuggestion;
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult;
import com.google.mlkit.nl.smartreply.TextMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for on-device ML features using Google ML Kit.
 * Provides summarization, rewriting, and smart reply capabilities.
 */
public class OnDeviceMLService {
    private static final String TAG = "OnDeviceMLService";
    
    private final Context context;
    private final ExecutorService executorService;
    private SmartReplyGenerator smartReplyGenerator;

    /**
     * Callback interface for summarization operations.
     */
    public interface SummarizationCallback {
        void onSummarizationComplete(boolean success, String summary, String errorMessage);
    }

    /**
     * Callback interface for rewriting operations.
     */
    public interface RewritingCallback {
        void onRewritingComplete(boolean success, String rewrittenText, String errorMessage);
    }

    /**
     * Callback interface for smart reply operations.
     */
    public interface SmartReplyCallback {
        void onSmartReplyComplete(boolean success, List<String> suggestions, String errorMessage);
    }

    /**
     * Enumeration for rewriting styles.
     */
    public enum RewriteStyle {
        ELABORATE("elaborate"),
        EMOJIFY("emojify"),
        SHORTEN("shorten"),
        FRIENDLY("friendly"),
        PROFESSIONAL("professional"),
        REPHRASE("rephrase");

        private final String style;

        RewriteStyle(String style) {
            this.style = style;
        }

        public String getStyle() {
            return style;
        }
    }

    public OnDeviceMLService(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newFixedThreadPool(2);
        this.smartReplyGenerator = SmartReply.getClient();
    }

    /**
     * Summarizes the given text using on-device ML.
     *
     * @param text The text to summarize
     * @param callback The callback to receive the result
     */
    public void summarizeText(String text, SummarizationCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            if (callback != null) {
                callback.onSummarizationComplete(false, null, "Text is empty");
            }
            return;
        }

        // Check minimum text length for meaningful summarization
        if (text.trim().length() < 100) {
            if (callback != null) {
                callback.onSummarizationComplete(false, null, "Text is too short to summarize");
            }
            return;
        }

        executorService.execute(() -> {
            try {
                // For now, implement a simple extractive summarization
                // In a real implementation, you would use ML Kit's generative AI features
                // when they become available for summarization
                String summary = performExtractiveSummarization(text);
                
                if (callback != null) {
                    callback.onSummarizationComplete(true, summary, null);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during summarization", e);
                if (callback != null) {
                    callback.onSummarizationComplete(false, null, 
                        "Summarization failed: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Rewrites the given text in the specified style.
     *
     * @param text The text to rewrite
     * @param style The rewriting style
     * @param callback The callback to receive the result
     */
    public void rewriteText(String text, RewriteStyle style, RewritingCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            if (callback != null) {
                callback.onRewritingComplete(false, null, "Text is empty");
            }
            return;
        }

        executorService.execute(() -> {
            try {
                // For now, implement basic text transformations
                // In a real implementation, you would use ML Kit's generative AI features
                String rewrittenText = performTextRewriting(text, style);
                
                if (callback != null) {
                    callback.onRewritingComplete(true, rewrittenText, null);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during rewriting", e);
                if (callback != null) {
                    callback.onRewritingComplete(false, null, 
                        "Rewriting failed: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Generates smart reply suggestions based on conversation history.
     *
     * @param messages The conversation messages (last 10 messages recommended)
     * @param callback The callback to receive the result
     */
    public void generateSmartReplies(List<Message> messages, SmartReplyCallback callback) {
        if (messages == null || messages.isEmpty()) {
            if (callback != null) {
                callback.onSmartReplyComplete(false, null, "No messages provided");
            }
            return;
        }

        executorService.execute(() -> {
            try {
                // Convert messages to SmartReply format
                List<TextMessage> textMessages = convertToTextMessages(messages);
                
                if (textMessages.isEmpty()) {
                    if (callback != null) {
                        callback.onSmartReplyComplete(false, null, "No valid messages for analysis");
                    }
                    return;
                }

                // Generate smart replies using ML Kit
                Task<SmartReplySuggestionResult> task = smartReplyGenerator.suggestReplies(textMessages);
                
                task.addOnSuccessListener(result -> {
                    if (result.getStatus() == SmartReplySuggestionResult.STATUS_SUCCESS) {
                        List<String> suggestions = new ArrayList<>();
                        for (SmartReplySuggestion suggestion : result.getSuggestions()) {
                            suggestions.add(suggestion.getText());
                        }
                        
                        if (callback != null) {
                            callback.onSmartReplyComplete(true, suggestions, null);
                        }
                    } else {
                        String errorMessage = getSmartReplyErrorMessage(result.getStatus());
                        if (callback != null) {
                            callback.onSmartReplyComplete(false, null, errorMessage);
                        }
                    }
                }).addOnFailureListener(exception -> {
                    Log.e(TAG, "Smart reply generation failed", exception);
                    if (callback != null) {
                        callback.onSmartReplyComplete(false, null, 
                            "Smart reply failed: " + exception.getMessage());
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error during smart reply generation", e);
                if (callback != null) {
                    callback.onSmartReplyComplete(false, null, 
                        "Smart reply failed: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Performs extractive summarization using simple algorithms.
     * This is a placeholder implementation until ML Kit provides summarization APIs.
     */
    private String performExtractiveSummarization(String text) {
        // Simple extractive summarization: take key sentences
        String[] sentences = text.split("\\. ");
        if (sentences.length <= 3) {
            return text;
        }

        // Take first sentence, middle sentence, and last sentence as a basic summary
        StringBuilder summary = new StringBuilder();
        summary.append(sentences[0].trim());
        if (!summary.toString().endsWith(".")) {
            summary.append(".");
        }
        
        summary.append(" ");
        summary.append(sentences[sentences.length / 2].trim());
        if (!summary.toString().endsWith(".")) {
            summary.append(".");
        }
        
        summary.append(" ");
        summary.append(sentences[sentences.length - 1].trim());
        if (!summary.toString().endsWith(".")) {
            summary.append(".");
        }

        return summary.toString();
    }

    /**
     * Performs text rewriting based on the specified style.
     * This is a placeholder implementation until ML Kit provides rewriting APIs.
     */
    private String performTextRewriting(String text, RewriteStyle style) {
        switch (style) {
            case ELABORATE:
                return elaborateText(text);
            case EMOJIFY:
                return emojifyText(text);
            case SHORTEN:
                return shortenText(text);
            case FRIENDLY:
                return makeFriendly(text);
            case PROFESSIONAL:
                return makeProfessional(text);
            case REPHRASE:
                return rephraseText(text);
            default:
                return text;
        }
    }

    private String elaborateText(String text) {
        // Add descriptive words and details
        return text.replace("good", "really good")
                  .replace("bad", "quite bad")
                  .replace("nice", "very nice")
                  .replace(".", ". This is particularly noteworthy.");
    }

    private String emojifyText(String text) {
        // Add relevant emojis
        return text.replace("good", "good 😊")
                  .replace("bad", "bad 😞")
                  .replace("love", "love ❤️")
                  .replace("happy", "happy 😄")
                  .replace("sad", "sad 😢")
                  .replace("!", "! 🎉");
    }

    private String shortenText(String text) {
        // Remove unnecessary words and make more concise
        return text.replace("very ", "")
                  .replace("really ", "")
                  .replace("quite ", "")
                  .replace("I think that ", "")
                  .replace("It seems like ", "");
    }

    private String makeFriendly(String text) {
        // Make tone more casual and friendly
        return text.replace("Hello", "Hey")
                  .replace("Goodbye", "See ya")
                  .replace("Thank you", "Thanks")
                  .replace(".", "! 😊");
    }

    private String makeProfessional(String text) {
        // Make tone more formal
        return text.replace("Hey", "Hello")
                  .replace("Thanks", "Thank you")
                  .replace("gonna", "going to")
                  .replace("wanna", "want to")
                  .replace("!", ".");
    }

    private String rephraseText(String text) {
        // Simple rephrasing by changing sentence structure
        return text.replace("I am", "I'm")
                  .replace("cannot", "can't")
                  .replace("will not", "won't")
                  .replace("should not", "shouldn't");
    }

    /**
     * Converts Message objects to TextMessage objects for Smart Reply.
     */
    private List<TextMessage> convertToTextMessages(List<Message> messages) {
        List<TextMessage> textMessages = new ArrayList<>();
        
        // Take last 10 messages for context (as recommended by the issue)
        int startIndex = Math.max(0, messages.size() - 10);
        
        for (int i = startIndex; i < messages.size(); i++) {
            Message message = messages.get(i);
            if (message.getBody() != null && !message.getBody().trim().isEmpty()) {
                // Determine if message is from remote user or local user
                boolean isLocalUser = message.getType() == Message.TYPE_SENT;
                long timestamp = message.getDate();
                
                TextMessage textMessage = TextMessage.createForRemoteUser(
                    message.getBody(), 
                    timestamp, 
                    isLocalUser ? "local" : "remote"
                );
                textMessages.add(textMessage);
            }
        }
        
        return textMessages;
    }

    /**
     * Gets a user-friendly error message for Smart Reply status codes.
     */
    private String getSmartReplyErrorMessage(int status) {
        switch (status) {
            case SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE:
                return "Language not supported for smart replies";
            case SmartReplySuggestionResult.STATUS_NO_REPLY:
                return "No suitable replies found";
            default:
                return "Smart reply generation failed";
        }
    }

    /**
     * Cleanup resources.
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        Log.d(TAG, "OnDeviceMLService cleanup complete");
    }
}