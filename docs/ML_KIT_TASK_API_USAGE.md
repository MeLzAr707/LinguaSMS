# ML Kit Task API Usage Guide

## Issue Background

Build error encountered:
```
OfflineModelManager.java:196: error: cannot find symbol
        downloadTask.addOnProgressListener(progress -> {
                    ^
  symbol:   method addOnProgressListener((progress)[...]; } })
  location: variable downloadTask of type Task<Void>
```

## Root Cause

The `Task<Void>` returned by ML Kit's `downloadModelIfNeeded()` method does **NOT** have an `addOnProgressListener` method.

## Available Methods for Task<T>

The Google Play Services Tasks API provides these methods:

- `addOnSuccessListener(OnSuccessListener<? super TResult> listener)`
- `addOnFailureListener(OnFailureListener listener)`
- `addOnCompleteListener(OnCompleteListener<TResult> listener)`

## ❌ INCORRECT Usage (Causes Build Error)

```java
// THIS IS WRONG - will cause compilation error
Task<Void> downloadTask = translator.downloadModelIfNeeded();
downloadTask.addOnProgressListener(progress -> {
    // ERROR: method does not exist
});
```

## ✅ CORRECT Usage 

### 1. Basic Success/Failure Handling

```java
translator.downloadModelIfNeeded()
    .addOnSuccessListener(aVoid -> {
        Log.d(TAG, "Model downloaded successfully");
        // Handle success
    })
    .addOnFailureListener(exception -> {
        Log.e(TAG, "Model download failed", exception);
        // Handle failure
    });
```

### 2. Complete Listener (Success + Failure)

```java
translator.downloadModelIfNeeded()
    .addOnCompleteListener(task -> {
        if (task.isSuccessful()) {
            Log.d(TAG, "Model downloaded successfully");
            // Handle success
        } else {
            Log.e(TAG, "Model download failed", task.getException());
            // Handle failure
        }
    });
```

## Progress Tracking Implementation

Since ML Kit doesn't provide progress callbacks for model downloads, progress must be implemented at the application level:

### Option 1: Custom Callback Interface (Currently Used)

```java
public interface ModelDownloadCallback {
    void onDownloadComplete(boolean success, String languageCode, String errorMessage);
    void onDownloadProgress(String languageCode, int progress);
}

// In OfflineTranslationService:
public void downloadLanguageModel(String languageCode, ModelDownloadCallback callback) {
    // Convert to ML Kit format
    String mlkitCode = convertToMLKitLanguageCode(languageCode);
    
    // Create translator
    TranslatorOptions options = new TranslatorOptions.Builder()
            .setSourceLanguage(mlkitCode)
            .setTargetLanguage(mlkitCode)
            .build();
    
    Translator translator = Translation.getClient(options);
    
    // Start download with proper Task API
    translator.downloadModelIfNeeded()
            .addOnSuccessListener(aVoid -> {
                // Verify download success
                verifyModelDownloadSuccess(mlkitCode, languageCode, callback);
            })
            .addOnFailureListener(exception -> {
                if (callback != null) {
                    callback.onDownloadComplete(false, languageCode, exception.getMessage());
                }
            });
}
```

### Option 2: Simulated Progress (For Demonstration)

```java
// In OfflineModelManager (for UI demonstration purposes):
public void downloadModel(OfflineModelInfo model, DownloadListener listener) {
    new Thread(() -> {
        try {
            // Simulate progress updates
            for (int progress = 0; progress <= 100; progress += 10) {
                Thread.sleep(500);
                
                if (listener != null) {
                    listener.onProgress(progress);
                }
            }
            
            // Handle completion
            if (listener != null) {
                listener.onSuccess();
            }
        } catch (InterruptedException e) {
            if (listener != null) {
                listener.onError("Download interrupted");
            }
        }
    }).start();
}
```

## Best Practices

1. **Always use the correct Task API methods** - never try to use non-existent methods like `addOnProgressListener`

2. **Implement progress at application level** - ML Kit doesn't provide granular download progress

3. **Handle both success and failure cases** - network issues can cause downloads to fail

4. **Verify download success** - test the model after download to ensure it works

5. **Use proper error handling** - provide meaningful error messages to users

## Current Implementation Status

The LinguaSMS app correctly implements ML Kit Task API usage in:

- `OfflineTranslationService.java` - Real ML Kit downloads with proper Task handling
- `OfflineModelsActivity.java` - UI integration with proper callback handling  
- `OfflineModelAdapter.java` - Progress display in RecyclerView

All code follows the correct patterns and should compile without errors.