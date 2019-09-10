package com.winllc.acme.server.model.data;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.model.acme.Authorization;

public class AuthorizationData extends DataObject<Authorization> {

    private String orderId;

    @Override
    public String buildUrl() {
        return buildBaseUrl() + "authz/" + getId();
    }

    private AuthorizationData(){}

    public AuthorizationData(Authorization object, String directory){
        super(object, directory);
    }

    public AuthorizationData(Authorization authorization, String directory, String orderId){
        this(authorization, directory);
        this.orderId = orderId;
    }


    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }


    @Override
    public String toString() {
        return "AuthorizationData{" +
                "orderId='" + orderId + '\'' +
                ", accountId='" + getAccountId() + '\'' +
                "} " + super.toString();
    }
}
