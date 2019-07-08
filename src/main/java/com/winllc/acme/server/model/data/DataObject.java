package com.winllc.acme.server.model.data;

import com.winllc.acme.server.util.AppUtil;

public abstract class DataObject<T> {
    private String id;
    private T object;
    private String accountId;

    public DataObject(T obj) {
        this.id = AppUtil.generateRandomString(10);
        this.object = obj;
    }

    public DataObject(T obj, AccountData accountData) {
        this.id = AppUtil.generateRandomString(10);
        this.object = obj;
        this.accountId = accountData.getId();
    }

    public abstract String buildUrl();

    public T getObject(){
        return this.object;
    }

    public DataObject<T> updateObject(T object){
        this.object = object;
        return this;
    }

    public String getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }
}
