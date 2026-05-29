package com.rfrade.payop.presentation;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.reactivex.disposables.CompositeDisposable;

public abstract class MviViewModel<S, I> extends ViewModel {

    protected final CompositeDisposable disposables = new CompositeDisposable();
    private final MutableLiveData<S> stateData = new MutableLiveData<>();

    public LiveData<S> states() {
        return stateData;
    }

    protected S currentState() {
        return stateData.getValue();
    }

    protected void setState(S state) {
        stateData.postValue(state);
    }

    public abstract void processIntent(I intent);

    protected void addDisposable(io.reactivex.disposables.Disposable disposable) {
        disposables.add(disposable);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
