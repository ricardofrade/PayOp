package com.rfrade.payop.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class TransactionEntity {

    @PrimaryKey
    @NonNull
    public String txnId;

    public long requestedAmountCents;

    public long approvedAmountCents;

    public long captureAmountCents;

    @NonNull
    public String state;

    public int retryCount;

    @Nullable
    public String lastErrorCode;

    public long createdAt;

    public long updatedAt;

    @NonNull
    public String transitionLog;

    public TransactionEntity() {
        this.txnId = "";
        this.state = "AUTHORIZING";
        this.transitionLog = "[]";
    }
}
