package com.winllc.acme.server.transaction;

public abstract class AbstractTransaction {
    TransactionContext transactionContext;

    AbstractTransaction(TransactionContext transactionContext){
        this.transactionContext = transactionContext;
    }

    public TransactionContext getTransactionContext() {
        return transactionContext;
    }
}
