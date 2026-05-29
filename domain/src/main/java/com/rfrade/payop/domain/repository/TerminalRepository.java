package com.rfrade.payop.domain.repository;

import com.rfrade.payop.domain.model.AuthResult;
import com.rfrade.payop.domain.model.OperationResult;

import java.util.UUID;

import io.reactivex.Single;

public interface TerminalRepository {

    Single<AuthResult> authorize(UUID txnId, long amountCents);

    Single<OperationResult> capture(UUID txnId, long amountCents);

    Single<OperationResult> cancel(UUID txnId);
}
