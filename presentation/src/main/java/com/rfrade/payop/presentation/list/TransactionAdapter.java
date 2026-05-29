package com.rfrade.payop.presentation.list;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;

import com.rfrade.payop.domain.model.Transaction;
import com.rfrade.payop.presentation.R;

public class TransactionAdapter extends ListAdapter<Transaction, TransactionViewHolder> {

    private final OnTransactionClickListener listener;

    public TransactionAdapter(OnTransactionClickListener listener) {
        super(new TransactionDiffCallback());
        this.listener = listener;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    public interface OnTransactionClickListener {
        void onClick(Transaction transaction);
    }
}
