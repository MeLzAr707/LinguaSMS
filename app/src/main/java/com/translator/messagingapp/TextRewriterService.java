package com.translator.messagingapp;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for rewriting text in different tones using offline rule-based transformations.
 * Provides completely offline text rewriting capabilities for enhanced privacy and speed.
 */
public class TextRewriterService {
    private static final String TAG = "TextRewriterService";
    
    /**
     * Available tone options for text rewriting
     */
    public enum Tone {
        FORMAL("Formal", "More professional and polite"),
        FRIENDLY("Friendly", "Warm and approachable"),
        CONCISE("Concise", "Brief and to the point"),
        ORIGINAL("Original", "Keep original text");
        
        private final String displayName;
        private final String description;
        
        Tone(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Callback interface for text rewriting operations
     */
    public interface RewriteCallback {
        void onRewriteComplete(boolean success, String rewrittenText, String errorMessage);
    }
    
    private final Context context;
    private final ExecutorService executorService;
    
    // Rule-based transformation patterns
    private final Map<String, String> formalReplacements;
    private final Map<String, String> friendlyReplacements;
    private final List<String> conciseWords;
    
    /**
     * Creates a new TextRewriterService
     *
     * @param context The application context
     */
    public TextRewriterService(Context context) {
        this.context = context;
        this.executorService = Executors.newCachedThreadPool();
        
        // Initialize transformation rules
        this.formalReplacements = initializeFormalReplacements();
        this.friendlyReplacements = initializeFriendlyReplacements();
        this.conciseWords = initializeConciseWords();
    }
    
    /**
     * Rewrites the given text according to the specified tone
     *
     * @param text The original text to rewrite
     * @param tone The target tone for rewriting
     * @param callback Callback to receive the result
     */
    public void rewriteText(String text, Tone tone, RewriteCallback callback) {
        if (TextUtils.isEmpty(text)) {
            callback.onRewriteComplete(false, text, "Text is empty");
            return;
        }
        
        if (tone == Tone.ORIGINAL) {
            callback.onRewriteComplete(true, text, null);
            return;
        }
        
        executorService.execute(() -> {
            try {
                String rewrittenText = performRewrite(text, tone);
                callback.onRewriteComplete(true, rewrittenText, null);
            } catch (Exception e) {
                Log.e(TAG, "Error rewriting text", e);
                callback.onRewriteComplete(false, text, e.getMessage());
            }
        });
    }
    
    /**
     * Performs the actual text rewriting based on the specified tone
     */
    private String performRewrite(String text, Tone tone) {
        String result = text;
        
        switch (tone) {
            case FORMAL:
                result = applyFormalTone(result);
                break;
            case FRIENDLY:
                result = applyFriendlyTone(result);
                break;
            case CONCISE:
                result = applyConciseTone(result);
                break;
            default:
                // Keep original
                break;
        }
        
        return result.trim();
    }
    
    /**
     * Applies formal tone transformations
     */
    private String applyFormalTone(String text) {
        String result = text;
        
        // Replace informal contractions and words
        for (Map.Entry<String, String> replacement : formalReplacements.entrySet()) {
            result = result.replaceAll("(?i)\\b" + Pattern.quote(replacement.getKey()) + "\\b", 
                                     replacement.getValue());
        }
        
        // Add courtesy phrases if not present
        if (!result.toLowerCase().contains("please") && !result.toLowerCase().contains("thank")) {
            // Add "please" to requests
            if (result.toLowerCase().contains("can you") || result.toLowerCase().contains("could you")) {
                result = result.replaceAll("(?i)\\b(can|could) you\\b", "$1 you please");
            }
        }
        
        // Ensure proper sentence structure
        result = ensureProperCapitalization(result);
        
        return result;
    }
    
    /**
     * Applies friendly tone transformations
     */
    private String applyFriendlyTone(String text) {
        String result = text;
        
        // Replace formal words with friendly alternatives
        for (Map.Entry<String, String> replacement : friendlyReplacements.entrySet()) {
            result = result.replaceAll("(?i)\\b" + Pattern.quote(replacement.getKey()) + "\\b", 
                                     replacement.getValue());
        }
        
        // Add friendly expressions
        if (!result.contains("!") && !result.contains("?")) {
            // Add enthusiasm to statements
            if (result.toLowerCase().contains("great") || result.toLowerCase().contains("good") || 
                result.toLowerCase().contains("thanks") || result.toLowerCase().contains("thank")) {
                result = result.replaceAll("\\.$", "!");
            }
        }
        
        return result;
    }
    
    /**
     * Applies concise tone transformations
     */
    private String applyConciseTone(String text) {
        String result = text;
        
        // Remove unnecessary words
        for (String word : conciseWords) {
            result = result.replaceAll("(?i)\\b" + Pattern.quote(word) + "\\s+", "");
        }
        
        // Remove redundant phrases
        result = result.replaceAll("(?i)\\b(just wanted to|i was wondering if|if you don't mind)\\s+", "");
        result = result.replaceAll("(?i)\\b(kind of|sort of)\\s+", "");
        result = result.replaceAll("(?i)\\b(really|very|quite)\\s+", "");
        
        // Simplify sentences
        result = result.replaceAll("(?i)\\bwould you be able to\\b", "can you");
        result = result.replaceAll("(?i)\\bi would like to\\b", "i'll");
        result = result.replaceAll("(?i)\\bit would be great if\\b", "please");
        
        return result;
    }
    
    /**
     * Ensures proper capitalization for sentences
     */
    private String ensureProperCapitalization(String text) {
        if (TextUtils.isEmpty(text)) {
            return text;
        }
        
        // Capitalize first letter
        StringBuilder result = new StringBuilder(text);
        result.setCharAt(0, Character.toUpperCase(result.charAt(0)));
        
        // Capitalize after periods, exclamation marks, question marks
        Pattern sentencePattern = Pattern.compile("([.!?]\\s+)([a-z])");
        Matcher matcher = sentencePattern.matcher(result.toString());
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1) + matcher.group(2).toUpperCase());
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * Initialize formal tone replacement rules
     */
    private Map<String, String> initializeFormalReplacements() {
        Map<String, String> replacements = new HashMap<>();
        
        // Contractions to full forms
        replacements.put("don't", "do not");
        replacements.put("won't", "will not");
        replacements.put("can't", "cannot");
        replacements.put("shouldn't", "should not");
        replacements.put("wouldn't", "would not");
        replacements.put("couldn't", "could not");
        replacements.put("didn't", "did not");
        replacements.put("hasn't", "has not");
        replacements.put("haven't", "have not");
        replacements.put("isn't", "is not");
        replacements.put("aren't", "are not");
        replacements.put("wasn't", "was not");
        replacements.put("weren't", "were not");
        replacements.put("i'm", "I am");
        replacements.put("you're", "you are");
        replacements.put("he's", "he is");
        replacements.put("she's", "she is");
        replacements.put("it's", "it is");
        replacements.put("we're", "we are");
        replacements.put("they're", "they are");
        replacements.put("i'll", "I will");
        replacements.put("you'll", "you will");
        replacements.put("i've", "I have");
        replacements.put("you've", "you have");
        replacements.put("we've", "we have");
        replacements.put("they've", "they have");
        
        // Informal to formal words
        replacements.put("yeah", "yes");
        replacements.put("yep", "yes");
        replacements.put("nope", "no");
        replacements.put("gonna", "going to");
        replacements.put("wanna", "want to");
        replacements.put("gotta", "have to");
        replacements.put("kinda", "kind of");
        replacements.put("sorta", "sort of");
        replacements.put("hi", "hello");
        replacements.put("hey", "hello");
        replacements.put("thanks", "thank you");
        replacements.put("ok", "okay");
        replacements.put("cool", "good");
        replacements.put("awesome", "excellent");
        replacements.put("great", "excellent");
        
        return replacements;
    }
    
    /**
     * Initialize friendly tone replacement rules
     */
    private Map<String, String> initializeFriendlyReplacements() {
        Map<String, String> replacements = new HashMap<>();
        
        // Formal to friendly words
        replacements.put("hello", "hi");
        replacements.put("greetings", "hey");
        replacements.put("excellent", "awesome");
        replacements.put("satisfactory", "good");
        replacements.put("acceptable", "fine");
        replacements.put("understand", "get");
        replacements.put("assistance", "help");
        replacements.put("regarding", "about");
        replacements.put("concerning", "about");
        replacements.put("furthermore", "also");
        replacements.put("additionally", "also");
        replacements.put("however", "but");
        replacements.put("nevertheless", "but");
        replacements.put("therefore", "so");
        replacements.put("consequently", "so");
        
        return replacements;
    }
    
    /**
     * Initialize words to remove for concise tone
     */
    private List<String> initializeConciseWords() {
        return Arrays.asList(
            "actually", "basically", "essentially", "literally", "obviously",
            "definitely", "absolutely", "totally", "completely", "entirely",
            "extremely", "incredibly", "amazingly", "surprisingly", "hopefully",
            "perhaps", "maybe", "possibly", "probably", "apparently",
            "honestly", "frankly", "truly", "really", "quite", "rather",
            "somewhat", "fairly", "pretty", "kind of", "sort of"
        );
    }
    
    /**
     * Gets all available tones
     */
    public Tone[] getAvailableTones() {
        return Tone.values();
    }
    
    /**
     * Cleanup resources
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}