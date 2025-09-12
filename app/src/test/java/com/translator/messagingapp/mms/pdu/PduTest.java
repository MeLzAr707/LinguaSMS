package com.translator.messagingapp.mms.pdu;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for MMS PDU classes.
 */
public class PduTest {

    @Before
    public void setUp() {
        // Setup for tests
    }

    @Test
    public void testPduHeaders() {
        // Test header constants
        assertEquals(0x80, PduHeaders.MESSAGE_TYPE_SEND_REQ);
        assertEquals(0x81, PduHeaders.MESSAGE_TYPE_SEND_CONF);
        assertEquals(0x82, PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND);
        
        // Test header names
        assertEquals("Message-Type", PduHeaders.getHeaderName(PduHeaders.MESSAGE_TYPE));
        assertEquals("Content-Type", PduHeaders.getHeaderName(PduHeaders.CONTENT_TYPE));
        assertEquals("From", PduHeaders.getHeaderName(PduHeaders.FROM));
        
        // Test message type names
        assertEquals("Send-Request", PduHeaders.getMessageTypeName(PduHeaders.MESSAGE_TYPE_SEND_REQ));
        assertEquals("Send-Confirmation", PduHeaders.getMessageTypeName(PduHeaders.MESSAGE_TYPE_SEND_CONF));
        
        // Test response status names
        assertEquals("OK", PduHeaders.getResponseStatusName(PduHeaders.RESPONSE_STATUS_OK));
        assertEquals("Error-Network-Problem", PduHeaders.getResponseStatusName(PduHeaders.RESPONSE_STATUS_ERROR_NETWORK_PROBLEM));
    }

    @Test
    public void testEncodedStringValue() {
        String testString = "Hello, World!";
        EncodedStringValue encodedValue = new EncodedStringValue(testString);
        
        assertEquals(testString, encodedValue.getString());
        assertEquals(EncodedStringValue.CHARSET_UTF8, encodedValue.getCharacterSet());
        assertArrayEquals(testString.getBytes(), encodedValue.getTextString());
        
        // Test toString
        assertEquals(testString, encodedValue.toString());
    }

    @Test
    public void testEncodedStringValueEquality() {
        String testString = "Test String";
        EncodedStringValue value1 = new EncodedStringValue(testString);
        EncodedStringValue value2 = new EncodedStringValue(testString);
        EncodedStringValue value3 = new EncodedStringValue("Different String");
        
        assertEquals(value1, value2);
        assertNotEquals(value1, value3);
        assertEquals(value1.hashCode(), value2.hashCode());
        assertNotEquals(value1.hashCode(), value3.hashCode());
    }

    @Test
    public void testSendReq() {
        SendReq sendReq = new SendReq();
        
        assertEquals(PduHeaders.MESSAGE_TYPE_SEND_REQ, sendReq.getMessageType());
        assertEquals(PduHeaders.PRIORITY_NORMAL, sendReq.getPriority());
        assertEquals(PduHeaders.VALUE_NO, sendReq.getDeliveryReport());
        assertEquals(PduHeaders.VALUE_NO, sendReq.getReadReport());
        
        // Test setting values
        byte[] transactionId = "T123".getBytes();
        sendReq.setTransactionId(transactionId);
        assertArrayEquals(transactionId, sendReq.getTransactionId());
        
        EncodedStringValue from = new EncodedStringValue("+1234567890");
        sendReq.setFrom(from);
        assertEquals(from, sendReq.getFrom());
        
        EncodedStringValue subject = new EncodedStringValue("Test Subject");
        sendReq.setSubject(subject);
        assertEquals(subject, sendReq.getSubject());
        
        long testDate = System.currentTimeMillis() / 1000L;
        sendReq.setDate(testDate);
        assertEquals(testDate, sendReq.getDate());
        
        sendReq.setPriority(PduHeaders.PRIORITY_HIGH);
        assertEquals(PduHeaders.PRIORITY_HIGH, sendReq.getPriority());
        
        sendReq.setDeliveryReport(PduHeaders.VALUE_YES);
        assertEquals(PduHeaders.VALUE_YES, sendReq.getDeliveryReport());
    }

    @Test
    public void testSendConf() {
        SendConf sendConf = new SendConf();
        
        assertEquals(PduHeaders.MESSAGE_TYPE_SEND_CONF, sendConf.getMessageType());
        
        // Test setting values
        byte[] transactionId = "T123".getBytes();
        sendConf.setTransactionId(transactionId);
        assertArrayEquals(transactionId, sendConf.getTransactionId());
        
        byte[] messageId = "MSG123".getBytes();
        sendConf.setMessageId(messageId);
        assertArrayEquals(messageId, sendConf.getMessageId());
        
        sendConf.setResponseStatus(PduHeaders.RESPONSE_STATUS_OK);
        assertEquals(PduHeaders.RESPONSE_STATUS_OK, sendConf.getResponseStatus());
        assertTrue(sendConf.isSuccessful());
        
        sendConf.setResponseStatus(PduHeaders.RESPONSE_STATUS_ERROR_NETWORK_PROBLEM);
        assertEquals(PduHeaders.RESPONSE_STATUS_ERROR_NETWORK_PROBLEM, sendConf.getResponseStatus());
        assertFalse(sendConf.isSuccessful());
        
        EncodedStringValue responseText = new EncodedStringValue("Success");
        sendConf.setResponseText(responseText);
        assertEquals(responseText, sendConf.getResponseText());
    }

    @Test
    public void testNotificationInd() {
        NotificationInd notificationInd = new NotificationInd();
        
        assertEquals(PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND, notificationInd.getMessageType());
        
        // Test setting values
        byte[] transactionId = "T456".getBytes();
        notificationInd.setTransactionId(transactionId);
        assertArrayEquals(transactionId, notificationInd.getTransactionId());
        
        byte[] contentLocation = "http://example.com/mms".getBytes();
        notificationInd.setContentLocation(contentLocation);
        assertArrayEquals(contentLocation, notificationInd.getContentLocation());
        
        EncodedStringValue from = new EncodedStringValue("+1234567890");
        notificationInd.setFrom(from);
        assertEquals(from, notificationInd.getFrom());
        
        EncodedStringValue subject = new EncodedStringValue("MMS Subject");
        notificationInd.setSubject(subject);
        assertEquals(subject, notificationInd.getSubject());
        
        long expiry = System.currentTimeMillis() + 86400000L; // 1 day
        notificationInd.setExpiry(expiry);
        assertEquals(expiry, notificationInd.getExpiry());
        
        long messageSize = 12345L;
        notificationInd.setMessageSize(messageSize);
        assertEquals(messageSize, notificationInd.getMessageSize());
    }

    @Test
    public void testPduPart() {
        PduPart part = new PduPart();
        
        // Test content type
        part.setContentType("image/jpeg");
        assertEquals("image/jpeg", part.getContentTypeString());
        assertTrue(part.isImage());
        assertFalse(part.isText());
        assertFalse(part.isVideo());
        assertFalse(part.isAudio());
        
        part.setContentType("text/plain");
        assertEquals("text/plain", part.getContentTypeString());
        assertTrue(part.isText());
        assertFalse(part.isImage());
        
        part.setContentType("video/mp4");
        assertEquals("video/mp4", part.getContentTypeString());
        assertTrue(part.isVideo());
        assertFalse(part.isImage());
        
        part.setContentType("audio/mp3");
        assertEquals("audio/mp3", part.getContentTypeString());
        assertTrue(part.isAudio());
        assertFalse(part.isVideo());
        
        // Test other properties
        part.setName("test_part");
        assertEquals("test_part", part.getName());
        
        part.setFilename("test.jpg");
        assertEquals("test.jpg", part.getFilename());
        
        part.setCharset(106); // UTF-8
        assertEquals(106, part.getCharset());
        
        byte[] testData = "test data".getBytes();
        part.setData(testData);
        assertArrayEquals(testData, part.getData());
    }

    @Test
    public void testPduBody() {
        PduBody body = new PduBody();
        
        assertTrue(body.isEmpty());
        assertEquals(0, body.getPartsNum());
        
        // Add parts
        PduPart part1 = new PduPart();
        part1.setContentType("text/plain");
        part1.setName("text_part");
        
        PduPart part2 = new PduPart();
        part2.setContentType("image/jpeg");
        part2.setName("image_part");
        
        body.addPart(part1);
        body.addPart(part2);
        
        assertFalse(body.isEmpty());
        assertEquals(2, body.getPartsNum());
        
        // Test getting parts
        assertEquals(part1, body.getPart(0));
        assertEquals(part2, body.getPart(1));
        assertNull(body.getPart(2)); // Out of bounds
        assertNull(body.getPart(-1)); // Negative index
        
        // Test getting all parts
        assertEquals(2, body.getParts().size());
        assertTrue(body.getParts().contains(part1));
        assertTrue(body.getParts().contains(part2));
        
        // Test removing parts
        PduPart removed = body.removePart(0);
        assertEquals(part1, removed);
        assertEquals(1, body.getPartsNum());
        
        assertNull(body.removePart(5)); // Out of bounds
        
        // Test clear
        body.clear();
        assertTrue(body.isEmpty());
        assertEquals(0, body.getPartsNum());
    }

    @Test
    public void testGenericPduToString() {
        SendReq sendReq = new SendReq();
        String toString = sendReq.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("SendReq"));
        assertTrue(toString.contains("Send-Request"));
    }
}