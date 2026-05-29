package com.rfrade.payop.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;

@Dao
public interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    Flowable<List<TransactionEntity>> observeAll();

    @Query("SELECT * FROM transactions WHERE txnId = :txnId")
    Single<TransactionEntity> getById(String txnId);

    @Query("SELECT * FROM transactions WHERE txnId = :txnId")
    TransactionEntity getByIdSync(String txnId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(TransactionEntity entity);

    @Transaction
    default void upsertAndTrim(TransactionEntity entity, int limit) {
        upsert(entity);
        trimOlderThan(limit);
    }

    @Query(
            "SELECT * FROM transactions WHERE state IN ('AUTHORIZING', 'CAPTURING', 'CANCELLING')"
                    + " ORDER BY createdAt ASC")
    List<TransactionEntity> getInFlightSync();

    @Query(
            "DELETE FROM transactions WHERE txnId NOT IN "
                    + "(SELECT txnId FROM transactions ORDER BY createdAt DESC LIMIT :limit)")
    void trimOlderThan(int limit);
}
