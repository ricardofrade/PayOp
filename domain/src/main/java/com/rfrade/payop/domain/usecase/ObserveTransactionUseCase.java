package com.rfrade.payop.domain.usecase;

import com.rfrade.payop.domain.model.Transaction;

import io.reactivex.Flowable;

public interface ObserveTransactionUseCase {
    Flowable<Transaction> execute(String txnId);
}
