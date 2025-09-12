package com.translator.messagingapp.mms;

import android.content.Context;
import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

/**
 * Tests for the MMS Transaction framework.
 */
@RunWith(RobolectricTestRunner.class)
public class TransactionTest {

    @Mock
    private Context mockContext;

    private Uri testUri;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        testUri = Uri.parse("content://mms/1");
    }

    @Test
    public void testTransactionStateInitialization() {
        TransactionState state = new TransactionState();
        
        assertEquals(TransactionState.INITIALIZED, state.getState());
        assertFalse(state.isProcessing());
        assertFalse(state.isSuccess());
        assertFalse(state.isFailed());
        assertFalse(state.isCancelled());
        assertNull(state.getErrorMessage());
        assertNull(state.getContentUri());
        assertTrue(state.getTimestamp() > 0);
    }

    @Test
    public void testTransactionStateTransitions() {
        TransactionState state = new TransactionState();
        
        // Test processing state
        state.setState(TransactionState.PROCESSING);
        assertTrue(state.isProcessing());
        assertEquals("PROCESSING", state.getStateString());
        
        // Test success state
        state.setState(TransactionState.SUCCESS);
        assertTrue(state.isSuccess());
        assertEquals("SUCCESS", state.getStateString());
        
        // Test failed state
        state.setState(TransactionState.FAILED);
        assertTrue(state.isFailed());
        assertEquals("FAILED", state.getStateString());
        
        // Test cancelled state
        state.setState(TransactionState.CANCELLED);
        assertTrue(state.isCancelled());
        assertEquals("CANCELLED", state.getStateString());
    }

    @Test
    public void testSendTransactionCreation() {
        long token = 12345L;
        SendTransaction transaction = new SendTransaction(mockContext, testUri, token);
        
        assertEquals(Transaction.SEND_TRANSACTION, transaction.getTransactionType());
        assertEquals(testUri, transaction.getUri());
        assertEquals(0, transaction.getRetryCount());
        assertTrue(transaction.canRetry());
        assertFalse(transaction.isCompleted());
    }

    @Test
    public void testNotificationTransactionCreation() {
        NotificationTransaction transaction = new NotificationTransaction(mockContext, testUri);
        
        assertEquals(Transaction.NOTIFICATION_TRANSACTION, transaction.getTransactionType());
        assertEquals(testUri, transaction.getUri());
        assertEquals(0, transaction.getRetryCount());
        assertTrue(transaction.canRetry());
        assertFalse(transaction.isCompleted());
    }

    @Test
    public void testTransactionRetryLogic() {
        SendTransaction transaction = new SendTransaction(mockContext, testUri, 0L);
        
        // Initially can retry
        assertTrue(transaction.canRetry());
        
        // After max retries, cannot retry
        transaction.setRetryCount(Transaction.MAX_RETRY_COUNT);
        assertFalse(transaction.canRetry());
        
        // Mark as failed
        transaction.markFailed();
        assertTrue(transaction.isCompleted());
        assertTrue(transaction.getTransactionState().isFailed());
    }

    @Test
    public void testTransactionCancellation() {
        SendTransaction transaction = new SendTransaction(mockContext, testUri, 0L);
        
        transaction.cancel();
        
        assertTrue(transaction.isCompleted());
        assertTrue(transaction.getTransactionState().isCancelled());
    }

    @Test
    public void testTransactionStateErrorHandling() {
        TransactionState state = new TransactionState();
        String errorMessage = "Test error message";
        
        state.setErrorMessage(errorMessage);
        state.setState(TransactionState.FAILED);
        
        assertEquals(errorMessage, state.getErrorMessage());
        assertTrue(state.isFailed());
    }

    @Test
    public void testTransactionStateContentUri() {
        TransactionState state = new TransactionState();
        Uri contentUri = Uri.parse("content://mms/inbox/2");
        
        state.setContentUri(contentUri);
        
        assertEquals(contentUri, state.getContentUri());
    }

    @Test
    public void testNotificationAutoDownloadCheck() {
        // Test the static method for auto-download checking
        boolean autoDownload = NotificationTransaction.allowAutoDownload(mockContext);
        
        // For now, this always returns true in our implementation
        assertTrue(autoDownload);
    }

    @Test
    public void testTransactionToString() {
        SendTransaction transaction = new SendTransaction(mockContext, testUri, 123L);
        String toString = transaction.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("SendTransaction"));
        assertTrue(toString.contains(testUri.toString()));
    }

    @Test
    public void testTransactionStateToString() {
        TransactionState state = new TransactionState();
        state.setState(TransactionState.PROCESSING);
        state.setErrorMessage("Test error");
        
        String toString = state.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("PROCESSING"));
    }

    /**
     * Test helper class for creating test transactions.
     */
    private static class TestTransaction extends Transaction {
        
        public TestTransaction(Context context, Uri uri) {
            super(context, uri);
        }

        @Override
        public void process() {
            mTransactionState.setState(TransactionState.PROCESSING);
        }

        @Override
        public int getTransactionType() {
            return 999; // Test type
        }
    }

    @Test
    public void testCustomTransaction() {
        TestTransaction transaction = new TestTransaction(mockContext, testUri);
        
        assertEquals(999, transaction.getTransactionType());
        assertEquals(TransactionState.INITIALIZED, transaction.getTransactionState().getState());
        
        transaction.process();
        
        assertEquals(TransactionState.PROCESSING, transaction.getTransactionState().getState());
    }
}