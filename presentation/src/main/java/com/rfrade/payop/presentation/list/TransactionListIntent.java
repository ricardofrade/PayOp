package com.rfrade.payop.presentation.list;

public abstract class TransactionListIntent {

    private TransactionListIntent() {

    }

    public static final class LoadTransactions extends TransactionListIntent {
        public static final LoadTransactions INSTANCE = new LoadTransactions();

        private LoadTransactions() {
        }
    }

    public static final class NewAuthorization extends TransactionListIntent {
        private final long amountCents;

        public NewAuthorization(long amountCents) {
            this.amountCents = amountCents;
        }

        public long getAmountCents() {
            return amountCents;
        }
    }
}
