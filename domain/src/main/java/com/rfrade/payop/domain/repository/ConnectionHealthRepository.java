package com.rfrade.payop.domain.repository;

import io.reactivex.Observable;

public interface ConnectionHealthRepository {

    void reportNetworkError(long timestampMs);

    void reportNetworkSuccess();

    Observable<Long> observeLastNetworkError();
}
