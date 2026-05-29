package com.rfrade.payop.presentation.list;

import com.rfrade.payop.domain.model.Transaction;

import java.util.Collections;
import java.util.List;

public final class TransactionListState {

    private final List<Transaction> transactions;
    private final boolean isLoading;
    private final String error;
    private final Long lastNetworkErrorMs;

    public TransactionListState(
            List<Transaction> transactions,
            boolean isLoading,
            String error,
            Long lastNetworkErrorMs) {
        this.transactions = transactions != null
                ? Collections.unmodifiableList(transactions)
                : Collections.<Transaction>emptyList();
        this.isLoading = isLoading;
        this.error = error;
        this.lastNetworkErrorMs = lastNetworkErrorMs;
    }

    public static TransactionListState initial() {
        return new TransactionListState(Collections.<Transaction>emptyList(), true, null, null);
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public String getError() {
        return error;
    }

    public Long getLastNetworkErrorMs() {
        return lastNetworkErrorMs;
    }

    public TransactionListState withTransactions(List<Transaction> transactions) {
        return new TransactionListState(transactions, false, null, this.lastNetworkErrorMs);
    }

    public TransactionListState withLoading() {
        return new TransactionListState(this.transactions, true, null, this.lastNetworkErrorMs);
    }

    public TransactionListState withError(String error) {
        return new TransactionListState(this.transactions, false, error, this.lastNetworkErrorMs);
    }

    public TransactionListState withLastNetworkError(Long lastNetworkErrorMs) {
        return new TransactionListState(
                this.transactions, this.isLoading, this.error, lastNetworkErrorMs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionListState)) return false;
        TransactionListState that = (TransactionListState) o;
        return isLoading == that.isLoading
                && transactions.equals(that.transactions)
                && (error != null ? error.equals(that.error) : that.error == null)
                && (lastNetworkErrorMs != null
                ? lastNetworkErrorMs.equals(that.lastNetworkErrorMs)
                : that.lastNetworkErrorMs == null);
    }

    @Override
    public int hashCode() {
        int result = transactions.hashCode();
        result = 31 * result + (isLoading ? 1 : 0);
        result = 31 * result + (error != null ? error.hashCode() : 0);
        result = 31 * result + (lastNetworkErrorMs != null ? lastNetworkErrorMs.hashCode() : 0);
        return result;
    }
}
