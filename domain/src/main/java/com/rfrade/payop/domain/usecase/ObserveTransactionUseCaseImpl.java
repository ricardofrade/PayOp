package com.rfrade.payop.domain.usecase;

import com.rfrade.payop.domain.model.Transaction;
import com.rfrade.payop.domain.repository.TransactionRepository;

import javax.inject.Inject;

import io.reactivex.Flowable;

public class ObserveTransactionUseCaseImpl implements ObserveTransactionUseCase {

    private final TransactionRepository repository;

    @Inject
    public ObserveTransactionUseCaseImpl(TransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Flowable<Transaction> execute(String txnId) {
        return repository
                .observeAll()
                .map(
                        transactions -> {
                            for (int i = 0; i < transactions.size(); i++) {
                                if (transactions.get(i).getTxnId().equals(txnId)) {
                                    return transactions.get(i);
                                }
                            }
                            throw new IllegalStateException("Transaction not found: " + txnId);
                        });
    }
}
