package com.translator.messagingapp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for enhanced search functionality with translation support.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class EnhancedSearchTest {

    @Mock
    private TranslationCache mockTranslationCache;

    private SearchFilter searchFilter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        searchFilter = new SearchFilter();
    }

    @Test
    public void testSearchFilter_defaultSettings() {
        SearchFilter filter = new SearchFilter();
        
        assertEquals("Default search scope should be ALL_CONTENT", 
                     SearchFilter.SearchScope.ALL_CONTENT, filter.getSearchScope());
        assertEquals("Default message type filter should be ALL", 
                     SearchFilter.MessageTypeFilter.ALL, filter.getMessageTypeFilter());
        assertEquals("Default attachment type filter should be ALL", 
                     SearchFilter.AttachmentTypeFilter.ALL, filter.getAttachmentTypeFilter());
        
        assertTrue("Should search original text by default", filter.shouldSearchOriginal());
        assertTrue("Should search translation by default", filter.shouldSearchTranslation());
    }

    @Test
    public void testSearchFilter_scopeSettings() {
        // Test ORIGINAL_ONLY scope
        searchFilter.setSearchScope(SearchFilter.SearchScope.ORIGINAL_ONLY);
        assertTrue("Should search original for ORIGINAL_ONLY", searchFilter.shouldSearchOriginal());
        assertFalse("Should not search translation for ORIGINAL_ONLY", searchFilter.shouldSearchTranslation());
        
        // Test TRANSLATION_ONLY scope
        searchFilter.setSearchScope(SearchFilter.SearchScope.TRANSLATION_ONLY);
        assertFalse("Should not search original for TRANSLATION_ONLY", searchFilter.shouldSearchOriginal());
        assertTrue("Should search translation for TRANSLATION_ONLY", searchFilter.shouldSearchTranslation());
        
        // Test ALL_CONTENT scope
        searchFilter.setSearchScope(SearchFilter.SearchScope.ALL_CONTENT);
        assertTrue("Should search original for ALL_CONTENT", searchFilter.shouldSearchOriginal());
        assertTrue("Should search translation for ALL_CONTENT", searchFilter.shouldSearchTranslation());
    }

    @Test
    public void testSearchFilter_messageTypeFiltering() {
        // Test SMS only
        searchFilter.setMessageTypeFilter(SearchFilter.MessageTypeFilter.SMS_ONLY);
        assertTrue("Should include SMS messages", 
                   searchFilter.shouldIncludeByMessageType(Message.MESSAGE_TYPE_SMS));
        assertFalse("Should not include MMS messages for SMS_ONLY", 
                    searchFilter.shouldIncludeByMessageType(Message.MESSAGE_TYPE_MMS));
        
        // Test MMS only
        searchFilter.setMessageTypeFilter(SearchFilter.MessageTypeFilter.MMS_ONLY);
        assertFalse("Should not include SMS messages for MMS_ONLY", 
                    searchFilter.shouldIncludeByMessageType(Message.MESSAGE_TYPE_SMS));
        assertTrue("Should include MMS messages", 
                   searchFilter.shouldIncludeByMessageType(Message.MESSAGE_TYPE_MMS));
        
        // Test ALL
        searchFilter.setMessageTypeFilter(SearchFilter.MessageTypeFilter.ALL);
        assertTrue("Should include SMS messages for ALL", 
                   searchFilter.shouldIncludeByMessageType(Message.MESSAGE_TYPE_SMS));
        assertTrue("Should include MMS messages for ALL", 
                   searchFilter.shouldIncludeByMessageType(Message.MESSAGE_TYPE_MMS));
    }

    @Test
    public void testSearchFilter_translationStatusFiltering() {
        // Test TRANSLATED_ONLY
        searchFilter.setMessageTypeFilter(SearchFilter.MessageTypeFilter.TRANSLATED_ONLY);
        assertTrue("Should include messages with translation", 
                   searchFilter.shouldIncludeByTranslationStatus(true));
        assertFalse("Should not include messages without translation for TRANSLATED_ONLY", 
                    searchFilter.shouldIncludeByTranslationStatus(false));
        
        // Test UNTRANSLATED_ONLY
        searchFilter.setMessageTypeFilter(SearchFilter.MessageTypeFilter.UNTRANSLATED_ONLY);
        assertFalse("Should not include messages with translation for UNTRANSLATED_ONLY", 
                    searchFilter.shouldIncludeByTranslationStatus(true));
        assertTrue("Should include messages without translation", 
                   searchFilter.shouldIncludeByTranslationStatus(false));
        
        // Test ALL
        searchFilter.setMessageTypeFilter(SearchFilter.MessageTypeFilter.ALL);
        assertTrue("Should include messages with translation for ALL", 
                   searchFilter.shouldIncludeByTranslationStatus(true));
        assertTrue("Should include messages without translation for ALL", 
                   searchFilter.shouldIncludeByTranslationStatus(false));
    }

    @Test
    public void testSearchFilter_dateFiltering() {
        assertFalse("Should not have date filter by default", searchFilter.hasDateFilter());
        
        long fromDate = System.currentTimeMillis() - 86400000; // 1 day ago
        long toDate = System.currentTimeMillis();
        
        searchFilter.setDateFromMs(fromDate);
        assertTrue("Should have date filter when fromDate is set", searchFilter.hasDateFilter());
        assertEquals("Should return correct fromDate", fromDate, (long) searchFilter.getDateFromMs());
        
        searchFilter.setDateToMs(toDate);
        assertEquals("Should return correct toDate", toDate, (long) searchFilter.getDateToMs());
    }

    @Test
    public void testSearchFilter_languageFiltering() {
        assertFalse("Should not have language filter by default", searchFilter.hasLanguageFilter());
        
        searchFilter.setLanguageFilters("en", "es", "fr");
        assertTrue("Should have language filter when languages are set", searchFilter.hasLanguageFilter());
        
        List<String> languages = searchFilter.getLanguageFilters();
        assertTrue("Should contain English", languages.contains("en"));
        assertTrue("Should contain Spanish", languages.contains("es"));
        assertTrue("Should contain French", languages.contains("fr"));
        assertEquals("Should have 3 languages", 3, languages.size());
    }

    @Test
    public void testSearchFilter_addressFiltering() {
        assertFalse("Should not have address filter by default", searchFilter.hasAddressFilter());
        
        searchFilter.setAddressFilter("+1234567890");
        assertTrue("Should have address filter when address is set", searchFilter.hasAddressFilter());
        assertEquals("Should return correct address", "+1234567890", searchFilter.getAddressFilter());
        
        searchFilter.setAddressFilter("");
        assertFalse("Should not have address filter when address is empty", searchFilter.hasAddressFilter());
        
        searchFilter.setAddressFilter("   ");
        assertFalse("Should not have address filter when address is whitespace", searchFilter.hasAddressFilter());
    }

    @Test
    public void testMessage_translationStatusCheck() {
        Message message = new Message();
        
        // Test with no translation
        assertFalse("Message without translated text should not be translated", message.isTranslated());
        
        // Test with empty translation
        message.setTranslatedText("");
        assertFalse("Message with empty translated text should not be translated", message.isTranslated());
        
        // Test with null translation
        message.setTranslatedText(null);
        assertFalse("Message with null translated text should not be translated", message.isTranslated());
        
        // Test with actual translation
        message.setTranslatedText("Hola mundo");
        assertTrue("Message with translated text should be translated", message.isTranslated());
    }

    @Test
    public void testMessage_searchMetadata() {
        Message message = new Message();
        
        assertFalse("Message should not have search query by default", message.hasSearchQuery());
        
        message.setSearchQuery("test query");
        assertTrue("Message should have search query when set", message.hasSearchQuery());
        assertEquals("Should return correct search query", "test query", message.getSearchQuery());
        
        message.setSearchQuery("");
        assertFalse("Message should not have search query when empty", message.hasSearchQuery());
        
        message.setSearchQuery(null);
        assertFalse("Message should not have search query when null", message.hasSearchQuery());
    }

    @Test
    public void testSearchFilter_comprehensiveScenario() {
        // Create a comprehensive search filter
        SearchFilter filter = new SearchFilter(SearchFilter.SearchScope.ALL_CONTENT);
        filter.setMessageTypeFilter(SearchFilter.MessageTypeFilter.TRANSLATED_ONLY);
        filter.setLanguageFilters(Arrays.asList("en", "es"));
        filter.setDateFromMs(System.currentTimeMillis() - 604800000L); // 1 week ago
        filter.setDateToMs(System.currentTimeMillis());
        filter.setAddressFilter("555");

        // Verify all settings
        assertEquals("Should have ALL_CONTENT scope", 
                     SearchFilter.SearchScope.ALL_CONTENT, filter.getSearchScope());
        assertEquals("Should have TRANSLATED_ONLY filter", 
                     SearchFilter.MessageTypeFilter.TRANSLATED_ONLY, filter.getMessageTypeFilter());
        assertTrue("Should have language filter", filter.hasLanguageFilter());
        assertTrue("Should have date filter", filter.hasDateFilter());
        assertTrue("Should have address filter", filter.hasAddressFilter());
        
        // Test filter behavior
        assertTrue("Should search both original and translation", 
                   filter.shouldSearchOriginal() && filter.shouldSearchTranslation());
        assertTrue("Should include translated messages", 
                   filter.shouldIncludeByTranslationStatus(true));
        assertFalse("Should not include untranslated messages", 
                    filter.shouldIncludeByTranslationStatus(false));
    }

    @Test
    public void testSearchFilter_toString() {
        SearchFilter filter = new SearchFilter();
        filter.setSearchScope(SearchFilter.SearchScope.TRANSLATION_ONLY);
        filter.setLanguageFilters("en");
        
        String filterString = filter.toString();
        assertNotNull("toString should not return null", filterString);
        assertTrue("toString should contain searchScope", filterString.contains("searchScope"));
        assertTrue("toString should contain TRANSLATION_ONLY", filterString.contains("TRANSLATION_ONLY"));
    }
}