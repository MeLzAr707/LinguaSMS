package com.translator.messagingapp;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

/**
 * DiffUtil callback for efficient RecyclerView updates in OfflineModelAdapter.
 * Calculates the difference between old and new offline model lists to provide
 * minimal UI updates instead of full dataset changes.
 */
public class OfflineModelDiffCallback extends DiffUtil.Callback {
    private final List<OfflineModelInfo> oldModels;
    private final List<OfflineModelInfo> newModels;
    
    public OfflineModelDiffCallback(List<OfflineModelInfo> oldModels, List<OfflineModelInfo> newModels) {
        this.oldModels = oldModels;
        this.newModels = newModels;
    }
    
    @Override
    public int getOldListSize() {
        return oldModels.size();
    }
    
    @Override
    public int getNewListSize() {
        return newModels.size();
    }
    
    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        OfflineModelInfo oldModel = oldModels.get(oldItemPosition);
        OfflineModelInfo newModel = newModels.get(newItemPosition);
        // Use language code as the unique identifier for models
        return isStringEqual(oldModel.getLanguageCode(), newModel.getLanguageCode());
    }
    
    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        OfflineModelInfo oldModel = oldModels.get(oldItemPosition);
        OfflineModelInfo newModel = newModels.get(newItemPosition);
        
        // Compare all relevant fields for content equality
        return areModelsContentEqual(oldModel, newModel);
    }
    
    /**
     * Helper method to compare offline model content for equality.
     * This includes all fields that could affect the UI display.
     */
    private boolean areModelsContentEqual(OfflineModelInfo oldModel, OfflineModelInfo newModel) {
        // Basic model fields
        boolean basicFieldsEqual = isStringEqual(oldModel.getLanguageCode(), newModel.getLanguageCode()) &&
                isStringEqual(oldModel.getLanguageName(), newModel.getLanguageName()) &&
                oldModel.getSizeBytes() == newModel.getSizeBytes();
        
        // Download state fields
        boolean downloadStateEqual = oldModel.isDownloaded() == newModel.isDownloaded() &&
                oldModel.isDownloading() == newModel.isDownloading() &&
                oldModel.getDownloadProgress() == newModel.getDownloadProgress();
        
        return basicFieldsEqual && downloadStateEqual;
    }
    
    /**
     * Helper method to safely compare strings, handling null values.
     */
    private boolean isStringEqual(String str1, String str2) {
        if (str1 == null && str2 == null) {
            return true;
        }
        if (str1 == null || str2 == null) {
            return false;
        }
        return str1.equals(str2);
    }
}