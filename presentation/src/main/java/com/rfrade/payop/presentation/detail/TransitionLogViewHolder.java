package com.rfrade.payop.presentation.detail;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.rfrade.payop.domain.model.StateTransition;
import com.rfrade.payop.presentation.R;
import com.rfrade.payop.presentation.util.TimeFormatter;

public class TransitionLogViewHolder extends RecyclerView.ViewHolder {
    private TextView textLogTransition;
    private TextView textLogDetail;
    private TextView textLogTimestamp;

    public TransitionLogViewHolder(View view) {
        super(view);
        bindViews(view);
    }

    private void bindViews(View view) {
        textLogTransition = view.findViewById(R.id.text_log_transition);
        textLogDetail = view.findViewById(R.id.text_log_detail);
        textLogTimestamp = view.findViewById(R.id.text_log_timestamp);
    }

    public void bind(StateTransition transition) {
        String from = transition.getFromState() != null ? transition.getFromState().name() : "—";
        textLogTransition.setText(from + " → " + transition.getToState().name());

        if (transition.getDetail() != null && !transition.getDetail().isEmpty()) {
            textLogDetail.setVisibility(View.VISIBLE);
            String detailText = transition.getDetail();
            if (transition.getErrorCode() != null) {
                detailText += " [" + transition.getErrorCode() + "]";
            }
            textLogDetail.setText(detailText);
        } else {
            textLogDetail.setVisibility(View.GONE);
        }

        textLogTimestamp.setText(TimeFormatter.format(transition.getTimestamp()));
    }
}
