package com.rfrade.payop.domain.bus;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

@Singleton
public class RxBus {
    
    private final PublishSubject<Object> bus = PublishSubject.create();

    @Inject
    public RxBus() {
    }

    public void send(Object event) {
        bus.onNext(event);
    }

    public <T> Observable<T> toObservable(Class<T> eventType) {
        return bus.ofType(eventType);
    }
}
