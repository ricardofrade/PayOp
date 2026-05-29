package com.rfrade.payop.domain.bus.handlers;

import com.rfrade.payop.domain.bus.TransactionCommand;
import com.rfrade.payop.domain.model.Transaction;
import com.rfrade.payop.domain.model.TransactionState;
import com.rfrade.payop.domain.repository.TransactionRepository;

import javax.inject.Inject;

import io.reactivex.Single;

public class RetryCommandHandler implements TransactionCommandHandler<TransactionCommand.Retry> {

    private final TransactionRepository repository;
    private final CaptureCommandHandler captureHandler;
    private final CancelCommandHandler cancelHandler;

    @Inject
    public RetryCommandHandler(
            TransactionRepository repository,
            CaptureCommandHandler captureHandler,
            CancelCommandHandler cancelHandler) {
        this.repository = repository;
        this.captureHandler = captureHandler;
        this.cancelHandler = cancelHandler;
    }

    @Override
    public Single<Transaction> handle(TransactionCommand.Retry command) {
        return repository.getById(command.txnId).flatMap(txn -> {
            if (!txn.getState().isRetryable()) {
                return Single.error(new IllegalStateException("Cannot retry transaction in state " + txn.getState()));
            }

            if (txn.getState() == TransactionState.CAPTURE_FAILED) {
                Transaction retrying = txn.withRetryCount(0).withState(TransactionState.CAPTURING, null, "Operator retry — re-entering capture");
                return repository.persist(retrying).andThen(captureHandler.executeCaptureLoop(retrying, 0));
            } else {
                Transaction retrying = txn.withRetryCount(0).withState(TransactionState.CANCELLING, null, "Operator retry — re-entering cancel");
                return repository.persist(retrying).andThen(cancelHandler.executeCancelLoop(retrying, 0));
            }
        });
    }
}
