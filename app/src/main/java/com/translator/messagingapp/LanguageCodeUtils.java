package com.translator.messagingapp;

import com.google.mlkit.nl.translate.TranslateLanguage;

/**
 * Utility class for language code conversions.
 * Centralizes the mapping between standard language codes and MLKit language codes
 * to avoid duplication between OfflineTranslationService and OfflineModelManager.
 */
public class LanguageCodeUtils {
    
    /**
     * Converts standard language codes to MLKit language codes.
     *
     * @param languageCode The standard language code
     * @return The MLKit language code, or null if not supported
     */
    public static String convertToMLKitLanguageCode(String languageCode) {
        if (languageCode == null) {
            return null;
        }

        // Remove region code if present (e.g., "en-US" -> "en")
        String baseCode = languageCode.split("-")[0].toLowerCase();

        // Map common language codes to MLKit codes
        switch (baseCode) {
            case "en": return TranslateLanguage.ENGLISH;
            case "es": return TranslateLanguage.SPANISH;
            case "fr": return TranslateLanguage.FRENCH;
            case "de": return TranslateLanguage.GERMAN;
            case "it": return TranslateLanguage.ITALIAN;
            case "pt": return TranslateLanguage.PORTUGUESE;
            case "ru": return TranslateLanguage.RUSSIAN;
            case "zh": return TranslateLanguage.CHINESE;
            case "ja": return TranslateLanguage.JAPANESE;
            case "ko": return TranslateLanguage.KOREAN;
            case "ar": return TranslateLanguage.ARABIC;
            case "hi": return TranslateLanguage.HINDI;
            case "nl": return TranslateLanguage.DUTCH;
            case "sv": return TranslateLanguage.SWEDISH;
            case "da": return TranslateLanguage.DANISH;
            case "no": return TranslateLanguage.NORWEGIAN;
            case "fi": return TranslateLanguage.FINNISH;
            case "pl": return TranslateLanguage.POLISH;
            case "cs": return TranslateLanguage.CZECH;
            case "sk": return TranslateLanguage.SLOVAK;
            case "hu": return TranslateLanguage.HUNGARIAN;
            case "ro": return TranslateLanguage.ROMANIAN;
            case "bg": return TranslateLanguage.BULGARIAN;
            case "hr": return TranslateLanguage.CROATIAN;
            case "sl": return TranslateLanguage.SLOVENIAN;
            case "et": return TranslateLanguage.ESTONIAN;
            case "lv": return TranslateLanguage.LATVIAN;
            case "lt": return TranslateLanguage.LITHUANIAN;
            case "th": return TranslateLanguage.THAI;
            case "vi": return TranslateLanguage.VIETNAMESE;
            case "id": return TranslateLanguage.INDONESIAN;
            case "ms": return TranslateLanguage.MALAY;
            case "tl": return TranslateLanguage.TAGALOG;
            case "sw": return TranslateLanguage.SWAHILI;
            case "tr": return TranslateLanguage.TURKISH;
            case "he": return TranslateLanguage.HEBREW;
            case "fa": return TranslateLanguage.PERSIAN;
            case "ur": return TranslateLanguage.URDU;
            case "bn": return TranslateLanguage.BENGALI;
            case "gu": return TranslateLanguage.GUJARATI;
            case "kn": return TranslateLanguage.KANNADA;
            case "mr": return TranslateLanguage.MARATHI;
            case "ta": return TranslateLanguage.TAMIL;
            case "te": return TranslateLanguage.TELUGU;
            default: return null; // Unsupported language
        }
    }

    /**
     * Converts MLKit language codes back to standard language codes.
     *
     * @param mlkitLanguageCode The MLKit language code
     * @return The standard language code, or null if not recognized
     */
    public static String convertFromMLKitLanguageCode(String mlkitLanguageCode) {
        if (mlkitLanguageCode == null) {
            return null;
        }

        // Map MLKit codes back to standard language codes
        switch (mlkitLanguageCode) {
            case TranslateLanguage.ENGLISH: return "en";
            case TranslateLanguage.SPANISH: return "es";
            case TranslateLanguage.FRENCH: return "fr";
            case TranslateLanguage.GERMAN: return "de";
            case TranslateLanguage.ITALIAN: return "it";
            case TranslateLanguage.PORTUGUESE: return "pt";
            case TranslateLanguage.RUSSIAN: return "ru";
            case TranslateLanguage.CHINESE: return "zh";
            case TranslateLanguage.JAPANESE: return "ja";
            case TranslateLanguage.KOREAN: return "ko";
            case TranslateLanguage.ARABIC: return "ar";
            case TranslateLanguage.HINDI: return "hi";
            case TranslateLanguage.DUTCH: return "nl";
            case TranslateLanguage.SWEDISH: return "sv";
            case TranslateLanguage.DANISH: return "da";
            case TranslateLanguage.NORWEGIAN: return "no";
            case TranslateLanguage.FINNISH: return "fi";
            case TranslateLanguage.POLISH: return "pl";
            case TranslateLanguage.CZECH: return "cs";
            case TranslateLanguage.SLOVAK: return "sk";
            case TranslateLanguage.HUNGARIAN: return "hu";
            case TranslateLanguage.ROMANIAN: return "ro";
            case TranslateLanguage.BULGARIAN: return "bg";
            case TranslateLanguage.CROATIAN: return "hr";
            case TranslateLanguage.SLOVENIAN: return "sl";
            case TranslateLanguage.ESTONIAN: return "et";
            case TranslateLanguage.LATVIAN: return "lv";
            case TranslateLanguage.LITHUANIAN: return "lt";
            case TranslateLanguage.THAI: return "th";
            case TranslateLanguage.VIETNAMESE: return "vi";
            case TranslateLanguage.INDONESIAN: return "id";
            case TranslateLanguage.MALAY: return "ms";
            case TranslateLanguage.TAGALOG: return "tl";
            case TranslateLanguage.SWAHILI: return "sw";
            case TranslateLanguage.TURKISH: return "tr";
            case TranslateLanguage.HEBREW: return "he";
            case TranslateLanguage.PERSIAN: return "fa";
            case TranslateLanguage.URDU: return "ur";
            case TranslateLanguage.BENGALI: return "bn";
            case TranslateLanguage.GUJARATI: return "gu";
            case TranslateLanguage.KANNADA: return "kn";
            case TranslateLanguage.MARATHI: return "mr";
            case TranslateLanguage.TAMIL: return "ta";
            case TranslateLanguage.TELUGU: return "te";
            default: return null; // Unknown MLKit language code
        }
    }
}