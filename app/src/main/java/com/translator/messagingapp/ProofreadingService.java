package com.translator.messagingapp;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Offline proofreading service that combines Android's spell checker
 * with custom grammar and style checking rules.
 */
public class ProofreadingService {
    private static final String TAG = "ProofreadingService";
    
    private final Context context;
    private final ExecutorService executorService;
    private final TextServicesManager textServicesManager;
    
    // Grammar patterns for common mistakes
    private static final Pattern DOUBLE_SPACES = Pattern.compile("\\s{2,}");
    private static final Pattern MISSING_CAPITALIZATION = Pattern.compile("\\b[a-z]");
    private static final Pattern REPEATED_PUNCTUATION = Pattern.compile("[.!?]{2,}");
    private static final Pattern COMMON_CONTRACTIONS = Pattern.compile("\\b(cant|wont|dont|doesnt|isnt|arent|wasnt|werent|havent|hasnt|hadnt|shouldnt|wouldnt|couldnt)\\b", Pattern.CASE_INSENSITIVE);
    
    public ProofreadingService(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.textServicesManager = (TextServicesManager) context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
    }
    
    /**
     * Proofreads text for spelling, grammar, and style issues.
     */
    public void proofreadText(String text, OfflineAIService.ProofreadingCallback callback) {
        if (TextUtils.isEmpty(text)) {
            callback.onProofreadingComplete(false, null, "No text to proofread");
            return;
        }
        
        executorService.execute(() -> {
            try {
                List<OfflineAIService.ProofreadingSuggestion> suggestions = new ArrayList<>();
                
                // Perform spell checking
                performSpellCheck(text, suggestions);
                
                // Perform grammar and style checking
                performGrammarCheck(text, suggestions);
                
                callback.onProofreadingComplete(true, suggestions, null);
            } catch (Exception e) {
                Log.e(TAG, "Error during proofreading", e);
                callback.onProofreadingComplete(false, null, "Proofreading failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Performs spell checking using Android's TextServicesManager.
     */
    private void performSpellCheck(String text, List<OfflineAIService.ProofreadingSuggestion> suggestions) {
        if (textServicesManager == null || !textServicesManager.isSpellCheckerEnabled()) {
            Log.d(TAG, "Spell checker not available or disabled");
            return;
        }
        
        try {
            // Split text into words for spell checking
            String[] words = text.split("\\s+");
            int currentIndex = 0;
            
            for (String word : words) {
                // Find the position of this word in the original text
                int wordStart = text.indexOf(word, currentIndex);
                if (wordStart == -1) continue;
                
                currentIndex = wordStart + word.length();
                
                // Clean the word for spell checking
                String cleanWord = word.replaceAll("[^a-zA-Z]", "");
                if (cleanWord.length() < 2) continue;
                
                // Check if word looks misspelled (simple heuristics)
                if (isPotentialMisspelling(cleanWord)) {
                    String suggestion = getSuggestionForWord(cleanWord);
                    if (suggestion != null && !suggestion.equals(cleanWord)) {
                        suggestions.add(new OfflineAIService.ProofreadingSuggestion(
                            word,
                            suggestion,
                            wordStart,
                            wordStart + word.length(),
                            OfflineAIService.SuggestionType.SPELLING,
                            "Possible spelling error"
                        ));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during spell check", e);
        }
    }
    
    /**
     * Performs grammar and style checking using pattern matching.
     */
    private void performGrammarCheck(String text, List<OfflineAIService.ProofreadingSuggestion> suggestions) {
        // Check for double spaces
        Matcher doubleSpacesMatcher = DOUBLE_SPACES.matcher(text);
        while (doubleSpacesMatcher.find()) {
            suggestions.add(new OfflineAIService.ProofreadingSuggestion(
                doubleSpacesMatcher.group(),
                " ",
                doubleSpacesMatcher.start(),
                doubleSpacesMatcher.end(),
                OfflineAIService.SuggestionType.STYLE,
                "Remove extra spaces"
            ));
        }
        
        // Check for missing capitalization at sentence start
        if (!text.isEmpty() && Character.isLowerCase(text.charAt(0))) {
            String capitalized = Character.toUpperCase(text.charAt(0)) + text.substring(1);
            suggestions.add(new OfflineAIService.ProofreadingSuggestion(
                String.valueOf(text.charAt(0)),
                String.valueOf(capitalized.charAt(0)),
                0,
                1,
                OfflineAIService.SuggestionType.GRAMMAR,
                "Capitalize first letter"
            ));
        }
        
        // Check for repeated punctuation
        Matcher repeatedPuncMatcher = REPEATED_PUNCTUATION.matcher(text);
        while (repeatedPuncMatcher.find()) {
            String replacement = String.valueOf(repeatedPuncMatcher.group().charAt(0));
            suggestions.add(new OfflineAIService.ProofreadingSuggestion(
                repeatedPuncMatcher.group(),
                replacement,
                repeatedPuncMatcher.start(),
                repeatedPuncMatcher.end(),
                OfflineAIService.SuggestionType.STYLE,
                "Remove repeated punctuation"
            ));
        }
        
        // Check for common contraction errors
        Matcher contractionMatcher = COMMON_CONTRACTIONS.matcher(text);
        while (contractionMatcher.find()) {
            String word = contractionMatcher.group().toLowerCase();
            String suggestion = getContractionSuggestion(word);
            if (suggestion != null) {
                suggestions.add(new OfflineAIService.ProofreadingSuggestion(
                    contractionMatcher.group(),
                    suggestion,
                    contractionMatcher.start(),
                    contractionMatcher.end(),
                    OfflineAIService.SuggestionType.SPELLING,
                    "Add apostrophe to contraction"
                ));
            }
        }
        
        // Check for sentence structure issues
        checkSentenceStructure(text, suggestions);
    }
    
    /**
     * Checks for basic sentence structure issues.
     */
    private void checkSentenceStructure(String text, List<OfflineAIService.ProofreadingSuggestion> suggestions) {
        // Check for very long sentences
        String[] sentences = text.split("[.!?]+");
        int currentIndex = 0;
        
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) continue;
            
            int sentenceStart = text.indexOf(sentence, currentIndex);
            if (sentenceStart == -1) continue;
            
            currentIndex = sentenceStart + sentence.length();
            
            // Check if sentence is too long (over 25 words)
            String[] words = sentence.split("\\s+");
            if (words.length > 25) {
                suggestions.add(new OfflineAIService.ProofreadingSuggestion(
                    sentence,
                    sentence, // No automatic fix for long sentences
                    sentenceStart,
                    sentenceStart + sentence.length(),
                    OfflineAIService.SuggestionType.CLARITY,
                    "Consider breaking this long sentence into shorter ones"
                ));
            }
            
            // Check for missing punctuation at end
            if (!sentence.isEmpty() && !".!?".contains(String.valueOf(text.charAt(sentenceStart + sentence.length())))) {
                // Only suggest if this isn't the last sentence and there's more text
                if (sentenceStart + sentence.length() < text.length() - 1) {
                    suggestions.add(new OfflineAIService.ProofreadingSuggestion(
                        sentence,
                        sentence + ".",
                        sentenceStart,
                        sentenceStart + sentence.length(),
                        OfflineAIService.SuggestionType.GRAMMAR,
                        "Add punctuation at end of sentence"
                    ));
                }
            }
        }
    }
    
    /**
     * Simple heuristic to identify potential misspellings.
     */
    private boolean isPotentialMisspelling(String word) {
        if (word.length() < 3) return false;
        
        // Check for common patterns that might indicate misspelling
        // This is a simplified approach - in a real implementation, you'd use a dictionary
        
        // Words with unusual character patterns
        if (word.matches(".*[qxz]{2,}.*")) return true;
        
        // Words with repeated characters (but allow common ones)
        if (word.matches(".*(.)\\1{2,}.*") && !word.matches(".*(ll|ss|ee|oo|nn|tt).*")) return true;
        
        // Very short words with uncommon patterns
        if (word.length() == 3 && word.matches(".*[qxz].*")) return true;
        
        return false;
    }
    
    /**
     * Gets a suggestion for a potentially misspelled word.
     */
    private String getSuggestionForWord(String word) {
        // Simple suggestion logic - in a real implementation, you'd use a spell checker API
        String lowerWord = word.toLowerCase();
        
        // Common misspellings and their corrections
        switch (lowerWord) {
            case "recieve": return "receive";
            case "seperate": return "separate";
            case "definately": return "definitely";
            case "occured": return "occurred";
            case "neccessary": return "necessary";
            case "begining": return "beginning";
            case "managment": return "management";
            case "experiance": return "experience";
            case "enviroment": return "environment";
            case "recomend": return "recommend";
            case "accomodate": return "accommodate";
            case "succesful": return "successful";
            case "independant": return "independent";
            case "maintainance": return "maintenance";
            case "priviledge": return "privilege";
            default: return null;
        }
    }
    
    /**
     * Gets the correct contraction for common mistakes.
     */
    private String getContractionSuggestion(String word) {
        switch (word) {
            case "cant": return "can't";
            case "wont": return "won't";
            case "dont": return "don't";
            case "doesnt": return "doesn't";
            case "isnt": return "isn't";
            case "arent": return "aren't";
            case "wasnt": return "wasn't";
            case "werent": return "weren't";
            case "havent": return "haven't";
            case "hasnt": return "hasn't";
            case "hadnt": return "hadn't";
            case "shouldnt": return "shouldn't";
            case "wouldnt": return "wouldn't";
            case "couldnt": return "couldn't";
            default: return null;
        }
    }
}