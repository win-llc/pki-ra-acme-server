package com.winllc.acme.server.model.data;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.model.Authorization;

public class AuthorizationData extends DataObject<Authorization> {

    private String orderId;

    @Override
    public String buildUrl() {
        return Application.baseURL + "authz/" + getId();
    }

    public AuthorizationData(Authorization authorization){
        super(authorization);
    }

    public AuthorizationData(Authorization authorization, OrderData orderData){
        this(authorization);
        this.orderId = orderData.getId();
    }


    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}
