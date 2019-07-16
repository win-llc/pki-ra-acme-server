package com.winllc.acme.server.model.data;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.util.AppUtil;

public abstract class DataObject<T> {
    private String id;
    private T object;
    private String accountId;
    private String directory;

    public DataObject(T obj) {
        this.id = AppUtil.generateRandomString(10);
        this.object = obj;
        this.directory = "";
    }

    public DataObject(T obj, DirectoryData directory) {
        this.id = AppUtil.generateRandomString(10);
        this.object = obj;
        this.directory = directory.getName();
    }

    public DataObject(T obj, DirectoryData directory, AccountData accountData) {
        this.id = AppUtil.generateRandomString(10);
        this.object = obj;
        this.accountId = accountData.getId();
        this.directory = directory.getName();
    }

    public abstract String buildUrl();

    protected String buildBaseUrl(){
        return Application.baseURL + directory + "/";
    }

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

    public String getDirectory() {
        return directory;
    }
}
