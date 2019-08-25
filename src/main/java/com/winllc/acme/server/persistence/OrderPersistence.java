package com.winllc.acme.server.persistence;

import com.winllc.acme.server.model.data.AccountData;
import com.winllc.acme.server.model.data.OrderData;
import org.springframework.stereotype.Component;

import java.util.*;
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

        List<OrderData> orders = new ArrayList<>();
        for(OrderData orderData : orderDataMap.values()){
            if(orderData.getAccountId().contentEquals(accountData.getId())){
                orders.add(orderData);
            }
        }

        return orders;
    }
}
