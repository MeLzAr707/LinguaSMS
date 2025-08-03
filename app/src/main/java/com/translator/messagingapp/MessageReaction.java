package com.translator.messagingapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a reaction to a message.
 */
public class MessageReaction {
    private String emoji;
    private String userId;
    private long timestamp;
    private int count = 1; // Default count is 1 for a single reaction

    /**
     * Creates a new message reaction.
     *
     * @param emoji The emoji used for the reaction
     * @param userId The user ID who reacted
     * @param timestamp The timestamp of the reaction
     */
    public MessageReaction(String emoji, String userId, long timestamp) {
        this.emoji = emoji;
        this.userId = userId;
        this.timestamp = timestamp;
    }

    /**
     * Gets the emoji.
     *
     * @return The emoji
     */
    public String getEmoji() {
        return emoji;
    }

    /**
     * Sets the emoji.
     *
     * @param emoji The emoji
     */
    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    /**
     * Gets the user ID.
     *
     * @return The user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user ID.
     *
     * @param userId The user ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the timestamp.
     *
     * @return The timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp.
     *
     * @param timestamp The timestamp
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the count of this reaction.
     *
     * @return The count
     */
    public int getCount() {
        return count;
    }

    /**
     * Sets the count of this reaction.
     *
     * @param count The count
     */
    public void setCount(int count) {
        this.count = count;
    }

    /**
     * Utility class for managing reactions to a message.
     */
    public static class ReactionManager {
        private Map<String, List<MessageReaction>> reactionsByEmoji;
        private List<MessageReaction> allReactions;

        /**
         * Creates a new reaction manager.
         */
        public ReactionManager() {
            reactionsByEmoji = new HashMap<>();
            allReactions = new ArrayList<>();
        }

        /**
         * Adds a reaction to the message.
         *
         * @param reaction The reaction to add
         * @return True if the reaction was added, false if it already exists
         */
        public boolean addReaction(MessageReaction reaction) {
            // Check if the user already reacted with this emoji
            for (MessageReaction existingReaction : allReactions) {
                if (existingReaction.getUserId().equals(reaction.getUserId()) && 
                    existingReaction.getEmoji().equals(reaction.getEmoji())) {
                    return false; // User already reacted with this emoji
                }
            }

            // Add the reaction
            allReactions.add(reaction);

            // Add to emoji map
            List<MessageReaction> emojiReactions = reactionsByEmoji.get(reaction.getEmoji());
            if (emojiReactions == null) {
                emojiReactions = new ArrayList<>();
                reactionsByEmoji.put(reaction.getEmoji(), emojiReactions);
            }
            emojiReactions.add(reaction);

            return true;
        }

        /**
         * Removes a reaction from the message.
         *
         * @param userId The user ID
         * @param emoji The emoji
         * @return True if the reaction was removed, false if it doesn't exist
         */
        public boolean removeReaction(String userId, String emoji) {
            // Find and remove from all reactions
            MessageReaction reactionToRemove = null;
            for (MessageReaction reaction : allReactions) {
                if (reaction.getUserId().equals(userId) && reaction.getEmoji().equals(emoji)) {
                    reactionToRemove = reaction;
                    break;
                }
            }

            if (reactionToRemove != null) {
                allReactions.remove(reactionToRemove);

                // Remove from emoji map
                List<MessageReaction> emojiReactions = reactionsByEmoji.get(emoji);
                if (emojiReactions != null) {
                    emojiReactions.remove(reactionToRemove);
                    if (emojiReactions.isEmpty()) {
                        reactionsByEmoji.remove(emoji);
                    }
                }

                return true;
            }

            return false;
        }

        /**
         * Gets all reactions.
         *
         * @return The list of all reactions
         */
        public List<MessageReaction> getAllReactions() {
            return allReactions;
        }

        /**
         * Gets reactions by emoji.
         *
         * @param emoji The emoji
         * @return The list of reactions for the emoji
         */
        public List<MessageReaction> getReactionsByEmoji(String emoji) {
            return reactionsByEmoji.getOrDefault(emoji, new ArrayList<>());
        }

        /**
         * Gets the count of reactions by emoji.
         *
         * @return A map of emoji to count
         */
        public Map<String, Integer> getReactionCounts() {
            Map<String, Integer> counts = new HashMap<>();
            for (Map.Entry<String, List<MessageReaction>> entry : reactionsByEmoji.entrySet()) {
                counts.put(entry.getKey(), entry.getValue().size());
            }
            return counts;
        }

        /**
         * Gets the total count of reactions.
         *
         * @return The total count
         */
        public int getTotalReactionCount() {
            return allReactions.size();
        }

        /**
         * Checks if a user has reacted with a specific emoji.
         *
         * @param userId The user ID
         * @param emoji The emoji
         * @return True if the user has reacted with the emoji, false otherwise
         */
        public boolean hasUserReacted(String userId, String emoji) {
            for (MessageReaction reaction : allReactions) {
                if (reaction.getUserId().equals(userId) && reaction.getEmoji().equals(emoji)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Gets all emojis used in reactions.
         *
         * @return The list of emojis
         */
        public List<String> getUsedEmojis() {
            return new ArrayList<>(reactionsByEmoji.keySet());
        }
    }
}
