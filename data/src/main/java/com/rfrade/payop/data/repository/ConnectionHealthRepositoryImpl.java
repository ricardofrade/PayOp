package com.rfrade.payop.data.repository;

import com.rfrade.payop.domain.repository.ConnectionHealthRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

@Singleton
public class ConnectionHealthRepositoryImpl implements ConnectionHealthRepository {

    private final BehaviorSubject<Long> lastNetworkErrorSubject = BehaviorSubject.create();

    @Inject
    public ConnectionHealthRepositoryImpl() {
    }

    @Override
    public void reportNetworkError(long timestampMs) {
        lastNetworkErrorSubject.onNext(timestampMs);
    }

    @Override
    public void reportNetworkSuccess() {
        lastNetworkErrorSubject.onNext(0L);
    }

    @Override
    public Observable<Long> observeLastNetworkError() {
        return lastNetworkErrorSubject.hide();
    }
}
