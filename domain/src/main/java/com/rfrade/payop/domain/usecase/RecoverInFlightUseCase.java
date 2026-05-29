package com.rfrade.payop.domain.usecase;

import io.reactivex.Completable;

public interface RecoverInFlightUseCase {
    public Completable execute();
}
