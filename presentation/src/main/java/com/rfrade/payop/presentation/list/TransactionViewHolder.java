package com.rfrade.payop.presentation.list;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.rfrade.payop.domain.model.Transaction;
import com.rfrade.payop.domain.model.TransactionState;
import com.rfrade.payop.presentation.R;
import com.rfrade.payop.presentation.util.CurrencyFormatter;
import com.rfrade.payop.presentation.util.TimeFormatter;

public class TransactionViewHolder extends RecyclerView.ViewHolder {
    private TextView textTxnId;
    private TextView textState;
    private TextView textAmount;
    private TextView textTimestamp;

    public TransactionViewHolder(View view) {
        super(view);
        bindViews(view);
    }

    private void bindViews(View view) {
        textTxnId = view.findViewById(R.id.text_txn_id);
        textState = view.findViewById(R.id.text_state);
        textAmount = view.findViewById(R.id.text_amount);
        textTimestamp = view.findViewById(R.id.text_timestamp);
    }

    public void bind(Transaction txn, TransactionAdapter.OnTransactionClickListener listener) {

        String idAbbr = txn.getTxnId().length() > 8
                ? txn.getTxnId().substring(0, 8) + "…"
                : txn.getTxnId();
        textTxnId.setText(idAbbr);

        String stateLabel = txn.getState().name();
        if ((txn.getState() == TransactionState.CAPTURING
                || txn.getState() == TransactionState.CANCELLING)
                && txn.getRetryCount() > 0) {
            stateLabel += " (#" + txn.getRetryCount() + ")";
        }
        textState.setText(stateLabel);

        GradientDrawable bg = (GradientDrawable) textState.getBackground().mutate();
        bg.setColor(getStateColor(txn.getState()));
        textState.setTextColor(Color.WHITE);

        textAmount.setText(CurrencyFormatter.formatCents(txn.getRequestedAmountCents()));

        textTimestamp.setText(TimeFormatter.format(txn.getUpdatedAt()));

        itemView.setOnClickListener(v -> listener.onClick(txn));
    }

    private int getStateColor(TransactionState state) {
        switch (state) {
            case AUTHORIZING:
            case CAPTURING:
            case CANCELLING:
                return Color.parseColor("#FF9800");
            case AUTHORIZED:
                return Color.parseColor("#2196F3");
            case CAPTURED:
                return Color.parseColor("#4CAF50");
            case CANCELLED:
                return Color.parseColor("#9E9E9E");
            case DECLINED:
                return Color.parseColor("#F44336");
            case CAPTURE_FAILED:
            case CANCEL_FAILED:
                return Color.parseColor("#D32F2F");
            default:
                return Color.GRAY;
        }
    }
}
