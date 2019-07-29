package com.winllc.acme.server.persistence;

import com.winllc.acme.server.model.data.OrderData;
import com.winllc.acme.server.model.data.OrderListData;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class OrderListPersistence implements DataPersistence<OrderListData> {

    private Map<String, OrderListData> orderListDataMap = new HashMap<>();

    @Override
    public Optional<OrderListData> getById(String id) {
        return Optional.of(orderListDataMap.get(id));
    }

    @Override
    public OrderListData save(OrderListData data) {
        orderListDataMap.put(data.getId(), data);
        return data;
    }
}
