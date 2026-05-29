package com.rfrade.payop.domain.bus.handlers;

import com.rfrade.payop.domain.bus.TransactionCommand;
import com.rfrade.payop.domain.model.Transaction;

import io.reactivex.Single;

public interface TransactionCommandHandler<T extends TransactionCommand> {
    Single<Transaction> handle(T command);
}
