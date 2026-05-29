package com.rfrade.payop.domain.usecase;

import com.rfrade.payop.domain.bus.TransactionCommandError;

import io.reactivex.Observable;

public interface ObserveTransactionCommandErrorUseCase {
    Observable<TransactionCommandError> execute();
}
