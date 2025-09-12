package com.translator.messagingapp.translation;

import com.translator.messagingapp.R;
import com.translator.messagingapp.translation.*;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying offline translation models in a RecyclerView.
 */
public class OfflineModelsAdapter extends RecyclerView.Adapter<OfflineModelsAdapter.ModelViewHolder> {
    
    private List<OfflineModelManager.OfflineLanguageModel> models;
    private OfflineModelsActivity activity;
    
    public OfflineModelsAdapter(OfflineModelsActivity activity) {
        this.activity = activity;
        this.models = new ArrayList<>();
    }
    
    public void updateModels(List<OfflineModelManager.OfflineLanguageModel> newModels) {
        this.models.clear();
        this.models.addAll(newModels);
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_offline_model, parent, false);
        return new ModelViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ModelViewHolder holder, int position) {
        OfflineModelManager.OfflineLanguageModel model = models.get(position);
        holder.bind(model, activity);
    }
    
    @Override
    public int getItemCount() {
        return models.size();
    }
    
    public static class ModelViewHolder extends RecyclerView.ViewHolder {
        private TextView languageNameText;
        private TextView statusText;
        private Button actionButton;
        private ProgressBar progressBar;
        private OfflineModelManager.OfflineLanguageModel currentModel;
        
        public ModelViewHolder(@NonNull View itemView) {
            super(itemView);
            languageNameText = itemView.findViewById(R.id.language_name);
            statusText = itemView.findViewById(R.id.status_text);
            actionButton = itemView.findViewById(R.id.action_button);
            progressBar = itemView.findViewById(R.id.progress_bar);
        }
        
        public void bind(OfflineModelManager.OfflineLanguageModel model, OfflineModelsActivity activity) {
            this.currentModel = model;
            
            languageNameText.setText(model.getDisplayName());
            updateUI();
            
            actionButton.setOnClickListener(v -> {
                if (model.isDownloaded()) {
                    // Delete model
                    activity.deleteModel(model.getLanguageCode(), this);
                } else if (!model.isDownloading()) {
                    // Download model
                    activity.downloadModel(model.getLanguageCode(), this);
                }
            });
        }
        
        private void updateUI() {
            if (currentModel.isDownloading()) {
                statusText.setText("Downloading...");
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(currentModel.getDownloadProgress());
                actionButton.setText("Cancel");
                actionButton.setEnabled(false); // Disable cancel for now
            } else if (currentModel.isDownloaded()) {
                statusText.setText("Downloaded");
                progressBar.setVisibility(View.GONE);
                actionButton.setText("Delete");
                actionButton.setEnabled(true);
            } else {
                statusText.setText("Not downloaded");
                progressBar.setVisibility(View.GONE);
                actionButton.setText("Download");
                actionButton.setEnabled(true);
            }
        }
        
        public void updateProgress(int progress) {
            if (currentModel != null) {
                currentModel.setDownloadProgress(progress);
                progressBar.setProgress(progress);
                
                if (progress >= 100) {
                    currentModel.setDownloading(false);
                    currentModel.setDownloaded(true);
                    updateUI();
                }
            }
        }
        
        public void setDownloadComplete() {
            if (currentModel != null) {
                currentModel.setDownloading(false);
                currentModel.setDownloaded(true);
                currentModel.setDownloadProgress(100);
                updateUI();
            }
        }
        
        public void setDownloadError() {
            if (currentModel != null) {
                currentModel.setDownloading(false);
                currentModel.setDownloadProgress(0);
                updateUI();
            }
        }
        
        public void setModelDeleted() {
            if (currentModel != null) {
                currentModel.setDownloaded(false);
                currentModel.setDownloadProgress(0);
                updateUI();
            }
        }
    }
}