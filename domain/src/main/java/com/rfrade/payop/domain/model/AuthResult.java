package com.rfrade.payop.domain.model;

public final class AuthResult {

    private final Outcome outcome;
    private final long approvedAmount;
    private final String errorCode;

    public AuthResult(Outcome outcome, long approvedAmount, String errorCode) {
        this.outcome = outcome;
        this.approvedAmount = approvedAmount;
        this.errorCode = errorCode;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public long getApprovedAmount() {
        return approvedAmount;
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
