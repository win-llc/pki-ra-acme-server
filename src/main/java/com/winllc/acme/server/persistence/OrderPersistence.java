package com.winllc.acme.server.persistence;

import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.OrderData;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderPersistence implements DataPersistence<OrderData> {

    private Map<String, OrderData> orderDataMap = new ConcurrentHashMap<>();

    @Override
    public Optional<OrderData> getById(String id) {
        return Optional.of(orderDataMap.get(id));
    }

    public OrderData save(OrderData orderData){
        //TODO
        orderDataMap.put(orderData.getId(), orderData);
        return orderData;
    }

    public List<OrderData> getOrdersForAccount(AccountData accountData){
        //TODO
        return null;
    }
}
