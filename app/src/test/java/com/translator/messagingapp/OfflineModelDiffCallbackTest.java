package com.translator.messagingapp;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for OfflineModelDiffCallback to verify DiffUtil optimization works correctly.
 */
public class OfflineModelDiffCallbackTest {
    
    private List<OfflineModelInfo> oldModels;
    private List<OfflineModelInfo> newModels;
    
    @Before
    public void setUp() {
        oldModels = new ArrayList<>();
        newModels = new ArrayList<>();
    }
    
    @Test
    public void testListSizes() {
        // Create test models
        oldModels.add(createTestModel("en", "English", 10485760)); // 10MB
        oldModels.add(createTestModel("es", "Spanish", 12582912)); // 12MB
        
        newModels.add(createTestModel("en", "English", 10485760));
        newModels.add(createTestModel("es", "Spanish", 12582912));
        newModels.add(createTestModel("fr", "French", 11534336)); // 11MB
        
        OfflineModelDiffCallback callback = new OfflineModelDiffCallback(oldModels, newModels);
        
        assertEquals("Old list size should be 2", 2, callback.getOldListSize());
        assertEquals("New list size should be 3", 3, callback.getNewListSize());
    }
    
    @Test
    public void testItemsTheSame() {
        OfflineModelInfo model1Old = createTestModel("en", "English", 10485760);
        OfflineModelInfo model1New = createTestModel("en", "English Updated", 10485760);
        
        oldModels.add(model1Old);
        newModels.add(model1New);
        
        OfflineModelDiffCallback callback = new OfflineModelDiffCallback(oldModels, newModels);
        
        assertTrue("Models with same language code should be considered the same item", 
                  callback.areItemsTheSame(0, 0));
    }
    
    @Test
    public void testItemsNotTheSame() {
        OfflineModelInfo model1 = createTestModel("en", "English", 10485760);
        OfflineModelInfo model2 = createTestModel("es", "Spanish", 12582912);
        
        oldModels.add(model1);
        newModels.add(model2);
        
        OfflineModelDiffCallback callback = new OfflineModelDiffCallback(oldModels, newModels);
        
        assertFalse("Models with different language codes should not be considered the same item", 
                   callback.areItemsTheSame(0, 0));
    }
    
    @Test
    public void testContentsTheSame() {
        OfflineModelInfo model1 = createTestModel("en", "English", 10485760);
        OfflineModelInfo model2 = createTestModel("en", "English", 10485760);
        
        oldModels.add(model1);
        newModels.add(model2);
        
        OfflineModelDiffCallback callback = new OfflineModelDiffCallback(oldModels, newModels);
        
        assertTrue("Models with identical content should be considered the same", 
                  callback.areContentsTheSame(0, 0));
    }
    
    @Test
    public void testContentsNotTheSame() {
        OfflineModelInfo model1 = createTestModel("en", "English", 10485760);
        OfflineModelInfo model2 = createTestModel("en", "English Updated", 10485760);
        
        oldModels.add(model1);
        newModels.add(model2);
        
        OfflineModelDiffCallback callback = new OfflineModelDiffCallback(oldModels, newModels);
        
        assertFalse("Models with different content should not be considered the same", 
                   callback.areContentsTheSame(0, 0));
    }
    
    @Test
    public void testDownloadStateChange() {
        OfflineModelInfo model1 = createTestModel("en", "English", 10485760);
        model1.setDownloaded(false);
        model1.setDownloading(false);
        
        OfflineModelInfo model2 = createTestModel("en", "English", 10485760);
        model2.setDownloaded(false);
        model2.setDownloading(true);
        model2.setDownloadProgress(50);
        
        oldModels.add(model1);
        newModels.add(model2);
        
        OfflineModelDiffCallback callback = new OfflineModelDiffCallback(oldModels, newModels);
        
        assertTrue("Models should be the same item (same language code)", 
                  callback.areItemsTheSame(0, 0));
        assertFalse("Models with different download state should have different content", 
                   callback.areContentsTheSame(0, 0));
    }
    
    @Test
    public void testDownloadProgressChange() {
        OfflineModelInfo model1 = createTestModel("en", "English", 10485760);
        model1.setDownloading(true);
        model1.setDownloadProgress(25);
        
        OfflineModelInfo model2 = createTestModel("en", "English", 10485760);
        model2.setDownloading(true);
        model2.setDownloadProgress(75);
        
        oldModels.add(model1);
        newModels.add(model2);
        
        OfflineModelDiffCallback callback = new OfflineModelDiffCallback(oldModels, newModels);
        
        assertTrue("Models should be the same item (same language code)", 
                  callback.areItemsTheSame(0, 0));
        assertFalse("Models with different download progress should have different content", 
                   callback.areContentsTheSame(0, 0));
    }
    
    @Test
    public void testDownloadCompletionChange() {
        OfflineModelInfo model1 = createTestModel("en", "English", 10485760);
        model1.setDownloaded(false);
        model1.setDownloading(true);
        model1.setDownloadProgress(99);
        
        OfflineModelInfo model2 = createTestModel("en", "English", 10485760);
        model2.setDownloaded(true);
        model2.setDownloading(false);
        model2.setDownloadProgress(100);
        
        oldModels.add(model1);
        newModels.add(model2);
        
        OfflineModelDiffCallback callback = new OfflineModelDiffCallback(oldModels, newModels);
        
        assertTrue("Models should be the same item (same language code)", 
                  callback.areItemsTheSame(0, 0));
        assertFalse("Models with different download completion state should have different content", 
                   callback.areContentsTheSame(0, 0));
    }
    
    @Test
    public void testEmptyLists() {
        OfflineModelDiffCallback callback = new OfflineModelDiffCallback(oldModels, newModels);
        
        assertEquals("Empty old list should have size 0", 0, callback.getOldListSize());
        assertEquals("Empty new list should have size 0", 0, callback.getNewListSize());
    }
    
    /**
     * Helper method to create a test offline model with basic properties.
     */
    private OfflineModelInfo createTestModel(String languageCode, String languageName, long sizeBytes) {
        OfflineModelInfo model = new OfflineModelInfo(languageCode, languageName, sizeBytes);
        model.setDownloaded(false);
        model.setDownloading(false);
        model.setDownloadProgress(0);
        return model;
    }
}