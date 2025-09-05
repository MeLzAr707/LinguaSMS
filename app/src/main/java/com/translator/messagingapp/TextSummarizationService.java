package com.translator.messagingapp;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Offline text summarization service using extractive summarization techniques.
 * Implements TF-IDF based sentence ranking for creating summaries.
 */
public class TextSummarizationService {
    private static final String TAG = "TextSummarizationService";
    private static final int MAX_SUMMARY_SENTENCES = 3;
    private static final int MIN_TEXT_LENGTH = 50;
    
    private final Context context;
    private final ExecutorService executorService;
    
    public TextSummarizationService(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Summarizes a single text using extractive summarization.
     */
    public void summarizeText(String text, OfflineAIService.SummarizationCallback callback) {
        if (text == null || text.trim().length() < MIN_TEXT_LENGTH) {
            callback.onSummarizationComplete(false, null, "Text too short for summarization");
            return;
        }
        
        executorService.execute(() -> {
            try {
                String summary = performExtractiveSummarization(text);
                callback.onSummarizationComplete(true, summary, null);
            } catch (Exception e) {
                Log.e(TAG, "Error during text summarization", e);
                callback.onSummarizationComplete(false, null, "Summarization failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Summarizes a conversation by combining messages and extracting key points.
     */
    public void summarizeConversation(List<String> messages, OfflineAIService.SummarizationCallback callback) {
        if (messages == null || messages.isEmpty()) {
            callback.onSummarizationComplete(false, null, "No messages to summarize");
            return;
        }
        
        executorService.execute(() -> {
            try {
                // Combine all messages into a single text
                StringBuilder combinedText = new StringBuilder();
                for (String message : messages) {
                    if (message != null && !message.trim().isEmpty()) {
                        combinedText.append(message.trim()).append(". ");
                    }
                }
                
                String text = combinedText.toString().trim();
                if (text.length() < MIN_TEXT_LENGTH) {
                    callback.onSummarizationComplete(false, null, "Conversation too short for summarization");
                    return;
                }
                
                String summary = performConversationSummarization(messages);
                callback.onSummarizationComplete(true, summary, null);
            } catch (Exception e) {
                Log.e(TAG, "Error during conversation summarization", e);
                callback.onSummarizationComplete(false, null, "Summarization failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Performs extractive summarization using TF-IDF and sentence ranking.
     */
    private String performExtractiveSummarization(String text) {
        // Split text into sentences
        List<String> sentences = splitIntoSentences(text);
        if (sentences.size() <= MAX_SUMMARY_SENTENCES) {
            return text; // Text is already short enough
        }
        
        // Calculate TF-IDF scores for each sentence
        Map<String, Double> sentenceScores = calculateSentenceScores(sentences);
        
        // Rank sentences by score
        List<String> rankedSentences = new ArrayList<>(sentences);
        rankedSentences.sort((s1, s2) -> 
            Double.compare(sentenceScores.getOrDefault(s2, 0.0), sentenceScores.getOrDefault(s1, 0.0)));
        
        // Take top sentences (up to MAX_SUMMARY_SENTENCES)
        int summaryLength = Math.min(MAX_SUMMARY_SENTENCES, rankedSentences.size());
        List<String> topSentences = rankedSentences.subList(0, summaryLength);
        
        // Reorder sentences to maintain original flow
        topSentences.sort(Comparator.comparingInt(sentences::indexOf));
        
        return String.join(" ", topSentences);
    }
    
    /**
     * Performs conversation summarization with context awareness.
     */
    private String performConversationSummarization(List<String> messages) {
        // Extract key topics and themes
        Map<String, Integer> topicFrequency = new HashMap<>();
        List<String> keyMessages = new ArrayList<>();
        
        for (String message : messages) {
            if (message == null || message.trim().length() < 10) continue;
            
            // Extract keywords from message
            List<String> keywords = extractKeywords(message);
            for (String keyword : keywords) {
                topicFrequency.put(keyword, topicFrequency.getOrDefault(keyword, 0) + 1);
            }
            
            // Consider longer messages as potentially more important
            if (message.trim().length() > 30) {
                keyMessages.add(message.trim());
            }
        }
        
        // Find the most discussed topics
        List<String> topTopics = new ArrayList<>();
        topicFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> topTopics.add(entry.getKey()));
        
        // Create summary
        StringBuilder summary = new StringBuilder();
        summary.append("Key topics: ").append(String.join(", ", topTopics));
        
        if (!keyMessages.isEmpty()) {
            summary.append(". ");
            // Add 1-2 most relevant messages
            int messagesToInclude = Math.min(2, keyMessages.size());
            for (int i = 0; i < messagesToInclude; i++) {
                String message = keyMessages.get(i);
                if (message.length() > 100) {
                    message = message.substring(0, 97) + "...";
                }
                summary.append(message);
                if (i < messagesToInclude - 1) {
                    summary.append(" ");
                }
            }
        }
        
        return summary.toString();
    }
    
    /**
     * Splits text into sentences using basic punctuation.
     */
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        String[] parts = text.split("[.!?]+");
        
        for (String part : parts) {
            String sentence = part.trim();
            if (sentence.length() > 10) { // Filter out very short fragments
                sentences.add(sentence);
            }
        }
        
        return sentences;
    }
    
    /**
     * Calculates sentence scores using simple TF-IDF-like approach.
     */
    private Map<String, Double> calculateSentenceScores(List<String> sentences) {
        Map<String, Double> scores = new HashMap<>();
        Map<String, Integer> wordFreq = new HashMap<>();
        
        // Count word frequencies across all sentences
        for (String sentence : sentences) {
            List<String> words = extractKeywords(sentence);
            for (String word : words) {
                wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
            }
        }
        
        // Calculate scores for each sentence
        for (String sentence : sentences) {
            double score = 0.0;
            List<String> words = extractKeywords(sentence);
            
            for (String word : words) {
                // Simple TF-IDF approximation: frequency / total occurrences
                double tf = Collections.frequency(words, word);
                double idf = Math.log((double) sentences.size() / wordFreq.get(word));
                score += tf * idf;
            }
            
            // Normalize by sentence length
            if (!words.isEmpty()) {
                score = score / words.size();
            }
            
            scores.put(sentence, score);
        }
        
        return scores;
    }
    
    /**
     * Extracts keywords from text (simple implementation).
     */
    private List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        String[] words = text.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", "")
                .split("\\s+");
        
        // Filter out common stop words and short words
        String[] stopWords = {"the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "is", "are", "was", "were", "be", "been", "have", "has", "had", "do", "does", "did", "will", "would", "could", "should", "may", "might", "i", "you", "he", "she", "it", "we", "they", "this", "that", "these", "those"};
        List<String> stopWordsList = Arrays.asList(stopWords);
        
        for (String word : words) {
            if (word.length() > 2 && !stopWordsList.contains(word)) {
                keywords.add(word);
            }
        }
        
        return keywords;
    }
}