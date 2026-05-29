package com.rfrade.payop.domain.usecase;

import com.rfrade.payop.domain.model.Transaction;

import java.util.List;

import io.reactivex.Flowable;

public interface ObserveTransactionsUseCase {
    public Flowable<List<Transaction>> execute();
}
