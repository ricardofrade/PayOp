package com.rfrade.payop.domain.usecase;

import com.rfrade.payop.domain.bus.TransactionCommand;

public interface DispatchTransactionCommandUseCase {
    void execute(TransactionCommand command);
}
