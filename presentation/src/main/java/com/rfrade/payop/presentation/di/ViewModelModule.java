package com.rfrade.payop.presentation.di;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.rfrade.payop.presentation.ViewModelFactory;
import com.rfrade.payop.presentation.detail.TransactionDetailViewModel;
import com.rfrade.payop.presentation.list.TransactionListViewModel;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public abstract class ViewModelModule {

    @Binds
    public abstract ViewModelProvider.Factory bindViewModelFactory(ViewModelFactory factory);

    @Binds
    @IntoMap
    @ViewModelKey(TransactionListViewModel.class)
    public abstract ViewModel bindTransactionListViewModel(TransactionListViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(TransactionDetailViewModel.class)
    public abstract ViewModel bindTransactionDetailViewModel(TransactionDetailViewModel viewModel);
}
