package com.rfrade.payop;

import android.app.Application;
import android.util.Log;

import com.rfrade.payop.di.AppComponent;
import com.rfrade.payop.di.AppModule;
import com.rfrade.payop.di.DaggerAppComponent;
import com.rfrade.payop.domain.bus.TransactionCommandProcessor;
import com.rfrade.payop.domain.usecase.RecoverInFlightUseCase;
import com.rfrade.payop.presentation.detail.TransactionDetailActivity;
import com.rfrade.payop.presentation.di.ComponentProvider;
import com.rfrade.payop.presentation.list.TransactionListActivity;

import javax.inject.Inject;

import io.reactivex.schedulers.Schedulers;

public class PayOpApplication extends Application implements ComponentProvider {

    private static final String TAG = "PayOpApplication";
    
    @Inject
    RecoverInFlightUseCase recoverInFlightUseCase;
    
    @Inject
    TransactionCommandProcessor processor;
    
    private AppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        appComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();

        appComponent.inject(this);

        recoverInFlightUseCase
                .execute()
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> Log.d(TAG, "Recovery complete"),
                        error -> Log.e(TAG, "Recovery error", error));

        processor.start();
    }

    public AppComponent getAppComponent() {
        return appComponent;
    }

    @Override
    public void inject(TransactionListActivity activity) {
        appComponent.inject(activity);
    }

    @Override
    public void inject(TransactionDetailActivity activity) {
        appComponent.inject(activity);
    }
}
