package com.rfrade.payop.data.di;

import android.app.Application;

import androidx.room.Room;

import com.rfrade.payop.data.local.AppDatabase;
import com.rfrade.payop.data.local.TransactionDao;
import com.rfrade.payop.data.repository.ConnectionHealthRepositoryImpl;
import com.rfrade.payop.data.repository.TransactionRepositoryImpl;
import com.rfrade.payop.domain.repository.ConnectionHealthRepository;
import com.rfrade.payop.domain.repository.TransactionRepository;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;

@Module
public abstract class DataModule {

    @Provides
    @Singleton
    static AppDatabase provideDatabase(Application application) {
        return Room.databaseBuilder(application, AppDatabase.class, AppDatabase.DATABASE_NAME)
                .build();
    }

    @Provides
    @Singleton
    static TransactionDao provideTransactionDao(AppDatabase database) {
        return database.transactionDao();
    }

    @Binds
    @Singleton
    abstract TransactionRepository bindTransactionRepository(TransactionRepositoryImpl impl);

    @Binds
    @Singleton
    abstract ConnectionHealthRepository bindConnectionHealthRepository(
            ConnectionHealthRepositoryImpl impl);
}
