package com.winllc.acme.server.util;

import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.DirectoryData;

public class PayloadAndAccount<T> {
    private T payload;
    private AccountData accountData;
    private DirectoryData directoryData;

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
