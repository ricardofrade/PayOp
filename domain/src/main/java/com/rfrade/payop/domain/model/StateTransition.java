package com.rfrade.payop.domain.model;

public final class StateTransition {

    private final TransactionState fromState;
    private final TransactionState toState;
    private final long timestamp;
    private final String errorCode;
    private final String detail;

    public StateTransition(
            TransactionState fromState,
            TransactionState toState,
            long timestamp,
            String errorCode,
            String detail) {
        this.fromState = fromState;
        this.toState = toState;
        this.timestamp = timestamp;
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public TransactionState getFromState() {
        return fromState;
    }

    public TransactionState getToState() {
        return toState;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public String toString() {
        return fromState
                + " → "
                + toState
                + " at "
                + timestamp
                + (errorCode != null ? " [" + errorCode + "]" : "")
                + (detail != null ? " (" + detail + ")" : "");
    }
}
