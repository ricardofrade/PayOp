package com.rfrade.payop.domain.repository;

import com.rfrade.payop.domain.model.Transaction;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;

public interface TransactionRepository {

    Completable persist(Transaction transaction);

    Single<Transaction> getById(String txnId);

    Flowable<List<Transaction>> observeAll();

    Single<List<Transaction>> getInFlight();
}
