package com.rfrade.payop.fake;

import com.rfrade.payop.domain.model.Transaction;
import com.rfrade.payop.domain.model.TransactionState;
import com.rfrade.payop.domain.repository.TransactionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.processors.BehaviorProcessor;

public class InMemoryTransactionRepository implements TransactionRepository {

    private static final int MAX_TRANSACTIONS = 50;

    private final Map<String, Transaction> store = new ConcurrentHashMap<>();
    private final BehaviorProcessor<List<Transaction>> subject = BehaviorProcessor.create();

    @Override
    public Completable persist(Transaction transaction) {
        return Completable.fromAction(
                () -> {
                    store.put(transaction.getTxnId(), transaction);
                    trimToLimit();
                    emitAll();
                });
    }

    @Override
    public Single<Transaction> getById(String txnId) {
        return Single.fromCallable(
                () -> {
                    Transaction txn = store.get(txnId);
                    if (txn == null) {
                        throw new IllegalStateException("Transaction not found: " + txnId);
                    }
                    return txn;
                });
    }

    @Override
    public Flowable<List<Transaction>> observeAll() {
        emitAll();
        return subject;
    }

    @Override
    public Single<List<Transaction>> getInFlight() {
        return Single.fromCallable(
                () -> {
                    List<Transaction> result = new ArrayList<>();
                    for (Transaction txn : store.values()) {
                        TransactionState state = txn.getState();
                        if (state == TransactionState.AUTHORIZING
                                || state == TransactionState.CAPTURING
                                || state == TransactionState.CANCELLING) {
                            result.add(txn);
                        }
                    }
                    return result;
                });
    }

    public int size() {
        return store.size();
    }

    public List<Transaction> getAll() {
        return new ArrayList<>(store.values());
    }

    private void trimToLimit() {
        if (store.size() <= MAX_TRANSACTIONS) return;

        List<Transaction> sorted = new ArrayList<>(store.values());
        sorted.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        for (int i = MAX_TRANSACTIONS; i < sorted.size(); i++) {
            store.remove(sorted.get(i).getTxnId());
        }
    }

    private void emitAll() {
        List<Transaction> list = new ArrayList<>(store.values());
        list.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        subject.onNext(list);
    }
}
