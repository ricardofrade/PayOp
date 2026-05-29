package com.rfrade.payop.domain.model;

public final class OperationResult {

    private final Outcome outcome;
    private final String errorCode;

    public OperationResult(Outcome outcome, String errorCode) {
        this.outcome = outcome;
        this.errorCode = errorCode;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public enum Outcome {
        APPROVED,
        DECLINED,
        TIMED_OUT
    }
}
