package com.translator.messagingapp;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Activity for managing offline translation models.
 * Allows users to download, delete, and view status of language models.
 */
public class OfflineModelsActivity extends BaseActivity {
    private static final String TAG = "OfflineModelsActivity";
    
    private RecyclerView modelsRecyclerView;
    private OfflineModelsAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private OfflineModelManager modelManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_models);
        
        initializeViews();
        setupToolbar();
        initializeModelManager();
        loadModels();
    }
    
    private void initializeViews() {
        modelsRecyclerView = findViewById(R.id.models_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        emptyStateText = findViewById(R.id.empty_state_text);
        
        // Setup RecyclerView
        modelsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OfflineModelsAdapter(this);
        modelsRecyclerView.setAdapter(adapter);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Offline Models");
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void initializeModelManager() {
        modelManager = new OfflineModelManager(this);
    }
    
    private void loadModels() {
        showLoading(true);
        
        modelManager.getAvailableModels(new OfflineModelManager.ModelStatusListener() {
            @Override
            public void onStatusUpdated(List<OfflineModelManager.OfflineLanguageModel> models) {
                runOnUiThread(() -> {
                    showLoading(false);
                    adapter.updateModels(models);
                    
                    if (models.isEmpty()) {
                        showEmptyState(true);
                    } else {
                        showEmptyState(false);
                    }
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showEmptyState(true);
                    Toast.makeText(OfflineModelsActivity.this, 
                        "Error loading models: " + errorMessage, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error loading models: " + errorMessage);
                });
            }
        });
    }
    
    public void downloadModel(String languageCode, OfflineModelsAdapter.ModelViewHolder viewHolder) {
        Log.d(TAG, "Starting download for language: " + languageCode);
        
        modelManager.downloadModel(languageCode, new OfflineModelManager.DownloadListener() {
            @Override
            public void onProgress(int progress) {
                runOnUiThread(() -> {
                    viewHolder.updateProgress(progress);
                });
            }
            
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    viewHolder.setDownloadComplete();
                    Toast.makeText(OfflineModelsActivity.this, 
                        getString(R.string.model_download_success), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Model download completed: " + languageCode);
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    viewHolder.setDownloadError();
                    String message = getString(R.string.model_download_error, errorMessage);
                    Toast.makeText(OfflineModelsActivity.this, message, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Model download failed for " + languageCode + ": " + errorMessage);
                });
            }
        });
    }
    
    public void deleteModel(String languageCode, OfflineModelsAdapter.ModelViewHolder viewHolder) {
        Log.d(TAG, "Starting deletion for language: " + languageCode);
        
        modelManager.deleteModel(languageCode, 
            () -> {
                // Success
                runOnUiThread(() -> {
                    viewHolder.setModelDeleted();
                    Toast.makeText(OfflineModelsActivity.this, 
                        getString(R.string.model_delete_success), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Model deleted: " + languageCode);
                });
            },
            (errorMessage) -> {
                // Error
                runOnUiThread(() -> {
                    Toast.makeText(OfflineModelsActivity.this, 
                        "Delete failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Model deletion failed for " + languageCode + ": " + errorMessage);
                });
            }
        );
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        modelsRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    
    private void showEmptyState(boolean show) {
        emptyStateText.setVisibility(show ? View.VISIBLE : View.GONE);
        modelsRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (modelManager != null) {
            modelManager.cleanup();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh model status when returning to activity
        loadModels();
    }
}