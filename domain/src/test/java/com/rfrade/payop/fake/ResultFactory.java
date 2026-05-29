package com.rfrade.payop.fake;

import com.rfrade.payop.domain.model.AuthResult;
import com.rfrade.payop.domain.model.OperationResult;

public final class ResultFactory {

    private ResultFactory() {
    }

    public static AuthResult authorizationApproved(long approvedAmount) {
        return new AuthResult(AuthResult.Outcome.APPROVED, approvedAmount, null);
    }

    public static AuthResult authorizationDeclined(String errorCode) {
        return new AuthResult(AuthResult.Outcome.DECLINED, 0, errorCode);
    }

    public static AuthResult authorizationTimedOut() {
        return new AuthResult(AuthResult.Outcome.TIMED_OUT, 0, "NETWORK_ERROR");
    }

    public static OperationResult operationApproved() {
        return new OperationResult(OperationResult.Outcome.APPROVED, null);
    }

    public static OperationResult operationDeclined(String errorCode) {
        return new OperationResult(OperationResult.Outcome.DECLINED, errorCode);
    }

    public static OperationResult operationTimedOut() {
        return new OperationResult(OperationResult.Outcome.TIMED_OUT, "NETWORK_ERROR");
    }
}
