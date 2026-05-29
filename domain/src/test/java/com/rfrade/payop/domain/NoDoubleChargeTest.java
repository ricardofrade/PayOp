package com.rfrade.payop.domain;

import static org.junit.Assert.assertEquals;

import com.rfrade.payop.domain.bus.TransactionCommand;
import com.rfrade.payop.domain.bus.handlers.AuthorizationCommandHandler;
import com.rfrade.payop.domain.bus.handlers.CancelCommandHandler;
import com.rfrade.payop.domain.model.RetryPolicy;
import com.rfrade.payop.domain.model.Transaction;
import com.rfrade.payop.domain.model.TransactionState;
import com.rfrade.payop.domain.model.TransactionStateMachine;
import com.rfrade.payop.fake.FakeTerminalGateway;
import com.rfrade.payop.fake.InMemoryTransactionRepository;
import com.rfrade.payop.fake.ResultFactory;

import org.junit.Before;
import org.junit.Test;

public class NoDoubleChargeTest {

    private FakeTerminalGateway gateway;
    private AuthorizationCommandHandler authHandler;

    @Before
    public void setUp() {
        gateway = new FakeTerminalGateway();
        InMemoryTransactionRepository repo = new InMemoryTransactionRepository();
        TransactionStateMachine sm = new TransactionStateMachine();
        RetryPolicy policy = new RetryPolicy(3, 0, 1.0, 0);

        CancelCommandHandler cancelHandler = new CancelCommandHandler(repo, gateway, sm, policy);
        authHandler = new AuthorizationCommandHandler(repo, gateway, sm, cancelHandler);
    }

    @Test
    public void authorize_called_once_on_happy_path() {
        gateway.setDefaultAuthorizeResponse(ResultFactory.authorizationApproved(1000));
        authHandler.handle(new TransactionCommand.NewAuthorization(1000L)).blockingGet();
        assertEquals(1, gateway.totalAuthorizeCallCount());
    }

    @Test
    public void authorize_called_once_on_timeout_then_cancel() {
        gateway.setDefaultAuthorizeResponse(ResultFactory.authorizationTimedOut());
        gateway.setDefaultCancelResponse(ResultFactory.operationDeclined("UNKNOWN_TRANSACTION_ID"));

        Transaction result = authHandler.handle(new TransactionCommand.NewAuthorization(1000L)).blockingGet();
        assertEquals(TransactionState.CANCELLED, result.getState());
        assertEquals(1, gateway.totalAuthorizeCallCount());
    }

    @Test
    public void three_transactions_get_three_authorizes() {
        gateway.setDefaultAuthorizeResponse(ResultFactory.authorizationApproved(500));
        authHandler.handle(new TransactionCommand.NewAuthorization(500L)).blockingGet();
        authHandler.handle(new TransactionCommand.NewAuthorization(500L)).blockingGet();
        authHandler.handle(new TransactionCommand.NewAuthorization(500L)).blockingGet();
        assertEquals(3, gateway.totalAuthorizeCallCount());
    }
}
