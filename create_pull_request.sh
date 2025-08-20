#!/bin/bash

# Script to create a pull request with all performance optimizations
# This script automates the steps to create a pull request for the performance optimizations

# Set colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}LinguaSMS Performance Optimization Script${NC}"
echo -e "${GREEN}=========================================${NC}"

# Check if git is installed
if ! command -v git &> /dev/null; then
    echo -e "${RED}Error: git is not installed. Please install git and try again.${NC}"
    exit 1
fi

# Step 1: Check if we're already in the LinguaSMS directory
if [ ! -d ".git" ]; then
    echo -e "${YELLOW}Not in a git repository. Checking if LinguaSMS directory exists...${NC}"
    
    if [ -d "LinguaSMS" ]; then
        echo -e "${YELLOW}LinguaSMS directory found. Changing to that directory...${NC}"
        cd LinguaSMS
    else
        echo -e "${YELLOW}LinguaSMS directory not found. Cloning the repository...${NC}"
        
        # Ask for GitHub username if forking
        read -p "Do you have write access to the original repository? (y/n): " has_write_access
        
        if [[ $has_write_access == "y" || $has_write_access == "Y" ]]; then
            git clone https://github.com/MeLzAr707/LinguaSMS.git
            cd LinguaSMS
        else
            read -p "Enter your GitHub username: " github_username
            git clone https://github.com/$github_username/LinguaSMS.git
            cd LinguaSMS
            
            # Add upstream remote
            git remote add upstream https://github.com/MeLzAr707/LinguaSMS.git
            echo -e "${GREEN}Added upstream remote to original repository${NC}"
        fi
    fi
fi

# Step 2: Create and checkout the performance-optimization branch
echo -e "${YELLOW}Creating and checking out performance-optimization branch...${NC}"
git checkout -b performance-optimization

# Step 3: Create necessary directories
echo -e "${YELLOW}Creating necessary directories...${NC}"
mkdir -p app/src/main/java/com/translator/messagingapp
mkdir -p app/src/main/res/layout
mkdir -p docs

# Step 4: Copy optimized files
echo -e "${YELLOW}Copying optimized files...${NC}"

# Function to copy a file if it exists
copy_file() {
    local src=$1
    local dest=$2
    
    if [ -f "$src" ]; then
        cp "$src" "$dest"
        echo -e "${GREEN}Copied $src to $dest${NC}"
    else
        echo -e "${RED}Warning: $src not found${NC}"
    fi
}

# Copy Java files
copy_file "../OptimizedMessageCache.java" "app/src/main/java/com/translator/messagingapp/"
copy_file "../OptimizedContactUtils.java" "app/src/main/java/com/translator/messagingapp/"
copy_file "../PaginationUtils.java" "app/src/main/java/com/translator/messagingapp/"
copy_file "../MessageDiffCallback.java" "app/src/main/java/com/translator/messagingapp/"
copy_file "../OptimizedMessageService.java" "app/src/main/java/com/translator/messagingapp/"
copy_file "../OptimizedMainActivity.java" "app/src/main/java/com/translator/messagingapp/"
copy_file "../OptimizedConversationActivity.java" "app/src/main/java/com/translator/messagingapp/"
copy_file "../OptimizedTranslatorApp.java" "app/src/main/java/com/translator/messagingapp/"
copy_file "../OptimizedMessageRecyclerAdapter.java" "app/src/main/java/com/translator/messagingapp/"
copy_file "../PerformanceBenchmark.java" "app/src/main/java/com/translator/messagingapp/"

# Copy layout files
copy_file "../item_loading.xml" "app/src/main/res/layout/"

# Copy AndroidManifest.xml.optimized
copy_file "../AndroidManifest.xml.optimized" "app/src/main/"

# Copy documentation files
copy_file "../performance_optimization_report.md" "docs/"
copy_file "../optimization_summary.md" "docs/"

# Step 5: Fix build.gradle.kts
echo -e "${YELLOW}Fixing build.gradle.kts file...${NC}"
if [ -f "app/build.gradle.kts" ]; then
    # Remove the problematic line and keep only the correct one
    sed -i 's/annotationProcessor(libs.compiler)//' app/build.gradle.kts
    echo -e "${GREEN}Fixed build.gradle.kts file${NC}"
else
    echo -e "${RED}Warning: app/build.gradle.kts not found${NC}"
fi

# Step 6: Fix item_loading.xml
echo -e "${YELLOW}Fixing item_loading.xml file...${NC}"
if [ -f "app/src/main/res/layout/item_loading.xml" ]; then
    # Replace hardcoded string with string resource
    sed -i 's/android:text="Loading more messages..."/android:text="@string\/loading_messages"/' app/src/main/res/layout/item_loading.xml
    echo -e "${GREEN}Fixed item_loading.xml file${NC}"
else
    echo -e "${RED}Warning: app/src/main/res/layout/item_loading.xml not found${NC}"
fi

# Step 7: Commit changes
echo -e "${YELLOW}Committing changes...${NC}"
git add .
git commit -m "Add comprehensive performance optimizations

This commit adds several performance optimizations to address slow loading of conversations and messages:

1. Enhanced Caching:
   - Implemented OptimizedMessageCache with LRU caching
   - Added proper cache invalidation strategies
   - Implemented copy-on-read/write to prevent modification of cached data

2. Batch Contact Lookup:
   - Created OptimizedContactUtils for efficient batch processing
   - Added contact caching to avoid repeated lookups
   - Processes contacts in smaller batches to avoid query size limitations

3. Pagination Support:
   - Created PaginationUtils for implementing pagination in RecyclerViews
   - Implemented paginated message loading in OptimizedMessageService
   - Added infinite scrolling with smooth loading indicators

4. RecyclerView Optimizations:
   - Implemented MessageDiffCallback for efficient RecyclerView updates
   - Reduced unnecessary view rebinding and layout passes
   - Added view type caching for faster view recycling

5. Background Processing:
   - Replaced basic Thread usage with proper Executor implementation
   - Added structured concurrency patterns for better thread management
   - Implemented background prefetching of conversations and contacts

6. Lazy Loading for Attachments:
   - Created OptimizedMessageRecyclerAdapter with lazy loading
   - Added two-phase loading (thumbnail first, then full image)
   - Implemented image caching for faster reloading

7. Performance Benchmarking:
   - Added PerformanceBenchmark class for measuring improvements
   - Created metrics for key operations
   - Documented performance gains

8. Documentation:
   - Added detailed performance optimization report
   - Updated technical documentation
   - Created implementation guides"

# Step 8: Push changes
echo -e "${YELLOW}Pushing changes to remote repository...${NC}"
git push origin performance-optimization

# Step 9: Instructions for creating the pull request
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}Next Steps to Create Pull Request${NC}"
echo -e "${GREEN}=========================================${NC}"
echo -e "${YELLOW}1. Go to the repository on GitHub${NC}"
echo -e "${YELLOW}2. You should see a prompt to create a pull request from your recently pushed branch${NC}"
echo -e "${YELLOW}3. Click on 'Compare & pull request'${NC}"
echo -e "${YELLOW}4. Use the content from pull_request.md as the description${NC}"
echo -e "${YELLOW}5. Click 'Create pull request'${NC}"

echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}Script completed successfully!${NC}"
echo -e "${GREEN}=========================================${NC}"