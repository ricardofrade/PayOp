package com.rfrade.payop.domain.model;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TransactionStateMachine {

    @Inject
    public TransactionStateMachine() {
    }

    public boolean isValidTransition(TransactionState from, TransactionState to) {
        if (from == null) {
            return to == TransactionState.AUTHORIZING;
        }
        if (from.isTerminal()) {
            return false;
        }
        switch (from) {
            case AUTHORIZING:
                return to == TransactionState.AUTHORIZED
                        || to == TransactionState.DECLINED
                        || to == TransactionState.CANCELLING;
            case AUTHORIZED:
                return to == TransactionState.CAPTURING || to == TransactionState.CANCELLING;
            case CAPTURING:
                return to == TransactionState.CAPTURED || to == TransactionState.CAPTURE_FAILED;
            case CANCELLING:
                return to == TransactionState.CANCELLED || to == TransactionState.CANCEL_FAILED;
            default:
                return false;
        }
    }

    public TransactionState resolveAuthorizationResult(AuthResult result) {
        switch (result.getOutcome()) {
            case APPROVED:
                return TransactionState.AUTHORIZED;
            case TIMED_OUT:
                return TransactionState.CANCELLING;
            case DECLINED:
            default:
                return TransactionState.DECLINED;
        }
    }

    public TransactionState resolveCaptureResult(OperationResult result) {
        switch (result.getOutcome()) {
            case APPROVED:
                return TransactionState.CAPTURED;
            case TIMED_OUT:
                return null;
            case DECLINED:
            default:

                if ("ALREADY_CAPTURED".equals(result.getErrorCode())) {
                    return TransactionState.CAPTURED;
                }
                return TransactionState.CAPTURE_FAILED;
        }
    }

    public TransactionState resolveCancelResult(OperationResult result) {
        switch (result.getOutcome()) {
            case APPROVED:
                return TransactionState.CANCELLED;
            case TIMED_OUT:
                return null;
            case DECLINED:
            default:
                String err = result.getErrorCode();
                if ("UNKNOWN_TRANSACTION_ID".equals(err) || "ALREADY_CANCELLED".equals(err)) {
                    return TransactionState.CANCELLED;
                }
                return TransactionState.CANCEL_FAILED;
        }
    }
}
