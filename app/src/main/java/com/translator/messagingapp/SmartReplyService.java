package com.translator.messagingapp;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Smart reply service that generates context-aware response suggestions
 * using template-based approaches and sentiment analysis.
 */
public class SmartReplyService {
    private static final String TAG = "SmartReplyService";
    private static final int MAX_REPLIES = 3;
    
    private final Context context;
    private final ExecutorService executorService;
    private final LanguageDetectionService languageDetectionService;
    private final Random random;
    
    // Question patterns
    private static final Pattern QUESTION_PATTERNS = Pattern.compile(
        ".*\\b(what|when|where|who|why|how|which|can|could|would|will|do|does|did|is|are|was|were)\\b.*\\?|.*\\?$",
        Pattern.CASE_INSENSITIVE
    );
    
    // Agreement/disagreement patterns
    private static final Pattern AGREEMENT_PATTERNS = Pattern.compile(
        ".*\\b(yes|yeah|yep|sure|ok|okay|alright|agreed|exactly|absolutely|correct|right)\\b.*",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DISAGREEMENT_PATTERNS = Pattern.compile(
        ".*\\b(no|nope|nah|wrong|incorrect|disagree|never)\\b.*",
        Pattern.CASE_INSENSITIVE
    );
    
    // Greeting patterns
    private static final Pattern GREETING_PATTERNS = Pattern.compile(
        ".*\\b(hello|hi|hey|good morning|good afternoon|good evening|how are you|what's up|whats up)\\b.*",
        Pattern.CASE_INSENSITIVE
    );
    
    // Farewell patterns
    private static final Pattern FAREWELL_PATTERNS = Pattern.compile(
        ".*\\b(bye|goodbye|see you|talk to you|good night|goodnight|take care|later)\\b.*",
        Pattern.CASE_INSENSITIVE
    );
    
    // Thanks patterns
    private static final Pattern THANKS_PATTERNS = Pattern.compile(
        ".*\\b(thank|thanks|appreciate|grateful)\\b.*",
        Pattern.CASE_INSENSITIVE
    );
    
    // Time-related patterns
    private static final Pattern TIME_PATTERNS = Pattern.compile(
        ".*\\b(when|time|schedule|meeting|appointment|today|tomorrow|yesterday|tonight)\\b.*",
        Pattern.CASE_INSENSITIVE
    );
    
    // Location-related patterns
    private static final Pattern LOCATION_PATTERNS = Pattern.compile(
        ".*\\b(where|location|place|address|here|there)\\b.*",
        Pattern.CASE_INSENSITIVE
    );
    
    public SmartReplyService(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.languageDetectionService = new LanguageDetectionService(context, null);
        this.random = new Random();
    }
    
    /**
     * Generates smart reply suggestions based on incoming message and conversation context.
     */
    public void generateSmartReplies(String incomingMessage, String conversationContext, 
                                   OfflineAIService.SmartReplyCallback callback) {
        if (TextUtils.isEmpty(incomingMessage)) {
            callback.onSmartRepliesGenerated(false, null, "No message to analyze");
            return;
        }
        
        executorService.execute(() -> {
            try {
                List<String> replies = generateReplies(incomingMessage, conversationContext);
                callback.onSmartRepliesGenerated(true, replies, null);
            } catch (Exception e) {
                Log.e(TAG, "Error generating smart replies", e);
                callback.onSmartRepliesGenerated(false, null, "Failed to generate replies: " + e.getMessage());
            }
        });
    }
    
    /**
     * Core logic for generating contextual replies.
     */
    private List<String> generateReplies(String message, String context) {
        List<String> replies = new ArrayList<>();
        String cleanMessage = message.trim().toLowerCase();
        
        // Detect sentiment and intent
        MessageIntent intent = detectIntent(cleanMessage);
        MessageSentiment sentiment = detectSentiment(cleanMessage);
        
        // Generate replies based on intent
        switch (intent) {
            case QUESTION:
                replies.addAll(generateQuestionReplies(cleanMessage, sentiment));
                break;
            case GREETING:
                replies.addAll(generateGreetingReplies(sentiment));
                break;
            case FAREWELL:
                replies.addAll(generateFarewellReplies(sentiment));
                break;
            case THANKS:
                replies.addAll(generateThanksReplies(sentiment));
                break;
            case AGREEMENT_SEEKING:
                replies.addAll(generateAgreementReplies(cleanMessage, sentiment));
                break;
            case TIME_RELATED:
                replies.addAll(generateTimeReplies(cleanMessage, sentiment));
                break;
            case LOCATION_RELATED:
                replies.addAll(generateLocationReplies(cleanMessage, sentiment));
                break;
            case STATEMENT:
            default:
                replies.addAll(generateStatementReplies(cleanMessage, sentiment, context));
                break;
        }
        
        // Ensure we have at least some generic replies
        if (replies.isEmpty()) {
            replies.addAll(generateGenericReplies(sentiment));
        }
        
        // Limit to MAX_REPLIES and add variety
        return selectBestReplies(replies);
    }
    
    /**
     * Detects the intent of the incoming message.
     */
    private MessageIntent detectIntent(String message) {
        if (QUESTION_PATTERNS.matcher(message).matches()) {
            return MessageIntent.QUESTION;
        }
        if (GREETING_PATTERNS.matcher(message).matches()) {
            return MessageIntent.GREETING;
        }
        if (FAREWELL_PATTERNS.matcher(message).matches()) {
            return MessageIntent.FAREWELL;
        }
        if (THANKS_PATTERNS.matcher(message).matches()) {
            return MessageIntent.THANKS;
        }
        if (TIME_PATTERNS.matcher(message).matches()) {
            return MessageIntent.TIME_RELATED;
        }
        if (LOCATION_PATTERNS.matcher(message).matches()) {
            return MessageIntent.LOCATION_RELATED;
        }
        if (message.contains("right?") || message.contains("agree") || message.contains("think")) {
            return MessageIntent.AGREEMENT_SEEKING;
        }
        return MessageIntent.STATEMENT;
    }
    
    /**
     * Detects the sentiment of the message.
     */
    private MessageSentiment detectSentiment(String message) {
        // Positive words
        String[] positiveWords = {"great", "good", "awesome", "excellent", "fantastic", "wonderful", 
                                "happy", "excited", "love", "like", "amazing", "perfect", "yes", "sure"};
        
        // Negative words
        String[] negativeWords = {"bad", "terrible", "awful", "hate", "dislike", "angry", "sad", 
                                "disappointed", "frustrated", "no", "never", "wrong", "problem"};
        
        // Urgent words
        String[] urgentWords = {"urgent", "asap", "immediately", "quickly", "fast", "hurry", "emergency"};
        
        int positiveCount = 0;
        int negativeCount = 0;
        int urgentCount = 0;
        
        for (String word : positiveWords) {
            if (message.contains(word)) positiveCount++;
        }
        for (String word : negativeWords) {
            if (message.contains(word)) negativeCount++;
        }
        for (String word : urgentWords) {
            if (message.contains(word)) urgentCount++;
        }
        
        if (urgentCount > 0) return MessageSentiment.URGENT;
        if (positiveCount > negativeCount) return MessageSentiment.POSITIVE;
        if (negativeCount > positiveCount) return MessageSentiment.NEGATIVE;
        return MessageSentiment.NEUTRAL;
    }
    
    /**
     * Generates replies for questions.
     */
    private List<String> generateQuestionReplies(String message, MessageSentiment sentiment) {
        List<String> replies = new ArrayList<>();
        
        if (message.contains("how are you") || message.contains("how r u")) {
            replies.add("I'm doing well, thanks! How about you?");
            replies.add("Pretty good! How are things with you?");
            replies.add("All good here! What's up?");
        } else if (message.contains("what") && message.contains("doing")) {
            replies.add("Just the usual! What about you?");
            replies.add("Not much, just relaxing. You?");
            replies.add("Working on some stuff. How about you?");
        } else if (message.contains("when")) {
            replies.add("Let me check and get back to you");
            replies.add("I'll need to look into that");
            replies.add("Good question, I'll find out");
        } else if (message.contains("where")) {
            replies.add("I'm not sure about the location");
            replies.add("Let me check the address");
            replies.add("I'll need to confirm that");
        } else {
            replies.add("That's a good question");
            replies.add("Let me think about that");
            replies.add("I'll get back to you on that");
            replies.add("Hmm, I'm not sure");
        }
        
        return replies;
    }
    
    /**
     * Generates replies for greetings.
     */
    private List<String> generateGreetingReplies(MessageSentiment sentiment) {
        List<String> replies = new ArrayList<>();
        replies.add("Hey there!");
        replies.add("Hi! How's it going?");
        replies.add("Hello! Good to hear from you");
        replies.add("Hey! What's up?");
        replies.add("Hi! How are you doing?");
        return replies;
    }
    
    /**
     * Generates replies for farewells.
     */
    private List<String> generateFarewellReplies(MessageSentiment sentiment) {
        List<String> replies = new ArrayList<>();
        replies.add("Bye! Take care");
        replies.add("See you later!");
        replies.add("Goodbye! Have a great day");
        replies.add("Later! Talk to you soon");
        replies.add("Bye! Catch you later");
        return replies;
    }
    
    /**
     * Generates replies for thanks messages.
     */
    private List<String> generateThanksReplies(MessageSentiment sentiment) {
        List<String> replies = new ArrayList<>();
        replies.add("You're welcome!");
        replies.add("No problem!");
        replies.add("Glad I could help");
        replies.add("Anytime!");
        replies.add("Happy to help");
        return replies;
    }
    
    /**
     * Generates replies for agreement-seeking messages.
     */
    private List<String> generateAgreementReplies(String message, MessageSentiment sentiment) {
        List<String> replies = new ArrayList<>();
        
        if (sentiment == MessageSentiment.POSITIVE) {
            replies.add("Absolutely!");
            replies.add("I totally agree");
            replies.add("Yes, exactly!");
            replies.add("Couldn't agree more");
        } else if (sentiment == MessageSentiment.NEGATIVE) {
            replies.add("I see your point");
            replies.add("That's understandable");
            replies.add("I can see why you'd think that");
        } else {
            replies.add("I think so too");
            replies.add("Makes sense to me");
            replies.add("I can see that");
            replies.add("That's reasonable");
        }
        
        return replies;
    }
    
    /**
     * Generates replies for time-related messages.
     */
    private List<String> generateTimeReplies(String message, MessageSentiment sentiment) {
        List<String> replies = new ArrayList<>();
        replies.add("Let me check my schedule");
        replies.add("I'll need to confirm the time");
        replies.add("What time works for you?");
        replies.add("I should be available");
        return replies;
    }
    
    /**
     * Generates replies for location-related messages.
     */
    private List<String> generateLocationReplies(String message, MessageSentiment sentiment) {
        List<String> replies = new ArrayList<>();
        replies.add("I'll send you the address");
        replies.add("Let me check the location");
        replies.add("Where would be convenient for you?");
        replies.add("I can meet you there");
        return replies;
    }
    
    /**
     * Generates replies for general statements.
     */
    private List<String> generateStatementReplies(String message, MessageSentiment sentiment, String context) {
        List<String> replies = new ArrayList<>();
        
        switch (sentiment) {
            case POSITIVE:
                replies.add("That's great!");
                replies.add("Awesome!");
                replies.add("So happy to hear that");
                replies.add("That's wonderful news");
                break;
            case NEGATIVE:
                replies.add("I'm sorry to hear that");
                replies.add("That's tough");
                replies.add("I understand how you feel");
                replies.add("Hang in there");
                break;
            case URGENT:
                replies.add("Got it, I'll handle this right away");
                replies.add("On it!");
                replies.add("I'll take care of this ASAP");
                break;
            case NEUTRAL:
            default:
                replies.add("I see");
                replies.add("Got it");
                replies.add("Makes sense");
                replies.add("Thanks for letting me know");
                break;
        }
        
        return replies;
    }
    
    /**
     * Generates generic fallback replies.
     */
    private List<String> generateGenericReplies(MessageSentiment sentiment) {
        List<String> replies = new ArrayList<>();
        replies.add("Got it");
        replies.add("OK");
        replies.add("Thanks");
        replies.add("I understand");
        replies.add("Sounds good");
        return replies;
    }
    
    /**
     * Selects the best replies, ensuring variety and relevance.
     */
    private List<String> selectBestReplies(List<String> allReplies) {
        if (allReplies.isEmpty()) {
            return Arrays.asList("OK", "Got it", "Thanks");
        }
        
        // Remove duplicates while preserving order
        List<String> uniqueReplies = new ArrayList<>();
        for (String reply : allReplies) {
            if (!uniqueReplies.contains(reply)) {
                uniqueReplies.add(reply);
            }
        }
        
        // Shuffle to add variety
        List<String> selectedReplies = new ArrayList<>(uniqueReplies);
        if (selectedReplies.size() > MAX_REPLIES) {
            // Keep first reply (most relevant) and randomly select others
            String firstReply = selectedReplies.get(0);
            selectedReplies.remove(0);
            
            // Shuffle remaining and select
            for (int i = selectedReplies.size() - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                String temp = selectedReplies.get(i);
                selectedReplies.set(i, selectedReplies.get(j));
                selectedReplies.set(j, temp);
            }
            
            List<String> result = new ArrayList<>();
            result.add(firstReply);
            result.addAll(selectedReplies.subList(0, Math.min(MAX_REPLIES - 1, selectedReplies.size())));
            return result;
        }
        
        return selectedReplies;
    }
    
    // Enums for classification
    private enum MessageIntent {
        QUESTION, GREETING, FAREWELL, THANKS, AGREEMENT_SEEKING, 
        TIME_RELATED, LOCATION_RELATED, STATEMENT
    }
    
    private enum MessageSentiment {
        POSITIVE, NEGATIVE, NEUTRAL, URGENT
    }
}