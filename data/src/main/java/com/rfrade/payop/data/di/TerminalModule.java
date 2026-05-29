package com.rfrade.payop.data.di;

import com.elecctro.recruitment.paymentterminal.PaymentTerminal;
import com.rfrade.payop.data.terminal.TerminalRepositoryImpl;
import com.rfrade.payop.domain.repository.TerminalRepository;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;

@Module
public abstract class TerminalModule {

    @Provides
    @Singleton
    static PaymentTerminal providePaymentTerminal() {

        return PaymentTerminal.create(PaymentTerminal.Mode.REAL);
    }

    @Binds
    @Singleton
    abstract TerminalRepository bindTerminalRepository(TerminalRepositoryImpl impl);
}
