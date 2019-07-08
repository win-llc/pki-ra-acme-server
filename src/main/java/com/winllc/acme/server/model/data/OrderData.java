package com.winllc.acme.server.model.data;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.model.Order;

public class OrderData extends DataObject<Order> {

    public OrderData(Order obj) {
        super(obj);
    }


    @Override
    public String buildUrl() {
        return Application.baseURL + "order/" + getId();
    }

}
