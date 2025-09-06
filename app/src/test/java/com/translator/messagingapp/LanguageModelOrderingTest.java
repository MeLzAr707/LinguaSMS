package com.translator.messagingapp;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Test to verify that language models are ordered by usage frequency.
 * Tests that the most common languages appear first in the list.
 */
@RunWith(AndroidJUnit4.class)
public class LanguageModelOrderingTest {

    private OfflineModelManager modelManager;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        modelManager = new OfflineModelManager(context);
    }

    @Test
    public void testLanguageModelsOrderedByFrequency() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final List<OfflineModelManager.OfflineLanguageModel>[] modelsHolder = new List[1];
        final String[] errorHolder = new String[1];

        modelManager.getAvailableModels(new OfflineModelManager.ModelStatusListener() {
            @Override
            public void onStatusUpdated(List<OfflineModelManager.OfflineLanguageModel> models) {
                modelsHolder[0] = models;
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                errorHolder[0] = errorMessage;
                latch.countDown();
            }
        });

        assertTrue("Should get models within timeout", latch.await(5, TimeUnit.SECONDS));
        assertNull("Should not have error: " + errorHolder[0], errorHolder[0]);
        assertNotNull("Should have models list", modelsHolder[0]);

        List<OfflineModelManager.OfflineLanguageModel> models = modelsHolder[0];
        assertTrue("Should have at least 10 models", models.size() >= 10);

        // Verify the top tier languages are in correct order at the beginning
        String[] expectedTopLanguages = {"en", "zh", "hi", "es", "ar"};
        
        for (int i = 0; i < expectedTopLanguages.length; i++) {
            assertTrue("Should have at least " + (i + 1) + " models", models.size() > i);
            assertEquals("Language at position " + i + " should be " + expectedTopLanguages[i],
                    expectedTopLanguages[i], models.get(i).getLanguageCode());
        }

        // Verify English is first (most common)
        assertEquals("English should be first", "en", models.get(0).getLanguageCode());
        assertEquals("English display name should be correct", "English", models.get(0).getDisplayName());

        // Verify Chinese is second
        assertEquals("Chinese should be second", "zh", models.get(1).getLanguageCode());
        assertEquals("Chinese display name should be correct", "Chinese", models.get(1).getDisplayName());

        // Verify Hindi is third
        assertEquals("Hindi should be third", "hi", models.get(2).getLanguageCode());
        assertEquals("Hindi display name should be correct", "Hindi", models.get(2).getDisplayName());

        // Verify Spanish is fourth
        assertEquals("Spanish should be fourth", "es", models.get(3).getLanguageCode());
        assertEquals("Spanish display name should be correct", "Spanish", models.get(3).getDisplayName());

        // Verify Arabic is fifth
        assertEquals("Arabic should be fifth", "ar", models.get(4).getLanguageCode());
        assertEquals("Arabic display name should be correct", "Arabic", models.get(4).getDisplayName());
    }

    @Test
    public void testMajorLanguagesInTopTen() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final List<OfflineModelManager.OfflineLanguageModel>[] modelsHolder = new List[1];

        modelManager.getAvailableModels(new OfflineModelManager.ModelStatusListener() {
            @Override
            public void onStatusUpdated(List<OfflineModelManager.OfflineLanguageModel> models) {
                modelsHolder[0] = models;
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                latch.countDown();
            }
        });

        assertTrue("Should get models within timeout", latch.await(5, TimeUnit.SECONDS));
        List<OfflineModelManager.OfflineLanguageModel> models = modelsHolder[0];
        assertNotNull("Should have models list", models);

        // Get the top 10 languages
        String[] topTenCodes = new String[Math.min(10, models.size())];
        for (int i = 0; i < topTenCodes.length; i++) {
            topTenCodes[i] = models.get(i).getLanguageCode();
        }

        // Verify major languages are in top 10
        String[] majorLanguages = {"en", "zh", "hi", "es", "ar", "pt", "fr", "ru", "ja", "de"};
        
        for (String majorLang : majorLanguages) {
            boolean found = false;
            for (String topLang : topTenCodes) {
                if (majorLang.equals(topLang)) {
                    found = true;
                    break;
                }
            }
            assertTrue("Major language " + majorLang + " should be in top 10", found);
        }
    }

    @Test
    public void testLanguageModelOrderConsistency() throws InterruptedException {
        // Test that the order is consistent across multiple calls
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        
        final List<OfflineModelManager.OfflineLanguageModel>[] firstCallModels = new List[1];
        final List<OfflineModelManager.OfflineLanguageModel>[] secondCallModels = new List[1];

        // First call
        modelManager.getAvailableModels(new OfflineModelManager.ModelStatusListener() {
            @Override
            public void onStatusUpdated(List<OfflineModelManager.OfflineLanguageModel> models) {
                firstCallModels[0] = models;
                latch1.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                latch1.countDown();
            }
        });

        assertTrue("First call should complete", latch1.await(5, TimeUnit.SECONDS));

        // Second call
        modelManager.getAvailableModels(new OfflineModelManager.ModelStatusListener() {
            @Override
            public void onStatusUpdated(List<OfflineModelManager.OfflineLanguageModel> models) {
                secondCallModels[0] = models;
                latch2.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                latch2.countDown();
            }
        });

        assertTrue("Second call should complete", latch2.await(5, TimeUnit.SECONDS));

        assertNotNull("First call should return models", firstCallModels[0]);
        assertNotNull("Second call should return models", secondCallModels[0]);
        assertEquals("Both calls should return same number of models", 
                firstCallModels[0].size(), secondCallModels[0].size());

        // Verify order is consistent
        for (int i = 0; i < Math.min(firstCallModels[0].size(), secondCallModels[0].size()); i++) {
            assertEquals("Language at position " + i + " should be consistent",
                    firstCallModels[0].get(i).getLanguageCode(),
                    secondCallModels[0].get(i).getLanguageCode());
        }
    }
}