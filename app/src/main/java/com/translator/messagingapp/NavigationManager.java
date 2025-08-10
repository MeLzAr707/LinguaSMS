package com.translator.messagingapp;

import android.app.Activity;
import android.util.Log;
import java.util.Stack;

/**
 * Manages navigation between activities and handles back navigation.
 * Uses the Singleton pattern to provide a global navigation state.
 */
public class NavigationManager {
    private static final String TAG = "NavigationManager";

    // Singleton instance
    private static NavigationManager instance;

    // Stack to track activity navigation
    private final Stack<String> activityStack = new Stack<>();

    // Private constructor for singleton
    private NavigationManager() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets the singleton instance of the NavigationManager.
     *
     * @return The NavigationManager instance
     */
    public static synchronized NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }

    /**
     * Pushes an activity onto the navigation stack.
     *
     * @param activityName The name of the activity
     */
    public void pushActivity(String activityName) {
        if (activityName != null && !activityName.isEmpty()) {
            Log.d(TAG, "Pushing activity: " + activityName);
            activityStack.push(activityName);
        }
    }

    /**
     * Pops the top activity from the navigation stack.
     *
     * @return The name of the popped activity, or null if the stack is empty
     */
    public String popActivity() {
        if (!activityStack.isEmpty()) {
            String activityName = activityStack.pop();
            Log.d(TAG, "Popping activity: " + activityName);
            return activityName;
        }
        return null;
    }

    /**
     * Peeks at the top activity on the navigation stack without removing it.
     *
     * @return The name of the top activity, or null if the stack is empty
     */
    public String peekActivity() {
        if (!activityStack.isEmpty()) {
            return activityStack.peek();
        }
        return null;
    }

    /**
     * Checks if the navigation stack contains a specific activity.
     *
     * @param activityName The name of the activity to check
     * @return true if the activity is in the stack, false otherwise
     */
    public boolean containsActivity(String activityName) {
        return activityName != null && activityStack.contains(activityName);
    }

    /**
     * Gets the size of the navigation stack.
     *
     * @return The number of activities in the stack
     */
    public int getStackSize() {
        return activityStack.size();
    }

    /**
     * Clears the navigation stack.
     */
    public void clearStack() {
        Log.d(TAG, "Clearing navigation stack");
        activityStack.clear();
    }

    /**
     * Handles back navigation.
     *
     * @param currentActivity The current activity
     */
    public void navigateBack(Activity currentActivity) {
        if (currentActivity == null) {
            return;
        }

        String currentActivityName = currentActivity.getClass().getName();
        Log.d(TAG, "Navigating back from: " + currentActivityName);

        // Pop the current activity from the stack
        if (!activityStack.isEmpty() && activityStack.peek().equals(currentActivityName)) {
            popActivity();
        }

        // If we're at the main activity, confirm exit
        if (currentActivity instanceof MainActivity) {
            // Let the MainActivity handle its own back button behavior
            currentActivity.onBackPressed();
        } else {
            // For other activities, just finish them
            currentActivity.finish();
        }
    }

    /**
     * Navigates to the home screen (MainActivity).
     *
     * @param currentActivity The current activity
     */
    public void navigateToHome(Activity currentActivity) {
        if (currentActivity == null) {
            return;
        }

        // Clear the stack except for MainActivity
        while (!activityStack.isEmpty()) {
            String activityName = activityStack.peek();
            if (activityName.equals(MainActivity.class.getName())) {
                break;
            }
            popActivity();
        }

        // If we're not already at MainActivity, finish the current activity
        if (!(currentActivity instanceof MainActivity)) {
            currentActivity.finish();
        }
    }

    /**
     * Gets a string representation of the navigation stack for debugging.
     *
     * @return A string representation of the stack
     */
    public String getStackTrace() {
        StringBuilder sb = new StringBuilder("Navigation Stack:\n");
        int index = 0;
        for (String activityName : activityStack) {
            sb.append(index++).append(": ").append(activityName).append("\n");
        }
        return sb.toString();
    }

    /**
     * Removes a specific activity from the navigation stack.
     *
     * @param activityName The name of the activity to remove
     * @return true if the activity was removed, false otherwise
     */
    public boolean removeActivity(String activityName) {
        if (activityName != null && !activityName.isEmpty()) {
            boolean removed = activityStack.remove(activityName);
            if (removed) {
                Log.d(TAG, "Removed activity from stack: " + activityName);
            }
            return removed;
        }
        return false;
    }

    /**
     * Checks if the navigation stack is empty.
     *
     * @return true if the stack is empty, false otherwise
     */
    public boolean isStackEmpty() {
        return activityStack.isEmpty();
    }
}

