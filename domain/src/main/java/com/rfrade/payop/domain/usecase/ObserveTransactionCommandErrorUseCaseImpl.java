package com.rfrade.payop.domain.usecase;

import com.rfrade.payop.domain.bus.RxBus;
import com.rfrade.payop.domain.bus.TransactionCommandError;

import javax.inject.Inject;

import io.reactivex.Observable;

public class ObserveTransactionCommandErrorUseCaseImpl implements ObserveTransactionCommandErrorUseCase {

    private final RxBus rxBus;

    @Inject
    public ObserveTransactionCommandErrorUseCaseImpl(RxBus rxBus) {
        this.rxBus = rxBus;
    }

    @Override
    public Observable<TransactionCommandError> execute() {
        return rxBus.toObservable(TransactionCommandError.class);
    }
}
