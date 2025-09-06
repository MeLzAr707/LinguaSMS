package com.translator.messagingapp;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text rewriting service that enhances messages for different tones and styles.
 * Provides rule-based text transformation for formality, brevity, and politeness.
 */
public class TextRewritingService {
    private static final String TAG = "TextRewritingService";
    
    private final Context context;
    private final ExecutorService executorService;
    
    // Patterns for informal text
    private static final Pattern CONTRACTIONS = Pattern.compile("\\b(I'm|you're|we're|they're|it's|he's|she's|isn't|aren't|wasn't|weren't|haven't|hasn't|hadn't|won't|wouldn't|can't|couldn't|shouldn't|don't|doesn't|didn't)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern INFORMAL_WORDS = Pattern.compile("\\b(gonna|wanna|gotta|yeah|yep|nope|ok|okay)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CASUAL_GREETINGS = Pattern.compile("\\b(hey|hi there|what's up|whats up)\\b", Pattern.CASE_INSENSITIVE);
    
    // Patterns for excessive words/phrases
    private static final Pattern FILLER_WORDS = Pattern.compile("\\b(um|uh|like|you know|basically|actually|literally|really|very|quite|rather|pretty much)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern REDUNDANT_PHRASES = Pattern.compile("\\b(in order to|for the purpose of|due to the fact that|in spite of the fact that)\\b", Pattern.CASE_INSENSITIVE);
    
    public TextRewritingService(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Rewrites text according to the specified mode.
     */
    public void rewriteText(String text, OfflineAIService.RewriteMode mode, OfflineAIService.RewritingCallback callback) {
        if (TextUtils.isEmpty(text)) {
            callback.onRewritingComplete(false, null, "No text to rewrite");
            return;
        }
        
        executorService.execute(() -> {
            try {
                String rewrittenText = performRewriting(text, mode);
                callback.onRewritingComplete(true, rewrittenText, null);
            } catch (Exception e) {
                Log.e(TAG, "Error during text rewriting", e);
                callback.onRewritingComplete(false, null, "Rewriting failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Core rewriting logic based on the selected mode.
     */
    private String performRewriting(String text, OfflineAIService.RewriteMode mode) {
        switch (mode) {
            case FORMAL:
                return makeFormal(text);
            case CASUAL:
                return makeCasual(text);
            case CONCISE:
                return makeConcise(text);
            case ELABORATE:
                return makeElaborate(text);
            case POLITE:
                return makePolite(text);
            default:
                return text;
        }
    }
    
    /**
     * Transforms text to a more formal tone.
     */
    private String makeFormal(String text) {
        String result = text;
        
        // Expand contractions
        result = expandContractions(result);
        
        // Replace informal words
        result = replaceInformalWords(result);
        
        // Replace casual greetings
        result = replaceCasualGreetings(result);
        
        // Ensure proper capitalization
        result = ensureProperCapitalization(result);
        
        // Add formal phrases where appropriate
        result = addFormalPhrases(result);
        
        return result.trim();
    }
    
    /**
     * Transforms text to a more casual tone.
     */
    private String makeCasual(String text) {
        String result = text;
        
        // Add contractions
        result = addContractions(result);
        
        // Replace formal words with casual alternatives
        result = replaceFormalWords(result);
        
        // Make greetings more casual
        result = makeCasualGreetings(result);
        
        // Simplify overly complex sentences
        result = simplifySentences(result);
        
        return result.trim();
    }
    
    /**
     * Makes text more concise by removing unnecessary words.
     */
    private String makeConcise(String text) {
        String result = text;
        
        // Remove filler words
        result = FILLER_WORDS.matcher(result).replaceAll("");
        
        // Replace redundant phrases
        result = replaceRedundantPhrases(result);
        
        // Combine short sentences
        result = combineShortSentences(result);
        
        // Remove unnecessary adjectives and adverbs
        result = removeUnnecessaryModifiers(result);
        
        // Clean up extra spaces
        result = result.replaceAll("\\s+", " ");
        
        return result.trim();
    }
    
    /**
     * Makes text more elaborate by adding detail and explanation.
     */
    private String makeElaborate(String text) {
        String result = text;
        
        // Add transitional phrases
        result = addTransitionalPhrases(result);
        
        // Expand simple statements
        result = expandSimpleStatements(result);
        
        // Add explanatory phrases
        result = addExplanatoryPhrases(result);
        
        return result.trim();
    }
    
    /**
     * Makes text more polite by adding courteous language.
     */
    private String makePolite(String text) {
        String result = text;
        
        // Add polite phrases
        result = addPolitePhrases(result);
        
        // Soften direct requests
        result = softenRequests(result);
        
        // Add courtesy words
        result = addCourtesyWords(result);
        
        return result.trim();
    }
    
    /**
     * Expands contractions to full forms.
     */
    private String expandContractions(String text) {
        String result = text;
        
        // Common contractions
        result = result.replaceAll("(?i)\\bI'm\\b", "I am");
        result = result.replaceAll("(?i)\\byou're\\b", "you are");
        result = result.replaceAll("(?i)\\bwe're\\b", "we are");
        result = result.replaceAll("(?i)\\bthey're\\b", "they are");
        result = result.replaceAll("(?i)\\bit's\\b", "it is");
        result = result.replaceAll("(?i)\\bhe's\\b", "he is");
        result = result.replaceAll("(?i)\\bshe's\\b", "she is");
        result = result.replaceAll("(?i)\\bisn't\\b", "is not");
        result = result.replaceAll("(?i)\\baren't\\b", "are not");
        result = result.replaceAll("(?i)\\bwasn't\\b", "was not");
        result = result.replaceAll("(?i)\\bweren't\\b", "were not");
        result = result.replaceAll("(?i)\\bhaven't\\b", "have not");
        result = result.replaceAll("(?i)\\bhasn't\\b", "has not");
        result = result.replaceAll("(?i)\\bhadn't\\b", "had not");
        result = result.replaceAll("(?i)\\bwon't\\b", "will not");
        result = result.replaceAll("(?i)\\bwouldn't\\b", "would not");
        result = result.replaceAll("(?i)\\bcan't\\b", "cannot");
        result = result.replaceAll("(?i)\\bcouldn't\\b", "could not");
        result = result.replaceAll("(?i)\\bshouldn't\\b", "should not");
        result = result.replaceAll("(?i)\\bdon't\\b", "do not");
        result = result.replaceAll("(?i)\\bdoesn't\\b", "does not");
        result = result.replaceAll("(?i)\\bdidn't\\b", "did not");
        
        return result;
    }
    
    /**
     * Replaces informal words with formal alternatives.
     */
    private String replaceInformalWords(String text) {
        String result = text;
        
        result = result.replaceAll("(?i)\\bgonna\\b", "going to");
        result = result.replaceAll("(?i)\\bwanna\\b", "want to");
        result = result.replaceAll("(?i)\\bgotta\\b", "have to");
        result = result.replaceAll("(?i)\\byeah\\b", "yes");
        result = result.replaceAll("(?i)\\byep\\b", "yes");
        result = result.replaceAll("(?i)\\bnope\\b", "no");
        result = result.replaceAll("(?i)\\bok\\b", "acceptable");
        result = result.replaceAll("(?i)\\bokay\\b", "acceptable");
        
        return result;
    }
    
    /**
     * Replaces casual greetings with formal ones.
     */
    private String replaceCasualGreetings(String text) {
        String result = text;
        
        result = result.replaceAll("(?i)\\bhey\\b", "Hello");
        result = result.replaceAll("(?i)\\bhi there\\b", "Good day");
        result = result.replaceAll("(?i)\\bwhat's up\\b", "How are you");
        result = result.replaceAll("(?i)\\bwhats up\\b", "How are you");
        
        return result;
    }
    
    /**
     * Ensures proper capitalization for formal text.
     */
    private String ensureProperCapitalization(String text) {
        if (text.isEmpty()) return text;
        
        // Capitalize first letter
        String result = Character.toUpperCase(text.charAt(0)) + text.substring(1);
        
        // Capitalize after periods
        result = result.replaceAll("(\\.\\s+)([a-z])", "$1" + "$2".toUpperCase());
        
        return result;
    }
    
    /**
     * Adds formal phrases to enhance professionalism.
     */
    private String addFormalPhrases(String text) {
        String result = text;
        
        // Add formal openings for requests
        if (result.toLowerCase().startsWith("can you")) {
            result = "Would you be able to" + result.substring(7);
        } else if (result.toLowerCase().startsWith("will you")) {
            result = "Would you kindly" + result.substring(8);
        }
        
        return result;
    }
    
    /**
     * Adds contractions for casual tone.
     */
    private String addContractions(String text) {
        String result = text;
        
        result = result.replaceAll("\\bI am\\b", "I'm");
        result = result.replaceAll("\\byou are\\b", "you're");
        result = result.replaceAll("\\bwe are\\b", "we're");
        result = result.replaceAll("\\bthey are\\b", "they're");
        result = result.replaceAll("\\bit is\\b", "it's");
        result = result.replaceAll("\\bis not\\b", "isn't");
        result = result.replaceAll("\\bare not\\b", "aren't");
        result = result.replaceAll("\\bdo not\\b", "don't");
        result = result.replaceAll("\\bdoes not\\b", "doesn't");
        result = result.replaceAll("\\bwill not\\b", "won't");
        result = result.replaceAll("\\bcannot\\b", "can't");
        
        return result;
    }
    
    /**
     * Replaces formal words with casual alternatives.
     */
    private String replaceFormalWords(String text) {
        String result = text;
        
        result = result.replaceAll("(?i)\\bacceptable\\b", "OK");
        result = result.replaceAll("(?i)\\bgoing to\\b", "gonna");
        result = result.replaceAll("(?i)\\bwant to\\b", "wanna");
        result = result.replaceAll("(?i)\\bhave to\\b", "gotta");
        
        return result;
    }
    
    /**
     * Makes greetings more casual.
     */
    private String makeCasualGreetings(String text) {
        String result = text;
        
        result = result.replaceAll("(?i)\\bGood day\\b", "Hey");
        result = result.replaceAll("(?i)\\bHello\\b", "Hi");
        result = result.replaceAll("(?i)\\bHow are you\\b", "What's up");
        
        return result;
    }
    
    /**
     * Simplifies overly complex sentences.
     */
    private String simplifySentences(String text) {
        String result = text;
        
        // Replace complex phrases with simpler ones
        result = result.replaceAll("(?i)\\bin order to\\b", "to");
        result = result.replaceAll("(?i)\\bdue to the fact that\\b", "because");
        result = result.replaceAll("(?i)\\bfor the purpose of\\b", "to");
        
        return result;
    }
    
    /**
     * Replaces redundant phrases with concise alternatives.
     */
    private String replaceRedundantPhrases(String text) {
        String result = text;
        
        result = result.replaceAll("(?i)\\bin order to\\b", "to");
        result = result.replaceAll("(?i)\\bfor the purpose of\\b", "to");
        result = result.replaceAll("(?i)\\bdue to the fact that\\b", "because");
        result = result.replaceAll("(?i)\\bin spite of the fact that\\b", "although");
        result = result.replaceAll("(?i)\\bat this point in time\\b", "now");
        result = result.replaceAll("(?i)\\bin the event that\\b", "if");
        
        return result;
    }
    
    /**
     * Combines short sentences for better flow.
     */
    private String combineShortSentences(String text) {
        String[] sentences = text.split("\\. ");
        if (sentences.length < 2) return text;
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i].trim();
            if (sentence.isEmpty()) continue;
            
            if (i > 0 && sentences[i-1].split("\\s+").length < 5 && sentence.split("\\s+").length < 5) {
                // Combine with previous short sentence
                result.append(" and ").append(sentence.toLowerCase());
            } else {
                if (result.length() > 0) result.append(". ");
                result.append(sentence);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Removes unnecessary modifiers.
     */
    private String removeUnnecessaryModifiers(String text) {
        String result = text;
        
        // Remove excessive adjectives
        result = result.replaceAll("\\b(very|quite|rather|extremely|incredibly|absolutely)\\s+", "");
        
        return result;
    }
    
    /**
     * Adds transitional phrases for better flow.
     */
    private String addTransitionalPhrases(String text) {
        String result = text;
        
        // Add transitions between sentences
        String[] sentences = result.split("\\. ");
        if (sentences.length > 1) {
            StringBuilder elaborated = new StringBuilder();
            for (int i = 0; i < sentences.length; i++) {
                if (i > 0) {
                    elaborated.append(". Furthermore, ");
                }
                elaborated.append(sentences[i]);
            }
            result = elaborated.toString();
        }
        
        return result;
    }
    
    /**
     * Expands simple statements with additional context.
     */
    private String expandSimpleStatements(String text) {
        String result = text;
        
        // Add explanatory context to short statements
        if (result.split("\\s+").length < 5) {
            if (result.toLowerCase().contains("yes")) {
                result = "I would like to confirm that " + result.toLowerCase();
            } else if (result.toLowerCase().contains("no")) {
                result = "I must respectfully indicate that " + result.toLowerCase();
            }
        }
        
        return result;
    }
    
    /**
     * Adds explanatory phrases.
     */
    private String addExplanatoryPhrases(String text) {
        String result = text;
        
        // Add explanation connectors
        if (!result.contains("because") && !result.contains("since") && !result.contains("due to")) {
            result += ", which I believe is important to mention";
        }
        
        return result;
    }
    
    /**
     * Adds polite phrases to text.
     */
    private String addPolitePhrases(String text) {
        String result = text;
        
        // Add polite openings
        if (!result.toLowerCase().startsWith("please") && !result.toLowerCase().startsWith("could") && !result.toLowerCase().startsWith("would")) {
            if (result.toLowerCase().contains("can you") || result.toLowerCase().contains("will you")) {
                result = "Please " + result.toLowerCase();
            }
        }
        
        // Add polite closings
        if (!result.toLowerCase().contains("please") && !result.toLowerCase().contains("thank")) {
            result += ", please";
        }
        
        return result;
    }
    
    /**
     * Softens direct requests.
     */
    private String softenRequests(String text) {
        String result = text;
        
        result = result.replaceAll("(?i)\\bcan you\\b", "could you please");
        result = result.replaceAll("(?i)\\bwill you\\b", "would you be able to");
        result = result.replaceAll("(?i)\\bdo this\\b", "handle this when convenient");
        
        return result;
    }
    
    /**
     * Adds courtesy words throughout the text.
     */
    private String addCourtesyWords(String text) {
        String result = text;
        
        // Add thank you if not present
        if (!result.toLowerCase().contains("thank")) {
            result += ". Thank you";
        }
        
        return result;
    }
}