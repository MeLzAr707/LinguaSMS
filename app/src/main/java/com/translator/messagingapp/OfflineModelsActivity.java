package com.translator.messagingapp;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for managing offline translation models.
 * 
 * @deprecated This activity has been replaced by GeminiNanoModelsActivity for better offline GenAI features.
 * Use GeminiNanoModelsActivity instead for new implementations.
 * This class is maintained for backward compatibility and existing tests only.
 */
@Deprecated
public class OfflineModelsActivity extends BaseActivity {
    private static final String TAG = "OfflineModelsActivity";

    private SwitchCompat switchOfflineTranslation;
    private RecyclerView recyclerOfflineModels;
    private TextView textNoModels;
    private OfflineModelAdapter modelAdapter;
    private OfflineModelManager modelManager;
    private UserPreferences userPreferences;
    private TranslationManager translationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_offline_models);
            
            // Initialize services
            TranslatorApp app = (TranslatorApp) getApplication();
            if (app != null) {
                userPreferences = app.getUserPreferences();
                modelManager = new OfflineModelManager(this);
                translationManager = app.getTranslationManager();
            }
            
            // Setup toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(R.string.offline_models_title);
            }
            
            // Initialize UI components
            initializeViews();
            setupRecyclerView();
            loadOfflineModels();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error loading offline models: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void initializeViews() {
        switchOfflineTranslation = findViewById(R.id.switch_offline_translation);
        recyclerOfflineModels = findViewById(R.id.recycler_offline_models);
        textNoModels = findViewById(R.id.text_no_models);
        
        // Setup offline translation switch
        if (userPreferences != null) {
            switchOfflineTranslation.setChecked(userPreferences.isOfflineTranslationEnabled());
            switchOfflineTranslation.setOnCheckedChangeListener((buttonView, isChecked) -> {
                userPreferences.setOfflineTranslationEnabled(isChecked);
                if (isChecked) {
                    Toast.makeText(this, "Offline translation enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Offline translation disabled", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    private void setupRecyclerView() {
        recyclerOfflineModels.setLayoutManager(new LinearLayoutManager(this));
        modelAdapter = new OfflineModelAdapter(new ArrayList<>(), new OfflineModelAdapter.OnModelActionListener() {
            @Override
            public void onDownloadModel(OfflineModelInfo model) {
                downloadModel(model);
            }
            
            @Override
            public void onDeleteModel(OfflineModelInfo model) {
                deleteModel(model);
            }
        });
        recyclerOfflineModels.setAdapter(modelAdapter);
    }
    
    private void loadOfflineModels() {
        if (modelManager == null) {
            Log.e(TAG, "ModelManager is null");
            showNoModelsMessage();
            return;
        }
        
        try {
            List<OfflineModelInfo> models = modelManager.getAvailableModels();
            
            // Synchronize model states with the real OfflineTranslationService
            synchronizeModelStates(models);
            
            if (models.isEmpty()) {
                showNoModelsMessage();
            } else {
                showModels(models);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading offline models", e);
            showNoModelsMessage();
        }
    }
    
    /**
     * Synchronizes model download states between OfflineModelManager and OfflineTranslationService.
     * This ensures the UI shows accurate download status based on real ML Kit model availability.
     */
    private void synchronizeModelStates(List<OfflineModelInfo> models) {
        OfflineTranslationService offlineService = getOfflineTranslationService();
        if (offlineService == null) {
            Log.w(TAG, "Cannot synchronize model states - OfflineTranslationService not available");
            return;
        }
        
        Log.d(TAG, "Synchronizing model states with OfflineTranslationService");
        
        for (OfflineModelInfo model : models) {
            String languageCode = model.getLanguageCode();
            
            // Check if model is actually available in OfflineTranslationService
            boolean actuallyDownloaded = offlineService.isLanguageModelDownloaded(languageCode);
            boolean managerThinks = modelManager.isModelDownloaded(languageCode);
            
            if (managerThinks != actuallyDownloaded) {
                Log.d(TAG, "Model state mismatch for " + languageCode + 
                     " - Manager: " + managerThinks + ", Service: " + actuallyDownloaded);
                
                // Update the model state to match reality
                model.setDownloaded(actuallyDownloaded);
                
                // Update the OfflineModelManager's tracking to match
                if (actuallyDownloaded && !managerThinks) {
                    // Service has model but manager doesn't know - update manager
                    modelManager.saveDownloadedModel(languageCode);
                    Log.d(TAG, "Updated OfflineModelManager to show " + languageCode + " as downloaded");
                } else if (!actuallyDownloaded && managerThinks) {
                    // Manager thinks model is downloaded but service doesn't have it - clean up manager
                    // Note: we don't automatically delete from manager as it might have been a temporary service issue
                    Log.w(TAG, "OfflineModelManager shows " + languageCode + " as downloaded but service doesn't have it");
                }
            }
        }
        
        // Refresh the translation service to ensure it's synchronized
        if (translationManager != null) {
            translationManager.refreshOfflineModels();
        }
    }
    
    private void showModels(List<OfflineModelInfo> models) {
        textNoModels.setVisibility(android.view.View.GONE);
        recyclerOfflineModels.setVisibility(android.view.View.VISIBLE);
        modelAdapter.updateModels(models);
    }
    
    private void showNoModelsMessage() {
        textNoModels.setVisibility(android.view.View.VISIBLE);
        recyclerOfflineModels.setVisibility(android.view.View.GONE);
    }
    
    private void downloadModel(OfflineModelInfo model) {
        if (modelManager == null) {
            Toast.makeText(this, "Model manager not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            Toast.makeText(this, "Starting download for " + model.getLanguageName(), Toast.LENGTH_SHORT).show();
            
            // Ensure we have a valid OfflineTranslationService for real ML Kit downloads
            OfflineTranslationService offlineService = getOfflineTranslationService();
            
            if (offlineService != null) {
                // Use real ML Kit download - this is the correct path
                Log.d(TAG, "Using real ML Kit download for: " + model.getLanguageCode());
                offlineService.downloadLanguageModel(
                    model.getLanguageCode(), 
                    new OfflineTranslationService.ModelDownloadCallback() {
                        @Override
                        public void onDownloadComplete(boolean success, String languageCode, String errorMessage) {
                            runOnUiThread(() -> {
                                if (success) {
                                    Toast.makeText(OfflineModelsActivity.this, 
                                        getString(R.string.model_download_success), Toast.LENGTH_SHORT).show();
                                    model.setDownloaded(true);
                                    
                                    // Also update the OfflineModelManager's tracking
                                    modelManager.saveDownloadedModel(model.getLanguageCode());
                                    
                                    modelAdapter.notifyDataSetChanged();
                                    
                                    // Refresh the translation service to pick up the new model
                                    if (translationManager != null) {
                                        translationManager.refreshOfflineModels();
                                    }
                                } else {
                                    Toast.makeText(OfflineModelsActivity.this, 
                                        getString(R.string.model_download_error, errorMessage), Toast.LENGTH_LONG).show();
                                }
                                modelAdapter.updateProgress(model, -1); // Hide progress
                            });
                        }

                        @Override
                        public void onDownloadProgress(String languageCode, int progress) {
                            runOnUiThread(() -> {
                                modelAdapter.updateProgress(model, progress);
                            });
                        }
                    });
            } else {
                // No valid OfflineTranslationService available - show error instead of falling back to simulation
                Log.e(TAG, "OfflineTranslationService not available - cannot download real models");
                Toast.makeText(this, "Real model download service not available. Please restart the app and try again.", 
                              Toast.LENGTH_LONG).show();
                
                // Don't fall back to simulation as it causes the "instant download" issue
                // Instead, guide user to restart the app to properly initialize services
            }
        } catch (Exception e) {
            Log.e(TAG, "Error downloading model", e);
            Toast.makeText(this, "Error downloading model: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Gets a valid OfflineTranslationService instance, ensuring robust download functionality.
     * This method tries multiple approaches to get a working service.
     */
    private OfflineTranslationService getOfflineTranslationService() {
        // First try: get from TranslationManager if available
        if (translationManager != null && translationManager.getOfflineTranslationService() != null) {
            Log.d(TAG, "Using OfflineTranslationService from TranslationManager");
            return translationManager.getOfflineTranslationService();
        }
        
        // Second try: create a new instance if we have userPreferences
        if (userPreferences != null) {
            Log.d(TAG, "Creating new OfflineTranslationService instance");
            return new OfflineTranslationService(this, userPreferences);
        }
        
        // Third try: create with minimal setup
        try {
            Log.d(TAG, "Creating OfflineTranslationService with minimal setup");
            UserPreferences fallbackPrefs = new UserPreferences(this);
            return new OfflineTranslationService(this, fallbackPrefs);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create OfflineTranslationService", e);
            return null;
        }
    }
    
    private void deleteModel(OfflineModelInfo model) {
        if (modelManager == null) {
            Toast.makeText(this, "Model manager not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Delete from both OfflineModelManager and OfflineTranslationService for consistency
            boolean success = modelManager.deleteModel(model);
            
            // Also delete from the real OfflineTranslationService
            OfflineTranslationService offlineService = getOfflineTranslationService();
            if (offlineService != null) {
                offlineService.deleteLanguageModel(model.getLanguageCode());
                Log.d(TAG, "Deleted model from OfflineTranslationService: " + model.getLanguageCode());
            } else {
                Log.w(TAG, "Could not delete from OfflineTranslationService - service not available");
            }
            
            if (success) {
                model.setDownloaded(false);
                modelAdapter.notifyDataSetChanged();
                Toast.makeText(this, getString(R.string.model_delete_success), Toast.LENGTH_SHORT).show();
                
                // Refresh the translation service to remove the deleted model
                if (translationManager != null) {
                    translationManager.refreshOfflineModels();
                }
            } else {
                Toast.makeText(this, getString(R.string.model_delete_error, "Unknown error"), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting model", e);
            Toast.makeText(this, getString(R.string.model_delete_error, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}