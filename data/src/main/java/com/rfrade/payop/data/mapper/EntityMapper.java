package com.rfrade.payop.data.mapper;

import com.rfrade.payop.data.local.Converters;
import com.rfrade.payop.data.local.TransactionEntity;
import com.rfrade.payop.domain.model.StateTransition;
import com.rfrade.payop.domain.model.Transaction;
import com.rfrade.payop.domain.model.TransactionState;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EntityMapper {

    @Inject
    public EntityMapper() {
    }

    public Transaction toDomain(TransactionEntity entity) {
        TransactionState state = TransactionState.valueOf(entity.state);
        List<StateTransition> log = Converters.toTransitionLog(entity.transitionLog);

        return new Transaction(
                entity.txnId,
                entity.requestedAmountCents,
                entity.approvedAmountCents,
                entity.captureAmountCents,
                state,
                entity.retryCount,
                entity.lastErrorCode,
                entity.createdAt,
                entity.updatedAt,
                log);
    }

    public TransactionEntity toEntity(Transaction txn) {
        TransactionEntity entity = new TransactionEntity();
        entity.txnId = txn.getTxnId();
        entity.requestedAmountCents = txn.getRequestedAmountCents();
        entity.approvedAmountCents = txn.getApprovedAmountCents();
        entity.captureAmountCents = txn.getCaptureAmountCents();
        entity.state = txn.getState().name();
        entity.retryCount = txn.getRetryCount();
        entity.lastErrorCode = txn.getLastErrorCode();
        entity.createdAt = txn.getCreatedAt();
        entity.updatedAt = txn.getUpdatedAt();
        entity.transitionLog = Converters.fromTransitionLog(txn.getTransitionLog());
        return entity;
    }

    public List<Transaction> toDomainList(List<TransactionEntity> entities) {
        List<Transaction> result = new ArrayList<>(entities.size());
        for (TransactionEntity entity : entities) {
            result.add(toDomain(entity));
        }
        return result;
    }
}
