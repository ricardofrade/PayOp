package com.rfrade.payop.presentation.detail;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.rfrade.payop.domain.model.StateTransition;

import java.util.Objects;

public class TransitionDiffCallback extends DiffUtil.ItemCallback<StateTransition> {
    @Override
    public boolean areItemsTheSame(@NonNull StateTransition oldItem, @NonNull StateTransition newItem) {
        return oldItem.getTimestamp() == newItem.getTimestamp()
                && oldItem.getToState() == newItem.getToState();
    }

    @Override
    public boolean areContentsTheSame(@NonNull StateTransition oldItem, @NonNull StateTransition newItem) {
        return Objects.equals(oldItem.getFromState(), newItem.getFromState())
                && Objects.equals(oldItem.getToState(), newItem.getToState())
                && oldItem.getTimestamp() == newItem.getTimestamp()
                && Objects.equals(oldItem.getErrorCode(), newItem.getErrorCode())
                && Objects.equals(oldItem.getDetail(), newItem.getDetail());
    }
}
