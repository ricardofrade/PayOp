package com.rfrade.payop.domain.bus.handlers;

import com.rfrade.payop.domain.bus.TransactionCommand;
import com.rfrade.payop.domain.model.Transaction;
import com.rfrade.payop.domain.model.TransactionState;
import com.rfrade.payop.domain.model.TransactionStateMachine;
import com.rfrade.payop.domain.repository.TerminalRepository;
import com.rfrade.payop.domain.repository.TransactionRepository;

import java.util.UUID;

import javax.inject.Inject;

import io.reactivex.Single;

public class AuthorizationCommandHandler implements TransactionCommandHandler<TransactionCommand.NewAuthorization> {

    private final TransactionRepository repository;
    private final TerminalRepository gateway;
    private final TransactionStateMachine stateMachine;
    private final CancelCommandHandler cancelHandler;

    @Inject
    public AuthorizationCommandHandler(
            TransactionRepository repository,
            TerminalRepository gateway,
            TransactionStateMachine stateMachine,
            CancelCommandHandler cancelHandler) {
        this.repository = repository;
        this.gateway = gateway;
        this.stateMachine = stateMachine;
        this.cancelHandler = cancelHandler;
    }

    @Override
    public Single<Transaction> handle(TransactionCommand.NewAuthorization command) {
        if (command.amountCents <= 0) {
            return Single.error(new IllegalArgumentException("Amount must be > 0"));
        }

        Transaction txn = Transaction.createNew(command.amountCents);
        return repository.persist(txn)
                .andThen(Single.defer(() -> gateway.authorize(UUID.fromString(txn.getTxnId()), command.amountCents)))
                .flatMap(result -> {
                    TransactionState newState = stateMachine.resolveAuthorizationResult(result);
                    String errorCode = result.getErrorCode();
                    Transaction updated;

                    if (newState == TransactionState.AUTHORIZED) {
                        updated = txn.withApprovedAmount(result.getApprovedAmount())
                                .withState(TransactionState.AUTHORIZED, null, "Approved for " + result.getApprovedAmount() + " cents");
                    } else if (newState == TransactionState.CANCELLING) {
                        updated = txn.withState(TransactionState.CANCELLING, errorCode, "Authorization timed out — auto-cancelling");
                        return repository.persist(updated).andThen(cancelHandler.executeCancelLoop(updated, 0));
                    } else {
                        updated = txn.withState(TransactionState.DECLINED, errorCode, "Declined: " + errorCode);
                    }

                    return repository.persist(updated).andThen(Single.just(updated));
                });
    }
}
