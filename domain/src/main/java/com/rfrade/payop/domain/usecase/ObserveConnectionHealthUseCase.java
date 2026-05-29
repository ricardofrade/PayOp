package com.rfrade.payop.domain.usecase;

import io.reactivex.Observable;

public interface ObserveConnectionHealthUseCase {
    Observable<Long> execute();
}
