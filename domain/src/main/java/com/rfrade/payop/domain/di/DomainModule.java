package com.rfrade.payop.domain.di;

import com.rfrade.payop.domain.bus.TransactionCommandProcessor;
import com.rfrade.payop.domain.bus.TransactionCommandProcessorImpl;
import com.rfrade.payop.domain.usecase.DispatchTransactionCommandUseCase;
import com.rfrade.payop.domain.usecase.DispatchTransactionCommandUseCaseImpl;
import com.rfrade.payop.domain.usecase.ObserveConnectionHealthUseCase;
import com.rfrade.payop.domain.usecase.ObserveConnectionHealthUseCaseImpl;
import com.rfrade.payop.domain.usecase.ObserveTransactionUseCase;
import com.rfrade.payop.domain.usecase.ObserveTransactionUseCaseImpl;
import com.rfrade.payop.domain.usecase.ObserveTransactionsUseCase;
import com.rfrade.payop.domain.usecase.ObserveTransactionsUseCaseImpl;
import com.rfrade.payop.domain.usecase.ObserveTransactionCommandErrorUseCase;
import com.rfrade.payop.domain.usecase.ObserveTransactionCommandErrorUseCaseImpl;
import com.rfrade.payop.domain.usecase.RecoverInFlightUseCase;
import com.rfrade.payop.domain.usecase.RecoverInFlightUseCaseImpl;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class DomainModule {

    @Binds
    public abstract TransactionCommandProcessor bindTransactionCommandProcessor(
            TransactionCommandProcessorImpl impl);

    @Binds
    public abstract DispatchTransactionCommandUseCase bindDispatchTransactionCommandUseCase(
            DispatchTransactionCommandUseCaseImpl impl);

    @Binds
    public abstract ObserveConnectionHealthUseCase bindObserveConnectionHealthUseCase(
            ObserveConnectionHealthUseCaseImpl impl);

    @Binds
    public abstract ObserveTransactionsUseCase bindObserveTransactionsUseCase(
            ObserveTransactionsUseCaseImpl impl);

    @Binds
    public abstract RecoverInFlightUseCase bindRecoverInFlightUseCase(
            RecoverInFlightUseCaseImpl impl);

    @Binds
    public abstract ObserveTransactionUseCase bindObserveTransactionUseCase(
            ObserveTransactionUseCaseImpl impl);

    @Binds
    public abstract ObserveTransactionCommandErrorUseCase bindObserveTransactionCommandErrorUseCase(
            ObserveTransactionCommandErrorUseCaseImpl impl);
}
