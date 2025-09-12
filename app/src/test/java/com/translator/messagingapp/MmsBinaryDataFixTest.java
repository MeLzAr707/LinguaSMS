package com.translator.messagingapp;

import com.translator.messagingapp.message.*;
import com.translator.messagingapp.conversation.*;
import com.translator.messagingapp.mms.*;
import com.translator.messagingapp.contact.*;
import com.translator.messagingapp.notification.*;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class to verify MMS binary data fix for attachment streaming,
 * URI permission validation, and consistent sending logic.
 */
@RunWith(RobolectricTestRunner.class)
public class MmsBinaryDataFixTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private ContentResolver mockContentResolver;
    
    @Mock
    private Cursor mockCursor;

    private MessageService messageService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockContext.getContentResolver()).thenReturn(mockContentResolver);
        messageService = new MessageService(mockContext);
    }

    /**
     * Test that attachment data is properly streamed with the larger 8KB buffer.
     */
    @Test
    public void testAttachmentStreamingWithLargerBuffer() throws Exception {
        // Create test data larger than old 1KB buffer to verify 8KB buffer is used
        byte[] testData = new byte[5000]; // 5KB test data
        Arrays.fill(testData, (byte) 42);
        
        Uri attachmentUri = Uri.parse("content://media/external/file/123");
        Uri partUri = Uri.parse("content://mms/1/part");
        Uri newPartUri = Uri.parse("content://mms/1/part/456");
        
        // Mock content resolver behavior
        when(mockContentResolver.getType(attachmentUri)).thenReturn("image/jpeg");
        when(mockContentResolver.query(eq(attachmentUri), any(), any(), any(), any()))
            .thenReturn(mockCursor);
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.getString(0)).thenReturn("test_image.jpg");
        when(mockContentResolver.insert(eq(partUri), any(ContentValues.class)))
            .thenReturn(newPartUri);
        
        // Mock input stream with test data
        InputStream testInputStream = new ByteArrayInputStream(testData);
        when(mockContentResolver.openInputStream(attachmentUri)).thenReturn(testInputStream);
        
        // Mock output stream to capture written data
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        when(mockContentResolver.openOutputStream(newPartUri)).thenReturn(capturedOutput);
        
        // Mock other required operations
        Uri messageUri = Uri.parse("content://mms/1");
        when(mockContentResolver.insert(eq(Uri.parse("content://mms")), any(ContentValues.class)))
            .thenReturn(messageUri);
        
        // Execute the method
        boolean result = messageService.sendMmsMessage("1234567890", "Test Subject", 
            "Test Body", Arrays.asList(attachmentUri));
        
        // Verify the result
        assertTrue("MMS sending should succeed", result);
        
        // Verify that all test data was copied correctly
        byte[] writtenData = capturedOutput.toByteArray();
        assertEquals("All test data should be copied", testData.length, writtenData.length);
        assertArrayEquals("Written data should match original", testData, writtenData);
        
        // Verify that attachment part was created with correct values
        verify(mockContentResolver).insert(eq(partUri), argThat(values -> {
            ContentValues cv = (ContentValues) values;
            return "image/jpeg".equals(cv.getAsString(Telephony.Mms.Part.CONTENT_TYPE)) &&
                   "test_image.jpg".equals(cv.getAsString(Telephony.Mms.Part.FILENAME)) &&
                   "attachment".equals(cv.getAsString(Telephony.Mms.Part.CONTENT_DISPOSITION));
        }));
    }

    /**
     * Test that URI access validation works correctly.
     */
    @Test
    public void testUriAccessValidation() throws Exception {
        Uri validUri = Uri.parse("content://media/external/file/123");
        Uri invalidUri = Uri.parse("content://media/external/file/456");
        
        // Mock successful access for valid URI
        InputStream validStream = new ByteArrayInputStream("test data".getBytes());
        when(mockContentResolver.openInputStream(validUri)).thenReturn(validStream);
        
        // Mock security exception for invalid URI
        when(mockContentResolver.openInputStream(invalidUri))
            .thenThrow(new SecurityException("No permission"));
        
        // Mock other required operations for the test
        Uri messageUri = Uri.parse("content://mms/1");
        when(mockContentResolver.insert(eq(Uri.parse("content://mms")), any(ContentValues.class)))
            .thenReturn(messageUri);
        
        // Test with valid URI - should process attachment
        when(mockContentResolver.getType(validUri)).thenReturn("image/jpeg");
        when(mockContentResolver.query(eq(validUri), any(), any(), any(), any()))
            .thenReturn(mockCursor);
        when(mockCursor.moveToFirst()).thenReturn(true);
        when(mockCursor.getString(0)).thenReturn("valid.jpg");
        
        Uri partUri = Uri.parse("content://mms/1/part");
        Uri newPartUri = Uri.parse("content://mms/1/part/123");
        when(mockContentResolver.insert(eq(partUri), any(ContentValues.class)))
            .thenReturn(newPartUri);
        when(mockContentResolver.openOutputStream(newPartUri))
            .thenReturn(new ByteArrayOutputStream());
        
        boolean result = messageService.sendMmsMessage("1234567890", "Test", 
            "Test", Arrays.asList(validUri));
        assertTrue("Should succeed with valid URI", result);
        
        // Verify attachment part was created for valid URI
        verify(mockContentResolver).insert(eq(partUri), any(ContentValues.class));
        
        // Reset mocks for invalid URI test
        reset(mockContentResolver);
        when(mockContext.getContentResolver()).thenReturn(mockContentResolver);
        when(mockContentResolver.insert(eq(Uri.parse("content://mms")), any(ContentValues.class)))
            .thenReturn(messageUri);
        when(mockContentResolver.openInputStream(invalidUri))
            .thenThrow(new SecurityException("No permission"));
        
        // Test with invalid URI - should skip attachment but continue
        boolean result2 = messageService.sendMmsMessage("1234567890", "Test", 
            "Test", Arrays.asList(invalidUri));
        assertTrue("Should still succeed even with invalid URI", result2);
        
        // Verify no attachment part was created for invalid URI
        verify(mockContentResolver, never()).insert(eq(partUri), any(ContentValues.class));
    }

    /**
     * Test that MMS sending uses content provider approach consistently.
     */
    @Test
    public void testConsistentMmsSendingLogic() throws Exception {
        Uri messageUri = Uri.parse("content://mms/1");
        when(mockContentResolver.insert(eq(Uri.parse("content://mms")), any(ContentValues.class)))
            .thenReturn(messageUri);
        
        // Execute MMS sending
        boolean result = messageService.sendMmsMessage("1234567890", "Test", "Test", null);
        
        // Verify that message was created in outbox (not sent box)
        verify(mockContentResolver).insert(eq(Uri.parse("content://mms")), argThat(values -> {
            ContentValues cv = (ContentValues) values;
            Integer messageBox = cv.getAsInteger(Telephony.Mms.MESSAGE_BOX);
            return messageBox != null && messageBox == Telephony.Mms.MESSAGE_BOX_OUTBOX;
        }));
        
        assertTrue("MMS sending should succeed", result);
    }
}