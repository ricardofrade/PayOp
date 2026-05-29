package com.rfrade.payop.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.rfrade.payop.data.local.TransactionEntity;
import com.rfrade.payop.data.mapper.EntityMapper;
import com.rfrade.payop.domain.model.StateTransition;
import com.rfrade.payop.domain.model.Transaction;
import com.rfrade.payop.domain.model.TransactionState;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class EntityMapperTest {

    private EntityMapper mapper;

    @Before
    public void setUp() {
        mapper = new EntityMapper();
    }

    @Test
    public void round_trip_preserves_all_fields() {
        Transaction original =
                Transaction.createNew(1500)
                        .withApprovedAmount(1200)
                        .withState(TransactionState.AUTHORIZED, null, "Approved for 1200")
                        .withCaptureAmount(800);

        TransactionEntity entity = mapper.toEntity(original);
        Transaction restored = mapper.toDomain(entity);

        assertEquals(original.getTxnId(), restored.getTxnId());
        assertEquals(original.getRequestedAmountCents(), restored.getRequestedAmountCents());
        assertEquals(original.getApprovedAmountCents(), restored.getApprovedAmountCents());
        assertEquals(original.getCaptureAmountCents(), restored.getCaptureAmountCents());
        assertEquals(original.getState(), restored.getState());
    }

    @Test
    public void transition_log_survives_round_trip() {
        Transaction txn =
                Transaction.createNew(1000)
                        .withState(TransactionState.AUTHORIZED, null, "Approved")
                        .withState(TransactionState.CAPTURING, null, "Capturing")
                        .withState(TransactionState.CAPTURED, null, "Done");

        TransactionEntity entity = mapper.toEntity(txn);
        Transaction restored = mapper.toDomain(entity);

        assertEquals(txn.getTransitionLog().size(), restored.getTransitionLog().size());
        for (int i = 0; i < txn.getTransitionLog().size(); i++) {
            StateTransition orig = txn.getTransitionLog().get(i);
            StateTransition rest = restored.getTransitionLog().get(i);
            assertEquals(orig.getFromState(), rest.getFromState());
            assertEquals(orig.getToState(), rest.getToState());
        }
    }

    @Test
    public void error_code_preserved() {
        Transaction txn =
                Transaction.createNew(500)
                        .withState(TransactionState.DECLINED, "INSUFFICIENT_FUNDS", "Not enough");

        TransactionEntity entity = mapper.toEntity(txn);
        Transaction restored = mapper.toDomain(entity);

        assertEquals("INSUFFICIENT_FUNDS", restored.getLastErrorCode());
    }

    @Test
    public void null_error_code_preserved() {
        Transaction txn =
                Transaction.createNew(500).withState(TransactionState.AUTHORIZED, null, "Approved");

        TransactionEntity entity = mapper.toEntity(txn);
        Transaction restored = mapper.toDomain(entity);

        assertNull(restored.getLastErrorCode());
    }

    @Test
    public void to_domain_list_maps_all() {
        List<TransactionEntity> entities = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            entities.add(mapper.toEntity(Transaction.createNew(100 * (i + 1))));
        }

        List<Transaction> transactions = mapper.toDomainList(entities);
        assertEquals(5, transactions.size());
    }
}
