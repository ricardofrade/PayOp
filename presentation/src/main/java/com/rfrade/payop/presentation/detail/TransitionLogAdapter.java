package com.rfrade.payop.presentation.detail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;

import com.rfrade.payop.domain.model.StateTransition;
import com.rfrade.payop.presentation.R;

public class TransitionLogAdapter extends ListAdapter<StateTransition, TransitionLogViewHolder> {

    public TransitionLogAdapter() {
        super(new TransitionDiffCallback());
    }

    @NonNull
    @Override
    public TransitionLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transition_log, parent, false);
        return new TransitionLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransitionLogViewHolder holder, int position) {
        holder.bind(getItem(position));
    }
}
