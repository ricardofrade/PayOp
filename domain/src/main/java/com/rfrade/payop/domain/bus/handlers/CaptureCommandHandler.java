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

public class CaptureCommandHandler implements TransactionCommandHandler<TransactionCommand.Capture> {

    private final TransactionRepository repository;
    private final TerminalRepository gateway;
    private final TransactionStateMachine stateMachine;
    private final RetryPolicy retryPolicy;

    @Inject
    public CaptureCommandHandler(
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
    public Single<Transaction> handle(TransactionCommand.Capture command) {
        return repository.getById(command.txnId).flatMap(txn -> {
            if (!txn.getState().allowsCapture()) {
                return Single.error(new IllegalStateException("Cannot capture transaction in state " + txn.getState()));
            }
            if (command.amountCents <= 0) {
                return Single.error(new IllegalArgumentException("Capture amount must be > 0"));
            }
            if (command.amountCents > txn.getApprovedAmountCents()) {
                return Single.error(new IllegalArgumentException("Capture amount " + command.amountCents + " exceeds approved amount " + txn.getApprovedAmountCents()));
            }

            Transaction capturing = txn.withCaptureAmount(command.amountCents)
                    .withRetryCount(0)
                    .withState(TransactionState.CAPTURING, null, "Capturing " + command.amountCents + " cents");

            return repository.persist(capturing).andThen(executeCaptureLoop(capturing, 0));
        });
    }

    public Single<Transaction> executeCaptureLoop(Transaction txn, int attempt) {
        UUID uuid = UUID.fromString(txn.getTxnId());
        return gateway.capture(uuid, txn.getCaptureAmountCents()).flatMap(result -> {
            TransactionState resolved = stateMachine.resolveCaptureResult(result);
            String errorCode = result.getErrorCode();

            if (resolved != null) {
                String detail = resolved == TransactionState.CAPTURED ? "Capture confirmed" : "Capture failed: " + errorCode;
                Transaction updated = txn.withState(resolved, errorCode, detail);
                return repository.persist(updated).andThen(Single.just(updated));
            }

            if (attempt >= retryPolicy.getMaxRetries()) {
                Transaction failed = txn.withState(TransactionState.CAPTURE_FAILED, errorCode, "Retries exhausted after " + (attempt + 1) + " attempts");
                return repository.persist(failed).andThen(Single.just(failed));
            }

            Transaction retrying = txn.withRetryIncrement();
            long delayMs = retryPolicy.delayForAttempt(attempt);

            return repository.persist(retrying)
                    .andThen(Single.timer(delayMs, TimeUnit.MILLISECONDS))
                    .flatMap(ignored -> executeCaptureLoop(retrying, attempt + 1));
        });
    }
}
