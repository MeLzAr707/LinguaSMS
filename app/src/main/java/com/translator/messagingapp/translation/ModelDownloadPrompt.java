package com.translator.messagingapp.translation;

import com.translator.messagingapp.R;
import com.translator.messagingapp.message.*;

import com.translator.messagingapp.translation.*;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

/**
 * Utility class for prompting users to download missing language models.
 * Handles the UI flow for model download prompts and progress indication.
 */
public class ModelDownloadPrompt {
    private static final String TAG = "ModelDownloadPrompt";
    
    /**
     * Interface for handling model download prompt results.
     */
    public interface ModelDownloadCallback {
        void onDownloadCompleted(boolean success, String errorMessage);
        void onUserDeclined();
    }
    
    /**
     * Shows a prompt asking the user if they want to download a missing language model.
     * 
     * @param activity The activity context for showing dialogs
     * @param sourceLanguage The source language code
     * @param targetLanguage The target language code  
     * @param translationManager The translation manager for language name resolution
     * @param offlineService The offline translation service for downloading models
     * @param callback The callback to handle the result
     */
    public static void promptForMissingModel(Activity activity, 
                                           String sourceLanguage, 
                                           String targetLanguage,
                                           TranslationManager translationManager,
                                           OfflineTranslationService offlineService,
                                           ModelDownloadCallback callback) {
        
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Activity is not valid for showing dialog");
            callback.onUserDeclined();
            return;
        }
        
        // Determine which models are missing
        boolean sourceAvailable = offlineService.isModelAvailable(sourceLanguage);
        boolean targetAvailable = offlineService.isModelAvailable(targetLanguage);
        
        String missingLanguages;
        if (!sourceAvailable && !targetAvailable) {
            String sourceName = translationManager.getLanguageName(sourceLanguage);
            String targetName = translationManager.getLanguageName(targetLanguage);
            missingLanguages = sourceName + " and " + targetName;
        } else if (!sourceAvailable) {
            missingLanguages = translationManager.getLanguageName(sourceLanguage);
        } else {
            missingLanguages = translationManager.getLanguageName(targetLanguage);
        }
        
        // Show confirmation dialog on UI thread
        activity.runOnUiThread(() -> {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.missing_language_model_title)
                    .setMessage(activity.getString(R.string.missing_language_model_message, missingLanguages))
                    .setPositiveButton(R.string.download_models, (dialog, which) -> {
                        downloadMissingModels(activity, sourceLanguage, targetLanguage, 
                                            sourceAvailable, targetAvailable, offlineService, callback);
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {
                        callback.onUserDeclined();
                    })
                    .setCancelable(false)
                    .show();
        });
    }
    
    /**
     * Downloads the missing language models with progress indication.
     */
    private static void downloadMissingModels(Activity activity,
                                            String sourceLanguage,
                                            String targetLanguage, 
                                            boolean sourceAvailable,
                                            boolean targetAvailable,
                                            OfflineTranslationService offlineService,
                                            ModelDownloadCallback callback) {
        
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Activity is not valid for showing progress dialog");
            callback.onDownloadCompleted(false, "Activity no longer available");
            return;
        }
        
        // Show progress dialog on UI thread
        activity.runOnUiThread(() -> {
            ProgressDialog progressDialog = new ProgressDialog(activity);
            progressDialog.setTitle(R.string.downloading_language_models);
            progressDialog.setMessage(activity.getString(R.string.downloading_models_message));
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMax(100);
            progressDialog.setCancelable(false);
            progressDialog.show();
            
            // Track download state
            final boolean[] downloadComplete = {false, false}; // [source, target]
            final String[] downloadError = {null};
            
            // Download source model if needed
            if (!sourceAvailable) {
                offlineService.downloadLanguageModel(sourceLanguage, new OfflineTranslationService.ModelDownloadCallback() {
                    @Override
                    public void onDownloadProgress(int progress) {
                        activity.runOnUiThread(() -> {
                            if (!activity.isFinishing() && !activity.isDestroyed() && progressDialog.isShowing()) {
                                // Update progress for source model (0-50%)
                                int adjustedProgress = progress / 2;
                                progressDialog.setProgress(adjustedProgress);
                            }
                        });
                    }
                    
                    @Override
                    public void onDownloadComplete(boolean success, String errorMessage) {
                        downloadComplete[0] = true;
                        if (!success) {
                            downloadError[0] = errorMessage;
                        }
                        
                        activity.runOnUiThread(() -> checkDownloadCompletion(activity, progressDialog, 
                                targetAvailable, downloadComplete, downloadError, callback));
                    }
                });
            } else {
                downloadComplete[0] = true;
            }
            
            // Download target model if needed
            if (!targetAvailable) {
                offlineService.downloadLanguageModel(targetLanguage, new OfflineTranslationService.ModelDownloadCallback() {
                    @Override
                    public void onDownloadProgress(int progress) {
                        activity.runOnUiThread(() -> {
                            if (!activity.isFinishing() && !activity.isDestroyed() && progressDialog.isShowing()) {
                                // Update progress for target model (50-100%)
                                int adjustedProgress = 50 + (progress / 2);
                                progressDialog.setProgress(adjustedProgress);
                            }
                        });
                    }
                    
                    @Override
                    public void onDownloadComplete(boolean success, String errorMessage) {
                        downloadComplete[1] = true;
                        if (!success) {
                            downloadError[0] = errorMessage;
                        }
                        
                        activity.runOnUiThread(() -> checkDownloadCompletion(activity, progressDialog, 
                                sourceAvailable, downloadComplete, downloadError, callback));
                    }
                });
            } else {
                downloadComplete[1] = true;
            }
            
            // If both models are already available (shouldn't happen, but safety check)
            if (sourceAvailable && targetAvailable) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                callback.onDownloadCompleted(true, null);
            }
        });
    }
    
    /**
     * Checks if all required downloads are complete and handles the result.
     */
    private static void checkDownloadCompletion(Activity activity,
                                              ProgressDialog progressDialog,
                                              boolean otherModelAvailable,
                                              boolean[] downloadComplete,
                                              String[] downloadError,
                                              ModelDownloadCallback callback) {
        
        // Check if all downloads are complete
        boolean allComplete = downloadComplete[0] && downloadComplete[1];
        
        if (allComplete) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            
            // Check if any downloads failed
            if (downloadError[0] != null) {
                callback.onDownloadCompleted(false, downloadError[0]);
            } else {
                callback.onDownloadCompleted(true, null);
            }
        }
    }
}