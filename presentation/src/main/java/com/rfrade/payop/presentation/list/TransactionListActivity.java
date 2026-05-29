package com.rfrade.payop.presentation.list;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.rfrade.payop.presentation.R;
import com.rfrade.payop.presentation.detail.TransactionDetailActivity;
import com.rfrade.payop.presentation.di.ComponentProvider;
import com.rfrade.payop.presentation.dialog.NewTransactionDialogFragment;
import com.rfrade.payop.presentation.util.TimeFormatter;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;

public class TransactionListActivity extends AppCompatActivity
        implements NewTransactionDialogFragment.OnAmountSubmittedListener {

    private final CompositeDisposable disposables = new CompositeDisposable();
    @Inject
    ViewModelProvider.Factory viewModelFactory;
    private TransactionListViewModel viewModel;
    private TransactionAdapter adapter;
    private Toolbar toolbar;
    private RecyclerView recyclerTransactions;
    private FloatingActionButton fabNewTransaction;
    private TextView textNetworkError;
    private TextView textEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_list);

        ((ComponentProvider) getApplication()).inject(this);
        viewModel = new ViewModelProvider(this, viewModelFactory).get(TransactionListViewModel.class);

        bindViews();
        setupToolbar();
        setupRecyclerView();
        setupFab();
        observeViewModel();

        viewModel.processIntent(TransactionListIntent.LoadTransactions.INSTANCE);
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerTransactions = findViewById(R.id.recycler_transactions);
        fabNewTransaction = findViewById(R.id.fab_new_transaction);
        textNetworkError = findViewById(R.id.text_network_error);
        textEmpty = findViewById(R.id.text_empty);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(transaction -> {
            Intent intent = new Intent(this, TransactionDetailActivity.class);
            intent.putExtra(TransactionDetailActivity.EXTRA_TXN_ID, transaction.getTxnId());
            startActivity(intent);
        });
        recyclerTransactions.setLayoutManager(new LinearLayoutManager(this));
        recyclerTransactions.setAdapter(adapter);
    }

    private void setupFab() {
        fabNewTransaction.setOnClickListener(v ->
                new NewTransactionDialogFragment()
                        .show(getSupportFragmentManager(), "new_txn")
        );
    }

    private void observeViewModel() {
        viewModel.states().observe(this, this::render);
    }

    private void render(TransactionListState state) {
        adapter.submitList(state.getTransactions());

        if (state.getLastNetworkErrorMs() != null) {
            textNetworkError.setVisibility(View.VISIBLE);
            String time = TimeFormatter.format(state.getLastNetworkErrorMs());
            textNetworkError.setText("Offline — SDK Network Error at " + time);
        } else {
            textNetworkError.setVisibility(View.GONE);
        }

        if (state.getTransactions().isEmpty() && !state.isLoading()) {
            textEmpty.setVisibility(View.VISIBLE);
        } else {
            textEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAmountSubmitted(long amountCents) {
        viewModel.processIntent(new TransactionListIntent.NewAuthorization(amountCents));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}
