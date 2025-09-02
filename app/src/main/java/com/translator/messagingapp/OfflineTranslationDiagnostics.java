package com.translator.messagingapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.mlkit.nl.translate.TranslateLanguage;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Diagnostic tool for analyzing offline translation functionality and identifying issues.
 * This tool helps diagnose synchronization problems, model state inconsistencies, 
 * and other issues that prevent offline translation from working properly.
 */
public class OfflineTranslationDiagnostics {
    private static final String TAG = "OfflineTranslationDiagnostics";
    
    private final Context context;
    private final OfflineModelManager modelManager;
    private final OfflineTranslationService translationService;
    private final UserPreferences userPreferences;
    
    public static class DiagnosticResult {
        public final String component;
        public final String issue;
        public final String severity; // "ERROR", "WARNING", "INFO"
        public final String description;
        public final String recommendation;
        
        public DiagnosticResult(String component, String issue, String severity, String description, String recommendation) {
            this.component = component;
            this.issue = issue;
            this.severity = severity;
            this.description = description;
            this.recommendation = recommendation;
        }
        
        @Override
        public String toString() {
            return String.format("[%s] %s: %s - %s\nRecommendation: %s\n", 
                severity, component, issue, description, recommendation);
        }
    }
    
    public OfflineTranslationDiagnostics(Context context) {
        this.context = context;
        this.userPreferences = new UserPreferences(context);
        this.modelManager = new OfflineModelManager(context);
        this.translationService = new OfflineTranslationService(context, userPreferences);
    }
    
    /**
     * Performs comprehensive diagnostics of offline translation functionality.
     * @return Map of diagnostic results organized by category
     */
    public Map<String, DiagnosticResult[]> performComprehensiveDiagnostics() {
        Map<String, DiagnosticResult[]> results = new HashMap<>();
        
        results.put("settings", diagnoseSettings());
        results.put("synchronization", diagnoseSynchronization());
        results.put("models", diagnoseModelStates());
        results.put("language_codes", diagnoseLanguageCodeHandling());
        results.put("functionality", diagnoseFunctionality());
        
        return results;
    }
    
    /**
     * Diagnoses user settings related to offline translation.
     */
    private DiagnosticResult[] diagnoseSettings() {
        DiagnosticResult[] results = new DiagnosticResult[3];
        
        // Check if offline translation is enabled
        boolean offlineEnabled = userPreferences.isOfflineTranslationEnabled();
        results[0] = new DiagnosticResult(
            "UserPreferences",
            "Offline Translation Enabled",
            offlineEnabled ? "INFO" : "WARNING",
            "Offline translation enabled: " + offlineEnabled,
            offlineEnabled ? "Setting is correct" : "Enable offline translation in Settings > Offline Translation"
        );
        
        // Check if prefer offline is set
        boolean preferOffline = userPreferences.getPreferOfflineTranslation();
        results[1] = new DiagnosticResult(
            "UserPreferences", 
            "Prefer Offline Translation",
            "INFO",
            "Prefer offline translation: " + preferOffline,
            "This setting controls whether to try offline first"
        );
        
        // Check target language
        String targetLang = userPreferences.getPreferredLanguage();
        results[2] = new DiagnosticResult(
            "UserPreferences",
            "Target Language",
            "INFO", 
            "Preferred language: " + targetLang,
            "Ensure target language models are downloaded"
        );
        
        return results;
    }
    
    /**
     * Diagnoses synchronization between OfflineModelManager and OfflineTranslationService.
     */
    private DiagnosticResult[] diagnoseSynchronization() {
        DiagnosticResult[] results = new DiagnosticResult[2];
        
        // Check SharedPreferences synchronization
        SharedPreferences modelPrefs = context.getSharedPreferences("offline_models", Context.MODE_PRIVATE);
        Set<String> storedModels = modelPrefs.getStringSet("downloaded_models", null);
        
        results[0] = new DiagnosticResult(
            "SharedPreferences",
            "Model Storage",
            storedModels != null ? "INFO" : "WARNING",
            "Stored models in SharedPreferences: " + (storedModels != null ? storedModels.size() : 0),
            storedModels != null ? "Models are stored correctly" : "No models found in storage - download models first"
        );
        
        // Check OfflineTranslationService model tracking
        Set<String> serviceModels = translationService.getDownloadedModels();
        boolean syncMatches = storedModels != null && storedModels.size() == serviceModels.size();
        
        results[1] = new DiagnosticResult(
            "Synchronization",
            "Service/Manager Sync",
            syncMatches ? "INFO" : "ERROR",
            String.format("OfflineModelManager models: %d, OfflineTranslationService models: %d", 
                storedModels != null ? storedModels.size() : 0, serviceModels.size()),
            syncMatches ? "Components are synchronized" : "CRITICAL: Components out of sync - restart app or re-download models"
        );
        
        return results;
    }
    
    /**
     * Diagnoses individual model states and integrity.
     */
    private DiagnosticResult[] diagnoseModelStates() {
        String[] testLanguages = {"en", "es", "fr", "de"};
        DiagnosticResult[] results = new DiagnosticResult[testLanguages.length];
        
        for (int i = 0; i < testLanguages.length; i++) {
            String lang = testLanguages[i];
            
            boolean managerSaysDownloaded = modelManager.isModelDownloaded(lang);
            boolean managerSaysVerified = modelManager.isModelDownloadedAndVerified(lang);
            boolean serviceSaysDownloaded = translationService.isLanguageModelDownloaded(lang);
            
            String status = String.format("Manager downloaded: %s, verified: %s | Service downloaded: %s", 
                managerSaysDownloaded, managerSaysVerified, serviceSaysDownloaded);
            
            String severity = "INFO";
            String recommendation = "Model state is consistent";
            
            if (managerSaysDownloaded != serviceSaysDownloaded) {
                severity = "ERROR";
                recommendation = "CRITICAL: Manager and Service disagree on model state - restart app or re-download model";
            } else if (managerSaysDownloaded && !managerSaysVerified) {
                severity = "WARNING";
                recommendation = "Model downloaded but not verified - may be corrupted, consider re-downloading";
            } else if (!managerSaysDownloaded) {
                severity = "INFO";
                recommendation = "Model not downloaded - download if needed for this language";
            }
            
            results[i] = new DiagnosticResult(
                "ModelState",
                lang.toUpperCase() + " Model",
                severity,
                status,
                recommendation
            );
        }
        
        return results;
    }
    
    /**
     * Diagnoses language code conversion and handling.
     */
    private DiagnosticResult[] diagnoseLanguageCodeHandling() {
        String[] testCodes = {"en", "es", "zh", "zh-CN", "pt", "pt-BR"};
        DiagnosticResult[] results = new DiagnosticResult[testCodes.length];
        
        for (int i = 0; i < testCodes.length; i++) {
            String code = testCodes[i];
            
            // Test conversion to MLKit format
            String mlkitCode = convertToMLKitLanguageCode(code);
            String backConverted = convertFromMLKitLanguageCode(mlkitCode);
            
            boolean conversionWorks = mlkitCode != null && backConverted != null;
            boolean roundTripWorks = conversionWorks && (code.equals(backConverted) || code.startsWith(backConverted));
            
            String status = String.format("%s -> %s -> %s", code, mlkitCode, backConverted);
            String severity = roundTripWorks ? "INFO" : "WARNING";
            String recommendation = roundTripWorks ? "Language code conversion works" : 
                "Language code conversion may cause issues - verify model availability manually";
            
            results[i] = new DiagnosticResult(
                "LanguageCodes",
                code + " Conversion",
                severity,
                status,
                recommendation
            );
        }
        
        return results;
    }
    
    /**
     * Diagnoses actual translation functionality.
     */
    private DiagnosticResult[] diagnoseFunctionality() {
        DiagnosticResult[] results = new DiagnosticResult[3];
        
        // Test basic availability check
        boolean enEsAvailable = translationService.isOfflineTranslationAvailable("en", "es");
        results[0] = new DiagnosticResult(
            "Functionality",
            "EN-ES Availability", 
            enEsAvailable ? "INFO" : "WARNING",
            "English to Spanish offline translation available: " + enEsAvailable,
            enEsAvailable ? "Basic language pair works" : "Download EN and ES models if needed"
        );
        
        // Check supported languages
        String[] supportedLangs = translationService.getSupportedLanguages();
        results[1] = new DiagnosticResult(
            "Functionality",
            "Supported Languages",
            "INFO",
            "Supported languages count: " + supportedLangs.length,
            "Service supports " + supportedLangs.length + " languages"
        );
        
        // Check model directory
        File modelDir = new File(context.getFilesDir(), "offline_models");
        boolean modelDirExists = modelDir.exists();
        int modelFileCount = modelDirExists ? (modelDir.listFiles() != null ? modelDir.listFiles().length : 0) : 0;
        
        results[2] = new DiagnosticResult(
            "Storage",
            "Model Files",
            modelDirExists ? "INFO" : "WARNING",
            String.format("Model directory exists: %s, files: %d", modelDirExists, modelFileCount),
            modelDirExists ? "Model storage is set up correctly" : "Model directory missing - models may not be properly stored"
        );
        
        return results;
    }
    
    /**
     * Generates a comprehensive diagnostic report as a formatted string.
     */
    public String generateDiagnosticReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== OFFLINE TRANSLATION DIAGNOSTIC REPORT ===\n\n");
        
        Map<String, DiagnosticResult[]> results = performComprehensiveDiagnostics();
        
        for (Map.Entry<String, DiagnosticResult[]> category : results.entrySet()) {
            report.append("== ").append(category.getKey().toUpperCase()).append(" ==\n");
            
            for (DiagnosticResult result : category.getValue()) {
                report.append(result.toString()).append("\n");
            }
            
            report.append("\n");
        }
        
        report.append("=== SUMMARY ===\n");
        report.append("Run this diagnostic if offline translation is not working.\n");
        report.append("Focus on ERROR and WARNING items first.\n");
        report.append("For synchronization errors, try restarting the app or re-downloading models.\n");
        
        return report.toString();
    }
    
    /**
     * Logs diagnostic results to Android log for debugging.
     */
    public void logDiagnosticResults() {
        String report = generateDiagnosticReport();
        Log.i(TAG, report);
    }
    
    // Helper methods copied from OfflineTranslationService for testing conversion
    private String convertToMLKitLanguageCode(String languageCode) {
        if (languageCode == null) {
            return null;
        }

        String baseCode = languageCode.split("-")[0].toLowerCase();

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
            default: return null;
        }
    }
    
    private String convertFromMLKitLanguageCode(String mlkitLanguageCode) {
        if (mlkitLanguageCode == null) {
            return null;
        }

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
            default: return null;
        }
    }
}