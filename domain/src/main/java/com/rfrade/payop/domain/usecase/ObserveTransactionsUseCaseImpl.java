package com.rfrade.payop.domain.usecase;

import com.rfrade.payop.domain.model.Transaction;
import com.rfrade.payop.domain.repository.TransactionRepository;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Flowable;

public class ObserveTransactionsUseCaseImpl implements ObserveTransactionsUseCase {

    private final TransactionRepository repository;

    @Inject
    public ObserveTransactionsUseCaseImpl(TransactionRepository repository) {
        this.repository = repository;
    }

    public Flowable<List<Transaction>> execute() {
        return repository.observeAll();
    }
}
