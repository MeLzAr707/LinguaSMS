package com.translator.messagingapp;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Dialog for picking emojis.
 */
public class EmojiPickerDialog extends Dialog {
    private OnEmojiSelectedListener listener;
    private boolean isReactionPicker;

    /**
     * Interface for emoji selection events.
     */
    public interface OnEmojiSelectedListener {
        void onEmojiSelected(String emoji);
    }

    /**
     * Creates a new emoji picker dialog.
     *
     * @param context The context
     * @param listener The listener for emoji selection events
     * @param isReactionPicker True if this is a reaction picker, false if it's an emoji input picker
     */
    public EmojiPickerDialog(@NonNull Context context, OnEmojiSelectedListener listener, boolean isReactionPicker) {
        super(context);
        this.listener = listener;
        this.isReactionPicker = isReactionPicker;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.emoji_picker_dialog);

        // Set title
        TextView titleTextView = findViewById(R.id.emoji_picker_title);
        titleTextView.setText(isReactionPicker ? R.string.add_reaction : R.string.insert_emoji);

        // Set up emoji grid
        RecyclerView emojiRecyclerView = findViewById(R.id.emoji_recycler_view);
        emojiRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 8));

        // Get emojis based on type
        List<String> emojis = isReactionPicker ? 
                EmojiUtils.getReactionEmojis() : 
                EmojiUtils.getAllEmojis();

        // Set adapter
        EmojiAdapter adapter = new EmojiAdapter(emojis, emoji -> {
            if (listener != null) {
                listener.onEmojiSelected(emoji);
            }
            // Add to recent emojis
            EmojiUtils.addRecentEmoji(getContext(), emoji);
            dismiss();
        });
        emojiRecyclerView.setAdapter(adapter);

        // Set up recent emojis
        setupRecentEmojis();

        // Set up cancel button
        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> dismiss());
    }

    /**
     * Sets up the recently used emojis section.
     */
    private void setupRecentEmojis() {
        LinearLayout recentContainer = findViewById(R.id.recently_used_container);
        List<String> recentEmojis = EmojiUtils.getRecentEmojis(getContext());

        // Clear existing views
        recentContainer.removeAllViews();

        // Show message if no recent emojis
        if (recentEmojis.isEmpty()) {
            TextView noRecentText = new TextView(getContext());
            noRecentText.setText(R.string.no_recent_emojis);
            noRecentText.setTextSize(14);
            noRecentText.setPadding(8, 8, 8, 8);
            recentContainer.addView(noRecentText);
            return;
        }

        // Add recent emojis
        for (String emoji : recentEmojis) {
            TextView emojiView = new TextView(getContext());
            emojiView.setText(emoji);
            emojiView.setTextSize(24);
            emojiView.setPadding(16, 8, 16, 8);
            emojiView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEmojiSelected(emoji);
                }
                // Add to recent emojis (to update order)
                EmojiUtils.addRecentEmoji(getContext(), emoji);
                dismiss();
            });
            recentContainer.addView(emojiView);
        }
    }

    /**
     * Shows an emoji picker dialog.
     *
     * @param context The context
     * @param listener The listener for emoji selection events
     * @param isReactionPicker True if this is a reaction picker, false if it's an emoji input picker
     */
    public static void show(Context context, OnEmojiSelectedListener listener, boolean isReactionPicker) {
        EmojiPickerDialog dialog = new EmojiPickerDialog(context, listener, isReactionPicker);
        dialog.show();
    }

    /**
     * Adapter for the emoji grid.
     */
    private static class EmojiAdapter extends RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder> {
        private List<String> emojis;
        private OnEmojiClickListener listener;

        /**
         * Interface for emoji click events.
         */
        interface OnEmojiClickListener {
            void onEmojiClick(String emoji);
        }

        /**
         * Creates a new emoji adapter.
         *
         * @param emojis The list of emojis
         * @param listener The listener for emoji click events
         */
        EmojiAdapter(List<String> emojis, OnEmojiClickListener listener) {
            this.emojis = emojis;
            this.listener = listener;
        }

        @NonNull
        @Override
        public EmojiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView emojiView = new TextView(parent.getContext());
            emojiView.setTextSize(24);
            emojiView.setPadding(16, 16, 16, 16);
            return new EmojiViewHolder(emojiView);
        }

        @Override
        public void onBindViewHolder(@NonNull EmojiViewHolder holder, int position) {
            String emoji = emojis.get(position);
            holder.emojiView.setText(emoji);
            holder.emojiView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEmojiClick(emoji);
                }
            });
        }

        @Override
        public int getItemCount() {
            return emojis.size();
        }

        /**
         * ViewHolder for emoji items.
         */
        static class EmojiViewHolder extends RecyclerView.ViewHolder {
            TextView emojiView;

            EmojiViewHolder(@NonNull TextView itemView) {
                super(itemView);
                emojiView = itemView;
            }
        }
    }
}