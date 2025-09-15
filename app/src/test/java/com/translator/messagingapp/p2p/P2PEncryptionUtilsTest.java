package com.translator.messagingapp.p2p;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Unit tests for P2P encryption utilities.
 */
public class P2PEncryptionUtilsTest {

    @Test
    public void testValidP2PTriggerDetection() {
        // Valid P2P trigger format
        String validTrigger = "P2P_CONNECT#USER:dGVzdGVuY3J5cHRlZGRhdGE=";
        assertTrue("Should detect valid P2P trigger", P2PEncryptionUtils.isValidP2PTrigger(validTrigger));
    }

    @Test
    public void testInvalidP2PTriggerDetection() {
        // Test various invalid formats
        assertFalse("Null message should be invalid", P2PEncryptionUtils.isValidP2PTrigger(null));
        assertFalse("Empty message should be invalid", P2PEncryptionUtils.isValidP2PTrigger(""));
        assertFalse("Regular SMS should be invalid", P2PEncryptionUtils.isValidP2PTrigger("Hello world"));
        assertFalse("Missing USER prefix should be invalid", P2PEncryptionUtils.isValidP2PTrigger("P2P_CONNECT#dGVzdA=="));
        assertFalse("Missing payload should be invalid", P2PEncryptionUtils.isValidP2PTrigger("P2P_CONNECT#USER:"));
        assertFalse("Invalid Base64 should be invalid", P2PEncryptionUtils.isValidP2PTrigger("P2P_CONNECT#USER:invalid_base64!"));
    }

    @Test
    public void testTriggerWithWhitespace() {
        // Valid trigger with whitespace
        String triggerWithWhitespace = "  P2P_CONNECT#USER:dGVzdGVuY3J5cHRlZGRhdGE=  ";
        assertTrue("Should handle whitespace in trigger", P2PEncryptionUtils.isValidP2PTrigger(triggerWithWhitespace));
    }

    @Test
    public void testExtractEncryptedPayload() {
        String validTrigger = "P2P_CONNECT#USER:dGVzdGVuY3J5cHRlZGRhdGE=";
        String expectedPayload = "dGVzdGVuY3J5cHRlZGRhdGE=";
        
        String extractedPayload = P2PEncryptionUtils.extractEncryptedPayload(validTrigger);
        assertEquals("Should extract correct payload", expectedPayload, extractedPayload);
    }

    @Test
    public void testExtractPayloadFromInvalidTrigger() {
        String invalidTrigger = "Hello world";
        assertNull("Should return null for invalid trigger", P2PEncryptionUtils.extractEncryptedPayload(invalidTrigger));
    }

    @Test
    public void testTriggerCaseInsensitivity() {
        // P2P trigger should be case sensitive (uppercase required)
        String lowerCaseTrigger = "p2p_connect#USER:dGVzdA==";
        assertFalse("Should be case sensitive", P2PEncryptionUtils.isValidP2PTrigger(lowerCaseTrigger));
    }

    @Test
    public void testLongPayload() {
        // Test with a long Base64 payload
        String longPayload = "dGVzdGVuY3J5cHRlZGRhdGF0ZXN0ZW5jcnlwdGVkZGF0YXRlc3RlbmNyeXB0ZWRkYXRhdGVzdGVuY3J5cHRlZGRhdGE=";
        String longTrigger = "P2P_CONNECT#USER:" + longPayload;
        
        assertTrue("Should handle long payloads", P2PEncryptionUtils.isValidP2PTrigger(longTrigger));
        assertEquals("Should extract long payload correctly", longPayload, P2PEncryptionUtils.extractEncryptedPayload(longTrigger));
    }

    @Test
    public void testMultipleHashSymbols() {
        // Test trigger with multiple # symbols in payload
        String triggerWithMultipleHashes = "P2P_CONNECT#USER:dGVzdA==#extra#data";
        // This should be invalid because the payload contains # which breaks Base64
        assertFalse("Should reject payload with invalid characters", P2PEncryptionUtils.isValidP2PTrigger(triggerWithMultipleHashes));
    }

    @Test
    public void testEmptyUserPrefix() {
        String triggerEmptyUser = "P2P_CONNECT#USER:";
        assertFalse("Should reject empty payload after USER:", P2PEncryptionUtils.isValidP2PTrigger(triggerEmptyUser));
    }

    @Test
    public void testTriggerAtMessageStart() {
        // Trigger should work when it's at the beginning of the message
        String messageStartTrigger = "P2P_CONNECT#USER:dGVzdA== with additional text";
        assertTrue("Should detect trigger at message start", P2PEncryptionUtils.isValidP2PTrigger(messageStartTrigger));
    }
}