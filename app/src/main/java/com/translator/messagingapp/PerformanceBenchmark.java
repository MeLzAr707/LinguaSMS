
package com.translator.messagingapp;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for benchmarking performance of different operations.
 */
public class PerformanceBenchmark {
    private static final String TAG = "PerformanceBenchmark";

    private final Context context;
    private final Map<String, List<Long>> benchmarkResults = new HashMap<>();
    private final GoogleTranslationService translationService;
    private final UserPreferences userPreferences;

    /**
     * Creates a new PerformanceBenchmark.
     *
     * @param context The context
     */
    public PerformanceBenchmark(Context context) {
        this.context = context;
        this.translationService = new GoogleTranslationService(context);
        this.userPreferences = new UserPreferences(context);
    }

    /**
     * Benchmarks loading conversations using the original MessageService.
     *
     * @return The time taken in milliseconds
     */
    public long benchmarkOriginalConversationLoading() {
        TranslationManager translationManager = new TranslationManager(context, translationService, userPreferences);
        MessageService messageService = new MessageService(context, translationManager);

        long startTime = SystemClock.elapsedRealtime();
        List<Conversation> conversations = messageService.loadConversations();
        long endTime = SystemClock.elapsedRealtime();

        long timeTaken = endTime - startTime;
        Log.d(TAG, "Original conversation loading took " + timeTaken + " ms for " +
                (conversations != null ? conversations.size() : 0) + " conversations");

        recordBenchmark("original_conversation_loading", timeTaken);
        return timeTaken;
    }

    /**
     * Benchmarks loading conversations using the optimized MessageService.
     *
     * @return The time taken in milliseconds
     */
    public long benchmarkOptimizedConversationLoading() {
        TranslationManager translationManager = new TranslationManager(context, translationService, userPreferences);
        MessageService messageService = new MessageService(context, translationManager);

        long startTime = SystemClock.elapsedRealtime();
        List<Conversation> conversations = messageService.loadConversations();
        long endTime = SystemClock.elapsedRealtime();

        long timeTaken = endTime - startTime;
        Log.d(TAG, "Optimized conversation loading took " + timeTaken + " ms for " +
                (conversations != null ? conversations.size() : 0) + " conversations");

        recordBenchmark("optimized_conversation_loading", timeTaken);
        return timeTaken;
    }

    /**
     * Benchmarks loading messages using the original MessageService.
     *
     * @param threadId The thread ID
     * @return The time taken in milliseconds
     */
    public long benchmarkOriginalMessageLoading(String threadId) {
        TranslationManager translationManager = new TranslationManager(context, translationService, userPreferences);
        MessageService messageService = new MessageService(context, translationManager);

        long startTime = SystemClock.elapsedRealtime();
        List<Message> messages = messageService.getMessagesByThreadId(threadId);
        long endTime = SystemClock.elapsedRealtime();

        long timeTaken = endTime - startTime;
        Log.d(TAG, "Original message loading took " + timeTaken + " ms for " +
                (messages != null ? messages.size() : 0) + " messages");

        recordBenchmark("original_message_loading", timeTaken);
        return timeTaken;
    }

    /**
     * Benchmarks loading messages using the optimized MessageService with pagination.
     *
     * @param threadId The thread ID
     * @param pageSize The page size
     * @return The time taken in milliseconds
     */
    public long benchmarkOptimizedMessageLoading(String threadId, int pageSize) {
        TranslationManager translationManager = new TranslationManager(context, translationService, userPreferences);
        OptimizedMessageService messageService = new OptimizedMessageService(context, translationManager);
        final CountDownLatch latch = new CountDownLatch(1);
        final List<Message> loadedMessages = new ArrayList<>();

        long startTime = SystemClock.elapsedRealtime();

        messageService.getMessagesByThreadIdPaginated(threadId, 0, pageSize, messages -> {
            loadedMessages.addAll(messages);
            latch.countDown();
        });

        try {
            // Wait for the async operation to complete
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Benchmark interrupted", e);
        }

        long endTime = SystemClock.elapsedRealtime();

        long timeTaken = endTime - startTime;
        Log.d(TAG, "Optimized message loading took " + timeTaken + " ms for " +
                loadedMessages.size() + " messages (page size: " + pageSize + ")");

        recordBenchmark("optimized_message_loading", timeTaken);
        return timeTaken;
    }

    /**
     * Benchmarks contact lookup using the original ContactUtils.
     *
     * @param phoneNumbers The phone numbers to look up
     * @return The time taken in milliseconds
     */
    public long benchmarkOriginalContactLookup(List<String> phoneNumbers) {
        long startTime = SystemClock.elapsedRealtime();

        for (String phoneNumber : phoneNumbers) {
            ContactUtils.getContactName(context, phoneNumber);
        }

        long endTime = SystemClock.elapsedRealtime();

        long timeTaken = endTime - startTime;
        Log.d(TAG, "Original contact lookup took " + timeTaken + " ms for " +
                phoneNumbers.size() + " phone numbers");

        recordBenchmark("original_contact_lookup", timeTaken);
        return timeTaken;
    }

    /**
     * Benchmarks contact lookup using the optimized ContactUtils.
     *
     * @param phoneNumbers The phone numbers to look up
     * @return The time taken in milliseconds
     */
    public long benchmarkOptimizedContactLookup(List<String> phoneNumbers) {
        long startTime = SystemClock.elapsedRealtime();

        OptimizedContactUtils.getContactNamesForNumbers(context, phoneNumbers);

        long endTime = SystemClock.elapsedRealtime();

        long timeTaken = endTime - startTime;
        Log.d(TAG, "Optimized contact lookup took " + timeTaken + " ms for " +
                phoneNumbers.size() + " phone numbers");

        recordBenchmark("optimized_contact_lookup", timeTaken);
        return timeTaken;
    }

    /**
     * Records a benchmark result.
     *
     * @param name The benchmark name
     * @param timeTaken The time taken in milliseconds
     */
    private void recordBenchmark(String name, long timeTaken) {
        List<Long> results = benchmarkResults.computeIfAbsent(name, k -> new ArrayList<>());
        results.add(timeTaken);
    }

    /**
     * Gets the average time for a benchmark.
     *
     * @param name The benchmark name
     * @return The average time in milliseconds, or -1 if no results
     */
    public double getAverageTime(String name) {
        List<Long> results = benchmarkResults.get(name);
        if (results == null || results.isEmpty()) {
            return -1;
        }

        long sum = 0;
        for (Long result : results) {
            sum += result;
        }

        return (double) sum / results.size();
    }

    /**
     * Gets the improvement percentage between original and optimized versions.
     *
     * @param originalName The original benchmark name
     * @param optimizedName The optimized benchmark name
     * @return The improvement percentage, or -1 if no results
     */
    public double getImprovementPercentage(String originalName, String optimizedName) {
        double originalAvg = getAverageTime(originalName);
        double optimizedAvg = getAverageTime(optimizedName);

        if (originalAvg <= 0 || optimizedAvg <= 0) {
            return -1;
        }

        return ((originalAvg - optimizedAvg) / originalAvg) * 100;
    }

    /**
     * Gets a report of all benchmark results.
     *
     * @return The benchmark report
     */
    public String getReport() {
        StringBuilder report = new StringBuilder();
        report.append("Performance Benchmark Report");
        report.append("===========================");

        // Report individual benchmarks
        for (Map.Entry<String, List<Long>> entry : benchmarkResults.entrySet()) {
            String name = entry.getKey();
            List<Long> results = entry.getValue();

            report.append(name).append(":");
            report.append("  Runs: ").append(results.size()).append("");
            report.append("  Average: ").append(String.format("%.2f", getAverageTime(name))).append(" ms");

            // Calculate min and max
            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;
            for (Long result : results) {
                min = Math.min(min, result);
                max = Math.max(max, result);
            }

            report.append("  Min: ").append(min).append(" ms");
            report.append("  Max: ").append(max).append(" ms");
        }

        // Report improvements
        report.append("Improvements");
        report.append("===========");

        double conversationImprovement = getImprovementPercentage(
                "original_conversation_loading", "optimized_conversation_loading");
        if (conversationImprovement >= 0) {
            report.append("Conversation Loading: ")
                    .append(String.format("%.2f", conversationImprovement))
                    .append("% faster");
        }

        double messageImprovement = getImprovementPercentage(
                "original_message_loading", "optimized_message_loading");
        if (messageImprovement >= 0) {
            report.append("Message Loading: ")
                    .append(String.format("%.2f", messageImprovement))
                    .append("% faster");
        }

        double contactImprovement = getImprovementPercentage(
                "original_contact_lookup", "optimized_contact_lookup");
        if (contactImprovement >= 0) {
            report.append("Contact Lookup: ")
                    .append(String.format("%.2f", contactImprovement))
                    .append("% faster");
        }

        return report.toString();
    }

    /**
     * Runs all benchmarks.
     *
     * @param threadId The thread ID to use for message loading benchmarks
     * @param phoneNumbers The phone numbers to use for contact lookup benchmarks
     * @param iterations The number of iterations to run each benchmark
     * @return The benchmark report
     */
    public String runAllBenchmarks(String threadId, List<String> phoneNumbers, int iterations) {
        for (int i = 0; i < iterations; i++) {
            Log.d(TAG, "Running benchmark iteration " + (i + 1) + " of " + iterations);

            benchmarkOriginalConversationLoading();
            benchmarkOptimizedConversationLoading();

            if (threadId != null && !threadId.isEmpty()) {
                benchmarkOriginalMessageLoading(threadId);
                benchmarkOptimizedMessageLoading(threadId, 50);
            }

            if (phoneNumbers != null && !phoneNumbers.isEmpty()) {
                benchmarkOriginalContactLookup(phoneNumbers);
                benchmarkOptimizedContactLookup(phoneNumbers);
            }
        }

        return getReport();
    }
}

