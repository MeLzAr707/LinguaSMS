#!/bin/bash

# Script to commit changes and create a pull request for the performance optimizations

# Ensure we're in the right branch
git checkout performance-optimization

# Add all new files
git add app/src/main/java/com/translator/messagingapp/OptimizedMessageCache.java
git add app/src/main/java/com/translator/messagingapp/OptimizedContactUtils.java
git add app/src/main/java/com/translator/messagingapp/PaginationUtils.java
git add app/src/main/java/com/translator/messagingapp/MessageDiffCallback.java
git add app/src/main/java/com/translator/messagingapp/OptimizedMessageService.java
git add app/src/main/java/com/translator/messagingapp/OptimizedMainActivity.java
git add app/src/main/java/com/translator/messagingapp/OptimizedConversationActivity.java
git add app/src/main/java/com/translator/messagingapp/OptimizedTranslatorApp.java
git add app/src/main/java/com/translator/messagingapp/OptimizedMessageRecyclerAdapter.java
git add app/src/main/java/com/translator/messagingapp/PerformanceBenchmark.java
git add app/src/main/res/layout/item_loading.xml
git add app/src/main/AndroidManifest.xml.optimized
git add docs/performance_optimization_report.md
git add pull_request_template.md

# Commit the changes
git commit -F commit_message.txt

# Push to remote repository
git push origin performance-optimization

# Instructions for creating PR
echo "Changes committed and pushed to the 'performance-optimization' branch."
echo ""
echo "To create a pull request:"
echo "1. Go to the repository on GitHub"
echo "2. Click on 'Pull requests'"
echo "3. Click on 'New pull request'"
echo "4. Select 'master' as the base branch and 'performance-optimization' as the compare branch"
echo "5. Click on 'Create pull request'"
echo "6. Use the content from pull_request_template.md for the PR description"
echo "7. Assign reviewers and submit the PR"