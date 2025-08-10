package com.translator.messagingapp;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Service for Google Translation API.
 * This implementation provides mock translation functionality for development/testing.
 * In a production environment, this would integrate with actual Google Translate API.
 */
public class GoogleTranslationService {
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
        LANGUAGE_PATTERNS.put("it", Pattern.compile("\\b(il|di|che|e|la|per|un|in|con|del|da|al|le|si|è|non|come|lo|nel|alla|su|una|sono|anche|più|essere|tutto|questo|se|degli|delle|dalla|dal|alle|ma|quando|dove|chi|cosa|molto|tempo|stesso|fatto|fare|ancora|dopo|mentre|prima|senza|sotto|sopra|attraverso)\\b"));
        LANGUAGE_PATTERNS.put("pt", Pattern.compile("\\b(o|de|a|e|do|da|em|um|para|é|com|não|uma|os|no|se|na|por|mais|as|dos|como|mas|foi|ao|ele|das|tem|à|seu|sua|ou|ser|quando|muito|há|nos|já|está|eu|também|só|pelo|pela|até|isso|ela|entre|era|depois|sem|sob|sobre|durante|antes|contra|todos)\\b"));
        LANGUAGE_PATTERNS.put("ru", Pattern.compile("[а-яё]+", Pattern.CASE_INSENSITIVE));
        LANGUAGE_PATTERNS.put("zh", Pattern.compile("[\\u4e00-\\u9fff]+"));
        LANGUAGE_PATTERNS.put("ja", Pattern.compile("[ひらがなカタカナ\\u3040-\\u309f\\u30a0-\\u30ff\\u4e00-\\u9faf]+"));
        LANGUAGE_PATTERNS.put("ko", Pattern.compile("[\\uac00-\\ud7af\\u1100-\\u11ff\\u3130-\\u318f\\ua960-\\ua97f\\ud7b0-\\ud7ff]+"));
        LANGUAGE_PATTERNS.put("ar", Pattern.compile("[\\u0600-\\u06ff\\u0750-\\u077f\\u08a0-\\u08ff\\ufb50-\\ufdff\\ufe70-\\ufeff]+"));
        LANGUAGE_PATTERNS.put("hi", Pattern.compile("[\\u0900-\\u097f]+"));
        
        // Initialize mock translations for common phrases
        MOCK_TRANSLATIONS.put("hello|en|es", "hola");
        MOCK_TRANSLATIONS.put("hello|en|fr", "bonjour");
        MOCK_TRANSLATIONS.put("hello|en|de", "hallo");
        MOCK_TRANSLATIONS.put("hello|en|it", "ciao");
        MOCK_TRANSLATIONS.put("hello|en|pt", "olá");
        MOCK_TRANSLATIONS.put("how are you|en|es", "¿cómo estás?");
        MOCK_TRANSLATIONS.put("how are you|en|fr", "comment ça va?");
        MOCK_TRANSLATIONS.put("how are you|en|de", "wie geht es dir?");
        MOCK_TRANSLATIONS.put("good morning|en|es", "buenos días");
        MOCK_TRANSLATIONS.put("good morning|en|fr", "bonjour");
        MOCK_TRANSLATIONS.put("good morning|en|de", "guten morgen");
        MOCK_TRANSLATIONS.put("thank you|en|es", "gracias");
        MOCK_TRANSLATIONS.put("thank you|en|fr", "merci");
        MOCK_TRANSLATIONS.put("thank you|en|de", "danke");
        MOCK_TRANSLATIONS.put("yes|en|es", "sí");
        MOCK_TRANSLATIONS.put("yes|en|fr", "oui");
        MOCK_TRANSLATIONS.put("yes|en|de", "ja");
        MOCK_TRANSLATIONS.put("no|en|es", "no");
        MOCK_TRANSLATIONS.put("no|en|fr", "non");
        MOCK_TRANSLATIONS.put("no|en|de", "nein");
        
        // Reverse translations
        MOCK_TRANSLATIONS.put("hola|es|en", "hello");
        MOCK_TRANSLATIONS.put("bonjour|fr|en", "hello");
        MOCK_TRANSLATIONS.put("hallo|de|en", "hello");
        MOCK_TRANSLATIONS.put("¿cómo estás?|es|en", "how are you?");
        MOCK_TRANSLATIONS.put("comment ça va?|fr|en", "how are you?");
        MOCK_TRANSLATIONS.put("wie geht es dir?|de|en", "how are you?");
        MOCK_TRANSLATIONS.put("gracias|es|en", "thank you");
        MOCK_TRANSLATIONS.put("merci|fr|en", "thank you");
        MOCK_TRANSLATIONS.put("danke|de|en", "thank you");
    }
    
    /**
     * Creates a new GoogleTranslationService.
     *
     * @param apiKey The API key
     */
    public GoogleTranslationService(String apiKey) {
        this.apiKey = apiKey;
        Log.d(TAG, "GoogleTranslationService initialized with API key: " + (hasApiKey() ? "provided" : "not provided"));
    }

    /**
     * Creates a new GoogleTranslationService with context.
     *
     * @param context The application context
     */
    public GoogleTranslationService(Context context) {
        // Get API key from preferences or other source
        UserPreferences preferences = new UserPreferences(context);
        this.apiKey = preferences.getApiKey();
        Log.d(TAG, "GoogleTranslationService initialized from context with API key: " + (hasApiKey() ? "provided" : "not provided"));
    }
    
    /**
     * Sets the API key.
     *
     * @param apiKey The API key
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        Log.d(TAG, "API key updated: " + (hasApiKey() ? "provided" : "cleared"));
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
        Log.d(TAG, "API key test result: " + valid);
        return valid;
    }
    
    /**
     * Detects the language of the given text.
     *
     * @param text The text to detect the language of
     * @return The detected language code
     */
    public String detectLanguage(String text) {
        if (TextUtils.isEmpty(text)) {
            Log.w(TAG, "Cannot detect language of empty text");
            return "en"; // Default to English
        }
        
        Log.d(TAG, "Detecting language for text: " + text.substring(0, Math.min(text.length(), 50)) + "...");
        
        // Check for non-English patterns first
        for (Map.Entry<String, Pattern> entry : LANGUAGE_PATTERNS.entrySet()) {
            String language = entry.getKey();
            Pattern pattern = entry.getValue();
            
            if (pattern.matcher(text.toLowerCase()).find()) {
                Log.d(TAG, "Detected language: " + language);
                return language;
            }
        }
        
        // Default to English if no specific pattern is found
        Log.d(TAG, "No specific language pattern found, defaulting to English");
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
        if (TextUtils.isEmpty(text)) {
            Log.w(TAG, "Cannot translate empty text");
            return text;
        }
        
        if (TextUtils.isEmpty(sourceLanguage) || TextUtils.isEmpty(targetLanguage)) {
            Log.w(TAG, "Source or target language is empty");
            return text;
        }
        
        // If source and target languages are the same, return original text
        if (sourceLanguage.equals(targetLanguage)) {
            Log.d(TAG, "Source and target languages are the same, returning original text");
            return text;
        }
        
        Log.d(TAG, "Translating from " + sourceLanguage + " to " + targetLanguage + ": " + 
              text.substring(0, Math.min(text.length(), 50)) + "...");
        
        // Check for exact mock translation
        String key = text.toLowerCase() + "|" + sourceLanguage + "|" + targetLanguage;
        if (MOCK_TRANSLATIONS.containsKey(key)) {
            String translation = MOCK_TRANSLATIONS.get(key);
            Log.d(TAG, "Found exact mock translation: " + translation);
            return translation;
        }
        
        // Generate a mock translation that indicates the target language
        String mockTranslation = generateMockTranslation(text, sourceLanguage, targetLanguage);
        Log.d(TAG, "Generated mock translation: " + mockTranslation);
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
            case "ru":
                translation.append("[RU] ");
                break;
            case "zh":
                translation.append("[ZH] ");
                break;
            case "ja":
                translation.append("[JA] ");
                break;
            case "ko":
                translation.append("[KO] ");
                break;
            case "ar":
                translation.append("[AR] ");
                break;
            case "hi":
                translation.append("[HI] ");
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