package com.rfrade.payop.domain.usecase;

import com.rfrade.payop.domain.repository.ConnectionHealthRepository;

import javax.inject.Inject;

import io.reactivex.Observable;

public class ObserveConnectionHealthUseCaseImpl implements ObserveConnectionHealthUseCase {

    private final ConnectionHealthRepository repository;

    @Inject
    public ObserveConnectionHealthUseCaseImpl(ConnectionHealthRepository repository) {
        this.repository = repository;
    }

    @Override
    public Observable<Long> execute() {
        return repository.observeLastNetworkError();
    }
}
