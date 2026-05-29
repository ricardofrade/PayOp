package com.rfrade.payop.data.repository;

import com.rfrade.payop.data.local.TransactionDao;
import com.rfrade.payop.data.mapper.EntityMapper;
import com.rfrade.payop.domain.model.Transaction;
import com.rfrade.payop.domain.repository.TransactionRepository;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

@Singleton
public class TransactionRepositoryImpl implements TransactionRepository {

    private static final int MAX_TRANSACTIONS = 50;

    private final TransactionDao dao;
    private final EntityMapper mapper;

    @Inject
    public TransactionRepositoryImpl(TransactionDao dao, EntityMapper mapper) {
        this.dao = dao;
        this.mapper = mapper;
    }

    @Override
    public Completable persist(Transaction transaction) {
        return Completable.fromAction(() -> dao.upsertAndTrim(mapper.toEntity(transaction), MAX_TRANSACTIONS))
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<Transaction> getById(String txnId) {
        return dao.getById(txnId).map(mapper::toDomain).subscribeOn(Schedulers.io());
    }

    @Override
    public Flowable<List<Transaction>> observeAll() {
        return dao.observeAll().map(mapper::toDomainList).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<List<Transaction>> getInFlight() {
        return Single.fromCallable(() -> mapper.toDomainList(dao.getInFlightSync()))
                .subscribeOn(Schedulers.io());
    }
}
