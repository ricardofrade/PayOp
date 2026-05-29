package com.rfrade.payop.presentation.detail;

import com.rfrade.payop.domain.bus.TransactionCommand;
import com.rfrade.payop.domain.bus.TransactionCommandError;
import com.rfrade.payop.domain.usecase.DispatchTransactionCommandUseCase;
import com.rfrade.payop.domain.usecase.ObserveTransactionUseCase;
import com.rfrade.payop.domain.usecase.ObserveTransactionCommandErrorUseCase;
import com.rfrade.payop.presentation.MviViewModel;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class TransactionDetailViewModel
        extends MviViewModel<TransactionDetailState, TransactionDetailIntent> {

    private final ObserveTransactionUseCase observeUseCase;
    private final DispatchTransactionCommandUseCase dispatchUseCase;
    private final ObserveTransactionCommandErrorUseCase observeTransactionCommandErrorUseCase;
    private String currentTxnId;

    @Inject
    public TransactionDetailViewModel(
            ObserveTransactionUseCase observeUseCase,
            DispatchTransactionCommandUseCase dispatchUseCase,
            ObserveTransactionCommandErrorUseCase observeTransactionCommandErrorUseCase) {
        this.observeUseCase = observeUseCase;
        this.dispatchUseCase = dispatchUseCase;
        this.observeTransactionCommandErrorUseCase = observeTransactionCommandErrorUseCase;
        setState(TransactionDetailState.loading());
        observeErrors();
    }

    private void observeErrors() {
        addDisposable(
                observeTransactionCommandErrorUseCase.execute()
                        .filter(error -> currentTxnId != null && currentTxnId.equals(error.getTxnId()))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(error -> {
                                    TransactionDetailState current = currentState();
                                    if (current != null) {
                                        setState(current.withError(error.getErrorMessage()));
                                    }
                                }
                        )
        );
    }

    @Override
    public void processIntent(TransactionDetailIntent intent) {
        if (intent instanceof TransactionDetailIntent.LoadDetail) {
            currentTxnId = ((TransactionDetailIntent.LoadDetail) intent).getTxnId();
            loadDetail(currentTxnId);
        } else if (intent instanceof TransactionDetailIntent.Capture) {
            TransactionDetailIntent.Capture capture = (TransactionDetailIntent.Capture) intent;
            capture(capture.getTxnId(), capture.getAmountCents());
        } else if (intent instanceof TransactionDetailIntent.Cancel) {
            cancel(((TransactionDetailIntent.Cancel) intent).getTxnId());
        } else if (intent instanceof TransactionDetailIntent.Retry) {
            retry(((TransactionDetailIntent.Retry) intent).getTxnId());
        }
    }

    private void loadDetail(String txnId) {

        addDisposable(
                observeUseCase
                        .execute(txnId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                txn -> setState(new TransactionDetailState(txn, false, null)),
                                error -> {
                                    TransactionDetailState current = currentState();
                                    if (current != null) {
                                        setState(current.withError(error.getMessage()));
                                    }
                                }));
    }

    private void capture(String txnId, long amountCents) {
        TransactionDetailState current = currentState();
        if (current != null) {
            setState(current.withProcessing());
        }
        dispatchUseCase.execute(new TransactionCommand.Capture(txnId, amountCents));
    }

    private void cancel(String txnId) {
        TransactionDetailState current = currentState();
        if (current != null) {
            setState(current.withProcessing());
        }
        dispatchUseCase.execute(new TransactionCommand.Cancel(txnId));
    }

    private void retry(String txnId) {
        TransactionDetailState current = currentState();
        if (current != null) {
            setState(current.withProcessing());
        }
        dispatchUseCase.execute(new TransactionCommand.Retry(txnId));
    }
}
