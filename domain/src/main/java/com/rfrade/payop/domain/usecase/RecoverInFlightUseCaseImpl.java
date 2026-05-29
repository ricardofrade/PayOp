package com.rfrade.payop.domain.usecase;

import com.rfrade.payop.domain.bus.RxBus;
import com.rfrade.payop.domain.bus.TransactionCommand;
import com.rfrade.payop.domain.model.Transaction;
import com.rfrade.payop.domain.model.TransactionState;
import com.rfrade.payop.domain.repository.TransactionRepository;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;

public class RecoverInFlightUseCaseImpl implements RecoverInFlightUseCase {

    private final TransactionRepository repository;
    private final RxBus rxBus;

    @Inject
    public RecoverInFlightUseCaseImpl(
            TransactionRepository repository,
            RxBus rxBus) {
        this.repository = repository;
        this.rxBus = rxBus;
    }

    public Completable execute() {
        return repository
                .getInFlight()
                .flatMapCompletable(
                        transactions ->
                                Observable.fromIterable(transactions)
                                        .flatMapCompletable(this::recoverTransaction));
    }

    private Completable recoverTransaction(Transaction txn) {
        switch (txn.getState()) {
            case AUTHORIZING:
                return recoverAuthorizing(txn);
            case CAPTURING:
                return recoverCapturing(txn);
            case CANCELLING:
                return recoverCancelling(txn);
            default:

                return Completable.complete();
        }
    }

    private Completable recoverAuthorizing(Transaction txn) {
        Transaction cancelling =
                txn.withState(
                        TransactionState.CANCELLING,
                        null,
                        "Recovery: authorization fate unknown — cancelling");
        return repository
                .persist(cancelling)
                .andThen(Completable.fromAction(() -> 
                        rxBus.send(new TransactionCommand.Cancel(cancelling.getTxnId()))
                ))
                .onErrorComplete();
    }

    private Completable recoverCapturing(Transaction txn) {
        return Completable.fromAction(() -> 
                rxBus.send(new TransactionCommand.Capture(txn.getTxnId(), txn.getCaptureAmountCents()))
        ).onErrorComplete();
    }

    private Completable recoverCancelling(Transaction txn) {
        return Completable.fromAction(() -> 
                rxBus.send(new TransactionCommand.Cancel(txn.getTxnId()))
        ).onErrorComplete();
    }
}
