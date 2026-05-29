package com.rfrade.payop.domain.bus;

public class TransactionCommandError {
    
    private final String txnId;
    private final String errorMessage;

    public TransactionCommandError(String txnId, String errorMessage) {
        this.txnId = txnId;
        this.errorMessage = errorMessage;
    }

    public String getTxnId() {
        return txnId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
