package com.translator.messagingapp;

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
     * Sets up pagination for a RecyclerView.
     *
     * @param recyclerView The RecyclerView to set up pagination for
     * @param layoutManager The LinearLayoutManager for the RecyclerView
     * @param loadMoreCallback The callback to invoke when more items need to be loaded
     * @param threshold The number of items from the end to trigger loading more
     * @param loadingIndicator The loading indicator view
     */
    public static void setupPagination(
            RecyclerView recyclerView,
            LinearLayoutManager layoutManager,
            OnLoadingCompleteCallback loadMoreCallback,
            int threshold,
            View loadingIndicator) {

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private boolean isLoading = false;

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
        });
    }
}