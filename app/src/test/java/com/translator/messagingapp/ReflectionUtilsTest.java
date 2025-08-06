package com.translator.messagingapp;

import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.reflect.Method;

/**
 * Unit tests for ReflectionUtils to ensure safe handling of reflection operations.
 */
public class ReflectionUtilsTest {

    @Test
    public void testSafeInvokeWithNullMethod() {
        // Test that null method doesn't cause NPE
        Object result = ReflectionUtils.safeInvoke(null, new Object());
        assertNull("safeInvoke should return null for null method", result);
    }

    @Test
    public void testSafeInvokeWithValidMethod() {
        try {
            // Test with a valid method that should work
            Method toStringMethod = Object.class.getMethod("toString");
            Object testObject = "test";
            Object result = ReflectionUtils.safeInvoke(toStringMethod, testObject);
            assertNotNull("safeInvoke should return result for valid method", result);
            assertEquals("Result should be string representation", "test", result);
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        }
    }

    @Test
    public void testSafeGetMethodWithNullClass() {
        Method result = ReflectionUtils.safeGetMethod(null, "toString");
        assertNull("safeGetMethod should return null for null class", result);
    }

    @Test
    public void testSafeGetMethodWithNullMethodName() {
        Method result = ReflectionUtils.safeGetMethod(Object.class, null);
        assertNull("safeGetMethod should return null for null method name", result);
    }

    @Test
    public void testSafeGetMethodWithValidInput() {
        Method result = ReflectionUtils.safeGetMethod(Object.class, "toString");
        assertNotNull("safeGetMethod should return method for valid input", result);
        assertEquals("Method name should match", "toString", result.getName());
    }

    @Test
    public void testSafeGetMethodWithNonExistentMethod() {
        Method result = ReflectionUtils.safeGetMethod(Object.class, "nonExistentMethod");
        assertNull("safeGetMethod should return null for non-existent method", result);
    }

    @Test
    public void testSafeGetClassWithNullName() {
        Class<?> result = ReflectionUtils.safeGetClass(null);
        assertNull("safeGetClass should return null for null class name", result);
    }

    @Test
    public void testSafeGetClassWithValidName() {
        Class<?> result = ReflectionUtils.safeGetClass("java.lang.String");
        assertNotNull("safeGetClass should return class for valid name", result);
        assertEquals("Class should be String", String.class, result);
    }

    @Test
    public void testSafeGetClassWithNonExistentClass() {
        Class<?> result = ReflectionUtils.safeGetClass("com.nonexistent.Class");
        assertNull("safeGetClass should return null for non-existent class", result);
    }

    @Test
    public void testTryGcControlDoesNotThrow() {
        // Test that GC control methods don't throw exceptions
        try {
            boolean result1 = ReflectionUtils.tryGcControl(true);
            boolean result2 = ReflectionUtils.tryGcControl(false);
            // Results can be true or false, but should not throw
            assertTrue("Test completed without exception", true);
        } catch (Exception e) {
            fail("tryGcControl should not throw exceptions: " + e.getMessage());
        }
    }

    @Test
    public void testReflectionOperationsAreRobust() {
        // Test various edge cases to ensure robustness
        assertNull("Null method safe", ReflectionUtils.safeInvoke(null, null));
        assertNull("Null class safe", ReflectionUtils.safeGetMethod(null, "test"));
        assertNull("Null declared method safe", ReflectionUtils.safeGetDeclaredMethod(null, "test"));
        assertNull("Null class name safe", ReflectionUtils.safeGetClass(null));
    }
}