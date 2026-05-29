package com.rfrade.payop.data.terminal;

import com.elecctro.recruitment.paymentterminal.PaymentTerminal;
import com.rfrade.payop.data.mapper.ResultMapper;
import com.rfrade.payop.domain.model.AuthResult;
import com.rfrade.payop.domain.model.OperationResult;
import com.rfrade.payop.domain.repository.ConnectionHealthRepository;
import com.rfrade.payop.domain.repository.TerminalRepository;

import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;

@Singleton
public class TerminalRepositoryImpl implements TerminalRepository {

    private final PaymentTerminal terminal;
    private final ConnectionHealthRepository healthMonitor;

    @Inject
    public TerminalRepositoryImpl(
            PaymentTerminal terminal, ConnectionHealthRepository healthMonitor) {
        this.terminal = terminal;
        this.healthMonitor = healthMonitor;
    }

    @Override
    public Single<AuthResult> authorize(UUID txnId, long amountCents) {
        return LiveDataAdapter
                .<com.elecctro.recruitment.paymentterminal.AuthorizationResult>toSingle(
                        terminal.authorize(txnId, amountCents))
                .map(ResultMapper::toAuthResult)
                .doOnSuccess(
                        result -> {
                            if ("NETWORK_ERROR".equals(result.getErrorCode())) {
                                healthMonitor.reportNetworkError(System.currentTimeMillis());
                            } else {
                                healthMonitor.reportNetworkSuccess();
                            }
                        });
    }

    @Override
    public Single<OperationResult> capture(UUID txnId, long amountCents) {
        return LiveDataAdapter.<com.elecctro.recruitment.paymentterminal.TerminalResult>toSingle(
                        terminal.capture(txnId, amountCents))
                .map(ResultMapper::toOperationResult)
                .doOnSuccess(
                        result -> {
                            if ("NETWORK_ERROR".equals(result.getErrorCode())) {
                                healthMonitor.reportNetworkError(System.currentTimeMillis());
                            } else {
                                healthMonitor.reportNetworkSuccess();
                            }
                        });
    }

    @Override
    public Single<OperationResult> cancel(UUID txnId) {
        return LiveDataAdapter.<com.elecctro.recruitment.paymentterminal.TerminalResult>toSingle(
                        terminal.cancel(txnId))
                .map(ResultMapper::toOperationResult)
                .doOnSuccess(
                        result -> {
                            if ("NETWORK_ERROR".equals(result.getErrorCode())) {
                                healthMonitor.reportNetworkError(System.currentTimeMillis());
                            } else {
                                healthMonitor.reportNetworkSuccess();
                            }
                        });
    }
}
