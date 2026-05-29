package com.rfrade.payop.presentation.list;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.rfrade.payop.domain.model.Transaction;

import java.util.Objects;

public class TransactionDiffCallback extends DiffUtil.ItemCallback<Transaction> {
    @Override
    public boolean areItemsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
        return Objects.equals(oldItem.getTxnId(), newItem.getTxnId());
    }

    @Override
    public boolean areContentsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
        return Objects.equals(oldItem.getState(), newItem.getState())
                && oldItem.getRetryCount() == newItem.getRetryCount()
                && oldItem.getUpdatedAt() == newItem.getUpdatedAt();
    }
}
