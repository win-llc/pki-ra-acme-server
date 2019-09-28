package com.winllc.acme.server.model.data;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.acme.Order;

public class OrderData extends DataObject<Order> {

    public OrderData(Order object, String directory, String accountId) {
        super(object, directory, accountId);
    }

    @Override
    public String buildUrl() {
        return buildBaseUrl() + "order/" + getId();
    }

    @Override
    public String toString() {
        return "OrderData{} " + super.toString();
    }
}
