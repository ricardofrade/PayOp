package com.rfrade.payop.domain.bus.handlers;

import com.rfrade.payop.domain.bus.TransactionCommand;
import com.rfrade.payop.domain.model.RetryPolicy;
import com.rfrade.payop.domain.model.Transaction;
import com.rfrade.payop.domain.model.TransactionState;
import com.rfrade.payop.domain.model.TransactionStateMachine;
import com.rfrade.payop.domain.repository.TerminalRepository;
import com.rfrade.payop.domain.repository.TransactionRepository;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Single;

public class CancelCommandHandler implements TransactionCommandHandler<TransactionCommand.Cancel> {

    private final TransactionRepository repository;
    private final TerminalRepository gateway;
    private final TransactionStateMachine stateMachine;
    private final RetryPolicy retryPolicy;

    @Inject
    public CancelCommandHandler(
            TransactionRepository repository,
            TerminalRepository gateway,
            TransactionStateMachine stateMachine,
            RetryPolicy retryPolicy) {
        this.repository = repository;
        this.gateway = gateway;
        this.stateMachine = stateMachine;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public Single<Transaction> handle(TransactionCommand.Cancel command) {
        return repository.getById(command.txnId).flatMap(txn -> {
            if (!txn.getState().allowsCancel()) {
                return Single.error(new IllegalStateException("Cannot cancel transaction in state " + txn.getState()));
            }
            Transaction cancelling = txn.withRetryCount(0).withState(TransactionState.CANCELLING, null, "Cancelling transaction");
            return repository.persist(cancelling).andThen(executeCancelLoop(cancelling, 0));
        });
    }

    public Single<Transaction> executeCancelLoop(Transaction txn, int attempt) {
        UUID uuid = UUID.fromString(txn.getTxnId());
        return gateway.cancel(uuid).flatMap(result -> {
            TransactionState resolved = stateMachine.resolveCancelResult(result);
            String errorCode = result.getErrorCode();

            if (resolved != null) {
                String detail = resolved == TransactionState.CANCELLED ? "Cancel confirmed" : "Cancel failed: " + errorCode;
                Transaction updated = txn.withState(resolved, errorCode, detail);
                return repository.persist(updated).andThen(Single.just(updated));
            }

            if (attempt >= retryPolicy.getMaxRetries()) {
                Transaction failed = txn.withState(TransactionState.CANCEL_FAILED, errorCode, "Retries exhausted after " + (attempt + 1) + " attempts");
                return repository.persist(failed).andThen(Single.just(failed));
            }

            Transaction retrying = txn.withRetryIncrement();
            long delayMs = retryPolicy.delayForAttempt(attempt);

            return repository.persist(retrying)
                    .andThen(Single.timer(delayMs, TimeUnit.MILLISECONDS))
                    .flatMap(ignored -> executeCancelLoop(retrying, attempt + 1));
        });
    }
}
