package com.translator.messagingapp;

import org.junit.Test;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

/**
 * Test to verify that duplicate SMS prompt methods have been removed from PhoneUtils
 * and that DefaultSmsAppManager is the single source of truth for SMS prompts.
 */
public class DuplicateSmsPromptFixTest {

    @Test
    public void testPhoneUtilsDoesNotHaveDuplicateCheckAndRequestMethod() {
        // Verify that PhoneUtils no longer has the checkAndRequestDefaultSmsApp method
        Method[] methods = PhoneUtils.class.getDeclaredMethods();
        for (Method method : methods) {
            assertFalse("PhoneUtils should not have checkAndRequestDefaultSmsApp method",
                    method.getName().equals("checkAndRequestDefaultSmsApp"));
        }
    }

    @Test
    public void testPhoneUtilsDoesNotHaveShowDefaultSmsExplanationDialog() {
        // Verify that PhoneUtils no longer has the showDefaultSmsExplanationDialog method
        Method[] methods = PhoneUtils.class.getDeclaredMethods();
        for (Method method : methods) {
            assertFalse("PhoneUtils should not have showDefaultSmsExplanationDialog method",
                    method.getName().equals("showDefaultSmsExplanationDialog"));
        }
    }

    @Test
    public void testPhoneUtilsDoesNotHaveHandleDefaultSmsAppResult() {
        // Verify that PhoneUtils no longer has the handleDefaultSmsAppResult method
        Method[] methods = PhoneUtils.class.getDeclaredMethods();
        for (Method method : methods) {
            assertFalse("PhoneUtils should not have handleDefaultSmsAppResult method",
                    method.getName().equals("handleDefaultSmsAppResult"));
        }
    }

    @Test
    public void testDefaultSmsAppManagerHasCheckAndRequestMethod() {
        // Verify that DefaultSmsAppManager still has the checkAndRequestDefaultSmsApp method
        Method[] methods = DefaultSmsAppManager.class.getDeclaredMethods();
        boolean hasMethod = false;
        for (Method method : methods) {
            if (method.getName().equals("checkAndRequestDefaultSmsApp")) {
                hasMethod = true;
                break;
            }
        }
        assertTrue("DefaultSmsAppManager should have checkAndRequestDefaultSmsApp method", hasMethod);
    }

    @Test
    public void testPhoneUtilsStillHasUtilityMethods() {
        // Verify that PhoneUtils still has its utility methods
        Method[] methods = PhoneUtils.class.getDeclaredMethods();
        boolean hasIsDefaultSmsApp = false;
        boolean hasRequestDefaultSmsApp = false;
        boolean hasCallPhoneNumber = false;

        for (Method method : methods) {
            if (method.getName().equals("isDefaultSmsApp")) {
                hasIsDefaultSmsApp = true;
            } else if (method.getName().equals("requestDefaultSmsApp")) {
                hasRequestDefaultSmsApp = true;
            } else if (method.getName().equals("callPhoneNumber")) {
                hasCallPhoneNumber = true;
            }
        }

        assertTrue("PhoneUtils should have isDefaultSmsApp method", hasIsDefaultSmsApp);
        assertTrue("PhoneUtils should have requestDefaultSmsApp method", hasRequestDefaultSmsApp);
        assertTrue("PhoneUtils should have callPhoneNumber method", hasCallPhoneNumber);
    }
}