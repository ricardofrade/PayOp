package com.rfrade.payop.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.rfrade.payop.domain.bus.TransactionCommand;
import com.rfrade.payop.domain.bus.handlers.AuthorizationCommandHandler;
import com.rfrade.payop.domain.bus.handlers.CancelCommandHandler;
import com.rfrade.payop.domain.bus.handlers.CaptureCommandHandler;
import com.rfrade.payop.domain.model.RetryPolicy;
import com.rfrade.payop.domain.model.Transaction;
import com.rfrade.payop.domain.model.TransactionState;
import com.rfrade.payop.domain.model.TransactionStateMachine;
import com.rfrade.payop.fake.FakeTerminalGateway;
import com.rfrade.payop.fake.InMemoryTransactionRepository;
import com.rfrade.payop.fake.ResultFactory;

import org.junit.Before;
import org.junit.Test;

public class CaptureValidationTest {

    private FakeTerminalGateway gateway;
    private InMemoryTransactionRepository repo;
    private AuthorizationCommandHandler authHandler;
    private CaptureCommandHandler captureHandler;

    @Before
    public void setUp() {
        gateway = new FakeTerminalGateway();
        repo = new InMemoryTransactionRepository();
        TransactionStateMachine sm = new TransactionStateMachine();
        RetryPolicy policy = new RetryPolicy(3, 0, 1.0, 0);

        CancelCommandHandler cancelHandler = new CancelCommandHandler(repo, gateway, sm, policy);
        captureHandler = new CaptureCommandHandler(repo, gateway, sm, policy);
        authHandler = new AuthorizationCommandHandler(repo, gateway, sm, cancelHandler);
    }

    @Test
    public void capture_over_approved_amount_rejected() {
        gateway.setDefaultAuthorizeResponse(ResultFactory.authorizationApproved(1000));
        Transaction txn = authHandler.handle(new TransactionCommand.NewAuthorization(1000L)).blockingGet();
        assertEquals(TransactionState.AUTHORIZED, txn.getState());

        try {
            captureHandler.handle(new TransactionCommand.Capture(txn.getTxnId(), 1001L)).blockingGet();
            fail("Over-capture should throw");
        } catch (Exception e) {

        }
    }

    @Test
    public void capture_zero_amount_rejected() {
        gateway.setDefaultAuthorizeResponse(ResultFactory.authorizationApproved(1000));
        Transaction txn = authHandler.handle(new TransactionCommand.NewAuthorization(1000L)).blockingGet();

        try {
            captureHandler.handle(new TransactionCommand.Capture(txn.getTxnId(), 0L)).blockingGet();
            fail("Zero amount should throw");
        } catch (Exception e) {

        }
    }

    @Test
    public void capture_from_declined_state_rejected() {
        gateway.setDefaultAuthorizeResponse(ResultFactory.authorizationDeclined("DECLINED"));
        Transaction txn = authHandler.handle(new TransactionCommand.NewAuthorization(1000L)).blockingGet();
        assertEquals(TransactionState.DECLINED, txn.getState());

        try {
            captureHandler.handle(new TransactionCommand.Capture(txn.getTxnId(), 500L)).blockingGet();
            fail("Capture from DECLINED should throw");
        } catch (Exception e) {

        }
    }

    @Test
    public void partial_capture_succeeds() {
        gateway.setDefaultAuthorizeResponse(ResultFactory.authorizationApproved(2000));
        gateway.setDefaultCaptureResponse(ResultFactory.operationApproved());

        Transaction txn = authHandler.handle(new TransactionCommand.NewAuthorization(2000L)).blockingGet();
        Transaction captured = captureHandler.handle(new TransactionCommand.Capture(txn.getTxnId(), 500L)).blockingGet();
        assertEquals(TransactionState.CAPTURED, captured.getState());
    }
}
