package com.rfrade.payop.domain.model;

public enum TransactionState {

    AUTHORIZING,

    AUTHORIZED,

    CAPTURING,

    CAPTURED,

    CANCELLING,

    CANCELLED,

    DECLINED,

    CAPTURE_FAILED,

    CANCEL_FAILED;

    public boolean isTerminal() {
        switch (this) {
            case CAPTURED:
            case CANCELLED:
            case DECLINED:
            case CAPTURE_FAILED:
            case CANCEL_FAILED:
                return true;
            default:
                return false;
        }
    }

    public boolean allowsCapture() {
        return this == AUTHORIZED || this == CAPTURING;
    }

    public boolean allowsCancel() {
        return this == AUTHORIZED || this == AUTHORIZING || this == CANCELLING;
    }

    public boolean isRetryable() {
        return this == CAPTURE_FAILED || this == CANCEL_FAILED;
    }
}
