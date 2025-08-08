#!/bin/bash

# Script to create a pull request via GitHub API
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
BASE_BRANCH="master"
HEAD_BRANCH="performance-optimization"
PR_TITLE="Add comprehensive performance optimizations"

# Read the PR body from the pull request template
PR_BODY=$(cat pull_request_template.md)

echo "Creating pull request from $HEAD_BRANCH to $BASE_BRANCH..."

# Create the pull request using GitHub API
curl -X POST \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/pulls \
  -d "{
    &quot;title&quot;: &quot;$PR_TITLE&quot;,
    &quot;body&quot;: &quot;$PR_BODY&quot;,
    &quot;head&quot;: &quot;$HEAD_BRANCH&quot;,
    &quot;base&quot;: &quot;$BASE_BRANCH&quot;
  }"

echo -e "\nPull request creation attempted. Check the output above for success or error messages."