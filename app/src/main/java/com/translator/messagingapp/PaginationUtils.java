package com.translator.messagingapp;

import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Utility class for implementing pagination in RecyclerViews.
 */
public class PaginationUtils {
    private static final String TAG = "PaginationUtils";
    
    /**
     * Sets up pagination for a RecyclerView.
     *
     * @param recyclerView The RecyclerView to paginate
     * @param loadMoreCallback Callback to load more items
     * @param threshold Number of items from the end to trigger loading more
     * @param loadingIndicator Optional loading indicator to show when loading
     * @return The scroll listener that was added
     */
    public static RecyclerView.OnScrollListener setupPagination(
            RecyclerView recyclerView,
            LoadMoreCallback loadMoreCallback,
            int threshold,
            ProgressBar loadingIndicator) {
        
        PaginationScrollListener scrollListener = new PaginationScrollListener(
                (LinearLayoutManager) recyclerView.getLayoutManager(),
                loadMoreCallback,
                threshold,
                loadingIndicator);
        
        recyclerView.addOnScrollListener(scrollListener);
        return scrollListener;
    }
    
    /**
     * Scroll listener for pagination.
     */
    public static class PaginationScrollListener extends RecyclerView.OnScrollListener {
        private final LinearLayoutManager layoutManager;
        private final LoadMoreCallback loadMoreCallback;
        private final int threshold;
        private final ProgressBar loadingIndicator;
        private boolean isLoading = false;
        private boolean hasMoreItems = true;
        
        public PaginationScrollListener(
                LinearLayoutManager layoutManager,
                LoadMoreCallback loadMoreCallback,
                int threshold,
                ProgressBar loadingIndicator) {
            this.layoutManager = layoutManager;
            this.loadMoreCallback = loadMoreCallback;
            this.threshold = threshold;
            this.loadingIndicator = loadingIndicator;
        }
        
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            
            // Only trigger loading more when scrolling down
            if (dy <= 0) {
                return;
            }
            
            int visibleItemCount = layoutManager.getChildCount();
            int totalItemCount = layoutManager.getItemCount();
            int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
            
            if (!isLoading && hasMoreItems) {
                if ((visibleItemCount + firstVisibleItemPosition + threshold) >= totalItemCount
                        && firstVisibleItemPosition >= 0) {
                    loadMore();
                }
            }
        }
        
        /**
         * Loads more items.
         */
        private void loadMore() {
            isLoading = true;
            showLoading(true);
            loadMoreCallback.onLoadMore(() -> {
                isLoading = false;
                showLoading(false);
            });
        }
        
        /**
         * Shows or hides the loading indicator.
         *
         * @param show Whether to show the loading indicator
         */
        private void showLoading(boolean show) {
            if (loadingIndicator != null) {
                loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        }
        
        /**
         * Sets whether there are more items to load.
         *
         * @param hasMore Whether there are more items
         */
        public void setHasMoreItems(boolean hasMore) {
            this.hasMoreItems = hasMore;
        }
        
        /**
         * Sets whether items are currently being loaded.
         *
         * @param loading Whether items are being loaded
         */
        public void setLoading(boolean loading) {
            this.isLoading = loading;
            showLoading(loading);
        }
        
        /**
         * Resets the pagination state.
         */
        public void reset() {
            this.isLoading = false;
            this.hasMoreItems = true;
            showLoading(false);
        }
    }
    
    /**
     * Callback interface for loading more items.
     */
    public interface LoadMoreCallback {
        /**
         * Called when more items should be loaded.
         *
         * @param onLoadingComplete Callback to call when loading is complete
         */
        void onLoadMore(OnLoadingCompleteCallback onLoadingComplete);
    }
    
    /**
     * Callback interface for when loading is complete.
     */
    public interface OnLoadingCompleteCallback {
        /**
         * Called when loading is complete.
         */
        void onLoadingComplete();
    }
}