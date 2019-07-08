package com.winllc.acme.server.util;

import com.winllc.acme.server.model.data.AccountData;

public class PayloadAndAccount<T> {
    private T payload;
    private AccountData accountData;

    public PayloadAndAccount(T payload, AccountData accountData) {
        this.payload = payload;
        this.accountData = accountData;
    }

    public T getPayload() {
        return payload;
    }

    public AccountData getAccountData() {
        return accountData;
    }
}
