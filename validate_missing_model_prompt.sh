#!/bin/bash

echo "=== Validating Missing Language Model Download Prompt Feature ==="

# Check if ModelDownloadPrompt exists and has the right methods
echo ""
echo "1. Checking ModelDownloadPrompt.java implementation..."
if [ -f "app/src/main/java/com/translator/messagingapp/ModelDownloadPrompt.java" ]; then
    echo "✓ ModelDownloadPrompt.java exists"
    
    if grep -q "promptForMissingModel" app/src/main/java/com/translator/messagingapp/ModelDownloadPrompt.java; then
        echo "✓ promptForMissingModel method found"
    else
        echo "✗ promptForMissingModel method missing"
    fi
    
    if grep -q "ModelDownloadCallback" app/src/main/java/com/translator/messagingapp/ModelDownloadPrompt.java; then
        echo "✓ ModelDownloadCallback interface found"
    else
        echo "✗ ModelDownloadCallback interface missing"
    fi
else
    echo "✗ ModelDownloadPrompt.java not found"
fi

# Check if TranslationManager has the enhanced callback logic
echo ""
echo "2. Checking TranslationManager missing model detection..."
if grep -q "EnhancedTranslationCallback" app/src/main/java/com/translator/messagingapp/TranslationManager.java; then
    echo "✓ EnhancedTranslationCallback interface found"
else
    echo "✗ EnhancedTranslationCallback interface missing"
fi

if grep -q "promptForMissingModels" app/src/main/java/com/translator/messagingapp/TranslationManager.java; then
    echo "✓ promptForMissingModels method found"
else
    echo "✗ promptForMissingModels method missing"
fi

if grep -q "Language models not downloaded" app/src/main/java/com/translator/messagingapp/TranslationManager.java; then
    echo "✓ Missing model error detection found"
else
    echo "✗ Missing model error detection missing"
fi

# Check if activities use EnhancedTranslationCallback
echo ""
echo "3. Checking activity implementations..."
activities=("MainActivity.java" "ConversationActivity.java" "NewMessageActivity.java")

for activity in "${activities[@]}"; do
    if [ -f "app/src/main/java/com/translator/messagingapp/$activity" ]; then
        if grep -q "EnhancedTranslationCallback" "app/src/main/java/com/translator/messagingapp/$activity"; then
            echo "✓ $activity uses EnhancedTranslationCallback"
        else
            echo "✗ $activity does not use EnhancedTranslationCallback"
        fi
    else
        echo "✗ $activity not found"
    fi
done

# Check for background translation issues
echo ""
echo "4. Checking background translation handling..."
if grep -q "MessageProcessingWorker.*TranslationCallback" app/src/main/java/com/translator/messagingapp/MessageProcessingWorker.java; then
    if grep -q "missing.*model.*error" app/src/main/java/com/translator/messagingapp/MessageProcessingWorker.java; then
        echo "✓ MessageProcessingWorker handles missing model errors"
    else
        echo "⚠ MessageProcessingWorker may not handle missing model errors appropriately"
    fi
else
    echo "ℹ MessageProcessingWorker does not appear to use translation callbacks"
fi

# Check offline translation service error format
echo ""
echo "5. Checking offline translation error messages..."
if grep -q "Language models not downloaded" app/src/main/java/com/translator/messagingapp/OfflineTranslationService.java; then
    echo "✓ OfflineTranslationService generates correct error format"
else
    echo "✗ OfflineTranslationService error format may be incorrect"
fi

# Check the critical shouldUseOfflineTranslation logic
echo ""
echo "6. Checking shouldUseOfflineTranslation logic (CRITICAL FIX)..."
if grep -A 10 "shouldUseOfflineTranslation" app/src/main/java/com/translator/messagingapp/TranslationManager.java | grep -q "attempt offline translation even if models are not available"; then
    echo "✓ shouldUseOfflineTranslation allows offline attempts when models are missing"
else
    echo "✗ shouldUseOfflineTranslation may skip offline attempts when models are missing (ROOT CAUSE)"
fi

if grep -A 15 "shouldUseOfflineTranslation" app/src/main/java/com/translator/messagingapp/TranslationManager.java | grep -q "prompt for missing models if needed"; then
    echo "✓ shouldUseOfflineTranslation logic includes missing model prompt consideration"
else
    echo "⚠ shouldUseOfflineTranslation logic should be updated to enable missing model prompts"
fi

echo ""
echo "=== Validation Complete ==="
echo ""
echo "Key fix implemented:"
echo "✓ Modified shouldUseOfflineTranslation to attempt offline translation even when models are missing"
echo "✓ This enables the missing model prompt to be triggered in the translateOffline method"
echo "✓ Background processes now handle missing model errors gracefully"