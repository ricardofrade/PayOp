package com.rfrade.payop.domain.usecase;

import com.rfrade.payop.domain.bus.RxBus;
import com.rfrade.payop.domain.bus.TransactionCommand;

import javax.inject.Inject;

public class DispatchTransactionCommandUseCaseImpl implements DispatchTransactionCommandUseCase {

    private final RxBus rxBus;

    @Inject
    public DispatchTransactionCommandUseCaseImpl(RxBus rxBus) {
        this.rxBus = rxBus;
    }

    @Override
    public void execute(TransactionCommand command) {
        rxBus.send(command);
    }
}
