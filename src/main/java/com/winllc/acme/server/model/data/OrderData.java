package com.winllc.acme.server.model.data;

import com.winllc.acme.server.Application;
import com.winllc.acme.server.model.acme.Directory;
import com.winllc.acme.server.model.acme.Order;

public class OrderData extends DataObject<Order> {

    public OrderData(Order obj, DirectoryData directoryData) {
        super(obj, directoryData);
    }


    @Override
    public String buildUrl() {
        return Application.baseURL + "order/" + getId();
    }

}
