package com.winllc.acme.server.persistence;

import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.OrderData;

import java.util.List;
import java.util.Optional;

public class OrderPersistence implements DataPersistence<OrderData> {

    @Override
    public Optional<OrderData> getById(String id) {
        return Optional.empty();
    }

    public OrderData save(OrderData orderData){
        //TODO
        return null;
    }

    public List<OrderData> getOrdersForAccount(AccountData accountData){
        //TODO
        return null;
    }
}
