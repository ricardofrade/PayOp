package com.rfrade.payop.presentation.detail;

public abstract class TransactionDetailIntent {

    private TransactionDetailIntent() {
    }

    public static final class LoadDetail extends TransactionDetailIntent {
        private final String txnId;

        public LoadDetail(String txnId) {
            this.txnId = txnId;
        }

        public String getTxnId() {
            return txnId;
        }
    }

    public static final class Capture extends TransactionDetailIntent {
        private final String txnId;
        private final long amountCents;

        public Capture(String txnId, long amountCents) {
            this.txnId = txnId;
            this.amountCents = amountCents;
        }

        public String getTxnId() {
            return txnId;
        }

        public long getAmountCents() {
            return amountCents;
        }
    }

    public static final class Cancel extends TransactionDetailIntent {
        private final String txnId;

        public Cancel(String txnId) {
            this.txnId = txnId;
        }

        public String getTxnId() {
            return txnId;
        }
    }

    public static final class Retry extends TransactionDetailIntent {
        private final String txnId;

        public Retry(String txnId) {
            this.txnId = txnId;
        }

        public String getTxnId() {
            return txnId;
        }
    }
}
