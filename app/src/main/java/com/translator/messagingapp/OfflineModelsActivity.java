package com.translator.messagingapp;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for managing offline language models.
 */
public class OfflineModelsActivity extends BaseActivity {
    private static final String TAG = "OfflineModelsActivity";

    private RecyclerView recyclerView;
    private LanguageModelAdapter adapter;
    private OfflineTranslationService offlineTranslationService;
    private List<LanguageModel> languageModels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_offline_models);
            
            // Setup toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(R.string.manage_offline_models);
            }
            
            // Initialize offline translation service
            TranslatorApp app = (TranslatorApp) getApplication();
            offlineTranslationService = app.getTranslationManager().getOfflineTranslationService();
            
            // Setup RecyclerView
            recyclerView = findViewById(R.id.recycler_view_languages);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            
            // Initialize language models list
            initializeLanguageModels();
            
            // Setup adapter
            adapter = new LanguageModelAdapter(languageModels);
            recyclerView.setAdapter(adapter);
            
            Log.d(TAG, "OfflineModelsActivity created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error loading offline models", Toast.LENGTH_LONG).show();
            finish();
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
    
    /**
     * Initialize the list of language models.
     */
    private void initializeLanguageModels() {
        languageModels = new ArrayList<>();
        
        // Get supported languages from offline service
        String[] supportedLanguages = offlineTranslationService.getSupportedLanguages();
        String[] languageNames = getResources().getStringArray(R.array.language_names);
        String[] languageCodes = getResources().getStringArray(R.array.language_codes);
        
        // Create language models for supported languages
        for (String langCode : supportedLanguages) {
            String languageName = getLanguageName(langCode, languageCodes, languageNames);
            boolean isDownloaded = offlineTranslationService.isLanguageModelDownloaded(langCode);
            languageModels.add(new LanguageModel(langCode, languageName, isDownloaded));
        }
    }
    
    /**
     * Get language name from code.
     */
    private String getLanguageName(String code, String[] codes, String[] names) {
        for (int i = 0; i < codes.length && i < names.length; i++) {
            if (codes[i].equals(code)) {
                return names[i];
            }
        }
        return code; // Return code if name not found
    }
    
    /**
     * Language model data class.
     */
    private static class LanguageModel {
        final String code;
        final String name;
        boolean isDownloaded;
        boolean isDownloading;
        
        LanguageModel(String code, String name, boolean isDownloaded) {
            this.code = code;
            this.name = name;
            this.isDownloaded = isDownloaded;
            this.isDownloading = false;
        }
    }
    
    /**
     * RecyclerView adapter for language models.
     */
    private class LanguageModelAdapter extends RecyclerView.Adapter<LanguageModelAdapter.ViewHolder> {
        private final List<LanguageModel> models;
        
        LanguageModelAdapter(List<LanguageModel> models) {
            this.models = models;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_language_model, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LanguageModel model = models.get(position);
            holder.bind(model);
        }
        
        @Override
        public int getItemCount() {
            return models.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView textLanguageName;
            private final TextView textStatus;
            private final Button buttonAction;
            
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                textLanguageName = itemView.findViewById(R.id.text_language_name);
                textStatus = itemView.findViewById(R.id.text_status);
                buttonAction = itemView.findViewById(R.id.button_action);
            }
            
            void bind(LanguageModel model) {
                textLanguageName.setText(model.name);
                
                if (model.isDownloading) {
                    textStatus.setText(R.string.downloading_language_model);
                    buttonAction.setText(R.string.cancel);
                    buttonAction.setEnabled(false);
                } else if (model.isDownloaded) {
                    textStatus.setText(R.string.download_complete);
                    buttonAction.setText(R.string.delete);
                    buttonAction.setEnabled(true);
                } else {
                    textStatus.setText("Not downloaded");
                    buttonAction.setText(R.string.download_language_model);
                    buttonAction.setEnabled(true);
                }
                
                buttonAction.setOnClickListener(v -> {
                    if (model.isDownloaded && !model.isDownloading) {
                        // Delete model
                        deleteLanguageModel(model, getAdapterPosition());
                    } else if (!model.isDownloaded && !model.isDownloading) {
                        // Download model
                        downloadLanguageModel(model, getAdapterPosition());
                    }
                });
            }
        }
    }
    
    /**
     * Download a language model.
     */
    private void downloadLanguageModel(LanguageModel model, int position) {
        model.isDownloading = true;
        adapter.notifyItemChanged(position);
        
        offlineTranslationService.downloadLanguageModel(model.code, 
            new OfflineTranslationService.ModelDownloadCallback() {
                @Override
                public void onDownloadComplete(boolean success, String languageCode, String errorMessage) {
                    runOnUiThread(() -> {
                        model.isDownloading = false;
                        if (success) {
                            model.isDownloaded = true;
                            Toast.makeText(OfflineModelsActivity.this, 
                                getString(R.string.download_complete) + ": " + model.name, 
                                Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(OfflineModelsActivity.this, 
                                getString(R.string.download_failed) + ": " + errorMessage, 
                                Toast.LENGTH_LONG).show();
                        }
                        adapter.notifyItemChanged(position);
                    });
                }
                
                @Override
                public void onDownloadProgress(String languageCode, int progress) {
                    // Could update progress here if needed
                }
            });
    }
    
    /**
     * Delete a language model.
     */
    private void deleteLanguageModel(LanguageModel model, int position) {
        try {
            offlineTranslationService.deleteLanguageModel(model.code);
            model.isDownloaded = false;
            adapter.notifyItemChanged(position);
            Toast.makeText(this, 
                getString(R.string.language_model_deleted) + ": " + model.name, 
                Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting language model", e);
            Toast.makeText(this, "Error deleting model", Toast.LENGTH_SHORT).show();
        }
    }
}