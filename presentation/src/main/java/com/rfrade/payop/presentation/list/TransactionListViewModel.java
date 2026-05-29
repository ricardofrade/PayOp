package com.rfrade.payop.presentation.list;

import com.rfrade.payop.domain.bus.TransactionCommand;
import com.rfrade.payop.domain.bus.TransactionCommandError;
import com.rfrade.payop.domain.usecase.DispatchTransactionCommandUseCase;
import com.rfrade.payop.domain.usecase.ObserveConnectionHealthUseCase;
import com.rfrade.payop.domain.usecase.ObserveTransactionsUseCase;
import com.rfrade.payop.domain.usecase.ObserveTransactionCommandErrorUseCase;
import com.rfrade.payop.presentation.MviViewModel;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class TransactionListViewModel
        extends MviViewModel<TransactionListState, TransactionListIntent> {

    private final ObserveTransactionsUseCase observeUseCase;
    private final DispatchTransactionCommandUseCase dispatchUseCase;
    private final ObserveTransactionCommandErrorUseCase observeTransactionCommandErrorUseCase;
    private final ObserveConnectionHealthUseCase observeConnectionHealthUseCase;

    @Inject
    public TransactionListViewModel(
            ObserveTransactionsUseCase observeUseCase,
            DispatchTransactionCommandUseCase dispatchUseCase,
            ObserveTransactionCommandErrorUseCase observeTransactionCommandErrorUseCase,
            ObserveConnectionHealthUseCase observeConnectionHealthUseCase) {
        this.observeUseCase = observeUseCase;
        this.dispatchUseCase = dispatchUseCase;
        this.observeTransactionCommandErrorUseCase = observeTransactionCommandErrorUseCase;
        this.observeConnectionHealthUseCase = observeConnectionHealthUseCase;
        setState(TransactionListState.initial());
        observeHealth();
        observeErrors();
    }

    private void observeErrors() {
        addDisposable(
                observeTransactionCommandErrorUseCase.execute()
                        .filter(error -> error.getTxnId() == null)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(error -> {
                                    TransactionListState current = currentState();
                                    if (current != null) {
                                        setState(current.withError(error.getErrorMessage()));
                                    }
                                }
                        )
        );
    }

    private void observeHealth() {
        addDisposable(
                observeConnectionHealthUseCase
                        .execute()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(timestampMs -> {
                            TransactionListState current = currentState();
                            if (current != null) {
                                setState(current.withLastNetworkError(timestampMs == 0L ? null : timestampMs));
                            }
                        })
        );
    }

    @Override
    public void processIntent(TransactionListIntent intent) {
        if (intent instanceof TransactionListIntent.LoadTransactions) {
            loadTransactions();
        } else if (intent instanceof TransactionListIntent.NewAuthorization) {
            newAuthorization(((TransactionListIntent.NewAuthorization) intent).getAmountCents());
        }
    }

    private void loadTransactions() {
        addDisposable(
                observeUseCase.execute()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                transactions -> {
                                    TransactionListState current = currentState();
                                    if (current != null) {
                                        setState(current.withTransactions(transactions));
                                    } else {
                                        setState(new TransactionListState(transactions, false, null, null));
                                    }
                                },
                                error -> {
                                    TransactionListState current = currentState();
                                    if (current != null) {
                                        setState(current.withError(error.getMessage()));
                                    }
                                }
                        )
        );
    }

    private void newAuthorization(long amountCents) {
        dispatchUseCase.execute(new TransactionCommand.NewAuthorization(amountCents));
    }
}
