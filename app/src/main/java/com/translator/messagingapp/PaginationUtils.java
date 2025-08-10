package com.translator.messagingapp;

import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class PaginationUtils {

    public interface OnLoadingCompleteCallback {
        void onLoadingComplete();
    }

    public static abstract class PaginationScrollListener extends RecyclerView.OnScrollListener {
        private boolean hasMoreItems = true;

        public void setHasMoreItems(boolean hasMoreItems) {
            this.hasMoreItems = hasMoreItems;
        }

        public boolean getHasMoreItems() {
            return hasMoreItems;
        }

        public abstract void loadMoreItems();
        public abstract boolean isLoading();
    }

    public static PaginationScrollListener setupPagination(
            RecyclerView recyclerView,
            OnLoadingCompleteCallback onLoadingComplete,
            int threshold,
            ProgressBar loadingIndicator) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        PaginationScrollListener listener = new PaginationScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy < 0) { // only trigger on scroll down
                    return;
                }

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading() && getHasMoreItems()) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= threshold) {
                        loadMoreItems();
                    }
                }
            }

            @Override
            public void loadMoreItems() {
                if (loadingIndicator != null) {
                    loadingIndicator.setVisibility(ProgressBar.VISIBLE);
                }
                onLoadingComplete.onLoadingComplete();
            }

            @Override
            public boolean isLoading() {
                return loadingIndicator != null && loadingIndicator.getVisibility() == ProgressBar.VISIBLE;
            }
        };
        recyclerView.addOnScrollListener(listener);
        return listener;
    }
}
