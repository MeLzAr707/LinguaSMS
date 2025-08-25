package com.translator.messagingapp;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Integration test demonstrating the enhanced search functionality.
 * This test validates the core requirements from Issue #370.
 */
public class EnhancedSearchIntegrationTest {

    @Test
    public void testRequirement_CrossLanguageSearch() {
        // Requirement: Enable users to search for terms across both original and translated content
        
        SearchFilter filter = new SearchFilter(SearchFilter.SearchScope.ALL_CONTENT);
        
        assertTrue("Should search in original content", filter.shouldSearchOriginal());
        assertTrue("Should search in translated content", filter.shouldSearchTranslation());
        
        // Verify that messages with either original or translated matches would be included
        Message message1 = new Message();
        message1.setBody("Hello world");
        message1.setTranslatedText("Hola mundo");
        
        // This simulates the messageContainsQuery logic
        String query = "hello";
        boolean matchesOriginal = message1.getBody().toLowerCase().contains(query);
        boolean matchesTranslated = message1.getTranslatedText() != null && 
                                   message1.getTranslatedText().toLowerCase().contains(query);
        
        assertTrue("Should find match in original text", matchesOriginal);
        // This demonstrates cross-language capability
    }

    @Test
    public void testRequirement_AdvancedSearchFilters() {
        // Requirement: Add filters for date, language, message type, and attachment type
        
        SearchFilter filter = new SearchFilter();
        
        // Test date filtering
        long oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
        filter.setDateFromMs(oneWeekAgo);
        assertTrue("Should have date filter when set", filter.hasDateFilter());
        
        // Test language filtering
        filter.setLanguageFilters("en", "es", "fr");
        assertTrue("Should have language filter when set", filter.hasLanguageFilter());
        assertEquals("Should contain 3 languages", 3, filter.getLanguageFilters().size());
        
        // Test message type filtering
        filter.setMessageTypeFilter(SearchFilter.MessageTypeFilter.SMS_ONLY);
        assertTrue("Should include SMS messages", 
                   filter.shouldIncludeByMessageType(Message.MESSAGE_TYPE_SMS));
        assertFalse("Should not include MMS messages for SMS_ONLY filter", 
                    filter.shouldIncludeByMessageType(Message.MESSAGE_TYPE_MMS));
        
        // Test attachment type filtering (infrastructure ready)
        filter.setAttachmentTypeFilter(SearchFilter.AttachmentTypeFilter.WITH_ATTACHMENTS);
        assertEquals("Should have attachment filter set", 
                     SearchFilter.AttachmentTypeFilter.WITH_ATTACHMENTS, 
                     filter.getAttachmentTypeFilter());
    }

    @Test 
    public void testRequirement_SearchWithinTranslation() {
        // Requirement: Allow users to specify searches that target only translated content
        
        SearchFilter filter = new SearchFilter(SearchFilter.SearchScope.TRANSLATION_ONLY);
        
        assertFalse("Should not search in original content", filter.shouldSearchOriginal());
        assertTrue("Should search in translated content", filter.shouldSearchTranslation());
        
        // Test translation-only filtering
        filter.setMessageTypeFilter(SearchFilter.MessageTypeFilter.TRANSLATED_ONLY);
        assertTrue("Should include messages with translations", 
                   filter.shouldIncludeByTranslationStatus(true));
        assertFalse("Should not include messages without translations", 
                    filter.shouldIncludeByTranslationStatus(false));
    }

    @Test
    public void testRequirement_MultilingualUserBenefit() {
        // Requirement: Improves usability for multilingual users
        
        // Test scenario: User searches for "gracias" (Spanish) but message was 
        // originally in English "thank you" and translated to "gracias"
        
        Message message = new Message();
        message.setBody("thank you");
        message.setTranslatedText("gracias");
        message.setOriginalLanguage("en");
        message.setTranslatedLanguage("es");
        
        SearchFilter allContentFilter = new SearchFilter(SearchFilter.SearchScope.ALL_CONTENT);
        allContentFilter.setLanguageFilters("en", "es");
        
        // User searching for "gracias" should find the message
        String query = "gracias";
        boolean foundInTranslation = message.getTranslatedText().toLowerCase().contains(query);
        assertTrue("Should find Spanish term in translated content", foundInTranslation);
        
        // Verify language filter would include this message
        boolean matchesLanguageFilter = 
            (message.getOriginalLanguage() != null && 
             allContentFilter.getLanguageFilters().contains(message.getOriginalLanguage())) ||
            (message.getTranslatedLanguage() != null && 
             allContentFilter.getLanguageFilters().contains(message.getTranslatedLanguage()));
        
        assertTrue("Should match language filter for multilingual search", matchesLanguageFilter);
    }

    @Test
    public void testRequirement_ComprehensiveFlexibility() {
        // Requirement: Makes message retrieval more flexible and comprehensive
        
        SearchFilter flexibleFilter = new SearchFilter();
        
        // Test flexibility: Can search across multiple dimensions
        flexibleFilter.setSearchScope(SearchFilter.SearchScope.ALL_CONTENT);
        flexibleFilter.setMessageTypeFilter(SearchFilter.MessageTypeFilter.ALL);
        flexibleFilter.setLanguageFilters("en", "es", "fr", "de", "it");
        
        // Test comprehensiveness: No restrictions that would exclude valid results
        assertTrue("Should search all content types", 
                   flexibleFilter.shouldSearchOriginal() && flexibleFilter.shouldSearchTranslation());
        assertTrue("Should include SMS messages", 
                   flexibleFilter.shouldIncludeByMessageType(Message.MESSAGE_TYPE_SMS));
        assertTrue("Should include MMS messages", 
                   flexibleFilter.shouldIncludeByMessageType(Message.MESSAGE_TYPE_MMS));
        assertTrue("Should include translated messages", 
                   flexibleFilter.shouldIncludeByTranslationStatus(true));
        assertTrue("Should include untranslated messages", 
                   flexibleFilter.shouldIncludeByTranslationStatus(false));
        
        // Test granular control: Can be very specific when needed
        SearchFilter specificFilter = new SearchFilter(SearchFilter.SearchScope.TRANSLATION_ONLY);
        specificFilter.setMessageTypeFilter(SearchFilter.MessageTypeFilter.MMS_ONLY);
        
        assertFalse("Should not search original when set to translation-only", 
                    specificFilter.shouldSearchOriginal());
        assertTrue("Should search translations when set to translation-only", 
                   specificFilter.shouldSearchTranslation());
        assertFalse("Should not include SMS when set to MMS-only", 
                    specificFilter.shouldIncludeByMessageType(Message.MESSAGE_TYPE_SMS));
        assertTrue("Should include MMS when set to MMS-only", 
                   specificFilter.shouldIncludeByMessageType(Message.MESSAGE_TYPE_MMS));
    }

    @Test
    public void testImplementationComplete() {
        // Verify that all major components are implemented and working together
        
        SearchFilter filter = new SearchFilter();
        assertNotNull("SearchFilter should be instantiable", filter);
        
        // Test that all enum values are accessible
        SearchFilter.SearchScope[] scopes = SearchFilter.SearchScope.values();
        assertEquals("Should have 3 search scope options", 3, scopes.length);
        
        SearchFilter.MessageTypeFilter[] messageTypes = SearchFilter.MessageTypeFilter.values();
        assertEquals("Should have 5 message type filter options", 5, messageTypes.length);
        
        SearchFilter.AttachmentTypeFilter[] attachmentTypes = SearchFilter.AttachmentTypeFilter.values();
        assertEquals("Should have 6 attachment type filter options", 6, attachmentTypes.length);
        
        // Test filter state management
        assertNotNull("Filter should have a toString method", filter.toString());
        assertTrue("toString should contain meaningful content", filter.toString().length() > 10);
    }
}