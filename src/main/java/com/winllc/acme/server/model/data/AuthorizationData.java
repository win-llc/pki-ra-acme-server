package com.winllc.acme.server.model.data;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.model.acme.Authorization;

public class AuthorizationData extends DataObject<Authorization> {

    private String orderId;
    private String accountId;

    @Override
    public String buildUrl() {
        return buildBaseUrl() + "authz/" + getId();
    }

    public AuthorizationData(Authorization authorization, DirectoryData directoryData){
        super(authorization, directoryData);
    }

    public AuthorizationData(Authorization authorization, DirectoryData directoryData, OrderData orderData){
        this(authorization, directoryData);
        this.orderId = orderData.getId();
    }


    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}
