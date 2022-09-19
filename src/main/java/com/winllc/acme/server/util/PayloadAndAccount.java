package com.winllc.acme.server.util;

import com.winllc.acme.common.model.data.AccountData;
import com.winllc.acme.common.model.data.DirectoryData;

public class PayloadAndAccount<T> {
    private final T payload;
    private final AccountData accountData;
    private final DirectoryData directoryData;

    public PayloadAndAccount(T payload, AccountData accountData, DirectoryData directoryData) {
        this.payload = payload;
        this.accountData = accountData;
        this.directoryData = directoryData;
    }

    public T getPayload() {
        return payload;
    }

    public AccountData getAccountData() {
        return accountData;
    }

    public DirectoryData getDirectoryData() {
        return directoryData;
    }
}
