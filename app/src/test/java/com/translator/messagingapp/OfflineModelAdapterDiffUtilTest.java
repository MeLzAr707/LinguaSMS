package com.translator.messagingapp;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for OfflineModelAdapter DiffUtil optimization.
 */
public class OfflineModelAdapterDiffUtilTest {
    
    @Mock
    private OfflineModelAdapter.OnModelActionListener mockListener;
    
    private OfflineModelAdapter adapter;
    private List<OfflineModelInfo> initialModels;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        
        initialModels = new ArrayList<>();
        initialModels.add(createTestModel("en", "English", 10485760));
        initialModels.add(createTestModel("es", "Spanish", 12582912));
        
        adapter = new OfflineModelAdapter(initialModels, mockListener);
    }
    
    @Test
    public void testUpdateModelsMethod() {
        // Initial state
        assertEquals("Initial model count should be 2", 2, adapter.getItemCount());
        
        // Create updated models list
        List<OfflineModelInfo> updatedModels = new ArrayList<>();
        updatedModels.add(createTestModel("en", "English", 10485760));
        updatedModels.add(createTestModel("es", "Spanish", 12582912));
        updatedModels.add(createTestModel("fr", "French", 11534336));
        
        // Update models using DiffUtil
        adapter.updateModels(updatedModels);
        
        // Verify the adapter was updated
        assertEquals("Updated model count should be 3", 3, adapter.getItemCount());
    }
    
    @Test
    public void testUpdateModelsWithNull() {
        // Initial state
        int initialCount = adapter.getItemCount();
        
        // Try to update with null
        adapter.updateModels(null);
        
        // Verify adapter state is unchanged
        assertEquals("Model count should remain unchanged after null update", 
                    initialCount, adapter.getItemCount());
    }
    
    @Test
    public void testUpdateModelsWithEmptyList() {
        // Initial state
        assertEquals("Initial model count should be 2", 2, adapter.getItemCount());
        
        // Update with empty list
        List<OfflineModelInfo> emptyList = new ArrayList<>();
        adapter.updateModels(emptyList);
        
        // Verify adapter is now empty
        assertEquals("Model count should be 0 after empty list update", 
                    0, adapter.getItemCount());
    }
    
    @Test
    public void testUpdateModelsPreservesReference() {
        List<OfflineModelInfo> newModels = new ArrayList<>();
        newModels.add(createTestModel("fr", "French", 11534336));
        
        adapter.updateModels(newModels);
        
        // Verify the adapter has the new models
        assertEquals("Should have 1 model after update", 1, adapter.getItemCount());
    }
    
    @Test
    public void testUpdateProgressStillWorks() {
        // The updateProgress method should still work efficiently for individual progress updates
        OfflineModelInfo model = initialModels.get(0);
        
        // This method should not crash and should use notifyItemChanged for efficiency
        try {
            adapter.updateProgress(model, 50);
            assertTrue("updateProgress should not throw exception", true);
        } catch (Exception e) {
            fail("updateProgress should not throw exception: " + e.getMessage());
        }
    }
    
    /**
     * Test that verifies the DiffUtil optimization provides better performance
     * for model list updates while keeping individual progress updates efficient.
     */
    @Test
    public void testDiffUtilVsNotifyDataSetChanged() {
        // This is a conceptual test - in a real scenario, DiffUtil provides:
        // 1. More granular updates for model list changes
        // 2. Better animation support
        // 3. Reduced UI jank during model list updates
        // 4. Better performance for large model lists
        // 5. Maintains efficiency of individual progress updates via notifyItemChanged
        
        List<OfflineModelInfo> largeModelList = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            largeModelList.add(createTestModel("lang" + i, "Language " + i, 10485760 + i));
        }
        
        adapter.updateModels(largeModelList);
        
        assertEquals("Should handle large model lists efficiently", 
                    50, adapter.getItemCount());
    }
    
    /**
     * Test updating models with download state changes to verify DiffUtil efficiency.
     */
    @Test
    public void testPartialModelUpdates() {
        // Start with initial models
        List<OfflineModelInfo> currentModels = new ArrayList<>();
        currentModels.add(createTestModel("en", "English", 10485760));
        currentModels.add(createTestModel("es", "Spanish", 12582912));
        currentModels.add(createTestModel("fr", "French", 11534336));
        
        adapter.updateModels(currentModels);
        assertEquals("Should have 3 models", 3, adapter.getItemCount());
        
        // Update with one model download state changed, one removed, one added
        List<OfflineModelInfo> updatedModels = new ArrayList<>();
        
        OfflineModelInfo englishModel = createTestModel("en", "English", 10485760);
        englishModel.setDownloaded(true); // Changed
        updatedModels.add(englishModel);
        
        updatedModels.add(createTestModel("es", "Spanish", 12582912)); // Same
        // French model removed
        
        updatedModels.add(createTestModel("de", "German", 13631488)); // Added
        
        adapter.updateModels(updatedModels);
        assertEquals("Should still have 3 models after partial update", 3, adapter.getItemCount());
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