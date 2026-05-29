package com.rfrade.payop.presentation.detail;

import com.rfrade.payop.domain.model.Transaction;

public final class TransactionDetailState {

    private final Transaction transaction;
    private final boolean isProcessing;
    private final String error;

    public TransactionDetailState(Transaction transaction, boolean isProcessing, String error) {
        this.transaction = transaction;
        this.isProcessing = isProcessing;
        this.error = error;
    }

    public static TransactionDetailState loading() {
        return new TransactionDetailState(null, true, null);
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    public String getError() {
        return error;
    }

    public boolean isCaptureEnabled() {
        return transaction != null && transaction.getState().allowsCapture() && !isProcessing;
    }

    public boolean isCancelEnabled() {
        return transaction != null && transaction.getState().allowsCancel() && !isProcessing;
    }

    public boolean isRetryEnabled() {
        return transaction != null && transaction.getState().isRetryable() && !isProcessing;
    }

    public TransactionDetailState withTransaction(Transaction transaction) {
        return new TransactionDetailState(transaction, false, null);
    }

    public TransactionDetailState withProcessing() {
        return new TransactionDetailState(this.transaction, true, null);
    }

    public TransactionDetailState withError(String error) {
        return new TransactionDetailState(this.transaction, false, error);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionDetailState)) return false;
        TransactionDetailState that = (TransactionDetailState) o;
        return isProcessing == that.isProcessing
                && (transaction != null
                ? transaction.equals(that.transaction)
                : that.transaction == null)
                && (error != null ? error.equals(that.error) : that.error == null);
    }

    @Override
    public int hashCode() {
        int result = transaction != null ? transaction.hashCode() : 0;
        result = 31 * result + (isProcessing ? 1 : 0);
        result = 31 * result + (error != null ? error.hashCode() : 0);
        return result;
    }
}
