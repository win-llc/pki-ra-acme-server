package com.winllc.acme.server.model.data;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.util.SecurityValidatorUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class DataObject<T> {
    private String id;
    private T object;
    private String accountId;
    private String directory;

    public DataObject(T obj) {
        this.id = SecurityValidatorUtil.generateRandomString(10);
        this.object = obj;
        this.directory = "";
    }

    public DataObject(T obj, DirectoryData directory) {
        this.id = SecurityValidatorUtil.generateRandomString(10);
        this.object = obj;
        this.directory = directory.getName();
    }

    public DataObject(T obj, DirectoryData directory, AccountData accountData) {
        this.id = SecurityValidatorUtil.generateRandomString(10);
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

    public <T> List<List<T>> getPages(Collection<T> c, Integer pageSize) {
        if (c == null)
            return Collections.emptyList();
        List<T> list = new ArrayList<T>(c);
        if (pageSize == null || pageSize <= 0 || pageSize > list.size())
            pageSize = list.size();
        int numPages = (int) Math.ceil((double)list.size() / (double)pageSize);
        List<List<T>> pages = new ArrayList<>(numPages);
        for (int pageNum = 0; pageNum < numPages;)
            pages.add(list.subList(pageNum * pageSize, Math.min(++pageNum * pageSize, list.size())));
        return pages;
    }

    @Override
    public String toString() {
        return "DataObject{" +
                "id='" + id + '\'' +
                ", object=" + object +
                ", accountId='" + accountId + '\'' +
                ", directory='" + directory + '\'' +
                '}';
    }
}
