package com.rfrade.payop.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class Transaction {

    private final String txnId;
    private final long requestedAmountCents;
    private final long approvedAmountCents;
    private final long captureAmountCents;
    private final TransactionState state;
    private final int retryCount;
    private final String lastErrorCode;
    private final long createdAt;
    private final long updatedAt;
    private final List<StateTransition> transitionLog;

    public Transaction(
            String txnId,
            long requestedAmountCents,
            long approvedAmountCents,
            long captureAmountCents,
            TransactionState state,
            int retryCount,
            String lastErrorCode,
            long createdAt,
            long updatedAt,
            List<StateTransition> transitionLog) {
        this.txnId = txnId;
        this.requestedAmountCents = requestedAmountCents;
        this.approvedAmountCents = approvedAmountCents;
        this.captureAmountCents = captureAmountCents;
        this.state = state;
        this.retryCount = retryCount;
        this.lastErrorCode = lastErrorCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.transitionLog =
                transitionLog != null
                        ? Collections.unmodifiableList(new ArrayList<>(transitionLog))
                        : Collections.<StateTransition>emptyList();
    }

    public static Transaction createNew(long amountCents) {
        String id = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        List<StateTransition> log = new ArrayList<>();
        log.add(new StateTransition(null, TransactionState.AUTHORIZING, now, null, "Created"));
        return new Transaction(
                id, amountCents, 0, 0, TransactionState.AUTHORIZING, 0, null, now, now, log);
    }

    public String getTxnId() {
        return txnId;
    }

    public long getRequestedAmountCents() {
        return requestedAmountCents;
    }

    public long getApprovedAmountCents() {
        return approvedAmountCents;
    }

    public long getCaptureAmountCents() {
        return captureAmountCents;
    }

    public TransactionState getState() {
        return state;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public List<StateTransition> getTransitionLog() {
        return transitionLog;
    }

    public Transaction withState(TransactionState newState, String errorCode, String detail) {
        List<StateTransition> newLog = new ArrayList<>(this.transitionLog);
        long now = System.currentTimeMillis();
        newLog.add(new StateTransition(this.state, newState, now, errorCode, detail));
        return new Transaction(
                txnId,
                requestedAmountCents,
                approvedAmountCents,
                captureAmountCents,
                newState,
                retryCount,
                errorCode != null ? errorCode : lastErrorCode,
                createdAt,
                now,
                newLog);
    }

    public Transaction withApprovedAmount(long approved) {
        return new Transaction(
                txnId,
                requestedAmountCents,
                approved,
                captureAmountCents,
                state,
                retryCount,
                lastErrorCode,
                createdAt,
                updatedAt,
                transitionLog);
    }

    public Transaction withCaptureAmount(long capture) {
        return new Transaction(
                txnId,
                requestedAmountCents,
                approvedAmountCents,
                capture,
                state,
                retryCount,
                lastErrorCode,
                createdAt,
                updatedAt,
                transitionLog);
    }

    public Transaction withRetryCount(int count) {
        return new Transaction(
                txnId,
                requestedAmountCents,
                approvedAmountCents,
                captureAmountCents,
                state,
                count,
                lastErrorCode,
                createdAt,
                System.currentTimeMillis(),
                transitionLog);
    }

    public Transaction withRetryIncrement() {
        return withRetryCount(this.retryCount + 1);
    }

    @Override
    public String toString() {
        return "Transaction{"
                + txnId.substring(0, Math.min(8, txnId.length()))
                + " state="
                + state
                + " req="
                + requestedAmountCents
                + " appr="
                + approvedAmountCents
                + " retry="
                + retryCount
                + "}";
    }
}
