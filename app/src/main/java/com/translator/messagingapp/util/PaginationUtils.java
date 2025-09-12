package com.translator.messagingapp.util;

import com.translator.messagingapp.util.*;

import com.translator.messagingapp.conversation.*;

import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Utility class for handling pagination in RecyclerView.
 */
public class PaginationUtils {

    /**
     * Interface for pagination loading complete callback.
     */
    public interface OnLoadingCompleteCallback {
        /**
         * Called when loading is complete.
         */
        void onLoadingComplete();
    }

    /**
     * Scroll listener for pagination.
     */
    public static class PaginationScrollListener extends RecyclerView.OnScrollListener {
        private boolean isLoading = false;
        private final LinearLayoutManager layoutManager;
        private final OnLoadingCompleteCallback loadMoreCallback;
        private final int threshold;
        private final View loadingIndicator;

        /**
         * Creates a new PaginationScrollListener.
         *
         * @param layoutManager The LinearLayoutManager for the RecyclerView
         * @param loadMoreCallback The callback to invoke when more items need to be loaded
         * @param threshold The number of items from the end to trigger loading more
         * @param loadingIndicator The loading indicator view
         */
        public PaginationScrollListener(
                LinearLayoutManager layoutManager,
                OnLoadingCompleteCallback loadMoreCallback,
                int threshold,
                View loadingIndicator) {
            this.layoutManager = layoutManager;
            this.loadMoreCallback = loadMoreCallback;
            this.threshold = threshold;
            this.loadingIndicator = loadingIndicator;
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            int totalItemCount = layoutManager.getItemCount();
            int lastVisibleItem = layoutManager.findLastVisibleItemPosition();

            if (!isLoading && totalItemCount <= (lastVisibleItem + threshold)) {
                // Show loading indicator
                if (loadingIndicator != null) {
                    loadingIndicator.setVisibility(View.VISIBLE);
                }

                isLoading = true;

                // Load more data
                if (loadMoreCallback != null) {
                    loadMoreCallback.onLoadingComplete();
                }

                // Hide loading indicator
                if (loadingIndicator != null) {
                    loadingIndicator.setVisibility(View.GONE);
                }

                isLoading = false;
            }
        }

        private boolean hasMoreItems = true;

        /**
         * Sets the loading state.
         *
         * @param loading The loading state
         */
        public void setLoading(boolean loading) {
            isLoading = loading;
        }

        /**
         * Gets the loading state.
         *
         * @return The loading state
         */
        public boolean isLoading() {
            return isLoading;
        }

        /**
         * Sets whether there are more items to load.
         *
         * @param hasMoreItems True if there are more items to load
         */
        public void setHasMoreItems(boolean hasMoreItems) {
            this.hasMoreItems = hasMoreItems;
        }

        /**
         * Gets whether there are more items to load.
         *
         * @return True if there are more items to load
         */
        public boolean hasMoreItems() {
            return hasMoreItems;
        }
    }

    /**
     * Sets up pagination for a RecyclerView.
     *
     * @param recyclerView The RecyclerView to set up pagination for
     * @param layoutManager The LinearLayoutManager for the RecyclerView
     * @param loadMoreCallback The callback to invoke when more items need to be loaded
     * @param threshold The number of items from the end to trigger loading more
     * @param loadingIndicator The loading indicator view
     * @return The PaginationScrollListener
     */
    public static PaginationScrollListener setupPagination(
            RecyclerView recyclerView,
            LinearLayoutManager layoutManager,
            OnLoadingCompleteCallback loadMoreCallback,
            int threshold,
            View loadingIndicator) {

        PaginationScrollListener scrollListener = new PaginationScrollListener(
                layoutManager, loadMoreCallback, threshold, loadingIndicator);
        recyclerView.addOnScrollListener(scrollListener);
        return scrollListener;
    }

    /**
     * Sets up pagination for a RecyclerView with a lambda callback.
     *
     * @param recyclerView The RecyclerView to set up pagination for
     * @param loadMoreCallback The lambda callback to invoke when more items need to be loaded
     * @param threshold The number of items from the end to trigger loading more
     * @param loadingIndicator The loading indicator view
     * @return The PaginationScrollListener
     */
    public static PaginationScrollListener setupPagination(
            RecyclerView recyclerView,
            Runnable loadMoreCallback,
            int threshold,
            View loadingIndicator) {

        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null) {
            throw new IllegalStateException("RecyclerView must have a LinearLayoutManager");
        }

        OnLoadingCompleteCallback callback = () -> {
            if (loadMoreCallback != null) {
                loadMoreCallback.run();
            }
        };

        return setupPagination(recyclerView, layoutManager, callback, threshold, loadingIndicator);
    }

    /**
     * Sets up pagination for a RecyclerView with a lambda callback.
     * This overload is used by OptimizedConversationActivity.
     *
     * @param recyclerView The RecyclerView to set up pagination for
     * @param loadMoreCallback The lambda callback to invoke when more items need to be loaded
     * @param threshold The number of items from the end to trigger loading more
     * @param loadingIndicator The loading indicator view (as a ProgressBar)
     * @return The PaginationScrollListener
     */
    public static PaginationScrollListener setupPagination(
            RecyclerView recyclerView,
            Runnable loadMoreCallback,
            int threshold,
            android.widget.ProgressBar loadingIndicator) {

        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null) {
            throw new IllegalStateException("RecyclerView must have a LinearLayoutManager");
        }

        OnLoadingCompleteCallback callback = () -> {
            if (loadMoreCallback != null) {
                loadMoreCallback.run();
            }
        };

        return setupPagination(recyclerView, layoutManager, callback, threshold, loadingIndicator);
    }
}
