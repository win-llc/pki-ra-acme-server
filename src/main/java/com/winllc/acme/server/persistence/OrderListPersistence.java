package com.winllc.acme.server.persistence;

import com.winllc.acme.server.model.data.OrderData;
import com.winllc.acme.server.model.data.OrderListData;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface OrderListPersistence extends DataPersistence<OrderListData> {

}
