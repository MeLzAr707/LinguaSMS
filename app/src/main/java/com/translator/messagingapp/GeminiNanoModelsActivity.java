package com.translator.messagingapp;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

/**
 * Activity for managing Gemini Nano model for offline GenAI features.
 * Replaces OfflineModelsActivity for Gemini Nano model management.
 */
public class GeminiNanoModelsActivity extends BaseActivity {
    private static final String TAG = "GeminiNanoModelsActivity";

    private SwitchCompat switchOfflineGenAI;
    private TextView textModelStatus;
    private TextView textModelSize;
    private Button buttonDownloadModel;
    private Button buttonDeleteModel;
    private ProgressBar progressDownload;
    private TextView textDownloadProgress;
    
    private GeminiNanoModelManager modelManager;
    private UserPreferences userPreferences;
    private TranslationManager translationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_gemini_nano_models);
            
            // Initialize services
            TranslatorApp app = (TranslatorApp) getApplication();
            if (app != null) {
                userPreferences = app.getUserPreferences();
                modelManager = new GeminiNanoModelManager(this);
                translationManager = app.getTranslationManager();
            }
            
            // Setup toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Gemini Nano Models");
            }
            
            // Initialize views
            initializeViews();
            
            // Setup listeners
            setupListeners();
            
            // Update UI with current status
            updateUI();
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating GeminiNanoModelsActivity", e);
            Toast.makeText(this, "Error initializing Gemini Nano models", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        switchOfflineGenAI = findViewById(R.id.switch_offline_genai);
        textModelStatus = findViewById(R.id.text_model_status);
        textModelSize = findViewById(R.id.text_model_size);
        buttonDownloadModel = findViewById(R.id.button_download_model);
        buttonDeleteModel = findViewById(R.id.button_delete_model);
        progressDownload = findViewById(R.id.progress_download);
        textDownloadProgress = findViewById(R.id.text_download_progress);
        
        // Initially hide progress elements
        progressDownload.setVisibility(View.GONE);
        textDownloadProgress.setVisibility(View.GONE);
    }

    private void setupListeners() {
        // Offline GenAI toggle
        switchOfflineGenAI.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (userPreferences != null) {
                userPreferences.setOfflineTranslationEnabled(isChecked);
                updateUI();
            }
        });

        // Download button
        buttonDownloadModel.setOnClickListener(v -> downloadGeminiNanoModel());

        // Delete button
        buttonDeleteModel.setOnClickListener(v -> deleteGeminiNanoModel());
    }

    private void updateUI() {
        try {
            // Update switch state
            if (userPreferences != null) {
                switchOfflineGenAI.setChecked(userPreferences.isOfflineTranslationEnabled());
            }

            // Update model status
            if (modelManager != null) {
                String status = modelManager.getModelStatus();
                textModelStatus.setText("Status: " + status);

                long sizeInBytes = modelManager.getModelSize();
                String sizeText = formatSize(sizeInBytes);
                textModelSize.setText("Size: " + sizeText);

                // Update button states
                boolean isModelAvailable = modelManager.isGeminiNanoModelAvailable();
                buttonDownloadModel.setEnabled(!isModelAvailable);
                buttonDeleteModel.setEnabled(isModelAvailable);

                if (isModelAvailable) {
                    buttonDownloadModel.setText("Model Downloaded");
                    buttonDeleteModel.setText("Delete Model");
                } else {
                    buttonDownloadModel.setText("Download Gemini Nano Model");
                    buttonDeleteModel.setText("No Model to Delete");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI", e);
        }
    }

    private void downloadGeminiNanoModel() {
        if (modelManager == null) {
            Toast.makeText(this, "Model manager not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        progressDownload.setVisibility(View.VISIBLE);
        textDownloadProgress.setVisibility(View.VISIBLE);
        textDownloadProgress.setText("Downloading Gemini Nano model...");
        
        // Disable buttons during download
        buttonDownloadModel.setEnabled(false);
        buttonDeleteModel.setEnabled(false);

        // Start download
        modelManager.downloadGeminiNanoModel(new GeminiNanoModelManager.DownloadListener() {
            @Override
            public void onProgress(int progress) {
                runOnUiThread(() -> {
                    progressDownload.setProgress(progress);
                    textDownloadProgress.setText("Downloading... " + progress + "%");
                });
            }

            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    progressDownload.setVisibility(View.GONE);
                    textDownloadProgress.setVisibility(View.GONE);
                    
                    Toast.makeText(GeminiNanoModelsActivity.this, 
                        "Gemini Nano model downloaded successfully", Toast.LENGTH_SHORT).show();
                    
                    updateUI();
                    
                    // Refresh translation manager
                    if (translationManager != null) {
                        translationManager.refreshOfflineModels();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressDownload.setVisibility(View.GONE);
                    textDownloadProgress.setVisibility(View.GONE);
                    
                    Toast.makeText(GeminiNanoModelsActivity.this, 
                        "Download failed: " + error, Toast.LENGTH_LONG).show();
                    
                    updateUI();
                });
            }
        });
    }

    private void deleteGeminiNanoModel() {
        if (modelManager == null) {
            Toast.makeText(this, "Model manager not available", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            boolean success = modelManager.deleteModel();
            if (success) {
                Toast.makeText(this, "Gemini Nano model deleted successfully", Toast.LENGTH_SHORT).show();
                updateUI();
                
                // Refresh translation manager
                if (translationManager != null) {
                    translationManager.refreshOfflineModels();
                }
            } else {
                Toast.makeText(this, "Failed to delete Gemini Nano model", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting model", e);
            Toast.makeText(this, "Error deleting model: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        try {
            if (modelManager != null) {
                modelManager.cleanup();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }
}