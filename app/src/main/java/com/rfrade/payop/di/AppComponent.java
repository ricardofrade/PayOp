package com.rfrade.payop.di;

import com.rfrade.payop.PayOpApplication;
import com.rfrade.payop.data.di.DataModule;
import com.rfrade.payop.data.di.TerminalModule;
import com.rfrade.payop.domain.di.DomainModule;
import com.rfrade.payop.presentation.detail.TransactionDetailActivity;
import com.rfrade.payop.presentation.list.TransactionListActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(
        modules = {
                AppModule.class,
                DataModule.class,
                DomainModule.class,
                TerminalModule.class,
                com.rfrade.payop.presentation.di.ViewModelModule.class
        })
public interface AppComponent {

    void inject(PayOpApplication app);

    void inject(TransactionListActivity activity);

    void inject(TransactionDetailActivity activity);
}
