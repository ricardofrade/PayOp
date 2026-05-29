package com.rfrade.payop.presentation.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.rfrade.payop.presentation.R;

public class NewTransactionDialogFragment extends DialogFragment {

    private OnAmountSubmittedListener listener;
    private EditText inputAmount;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnAmountSubmittedListener) {
            listener = (OnAmountSubmittedListener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement OnAmountSubmittedListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.dialog_new_transaction, null);
        bindViews(view);

        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .setPositiveButton("Authorize", (dialog, which) -> {
                    String text = inputAmount.getText().toString().trim();
                    if (!text.isEmpty()) {
                        try {
                            long amount = Long.parseLong(text);
                            if (amount > 0 && listener != null) {
                                listener.onAmountSubmitted(amount);
                            }
                        } catch (NumberFormatException e) {

                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
    }

    private void bindViews(View view) {
        inputAmount = view.findViewById(R.id.input_amount);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public interface OnAmountSubmittedListener {
        void onAmountSubmitted(long amountCents);
    }
}
