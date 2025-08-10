package com.translator.messagingapp;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Simplified Google Translation Service for testing without Android dependencies.
 * This implementation provides mock translation functionality for development/testing.
 */
public class GoogleTranslationServiceSimple {
    private static final String TAG = "GoogleTranslationService";
    private String apiKey;
    
    // Language detection patterns for common languages
    private static final Map<String, Pattern> LANGUAGE_PATTERNS = new HashMap<>();
    
    // Mock translations for testing
    private static final Map<String, String> MOCK_TRANSLATIONS = new HashMap<>();
    
    static {
        // Initialize language detection patterns
        LANGUAGE_PATTERNS.put("es", Pattern.compile("\\b(el|la|de|en|y|a|que|es|se|no|te|lo|le|da|su|por|son|con|para|una|tienen|más|fue|era|muy|año|hasta|desde|está|este|cuando|entre|sin|sobre|también|me|si|todo|bien|puede|tiempo|cada|eso|algo|lugar|después|vida|trabajo|parte|gobierno|mientras|aunque|porque|mundo|mayor|donde|durante|antes|todos|vez|mismo|estado|nunca|país|día|así|otro|contra|tanto|nivel|personas|forma|casa|mejor)\\b"));
        LANGUAGE_PATTERNS.put("fr", Pattern.compile("\\b(le|de|et|à|un|il|être|et|en|avoir|que|pour|dans|ce|son|une|sur|avec|ne|se|pas|tout|plus|pouvoir|par|je|son|que|qui|ce|lui|mais|ou|si|leur|bien|temps|très|même|autre|grand|nouveau|premier|où|comment|sans|sous|contre|après|pendant|avant|chez|entre|depuis|jusqu|encore)\\b"));
        LANGUAGE_PATTERNS.put("de", Pattern.compile("\\b(der|die|und|in|den|von|zu|das|mit|sich|des|auf|für|ist|im|dem|nicht|ein|eine|als|auch|es|an|werden|aus|er|hat|dass|sie|nach|wird|bei|einer|um|am|sind|noch|wie|einem|über|einen|so|zum|war|haben|nur|oder|aber|vor|zur|bis|unter|während|durch|mehr|gegen|vom|beim)\\b"));
        
        // Initialize mock translations for common phrases
        MOCK_TRANSLATIONS.put("hello|en|es", "hola");
        MOCK_TRANSLATIONS.put("hello|en|fr", "bonjour");
        MOCK_TRANSLATIONS.put("hello|en|de", "hallo");
        MOCK_TRANSLATIONS.put("how are you|en|es", "¿cómo estás?");
        MOCK_TRANSLATIONS.put("how are you|en|fr", "comment ça va?");
        MOCK_TRANSLATIONS.put("how are you|en|de", "wie geht es dir?");
        MOCK_TRANSLATIONS.put("good morning|en|es", "buenos días");
        MOCK_TRANSLATIONS.put("good morning|en|fr", "bonjour");
        MOCK_TRANSLATIONS.put("good morning|en|de", "guten morgen");
        MOCK_TRANSLATIONS.put("thank you|en|es", "gracias");
        MOCK_TRANSLATIONS.put("thank you|en|fr", "merci");
        MOCK_TRANSLATIONS.put("thank you|en|de", "danke");
    }
    
    /**
     * Creates a new GoogleTranslationService.
     *
     * @param apiKey The API key
     */
    public GoogleTranslationServiceSimple(String apiKey) {
        this.apiKey = apiKey;
        System.out.println(TAG + " initialized with API key: " + (hasApiKey() ? "provided" : "not provided"));
    }
    
    /**
     * Sets the API key.
     *
     * @param apiKey The API key
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        System.out.println(TAG + " API key updated: " + (hasApiKey() ? "provided" : "cleared"));
    }
    
    /**
     * Checks if the service has a valid API key.
     *
     * @return true if the service has a valid API key, false otherwise
     */
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
    
    /**
     * Tests if the API key is valid.
     *
     * @return true if the API key is valid, false otherwise
     */
    public boolean testApiKey() {
        // For mock implementation, consider any non-empty key as valid
        boolean valid = hasApiKey();
        System.out.println(TAG + " API key test result: " + valid);
        return valid;
    }
    
    /**
     * Detects the language of the given text.
     *
     * @param text The text to detect the language of
     * @return The detected language code
     */
    public String detectLanguage(String text) {
        if (text == null || text.trim().isEmpty()) {
            System.out.println(TAG + " Cannot detect language of empty text");
            return "en"; // Default to English
        }
        
        System.out.println(TAG + " Detecting language for text: " + text.substring(0, Math.min(text.length(), 50)) + "...");
        
        // Check for non-English patterns first
        for (Map.Entry<String, Pattern> entry : LANGUAGE_PATTERNS.entrySet()) {
            String language = entry.getKey();
            Pattern pattern = entry.getValue();
            
            if (pattern.matcher(text.toLowerCase()).find()) {
                System.out.println(TAG + " Detected language: " + language);
                return language;
            }
        }
        
        // Default to English if no specific pattern is found
        System.out.println(TAG + " No specific language pattern found, defaulting to English");
        return "en";
    }
    
    /**
     * Translates the given text from the source language to the target language.
     *
     * @param text The text to translate
     * @param sourceLanguage The source language code
     * @param targetLanguage The target language code
     * @return The translated text
     */
    public String translate(String text, String sourceLanguage, String targetLanguage) {
        if (text == null || text.trim().isEmpty()) {
            System.out.println(TAG + " Cannot translate empty text");
            return text;
        }
        
        if (sourceLanguage == null || sourceLanguage.trim().isEmpty() || 
            targetLanguage == null || targetLanguage.trim().isEmpty()) {
            System.out.println(TAG + " Source or target language is empty");
            return text;
        }
        
        // If source and target languages are the same, return original text
        if (sourceLanguage.equals(targetLanguage)) {
            System.out.println(TAG + " Source and target languages are the same, returning original text");
            return text;
        }
        
        System.out.println(TAG + " Translating from " + sourceLanguage + " to " + targetLanguage + ": " + 
              text.substring(0, Math.min(text.length(), 50)) + "...");
        
        // Check for exact mock translation
        String key = text.toLowerCase() + "|" + sourceLanguage + "|" + targetLanguage;
        if (MOCK_TRANSLATIONS.containsKey(key)) {
            String translation = MOCK_TRANSLATIONS.get(key);
            System.out.println(TAG + " Found exact mock translation: " + translation);
            return translation;
        }
        
        // Generate a mock translation that indicates the target language
        String mockTranslation = generateMockTranslation(text, sourceLanguage, targetLanguage);
        System.out.println(TAG + " Generated mock translation: " + mockTranslation);
        return mockTranslation;
    }
    
    /**
     * Generates a mock translation for development/testing purposes.
     *
     * @param text The original text
     * @param sourceLanguage The source language
     * @param targetLanguage The target language
     * @return A mock translated text
     */
    private String generateMockTranslation(String text, String sourceLanguage, String targetLanguage) {
        // Create a mock translation that shows it has been processed
        StringBuilder translation = new StringBuilder();
        
        // Add language indicator prefix
        switch (targetLanguage) {
            case "es":
                translation.append("[ES] ");
                break;
            case "fr":
                translation.append("[FR] ");
                break;
            case "de":
                translation.append("[DE] ");
                break;
            case "it":
                translation.append("[IT] ");
                break;
            case "pt":
                translation.append("[PT] ");
                break;
            default:
                translation.append("[").append(targetLanguage.toUpperCase()).append("] ");
                break;
        }
        
        // Add the original text with a translation indicator
        translation.append("Translated: ").append(text);
        
        return translation.toString();
    }
}