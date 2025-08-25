package com.translator.messagingapp;

import android.content.Context;

/**
 * Example class showing how contact language preferences would be integrated
 * into the translation workflow for outgoing messages.
 * 
 * This demonstrates the expected usage pattern for the new feature.
 */
public class ContactLanguageIntegrationExample {

    /**
     * Example method showing how to get the target language for translation
     * when sending a message to a specific contact.
     * 
     * This is how the feature would be used in the actual messaging workflow.
     */
    public static String getTargetLanguageForMessage(Context context, String recipientPhoneNumber) {
        // Use the new contact language preference system
        return ContactUtils.getEffectiveOutgoingLanguageForContact(context, recipientPhoneNumber);
    }

    /**
     * Example of how to set up language preferences for contacts.
     * This would typically be called from a UI settings screen for contacts.
     */
    public static void setupContactLanguagePreferences(Context context) {
        // Set up some example contact language preferences
        
        // Spanish speaker
        ContactUtils.setContactLanguagePreference(context, "1234567890", "es");
        
        // French speaker  
        ContactUtils.setContactLanguagePreference(context, "9876543210", "fr");
        
        // German speaker
        ContactUtils.setContactLanguagePreference(context, "5555555555", "de");
        
        // No preference set for this contact - will use global settings
        // ContactUtils.setContactLanguagePreference(context, "1111111111", null);
    }

    /**
     * Example of how the translation logic would work with the new system.
     * This shows the integration point for the existing translation workflow.
     */
    public static class TranslationWorkflowExample {
        
        private Context context;
        private UserPreferences userPreferences;
        
        public TranslationWorkflowExample(Context context) {
            this.context = context;
            this.userPreferences = new UserPreferences(context);
        }
        
        /**
         * Example method showing how to translate a message for a specific recipient.
         * This replaces the old approach of using only global language preferences.
         */
        public String translateMessageForRecipient(String message, String recipientPhoneNumber) {
            // Get the effective language for this specific contact
            String targetLanguage = ContactUtils.getEffectiveOutgoingLanguageForContact(context, recipientPhoneNumber);
            
            // This would integrate with the existing TranslationManager
            // return translationManager.translateText(message, "auto", targetLanguage);
            
            // For demonstration, just return the target language that would be used
            return "Would translate to: " + targetLanguage + " for recipient: " + recipientPhoneNumber;
        }
        
        /**
         * Example showing the fallback logic in action.
         */
        public void demonstrateFallbackLogic() {
            // Set up global preferences
            userPreferences.setPreferredLanguage("en");           // General preference
            userPreferences.setPreferredOutgoingLanguage("fr");   // Outgoing preference
            
            String contact1 = "1234567890";  // Has specific preference set to Spanish
            String contact2 = "9999999999";  // No specific preference set
            
            // Set specific preference for contact1
            ContactUtils.setContactLanguagePreference(context, contact1, "es");
            
            // Get effective languages
            String lang1 = ContactUtils.getEffectiveOutgoingLanguageForContact(context, contact1);
            String lang2 = ContactUtils.getEffectiveOutgoingLanguageForContact(context, contact2);
            
            // lang1 should be "es" (contact-specific preference)
            // lang2 should be "fr" (global outgoing preference)
            
            System.out.println("Contact with specific preference gets: " + lang1);
            System.out.println("Contact without specific preference gets: " + lang2);
        }
    }

    /**
     * Example showing how to manage contact language preferences in a UI context.
     */
    public static class ContactLanguageManagementExample {
        
        private Context context;
        
        public ContactLanguageManagementExample(Context context) {
            this.context = context;
        }
        
        /**
         * Set language preference for a contact (e.g., from a contact settings screen).
         */
        public void setContactLanguage(String phoneNumber, String languageCode) {
            ContactUtils.setContactLanguagePreference(context, phoneNumber, languageCode);
        }
        
        /**
         * Get current language preference for a contact (e.g., to display in UI).
         */
        public String getContactLanguage(String phoneNumber) {
            return ContactUtils.getContactLanguagePreference(context, phoneNumber);
        }
        
        /**
         * Check if a contact has a specific language preference set.
         */
        public boolean hasContactLanguageSet(String phoneNumber) {
            return ContactUtils.hasContactLanguagePreference(context, phoneNumber);
        }
        
        /**
         * Remove language preference for a contact (reset to global settings).
         */
        public void resetContactLanguage(String phoneNumber) {
            ContactUtils.removeContactLanguagePreference(context, phoneNumber);
        }
        
        /**
         * Get the display text for what language will be used for a contact.
         */
        public String getEffectiveLanguageDisplayText(String phoneNumber) {
            String contactSpecific = ContactUtils.getContactLanguagePreference(context, phoneNumber);
            if (contactSpecific != null) {
                return "Contact preference: " + getLanguageName(contactSpecific);
            }
            
            String effective = ContactUtils.getEffectiveOutgoingLanguageForContact(context, phoneNumber);
            return "Global setting: " + getLanguageName(effective);
        }
        
        /**
         * Helper method to get human-readable language names.
         */
        private String getLanguageName(String languageCode) {
            switch (languageCode) {
                case "en": return "English";
                case "es": return "Spanish";
                case "fr": return "French";
                case "de": return "German";
                case "it": return "Italian";
                case "pt": return "Portuguese";
                case "ru": return "Russian";
                case "zh": return "Chinese";
                case "ja": return "Japanese";
                case "ko": return "Korean";
                case "ar": return "Arabic";
                case "hi": return "Hindi";
                default: return languageCode;
            }
        }
    }
}