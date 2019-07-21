package com.winllc.acme.server.persistence;

import com.winllc.acme.server.model.data.OrderData;
import com.winllc.acme.server.model.data.OrderListData;

import java.util.Optional;

public class OrderListPersistence implements DataPersistence<OrderListData> {


    @Override
    public Optional<OrderListData> getById(String id) {
        return Optional.empty();
    }

    @Override
    public OrderListData save(OrderListData data) {
        return null;
    }
}
