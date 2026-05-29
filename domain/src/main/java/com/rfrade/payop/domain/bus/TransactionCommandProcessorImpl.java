package com.rfrade.payop.domain.bus;

import android.util.Log;

import com.rfrade.payop.domain.bus.handlers.AuthorizationCommandHandler;
import com.rfrade.payop.domain.bus.handlers.CancelCommandHandler;
import com.rfrade.payop.domain.bus.handlers.CaptureCommandHandler;
import com.rfrade.payop.domain.bus.handlers.RetryCommandHandler;
import com.rfrade.payop.domain.model.Transaction;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

@Singleton
public class TransactionCommandProcessorImpl implements TransactionCommandProcessor {

    private static final String TAG = "TxnProcessor";

    private final RxBus rxBus;
    private final CaptureCommandHandler captureHandler;
    private final CancelCommandHandler cancelHandler;
    private final RetryCommandHandler retryHandler;
    private final AuthorizationCommandHandler authHandler;

    private final CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    public TransactionCommandProcessorImpl(
            RxBus rxBus,
            CaptureCommandHandler captureHandler,
            CancelCommandHandler cancelHandler,
            RetryCommandHandler retryHandler,
            AuthorizationCommandHandler authHandler) {
        this.rxBus = rxBus;
        this.captureHandler = captureHandler;
        this.cancelHandler = cancelHandler;
        this.retryHandler = retryHandler;
        this.authHandler = authHandler;
    }

    public void start() {
        disposables.add(
                rxBus.toObservable(TransactionCommand.class)
                        .observeOn(Schedulers.io())
                        .flatMapCompletable(cmd -> {
                            Single<Transaction> operation;
                            String txnId = null;

                            if (cmd instanceof TransactionCommand.Capture) {
                                TransactionCommand.Capture capture = (TransactionCommand.Capture) cmd;
                                txnId = capture.txnId;
                                operation = captureHandler.handle(capture);
                            } else if (cmd instanceof TransactionCommand.Cancel) {
                                TransactionCommand.Cancel cancel = (TransactionCommand.Cancel) cmd;
                                txnId = cancel.txnId;
                                operation = cancelHandler.handle(cancel);
                            } else if (cmd instanceof TransactionCommand.Retry) {
                                TransactionCommand.Retry retry = (TransactionCommand.Retry) cmd;
                                txnId = retry.txnId;
                                operation = retryHandler.handle(retry);
                            } else if (cmd instanceof TransactionCommand.NewAuthorization) {
                                operation = authHandler.handle((TransactionCommand.NewAuthorization) cmd);
                            } else {
                                return Completable.complete();
                            }

                            final String finalTxnId = txnId;
                            return operation.ignoreElement().onErrorResumeNext(error -> {
                                Log.e(TAG, "Command failed: " + cmd.getClass().getSimpleName(), error);
                                rxBus.send(new TransactionCommandError(finalTxnId, error.getMessage()));
                                return Completable.complete();
                            });
                        })
                        .subscribe(
                                () -> {},
                                error -> Log.e(TAG, "Processor stream crashed!", error)
                        )
        );
    }
}
