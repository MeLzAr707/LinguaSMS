package com.translator.messagingapp;

import java.util.Arrays;
import java.util.List;

/**
 * Represents search filter criteria for enhanced message search functionality.
 * Supports filtering by date range, language, message type, and attachment type.
 */
public class SearchFilter {
    
    // Search scope options
    public enum SearchScope {
        ALL_CONTENT,           // Search both original and translated text
        ORIGINAL_ONLY,         // Search only original text
        TRANSLATION_ONLY       // Search only translated text
    }
    
    // Message type filter options
    public enum MessageTypeFilter {
        ALL,                   // All message types
        SMS_ONLY,             // SMS messages only
        MMS_ONLY,             // MMS messages only
        TRANSLATED_ONLY,      // Only messages with translations
        UNTRANSLATED_ONLY     // Only messages without translations
    }
    
    // Attachment type filter options
    public enum AttachmentTypeFilter {
        ALL,                  // All messages (with or without attachments)
        WITH_ATTACHMENTS,     // Only messages with attachments
        WITHOUT_ATTACHMENTS,  // Only messages without attachments
        IMAGES_ONLY,          // Only messages with image attachments
        VIDEOS_ONLY,          // Only messages with video attachments
        AUDIO_ONLY            // Only messages with audio attachments
    }
    
    // Filter criteria
    private SearchScope searchScope;
    private MessageTypeFilter messageTypeFilter;
    private AttachmentTypeFilter attachmentTypeFilter;
    private Long dateFromMs;
    private Long dateToMs;
    private List<String> languageFilters; // Language codes to filter by
    private String addressFilter;         // Phone number/contact filter
    
    /**
     * Creates a default SearchFilter with no restrictions (search all content).
     */
    public SearchFilter() {
        this.searchScope = SearchScope.ALL_CONTENT;
        this.messageTypeFilter = MessageTypeFilter.ALL;
        this.attachmentTypeFilter = AttachmentTypeFilter.ALL;
    }
    
    /**
     * Creates a SearchFilter with specified scope.
     * 
     * @param searchScope The scope of the search (original, translation, or both)
     */
    public SearchFilter(SearchScope searchScope) {
        this();
        this.searchScope = searchScope;
    }
    
    // Getters and setters
    
    public SearchScope getSearchScope() {
        return searchScope;
    }
    
    public void setSearchScope(SearchScope searchScope) {
        this.searchScope = searchScope;
    }
    
    public MessageTypeFilter getMessageTypeFilter() {
        return messageTypeFilter;
    }
    
    public void setMessageTypeFilter(MessageTypeFilter messageTypeFilter) {
        this.messageTypeFilter = messageTypeFilter;
    }
    
    public AttachmentTypeFilter getAttachmentTypeFilter() {
        return attachmentTypeFilter;
    }
    
    public void setAttachmentTypeFilter(AttachmentTypeFilter attachmentTypeFilter) {
        this.attachmentTypeFilter = attachmentTypeFilter;
    }
    
    public Long getDateFromMs() {
        return dateFromMs;
    }
    
    public void setDateFromMs(Long dateFromMs) {
        this.dateFromMs = dateFromMs;
    }
    
    public Long getDateToMs() {
        return dateToMs;
    }
    
    public void setDateToMs(Long dateToMs) {
        this.dateToMs = dateToMs;
    }
    
    public List<String> getLanguageFilters() {
        return languageFilters;
    }
    
    public void setLanguageFilters(List<String> languageFilters) {
        this.languageFilters = languageFilters;
    }
    
    public void setLanguageFilters(String... languageCodes) {
        this.languageFilters = Arrays.asList(languageCodes);
    }
    
    public String getAddressFilter() {
        return addressFilter;
    }
    
    public void setAddressFilter(String addressFilter) {
        this.addressFilter = addressFilter;
    }
    
    /**
     * Checks if this filter has any date restrictions.
     * 
     * @return true if date filtering is applied, false otherwise
     */
    public boolean hasDateFilter() {
        return dateFromMs != null || dateToMs != null;
    }
    
    /**
     * Checks if this filter has any language restrictions.
     * 
     * @return true if language filtering is applied, false otherwise
     */
    public boolean hasLanguageFilter() {
        return languageFilters != null && !languageFilters.isEmpty();
    }
    
    /**
     * Checks if this filter has any address restrictions.
     * 
     * @return true if address filtering is applied, false otherwise
     */
    public boolean hasAddressFilter() {
        return addressFilter != null && !addressFilter.trim().isEmpty();
    }
    
    /**
     * Checks if this filter should search in original text.
     * 
     * @return true if original text should be searched, false otherwise
     */
    public boolean shouldSearchOriginal() {
        return searchScope == SearchScope.ALL_CONTENT || searchScope == SearchScope.ORIGINAL_ONLY;
    }
    
    /**
     * Checks if this filter should search in translated text.
     * 
     * @return true if translated text should be searched, false otherwise
     */
    public boolean shouldSearchTranslation() {
        return searchScope == SearchScope.ALL_CONTENT || searchScope == SearchScope.TRANSLATION_ONLY;
    }
    
    /**
     * Checks if a message should be included based on its translation status.
     * 
     * @param hasTranslation true if the message has a translation, false otherwise
     * @return true if the message should be included, false otherwise
     */
    public boolean shouldIncludeByTranslationStatus(boolean hasTranslation) {
        switch (messageTypeFilter) {
            case TRANSLATED_ONLY:
                return hasTranslation;
            case UNTRANSLATED_ONLY:
                return !hasTranslation;
            default:
                return true;
        }
    }
    
    /**
     * Checks if a message should be included based on its type.
     * 
     * @param messageType The message type (Message.MESSAGE_TYPE_SMS or Message.MESSAGE_TYPE_MMS)
     * @return true if the message should be included, false otherwise
     */
    public boolean shouldIncludeByMessageType(int messageType) {
        switch (messageTypeFilter) {
            case SMS_ONLY:
                return messageType == Message.MESSAGE_TYPE_SMS;
            case MMS_ONLY:
                return messageType == Message.MESSAGE_TYPE_MMS;
            default:
                return true;
        }
    }
    
    @Override
    public String toString() {
        return "SearchFilter{" +
                "searchScope=" + searchScope +
                ", messageTypeFilter=" + messageTypeFilter +
                ", attachmentTypeFilter=" + attachmentTypeFilter +
                ", dateFromMs=" + dateFromMs +
                ", dateToMs=" + dateToMs +
                ", languageFilters=" + languageFilters +
                ", addressFilter='" + addressFilter + '\'' +
                '}';
    }
}