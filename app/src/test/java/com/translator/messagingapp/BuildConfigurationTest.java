package com.translator.messagingapp;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Test to validate that the build configuration is correct
 * and the "Plugin with id 'com.android.application' not found" error is resolved.
 */
public class BuildConfigurationTest {
    
    private static final String PROJECT_ROOT = System.getProperty("user.dir");
    private static final String BUILD_GRADLE_PATH = PROJECT_ROOT + "/build.gradle";
    private static final String APP_BUILD_GRADLE_PATH = PROJECT_ROOT + "/app/build.gradle";
    private static final String SETTINGS_GRADLE_PATH = PROJECT_ROOT + "/settings.gradle";
    
    @Test
    public void testRootBuildGradleExists() {
        File buildGradle = new File(BUILD_GRADLE_PATH);
        assertTrue("Root build.gradle should exist", buildGradle.exists());
    }
    
    @Test
    public void testAppBuildGradleExists() {
        File appBuildGradle = new File(APP_BUILD_GRADLE_PATH);
        assertTrue("App build.gradle should exist", appBuildGradle.exists());
    }
    
    @Test
    public void testSettingsGradleExists() {
        File settingsGradle = new File(SETTINGS_GRADLE_PATH);
        assertTrue("settings.gradle should exist", settingsGradle.exists());
    }
    
    @Test
    public void testNoConflictingKotlinDslFiles() {
        File rootBuildKts = new File(PROJECT_ROOT + "/build.gradle.kts");
        File appBuildKts = new File(PROJECT_ROOT + "/app/build.gradle.kts");
        File settingsKts = new File(PROJECT_ROOT + "/settings.gradle.kts");
        
        assertFalse("Root build.gradle.kts should not exist to avoid conflicts", rootBuildKts.exists());
        assertFalse("App build.gradle.kts should not exist to avoid conflicts", appBuildKts.exists());
        assertFalse("settings.gradle.kts should not exist to avoid conflicts", settingsKts.exists());
    }
    
    @Test
    public void testAndroidGradlePluginClasspathDeclared() throws IOException {
        String buildGradleContent = new String(Files.readAllBytes(Paths.get(BUILD_GRADLE_PATH)));
        assertTrue("Root build.gradle should contain Android Gradle Plugin classpath", 
                   buildGradleContent.contains("com.android.tools.build:gradle"));
    }
    
    @Test
    public void testAndroidApplicationPluginApplied() throws IOException {
        String appBuildGradleContent = new String(Files.readAllBytes(Paths.get(APP_BUILD_GRADLE_PATH)));
        assertTrue("App build.gradle should apply Android application plugin", 
                   appBuildGradleContent.contains("apply plugin: 'com.android.application'"));
    }
    
    @Test
    public void testAppModuleIncluded() throws IOException {
        String settingsContent = new String(Files.readAllBytes(Paths.get(SETTINGS_GRADLE_PATH)));
        assertTrue("settings.gradle should include app module", 
                   settingsContent.contains("include ':app'"));
    }
    
    @Test
    public void testRepositoriesConfigured() throws IOException {
        String buildGradleContent = new String(Files.readAllBytes(Paths.get(BUILD_GRADLE_PATH)));
        assertTrue("Root build.gradle should contain google() repository", 
                   buildGradleContent.contains("google()"));
        assertTrue("Root build.gradle should contain mavenCentral() repository", 
                   buildGradleContent.contains("mavenCentral()"));
    }
}