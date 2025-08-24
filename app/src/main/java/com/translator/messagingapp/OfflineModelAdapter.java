package com.translator.messagingapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for displaying offline translation models in a RecyclerView.
 */
public class OfflineModelAdapter extends RecyclerView.Adapter<OfflineModelAdapter.ModelViewHolder> {
    private List<OfflineModelInfo> models;
    private OnModelActionListener listener;
    
    public interface OnModelActionListener {
        void onDownloadModel(OfflineModelInfo model);
        void onDeleteModel(OfflineModelInfo model);
    }
    
    public OfflineModelAdapter(List<OfflineModelInfo> models, OnModelActionListener listener) {
        this.models = models;
        this.listener = listener;
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
        OfflineModelInfo model = models.get(position);
        holder.bind(model);
    }
    
    @Override
    public int getItemCount() {
        return models.size();
    }
    
    /**
     * Updates the models list using DiffUtil for efficient RecyclerView updates.
     * This method calculates the difference between the old and new model lists
     * and only updates the items that have changed, improving performance.
     *
     * @param newModels The new list of offline models to display
     */
    public void updateModels(List<OfflineModelInfo> newModels) {
        if (newModels == null) {
            return;
        }
        
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new OfflineModelDiffCallback(models, newModels));
        
        this.models = newModels;
        
        diffResult.dispatchUpdatesTo(this);
    }
    
    public void updateProgress(OfflineModelInfo model, int progress) {
        int position = models.indexOf(model);
        if (position != -1) {
            model.setDownloadProgress(progress);
            // Keep downloading state true until success callback explicitly sets it to false
            // This ensures progress bar shows 100% while still in downloading state
            if (progress < 0) {
                // Hide progress (error case)
                model.setDownloading(false);
            } else if (progress < 100) {
                // Still downloading
                model.setDownloading(true);
            }
            // For progress == 100, don't change downloading state - let success callback handle it
            notifyItemChanged(position);
        }
    }
    
    class ModelViewHolder extends RecyclerView.ViewHolder {
        private TextView textLanguageName;
        private TextView textLanguageCode;
        private TextView textStatus;
        private TextView textSize;
        private Button buttonAction;
        private ProgressBar progressDownload;
        private TextView textProgress;
        
        public ModelViewHolder(@NonNull View itemView) {
            super(itemView);
            textLanguageName = itemView.findViewById(R.id.text_language_name);
            textLanguageCode = itemView.findViewById(R.id.text_language_code);
            textStatus = itemView.findViewById(R.id.text_status);
            textSize = itemView.findViewById(R.id.text_size);
            buttonAction = itemView.findViewById(R.id.button_action);
            progressDownload = itemView.findViewById(R.id.progress_download);
            textProgress = itemView.findViewById(R.id.text_progress);
        }
        
        public void bind(OfflineModelInfo model) {
            textLanguageName.setText(model.getLanguageName());
            textLanguageCode.setText(model.getLanguageCode());
            textSize.setText(itemView.getContext().getString(R.string.model_size, model.getFormattedSize()));
            
            if (model.isDownloading()) {
                // Model is currently downloading
                textStatus.setText(R.string.model_downloading);
                textStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_orange_light));
                
                buttonAction.setText(R.string.cancel);
                buttonAction.setEnabled(false);
                
                progressDownload.setVisibility(View.VISIBLE);
                progressDownload.setProgress(model.getDownloadProgress());
                
                textProgress.setVisibility(View.VISIBLE);
                textProgress.setText(itemView.getContext().getString(R.string.download_progress, model.getDownloadProgress()));
                
            } else if (model.isDownloaded()) {
                // Model is downloaded
                textStatus.setText(R.string.model_downloaded);
                textStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_green_light));
                
                buttonAction.setText(R.string.delete_model);
                buttonAction.setEnabled(true);
                buttonAction.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteModel(model);
                    }
                });
                
                progressDownload.setVisibility(View.GONE);
                textProgress.setVisibility(View.GONE);
                
            } else {
                // Model is not downloaded
                textStatus.setText(R.string.model_not_downloaded);
                textStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.darker_gray));
                
                buttonAction.setText(R.string.download_model);
                buttonAction.setEnabled(true);
                buttonAction.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDownloadModel(model);
                    }
                });
                
                progressDownload.setVisibility(View.GONE);
                textProgress.setVisibility(View.GONE);
            }
        }
    }
}