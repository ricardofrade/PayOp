package com.rfrade.payop.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.rfrade.payop.domain.model.TransactionState;
import com.rfrade.payop.domain.model.TransactionStateMachine;
import com.rfrade.payop.fake.ResultFactory;

import org.junit.Before;
import org.junit.Test;

public class TransactionStateMachineTest {

    private TransactionStateMachine sm;

    @Before
    public void setUp() {
        sm = new TransactionStateMachine();
    }

    @Test
    public void initial_to_authorizing() {
        assertTrue(sm.isValidTransition(null, TransactionState.AUTHORIZING));
    }

    @Test
    public void authorizing_to_authorized() {
        assertTrue(sm.isValidTransition(TransactionState.AUTHORIZING, TransactionState.AUTHORIZED));
    }

    @Test
    public void authorizing_to_declined() {
        assertTrue(sm.isValidTransition(TransactionState.AUTHORIZING, TransactionState.DECLINED));
    }

    @Test
    public void authorizing_to_cancelling() {
        assertTrue(sm.isValidTransition(TransactionState.AUTHORIZING, TransactionState.CANCELLING));
    }

    @Test
    public void authorized_to_capturing() {
        assertTrue(sm.isValidTransition(TransactionState.AUTHORIZED, TransactionState.CAPTURING));
    }

    @Test
    public void authorized_to_cancelling() {
        assertTrue(sm.isValidTransition(TransactionState.AUTHORIZED, TransactionState.CANCELLING));
    }

    @Test
    public void capturing_to_captured() {
        assertTrue(sm.isValidTransition(TransactionState.CAPTURING, TransactionState.CAPTURED));
    }

    @Test
    public void capturing_to_capture_failed() {
        assertTrue(sm.isValidTransition(TransactionState.CAPTURING, TransactionState.CAPTURE_FAILED));
    }

    @Test
    public void cancelling_to_cancelled() {
        assertTrue(sm.isValidTransition(TransactionState.CANCELLING, TransactionState.CANCELLED));
    }

    @Test
    public void cancelling_to_cancel_failed() {
        assertTrue(sm.isValidTransition(TransactionState.CANCELLING, TransactionState.CANCEL_FAILED));
    }

    @Test
    public void initial_to_authorized_invalid() {
        assertFalse(sm.isValidTransition(null, TransactionState.AUTHORIZED));
    }

    @Test
    public void authorizing_to_captured_invalid() {
        assertFalse(sm.isValidTransition(TransactionState.AUTHORIZING, TransactionState.CAPTURED));
    }

    @Test
    public void authorized_to_captured_invalid() {
        assertFalse(sm.isValidTransition(TransactionState.AUTHORIZED, TransactionState.CAPTURED));
    }

    @Test
    public void captured_rejects_all() {
        for (TransactionState to : TransactionState.values())
            assertFalse(sm.isValidTransition(TransactionState.CAPTURED, to));
    }

    @Test
    public void cancelled_rejects_all() {
        for (TransactionState to : TransactionState.values())
            assertFalse(sm.isValidTransition(TransactionState.CANCELLED, to));
    }

    @Test
    public void declined_rejects_all() {
        for (TransactionState to : TransactionState.values())
            assertFalse(sm.isValidTransition(TransactionState.DECLINED, to));
    }

    @Test
    public void capture_failed_rejects_all() {
        for (TransactionState to : TransactionState.values())
            assertFalse(sm.isValidTransition(TransactionState.CAPTURE_FAILED, to));
    }

    @Test
    public void cancel_failed_rejects_all() {
        for (TransactionState to : TransactionState.values())
            assertFalse(sm.isValidTransition(TransactionState.CANCEL_FAILED, to));
    }

    @Test
    public void resolve_auth_approved() {
        assertEquals(TransactionState.AUTHORIZED, sm.resolveAuthorizationResult(ResultFactory.authorizationApproved(1000)));
    }

    @Test
    public void resolve_auth_declined() {
        assertEquals(TransactionState.DECLINED, sm.resolveAuthorizationResult(ResultFactory.authorizationDeclined("INSUFFICIENT_FUNDS")));
    }

    @Test
    public void resolve_auth_timed_out() {
        assertEquals(TransactionState.CANCELLING, sm.resolveAuthorizationResult(ResultFactory.authorizationTimedOut()));
    }

    @Test
    public void resolve_capture_approved() {
        assertEquals(TransactionState.CAPTURED, sm.resolveCaptureResult(ResultFactory.operationApproved()));
    }

    @Test
    public void resolve_capture_already_captured() {
        assertEquals(TransactionState.CAPTURED, sm.resolveCaptureResult(ResultFactory.operationDeclined("ALREADY_CAPTURED")));
    }

    @Test
    public void resolve_capture_unknown() {
        assertEquals(TransactionState.CAPTURE_FAILED, sm.resolveCaptureResult(ResultFactory.operationDeclined("UNKNOWN_TRANSACTION_ID")));
    }

    @Test
    public void resolve_capture_timed_out() {
        assertNull(sm.resolveCaptureResult(ResultFactory.operationTimedOut()));
    }

    @Test
    public void resolve_cancel_approved() {
        assertEquals(TransactionState.CANCELLED, sm.resolveCancelResult(ResultFactory.operationApproved()));
    }

    @Test
    public void resolve_cancel_unknown() {
        assertEquals(TransactionState.CANCELLED, sm.resolveCancelResult(ResultFactory.operationDeclined("UNKNOWN_TRANSACTION_ID")));
    }

    @Test
    public void resolve_cancel_already_cancelled() {
        assertEquals(TransactionState.CANCELLED, sm.resolveCancelResult(ResultFactory.operationDeclined("ALREADY_CANCELLED")));
    }

    @Test
    public void resolve_cancel_already_captured() {
        assertEquals(TransactionState.CANCEL_FAILED, sm.resolveCancelResult(ResultFactory.operationDeclined("ALREADY_CAPTURED")));
    }

    @Test
    public void resolve_cancel_timed_out() {
        assertNull(sm.resolveCancelResult(ResultFactory.operationTimedOut()));
    }
}
