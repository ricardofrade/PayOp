package com.rfrade.payop.presentation.di;

import com.rfrade.payop.presentation.detail.TransactionDetailActivity;
import com.rfrade.payop.presentation.list.TransactionListActivity;

public interface ComponentProvider {
    void inject(TransactionListActivity activity);

    void inject(TransactionDetailActivity activity);
}
