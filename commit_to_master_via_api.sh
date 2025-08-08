#!/bin/bash

# Script to commit changes directly to master branch via GitHub API
# This script requires a GitHub personal access token with repo permissions

# Check if token is provided
if [ -z "$1" ]; then
  echo "Usage: $0 <github_token>"
  echo "Please provide your GitHub personal access token as an argument."
  exit 1
fi

# Configuration
GITHUB_TOKEN="$1"
REPO_OWNER="MeLzAr707"
REPO_NAME="LinguaSMS"
BRANCH="master"
COMMIT_MESSAGE="Add comprehensive performance optimizations"

echo "Starting direct commit to $BRANCH branch..."

# Get the latest commit SHA of the master branch
echo "Fetching latest commit SHA..."
LATEST_COMMIT_SHA=$(curl -s -H "Authorization: token $GITHUB_TOKEN" \
  "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/git/refs/heads/$BRANCH" | \
  grep -o '"sha": "[^"]*' | grep -o '[a-f0-9]*$')

echo "Latest commit SHA: $LATEST_COMMIT_SHA"

# Function to create a blob for a file
create_blob() {
  local file_path=$1
  local file_content=$(cat "$file_path" | base64)
  
  curl -s -X POST \
    -H "Authorization: token $GITHUB_TOKEN" \
    -H "Accept: application/vnd.github.v3+json" \
    "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/git/blobs" \
    -d "{
      &quot;content&quot;: &quot;$file_content&quot;,
      &quot;encoding&quot;: &quot;base64&quot;
    }" | grep -o '"sha": "[^"]*' | grep -o '[a-f0-9]*$'
}

# Create blobs for our new files
echo "Creating blobs for new files..."

# List of files to commit
FILES=(
  "app/src/main/java/com/translator/messagingapp/OptimizedMainActivity.java"
  "app/src/main/java/com/translator/messagingapp/OptimizedConversationActivity.java"
  "app/src/main/java/com/translator/messagingapp/OptimizedTranslatorApp.java"
  "app/src/main/java/com/translator/messagingapp/OptimizedMessageRecyclerAdapter.java"
  "app/src/main/java/com/translator/messagingapp/PerformanceBenchmark.java"
  "app/src/main/res/layout/item_loading.xml"
  "app/src/main/AndroidManifest.xml.optimized"
  "docs/performance_optimization_report.md"
  "docs/optimization_summary.md"
)

# Create tree entries for each file
TREE_ENTRIES=""
for file in "${FILES[@]}"; do
  echo "Processing $file..."
  BLOB_SHA=$(create_blob "$file")
  TREE_ENTRIES="$TREE_ENTRIES,{&quot;path&quot;:&quot;$file&quot;,&quot;mode&quot;:&quot;100644&quot;,&quot;type&quot;:&quot;blob&quot;,&quot;sha&quot;:&quot;$BLOB_SHA&quot;}"
done

# Remove the leading comma
TREE_ENTRIES=${TREE_ENTRIES:1}

# Create a new tree
echo "Creating new tree..."
TREE_SHA=$(curl -s -X POST \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/git/trees" \
  -d "{
    &quot;base_tree&quot;: &quot;$LATEST_COMMIT_SHA&quot;,
    &quot;tree&quot;: [$TREE_ENTRIES]
  }" | grep -o '"sha": "[^"]*' | grep -o '[a-f0-9]*$')

echo "New tree SHA: $TREE_SHA"

# Create a new commit
echo "Creating new commit..."
COMMIT_SHA=$(curl -s -X POST \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/git/commits" \
  -d "{
    &quot;message&quot;: &quot;$COMMIT_MESSAGE&quot;,
    &quot;parents&quot;: [&quot;$LATEST_COMMIT_SHA&quot;],
    &quot;tree&quot;: &quot;$TREE_SHA&quot;
  }" | grep -o '"sha": "[^"]*' | grep -o '[a-f0-9]*$')

echo "New commit SHA: $COMMIT_SHA"

# Update the reference
echo "Updating reference..."
curl -s -X PATCH \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/git/refs/heads/$BRANCH" \
  -d "{
    &quot;sha&quot;: &quot;$COMMIT_SHA&quot;,
    &quot;force&quot;: false
  }"

echo -e "\nChanges committed directly to $BRANCH branch."