package com.rfrade.payop.presentation.detail;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.rfrade.payop.domain.model.Transaction;
import com.rfrade.payop.domain.model.TransactionState;
import com.rfrade.payop.presentation.R;
import com.rfrade.payop.presentation.di.ComponentProvider;
import com.rfrade.payop.presentation.util.CurrencyFormatter;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;

public class TransactionDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TXN_ID = "extra_txn_id";
    private final CompositeDisposable disposables = new CompositeDisposable();
    @Inject
    ViewModelProvider.Factory viewModelFactory;
    private TransactionDetailViewModel viewModel;
    private TransitionLogAdapter logAdapter;
    private String txnId;

    private Toolbar toolbarDetail;
    private RecyclerView recyclerTransitionLog;
    private MaterialButton btnCapture;
    private MaterialButton btnCancel;
    private MaterialButton btnRetry;
    private TextView textDetailTxnId;
    private TextView textDetailState;
    private TextView textDetailRequested;
    private TextView textDetailApproved;
    private TextView textDetailCaptured;
    private TextView textDetailError;
    private TextView textDetailRetryCount;
    private TextInputLayout inputCaptureAmountLayout;
    private EditText inputCaptureAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);

        ((ComponentProvider) getApplication()).inject(this);
        viewModel = new ViewModelProvider(this, viewModelFactory).get(TransactionDetailViewModel.class);

        txnId = getIntent().getStringExtra(EXTRA_TXN_ID);
        if (txnId == null) {
            finish();
            return;
        }

        bindViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        observeViewModel();

        viewModel.processIntent(new TransactionDetailIntent.LoadDetail(txnId));
    }

    private void bindViews() {
        toolbarDetail = findViewById(R.id.toolbar_detail);
        recyclerTransitionLog = findViewById(R.id.recycler_transition_log);
        btnCapture = findViewById(R.id.btn_capture);
        btnCancel = findViewById(R.id.btn_cancel);
        btnRetry = findViewById(R.id.btn_retry);
        textDetailTxnId = findViewById(R.id.text_detail_txn_id);
        textDetailState = findViewById(R.id.text_detail_state);
        textDetailRequested = findViewById(R.id.text_detail_requested);
        textDetailApproved = findViewById(R.id.text_detail_approved);
        textDetailCaptured = findViewById(R.id.text_detail_captured);
        textDetailError = findViewById(R.id.text_detail_error);
        textDetailRetryCount = findViewById(R.id.text_detail_retry_count);
        inputCaptureAmountLayout = findViewById(R.id.input_capture_amount_layout);
        inputCaptureAmount = findViewById(R.id.input_capture_amount);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbarDetail);
        toolbarDetail.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        logAdapter = new TransitionLogAdapter();
        recyclerTransitionLog.setLayoutManager(new LinearLayoutManager(this));
        recyclerTransitionLog.setAdapter(logAdapter);
    }

    private void setupListeners() {
        btnCapture.setOnClickListener(v -> onCaptureClicked());
        btnCancel.setOnClickListener(v -> viewModel.processIntent(new TransactionDetailIntent.Cancel(txnId)));
        btnRetry.setOnClickListener(v -> viewModel.processIntent(new TransactionDetailIntent.Retry(txnId)));
    }

    private void observeViewModel() {
        viewModel.states().observe(this, this::render);
    }

    private void render(TransactionDetailState state) {
        Transaction txn = state.getTransaction();
        if (txn == null) return;

        textDetailTxnId.setText("ID: " + txn.getTxnId());

        String stateLabel = txn.getState().name();
        if ((txn.getState() == TransactionState.CAPTURING
                || txn.getState() == TransactionState.CANCELLING)
                && txn.getRetryCount() > 0) {
            stateLabel += " (#" + txn.getRetryCount() + ")";
        }
        textDetailState.setText(stateLabel);

        textDetailRequested.setText(CurrencyFormatter.formatCents(txn.getRequestedAmountCents()));
        textDetailApproved.setText(CurrencyFormatter.formatCents(txn.getApprovedAmountCents()));
        textDetailCaptured.setText(CurrencyFormatter.formatCents(txn.getCaptureAmountCents()));

        if (txn.getLastErrorCode() != null && !txn.getLastErrorCode().isEmpty()) {
            textDetailError.setVisibility(View.VISIBLE);
            textDetailError.setText("Last error: " + txn.getLastErrorCode());
        } else {
            textDetailError.setVisibility(View.GONE);
        }

        if (txn.getRetryCount() > 0) {
            textDetailRetryCount.setVisibility(View.VISIBLE);
            textDetailRetryCount.setText("Retry count: " + txn.getRetryCount());
        } else {
            textDetailRetryCount.setVisibility(View.GONE);
        }

        btnCapture.setVisibility(state.isCaptureEnabled() ? View.VISIBLE : View.GONE);
        inputCaptureAmountLayout.setVisibility(state.isCaptureEnabled() ? View.VISIBLE : View.GONE);
        btnCancel.setVisibility(state.isCancelEnabled() ? View.VISIBLE : View.GONE);
        btnRetry.setVisibility(state.isRetryEnabled() ? View.VISIBLE : View.GONE);

        if (state.isCaptureEnabled() && inputCaptureAmount.getText().toString().isEmpty()) {
            inputCaptureAmount.setText(String.valueOf(txn.getApprovedAmountCents()));
        }

        logAdapter.submitList(txn.getTransitionLog());

        if (state.getError() != null) {
            Toast.makeText(this, state.getError(), Toast.LENGTH_LONG).show();
        }
    }

    private void onCaptureClicked() {
        String text = inputCaptureAmount.getText().toString().trim();
        if (text.isEmpty()) {
            inputCaptureAmountLayout.setError("Enter amount");
            return;
        }
        try {
            long amount = Long.parseLong(text);
            if (amount <= 0) {
                inputCaptureAmountLayout.setError("Amount must be > 0");
                return;
            }
            inputCaptureAmountLayout.setError(null);
            viewModel.processIntent(new TransactionDetailIntent.Capture(txnId, amount));
        } catch (NumberFormatException e) {
            inputCaptureAmountLayout.setError("Invalid number");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}
