package com.translator.messagingapp;

import com.translator.messagingapp.util.SecretMessageUtils;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for SecretMessageUtils
 */
public class SecretMessageUtilsTest {

    @Test
    public void testBasicEncodeAndDecode() {
        String visibleMessage = "Hello World";
        String secretMessage = "Secret!";
        
        String encoded = SecretMessageUtils.encodeSecretMessage(visibleMessage, secretMessage);
        String decoded = SecretMessageUtils.decodeSecretMessage(encoded);
        
        assertNotNull(encoded);
        assertNotEquals(visibleMessage, encoded); // Should be different
        assertTrue(encoded.contains(visibleMessage)); // Should contain visible message
        assertEquals(secretMessage, decoded); // Should decode correctly
    }

    @Test
    public void testHasSecretMessage() {
        String visibleMessage = "Hello World";
        String secretMessage = "Secret!";
        
        String encoded = SecretMessageUtils.encodeSecretMessage(visibleMessage, secretMessage);
        String normal = "Just a normal message";
        
        assertTrue(SecretMessageUtils.hasSecretMessage(encoded));
        assertFalse(SecretMessageUtils.hasSecretMessage(normal));
        assertFalse(SecretMessageUtils.hasSecretMessage(null));
        assertFalse(SecretMessageUtils.hasSecretMessage(""));
    }

    @Test
    public void testRemoveSecretMessage() {
        String visibleMessage = "Hello World";
        String secretMessage = "Secret!";
        
        String encoded = SecretMessageUtils.encodeSecretMessage(visibleMessage, secretMessage);
        String cleaned = SecretMessageUtils.removeSecretMessage(encoded);
        
        assertEquals(visibleMessage, cleaned);
    }

    @Test
    public void testEmptySecretMessage() {
        String visibleMessage = "Hello World";
        String secretMessage = "";
        
        String encoded = SecretMessageUtils.encodeSecretMessage(visibleMessage, secretMessage);
        assertEquals(visibleMessage, encoded); // Should be unchanged
        assertNull(SecretMessageUtils.decodeSecretMessage(encoded)); // No secret to decode
    }

    @Test
    public void testNullInputs() {
        assertNull(SecretMessageUtils.encodeSecretMessage(null, "secret"));
        assertEquals("visible", SecretMessageUtils.encodeSecretMessage("visible", null));
        assertNull(SecretMessageUtils.decodeSecretMessage(null));
    }

    @Test
    public void testLongSecretMessage() {
        String visibleMessage = "Short";
        String secretMessage = "This is a much longer secret message that contains multiple words and characters!";
        
        String encoded = SecretMessageUtils.encodeSecretMessage(visibleMessage, secretMessage);
        String decoded = SecretMessageUtils.decodeSecretMessage(encoded);
        
        assertEquals(secretMessage, decoded);
    }
}