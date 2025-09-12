package com.translator.messagingapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for emoji-related operations.
 */
public class EmojiUtils {
    private static final String PREFS_NAME = "emoji_prefs";
    private static final String KEY_RECENT_EMOJIS = "recent_emojis";
    private static final int MAX_RECENT_EMOJIS = 20;

    // Common emoji categories
    private static final String[] EMOJI_SMILEYS = {
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
            "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
            "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🤩",
            "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣", "😖",
            "😫", "😩", "🥺", "😢", "😭", "😤", "😠", "😡", "🤬", "🤯"
    };

    private static final String[] EMOJI_REACTIONS = {
            "👍", "👎", "❤️", "😂", "😢", "😡", "🎉", "🔥", "👀", "🙏"
    };

    /**
     * Gets a list of common reaction emojis.
     *
     * @return The list of reaction emojis
     */
    public static List<String> getReactionEmojis() {
        return Arrays.asList(EMOJI_REACTIONS);
    }

    /**
     * Gets a list of smiley emojis.
     *
     * @return The list of smiley emojis
     */
    public static List<String> getSmileyEmojis() {
        return Arrays.asList(EMOJI_SMILEYS);
    }

    /**
     * Gets a list of all emojis.
     *
     * @return The list of all emojis
     */
    public static List<String> getAllEmojis() {
        List<String> allEmojis = new ArrayList<>();
        allEmojis.addAll(Arrays.asList(EMOJI_SMILEYS));
        // Add more emoji categories as needed
        return allEmojis;
    }

    /**
     * Gets a list of recently used emojis.
     *
     * @param context The context
     * @return The list of recently used emojis
     */
    public static List<String> getRecentEmojis(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> recentEmojisSet = prefs.getStringSet(KEY_RECENT_EMOJIS, new HashSet<>());
        List<String> recentEmojis = new ArrayList<>(recentEmojisSet);
        Collections.reverse(recentEmojis); // Most recent first
        return recentEmojis;
    }

    /**
     * Adds an emoji to the recently used list.
     *
     * @param context The context
     * @param emoji The emoji to add
     */
    public static void addRecentEmoji(Context context, String emoji) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> recentEmojisSet = new LinkedHashSet<>(prefs.getStringSet(KEY_RECENT_EMOJIS, new HashSet<>()));
        
        // Remove the emoji if it already exists (to move it to the front)
        recentEmojisSet.remove(emoji);
        
        // Add the emoji to the front
        List<String> recentEmojis = new ArrayList<>(recentEmojisSet);
        recentEmojis.add(0, emoji);
        
        // Trim the list if it's too long
        if (recentEmojis.size() > MAX_RECENT_EMOJIS) {
            recentEmojis = recentEmojis.subList(0, MAX_RECENT_EMOJIS);
        }
        
        // Save the updated list
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(KEY_RECENT_EMOJIS, new LinkedHashSet<>(recentEmojis));
        editor.apply();
    }

    /**
     * Inserts an emoji at the current cursor position in an EditText.
     *
     * @param editText The EditText
     * @param emoji The emoji to insert
     */
    public static void insertEmoji(android.widget.EditText editText, String emoji) {
        Editable editable = editText.getText();
        int start = Math.max(editText.getSelectionStart(), 0);
        int end = Math.max(editText.getSelectionEnd(), 0);
        editable.replace(Math.min(start, end), Math.max(start, end), emoji);
    }

    /**
     * Checks if a string is a single emoji.
     *
     * @param str The string to check
     * @return True if the string is a single emoji, false otherwise
     */
    public static boolean isSingleEmoji(String str) {
        if (str == null || str.length() > 4) {
            return false;
        }
        
        // Simple check for emoji character range
        // This is a simplified check and may not catch all emojis
        return str.codePointCount(0, str.length()) == 1 && 
               str.codePointAt(0) >= 0x1F000;
    }
}