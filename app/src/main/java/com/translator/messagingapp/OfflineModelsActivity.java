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
 */
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
            // Sync with MLKit before loading models to ensure accuracy
            modelManager.syncWithMLKit();
            List<OfflineModelInfo> models = modelManager.getAvailableModels();
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
            
            modelManager.downloadModel(model, new OfflineModelManager.DownloadListener() {
                @Override
                public void onProgress(int progress) {
                    runOnUiThread(() -> {
                        modelAdapter.updateProgress(model, progress);
                    });
                }
                
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        Toast.makeText(OfflineModelsActivity.this, 
                            getString(R.string.model_download_success), Toast.LENGTH_SHORT).show();
                        model.setDownloaded(true);
                        modelAdapter.notifyDataSetChanged();
                        
                        // Refresh the translation service to pick up the new model
                        if (translationManager != null) {
                            translationManager.refreshOfflineModels();
                        }
                        
                        // Sync with MLKit after successful download
                        new Thread(() -> modelManager.syncWithMLKit()).start();
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(OfflineModelsActivity.this, 
                            getString(R.string.model_download_error, error), Toast.LENGTH_LONG).show();
                        modelAdapter.updateProgress(model, -1); // Hide progress
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error downloading model", e);
            Toast.makeText(this, "Error downloading model: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void deleteModel(OfflineModelInfo model) {
        if (modelManager == null) {
            Toast.makeText(this, "Model manager not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            boolean success = modelManager.deleteModel(model);
            if (success) {
                model.setDownloaded(false);
                modelAdapter.notifyDataSetChanged();
                Toast.makeText(this, getString(R.string.model_delete_success), Toast.LENGTH_SHORT).show();
                
                // Refresh the translation service to remove the deleted model
                if (translationManager != null) {
                    translationManager.refreshOfflineModels();
                }
                
                // Sync with MLKit after successful deletion
                new Thread(() -> modelManager.syncWithMLKit()).start();
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