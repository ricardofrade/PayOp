package com.rfrade.payop.domain;

import static org.junit.Assert.assertEquals;

import com.rfrade.payop.domain.bus.RxBus;
import com.rfrade.payop.domain.bus.TransactionCommandProcessorImpl;
import com.rfrade.payop.domain.bus.handlers.AuthorizationCommandHandler;
import com.rfrade.payop.domain.bus.handlers.CancelCommandHandler;
import com.rfrade.payop.domain.bus.handlers.CaptureCommandHandler;
import com.rfrade.payop.domain.bus.handlers.RetryCommandHandler;
import com.rfrade.payop.domain.model.RetryPolicy;
import com.rfrade.payop.domain.model.Transaction;
import com.rfrade.payop.domain.model.TransactionState;
import com.rfrade.payop.domain.model.TransactionStateMachine;
import com.rfrade.payop.domain.usecase.RecoverInFlightUseCase;
import com.rfrade.payop.domain.usecase.RecoverInFlightUseCaseImpl;
import com.rfrade.payop.fake.FakeTerminalGateway;
import com.rfrade.payop.fake.InMemoryTransactionRepository;
import com.rfrade.payop.fake.ResultFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

public class RecoveryTest {

    private FakeTerminalGateway gateway;
    private InMemoryTransactionRepository repo;
    private RecoverInFlightUseCase recoverUseCase;

    @Before
    public void setUp() {
        gateway = new FakeTerminalGateway();
        repo = new InMemoryTransactionRepository();
        TransactionStateMachine sm = new TransactionStateMachine();
        RetryPolicy policy = new RetryPolicy(3, 0, 1.0, 0);

        RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
        RxJavaPlugins.setComputationSchedulerHandler(scheduler -> Schedulers.trampoline());

        CancelCommandHandler cancelHandler = new CancelCommandHandler(repo, gateway, sm, policy);
        CaptureCommandHandler captureHandler = new CaptureCommandHandler(repo, gateway, sm, policy);
        AuthorizationCommandHandler authHandler = new AuthorizationCommandHandler(repo, gateway, sm, cancelHandler);
        RetryCommandHandler retryHandler = new RetryCommandHandler(repo, captureHandler, cancelHandler);

        RxBus rxBus = new RxBus();
        TransactionCommandProcessorImpl processor = new TransactionCommandProcessorImpl(rxBus, captureHandler, cancelHandler, retryHandler, authHandler);
        processor.start();

        recoverUseCase = new RecoverInFlightUseCaseImpl(repo, rxBus);
    }

    @After
    public void tearDown() {
        RxJavaPlugins.reset();
    }

    @Test
    public void recovery_authorizing_unknown_txn_resolves_to_cancelled() {
        Transaction stuck = Transaction.createNew(1000);
        repo.persist(stuck).blockingAwait();

        gateway.setDefaultCancelResponse(ResultFactory.operationDeclined("UNKNOWN_TRANSACTION_ID"));
        recoverUseCase.execute().blockingAwait();

        assertEquals(TransactionState.CANCELLED, repo.getById(stuck.getTxnId()).blockingGet().getState());
    }

    @Test
    public void recovery_authorizing_approved_cancel_resolves_to_cancelled() {
        Transaction stuck = Transaction.createNew(2000);
        repo.persist(stuck).blockingAwait();

        gateway.setDefaultCancelResponse(ResultFactory.operationApproved());
        recoverUseCase.execute().blockingAwait();

        assertEquals(TransactionState.CANCELLED, repo.getById(stuck.getTxnId()).blockingGet().getState());
    }

    @Test
    public void recovery_authorizing_does_not_re_authorize() {
        Transaction stuck = Transaction.createNew(1500);
        repo.persist(stuck).blockingAwait();

        gateway.setDefaultCancelResponse(ResultFactory.operationDeclined("UNKNOWN_TRANSACTION_ID"));
        recoverUseCase.execute().blockingAwait();

        assertEquals(0, gateway.totalAuthorizeCallCount());
    }

    @Test
    public void recovery_capturing_approved_resolves_to_captured() {
        Transaction stuck = createCapturing(1000);
        repo.persist(stuck).blockingAwait();

        gateway.setDefaultCaptureResponse(ResultFactory.operationApproved());
        recoverUseCase.execute().blockingAwait();

        assertEquals(TransactionState.CAPTURED, repo.getById(stuck.getTxnId()).blockingGet().getState());
    }

    @Test
    public void recovery_capturing_already_captured_resolves_to_captured() {
        Transaction stuck = createCapturing(2000);
        repo.persist(stuck).blockingAwait();

        gateway.setDefaultCaptureResponse(ResultFactory.operationDeclined("ALREADY_CAPTURED"));
        recoverUseCase.execute().blockingAwait();

        assertEquals(TransactionState.CAPTURED, repo.getById(stuck.getTxnId()).blockingGet().getState());
    }

    @Test
    public void recovery_capturing_unknown_resolves_to_failed() {
        Transaction stuck = createCapturing(3000);
        repo.persist(stuck).blockingAwait();

        gateway.setDefaultCaptureResponse(ResultFactory.operationDeclined("UNKNOWN_TRANSACTION_ID"));
        recoverUseCase.execute().blockingAwait();

        assertEquals(TransactionState.CAPTURE_FAILED, repo.getById(stuck.getTxnId()).blockingGet().getState());
    }

    @Test
    public void recovery_cancelling_approved_resolves_to_cancelled() {
        Transaction stuck = createCancelling(1000);
        repo.persist(stuck).blockingAwait();

        gateway.setDefaultCancelResponse(ResultFactory.operationApproved());
        recoverUseCase.execute().blockingAwait();

        assertEquals(TransactionState.CANCELLED, repo.getById(stuck.getTxnId()).blockingGet().getState());
    }

    @Test
    public void recovery_cancelling_already_captured_resolves_to_cancel_failed() {
        Transaction stuck = createCancelling(2000);
        repo.persist(stuck).blockingAwait();

        gateway.setDefaultCancelResponse(ResultFactory.operationDeclined("ALREADY_CAPTURED"));
        recoverUseCase.execute().blockingAwait();

        assertEquals(TransactionState.CANCEL_FAILED, repo.getById(stuck.getTxnId()).blockingGet().getState());
    }

    private Transaction createCapturing(long amount) {
        return Transaction.createNew(amount)
                .withApprovedAmount(amount)
                .withState(TransactionState.AUTHORIZED, null, "Auth")
                .withCaptureAmount(amount)
                .withState(TransactionState.CAPTURING, null, "Capturing");
    }

    private Transaction createCancelling(long amount) {
        return Transaction.createNew(amount)
                .withApprovedAmount(amount)
                .withState(TransactionState.AUTHORIZED, null, "Auth")
                .withState(TransactionState.CANCELLING, null, "Cancelling");
    }
}
