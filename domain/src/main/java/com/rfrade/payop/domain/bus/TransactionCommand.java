package com.rfrade.payop.domain.bus;

public abstract class TransactionCommand {

    public static class Capture extends TransactionCommand {
        public final String txnId;
        public final long amountCents;

        public Capture(String txnId, long amountCents) {
            this.txnId = txnId;
            this.amountCents = amountCents;
        }
    }

    public static class Cancel extends TransactionCommand {
        public final String txnId;

        public Cancel(String txnId) {
            this.txnId = txnId;
        }
    }

    public static class Retry extends TransactionCommand {
        public final String txnId;

        public Retry(String txnId) {
            this.txnId = txnId;
        }
    }

    public static class NewAuthorization extends TransactionCommand {
        public final long amountCents;

        public NewAuthorization(long amountCents) {
            this.amountCents = amountCents;
        }
    }
}
